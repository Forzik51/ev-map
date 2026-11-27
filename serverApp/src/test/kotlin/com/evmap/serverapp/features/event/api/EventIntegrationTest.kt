package com.evmap.serverapp.features.event.api

import com.evmap.serverapp.features.auth.PasswordResetIntegrationTest
import com.evmap.serverapp.features.event.domain.EventRepositoryPort
import com.evmap.serverapp.features.user.domian.UserRepositoryPort
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.support.TransactionTemplate
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.util.Random
import java.util.UUID
import javax.imageio.ImageIO

@EnabledIfEnvironmentVariable(named = "EVMAP_TEST_DB_URL", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventIntegrationTest {
    @LocalServerPort private var port = 0
    @Autowired private lateinit var db: DSLContext
    @Autowired private lateinit var json: ObjectMapper
    @Autowired private lateinit var encoder: PasswordEncoder
    @Autowired private lateinit var transactions: TransactionTemplate
    @Autowired private lateinit var events: EventRepositoryPort
    @Autowired private lateinit var users: UserRepositoryPort

    private val client = HttpClient.newHttpClient()
    private lateinit var owner: String
    private lateinit var visitor: String
    private lateinit var admin: String
    private var locationId = 0L
    private var categoryId = 0L

    companion object {
        @TempDir @JvmField var mediaRoot: Path? = null
        private val geocoderRequests=java.util.concurrent.atomic.AtomicInteger()
        private val geocoder=com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1",0),0).apply {
            createContext("/search") { exchange ->
                geocoderRequests.incrementAndGet()
                val reply="""[{"name":"Fixture country","display_name":"Fixture country address","lat":"52","lon":"21","addresstype":"country","geojson":{"type":"Polygon","coordinates":[[[20,51],[22,51],[22,53],[20,53],[20,51]]]}}]""".toByteArray()
                exchange.responseHeaders.add("Content-Type","application/json")
                exchange.sendResponseHeaders(200,reply.size.toLong());exchange.responseBody.use {it.write(reply)}
            }
            start()
        }
        @org.junit.jupiter.api.AfterAll @JvmStatic fun stopGeocoder() { geocoder.stop(0) }


        @JvmStatic @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            PasswordResetIntegrationTest.database(registry)
            registry.add("app.geocoding.url") {"http://127.0.0.1:${geocoder.address.port}"}
            registry.add("app.media.root") { checkNotNull(mediaRoot).toString() }
        }
    }

    @BeforeEach
    fun setup() {
        owner = login()
        visitor = login()
        admin = login("ADMIN")
        locationId = db.fetchOne("SELECT id FROM location ORDER BY id LIMIT 1")!!.get(0, Long::class.java)!!
        categoryId = db.fetchOne("SELECT id FROM category ORDER BY id LIMIT 1")!!.get(0, Long::class.java)!!
    }

    @Test
    fun `only owner or admin can edit and delete, and edit preserves other fields`() {
        val id = create()
        assertEquals(403, request("PATCH", "/api/events/$id", visitor, eventFields("Forbidden")).statusCode())
        assertEquals(403, request("DELETE", "/api/events/$id", visitor).statusCode())
        assertEquals(204, request("PATCH", "/api/events/$id", owner, eventFields("Edited")).statusCode())
        val event = body(request("GET", "/api/events/$id", owner))
        assertEquals("Edited", event["name"].asText())
        assertEquals(locationId, event["locationId"].asLong())
        assertEquals("2030-03-22T10:00:00Z", event["startsAt"].asText())
        assertEquals("2030-03-22T12:30:00Z", event["endsAt"].asText())
        assertEquals(categoryId, event["categories"].first()["id"].asLong())
        assertEquals(204, request("PATCH", "/api/events/$id", admin, eventFields("Moderated")).statusCode())
        assertEquals(204, request("DELETE", "/api/events/$id", admin).statusCode())
        assertEquals(404, request("GET", "/api/events/$id", owner).statusCode())
    }

    @Test
    fun `catalog pages sort equal start dates by id`() {
        val first = create("Same date A", "2090-03-22")
        val second = create("Same date B", "2090-03-22")
        val page0 = body(request("GET", "/api/events?page=0&size=1", owner))
        val page1 = body(request("GET", "/api/events?page=1&size=1", owner))
        assertEquals(second, page0["content"].first()["id"].asLong())
        assertEquals(first, page1["content"].first()["id"].asLong())
        request("DELETE", "/api/events/$first", owner)
        request("DELETE", "/api/events/$second", owner)
    }

    @Test
    fun `profile pagination filters author before applying limit and offset`() {
        val authorId = body(request("GET", "/api/users/me", owner))["id"].asLong()
        val first = create("First by author", "2090-03-22")
        val second = create("Second by author", "2090-03-22")
        val otherId = body(request("GET", "/api/users/me", visitor))["id"].asLong()
        db.execute(
            """
            INSERT INTO event(name, description, location_id, start_at, created_at, user_id)
            SELECT 'Above author events ' || n, '', ?, TIMESTAMPTZ '2091-01-01 00:00:00Z', now(), ?
            FROM generate_series(1, 51) n
            """.trimIndent(), locationId, otherId,
        )
        try {
            val firstPage = body(request("GET", "/api/users/$authorId/events?page=0&size=1", visitor))
            val secondPage = body(request("GET", "/api/users/$authorId/events?page=1&size=1", visitor))
            assertEquals(2, firstPage["totalElements"].asInt())
            assertEquals(second, firstPage["content"].single()["id"].asLong())
            assertEquals(first, secondPage["content"].single()["id"].asLong())
            assertTrue(secondPage["last"].asBoolean())
        } finally {
            db.execute("DELETE FROM event WHERE user_id = ?", otherId)
            events.delete(first)
            events.delete(second)
        }
    }

    @Test
    fun `account deletion cleans owned records and preserves other users replies and files on rollback`() {
        val ownerId = body(request("GET", "/api/users/me", owner))["id"].asLong()
        val visitorId = body(request("GET", "/api/users/me", visitor))["id"].asLong()
        val event = create()
        val visitorEvent = body(request("POST", "/api/events", visitor, eventFields("Keep this event"))).asLong()
        val mediaPath = body(upload("POST", "/api/events/$event/photos", owner, png(12)))["imagePath"].asText()
        val file = checkNotNull(mediaRoot).resolve(mediaPath.removePrefix("/media/"))
        db.execute("INSERT INTO search_history(user_id, query, search_type, created_at) VALUES (?, 'Test', 'EVENT', now())", ownerId)
        db.execute("INSERT INTO rating(rating, event_id, user_id) VALUES (4, ?, ?)", visitorEvent, ownerId)
        db.execute("INSERT INTO repost(event_id, user_id) VALUES (?, ?)", visitorEvent, ownerId)
        val parent = db.fetchOne(
            "INSERT INTO comment(text, event_id, comment_at, user_id) VALUES ('Parent', ?, now(), ?) RETURNING id",
            visitorEvent, ownerId,
        )!!.get(0, Long::class.javaObjectType)!!
        val reply = db.fetchOne(
            "INSERT INTO comment(text, event_id, parent_comment_id, comment_at, user_id) VALUES ('Keep reply', ?, ?, now(), ?) RETURNING id",
            visitorEvent, parent, visitorId,
        )!!.get(0, Long::class.javaObjectType)!!
        val company = db.fetchOne(
            "INSERT INTO company(name, link, user_id) VALUES ('Example', 'https://example.invalid', ?) RETURNING id", ownerId,
        )!!.get(0, Long::class.javaObjectType)!!
        db.execute("INSERT INTO advertising(start_date, end_date, event_id, company_id) VALUES (now(), now(), ?, ?)", visitorEvent, company)

        transactions.executeWithoutResult { status -> users.delete(ownerId); status.setRollbackOnly() }
        assertTrue(Files.exists(file))
        assertEquals(200, request("GET", "/api/events/$event", owner).statusCode())
        assertEquals(204, request("DELETE", "/api/users/$ownerId", owner).statusCode())
        assertEquals(404, request("GET", "/api/events/$event", visitor).statusCode())
        assertEquals(200, request("GET", "/api/events/$visitorEvent", visitor).statusCode())
        assertTrue(Files.notExists(file))
        assertTrue(db.fetchOne("SELECT id FROM comment WHERE id = ? AND parent_comment_id IS NULL", reply) != null)
        assertEquals(0, db.fetchOne("SELECT count(*) FROM refresh_token WHERE user_id = ?", ownerId)!!.get(0, Int::class.javaObjectType))
    }

    @Test
    fun `feed sorts tied events by id across pages and marks a full last page`() {
        val pageSize = 2
        val existing = body(request("GET", "/api/events/feed?size=$pageSize", owner))["totalElements"].asInt()
        val created = mutableListOf<Long>()
        try {
            repeat(2 * pageSize + existing % pageSize) { index ->
                created += create("Feed event $index", "2090-03-22")
            }
            val pageCount = (existing + created.size) / pageSize
            val ids = (0 until pageCount).flatMap { number ->
                val response = request("GET", "/api/events/feed?page=$number&size=$pageSize", owner)
                assertEquals(200, response.statusCode())
                val page = body(response)
                assertEquals(number, page["number"].asInt())
                assertEquals(pageSize, page["content"].size())
                assertEquals(number == pageCount - 1, page["last"].asBoolean())
                page["content"].map { it["id"].asLong() }
            }
            assertEquals(existing + created.size, ids.distinct().size)
            assertEquals(created.sortedDescending(), ids.filter { it in created })
        } finally {
            created.forEach { id -> request("DELETE", "/api/events/$id", owner) }
        }
    }

    @Test
    fun `event search filters by category and supports rating and date sort`() {
        val otherCategory = db.fetchOne("SELECT id FROM category WHERE id <> ? ORDER BY id LIMIT 1", categoryId)!!
            .get(0, Long::class.java)!!
        val term = "Подія & # ${UUID.randomUUID().toString().take(8)}"
        val created = mutableListOf<Long>()
        try {
            val high = create("$term High", "2030-03-22").also { created += it }
            val recent = create("$term Recent", "2030-03-23").also { created += it }
            val outside = create("$term Other", "2030-03-24", otherCategory).also { created += it }
            for ((id, rating) in listOf(high to 4, recent to 3, outside to 5)) {
                assertEquals(204, request("POST", "/api/events/$id/reactions?reaction=$rating", visitor).statusCode())
            }
            val encodedTerm = URLEncoder.encode(term, Charsets.UTF_8)
            val path = "/api/search?query=$encodedTerm&type=events&limit=30"
            fun results(parameters: String): List<Long> {
                val response = request("GET", "$path&$parameters", owner)
                assertEquals(200, response.statusCode(), response.body().decodeToString())
                return body(response)["events"].map { it["id"].asLong() }
            }

            assertEquals(listOf(outside, high, recent), results("sort=rating"))
            assertEquals(listOf(high, recent), results("sort=rating&categoryIds=$categoryId"))
            assertEquals(listOf(recent, high), results("sort=date&categoryIds=$categoryId"))
        } finally {
            created.forEach { id -> request("DELETE", "/api/events/$id", owner) }
        }
    }

    @Test
    fun `saved lists support owned CRUD and return event cards without duplicating membership`() {
        val event = create("Saved event")
        val created = request("POST", "/api/lists", owner, mapOf("name" to "  Weekend  "))
        assertEquals(201, created.statusCode())
        val listId = body(created).asLong()
        val path = "/api/lists/$listId"
        val membership = "$path/events/$event"
        try {
            val empty = body(request("GET", path, owner))
            assertEquals("Weekend", empty["list"]["name"].asText())
            assertEquals(0, empty["list"]["eventsCount"].asInt())
            assertTrue(empty["events"].isEmpty)

            assertEquals(403, request("GET", path, visitor).statusCode())
            assertEquals(403, request("POST", membership, visitor).statusCode())
            assertEquals(403, request("DELETE", membership, visitor).statusCode())
            assertEquals(403, request("PATCH", "$path/name?name=Forbidden", visitor).statusCode())
            assertEquals(403, request("DELETE", path, visitor).statusCode())
            assertEquals(200, request("GET", path, admin).statusCode())

            repeat(2) { assertEquals(204, request("POST", membership, owner).statusCode()) }
            val details = body(request("GET", path, owner))
            assertEquals(1, details["list"]["eventsCount"].asInt())
            assertEquals(event, details["events"].single()["id"].asLong())
            assertEquals("Saved event", details["events"].single()["name"].asText())
            assertEquals(listOf(event), body(request("GET", "$path/events", owner)).map { it.asLong() })

            for (invalid in listOf("  ", "a".repeat(21))) {
                val name = URLEncoder.encode(invalid, Charsets.UTF_8)
                assertEquals(400, request("POST", "/api/lists", owner, mapOf("name" to invalid)).statusCode())
                assertEquals(400, request("PATCH", "$path/name?name=$name", owner).statusCode())
            }
            val newName = "R".repeat(20)
            val encoded = URLEncoder.encode("  $newName  ", Charsets.UTF_8)
            assertEquals(204, request("PATCH", "$path/name?name=$encoded", owner).statusCode())
            val summary = body(request("GET", "/api/lists/me", owner)).first { it["id"].asLong() == listId }
            assertEquals(newName, summary["name"].asText())
            assertEquals(1, summary["eventsCount"].asInt())

            repeat(2) { assertEquals(204, request("DELETE", membership, owner).statusCode()) }
            assertTrue(body(request("GET", path, owner))["events"].isEmpty)
            assertEquals(204, request("POST", membership, owner).statusCode())
            assertEquals(204, request("DELETE", path, owner).statusCode())
            assertEquals(404, request("GET", path, owner).statusCode())
            assertEquals(200, request("GET", "/api/events/$event", owner).statusCode())
        } finally {
            request("DELETE", path, owner)
            request("DELETE", "/api/events/$event", owner)
        }
    }

    @Test
    fun `comments reject invalid text and rating changes update the same row`() {
        val id = create()
        assertEquals(400, comment(id, "   ").statusCode())
        assertEquals(400, comment(id, "a".repeat(101)).statusCode())
        assertEquals(204, comment(id, "  A useful comment  ").statusCode())
        val comments = body(request("GET", "/api/events/$id/comments?page=0&size=1", owner))
        assertEquals("A useful comment", comments["content"].first()["text"].asText())
        assertEquals(1L, comments["totalElements"].asLong())

        fun feedEvent(): JsonNode = body(request("GET", "/api/events/feed?size=100", owner))["content"]
            .first { it["id"].asLong() == id }

        assertTrue(feedEvent()["ratingAvg"].isNull)
        assertEquals(400, request("POST", "/api/events/$id/reactions?reaction=6", visitor).statusCode())
        assertEquals(204, request("POST", "/api/events/$id/reactions?reaction=0", visitor).statusCode())
        assertEquals(0.0, feedEvent()["ratingAvg"].asDouble())
        assertEquals(204, request("POST", "/api/events/$id/reactions?reaction=5", visitor).statusCode())
        assertEquals(204, request("POST", "/api/events/$id/reactions?reaction=4", owner).statusCode())
        val event = body(request("GET", "/api/events/$id", visitor))
        assertEquals(4.5, event["ratingAvg"].asDouble())
        assertEquals(5.0, event["myRating"].asDouble())
        assertEquals(2, event["ratingsCount"].asInt())
        assertEquals(1, event["commentsCount"].asInt())
        assertEquals(4.0, body(request("GET", "/api/events/$id", owner))["myRating"].asDouble())
        for (path in listOf("/api/events?size=100", "/api/events/feed?size=100")) {
            val listed = body(request("GET", path, owner))["content"].first { it["id"].asLong() == id }
            assertEquals(4.5, listed["ratingAvg"].asDouble())
        }
        assertEquals(204, request("DELETE", "/api/events/$id", owner).statusCode())
    }

    @Test
    fun `deleting a legacy event detaches comment references from other events`() {
        val id = create()
        val otherId = create("Other event")
        assertEquals(204, comment(id, "Parent comment").statusCode())
        val parentId = body(request("GET", "/api/events/$id/comments", owner))["content"].first()["id"].asLong()
        db.execute(
            """
            INSERT INTO comment(text, event_id, parent_comment_id, comment_at, user_id)
            SELECT 'Imported comment', id, ?, now(), user_id FROM event WHERE id = ?
            """.trimIndent(),
            parentId, otherId,
        )

        assertEquals(204, request("DELETE", "/api/events/$id", owner).statusCode())
        val surviving = body(request("GET", "/api/events/$otherId/comments", owner))["content"].first()
        assertEquals("Imported comment", surviving["text"].asText())
        assertTrue(surviving["parentCommentId"].isNull)
        assertEquals(204, request("DELETE", "/api/events/$otherId", owner).statusCode())
    }

    @Test
    fun `photo writes require ownership, retries reuse the photo, and media requires JWT`() {
        val id = create()
        val uploadId = UUID.randomUUID()
        val path = "/api/events/$id/photos"
        val png = png(10)
        assertEquals(403, upload("POST", path, visitor, png).statusCode())
        assertEquals(400, upload("POST", path, owner, "not a png".toByteArray()).statusCode())
        val uploaded = upload("POST", "$path?uploadId=$uploadId", owner, png)
        assertEquals(201, uploaded.statusCode())
        val photo = body(uploaded)
        val retry = upload("POST", "$path?uploadId=$uploadId", owner, png)
        assertEquals(201, retry.statusCode())
        assertEquals(photo, body(retry))
        assertEquals(1, body(request("GET", path, owner)).size())
        val photoPath = "$path/${photo["id"].asLong()}"
        val mediaPath = photo["imagePath"].asText()
        assertEquals(401, request("GET", mediaPath, null).statusCode())
        assertEquals(200, request("GET", mediaPath, visitor).statusCode())
        assertEquals(403, upload("PUT", photoPath, visitor, png).statusCode())
        assertEquals(403, request("DELETE", photoPath, visitor).statusCode())

        val replaced = upload("PUT", photoPath, admin, png(11))
        assertEquals(200, replaced.statusCode())
        val replacementPath = body(replaced)["imagePath"].asText()
        assertEquals(404, request("GET", mediaPath, owner).statusCode())
        assertEquals(200, request("GET", replacementPath, owner).statusCode())
        assertEquals(204, request("DELETE", photoPath, owner).statusCode())
        assertEquals(404, request("GET", replacementPath, owner).statusCode())
        assertEquals(204, request("DELETE", "/api/events/$id", owner).statusCode())
    }

    @Test
    fun `uploads above the old limit work and rolling back event deletion retains the image`() {
        val id = create()
        val png = png(850)
        assertTrue(png.size > 1024 * 1024)
        assertTrue(png.size < 5 * 1024 * 1024)
        val uploaded = upload("POST", "/api/events/$id/photos", owner, png)
        assertEquals(201, uploaded.statusCode())
        val mediaPath = body(uploaded)["imagePath"].asText()
        val file = checkNotNull(mediaRoot).resolve(mediaPath.removePrefix("/media/"))
        db.execute("INSERT INTO photo(event_id, image_path) VALUES (?, ?)", id, "https://example.com/legacy.png")
        transactions.executeWithoutResult { status ->
            events.delete(id)
            status.setRollbackOnly()
        }
        assertEquals(200, request("GET", "/api/events/$id", owner).statusCode())
        assertTrue(Files.exists(file))
        assertEquals(204, request("DELETE", "/api/events/$id", owner).statusCode())
        assertEquals(404, request("GET", mediaPath, owner).statusCode())
        assertTrue(Files.notExists(file))
    }

    @Test
    fun `undated creation is idempotent and can be read from every event surface`() {
        val key=UUID.randomUUID().toString()
        val fields=eventFields("Undated deployment").toMutableMap().apply { remove("startsAt");remove("endsAt");put("dateMode","UNSPECIFIED");put("clientRequestId",key);put("tags",listOf("Jazz","jazz")) }
        val first=request("POST","/api/events",owner,fields)
        assertEquals(201,first.statusCode(),first.body().decodeToString())
        val id=body(first).asLong()
        assertEquals(id,body(request("POST","/api/events",owner,fields)).asLong())
        assertEquals(409,request("POST","/api/events",owner,fields+mapOf("name" to "Changed payload")).statusCode())
        val event=body(request("GET","/api/events/$id",owner))
        assertTrue(event["startsAt"].isNull)
        assertEquals("UNSPECIFIED",event["dateMode"].asText())
        assertEquals(1,event["tags"].size())
        for(path in listOf("/api/events?page=0&size=100","/api/events/feed?page=0&size=100","/api/map/events/$id","/api/search?query=Undated&type=events&tags=jazz")) {
            val response=request("GET",path,owner);assertEquals(200,response.statusCode(),response.body().decodeToString())
        }
        assertEquals(204,request("DELETE","/api/events/$id",owner).statusCode())
        assertEquals(410,request("POST","/api/events",owner,fields).statusCode())
    }

    @Test
    fun `date-only schedule preserves calendar day across a timezone transition`() {
        val fields=eventFields("Date only").toMutableMap().apply {remove("startsAt");remove("endsAt");put("dateMode","DATE");put("startsOn","2027-03-28");put("timeZone","Europe/Warsaw")}
        val response=request("POST","/api/events",owner,fields)
        assertEquals(201,response.statusCode(),response.body().decodeToString())
        val id=body(response).asLong()
        val event=body(request("GET","/api/events/$id",owner))
        assertEquals("2027-03-28",event["startsOn"].asText())
        assertEquals("Europe/Warsaw",event["timeZone"].asText())
        val invalid=request("PATCH","/api/events/$id",owner,fields+mapOf("endsOn" to "2027-03-27"))
        assertEquals(400,invalid.statusCode())
        assertTrue(body(invalid)["fieldErrors"].has("endsOn"))
        request("DELETE","/api/events/$id",owner)
    }

    @Test
    fun `custom area uses its entire geometry for nearby and tag filters`() {
        val geometry="""{"type":"Polygon","coordinates":[[[20,51],[22,51],[22,53],[20,53],[20,51]]]}"""
        val place=request("POST","/api/locations",owner,mapOf("name" to "Wide test area","kind" to "CUSTOM_AREA","geometry" to geometry))
        assertEquals(200,place.statusCode(),place.body().decodeToString())
        val location=body(place)["id"].asLong()
        val response=request("POST","/api/events",owner,eventFields("Area jazz")+mapOf("locationId" to location,"tags" to listOf("Jazz")))
        assertEquals(201,response.statusCode(),response.body().decodeToString())
        val id=body(response).asLong()
        val nearby=request("GET","/api/map/events/nearby?lat=52.9&lng=21.9&radiusMeters=100&tags=jazz",owner)
        assertEquals(200,nearby.statusCode(),nearby.body().decodeToString())
        assertTrue(body(nearby).any {it["eventId"].asLong()==id})
        val none=body(request("GET","/api/map/events/nearby?lat=52.9&lng=21.9&radiusMeters=100&tags=unmatched",owner))
        assertTrue(none.none {it["eventId"].asLong()==id})
        val invalid=request("POST","/api/locations",owner,mapOf("name" to "Bad","geometry" to """{"type":"Polygon","coordinates":[[[20,51],[22,53],[22,51],[20,53],[20,51]]]}"""))
        assertEquals(400,invalid.statusCode())
        request("DELETE","/api/events/$id",owner)
        db.execute("DELETE FROM location WHERE id=?",location)
    }

    @Test
    fun `photo order controls the cover and remains owned`() {
        val id=create("Ordered photos")
        val a=body(upload("POST","/api/events/$id/photos",owner,png(2)))
        val b=body(upload("POST","/api/events/$id/photos",owner,png(3)))
        val order=listOf(b["id"].asLong(),a["id"].asLong())
        assertEquals(403,request("PUT","/api/events/$id/photos/order",visitor,order).statusCode())
        assertEquals(204,request("PUT","/api/events/$id/photos/order",owner,order).statusCode())
        assertEquals(b["imagePath"].asText(),body(request("GET","/api/events/$id",owner))["imagePath"].asText())
        assertEquals(b["id"].asLong(),body(request("GET","/api/events/$id/photos",owner)).first()["id"].asLong())
        assertEquals(400,request("PUT","/api/events/$id/photos/order",owner,listOf(a["id"].asLong())).statusCode())
        request("DELETE","/api/events/$id",owner)
    }

    @Test
    fun `category proposal needs an administrator decision`() {
        val name="Category ${UUID.randomUUID()}"
        val proposal=request("POST","/api/event-taxonomy/category-requests",owner,mapOf("name" to name))
        assertEquals(200,proposal.statusCode(),proposal.body().decodeToString())
        val id=body(proposal)["id"].asLong()
        assertEquals(403,request("POST","/api/event-taxonomy/category-requests/$id/decision",visitor,mapOf("approved" to true)).statusCode())
        assertEquals(200,request("POST","/api/event-taxonomy/category-requests/$id/decision",admin,mapOf("approved" to true)).statusCode())
        assertTrue(body(request("GET","/api/events/categories",owner)).any {it["name"].asText()==name})
    }

    @Test
    fun `external place search is cached and a candidate resolves only once`() {
        val before=geocoderRequests.get()
        val query="fixture-${UUID.randomUUID()}"
        val first=request("GET","/api/locations/search?query=$query",owner)
        assertEquals(200,first.statusCode(),first.body().decodeToString())
        val candidate=body(first).first()
        assertEquals("COUNTRY",candidate["kind"].asText())
        assertEquals(200,request("GET","/api/locations/search?query=$query",owner).statusCode())
        assertEquals(before+1,geocoderRequests.get())
        val payload=mapOf("candidateId" to candidate["candidateId"].asText())
        val place=request("POST","/api/locations/resolve",owner,payload)
        assertEquals(200,place.statusCode(),place.body().decodeToString())
        val id=body(place)["id"].asLong()
        assertEquals(id,body(request("POST","/api/locations/resolve",owner,payload))["id"].asLong())
        assertEquals(404,request("POST","/api/locations/resolve",visitor,payload).statusCode())
    }
    @Test
    fun `registration accepts missing optional contacts and validates a supplied birthday`() {
        val suffix=UUID.randomUUID().toString().take(12)
        val user=mapOf("name" to "Optional","surname" to null,"email" to "$suffix@example.com","phone" to null,"dateOfBirth" to null,"username" to suffix,"pageDescription" to "","password" to "valid password")
        val registered=request("POST","/api/auth/register",null,mapOf("user" to user))
        assertEquals(201,registered.statusCode(),registered.body().decodeToString())
        val token=body(registered)["accessToken"].asText()
        val profile=body(request("GET","/api/users/me",token))
        assertTrue(profile["phone"].isNull)
        assertTrue(profile["birthdate"].isNull)
        val invalid=request("PATCH","/api/users/me",token,mapOf("name" to "Optional","username" to suffix,"surname" to null,"phone" to null,"pageDescription" to "","dateOfBirth" to java.time.LocalDate.now().plusDays(1).toString(),"updateDateOfBirth" to true))
        assertEquals(400,invalid.statusCode())
        assertTrue(body(invalid)["fieldErrors"].has("dateOfBirth"))
        request("DELETE","/api/users/${profile["id"].asLong()}",token)
    }
    @Test
    fun `simultaneous publication retries create a single event`() {
        val fields=eventFields("Concurrent publish")+mapOf("clientRequestId" to UUID.randomUUID().toString())
        val first=java.util.concurrent.CompletableFuture.supplyAsync {request("POST","/api/events",owner,fields)}
        val second=java.util.concurrent.CompletableFuture.supplyAsync {request("POST","/api/events",owner,fields)}
        val a=first.get(20,java.util.concurrent.TimeUnit.SECONDS);val b=second.get(20,java.util.concurrent.TimeUnit.SECONDS)
        assertEquals(201,a.statusCode(),a.body().decodeToString());assertEquals(201,b.statusCode(),b.body().decodeToString())
        assertEquals(body(a).asLong(),body(b).asLong())
        request("DELETE","/api/events/${body(a).asLong()}",owner)
    }

    private fun create(name: String = "Test event", date: String = "2030-03-22", category: Long = categoryId): Long {
        val response = request("POST", "/api/events", owner, eventFields(name, date, category))
        assertEquals(201, response.statusCode(), response.body().decodeToString())
        return body(response).asLong()
    }

    private fun eventFields(name: String, date: String = "2030-03-22", category: Long = categoryId) = mapOf(
        "name" to name,
        "description" to "Test description",
        "startsAt" to "${date}T10:00:00Z",
        "endsAt" to "${date}T12:30:00Z",
        "locationId" to locationId,
        "categoryIds" to listOf(category),
    )

    private fun login(role: String = "USER"): String {
        val id = UUID.randomUUID().toString()
        val email = "$id@example.com"
        val password = "test password"
        db.execute(
            """INSERT INTO app_user(name, email, phone, birthdate, password_hash, username, page_description, role)
               VALUES ('Test', ?, ?, DATE '1990-01-01', ?, ?, 'Test account', ?)""",
            email, id.take(15), encoder.encode(password), id.take(20), role,
        )
        val response = request("POST", "/api/auth/login", null, mapOf("usernameOrEmail" to email, "password" to password))
        assertEquals(200, response.statusCode())
        return body(response)["accessToken"].asText()
    }

    private fun comment(eventId: Long, text: String): HttpResponse<ByteArray> =
        request("POST", "/api/events/$eventId/comments?comment=${URLEncoder.encode(text, Charsets.UTF_8)}", visitor)

    private fun request(method: String, path: String, token: String?, payload: Any? = null): HttpResponse<ByteArray> {
        val builder = HttpRequest.newBuilder(URI("http://127.0.0.1:$port$path"))
        if (token != null) builder.header("Authorization", "Bearer $token")
        val data = if (payload == null) HttpRequest.BodyPublishers.noBody() else {
            builder.header("Content-Type", "application/json")
            HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(payload))
        }
        return client.send(builder.method(method, data).build(), HttpResponse.BodyHandlers.ofByteArray())
    }

    private fun upload(method: String, path: String, token: String, bytes: ByteArray): HttpResponse<ByteArray> {
        val boundary = UUID.randomUUID().toString()
        val header = ("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"image.png\"\r\n" +
            "Content-Type: image/png\r\n\r\n").toByteArray()
        val footer = "\r\n--$boundary--\r\n".toByteArray()
        val request = HttpRequest.newBuilder(URI("http://127.0.0.1:$port$path"))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "multipart/form-data; boundary=$boundary")
            .method(method, HttpRequest.BodyPublishers.ofByteArray(header + bytes + footer))
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofByteArray())
    }

    private fun body(response: HttpResponse<ByteArray>): JsonNode = json.readTree(response.body())

    private fun png(size: Int): ByteArray {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
        val random = Random(17)
        for (y in 0 until size) {
            for (x in 0 until size) image.setRGB(x, y, random.nextInt())
        }
        return ByteArrayOutputStream().use { stream ->
            ImageIO.write(image, "png", stream)
            stream.toByteArray()
        }
    }
}
