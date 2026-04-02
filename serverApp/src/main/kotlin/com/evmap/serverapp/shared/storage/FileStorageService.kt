package com.evmap.serverapp.shared.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
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
        val ext = resolveExtension(file)
        val relativePath = "users/$userId/profile/${UUID.randomUUID()}.$ext"
        write(file, relativePath)
        return toPublicPath(relativePath)
    }

    fun saveEventPhoto(eventId: Long, file: MultipartFile): String {
        validateImage(file)
        val ext = resolveExtension(file)
        val relativePath = "events/$eventId/${UUID.randomUUID()}.$ext"
        write(file, relativePath)
        return toPublicPath(relativePath)
    }

    fun deleteByStoredPath(storedPath: String?) {
        if (storedPath.isNullOrBlank()) return

        val relativePath = normalizeStoredPath(storedPath)
        val target = rootPath.resolve(relativePath).normalize()
        if (!target.startsWith(rootPath)) return

        runCatching { Files.deleteIfExists(target) }
        cleanupEmptyParents(target.parent)
    }

    fun deleteAllByStoredPath(storedPaths: Iterable<String?>) {
        storedPaths.forEach { deleteByStoredPath(it) }
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

    private fun resolveExtension(file: MultipartFile): String {
        val byContentType = when (file.contentType?.lowercase()) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> null
        }

        val byName = file.originalFilename
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?.takeIf { it in setOf("jpg", "jpeg", "png", "webp") }
            ?.let { if (it == "jpeg") "jpg" else it }

        return byName ?: byContentType ?: "jpg"
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
