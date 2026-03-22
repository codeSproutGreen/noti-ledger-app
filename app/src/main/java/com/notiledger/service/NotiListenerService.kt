package com.notiledger.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.notiledger.NotiLedgerApp
import com.notiledger.data.model.NotiRecord
import com.notiledger.util.FinanceApps
import com.notiledger.webhook.WebhookSender
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class NotiListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotiListenerService"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val repository by lazy {
        (application as NotiLedgerApp).repository
    }

    private val settings by lazy {
        (application as NotiLedgerApp).settingsRepository
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        // 시스템 알림 무시
        if (sbn.packageName == packageName) return
        if (sbn.packageName == "android") return

        scope.launch {
            try {
                // 알림 수집이 꺼져있으면 무시
                val collectEnabled = settings.collectNotifications.first()
                if (!collectEnabled) return@launch

                // 금융앱이 아니면 무시 (기타앱 차단)
                if (!FinanceApps.isFinanceApp(sbn.packageName)) return@launch

                // 앱 필터 확인
                val isEnabled = repository.isAppEnabled(sbn.packageName)
                if (!isEnabled) return@launch

                val notification = sbn.notification
                val extras = notification.extras

                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

                // 내용이 없는 알림은 무시
                val content = bigText ?: text
                if (content.isBlank() && title.isBlank()) return@launch

                // 금융 키워드 필터: 관련 문구 없으면 무시
                val combined = "$title $content"
                val financeKeywords = listOf("승인", "출금", "입금", "결제", "이체", "취소")
                if (financeKeywords.none { combined.contains(it) }) return@launch

                // 금액 정보(n원)가 없는 메시지는 무시
                if (!Regex("\\d[\\d,]*원").containsMatchIn(combined)) return@launch

                // 메시지 앱인 경우: 금융사 이름 추출 + SMS 중복 체크
                val isMessaging = FinanceApps.isMessagingApp(sbn.packageName)
                if (isMessaging) {
                    // 같은 내용이 SMS로 이미 저장됐으면 중복 무시
                    val dupWindow = 60_000L // 1분
                    if (repository.hasDuplicate(content, sbn.postTime - dupWindow, sbn.postTime + dupWindow)) {
                        Log.d(TAG, "Skipping duplicate (already saved as SMS): ${content.take(30)}")
                        return@launch
                    }
                }

                // 앱 이름 가져오기
                val appName = if (isMessaging) {
                    // 메시지 앱이면 내용에서 금융사 이름 추출
                    FinanceApps.extractFinanceName(content) ?: try {
                        val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (e: PackageManager.NameNotFoundException) {
                        sbn.packageName
                    }
                } else {
                    try {
                        val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (e: PackageManager.NameNotFoundException) {
                        sbn.packageName
                    }
                }

                // 앱을 필터 목록에 등록 (처음 보는 앱이면)
                repository.registerApp(sbn.packageName, appName)

                // DB에 저장
                val record = NotiRecord(
                    type = if (isMessaging) "SMS" else "NOTIFICATION",
                    source = sbn.packageName,
                    sourceName = appName,
                    title = title,
                    content = content,
                    timestamp = sbn.postTime
                )

                val id = repository.insertRecord(record)

                // 웹훅 전송
                sendWebhook(record.copy(id = id))

                Log.d(TAG, "Saved notification from $appName: $title")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // 알림 제거 시 별도 처리 없음
    }

    private suspend fun sendWebhook(record: NotiRecord) {
        try {
            val webhookEnabled = settings.webhookEnabled.first()
            if (!webhookEnabled) return

            val url = settings.webhookUrl.first()
            if (url.isBlank()) return

            val secret = settings.webhookSecret.first()
            val deviceName = settings.deviceName.first()
            val success = WebhookSender.send(url, record, secret, deviceName)

            if (success) {
                repository.markWebhookSent(record.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Webhook send error", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
