package com.evmap.mobileapp.feature.chat.data

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSink
import okio.source
import java.io.File
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

enum class ChatMediaKind { IMAGE, FILE, VOICE }

data class ChatMediaDraft(
    val file: File,
    val name: String,
    val contentType: String,
    val kind: ChatMediaKind,
    val durationMs: Long? = null,
    val clientId: String = UUID.randomUUID().toString(),
)

class ChatMedia @Inject constructor(@ApplicationContext private val context: Context) {
    private val resolver = context.contentResolver

    suspend fun prepare(uri: Uri, kind: ChatMediaKind): ChatMediaDraft = withContext(Dispatchers.IO) {
        val contentType = resolver.getType(uri) ?: "application/octet-stream"
        require(kind != ChatMediaKind.IMAGE || contentType in setOf("image/jpeg", "image/png", "image/webp")) {
            "Choose a JPEG, PNG or WebP image."
        }
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }.orEmpty().filterNot { it.isISOControl() }.take(160).ifBlank { "attachment" }
        val target = tempFile(".upload")
        try {
            val input = requireNotNull(resolver.openInputStream(uri)) { "This file is unavailable." }
            input.use { copyBounded(it, target) }
            ChatMediaDraft(target, name, contentType, kind)
        } catch (error: Exception) {
            target.delete()
            throw error
        }
    }

    fun multipart(media: ChatMediaDraft, progress: (Int) -> Unit): MultipartBody.Part {
        val size = media.file.length()
        val body = object : RequestBody() {
            override fun contentType() = media.contentType.toMediaType()
            override fun contentLength() = size
            override fun writeTo(sink: BufferedSink) {
                var written = 0L
                var reported = -1
                val buffer = Buffer()
                media.file.source().use { source ->
                    while (true) {
                        val count = source.read(buffer, 8192)
                        if (count == -1L) break
                        sink.write(buffer, count)
                        written += count
                        val percent = (written * 100 / size).toInt()
                        if (percent != reported) {
                            progress(percent)
                            reported = percent
                        }
                    }
                }
            }
        }
        return MultipartBody.Part.createFormData("file", media.name, body)
    }

    suspend fun saveFile(body: ResponseBody, uri: Uri) = withContext(Dispatchers.IO) {
        body.use { response ->
            requireNotNull(resolver.openOutputStream(uri, "wt")) { "Could not open the destination file." }.use { output ->
                response.byteStream().copyTo(output)
            }
        }
    }

    suspend fun cacheVoice(body: ResponseBody): File = withContext(Dispatchers.IO) {
        val target = tempFile(".m4a")
        try {
            body.use { copyBounded(it.byteStream(), target) }
            target
        } catch (error: Exception) {
            target.delete()
            throw error
        }
    }

    @Suppress("DEPRECATION")
    fun startRecording(onLimit: () -> Unit): ChatRecording {
        val target = tempFile(".m4a")
        val recorder = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(64_000)
            recorder.setOutputFile(target.absolutePath)
            recorder.setMaxFileSize(MAX_BYTES.toLong() - 65_536)
            recorder.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) onLimit()
            }
            recorder.prepare()
            recorder.start()
            return ChatRecording(recorder, target, SystemClock.elapsedRealtime())
        } catch (error: Exception) {
            recorder.release()
            target.delete()
            throw error
        }
    }

    private fun tempFile(suffix: String): File = File.createTempFile("chat-", suffix, context.cacheDir)

    private suspend fun copyBounded(input: InputStream, target: File) {
        target.outputStream().use { output ->
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count == -1) break
                total += count
                require(total <= MAX_BYTES) { "Choose a file up to 5 MB." }
                output.write(buffer, 0, count)
            }
            require(total > 0) { "This file is empty." }
        }
    }

    private companion object {
        const val MAX_BYTES = 5 * 1024 * 1024
    }
}

class ChatRecording(private val recorder: MediaRecorder, private val file: File, private val startedAt: Long) {
    val elapsedMs: Long get() = SystemClock.elapsedRealtime() - startedAt

    fun finish(): ChatMediaDraft {
        try {
            recorder.stop()
            require(file.length() > 0) { "The recording is empty." }
            return ChatMediaDraft(file, "Voice message.m4a", "audio/mp4", ChatMediaKind.VOICE, elapsedMs.coerceAtLeast(1))
        } catch (error: Exception) {
            file.delete()
            throw error
        } finally {
            recorder.release()
        }
    }

    fun cancel() {
        recorder.release()
        file.delete()
    }
}
