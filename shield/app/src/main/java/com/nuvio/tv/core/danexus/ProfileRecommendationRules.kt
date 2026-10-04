package com.nuvio.tv.core.danexus

internal object ProfileRecommendationRules {
    fun inbox(rows: List<ProfileRecommendation>, active: Int, sent: Boolean, person: Int?) =
        rows.filter { row ->
            if (sent) row.from == active && (person == null || row.to == person)
            else row.to == active && (person == null || row.from == person)
        }

    fun deliver(rows: List<ProfileRecommendation>, item: ProfileRecommendation): List<ProfileRecommendation> {
        if (item.from == item.to) return rows
        return listOf(item) + rows.filterNot {
            it.from == item.from && it.to == item.to && it.contentId == item.contentId && it.type == item.type
        }
    }

    fun markSeen(rows: List<ProfileRecommendation>, key: String, active: Int) =
        rows.map { if (it.key == key && it.to == active) it.copy(seen = true) else it }

    fun dismiss(rows: List<ProfileRecommendation>, key: String, active: Int) =
        rows.filterNot { it.key == key && (it.to == active || it.from == active) }

    fun removeProfile(rows: List<ProfileRecommendation>, id: Int) = rows.filterNot { it.from == id || it.to == id }
}
