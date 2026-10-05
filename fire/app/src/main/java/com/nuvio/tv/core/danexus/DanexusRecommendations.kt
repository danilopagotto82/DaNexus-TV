package com.nuvio.tv.core.danexus

import android.content.Context
import com.nuvio.tv.domain.model.Meta
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ProfileRecommendation(
    val key: String,
    val from: Int,
    val to: Int,
    val contentId: String,
    val type: String,
    val title: String,
    val poster: String?,
    val createdAt: Long,
    val seen: Boolean = false,
    val backdrop: String? = null,
)

/** Shared by local profiles only. Deleting a profile removes its recommendations. */
class DanexusRecommendations(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("danexus-profile-recommendations", Context.MODE_PRIVATE)
    fun read(): List<ProfileRecommendation> = synchronized(lock) {
        runCatching {
            val array = JSONArray(prefs.getString("items", "[]"))
            (0 until array.length()).mapNotNull { i -> runCatching {
                val r = array.getJSONObject(i)
                ProfileRecommendation(r.getString("key"), r.getInt("from"), r.getInt("to"),
                    r.getString("id"), r.getString("type"), r.getString("title"),
                    r.optString("poster").takeIf { it.isNotBlank() }, r.getLong("created"), r.optBoolean("seen"),
                    r.optString("backdrop").takeIf { it.isNotBlank() })
            }.getOrNull() }.sortedByDescending { it.createdAt }
        }.getOrDefault(emptyList())
    }

    fun send(from: Int, to: Int, meta: Meta) = send(from, listOf(to), meta)

    fun send(from: Int, recipients: Collection<Int>, meta: Meta) = synchronized(lock) {
        val targets = recipients.distinct().filter { it != from }
        if (targets.isEmpty()) return@synchronized
        val createdAt = System.currentTimeMillis()
        val updated = targets.fold(read()) { rows, to ->
            ProfileRecommendationRules.deliver(rows, ProfileRecommendation(
                UUID.randomUUID().toString(), from, to, meta.id, meta.apiType,
                meta.name, meta.poster, createdAt,
                backdrop = meta.background?.takeIf { it.isNotBlank() } ?: meta.landscapePoster?.takeIf { it.isNotBlank() }
            ))
        }
        save(updated)
    }
    fun markSeen(key: String, recipient: Int) = synchronized(lock) {
        save(ProfileRecommendationRules.markSeen(read(), key, recipient))
    }
    fun dismiss(key: String, active: Int) = synchronized(lock) {
        save(ProfileRecommendationRules.dismiss(read(), key, active))
    }
    fun removeProfile(id: Int) = synchronized(lock) { save(ProfileRecommendationRules.removeProfile(read(), id)) }
    private fun save(rows: List<ProfileRecommendation>) {
        val array = JSONArray()
        rows.take(600).forEach { r -> array.put(JSONObject().put("key", r.key).put("from", r.from).put("to", r.to)
            .put("id", r.contentId).put("type", r.type).put("title", r.title).put("poster", r.poster.orEmpty())
            .put("created", r.createdAt).put("seen", r.seen).put("backdrop", r.backdrop.orEmpty())) }
        prefs.edit().putString("items", array.toString()).apply()
    }
    companion object { private val lock = Any() }
}
