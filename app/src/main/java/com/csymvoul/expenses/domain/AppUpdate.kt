package com.csymvoul.expenses.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** "1.10.0" (a leading "v" and anything after "-" are ignored); compares part by part, so 1.10 is newer than 1.9. */
data class AppVersion(val parts: List<Int>) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion): Int {
        for (i in 0 until maxOf(parts.size, other.parts.size)) {
            val c = parts.getOrElse(i) { 0 }.compareTo(other.parts.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }

    override fun toString() = parts.joinToString(".")

    companion object {
        fun parse(text: String?): AppVersion? {
            val core = text?.trim()?.removePrefix("v")?.removePrefix("V")?.substringBefore('-')?.takeIf { it.isNotEmpty() } ?: return null
            val parts = core.split('.').map { it.toIntOrNull() ?: return null }
            return AppVersion(parts)
        }
    }
}

/** The newest release on GitHub that carries an APK. */
data class Release(val version: AppVersion, val tag: String, val apkUrl: String, val apkName: String, val pageUrl: String)

object AppUpdate {
    /** GitHub's "latest release" endpoint for the app's repository (drafts and pre-releases are left out by GitHub). */
    const val LATEST_URL = "https://api.github.com/repos/csymvoul/expensiarmus/releases/latest"

    /** The app checks at most this often on its own (Settings → Check now always checks). */
    const val CHECK_EVERY_MILLIS = 20L * 60 * 60 * 1000

    private val json = Json { ignoreUnknownKeys = true }

    /** Reads GitHub's release JSON; null when it has no version tag or no .apk attached. */
    fun parseLatestRelease(body: String): Release? = runCatching {
        val o = json.parseToJsonElement(body).jsonObject
        val tag = o.string("tag_name") ?: return null
        val version = AppVersion.parse(tag) ?: return null
        val apk = o["assets"]?.jsonArray?.map { it.jsonObject }
            ?.firstOrNull { it.string("name")?.endsWith(".apk", ignoreCase = true) == true } ?: return null
        Release(
            version = version,
            tag = tag,
            apkUrl = apk.string("browser_download_url") ?: return null,
            apkName = apk.string("name")!!,
            pageUrl = o.string("html_url") ?: "https://github.com/csymvoul/expensiarmus/releases/latest",
        )
    }.getOrNull()

    /** True when [release] is newer than the installed [current] version. */
    fun isNewer(release: Release?, current: String?): Boolean {
        val installed = AppVersion.parse(current) ?: return release != null
        return release != null && release.version > installed
    }

    /** Time for the daily check: never checked, or the last check was [CHECK_EVERY_MILLIS] ago (or the clock went back). */
    fun isDue(lastCheckMillis: Long, nowMillis: Long): Boolean =
        lastCheckMillis <= 0 || nowMillis - lastCheckMillis >= CHECK_EVERY_MILLIS || nowMillis < lastCheckMillis

    /** Each new version is announced once: only when its tag differs from the last one a notification was sent for. */
    fun shouldNotify(release: Release?, current: String?, lastNotifiedTag: String?): Boolean =
        isNewer(release, current) && release!!.tag != lastNotifiedTag

    private fun JsonObject.string(key: String) = this[key]?.jsonPrimitive?.takeIf { it.isString }?.content
}
