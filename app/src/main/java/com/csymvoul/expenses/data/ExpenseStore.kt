package com.csymvoul.expenses.data

import com.csymvoul.expenses.model.AppData
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Keeps all app data in one JSON file in the app's private storage. */
class ExpenseStore(private val file: File) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): AppData {
        if (!file.exists()) return AppData()
        return try {
            json.decodeFromString(AppData.serializer(), file.readText()).let { it.copy(goals = it.goals.migrated()) }
        } catch (e: Exception) {
            // Keep the unreadable file so the next save does not destroy it.
            file.copyTo(File(file.parentFile, "${file.name}.corrupt-${System.currentTimeMillis()}"), overwrite = true)
            AppData()
        }
    }

    /** Writes to a temp file first, then swaps it in, so a crash mid-write cannot corrupt the data. */
    fun save(data: AppData) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), data))
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }
}
