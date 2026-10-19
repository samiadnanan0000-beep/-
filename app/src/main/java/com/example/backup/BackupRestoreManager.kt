package com.example.backup

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.data.WafiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRestoreManager(
    private val context: Context,
    private val repository: WafiRepository
) {
    companion object {
        private const val TAG = "WafiBackupManager"
        private const val BACKUP_DIR_NAME = "WafiBackup"
        private const val PREFS_NAME = "wafi_backup_prefs"
        private const val KEY_LAST_AUTO_BACKUP = "last_auto_backup_date"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getBackupDirectories(): List<File> {
        val dirs = mutableListOf<File>()
        try {
            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val publicDir = File(publicDocs, BACKUP_DIR_NAME)
            if (!publicDir.exists()) publicDir.mkdirs()
            dirs.add(publicDir)
        } catch (e: Exception) {
            Log.w(TAG, "Public documents dir inaccessible: ${e.message}")
        }

        try {
            val appDocs = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (appDocs != null) {
                val appDir = File(appDocs, BACKUP_DIR_NAME)
                if (!appDir.exists()) appDir.mkdirs()
                dirs.add(appDir)
            }
        } catch (e: Exception) {
            Log.w(TAG, "App external files dir inaccessible: ${e.message}")
        }

        val internalDir = File(context.filesDir, BACKUP_DIR_NAME)
        if (!internalDir.exists()) internalDir.mkdirs()
        dirs.add(internalDir)

        return dirs
    }

    suspend fun createBackup(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val jsonData = repository.exportDataToJson()
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
            val fileName = "wafi_backup_$dateStr.json"
            val staticFileName = "wafi_latest_backup.json"

            var savedFile: File? = null
            for (dir in getBackupDirectories()) {
                if (dir.exists() || dir.mkdirs()) {
                    val file = File(dir, fileName)
                    FileOutputStream(file).use { it.write(jsonData.toByteArray(Charsets.UTF_8)) }
                    val staticFile = File(dir, staticFileName)
                    FileOutputStream(staticFile).use { it.write(jsonData.toByteArray(Charsets.UTF_8)) }
                    if (savedFile == null) savedFile = file
                }
            }

            if (savedFile != null) {
                prefs.edit().putLong(KEY_LAST_AUTO_BACKUP, System.currentTimeMillis()).apply()
                Result.success(savedFile)
            } else {
                Result.failure(Exception("تعذر العثور على مسار لحفظ النسخة الاحتياطية"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun checkAndPerformDailyAutoBackup() = withContext(Dispatchers.IO) {
        val lastBackup = prefs.getLong(KEY_LAST_AUTO_BACKUP, 0L)
        val oneDayMillis = 24 * 60 * 60 * 1000L
        if (System.currentTimeMillis() - lastBackup > oneDayMillis) {
            Log.d(TAG, "Triggering automatic daily backup...")
            createBackup()
        }
    }

    suspend fun scanAndRestoreBackup(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val candidateFiles = mutableListOf<File>()
            for (dir in getBackupDirectories()) {
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
                    if (files != null) {
                        candidateFiles.addAll(files)
                    }
                }
            }

            if (candidateFiles.isEmpty()) {
                return@withContext Result.failure(Exception("لم يتم العثور على أي ملف نسخة احتياطية في مجلد $BACKUP_DIR_NAME"))
            }

            val latestBackup = candidateFiles.maxByOrNull { it.lastModified() }
                ?: return@withContext Result.failure(Exception("تعذر العثور على ملفات صالحة"))

            val jsonContent = FileInputStream(latestBackup).bufferedReader().use { it.readText() }
            val success = repository.restoreDataFromJson(jsonContent)
            if (success) {
                Result.success(1)
            } else {
                Result.failure(Exception("ملف النسخة الاحتياطية غير متوافق أو تالف"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup: ${e.message}", e)
            Result.failure(e)
        }
    }
}
