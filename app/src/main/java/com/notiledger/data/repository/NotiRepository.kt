package com.notiledger.data.repository

import com.notiledger.data.db.AppFilterDao
import com.notiledger.data.db.NotiRecordDao
import com.notiledger.data.model.AppFilter
import com.notiledger.data.model.NotiRecord
import kotlinx.coroutines.flow.Flow

class NotiRepository(
    private val recordDao: NotiRecordDao,
    private val filterDao: AppFilterDao
) {
    // ── Records ──
    fun getAllRecords(): Flow<List<NotiRecord>> = recordDao.getAllRecords()
    fun getRecordsByType(type: String): Flow<List<NotiRecord>> = recordDao.getRecordsByType(type)
    fun getRecordsBySource(source: String): Flow<List<NotiRecord>> = recordDao.getRecordsBySource(source)
    fun searchRecords(query: String): Flow<List<NotiRecord>> = recordDao.searchRecords(query)
    fun getRecordsByDateRange(from: Long, to: Long): Flow<List<NotiRecord>> = recordDao.getRecordsByDateRange(from, to)
    fun getUnreadCount(): Flow<Int> = recordDao.getUnreadCount()

    suspend fun insertRecord(record: NotiRecord): Long = recordDao.insert(record)
    suspend fun deleteRecord(record: NotiRecord) = recordDao.delete(record)
    suspend fun deleteAllRecords() = recordDao.deleteAll()
    suspend fun markAsRead(id: Long) = recordDao.markAsRead(id)
    suspend fun markWebhookSent(id: Long) = recordDao.markWebhookSent(id)
    suspend fun getAllRecordsSync(): List<NotiRecord> = recordDao.getAllRecordsSync()
    suspend fun insertAllRecords(records: List<NotiRecord>) = recordDao.insertAll(records)
    suspend fun getRecordCount(): Int = recordDao.getCount()

    // ── Filters ──
    fun getAllFilters(): Flow<List<AppFilter>> = filterDao.getAllFilters()

    suspend fun isAppEnabled(packageName: String): Boolean {
        return filterDao.isAppEnabled(packageName) ?: true // 새 앱은 기본 활성화
    }

    suspend fun registerApp(packageName: String, appName: String) {
        filterDao.insert(AppFilter(packageName = packageName, appName = appName))
        filterDao.updateLastSeen(packageName, System.currentTimeMillis())
    }

    suspend fun setAppEnabled(packageName: String, enabled: Boolean) {
        filterDao.setEnabled(packageName, enabled)
    }

    suspend fun getAllFiltersSync(): List<AppFilter> = filterDao.getAllFiltersSync()
    suspend fun deleteAllFilters() = filterDao.deleteAll()
    suspend fun insertAllFilters(filters: List<AppFilter>) = filterDao.insertAll(filters)
}
