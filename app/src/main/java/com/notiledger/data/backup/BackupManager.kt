package com.notiledger.data.backup

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.notiledger.data.model.AppFilter
import com.notiledger.data.model.NotiRecord
import com.notiledger.data.repository.NotiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class BackupData(
    val version: Int = 1,
    val createdAt: String = "",
    val deviceInfo: String = "",
    val recordCount: Int = 0,
    val filterCount: Int = 0,
    val records: List<NotiRecord> = emptyList(),
    val filters: List<AppFilter> = emptyList()
)

class BackupManager(
    private val context: Context,
    private val repository: NotiRepository
) {
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    suspend fun createBackup(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val records = repository.getAllRecordsSync()
            val filters = repository.getAllFiltersSync()

            val backup = BackupData(
                version = 1,
                createdAt = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
                ).format(Date()),
                deviceInfo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                recordCount = records.size,
                filterCount = filters.size,
                records = records,
                filters = filters
            )

            val json = gson.toJson(backup)

            val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
            val fileName = "notiledger_backup_${dateFormat.format(Date())}.json"
            val file = File(backupDir, fileName)
            file.writeText(json, Charsets.UTF_8)

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(uri: Uri): Result<BackupData> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("파일을 열 수 없습니다"))

            val json = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()

            val backup = gson.fromJson(json, BackupData::class.java)

            // 기존 데이터 삭제 후 복원
            repository.deleteAllRecords()
            repository.deleteAllFilters()

            val resetRecords = backup.records.map { it.copy(id = 0) }
            repository.insertAllRecords(resetRecords)
            repository.insertAllFilters(backup.filters)

            Result.success(backup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
