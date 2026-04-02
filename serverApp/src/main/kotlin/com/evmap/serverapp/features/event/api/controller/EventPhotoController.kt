package com.evmap.serverapp.features.event.api.controller

import com.evmap.serverapp.features.event.api.dto.ViewEventPhoto
import com.evmap.serverapp.features.event.application.EventPhotoNotFoundException
import com.evmap.serverapp.features.event.infra.write.EventPhotoRepository
import com.evmap.serverapp.shared.storage.FileStorageService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/events/{eventId}/photos")
class EventPhotoController(
    private val eventPhotoRepository: EventPhotoRepository,
    private val fileStorageService: FileStorageService,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable eventId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ViewEventPhoto {
        val newPath = fileStorageService.saveEventPhoto(eventId, file)
        return try {
            eventPhotoRepository.create(eventId, newPath)
        } catch (ex: Exception) {
            fileStorageService.deleteByStoredPath(newPath)
            throw ex
        }
    }

    @GetMapping
    fun getAll(@PathVariable eventId: Long): List<ViewEventPhoto> =
        eventPhotoRepository.findAllByEventId(eventId)

    @GetMapping("/{photoId}")
    fun getById(
        @PathVariable eventId: Long,
        @PathVariable photoId: Long,
    ): ViewEventPhoto =
        eventPhotoRepository.findById(eventId, photoId)
            ?: throw EventPhotoNotFoundException(eventId, photoId)

    @PutMapping("/{photoId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun update(
        @PathVariable eventId: Long,
        @PathVariable photoId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ViewEventPhoto {
        val existing = eventPhotoRepository.findById(eventId, photoId)
            ?: throw EventPhotoNotFoundException(eventId, photoId)

        val newPath = fileStorageService.saveEventPhoto(eventId, file)
        return try {
            val updated = eventPhotoRepository.updatePath(eventId, photoId, newPath)
                ?: throw EventPhotoNotFoundException(eventId, photoId)
            fileStorageService.deleteByStoredPath(existing.imagePath)
            updated
        } catch (ex: Exception) {
            fileStorageService.deleteByStoredPath(newPath)
            throw ex
        }
    }

    @DeleteMapping("/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable eventId: Long,
        @PathVariable photoId: Long,
    ) {
        val removedPath = eventPhotoRepository.delete(eventId, photoId)
            ?: throw EventPhotoNotFoundException(eventId, photoId)
        fileStorageService.deleteByStoredPath(removedPath)
    }
}
