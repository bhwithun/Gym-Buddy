package com.gymbuddy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseVersionTest {

    @Test
    fun readsThreePartTag() {
        assertEquals("9.0.2", ReleaseVersion.nameFromTag("v9.0.2"))
        assertEquals("9.0.1", ReleaseVersion.nameFromTag("9.0.1"))
        assertNull(ReleaseVersion.nameFromTag("v9.0"))
        assertNull(ReleaseVersion.nameFromTag("v9.0.2-beta"))
    }

    @Test
    fun comparesNumericParts() {
        assertTrue(ReleaseVersion.isNewer("9.0.2", "9.0.1"))
        assertTrue(ReleaseVersion.isNewer("v9.1.0", "9.0.9"))
        assertFalse(ReleaseVersion.isNewer("9.0.1", "9.0.1"))
        assertFalse(ReleaseVersion.isNewer("9.0.1", "9.0.2"))
        assertFalse(ReleaseVersion.isNewer("latest", "9.0.1"))
    }

    @Test
    fun picksTheReleaseApk() {
        val json = """
            {
              "tag_name": "v9.0.2",
              "assets": [
                {"name": "notes.txt", "browser_download_url": "https://github.com/notes"},
                {"name": "gym-buddy-9.0.2.apk", "browser_download_url": "https://github.com/gym.apk"}
              ]
            }
        """.trimIndent()
        val offer = ReleaseVersion.parseLatestRelease(json)
        assertEquals("9.0.2", offer?.versionName)
        assertEquals("https://github.com/gym.apk", offer?.apkUrl)
    }

    @Test
    fun ignoresAReleaseWithoutAnApk() {
        assertNull(ReleaseVersion.parseLatestRelease("""{"tag_name":"v9.0.2","assets":[]}"""))
    }

    @Test
    fun requiresTheVersionInTheNameWhenSeveralApksExist() {
        val json = """
            {"tag_name":"v9.0.2","assets":[
              {"name":"other.apk","browser_download_url":"https://github.com/other.apk"},
              {"name":"gym-buddy-9.0.2.apk","browser_download_url":"https://github.com/gym.apk"}
            ]}
        """.trimIndent()
        assertEquals("https://github.com/gym.apk", ReleaseVersion.parseLatestRelease(json)?.apkUrl)
        val ambiguous = """
            {"tag_name":"v9.0.2","assets":[
              {"name":"a.apk","browser_download_url":"https://github.com/a.apk"},
              {"name":"b.apk","browser_download_url":"https://github.com/b.apk"}
            ]}
        """.trimIndent()
        assertNull(ReleaseVersion.parseLatestRelease(ambiguous))
    }
}
