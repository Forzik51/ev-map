package com.evmap.serverapp.features.event.application.command

import com.evmap.serverapp.features.event.api.dto.ViewEventPhoto
import com.evmap.serverapp.features.event.application.EventPhotoNotFoundException
import com.evmap.serverapp.features.event.infra.write.EventPhotoRepository
import com.evmap.serverapp.shared.security.CurrentUserProvider
import com.evmap.serverapp.shared.storage.FileStorageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class EventPhotoService(
    private val photos: EventPhotoRepository,
    private val storage: FileStorageService,
    private val currentUser: CurrentUserProvider,
) {
    @Transactional
    fun create(eventId: Long, file: MultipartFile, uploadId: UUID?): ViewEventPhoto {
        currentUser.requireSelfOrAdmin(photos.lockEventOwner(eventId))
        if (uploadId != null) photos.findByUploadId(eventId, uploadId)?.let { return it }
        require(photos.findAllByEventId(eventId).size < 10) { "Дозволено до 10 фотографій" }

        val path = storage.saveEventPhoto(eventId, file, uploadId)
        storage.deleteAfterRollback(path)
        return photos.create(eventId, path)
    }

    @Transactional
    fun replace(eventId: Long, photoId: Long, file: MultipartFile): ViewEventPhoto {
        currentUser.requireSelfOrAdmin(photos.lockEventOwner(eventId))
        val existing = photos.findById(eventId, photoId) ?: throw EventPhotoNotFoundException(eventId, photoId)
        val newPath = storage.saveEventPhoto(eventId, file)
        storage.deleteAfterRollback(newPath)
        val updated = photos.updatePath(eventId, photoId, newPath) ?: throw EventPhotoNotFoundException(eventId, photoId)
        storage.deleteAfterCommit(listOf(existing.imagePath))
        return updated
    }

    @Transactional
    fun delete(eventId: Long, photoId: Long) {
        currentUser.requireSelfOrAdmin(photos.lockEventOwner(eventId))
        val path = photos.delete(eventId, photoId) ?: throw EventPhotoNotFoundException(eventId, photoId)
        storage.deleteAfterCommit(listOf(path))
    }

    @Transactional
    fun order(eventId: Long, ids: List<Long>) {
        currentUser.requireSelfOrAdmin(photos.lockEventOwner(eventId))
        val existing = photos.findAllByEventId(eventId).map { it.id }.toSet()
        require(ids.size == ids.distinct().size && ids.toSet() == existing) { "Порядок має містити всі фотографії події без повторів" }
        photos.order(eventId, ids)
    }
}
