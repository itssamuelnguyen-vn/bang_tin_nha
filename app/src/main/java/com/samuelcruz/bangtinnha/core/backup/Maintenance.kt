package com.samuelcruz.bangtinnha.core.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import com.samuelcruz.bangtinnha.core.db.AppDatabase
import com.samuelcruz.bangtinnha.core.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Monthly VACUUM, database size, and export/import of the database file. */
class Maintenance(
    private val context: Context,
    private val database: AppDatabase,
    private val settings: SettingsRepository,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val dbFile: File get() = context.getDatabasePath(AppDatabase.NAME)

    /** Rewrites the database file to give back freed space, about once a month. */
    suspend fun vacuumIfDue() {
        if (now() - settings.current().lastVacuumAt < 30L * 24 * 3600 * 1000) return
        withContext(Dispatchers.IO) { database.openHelper.writableDatabase.execSQL("VACUUM") }
        settings.setLastVacuumAt(now())
    }

    /** Size of the database and its WAL file, in bytes. */
    fun databaseSize(): Long = listOf("", "-wal", "-shm").sumOf { File(dbFile.path + it).length() }

    /** Copies the whole database into [target] (a file the user picked). */
    suspend fun export(target: Uri) = withContext(Dispatchers.IO) {
        // Flush the write-ahead log into the main file so the copy is complete.
        database.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)")).close()
        context.contentResolver.openOutputStream(target, "w")?.use { out -> dbFile.inputStream().use { it.copyTo(out) } }
            ?: throw IOException("Không mở được file để ghi")
    }

    /**
     * Replaces the database with [source] and restarts the app. Checks the SQLite header first so a wrong file
     * cannot wipe the data.
     */
    suspend fun import(source: Uri) = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(source)?.use { it.readBytes() }
            ?: throw IOException("Không đọc được file")
        if (bytes.size < 16 || String(bytes, 0, 15) != "SQLite format 3") throw IOException("File không phải database SQLite")
        database.close()
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
        dbFile.writeBytes(bytes)
        restart()
    }

    private fun restart() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
