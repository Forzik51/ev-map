package com.evmap.serverapp.features.event.api.controller

import com.evmap.serverapp.features.event.api.dto.ViewEventPhoto
import com.evmap.serverapp.features.event.application.EventPhotoNotFoundException
import com.evmap.serverapp.features.event.application.command.EventPhotoService
import com.evmap.serverapp.features.event.infra.write.EventPhotoRepository
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/events/{eventId}/photos")
class EventPhotoController(
    private val eventPhotoRepository: EventPhotoRepository,
    private val eventPhotos: EventPhotoService,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable eventId: Long,
        @RequestPart("file") file: MultipartFile,
        @RequestParam(required = false) uploadId: UUID?,
    ): ViewEventPhoto = eventPhotos.create(eventId, file, uploadId)

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
    ): ViewEventPhoto = eventPhotos.replace(eventId, photoId, file)

    @DeleteMapping("/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable eventId: Long,
        @PathVariable photoId: Long,
    ) = eventPhotos.delete(eventId, photoId)
}
