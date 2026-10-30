package com.evilgpt.app.data

data class UserSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val expiresAt: Long
)

data class ChatSession(
    val id: String,
    val title: String = "New Chat",
    val customTitle: String? = null,
    val starred: Boolean = false,
    val updatedAt: String = ""
) {
    val displayTitle: String get() = customTitle?.takeIf { it.isNotBlank() } ?: title.ifBlank { "New Chat" }
}

data class ChatAttachment(val name: String, val path: String, val mimeType: String, val size: Long)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String,
    val content: String,
    val createdAt: String = "",
    val images: List<ChatAttachment> = emptyList()
)

data class AiModel(val id: String, val label: String, val description: String, val category: String)

object EvilGptModels {
    val fallback = listOf(
        AiModel("model-free", "EvilGPT FREE", "Free uncensored chat", "free"),
        AiModel("model-16", "EvilGPT Quantum (235B)", "Ultra-large model", "uncensored"),
        AiModel("model-18", "EvilGPT Coder Pro", "Long code generation", "uncensored"),
        AiModel("model-22", "EvilGPT BIG", "Flagship reasoning powerhouse", "uncensored"),
        AiModel("model-23", "EvilGPT Latest", "Fast flagship, long context", "uncensored"),
        AiModel("model-24", "EvilGPT Extended", "Extended reasoning", "uncensored")
    )
}
