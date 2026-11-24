package com.csymvoul.expenses.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateTest {
    private val github = """
        {"tag_name":"v1.12.0","html_url":"https://github.com/csymvoul/expensiarmus/releases/tag/v1.12.0","draft":false,
         "assets":[{"name":"notes.txt","browser_download_url":"https://x/notes.txt"},
                   {"name":"Expensiarmus-1.12.0.apk","size":8200000,
                    "browser_download_url":"https://github.com/csymvoul/expensiarmus/releases/download/v1.12.0/Expensiarmus-1.12.0.apk"}],
         "body":"Notes"}
    """.trimIndent()

    @Test fun `versions compare part by part`() {
        assertTrue(AppVersion.parse("1.10.0")!! > AppVersion.parse("1.9.5")!!)
        assertTrue(AppVersion.parse("v2.0")!! > AppVersion.parse("1.99.99")!!)
        assertEquals(0, AppVersion.parse("1.10")!!.compareTo(AppVersion.parse("1.10.0")!!))
        assertEquals(AppVersion(listOf(1, 11, 0)), AppVersion.parse("v1.11.0-beta"))
        assertNull(AppVersion.parse("latest"))
        assertNull(AppVersion.parse(""))
    }

    @Test fun `the APK is found in GitHub's release`() {
        val r = AppUpdate.parseLatestRelease(github)!!
        assertEquals("v1.12.0", r.tag)
        assertEquals(AppVersion(listOf(1, 12, 0)), r.version)
        assertEquals("Expensiarmus-1.12.0.apk", r.apkName)
        assertTrue(r.apkUrl.endsWith("/v1.12.0/Expensiarmus-1.12.0.apk"))
        assertEquals("https://github.com/csymvoul/expensiarmus/releases/tag/v1.12.0", r.pageUrl)
    }

    @Test fun `no APK, no tag or bad JSON means no update`() {
        assertNull(AppUpdate.parseLatestRelease("""{"tag_name":"v1.12.0","assets":[]}"""))
        assertNull(AppUpdate.parseLatestRelease("""{"assets":[{"name":"a.apk","browser_download_url":"u"}]}"""))
        assertNull(AppUpdate.parseLatestRelease("""{"message":"API rate limit exceeded"}"""))
        assertNull(AppUpdate.parseLatestRelease("not json"))
    }

    @Test fun `only a newer release counts`() {
        val r = AppUpdate.parseLatestRelease(github)
        assertTrue(AppUpdate.isNewer(r, "1.11.0"))
        assertFalse(AppUpdate.isNewer(r, "1.12.0"))
        assertFalse(AppUpdate.isNewer(r, "1.13.0"))
        assertFalse(AppUpdate.isNewer(null, "1.11.0"))
    }

    @Test fun `each version is announced once`() {
        val r = AppUpdate.parseLatestRelease(github)
        assertTrue(AppUpdate.shouldNotify(r, "1.11.0", lastNotifiedTag = "v1.11.0"))
        assertFalse(AppUpdate.shouldNotify(r, "1.11.0", lastNotifiedTag = "v1.12.0"))
        assertFalse(AppUpdate.shouldNotify(r, "1.12.0", lastNotifiedTag = null))
    }

    @Test fun `the daily check runs about once a day`() {
        val day = AppUpdate.CHECK_EVERY_MILLIS
        assertTrue(AppUpdate.isDue(0, 1_000))
        assertFalse(AppUpdate.isDue(10_000, 10_000 + day - 1))
        assertTrue(AppUpdate.isDue(10_000, 10_000 + day))
        assertTrue(AppUpdate.isDue(10_000, 5_000)) // clock moved back
    }

    @Test fun `GitHub's real answer for v1_10_0 is understood`() {
        val body = javaClass.getResource("/github-latest-release.json")!!.readText()
        val r = AppUpdate.parseLatestRelease(body)!!
        assertEquals("v1.10.0", r.tag)
        assertEquals("Expensiarmus-1.10.0.apk", r.apkName)
        assertEquals("https://github.com/csymvoul/expensiarmus/releases/download/v1.10.0/Expensiarmus-1.10.0.apk", r.apkUrl)
        assertTrue(AppUpdate.isNewer(r, "1.9.5"))
        assertFalse(AppUpdate.isNewer(r, "1.11.0"))
    }
}
