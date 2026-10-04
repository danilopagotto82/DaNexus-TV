package com.nuvio.tv.ui.screens.player.smartsource

/** Request identity stays in memory. Feedback uses opaque, sanitized keys. */
internal data class SmartSource<T>(
    val identity: String,
    val addonId: String,
    val mirrorKey: String,
    val payload: T,
    val timeoutMs: Long? = null,
    val score: Double = 70.0,
    val seeds: Int? = null,
    val originKey: String = addonId,
)

internal class SmartSourceQueue<T>(selected: SmartSource<T>, alternatives: List<SmartSource<T>>, private val manifestOrder: Boolean = false, private val learning: Boolean = true) {
    private val selectedIdentity = selected.identity
    private var rows: List<SmartSource<T>> = order(selected, alternatives)

    fun update(alternatives: List<SmartSource<T>>) {
        val fresh = alternatives.associateBy { it.identity }.toMutableMap()
        val selected = fresh.remove(selectedIdentity) ?: rows.first { it.identity == selectedIdentity }
        val known = rows.drop(1).map { fresh.remove(it.identity) ?: it }
        // Refresh metadata while preserving arrival order when scores are tied.
        rows = order(selected, known + fresh.values)
    }

    fun next(attempted: Set<String>): SmartSource<T>? = rows.firstOrNull { it.identity !in attempted }
    fun snapshot(): List<SmartSource<T>> = rows.toList()

    private fun order(selected: SmartSource<T>, alternatives: List<SmartSource<T>>): List<SmartSource<T>> {
        val unique = (listOf(selected) + alternatives).distinctBy { it.identity }
        fun rank(source: SmartSource<T>): Double = source.score - if (source.seeds == 0) 55.0 else 0.0
        val remaining = unique.filter { it.identity != selectedIdentity }
        if (manifestOrder) return listOf(selected) + if (learning) remaining.sortedByDescending { it.score } else remaining
        val selectedAddon = selected.originKey
        return listOf(selected) +
            remaining.filter { it.originKey == selectedAddon }.sortedByDescending(::rank) +
            remaining.filter { it.originKey != selectedAddon }.sortedByDescending(::rank)
    }
}
