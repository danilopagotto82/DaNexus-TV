package com.nuvio.tv.core.danexus

import org.junit.Assert.*
import org.junit.Test

class DanexusSeekFilmstripTest {
    @Test fun middleShowsFiveDifferentScenesWithSelectionInTheCenter() {
        assertEquals(listOf(30_000L, 40_000L, 50_000L, 60_000L, 70_000L),
            DanexusSeekPreviewPolicy.neighbors(50_000L, 120_000L))
    }
    @Test fun edgesHaveEmptySlotsInsteadOfRepeatingOrRequestingInvalidScenes() {
        assertEquals(listOf(null, null, 0L, 10_000L, 20_000L),
            DanexusSeekPreviewPolicy.neighbors(0L, 120_000L))
        assertEquals(listOf(99_999L, 109_999L, 119_999L, null, null),
            DanexusSeekPreviewPolicy.neighbors(120_000L, 120_000L))
        assertEquals(List<Long?>(5) { null }, DanexusSeekPreviewPolicy.neighbors(0L, 0L))
    }
    @Test fun cueSpacingMatchesTheProviderAndCannotOverflowTheEnd() {
        assertEquals(listOf(40_000L, 45_000L, 50_000L, 55_000L, 60_000L),
            DanexusSeekPreviewPolicy.neighbors(50_000L, 120_000L, 5_000L))
        val slots = DanexusSeekPreviewPolicy.neighbors(Long.MAX_VALUE, Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE - 1, slots[2])
        assertNull(slots[3])
        assertNull(slots[4])
    }
}
