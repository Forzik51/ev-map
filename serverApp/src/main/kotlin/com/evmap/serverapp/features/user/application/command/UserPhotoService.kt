package com.evmap.serverapp.features.user.application.command

import com.evmap.serverapp.features.user.api.dto.ViewUserPhoto
import com.evmap.serverapp.features.user.domian.UserRepositoryPort
import com.evmap.serverapp.shared.security.CurrentUserProvider
import com.evmap.serverapp.shared.storage.FileStorageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class UserPhotoService(
    private val users: UserRepositoryPort,
    private val storage: FileStorageService,
    private val currentUser: CurrentUserProvider,
) {
    @Transactional
    fun create(userId: Long, file: MultipartFile): ViewUserPhoto = save(userId, file, createOnly = true)

    @Transactional
    fun replace(userId: Long, file: MultipartFile): ViewUserPhoto = save(userId, file, createOnly = false)

    @Transactional
    fun delete(userId: Long) {
        currentUser.requireSelfOrAdmin(userId)
        val path = users.lockUserAndFindPath(userId) ?: return
        users.updatePath(userId, null)
        storage.deleteAfterCommit(listOf(path))
    }

    private fun save(userId: Long, file: MultipartFile, createOnly: Boolean): ViewUserPhoto {
        currentUser.requireSelfOrAdmin(userId)
        val currentPath = users.lockUserAndFindPath(userId)
        check(!createOnly || currentPath.isNullOrBlank()) { "A profile photo already exists. Use PUT to replace it." }

        val path = storage.saveUserProfilePhoto(userId, file)
        storage.deleteAfterRollback(path)
        users.updatePath(userId, path)
        currentPath?.let { storage.deleteAfterCommit(listOf(it)) }
        return ViewUserPhoto(userId = userId, path = path)
    }
}
