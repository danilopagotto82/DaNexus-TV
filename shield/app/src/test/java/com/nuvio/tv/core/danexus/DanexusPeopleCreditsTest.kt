package com.nuvio.tv.core.danexus

import com.nuvio.tv.domain.model.MetaCastMember
import org.junit.Assert.*
import org.junit.Test

class DanexusPeopleCreditsTest {
    @Test fun actorsAndDirectorsKeepTheirNativePersonIdsWithoutWriters() {
        val actor = MetaCastMember("Robert Downey Jr.", "Tony Stark", tmdbId = 3223)
        val director = MetaCastMember("Christopher Nolan", "Director", tmdbId = 525)
        val writer = MetaCastMember("Writer credit", "Writer", tmdbId = 99)
        val visible = DanexusPeopleCredits.visible(listOf(actor, writer), listOf(director))
        assertEquals(listOf(525, 3223), visible.map { it.tmdbId })
        assertTrue(DanexusPeopleCredits.isDirector(visible.first()))
        assertFalse(DanexusPeopleCredits.isDirector(visible.last()))
    }
    @Test fun aDirectorWhoAlsoActsHasOneBadgeWithTheDirectorRole() {
        val director = MetaCastMember("Same person", "Director", tmdbId = 12)
        val actor = MetaCastMember("Same person", "Himself", tmdbId = 12)
        assertEquals(listOf(director), DanexusPeopleCredits.visible(listOf(actor), listOf(director)))
    }
}
