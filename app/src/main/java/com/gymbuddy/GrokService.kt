package com.gymbuddy

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class GrokService(private val context: Context) : AIService {

    private val gson = Gson()
    private val prefs: SharedPreferences by lazy { createEncryptedPrefs() }
    private var selectedModel: String = "grok-3"

    private val apiService: GrokApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.x.ai/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(GrokApiService::class.java)
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "grok_prefs",
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
        return listOf("grok-3", "grok-beta")
    }

    override fun getProviderName(): String {
        return "xAI (Grok)"
    }

    override fun setSelectedModel(model: String) {
        selectedModel = model
    }

    override fun getSelectedModel(): String {
        return selectedModel
    }

    override fun fetchAvailableModelsFromAPI(callback: (Result<List<String>>) -> Unit) {
        val apiKey = getApiKey()
        if (apiKey == null) {
            callback(Result.failure(Exception("API key not set")))
            return
        }

        Thread {
            try {
                val response = apiService.getModels("Bearer $apiKey").execute()
                if (response.isSuccessful) {
                    val modelsResponse = response.body()
                    val modelIds = modelsResponse?.data?.map { it.id } ?: emptyList()
                    callback(Result.success(modelIds))
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val errorResponse = gson.fromJson(errorBody, GrokErrorResponse::class.java)
                        "API Error ${response.code()}: ${errorResponse.error.message}"
                    } catch (e: Exception) {
                        "API Error ${response.code()}: ${response.message()}\nBody: $errorBody"
                    }
                    callback(Result.failure(Exception(errorMessage)))
                }
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }.start()
    }

    override fun sendMessage(messages: List<AIMessage>, callback: (Result<String>) -> Unit) {
        val apiKey = getApiKey()
        if (apiKey == null) {
            callback(Result.failure(Exception("API key not set")))
            return
        }

        // Convert AIMessage to GrokMessage
        val grokMessages = messages.map { GrokMessage(it.role, it.content) }
        val request = GrokChatRequest(messages = grokMessages, model = selectedModel)

        Thread {
            try {
                val response = apiService.chatCompletion("Bearer $apiKey", request).execute()
                if (response.isSuccessful) {
                    val chatResponse = response.body()
                    val content = chatResponse?.choices?.firstOrNull()?.message?.content
                    if (content != null) {
                        callback(Result.success(content))
                    } else {
                        callback(Result.failure(Exception("Empty response from Grok")))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val errorResponse = gson.fromJson(errorBody, GrokErrorResponse::class.java)
                        "API Error ${response.code()}: ${errorResponse.error.message}"
                    } catch (e: Exception) {
                        "API Error ${response.code()}: ${response.message()}\nBody: $errorBody"
                    }
                    callback(Result.failure(Exception(errorMessage)))
                }
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }.start()
    }
}
