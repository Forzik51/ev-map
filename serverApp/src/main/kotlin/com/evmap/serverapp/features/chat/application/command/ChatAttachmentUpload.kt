package com.evmap.serverapp.features.chat.application.command

import com.evmap.serverapp.features.chat.domian.ChatAttachment
import org.springframework.web.multipart.MultipartFile

internal fun chatAttachment(file: MultipartFile, kind: String, durationMs: Long?): ChatAttachment {
    require(!file.isEmpty && file.size <= 5L * 1024 * 1024) { "Choose a non-empty file up to 5 MB" }
    require(durationMs == null || kind == "VOICE" && durationMs > 0) { "Invalid audio duration" }
    val bytes = file.bytes
    val contentType = when (kind) {
        "FILE" -> "application/octet-stream"
        "IMAGE" -> when {
            bytes.take(3) == listOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte()) -> "image/jpeg"
            bytes.take(8) == listOf<Byte>(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a) -> "image/png"
            bytes.size >= 12 && bytes.ascii(0, 4) == "RIFF" && bytes.ascii(8, 4) == "WEBP" -> "image/webp"
            else -> throw IllegalArgumentException("Choose a JPEG, PNG or WebP image")
        }
        "VOICE" -> {
            require(bytes.size >= 12 && bytes.ascii(4, 4) == "ftyp") { "Choose an M4A audio recording" }
            "audio/mp4"
        }
        else -> throw IllegalArgumentException("Unknown attachment type")
    }
    val name = file.originalFilename.orEmpty().substringAfterLast('/').substringAfterLast('\\')
        .filterNot { it.isISOControl() }.trim().take(160).ifBlank { "attachment" }
    return ChatAttachment(kind, name, contentType, bytes, durationMs)
}

private fun ByteArray.ascii(start: Int, length: Int) = String(this, start, length, Charsets.US_ASCII)
