package com.nuvio.tv.ui.screens.player

import android.content.Context
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.ui.screens.player.smartsource.LocalSourceReputationPolicy
import com.nuvio.tv.ui.screens.player.smartsource.PlaybackHealth
import com.nuvio.tv.ui.screens.player.smartsource.SmartSource
import com.nuvio.tv.ui.screens.player.smartsource.SourceReputationEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Operator config is separate from household UX; no credentials are accepted here. */
internal data class SmartSourceConfig(
    val enabled: Boolean,
    val allowedAddonNames: Set<String>,
    val kids: Boolean = false,
    val backendBaseUrls: List<String> = emptyList(),
    val backendProfileId: String? = null,
    val category: String = "",
    val waitSeconds: Int = 12,
    val promptSeconds: Int = 6,
    val stallSeconds: Int = 20,
    val showPrompt: Boolean = true,
    val learning: Boolean = true,
    val allAddons: Boolean = false,
    val sourceLimit: Int = 0,
) {
    val backendBaseUrl: String? get() = backendBaseUrls.firstOrNull()

    companion object {
        fun load(context: Context, addonName: String, profileId: Int): SmartSourceConfig {
            val options = com.nuvio.tv.core.danexus.DanexusPreferences.current(context)
            val operator = loadOperator(context, addonName, profileId)
            return operator.copy(enabled = operator.enabled && options.smartEnabled,
                waitSeconds = options.waitSeconds, promptSeconds = options.promptSeconds,
                stallSeconds = options.stallSeconds, showPrompt = options.showPrompt,
                learning = options.learning, allAddons = options.allAddons, sourceLimit = options.sourceLimit)
        }
        private fun loadOperator(context: Context, addonName: String, profileId: Int): SmartSourceConfig {
            val default = SmartSourceConfig(
                enabled = addonName.contains("danexus", ignoreCase = true),
                allowedAddonNames = setOf(addonName),
            )
            return runCatching {
                val local = File(context.filesDir, "danexus-smart-source.json")
                val raw = if (local.isFile) local.readText() else
                    context.assets.open("danexus-smart-source.json").bufferedReader().use { it.readText() }
                val profiles = JSONObject(raw).optJSONArray("profiles") ?: return default
                val row = (0 until profiles.length()).map { profiles.getJSONObject(it) }.firstOrNull {
                    (!it.has("nuvioProfileId") || it.optInt("nuvioProfileId") == profileId) &&
                        it.optJSONArray("selectedAddonNames").strings().any { name -> name.equals(addonName, ignoreCase = true) }
                } ?: return default
                val urls = buildList {
                    validBackendBaseUrl(row.optString("backendBaseUrl"))?.let(::add)
                    row.optJSONArray("backendBaseUrls").strings().mapNotNull(::validBackendBaseUrl).forEach(::add)
                }.distinct().take(4)
                SmartSourceConfig(
                    enabled = row.optBoolean("enabled", true),
                    allowedAddonNames = row.optJSONArray("allowedAddonNames").strings().toSet() + addonName,
                    kids = row.optBoolean("kids", false),
                    backendBaseUrls = urls,
                    backendProfileId = row.optString("backendProfileId").takeIf { it.matches(Regex("[a-zA-Z0-9_-]{1,100}")) },
                    category = row.optString("category").take(40),
                )
            }.getOrDefault(default)
        }
    }
}

private fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { optString(it) }.filter { it.isNotBlank() }

internal fun smartOpaqueKey(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

internal fun validBackendBaseUrl(raw: String): String? = runCatching {
    val uri = URI(raw)
    if (uri.userInfo != null || uri.rawQuery != null || uri.rawFragment != null) return null
    val host = uri.host?.lowercase() ?: return null
    val chunks = host.split('.')
    val parts = if (chunks.size == 4 && chunks.all { it.toIntOrNull() in 0..255 }) chunks.map { it.toInt() } else emptyList()
    val privateHost = host == "localhost" || host == "127.0.0.1" || host == "::1" ||
        (parts.size == 4 && (parts[0] == 10 || (parts[0] == 192 && parts[1] == 168) ||
            (parts[0] == 172 && parts[1] in 16..31)))
    if (uri.scheme != "https" && !(uri.scheme == "http" && privateHost)) return null
    raw.trimEnd('/')
}.getOrNull()

internal fun smartSafeLabel(raw: String?): String = raw.orEmpty()
    .replace(Regex("https?://\\S+", RegexOption.IGNORE_CASE), "")
    .replace(Regex("[\\r\\n\\u0000-\\u001f]"), " ").trim().take(120)

internal data class SmartRemoteCommand(val id: Long, val action: String)

/** Only whitelisted semantic fields enter the journal or backend. */
internal class SmartSourceBridge(private val context: Context, private val config: SmartSourceConfig) {
    private val client = OkHttpClient.Builder().connectTimeout(350, TimeUnit.MILLISECONDS)
        .readTimeout(650, TimeUnit.MILLISECONDS).callTimeout(850, TimeUnit.MILLISECONDS)
        .followRedirects(false).followSslRedirects(false).build()
    private val journal = File(context.filesDir, "danexus-smart-source-feedback.json")

    suspend fun rank(
        sources: List<SmartSource<Stream>>,
        selected: SmartSource<Stream>,
        mediaType: String?,
        titleId: String?,
    ): List<SmartSource<Stream>> = withContext(Dispatchers.IO) {
        val local = LocalSourceReputationPolicy.apply(
            sources = sources,
            events = readReputationEvents(),
            category = config.category,
            mediaType = mediaType,
        )
        val profile = config.backendProfileId ?: return@withContext local
        try {
            val payload = JSONObject().put("profileId", profile)
                .put("selectedIdentity", "opaque:${selected.identity}")
                .put("context", JSONObject().put("mediaType", smartSafeLabel(mediaType))
                    .put("titleId", smartSafeLabel(titleId)).put("category", config.category))
                .put("sources", JSONArray().apply {
                    (listOf(selected) + local).distinctBy { it.identity }.forEach { source ->
                        put(JSONObject().put("id", source.identity).put("addonId", source.addonId)
                            .put("mirrorKey", source.mirrorKey).put("localScore", source.score)
                            .apply { source.seeds?.let { put("seeds", it) } })
                    }
                })
            val response = postFirst("/api/smart-source/client/queue", payload) ?: return@withContext local
            val rows = response.optJSONArray("queue") ?: return@withContext local
            val ranked = (0 until rows.length()).associate { i ->
                val row = rows.getJSONObject(i)
                row.optString("id") to Pair(row.optLong("timeoutMs", 7_000L), row.optDouble("smartScore", 70.0))
            }
            local.map { source -> ranked[source.identity]?.let { (timeout, score) ->
                source.copy(timeoutMs = timeout, score = score)
            } ?: source }
        } catch (_: Exception) { local }
    }

    suspend fun heartbeat(
        sessionId: String,
        afterCommandId: Long,
        title: String?,
        state: String,
        attempt: Int,
        maxSources: Int,
        source: SmartSource<Stream>,
    ): SmartRemoteCommand? = withContext(Dispatchers.IO) {
        val profile = config.backendProfileId ?: return@withContext null
        val payload = JSONObject()
            .put("profileId", profile).put("sessionId", smartSafeLabel(sessionId))
            .put("afterCommandId", afterCommandId).put("title", smartSafeLabel(title))
            .put("category", config.category).put("state", smartSafeLabel(state))
            .put("attempt", attempt).put("maxSources", maxSources)
            .put("addonId", smartSafeLabel(source.addonId)).put("mirrorKey", source.mirrorKey)
        val response = runCatching { postFirst("/api/smart-source/client/heartbeat", payload) }.getOrNull()
            ?: return@withContext null
        val command = response.optJSONObject("command") ?: return@withContext null
        val id = command.optLong("id", 0L)
        val action = command.optString("action")
        if (id <= afterCommandId || action !in setOf("next_source", "toggle_play_pause")) null
        else SmartRemoteCommand(id, action)
    }

    suspend fun feedback(
        source: SmartSource<Stream>,
        health: PlaybackHealth,
        titleId: String?,
        title: String?,
        mediaType: String?,
        firstFrameMs: Long? = null,
        stallMs: Long? = null,
        httpStatus: Int? = null,
        evidence: String? = null,
    ) = withContext(Dispatchers.IO) {
        val kind = when (health) {
            PlaybackHealth.FIRST_FRAME_FAST -> "opened_quick"
            PlaybackHealth.FIRST_FRAME_SLOW -> "opened_slow"
            PlaybackHealth.HTTP_FATAL -> "http_error"
            PlaybackHealth.NETWORK_TIMEOUT -> "failed"
            PlaybackHealth.CODEC_ERROR -> "codec_error"
            PlaybackHealth.STALL -> "stalled"
            PlaybackHealth.PLAYBACK_OK, PlaybackHealth.USER_SKIP -> null
        }
        val event = JSONObject().put("id", "pb-${UUID.randomUUID()}")
            .put("at", Instant.now().toString()).put("result", health.name)
            .put("addonId", smartSafeLabel(source.addonId)).put("mirrorKey", source.mirrorKey)
            .put("titleId", smartSafeLabel(titleId)).put("title", smartSafeLabel(title))
            .put("mediaType", smartSafeLabel(mediaType)).put("category", config.category)
            .put("quality", smartSafeLabel(source.payload.quality)).apply {
                kind?.let { put("kind", it) }; firstFrameMs?.let { put("firstFrameMs", it) }
                stallMs?.let { put("stallMs", it) }; httpStatus?.let { put("httpStatus", it) }
                source.seeds?.let { put("seeds", it) }; evidence?.let { put("evidence", smartSafeLabel(it)) }
            }
        appendJournal(event.put("backendAcknowledged", false))
        if (kind != null && config.backendProfileId != null) {
            val sent = runCatching {
                postFirst("/api/smart-source/client/feedback",
                    JSONObject(event.toString()).put("profileId", config.backendProfileId))?.optBoolean("ok") == true
            }.getOrDefault(false)
            if (sent) {
                event.put("backendAcknowledged", true)
                replaceJournalEvent(event)
            }
        }
    }

    private fun readReputationEvents(): List<SourceReputationEvent> = synchronized(journalLock) {
        val rows = runCatching { JSONArray(journal.readText()) }.getOrDefault(JSONArray())
        (0 until rows.length()).mapNotNull { i ->
            val row = rows.optJSONObject(i) ?: return@mapNotNull null
            val health = runCatching { PlaybackHealth.valueOf(row.optString("result")) }.getOrNull()
                ?: return@mapNotNull null
            SourceReputationEvent(
                addonId = row.optString("addonId"),
                mirrorKey = row.optString("mirrorKey"),
                health = health,
                atEpochMs = runCatching { Instant.parse(row.optString("at")).toEpochMilli() }.getOrDefault(0L),
                category = row.optString("category"),
                mediaType = row.optString("mediaType"),
                firstFrameMs = row.optLong("firstFrameMs").takeIf { row.has("firstFrameMs") },
                seeds = row.optInt("seeds").takeIf { row.has("seeds") },
            )
        }
    }

    private fun appendJournal(event: JSONObject) = synchronized(journalLock) {
        val previous = runCatching { JSONArray(journal.readText()) }.getOrDefault(JSONArray())
        val bounded = JSONArray()
        for (i in (previous.length() - 399).coerceAtLeast(0) until previous.length()) bounded.put(previous.get(i))
        bounded.put(event)
        writeJournal(bounded)
    }

    private fun replaceJournalEvent(event: JSONObject) = synchronized(journalLock) {
        val previous = runCatching { JSONArray(journal.readText()) }.getOrDefault(JSONArray())
        val out = JSONArray()
        for (i in 0 until previous.length()) {
            val row = previous.optJSONObject(i)
            out.put(if (row?.optString("id") == event.optString("id")) event else previous.get(i))
        }
        writeJournal(out)
    }

    private fun writeJournal(rows: JSONArray) {
        runCatching {
            val temp = File(journal.parentFile, "${journal.name}.tmp")
            temp.writeText(rows.toString())
            if (!temp.renameTo(journal)) { journal.writeText(rows.toString()); temp.delete() }
        }
    }

    private fun postFirst(path: String, payload: JSONObject): JSONObject? {
        for (base in config.backendBaseUrls) {
            val response = runCatching { post("$base$path", payload) }.getOrNull()
            if (response != null) return response
        }
        return null
    }

    private fun post(url: String, payload: JSONObject): JSONObject? {
        val request = Request.Builder().url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType())).build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()?.let(::JSONObject)
        }
    }

    companion object { private val journalLock = Any() }
}
