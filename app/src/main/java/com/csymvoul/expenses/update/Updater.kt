package com.csymvoul.expenses.update

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.csymvoul.expenses.MainActivity
import com.csymvoul.expenses.R
import com.csymvoul.expenses.data.AppSettings
import com.csymvoul.expenses.domain.AppUpdate
import com.csymvoul.expenses.domain.Release
import com.csymvoul.expenses.reminder.OwlNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Where the update card in Settings stands. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Downloading(val release: Release, val percent: Int) : UpdateState
    data class ReadyToInstall(val release: Release, val file: File) : UpdateState
    data class Failed(val message: String, val release: Release?) : UpdateState
}

/**
 * Checks GitHub for a newer Expensiarmus, announces it once with a notification, downloads the APK into
 * the app's cache and hands it to Android's installer, where the user confirms. Only the release list
 * and the APK are fetched; nothing about the user's expenses is sent anywhere.
 */
object Updater {
    private const val CHANNEL = "updates"
    private const val NOTIFICATION_ID = 900
    private const val PREFS = "updates"
    private const val LAST_CHECK = "last_check"
    private const val LAST_NOTIFIED = "last_notified_tag"
    private const val USER_AGENT = "Expensiarmus-Android"

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    fun currentVersion(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"

    /** The daily check, if it's on in Settings and due. Safe to call often (app start, the evening alarm). */
    suspend fun checkIfDue(context: Context) {
        if (!AppSettings.get(context).updateCheck) return
        val last = prefs(context).getLong(LAST_CHECK, 0)
        if (!AppUpdate.isDue(last, System.currentTimeMillis())) return
        check(context, notify = true)
    }

    fun checkIfDue(context: Context, scope: CoroutineScope) {
        scope.launch { checkIfDue(context.applicationContext) }
    }

    /** Asks GitHub for the latest release. With [notify], a newer version is announced once. */
    suspend fun check(context: Context, notify: Boolean): UpdateState {
        val app = context.applicationContext
        if (_state.value is UpdateState.Downloading) return _state.value
        _state.value = UpdateState.Checking
        val release = withContext(Dispatchers.IO) {
            runCatching { AppUpdate.parseLatestRelease(get(AppUpdate.LATEST_URL)) }
        }
        val next = release.fold(
            onSuccess = { r ->
                prefs(app).edit().putLong(LAST_CHECK, System.currentTimeMillis()).apply()
                val current = currentVersion(app)
                if (notify && AppUpdate.shouldNotify(r, current, prefs(app).getString(LAST_NOTIFIED, null))) {
                    if (showNotification(app, r!!)) prefs(app).edit().putString(LAST_NOTIFIED, r.tag).apply()
                }
                if (AppUpdate.isNewer(r, current)) UpdateState.Available(r!!) else UpdateState.UpToDate
            },
            onFailure = { UpdateState.Failed("Couldn't reach GitHub. Check the internet connection and try again.", null) },
        )
        _state.value = next
        return next
    }

    /** Downloads the release's APK into the app's cache, then opens Android's installer. */
    suspend fun download(context: Context, release: Release) {
        val app = context.applicationContext
        _state.value = UpdateState.Downloading(release, 0)
        val file = withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(app.cacheDir, "updates").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() } // only ever keep the newest download
                val target = File(dir, release.apkName.replace(Regex("[^A-Za-z0-9._-]"), "_"))
                val conn = open(release.apkUrl, accept = "application/octet-stream")
                try {
                    val total = conn.contentLengthLong
                    conn.inputStream.use { input ->
                        target.outputStream().use { out ->
                            val buf = ByteArray(64 * 1024)
                            var done = 0L
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                done += n
                                if (total > 0) _state.value = UpdateState.Downloading(release, (done * 100 / total).toInt())
                            }
                        }
                    }
                } finally {
                    conn.disconnect()
                }
                if (target.length() == 0L) error("empty download")
                target
            }
        }
        _state.value = file.fold(
            onSuccess = { UpdateState.ReadyToInstall(release, it) },
            onFailure = { UpdateState.Failed("The download didn't finish. Try again.", release) },
        )
        (state.value as? UpdateState.ReadyToInstall)?.let { install(app, it.file) }
    }

    /** False until the user lets Expensiarmus install apps (Android asks once). */
    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    /**
     * Opens Android's installer for the downloaded APK, where the user taps Update. If installs from
     * Expensiarmus aren't allowed yet, opens that setting instead; the user comes back and taps Install again.
     */
    fun install(context: Context, file: File) {
        val intent = if (!canInstall(context)) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
        } else {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun showNotification(context: Context, release: Release): Boolean {
        if (!OwlNotifier.canNotify(context)) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "App updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "A newer Expensiarmus is ready to install"
            },
        )
        val wizard = AppSettings.get(context).wizardWords
        val open = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_UPDATES, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_quill)
            .setColor(0xFF7B1E1E.toInt())
            .setContentTitle(if (wizard) "🦉 Owl post: Expensiarmus ${release.version} is out" else "Expensiarmus ${release.version} is available")
            .setContentText("Tap to update. Your expenses stay as they are.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
        return true
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun open(url: String, accept: String = "application/vnd.github+json"): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 30_000
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", USER_AGENT)
        setRequestProperty("Accept", accept)
        if (responseCode !in 200..299) {
            disconnect()
            error("HTTP $responseCode for $url")
        }
    }

    private fun get(url: String): String {
        val conn = open(url)
        return try {
            conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        } finally {
            conn.disconnect()
        }
    }
}
