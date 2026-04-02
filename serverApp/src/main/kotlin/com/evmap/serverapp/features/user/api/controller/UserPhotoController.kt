package com.evmap.serverapp.features.user.api.controller

import com.evmap.serverapp.features.user.api.dto.ViewUserPhoto
import com.evmap.serverapp.features.user.application.UserPhotoNotFoundException
import com.evmap.serverapp.features.user.domian.UserRepositoryPort
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
@RequestMapping("/api/users/{userId}/photo")
class UserPhotoController(
    private val userRepository: UserRepositoryPort,
    private val fileStorageService: FileStorageService,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable userId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ViewUserPhoto {
        val existing = userRepository.findPathByUserId(userId)
        if (!existing.isNullOrBlank()) {
            throw IllegalStateException("Photo for user $userId already exists. Use PUT to replace it.")
        }

        val storedPath = fileStorageService.saveUserProfilePhoto(userId, file)
        userRepository.updatePath(userId, storedPath)
        return ViewUserPhoto(userId = userId, path = storedPath)
    }

    @PutMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun update(
        @PathVariable userId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ViewUserPhoto {
        val currentPath = userRepository.findPathByUserId(userId)
        val newPath = fileStorageService.saveUserProfilePhoto(userId, file)

        try {
            userRepository.updatePath(userId, newPath)
            fileStorageService.deleteByStoredPath(currentPath)
            return ViewUserPhoto(userId = userId, path = newPath)
        } catch (ex: Exception) {
            fileStorageService.deleteByStoredPath(newPath)
            throw ex
        }
    }

    @GetMapping
    fun get(@PathVariable userId: Long): ViewUserPhoto {
        val storedPath = userRepository.findPathByUserId(userId)
            ?: throw UserPhotoNotFoundException(userId)
        return ViewUserPhoto(userId = userId, path = storedPath)
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable userId: Long) {
        val storedPath = userRepository.findPathByUserId(userId)
            ?: throw UserPhotoNotFoundException(userId)
        fileStorageService.deleteByStoredPath(storedPath)
        userRepository.updatePath(userId, null)
    }
}
