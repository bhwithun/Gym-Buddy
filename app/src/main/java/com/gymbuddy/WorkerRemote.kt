package com.gymbuddy

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object WorkerRemote {
    private const val PREFS = "worker_remote_prefs"
    private const val KEY_URL = "worker_url"
    private const val KEY_TOKEN = "worker_token"

    fun isConfigured(context: Context): Boolean = !getUrl(context).isNullOrBlank()

    fun getUrl(context: Context): String? {
        val raw = prefs(context).getString(KEY_URL, null)?.trim().orEmpty()
        if (raw.isEmpty()) return null
        return normalizeUrl(raw)
    }

    fun getToken(context: Context): String? {
        val token = prefs(context).getString(KEY_TOKEN, null)?.trim().orEmpty()
        return token.ifEmpty { null }
    }

    fun save(context: Context, url: String, token: String) {
        prefs(context).edit()
            .putString(KEY_URL, url.trim())
            .putString(KEY_TOKEN, token.trim())
            .apply()
    }

    fun normalizeUrl(raw: String): String {
        var url = raw.trim().trimEnd('/')
        if (url.isEmpty()) return url
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        return url
    }

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}