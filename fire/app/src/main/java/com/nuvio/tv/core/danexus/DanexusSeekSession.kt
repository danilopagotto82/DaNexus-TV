package com.nuvio.tv.core.danexus

/** Preview navigation never changes the decoder position until the viewer confirms. */
class DanexusSeekSession {
    private var originalPosition: Long? = null
    val active: Boolean get() = originalPosition != null

    fun begin(position: Long, pause: () -> Unit) {
        if (active) return
        originalPosition = position.coerceAtLeast(0L)
        pause()
    }

    fun confirm(position: Long, seek: (Long) -> Unit, resume: () -> Unit): Boolean {
        if (!active) return false
        seek(position.coerceAtLeast(0L))
        originalPosition = null
        resume()
        return true
    }

    fun cancel(): Long? = originalPosition.also { originalPosition = null }
    fun reset() { originalPosition = null }
}
