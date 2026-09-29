package com.gymbuddy

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

object ReleaseVersion {
    data class Offer(val versionName: String, val apkUrl: String)

    private data class ReleaseJson(
        @SerializedName("tag_name") val tagName: String? = null,
        val assets: List<AssetJson>? = null
    )

    private data class AssetJson(
        val name: String? = null,
        @SerializedName("browser_download_url") val browserDownloadUrl: String? = null
    )

    private val gson = Gson()

    fun nameFromTag(tag: String): String? {
        val cleaned = tag.trim().removePrefix("v").removePrefix("V")
        val parts = cleaned.split('.')
        if (parts.size != 3) return null
        val numbers = parts.map { part ->
            if (part.isEmpty() || part.any { it !in '0'..'9' }) return null
            part.toInt()
        }
        return numbers.joinToString(".")
    }

    fun isNewer(remoteName: String, installedName: String): Boolean {
        val remote = nameFromTag(remoteName)?.split('.')?.map { it.toInt() } ?: return false
        val installed = nameFromTag(installedName)?.split('.')?.map { it.toInt() } ?: return false
        for (index in 0..2) {
            if (remote[index] != installed[index]) return remote[index] > installed[index]
        }
        return false
    }

    fun parseLatestRelease(json: String): Offer? {
        val release = try {
            gson.fromJson(json, ReleaseJson::class.java)
        } catch (_: Exception) {
            null
        } ?: return null
        val versionName = nameFromTag(release.tagName.orEmpty()) ?: return null
        val apks = release.assets.orEmpty().mapNotNull { asset ->
            val name = asset.name.orEmpty()
            val url = asset.browserDownloadUrl.orEmpty()
            if (name.endsWith(".apk", ignoreCase = true) && url.startsWith("https://")) {
                name to url
            } else {
                null
            }
        }
        val url = when (apks.size) {
            0 -> return null
            1 -> apks[0].second
            else -> apks.firstOrNull { (name, _) -> name.contains(versionName) }?.second ?: return null
        }
        return Offer(versionName, url)
    }
}
