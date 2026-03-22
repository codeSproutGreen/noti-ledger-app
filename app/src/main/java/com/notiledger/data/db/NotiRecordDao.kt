package com.notiledger.data.db

import androidx.room.*
import com.notiledger.data.model.NotiRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface NotiRecordDao {

    @Query("SELECT * FROM noti_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<NotiRecord>>

    @Query("SELECT * FROM noti_records WHERE type = :type ORDER BY timestamp DESC")
    fun getRecordsByType(type: String): Flow<List<NotiRecord>>

    @Query("SELECT * FROM noti_records WHERE source = :source ORDER BY timestamp DESC")
    fun getRecordsBySource(source: String): Flow<List<NotiRecord>>

    @Query("""
        SELECT * FROM noti_records
        WHERE content LIKE '%' || :query || '%'
           OR title LIKE '%' || :query || '%'
           OR sourceName LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchRecords(query: String): Flow<List<NotiRecord>>

    @Query("SELECT * FROM noti_records WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    fun getRecordsByDateRange(from: Long, to: Long): Flow<List<NotiRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: NotiRecord): Long

    @Update
    suspend fun update(record: NotiRecord)

    @Delete
    suspend fun delete(record: NotiRecord)

    @Query("DELETE FROM noti_records")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM noti_records")
    suspend fun getCount(): Int

    @Query("SELECT * FROM noti_records WHERE webhookSent = 0 ORDER BY timestamp DESC")
    fun getUnsentRecords(): Flow<List<NotiRecord>>

    @Query("SELECT COUNT(*) FROM noti_records WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("UPDATE noti_records SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE noti_records SET webhookSent = 1 WHERE id = :id")
    suspend fun markWebhookSent(id: Long)

    @Query("SELECT COUNT(*) FROM noti_records WHERE content = :content AND timestamp BETWEEN :from AND :to")
    suspend fun countDuplicates(content: String, from: Long, to: Long): Int

    // 백업용: 전체 데이터를 한번에 가져오기
    @Query("SELECT * FROM noti_records ORDER BY timestamp ASC")
    suspend fun getAllRecordsSync(): List<NotiRecord>

    // 백업 복원용
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<NotiRecord>)
}
