package com.notiledger.data.db

import androidx.room.*
import com.notiledger.data.model.AppFilter
import kotlinx.coroutines.flow.Flow

@Dao
interface AppFilterDao {

    @Query("SELECT * FROM app_filters ORDER BY appName ASC")
    fun getAllFilters(): Flow<List<AppFilter>>

    @Query("SELECT * FROM app_filters WHERE isEnabled = 1")
    suspend fun getEnabledFilters(): List<AppFilter>

    @Query("SELECT isEnabled FROM app_filters WHERE packageName = :packageName")
    suspend fun isAppEnabled(packageName: String): Boolean?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(filter: AppFilter)

    @Update
    suspend fun update(filter: AppFilter)

    @Query("UPDATE app_filters SET isEnabled = :enabled WHERE packageName = :packageName")
    suspend fun setEnabled(packageName: String, enabled: Boolean)

    @Query("UPDATE app_filters SET lastSeen = :timestamp WHERE packageName = :packageName")
    suspend fun updateLastSeen(packageName: String, timestamp: Long)

    @Query("DELETE FROM app_filters")
    suspend fun deleteAll()

    @Query("SELECT * FROM app_filters ORDER BY appName ASC")
    suspend fun getAllFiltersSync(): List<AppFilter>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(filters: List<AppFilter>)
}
