package com.notiledger.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.notiledger.NotiLedgerApp
import com.notiledger.data.model.NotiRecord
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

                // 앱 이름 가져오기
                val appName = try {
                    val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    sbn.packageName
                }

                // 앱을 필터 목록에 등록 (처음 보는 앱이면)
                repository.registerApp(sbn.packageName, appName)

                // DB에 저장
                val record = NotiRecord(
                    type = "NOTIFICATION",
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
            val success = WebhookSender.send(url, record, secret)

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
