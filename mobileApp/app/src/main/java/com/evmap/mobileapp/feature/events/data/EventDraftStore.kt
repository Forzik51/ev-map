package com.evmap.mobileapp.feature.events.data

import android.content.Context
import android.util.AtomicFile
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import com.evmap.mobileapp.core.data.session.TokenStore
import com.evmap.mobileapp.feature.events.ui.EventFormState
import com.evmap.mobileapp.feature.events.ui.PendingEventPhoto
import com.evmap.mobileapp.core.data.dto.EventPhotoDto
import com.evmap.mobileapp.core.data.dto.CreateEventRequest
import com.squareup.moshi.Moshi
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventDraftStore @Inject constructor(@ApplicationContext private val context: Context,private val tokens: TokenStore,private val moshi: Moshi) {
    fun owner()=tokens.session()?.userId ?: 0L
    private fun file(owner: Long,id: Long?)=File(context.filesDir,"event-drafts/$owner/${id ?: "new"}.json")
    @Synchronized fun read(owner: Long,id: Long?): JSONObject? = runCatching { JSONObject(AtomicFile(file(owner,id)).openRead().bufferedReader().use { it.readText() }) }.getOrNull()
    @Synchronized fun write(owner: Long,id: Long?,s: EventFormState) {
        val data=JSONObject().put("title",s.title).put("description",s.description).put("dateEnabled",s.dateEnabled).put("timeEnabled",s.timeEnabled).put("startsAt",s.startsAt?.toString()).put("endsAt",s.endsAt?.toString()).put("timeZone",s.timeZone)
            .put("locationId",s.locationId).put("locationName",s.locationName).put("categories",JSONArray(s.categoryIds.toList())).put("tags",s.tagsText).put("requestId",s.clientRequestId).put("savedEventId",s.savedEventId).put("order",JSONArray(s.photoOrder)).put("deletions",JSONArray(s.photosToDelete.toList())).put("discarded",JSONArray(s.discardedUploadIds.toList()))
            .put("pending",JSONArray(s.pendingPhotos.map { JSONObject().put("uri",it.uri.toString()).put("id",it.uploadId) })).put("existing",JSONArray(s.existingPhotos.map { JSONObject().put("id",it.id).put("eventId",it.eventId).put("path",it.imagePath) }))
        s.submission?.let { data.put("submission",moshi.adapter(CreateEventRequest::class.java).toJson(it)) }
        val target=file(owner,id);target.parentFile!!.mkdirs();val atomic=AtomicFile(target);val out=atomic.startWrite()
        try { out.write(data.toString().toByteArray());atomic.finishWrite(out) } catch(ex: Exception) { atomic.failWrite(out);throw ex }
    }
    fun restore(owner: Long,id: Long?,base: EventFormState): EventFormState {
        val d=read(owner,id) ?: return base
        fun optional(key: String)=d.optString(key).takeIf { it.isNotBlank() && it!="null" }
        fun longs(key: String): Set<Long> = d.optJSONArray(key)?.let { a->(0 until a.length()).map { a.getLong(it) }.toSet() }.orEmpty()
        val pending=d.optJSONArray("pending")?.let { a->(0 until a.length()).map { a.getJSONObject(it) }.map { PendingEventPhoto(Uri.parse(it.getString("uri")),it.getString("id")) } }.orEmpty()
        val existing=d.optJSONArray("existing")?.let { a->(0 until a.length()).map { a.getJSONObject(it) }.map { EventPhotoDto(it.getLong("id"),it.getLong("eventId"),it.getString("path")) } }.orEmpty()
        return base.copy(title=d.optString("title"),description=d.optString("description"),dateEnabled=d.optBoolean("dateEnabled"),timeEnabled=d.optBoolean("timeEnabled"),startsAt=optional("startsAt")?.let(Instant::parse),endsAt=optional("endsAt")?.let(Instant::parse),timeZone=d.optString("timeZone",base.timeZone),locationId=optional("locationId")?.toLongOrNull(),locationName=d.optString("locationName"),categoryIds=longs("categories"),tagsText=d.optString("tags"),clientRequestId=d.optString("requestId",base.clientRequestId),savedEventId=optional("savedEventId")?.toLongOrNull(),pendingPhotos=pending,existingPhotos=existing,photosToDelete=longs("deletions"),photoOrder=d.optJSONArray("order")?.let { a->(0 until a.length()).map { a.getString(it) } }.orEmpty(),submission=optional("submission")?.let { moshi.adapter(CreateEventRequest::class.java).fromJson(it) },discardedUploadIds=d.optJSONArray("discarded")?.let {a->(0 until a.length()).map {a.getString(it)}.toSet()}.orEmpty(),draftRestored=true)
    }
    @Synchronized fun clear(owner: Long,id: Long?) { AtomicFile(file(owner,id)).delete() }
}
