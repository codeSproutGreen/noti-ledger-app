package com.notiledger.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.notiledger.NotiLedgerApp
import com.notiledger.data.model.NotiRecord
import com.notiledger.webhook.WebhookSender
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val app = context.applicationContext as? NotiLedgerApp ?: return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // 같은 발신자의 메시지 조합 (긴 SMS는 여러 파트로 옴)
        val grouped = messages.groupBy { it.originatingAddress ?: "unknown" }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val collectSms = app.settingsRepository.collectSms.first()
                if (!collectSms) return@launch

                val authOnly = app.settingsRepository.smsFilterAuthOnly.first()

                for ((sender, parts) in grouped) {
                    val fullBody = parts.joinToString("") { it.messageBody ?: "" }
                    val timestamp = parts.firstOrNull()?.timestampMillis
                        ?: System.currentTimeMillis()

                    // 인증번호만 필터링 모드
                    if (authOnly && !isAuthMessage(fullBody)) continue

                    // 금융 SMS 필터: "승인" 문구 없으면 무시
                    if (!isFinanceSms(fullBody)) continue

                    val record = NotiRecord(
                        type = "SMS",
                        source = sender,
                        sourceName = sender,
                        title = "SMS from $sender",
                        content = fullBody,
                        timestamp = timestamp
                    )

                    val id = app.repository.insertRecord(record)

                    // 웹훅 전송
                    sendWebhook(app, record.copy(id = id))

                    Log.d(TAG, "Saved SMS from $sender: ${fullBody.take(50)}...")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun isFinanceSms(body: String): Boolean {
        val keywords = listOf("승인", "출금", "입금", "결제", "이체")
        return keywords.any { body.contains(it) }
    }

    private fun isAuthMessage(body: String): Boolean {
        val patterns = listOf(
            "인증", "인증번호", "verification", "verify",
            "OTP", "otp", "본인확인", "확인코드",
            "코드", "code", "승인번호", "보안코드",
            "일회용", "비밀번호"
        )
        return patterns.any { body.contains(it, ignoreCase = true) }
    }

    private suspend fun sendWebhook(app: NotiLedgerApp, record: NotiRecord) {
        try {
            val webhookEnabled = app.settingsRepository.webhookEnabled.first()
            Log.d(TAG, "Webhook enabled: $webhookEnabled")
            if (!webhookEnabled) return

            val url = app.settingsRepository.webhookUrl.first()
            Log.d(TAG, "Webhook URL: $url")
            if (url.isBlank()) return

            val secret = app.settingsRepository.webhookSecret.first()
            val deviceName = app.settingsRepository.deviceName.first()
            Log.d(TAG, "Sending webhook for record id=${record.id}")
            val success = WebhookSender.send(url, record, secret, deviceName)
            Log.d(TAG, "Webhook result: $success")

            if (success) {
                app.repository.markWebhookSent(record.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Webhook send error", e)
        }
    }
}
