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

/** One compressed sheet, decoded only at the requested tile, for low-memory Fire TV. */
class DanexusSeekPreviewImages {
    private var sheetUrl: String? = null
    private var sheetBytes: ByteArray? = null
    private val mutex = Mutex()
    suspend fun image(tile: SeekrTile): Bitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!DanexusSeekPreviewPolicy.trustedTile(tile.sheetUrl)) return@withLock null
            if (tile.w !in 1..1024 || tile.h !in 1..576 || tile.x < 0 || tile.y < 0) return@withLock null
            if (sheetUrl != tile.sheetUrl) {
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
                sheetBytes = bytes; sheetUrl = tile.sheetUrl
            }
            val bytes = sheetBytes ?: return@withLock null
            runCatching {
                @Suppress("DEPRECATION")
                val decoder = BitmapRegionDecoder.newInstance(bytes, 0, bytes.size, false) ?: return@runCatching null
                try {
                    if (tile.x.toLong() + tile.w > decoder.width || tile.y.toLong() + tile.h > decoder.height) return@runCatching null
                    decoder.decodeRegion(Rect(tile.x, tile.y, tile.x + tile.w, tile.y + tile.h),
                        BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 })
                } finally { decoder.recycle() }
            }.getOrNull()
        }
    }
    companion object {
        private const val MAX_BYTES = 8 * 1024 * 1024
        val http = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()
    }
}
