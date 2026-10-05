package com.nuvio.tv.core.danexus

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import tv.seekr.previews.core.SeekrTile
import java.util.concurrent.TimeUnit

/** A bounded neighborhood cache: at most 8 MiB of compressed sheets and twelve small RGB tiles. */
class DanexusSeekPreviewImages {
    private val sheets = LinkedHashMap<String, ByteArray>(2, 0.75f, true)
    private val frames = LinkedHashMap<String, Bitmap>(12, 0.75f, true)
    private val mutex = Mutex()
    suspend fun image(tile: SeekrTile): Bitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!DanexusSeekPreviewPolicy.trustedTile(tile.sheetUrl)) return@withLock null
            if (tile.w !in 1..1024 || tile.h !in 1..576 || tile.x < 0 || tile.y < 0) return@withLock null
            val frameKey = listOf(tile.sheetUrl, tile.x, tile.y, tile.w, tile.h)
                .joinToString("#")
            frames[frameKey]?.let { return@withLock it }
            if (tile.sheetUrl !in sheets) {
                val bytes = runCatching {
                    http.newCall(Request.Builder().url(tile.sheetUrl).build()).execute().use { response ->
                        if (!response.isSuccessful) return@use null
                        val body = response.body ?: return@use null
                        if (body.contentLength() > MAX_BYTES) return@use null
                        body.byteStream().use { stream ->
                            val output = java.io.ByteArrayOutputStream()
                            val chunk = ByteArray(8192)
                            while (true) {
                                val count = stream.read(chunk); if (count < 0) break
                                if (output.size() + count > MAX_BYTES) return@use null
                                output.write(chunk, 0, count)
                            }
                            output.toByteArray()
                        }
                    }
                }.getOrNull() ?: return@withLock null
                while (sheets.isNotEmpty() &&
                    (sheets.size >= 2 || sheets.values.sumOf { it.size } + bytes.size > MAX_BYTES)) {
                    sheets.remove(sheets.keys.first())
                }
                sheets[tile.sheetUrl] = bytes
            }
            val bytes = sheets[tile.sheetUrl] ?: return@withLock null
            val bitmap = runCatching {
                @Suppress("DEPRECATION")
                val decoder = BitmapRegionDecoder.newInstance(bytes, 0, bytes.size, false) ?: return@runCatching null
                try {
                    if (tile.x.toLong() + tile.w > decoder.width || tile.y.toLong() + tile.h > decoder.height) return@runCatching null
                    var sample = 1
                    while (tile.w / sample > 384 || tile.h / sample > 216) sample *= 2
                    decoder.decodeRegion(Rect(tile.x, tile.y, tile.x + tile.w, tile.y + tile.h),
                        BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.RGB_565
                            inSampleSize = sample
                        })
                } finally { decoder.recycle() }
            }.getOrNull()
            if (bitmap != null) {
                frames[frameKey] = bitmap
                while (frames.size > 12) frames.remove(frames.keys.first())
            }
            bitmap
        }
    }
    companion object {
        private const val MAX_BYTES = 8 * 1024 * 1024
        val http = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()
    }
}
