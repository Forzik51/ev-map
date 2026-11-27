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
    private val mediaRoot = java.io.File(context.filesDir,"event-draft-media")
    private fun type(uri: Uri) = resolver.getType(uri)?.lowercase() ?: when(uri.lastPathSegment?.substringAfterLast('.')) { "jpg","jpeg"->"image/jpeg";"png"->"image/png";"webp"->"image/webp";else->"" }

    suspend fun prepare(uri: Uri,owner: Long,id: String): Uri = withContext(Dispatchers.IO) {
        val mime=type(uri)
        val extension=when(mime) { "image/jpeg"->"jpg";"image/png"->"png";"image/webp"->"webp";else->throw IllegalArgumentException("Виберіть JPEG, PNG або WebP") }
        val bytes=resolver.openInputStream(uri)?.use { it.readBytesLimited() } ?: throw IllegalArgumentException("Фотографія недоступна")
        val options=android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds=true }
        android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
        require(options.outWidth>0 && options.outHeight>0) { "Файл не є коректним зображенням" }
        val target=java.io.File(mediaRoot,"$owner/$id.$extension");target.parentFile!!.mkdirs();target.writeBytes(bytes)
        Uri.fromFile(target)
    }
    private fun java.io.InputStream.readBytesLimited(): ByteArray {
        val output=ByteArrayOutputStream();val buffer=ByteArray(8192)
        while(true) { val count=read(buffer);if(count<0) break;require(output.size()+count<=MAX_PHOTO_BYTES) { "Фотографія має бути до 5 МБ" };output.write(buffer,0,count) }
        return output.toByteArray().also { require(it.isNotEmpty()) { "Файл порожній" } }
    }
    fun removePrepared(uri: Uri,owner: Long) {
        if(uri.scheme!="file") return
        val target=java.io.File(uri.path.orEmpty()).canonicalFile
        val parent=java.io.File(mediaRoot,owner.toString()).canonicalFile
        if(target.parentFile==parent) target.delete()
    }

    suspend fun filePart(uri: Uri): MultipartBody.Part = withContext(Dispatchers.IO) {
        val contentType = type(uri)
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
