package com.evmap.mobileapp.feature.events.ui

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.dto.*
import com.evmap.mobileapp.core.data.media.ImageMultipart
import com.evmap.mobileapp.feature.events.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import retrofit2.HttpException
import java.time.*
import java.util.UUID
import javax.inject.Inject

data class PendingEventPhoto(val uri: Uri,val uploadId: String=UUID.randomUUID().toString(),val error: String?=null)
data class EventFormState(
    val editing: Boolean,val loading: Boolean=true,val ready: Boolean=false,val saving: Boolean=false,
    val title: String="",val description: String="",val startsAt: Instant?=null,val endsAt: Instant?=null,
    val dateEnabled: Boolean=false,val timeEnabled: Boolean=false,val timeZone: String=ZoneId.systemDefault().id,
    val locationId: Long?=null,val locationName: String="",val categories: List<EventCategoryDto> = emptyList(),val categoryIds: Set<Long> = emptySet(),
    val tagsText: String="",val tagSuggestions: List<String> = emptyList(),val existingPhotos: List<EventPhotoDto> = emptyList(),val pendingPhotos: List<PendingEventPhoto> = emptyList(),val photosToDelete: Set<Long> = emptySet(),val photoOrder: List<String> = emptyList(),
    val locationQuery: String="",val locationResults: List<LocationDto> = emptyList(),val locationSearching: Boolean=false,val locationError: String?=null,
    val error: String?=null,val notice: String?=null,val fieldErrors: Map<String,String> = emptyMap(),val savedEventId: Long?=null,val finishedEventId: Long?=null,
    val creationUnconfirmed: Boolean=false,val clientRequestId: String=UUID.randomUUID().toString(),val submission: CreateEventRequest?=null,val draftRestored: Boolean=false,val preparingPhotos: Boolean=false,val discardedUploadIds: Set<String> = emptySet(),
)

@HiltViewModel
class EventFormViewModel @Inject constructor(savedStateHandle: SavedStateHandle,private val events: EventsRepository,private val locations: LocationRepository,private val photoUploader: ImageMultipart,private val drafts: EventDraftStore): ViewModel() {
    private val editingId=savedStateHandle.get<Long>("eventId")
    private val owner=drafts.owner()
    private val _state=MutableStateFlow(EventFormState(editing=editingId!=null))
    val state=_state.asStateFlow()
    private var loadJob: Job?=null
    private var tagsJob: Job?=null
    @OptIn(FlowPreview::class)
    private fun observeDraft()=viewModelScope.launch {
        state.debounce(400).collect { snapshot ->
            if(snapshot.ready && snapshot.finishedEventId==null) runCatching { withContext(Dispatchers.IO) { drafts.write(owner,editingId,snapshot) } }.onFailure { _state.update { it.copy(error="Не вдалося зберегти чернетку на телефоні") } }
        }
    }
    private var draftJob=observeDraft()
    init { load() }
    fun load() {
        if(loadJob?.isActive==true) return
        loadJob=viewModelScope.launch {
            _state.update { it.copy(loading=true,error=null) }
            try {
                val event=editingId?.let { events.getEvent(it).getOrThrow() }
                val categories=events.getCategories().getOrThrow()
                val photos=editingId?.let { events.getPhotos(it).getOrThrow() }.orEmpty()
                val zone=event?.timeZone ?: ZoneId.systemDefault().id
                val base=EventFormState(editing=editingId!=null,loading=false,ready=true,title=event?.name.orEmpty(),description=event?.description.orEmpty(),
                    startsAt=event?.startsOn?.let { LocalDate.parse(it).atStartOfDay(ZoneId.of(zone)).toInstant() } ?: event?.startsAt?.let(Instant::parse),
                    endsAt=if(event?.dateMode=="DATE") event.endsOn?.let { LocalDate.parse(it).atStartOfDay(ZoneId.of(zone)).toInstant() } else event?.endsAt?.let(Instant::parse),
                    dateEnabled=event!=null && event.dateMode!="UNSPECIFIED",timeEnabled=event?.dateMode=="DATETIME",timeZone=zone,
                    locationId=event?.locationId,locationName=event?.locationName.orEmpty(),categories=(categories+event?.categories.orEmpty()).distinctBy { it.id },categoryIds=event?.categories.orEmpty().map { it.id }.toSet(),existingPhotos=photos,photoOrder=photos.map { "s:${it.id}" },tagsText=event?.tags.orEmpty().joinToString(", "))
                _state.value=withContext(Dispatchers.IO) { runCatching { drafts.restore(owner,editingId,base) }.getOrDefault(base) }
            } catch(ex: CancellationException) { throw ex } catch(ex: Exception) { _state.update { it.copy(loading=false,error=ex.eventError()) } }
        }
    }
    private fun edit(block: (EventFormState)->EventFormState) { _state.update { if(it.saving || it.savedEventId!=null || it.submission!=null) it else block(it).copy(error=null,fieldErrors=emptyMap()) } }
    fun updateTitle(value: String)=edit { it.copy(title=value) }
    fun updateDescription(value: String)=edit { it.copy(description=value) }
    fun updateTags(value: String) {
        edit {it.copy(tagsText=value)}
        tagsJob?.cancel()
        tagsJob=viewModelScope.launch {
            delay(250)
            try { val selected=value.split(',').map {it.trim().lowercase()};val suggestions=locations.api.tags(value.substringAfterLast(',').trim()).map {it.name}.filterNot {it.lowercase() in selected};_state.update {it.copy(tagSuggestions=suggestions)} }
            catch(ex: CancellationException) {throw ex} catch(_: Exception) {_state.update {it.copy(tagSuggestions=emptyList())}}
        }
    }
    fun chooseTag(name: String) { val parts=state.value.tagsText.split(',').dropLast(1).map(String::trim).filter(String::isNotEmpty);updateTags((parts+name).distinctBy {it.lowercase()}.joinToString(", ")+", ") }

    fun setDateEnabled(value: Boolean)=edit { it.copy(dateEnabled=value) }
    fun setTimeEnabled(value: Boolean)=edit { it.copy(timeEnabled=value) }
    fun updateTimeZone(value: String)=edit { s->
        val previous=ZoneId.of(s.timeZone);val next=ZoneId.of(value)
        s.copy(timeZone=value,startsAt=s.startsAt?.atZone(previous)?.toLocalDateTime()?.atZone(next)?.toInstant(),endsAt=s.endsAt?.atZone(previous)?.toLocalDateTime()?.atZone(next)?.toInstant())
    }
    fun updateStartDate(value: LocalDate)=edit { it.copy(startsAt=onDate(it.startsAt,value,it.timeZone)) }
    fun updateEndDate(value: LocalDate)=edit { it.copy(endsAt=onDate(it.endsAt ?: it.startsAt,value,it.timeZone)) }
    fun updateTime(end: Boolean,hour: Int,minute: Int)=edit { s ->
        val current=if(end) s.endsAt ?: s.startsAt else s.startsAt
        val updated=(current ?: LocalDate.now().atStartOfDay(ZoneId.of(s.timeZone)).toInstant()).atZone(ZoneId.of(s.timeZone)).withHour(hour).withMinute(minute).withSecond(0).withNano(0).toInstant()
        if(end) s.copy(endsAt=updated) else s.copy(startsAt=updated)
    }
    fun clearEndDate()=edit { it.copy(endsAt=null) }
    fun toggleCategory(id: Long)=edit { it.copy(categoryIds=if(id in it.categoryIds) it.categoryIds-id else it.categoryIds+id) }
    fun updateLocationQuery(value: String) { _state.update { it.copy(locationQuery=value) } }
    fun searchLocation(query: String=state.value.locationQuery) {
        if(state.value.locationSearching) return
        _state.update { it.copy(locationSearching=true,locationError=null) }
        viewModelScope.launch { try { val items=locations.api.search(query.trim());_state.update { it.copy(locationResults=items,locationSearching=false) } } catch(ex: CancellationException) { throw ex } catch(ex: Exception) { _state.update { it.copy(locationSearching=false,locationError=ex.eventError()) } } }
    }
    fun selectLocation(item: LocationDto,onSelected: ()->Unit = {}) {
        viewModelScope.launch { try { val selected=locations.select(item);edit { it.copy(locationId=selected.id,locationName=selected.name) };onSelected() } catch(ex: CancellationException) { throw ex } catch(ex: Exception) { _state.update { it.copy(error=ex.eventError()) } } }
    }
    suspend fun createLocation(request: CreateLocationRequest): LocationDto = locations.api.create(request)
    fun proposeCategory(name: String) { viewModelScope.launch { try { locations.api.propose(mapOf("name" to name.trim()));_state.update { it.copy(error=null,notice="Пропозицію надіслано адміністратору. Поки виберіть «Інше» та додайте тег") } } catch(ex: CancellationException) { throw ex } catch(ex: Exception) { _state.update { it.copy(error=ex.eventError()) } } } }
    fun addPhotos(uris: List<Uri>) {
        if(state.value.preparingPhotos || state.value.saving) return
        val count=state.value.existingPhotos.count { it.id !in state.value.photosToDelete }+state.value.pendingPhotos.size
        if(count+uris.size>10) { _state.update { it.copy(error="Можна додати до 10 фотографій") };return }
        _state.update { it.copy(preparingPhotos=true) }
        viewModelScope.launch {
            try { for(uri in uris.distinct()) { val id=UUID.randomUUID().toString();val prepared=photoUploader.prepare(uri,owner,id);_state.update { it.copy(pendingPhotos=it.pendingPhotos+PendingEventPhoto(prepared,id),photoOrder=it.photoOrder+"p:$id") } } }
            catch(ex: CancellationException) { throw ex } catch(ex: Exception) { _state.update { it.copy(error=ex.eventError()) } }
            finally { _state.update { it.copy(preparingPhotos=false) } }
        }
    }
    fun removePendingPhoto(id: String) { val photo=state.value.pendingPhotos.find { it.uploadId==id };_state.update { it.copy(pendingPhotos=it.pendingPhotos.filterNot { it.uploadId==id },photoOrder=it.photoOrder-"p:$id",discardedUploadIds=it.discardedUploadIds+id) };photo?.let { photoUploader.removePrepared(it.uri,owner) } }
    fun removeExistingPhoto(id: Long) { _state.update { it.copy(photosToDelete=it.photosToDelete+id,photoOrder=it.photoOrder-"s:$id") } }
    fun undoPhotoRemovals() { _state.update { s->s.copy(photosToDelete=emptySet(),photoOrder=s.photoOrder+s.existingPhotos.filter { "s:${it.id}" !in s.photoOrder }.map { "s:${it.id}" }) } }
    fun movePhoto(key: String,first: Boolean=false,delta: Int=0) { _state.update { s ->if(s.saving) s else { val order=s.photoOrder.toMutableList();val from=order.indexOf(key);if(from<0) s else { order.removeAt(from);order.add(if(first) 0 else (from+delta).coerceIn(0,order.size),key);s.copy(photoOrder=order) } } } }
    suspend fun confirmLocation(item: LocationDto) { val selected=locations.select(item);edit {it.copy(locationId=selected.id,locationName=selected.name)} }
    fun leave(onDone: ()->Unit) {
        if(state.value.saving || state.value.preparingPhotos) return
        viewModelScope.launch {
            try { if(state.value.ready) withContext(Dispatchers.IO) {drafts.write(owner,editingId,state.value)};onDone() }
            catch(ex: CancellationException) {throw ex} catch(ex: Exception) {_state.update {it.copy(error="Не вдалося зберегти чернетку")}}
        }
    }
    fun deleteDraft() { viewModelScope.launch { draftJob.cancelAndJoin();withContext(Dispatchers.IO) { drafts.clear(owner,editingId) };state.value.pendingPhotos.forEach { photoUploader.removePrepared(it.uri,owner) };_state.value=EventFormState(editing=editingId!=null);draftJob=observeDraft();load() } }
    fun save() {
        val snapshot=state.value
        if(!snapshot.ready || snapshot.saving || snapshot.preparingPhotos) return
        val errors=if(snapshot.savedEventId==null && snapshot.submission==null) validateEventForm(snapshot) else emptyMap()
        if(errors.isNotEmpty()) { _state.update { it.copy(fieldErrors=errors,error="Перевірте позначені поля") };return }
        val request=snapshot.submission ?: if(snapshot.savedEventId==null) snapshot.toRequest() else null
        _state.update { it.copy(saving=true,error=null,submission=request) }
        viewModelScope.launch {
            try {
                // Persist the immutable request and key before the first network attempt.
                withContext(Dispatchers.IO) { drafts.write(owner,editingId,state.value) }
                val eventId=snapshot.savedEventId ?: if(editingId!=null) { events.updateEvent(editingId,request!!).getOrThrow();editingId } else events.createEvent(request!!).getOrThrow()
                _state.update { it.copy(savedEventId=eventId) }
                withContext(Dispatchers.IO) { drafts.write(owner,editingId,state.value) }
                val remote=events.getPhotos(eventId).getOrThrow()
                val uploaded=state.value.pendingPhotos.mapNotNull { pending -> remote.find { it.imagePath.contains("/${pending.uploadId}-") }?.let {pending to it} }
                _state.update { current ->
                    val replacements=uploaded.associate { "p:${it.first.uploadId}" to "s:${it.second.id}" }
                    val discarded=remote.filter { photo -> current.discardedUploadIds.any {photo.imagePath.contains("/$it-")} }.map {it.id}.toSet()
                    val retained=current.photoOrder.map {replacements[it] ?: it}.filter { key -> key.startsWith("p:") || remote.any {"s:${it.id}"==key} }
                    val extras=remote.filter {"s:${it.id}" !in retained && it.id !in current.photosToDelete && it.id !in discarded}.map {"s:${it.id}"}
                    current.copy(existingPhotos=remote,pendingPhotos=current.pendingPhotos.filterNot { pending->uploaded.any {it.first.uploadId==pending.uploadId} },photoOrder=retained+extras,photosToDelete=current.photosToDelete+discarded)
                }
                uploaded.forEach {photoUploader.removePrepared(it.first.uri,owner)}
                for(id in state.value.photosToDelete) { events.deletePhoto(eventId,id).getOrThrow();_state.update { it.copy(existingPhotos=it.existingPhotos.filterNot { it.id==id },photosToDelete=it.photosToDelete-id) } }
                for(photo in state.value.pendingPhotos) {
                    val uploaded=events.uploadPhoto(eventId,photo.uploadId,photoUploader.filePart(photo.uri)).getOrThrow()
                    _state.update { it.copy(existingPhotos=(it.existingPhotos+uploaded).distinctBy { it.id },pendingPhotos=it.pendingPhotos.filterNot { it.uploadId==photo.uploadId },photoOrder=it.photoOrder.map { key->if(key=="p:${photo.uploadId}") "s:${uploaded.id}" else key }) }
                    withContext(Dispatchers.IO) { drafts.write(owner,editingId,state.value) }
                    photoUploader.removePrepared(photo.uri,owner)
                }
                val ids=state.value.photoOrder.mapNotNull { it.removePrefix("s:").toLongOrNull() }
                events.orderPhotos(eventId,ids).getOrThrow()
                draftJob.cancelAndJoin();withContext(Dispatchers.IO) { drafts.clear(owner,editingId) }
                _state.update { it.copy(saving=false,finishedEventId=eventId) }
            } catch(ex: CancellationException) { throw ex } catch(ex: Exception) {
                val http=ex as? HttpException
                val fields=runCatching { JSONObject(http?.response()?.errorBody()?.string().orEmpty()).optJSONObject("fieldErrors") }.getOrNull()
                val errors=fields?.keys()?.asSequence()?.associateWith { fields.optString(it) }.orEmpty()
                _state.update { it.copy(saving=false,error=ex.eventError(),fieldErrors=errors,submission=if(http?.code()==400 && it.savedEventId==null) null else it.submission) }
            } finally { _state.update { it.copy(saving=false) } }
        }
    }
    fun finishWithSavedPhotos() { state.value.savedEventId?.let { id->_state.update { it.copy(finishedEventId=id) } } }
}

internal fun validateEventForm(s: EventFormState): Map<String,String> = buildMap {
    if(s.title.trim().length !in 1..100) put("name","Укажіть назву до 100 символів")
    if(s.description.trim().length !in 1..2000) put("description","Укажіть опис до 2000 символів")
    if(s.locationId==null) put("locationId","Виберіть місце проведення")
    if(s.categoryIds.size !in 1..3) put("categoryIds","Виберіть від 1 до 3 категорій")
    val zone=runCatching { ZoneId.of(s.timeZone) }.getOrNull()
    if(zone==null) put("timeZone","Виберіть коректний часовий пояс")
    if(s.dateEnabled && s.startsAt==null) put("startsAt","Виберіть дату початку")
    if(s.dateEnabled && s.startsAt!=null && s.endsAt!=null && (if(s.timeEnabled || zone==null) s.endsAt.isBefore(s.startsAt) else s.endsAt.atZone(zone).toLocalDate().isBefore(s.startsAt.atZone(zone).toLocalDate()))) put("endsAt","Завершення не може бути раніше початку")
    val tags=s.tagsText.split(',').map(String::trim).filter(String::isNotEmpty)
    if(tags.size>5 || tags.any { it.length>30 }) put("tags","До 5 тегів, кожен до 30 символів")
}
internal fun EventFormState.toRequest(): CreateEventRequest {
    val errors=validateEventForm(this);require(errors.isEmpty()) { errors.values.first() }
    val zone=ZoneId.of(timeZone)
    return CreateEventRequest(title.trim(),description.trim(),startsAt.takeIf { dateEnabled && timeEnabled }?.toString(),endsAt.takeIf { dateEnabled && timeEnabled }?.toString(),requireNotNull(locationId),categoryIds.toList().sorted(),
        dateMode=if(!dateEnabled) "UNSPECIFIED" else if(timeEnabled) "DATETIME" else "DATE",startsOn=startsAt.takeIf { dateEnabled && !timeEnabled }?.atZone(zone)?.toLocalDate()?.toString(),endsOn=endsAt.takeIf { dateEnabled && !timeEnabled }?.atZone(zone)?.toLocalDate()?.toString(),timeZone=timeZone,tags=tagsText.split(',').map(String::trim).filter(String::isNotEmpty).distinctBy { it.lowercase() },clientRequestId=clientRequestId)
}
private fun onDate(previous: Instant?,date: LocalDate,zoneName: String): Instant {
    val zone=runCatching { ZoneId.of(zoneName) }.getOrDefault(ZoneId.systemDefault())
    return previous?.atZone(zone)?.with(date)?.toInstant() ?: date.atStartOfDay(zone).toInstant()
}
