package com.notiledger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 앱별 알림 수집 필터 설정
 */
@Entity(tableName = "app_filters")
data class AppFilter(
    @PrimaryKey
    val packageName: String,

    /** 앱 표시 이름 */
    val appName: String,

    /** 이 앱의 알림을 수집할지 여부 */
    val isEnabled: Boolean = true,

    /** 마지막으로 알림을 받은 시간 */
    val lastSeen: Long = System.currentTimeMillis()
)
