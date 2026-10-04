package com.nuvio.tv.core.danexus

import org.junit.Assert.*
import org.junit.Test

class ProfileRecommendationRulesTest {
    private fun row(key: String, from: Int, to: Int, id: String = "film") =
        ProfileRecommendation(key, from, to, id, "movie", "Filme", null, 1L)

    @Test fun receivingProfileSeesOnlyItsOwnRecommendations() {
        val rows = listOf(row("a", 1, 2), row("b", 3, 2), row("c", 1, 3))
        assertEquals(listOf("a", "b"), ProfileRecommendationRules.inbox(rows, 2, false, null).map { it.key })
        assertEquals(listOf("a"), ProfileRecommendationRules.inbox(rows, 2, false, 1).map { it.key })
    }

    @Test fun sentListBelongsToTheActiveSender() {
        val rows = listOf(row("a", 1, 2), row("b", 3, 2), row("c", 1, 3))
        assertEquals(listOf("a", "c"), ProfileRecommendationRules.inbox(rows, 1, true, null).map { it.key })
        assertEquals(listOf("c"), ProfileRecommendationRules.inbox(rows, 1, true, 3).map { it.key })
    }

    @Test fun recommendingAgainReplacesOnlyTheSameSenderRecipientAndTitle() {
        val rows = listOf(row("old", 1, 2), row("other-profile", 1, 3), row("other-film", 1, 2, "second"))
        val updated = ProfileRecommendationRules.deliver(rows, row("new", 1, 2))
        assertEquals(listOf("new", "other-profile", "other-film"), updated.map { it.key })
        assertEquals(rows, ProfileRecommendationRules.deliver(rows, row("self", 1, 1)))
    }

    @Test fun unrelatedProfileCannotReadOrDismissARecommendation() {
        val rows = listOf(row("a", 1, 2))
        assertFalse(ProfileRecommendationRules.markSeen(rows, "a", 3).first().seen)
        assertTrue(ProfileRecommendationRules.markSeen(rows, "a", 2).first().seen)
        assertEquals(rows, ProfileRecommendationRules.dismiss(rows, "a", 3))
        assertTrue(ProfileRecommendationRules.dismiss(rows, "a", 2).isEmpty())
    }

    @Test fun deletingProfilePreventsRecommendationsFromReappearingInReusedSlot() {
        val rows = listOf(row("received", 1, 2), row("sent", 2, 3), row("unrelated", 1, 3))
        assertEquals(listOf("unrelated"), ProfileRecommendationRules.removeProfile(rows, 2).map { it.key })
    }
}
