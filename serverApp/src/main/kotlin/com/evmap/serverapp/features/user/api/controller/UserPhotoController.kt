package com.evmap.serverapp.features.user.api.controller

import com.evmap.serverapp.features.user.api.dto.ViewUserPhoto
import com.evmap.serverapp.features.user.application.UserPhotoNotFoundException
import com.evmap.serverapp.features.user.application.command.UserPhotoService
import com.evmap.serverapp.features.user.domian.UserRepositoryPort
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
@RequestMapping("/api/users/{userId}/photo")
class UserPhotoController(
    private val userRepository: UserRepositoryPort,
    private val photos: UserPhotoService,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable userId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ViewUserPhoto = photos.create(userId, file)

    @PutMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun update(
        @PathVariable userId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ViewUserPhoto = photos.replace(userId, file)

    @GetMapping
    fun get(@PathVariable userId: Long): ViewUserPhoto {
        val storedPath = userRepository.findPathByUserId(userId)
            ?: throw UserPhotoNotFoundException(userId)
        return ViewUserPhoto(userId = userId, path = storedPath)
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable userId: Long) = photos.delete(userId)
}
