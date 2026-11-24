package com.evmap.serverapp.shared.storage

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

@Service
class FileStorageService(
    @Value("\${app.media.root:./storage/media}")
    mediaRoot: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val rootPath: Path = Paths.get(mediaRoot).toAbsolutePath().normalize()

    private val allowedContentTypes = setOf(
        "image/jpeg",
        "image/png",
        "image/webp",
    )
    private val maxFileSizeBytes = 5L * 1024 * 1024

    init {
        Files.createDirectories(rootPath)
    }

    fun saveUserProfilePhoto(userId: Long, file: MultipartFile): String {
        validateImage(file)
        val ext = validateImageContent(file)
        val relativePath = "users/$userId/profile/${UUID.randomUUID()}.$ext"
        write(file, relativePath)
        return toPublicPath(relativePath)
    }

    fun saveEventPhoto(eventId: Long, file: MultipartFile, uploadId: UUID? = null): String {
        validateImage(file)
        val ext = validateImageContent(file)
        val fileId = UUID.randomUUID()
        val fileName = if (uploadId == null) "$fileId.$ext" else "$uploadId-$fileId.$ext"
        val relativePath = "events/$eventId/$fileName"
        write(file, relativePath)
        return toPublicPath(relativePath)
    }

    fun deleteByStoredPath(storedPath: String?) {
        if (storedPath.isNullOrBlank() || "://" in storedPath) return

        val relativePath = normalizeStoredPath(storedPath)
        runCatching {
            val target = rootPath.resolve(relativePath).normalize()
            if (target != rootPath && target.startsWith(rootPath)) {
                Files.deleteIfExists(target)
                cleanupEmptyParents(target.parent)
            }
        }.onFailure { log.warn("Could not remove stored media at {}", relativePath, it) }
    }

    fun deleteAfterCommit(storedPaths: List<String>) {
        if (storedPaths.isEmpty()) return
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                storedPaths.forEach(::deleteByStoredPath)
            }
        })
    }

    fun deleteAfterRollback(storedPath: String) {
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCompletion(status: Int) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) deleteByStoredPath(storedPath)
            }
        })
    }

    private fun validateImageContent(file: MultipartFile): String {
        val header = file.inputStream.use { it.readNBytes(12) }
        val contentType = when {
            header.take(3) == listOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte()) -> "image/jpeg"
            header.take(8) == listOf<Byte>(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a) -> "image/png"
            header.size == 12 && String(header, 0, 4, Charsets.US_ASCII) == "RIFF" &&
                String(header, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp"
            else -> throw IllegalArgumentException("Choose a JPEG, PNG or WebP image")
        }
        require(file.contentType.equals(contentType, ignoreCase = true)) { "Image content does not match its format" }
        return when (contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            else -> "webp"
        }
    }

    private fun validateImage(file: MultipartFile) {
        if (file.isEmpty) {
            throw IllegalArgumentException("File is empty")
        }
        if (file.size > maxFileSizeBytes) {
            throw IllegalArgumentException("File is too large. Max size is ${maxFileSizeBytes / (1024 * 1024)} MB")
        }

        val contentType = file.contentType?.lowercase()
            ?: throw IllegalArgumentException("Missing content type")
        if (contentType !in allowedContentTypes) {
            throw IllegalArgumentException("Unsupported content type: $contentType")
        }
    }

    private fun write(file: MultipartFile, relativePath: String) {
        val target = rootPath.resolve(relativePath).normalize()
        if (!target.startsWith(rootPath)) {
            throw IllegalArgumentException("Invalid destination path")
        }

        try {
            Files.createDirectories(target.parent)
            file.inputStream.use { input ->
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (ex: IOException) {
            runCatching { Files.deleteIfExists(target) }
            throw IllegalStateException("Failed to store file", ex)
        }
    }

    private fun toPublicPath(relativePath: String): String = "/media/$relativePath".replace('\\', '/')

    private fun normalizeStoredPath(storedPath: String): String {
        val normalized = storedPath.trim().replace('\\', '/').removePrefix("/")
        return if (normalized.startsWith("media/")) normalized.removePrefix("media/") else normalized
    }

    private fun cleanupEmptyParents(start: Path?) {
        var current = start
        while (current != null && current != rootPath && current.startsWith(rootPath)) {
            if (!Files.isDirectory(current)) break
            if (!isDirectoryEmpty(current)) break
            runCatching { Files.deleteIfExists(current) }
            current = current.parent
        }
    }

    private fun isDirectoryEmpty(path: Path): Boolean =
        Files.list(path).use { stream -> !stream.findAny().isPresent }
}
