package com.evilgpt.app

import com.evilgpt.app.data.ChatSession
import com.evilgpt.app.data.EvilGptModels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {
    @Test fun fallbackModelListContainsFreeAndFlagshipModels() {
        val ids = EvilGptModels.fallback.map { it.id }
        assertTrue(ids.contains("model-free"))
        assertTrue(ids.contains("model-22"))
    }

    @Test fun customTitleTakesPrecedence() {
        assertEquals("My saved chat", ChatSession("id", "New Chat", "My saved chat").displayTitle)
    }

    @Test fun blankCustomTitleFallsBackToSessionTitle() {
        assertEquals("New Chat", ChatSession("id", "New Chat", " ").displayTitle)
    }
}
