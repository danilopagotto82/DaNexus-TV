package com.nuvio.tv.ui.v2.components

import org.junit.Assert.*
import org.junit.Test

class GlassMaterialTest {
    @Test fun `body stays translucent readable and monotonically clearer across every role`() {
        GlassRole.entries.forEach { role ->
            listOf(false, true).forEach { playback ->
                val values = (0..100).map { glassBodyAlpha(role, playback, .28f, it) }
                assertTrue(values.all { it in .10f.. .88f })
                assertTrue(values.zipWithNext().all { (a, b) -> a >= b })
            }
            assertEquals(glassBodyAlpha(role, false, .50f, 60), glassBodyAlpha(role, false, .94f, 60))
        }
    }

    @Test fun `balanced retains the approved neutral player body opacity`() {
        assertEquals(.18f, glassBodyAlpha(GlassRole.CONTROL, true, .18f, 60), .0001f)
        assertEquals(.28f, glassBodyAlpha(GlassRole.PANEL, true, .28f, 60), .0001f)
        assertEquals(.38f, glassBodyAlpha(GlassRole.HUD, true, .38f, 60), .0001f)
    }

    @Test fun `smoked body follows the transparency setting within a readable range`() {
        assertEquals(.62f, smokedGlassBodyAlpha(60), .0001f)
        val values = (0..100).map { smokedGlassBodyAlpha(it) }
        assertTrue(values.all { it in .50f.. .86f })
        assertTrue(values.zipWithNext().all { (a, b) -> a >= b })
    }

    @Test fun `smoked fill tops any thinner body up to the smoked opacity and never thins a denser one`() {
        listOf(0, 30, 60, 100).forEach { transparency ->
            val target = smokedGlassBodyAlpha(transparency)
            listOf(.025f, .08f, .10f, .48f).forEach { body ->
                val fill = smokedGlassFillAlpha(body, transparency)
                assertEquals(target, 1f - (1f - body) * (1f - fill), .0001f)
            }
            assertEquals(0f, smokedGlassFillAlpha(.90f, transparency))
        }
    }
}

