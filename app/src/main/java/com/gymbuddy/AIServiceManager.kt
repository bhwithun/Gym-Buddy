package com.gymbuddy

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AIServiceManager(private val context: Context) {

    private val prefs: SharedPreferences by lazy { createEncryptedPrefs() }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "ai_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getAvailableProviders(): List<String> {
        return listOf("xAI (Grok)", "Google (Gemini)") // Add more as implemented
    }

    fun getAIService(provider: String): AIService {
        return when (provider) {
            "xAI (Grok)" -> GrokService(context)
            "Google (Gemini)" -> GeminiService(context) // Will implement later
            else -> GrokService(context)
        }
    }

    fun saveSelectedProvider(provider: String) {
        prefs.edit().putString("selected_provider", provider).apply()
    }

    fun getSelectedProvider(): String {
        return prefs.getString("selected_provider", "xAI (Grok)") ?: "xAI (Grok)"
    }

    fun saveSelectedModel(provider: String, model: String) {
        prefs.edit().putString("selected_model_$provider", model).apply()
    }

    fun getSelectedModel(provider: String): String {
        val service = getAIService(provider)
        return prefs.getString("selected_model_$provider", service.getAvailableModels().firstOrNull() ?: "") ?: ""
    }
}