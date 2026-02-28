package com.gymbuddy

interface AIService {
    fun sendMessage(messages: List<AIMessage>, callback: (Result<String>) -> Unit)
    fun hasApiKey(): Boolean
    fun saveApiKey(apiKey: String)
    fun getApiKey(): String?
    fun getAvailableModels(): List<String>
    fun getProviderName(): String
    fun setSelectedModel(model: String)
    fun getSelectedModel(): String
    fun fetchAvailableModelsFromAPI(callback: (Result<List<String>>) -> Unit)
}

data class AIMessage(
    val role: String, // "system", "user", or "assistant"
    val content: String
)