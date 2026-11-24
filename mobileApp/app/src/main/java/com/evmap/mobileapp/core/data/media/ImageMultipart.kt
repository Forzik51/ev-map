package com.evmap.mobileapp.core.data.media

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class ImageMultipart @Inject constructor(@ApplicationContext context: Context) {
    private val resolver = context.contentResolver

    suspend fun filePart(uri: Uri): MultipartBody.Part = withContext(Dispatchers.IO) {
        val contentType = resolver.getType(uri).orEmpty().lowercase()
        val extension = when (contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> throw IllegalArgumentException("Choose a JPEG, PNG or WebP photo.")
        }
        val bytes = resolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                require(output.size() + count <= MAX_PHOTO_BYTES) { "Choose a photo up to 5 MB." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: throw IllegalArgumentException("This photo is unavailable. Select it again.")
        require(bytes.isNotEmpty()) { "This photo is empty." }
        MultipartBody.Part.createFormData("file", "photo.$extension", bytes.toRequestBody(contentType.toMediaType()))
    }

    private companion object {
        const val MAX_PHOTO_BYTES = 5 * 1024 * 1024
    }
}
