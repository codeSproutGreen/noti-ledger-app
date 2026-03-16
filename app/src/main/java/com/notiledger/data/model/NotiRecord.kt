package com.notiledger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 알림/SMS 기록을 저장하는 엔티티
 */
@Entity(tableName = "noti_records")
data class NotiRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** "NOTIFICATION" 또는 "SMS" */
    val type: String,

    /** 앱 패키지명 (알림) 또는 발신번호 (SMS) */
    val source: String,

    /** 앱 이름 또는 발신자 */
    val sourceName: String,

    /** 알림 제목 또는 SMS 제목 */
    val title: String,

    /** 알림/SMS 본문 */
    val content: String,

    /** 수신 시간 (epoch millis) */
    val timestamp: Long = System.currentTimeMillis(),

    /** 웹훅 전송 여부 */
    val webhookSent: Boolean = false,

    /** 사용자가 읽었는지 여부 */
    val isRead: Boolean = false
)
