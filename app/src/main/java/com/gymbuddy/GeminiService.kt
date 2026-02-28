package com.gymbuddy

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class GeminiService(private val context: Context) : AIService {

    private val prefs: SharedPreferences by lazy { createEncryptedPrefs() }
    private var selectedModel: String = "gemini-pro"

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "gemini_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun saveApiKey(apiKey: String) {
        prefs.edit().putString("api_key", apiKey).apply()
    }

    override fun getApiKey(): String? {
        return prefs.getString("api_key", null)
    }

    override fun hasApiKey(): Boolean {
        return getApiKey() != null
    }

    override fun getAvailableModels(): List<String> {
        return listOf("gemini-pro", "gemini-pro-vision")
    }

    override fun getProviderName(): String {
        return "Google (Gemini)"
    }

    override fun setSelectedModel(model: String) {
        selectedModel = model
    }

    override fun getSelectedModel(): String {
        return selectedModel
    }

    override fun fetchAvailableModelsFromAPI(callback: (Result<List<String>>) -> Unit) {
        // TODO: Implement Gemini models API
        callback(Result.success(listOf("gemini-pro", "gemini-pro-vision")))
    }

    override fun sendMessage(messages: List<AIMessage>, callback: (Result<String>) -> Unit) {
        // TODO: Implement Gemini API integration
        callback(Result.failure(Exception("Gemini integration not yet implemented. Please use xAI (Grok) for now.")))
    }
}