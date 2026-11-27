package com.evmap.serverapp.features.location.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.evmap.serverapp.shared.api.FormValidationException
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.security.MessageDigest
import java.util.UUID

data class LocationView(val id: Long? = null, val candidateId: UUID? = null, val name: String, val address: String? = null, val kind: String = "PLACE", val geometry: String, val lat: Double, val lng: Double)
data class CreateLocationRequest(val name: String, val kind: String = "POINT", val geometry: String, val address: String? = null)
data class ResolveLocationRequest(val candidateId: UUID)

@Service
class LocationService(private val dsl: DSLContext, private val json: ObjectMapper,
    @Value("\${app.geocoding.url:https://nominatim.openstreetmap.org}") private val provider: String,
    @Value("\${app.geocoding.user-agent:Evmap/1.1 (+https://13.50.153.52/)}") private val userAgent: String,
) {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    private var nextRequestAt = 0L

    fun get(id: Long): LocationView = dsl.fetchOne("SELECT id,name,address,kind,ST_AsGeoJSON(shape) AS geometry,ST_Y(ST_PointOnSurface(shape)) AS lat,ST_X(ST_PointOnSurface(shape)) AS lng FROM location WHERE id=?",id)?.let {
        LocationView(it.get("id",Long::class.java),name=it.get("name",String::class.java)!!,address=it.get("address",String::class.java),kind=it.get("kind",String::class.java)!!,geometry=it.get("geometry",String::class.java)!!,lat=(it.get("lat") as Number).toDouble(),lng=(it.get("lng") as Number).toDouble())
    } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND,"Місце не знайдено")

    fun search(query: String, userId: Long): List<LocationView> {
        val term = query.trim()
        if(term.length !in 2..200) throw FormValidationException(mapOf("query" to "Введіть від 2 до 200 символів"))
        val local = dsl.fetch("SELECT id FROM location WHERE name ILIKE ? OR address ILIKE ? ORDER BY id DESC LIMIT 8","%$term%","%$term%").map { get(it.get(0,Long::class.java)!!) }
        val results = try { geocode(term) } catch(ex: ResponseStatusException) { if(local.isNotEmpty()) return local else throw ex }
        dsl.execute("DELETE FROM location_candidate WHERE expires_at<now()")
        val candidates = results.map { node ->
            val id = UUID.randomUUID()
            val lat=node.path("lat").asDouble(); val lng=node.path("lon").asDouble()
            val geometry = node.get("geojson")?.toString() ?: "{\"type\":\"Point\",\"coordinates\":[$lng,$lat]}"
            val kind=when(node.path("addresstype").asText()) { "country"->"COUNTRY"; "city","town","village"->"CITY";"state","county","region"->"REGION";"road","street"->"STREET";else->"PLACE" }
            val candidate=LocationView(candidateId=id,name=node.path("name").asText().ifBlank { node.path("display_name").asText() }.take(200),address=node.path("display_name").asText().take(500),kind=kind,geometry=geometry,lat=lat,lng=lng)
            dsl.execute("INSERT INTO location_candidate(id,user_id,payload,expires_at) VALUES(?,?,?,now()+interval '30 minutes')",id,userId,json.writeValueAsString(candidate))
            candidate
        }
        return local+candidates
    }

    @Synchronized
    private fun geocode(query: String): List<JsonNode> {
        val key=MessageDigest.getInstance("SHA-256").digest(query.lowercase().toByteArray()).joinToString("") { "%02x".format(it) }
        dsl.fetchOne("SELECT payload FROM geocoding_cache WHERE cache_key=? AND expires_at>now()",key)?.let { return json.readTree(it.get(0,String::class.java)).toList() }
        val now=System.currentTimeMillis()
        if(now<nextRequestAt) throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Зачекайте секунду й повторіть пошук")
        nextRequestAt=now+1100
        try {
            val url=provider.trimEnd('/')+"/search?format=jsonv2&addressdetails=1&namedetails=1&limit=5&accept-language=uk&polygon_geojson=1&polygon_threshold=0.02&q="+URLEncoder.encode(query,Charsets.UTF_8)
            val response=http.send(HttpRequest.newBuilder(URI(url)).timeout(Duration.ofSeconds(15)).header("User-Agent",userAgent).GET().build(),HttpResponse.BodyHandlers.ofInputStream())
            val bytes=response.body().use { it.readNBytes(1024*1024+1) }
            if(response.statusCode()!=200 || bytes.size>1024*1024) throw IllegalStateException()
            val tree=json.readTree(bytes)
            check(tree.isArray)
            dsl.execute("DELETE FROM geocoding_cache WHERE expires_at<now()")
            dsl.execute("DELETE FROM geocoding_cache WHERE cache_key IN (SELECT cache_key FROM geocoding_cache ORDER BY expires_at DESC OFFSET 499)")
            dsl.execute("INSERT INTO geocoding_cache(cache_key,payload,expires_at) VALUES(?,?,now()+interval '7 days') ON CONFLICT(cache_key) DO UPDATE SET payload=excluded.payload,expires_at=excluded.expires_at",key,tree.toString())
            return tree.toList()
        } catch(ex: Exception) { throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Пошук адрес тимчасово недоступний. Виберіть місце на карті",ex) }
    }

    @Transactional
    fun resolve(id: UUID,userId: Long): LocationView {
        val row=dsl.fetchOne("SELECT payload,location_id FROM location_candidate WHERE id=? AND user_id=? AND expires_at>now() FOR UPDATE",id,userId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND,"Повторіть пошук місця")
        (row.get("location_id") as? Number)?.toLong()?.let { return get(it) }
        val candidate=json.readValue(row.get("payload",String::class.java),LocationView::class.java)
        val saved=create(CreateLocationRequest(candidate.name,candidate.kind,candidate.geometry,candidate.address),userId)
        dsl.execute("UPDATE location_candidate SET location_id=? WHERE id=?",saved.id,id)
        return saved
    }

    @Transactional
    fun create(request: CreateLocationRequest,userId: Long): LocationView {
        if(request.name.trim().length !in 1..200) throw FormValidationException(mapOf("location" to "Укажіть назву місця до 200 символів"))
        if(request.address.orEmpty().length>500 || request.kind !in setOf("POINT","PLACE","CITY","COUNTRY","REGION","STREET","CUSTOM_AREA")) throw FormValidationException(mapOf("location" to "Перевірте адресу й тип місця"))
        val node=runCatching { json.readTree(request.geometry) }.getOrNull()
        if(node==null || request.geometry.length>500_000 || node.path("type").asText() !in setOf("Point","LineString","MultiLineString","Polygon","MultiPolygon")) throw FormValidationException(mapOf("location" to "Непідтримувана геометрія місця"))
        fun validCoordinates(value: JsonNode): Boolean = if(value.isArray && value.size()>0 && value[0].isNumber) value.size()==2 && value[0].asDouble().isFinite() && value[1].asDouble().isFinite() && value[0].asDouble() in -180.0..180.0 && value[1].asDouble() in -90.0..90.0 else value.isArray && value.size()>0 && value.all(::validCoordinates)
        if(!validCoordinates(node.path("coordinates"))) throw FormValidationException(mapOf("location" to "Перевірте координати місця"))
        val valid=try { dsl.fetchOne("SELECT ST_IsValid(g) AND NOT ST_IsEmpty(g) AND ST_NPoints(g)<=5000 AS valid FROM (SELECT ST_SetSRID(ST_GeomFromGeoJSON(?),4326) g) s",request.geometry)!!.get(0,Boolean::class.java) == true } catch(_: Exception) { throw FormValidationException(mapOf("location" to "Контур місця некоректний")) }
        if(!valid) throw FormValidationException(mapOf("location" to "Контур перетинає себе або містить забагато точок"))
        val row=dsl.fetchOne("""WITH geom AS (SELECT ST_SetSRID(ST_GeomFromGeoJSON(?),4326) g) INSERT INTO location(name,category_id,point,shape,kind,address,created_by) SELECT ?, (SELECT id FROM location_category WHERE name='Місце' LIMIT 1), ST_PointOnSurface(g), g, ?, ?, ? FROM geom RETURNING id""",request.geometry,request.name.trim(),request.kind,request.address?.trim(),userId)!!
        return get(row.get(0,Long::class.java)!!)
    }
}
