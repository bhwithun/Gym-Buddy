package com.gymbuddy

// Data classes for xAI Grok API

data class GrokChatRequest(
    val messages: List<GrokMessage>,
    val model: String,
    val stream: Boolean = false
)

data class GrokMessage(
    val role: String, // "system", "user", or "assistant"
    val content: String
)

data class GrokChatResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<GrokChoice>,
    val usage: GrokUsage
)

data class GrokChoice(
    val index: Int,
    val message: GrokMessage,
    val finish_reason: String?
)

data class GrokUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

// For error responses
data class GrokErrorResponse(
    val error: GrokError
)

data class GrokError(
    val message: String,
    val type: String,
    val code: String?
)

// Models endpoint response
data class GrokModelsResponse(
    val `object`: String,
    val data: List<GrokModelInfo>
)

data class GrokModelInfo(
    val id: String,
    val `object`: String,
    val created: Long,
    val owned_by: String
)
