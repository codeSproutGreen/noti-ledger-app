package com.notiledger.webhook

import android.util.Log
import com.google.gson.Gson
import com.notiledger.data.model.NotiRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object WebhookSender {
    private const val TAG = "WebhookSender"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun send(
        url: String,
        record: NotiRecord,
        secret: String = "",
        deviceName: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = mutableMapOf<String, Any>(
                "id" to record.id,
                "type" to record.type,
                "source" to record.source,
                "sourceName" to record.sourceName,
                "title" to record.title,
                "content" to record.content,
                "timestamp" to record.timestamp
            )
            if (deviceName.isNotBlank()) {
                payload["deviceName"] = deviceName
            }

            val jsonBody = gson.toJson(payload)
            val requestBody = jsonBody.toRequestBody(jsonMediaType)

            val requestBuilder = Request.Builder()
                .url(url)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .header("User-Agent", "NotiLedger/1.0")

            // HMAC-SHA256 서명 추가 (시크릿이 있는 경우)
            if (secret.isNotBlank()) {
                val signature = hmacSha256(secret, jsonBody)
                requestBuilder.header("X-Webhook-Signature", signature)
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val success = response.isSuccessful

            if (!success) {
                Log.w(TAG, "Webhook failed: ${response.code} - ${response.message}")
            }

            response.close()
            success
        } catch (e: Exception) {
            Log.e(TAG, "Webhook error: ${e.message}", e)
            false
        }
    }

    suspend fun testConnection(url: String, secret: String = ""): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val testPayload = mapOf(
                    "id" to 0,
                    "type" to "TEST",
                    "source" to "com.notiledger",
                    "sourceName" to "NotiLedger",
                    "title" to "연결 테스트",
                    "content" to "NotiLedger 웹훅 연결 테스트",
                    "timestamp" to System.currentTimeMillis()
                )

                val jsonBody = gson.toJson(testPayload)
                val requestBody = jsonBody.toRequestBody(jsonMediaType)

                val requestBuilder = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "NotiLedger/1.0")

                if (secret.isNotBlank()) {
                    val signature = hmacSha256(secret, jsonBody)
                    requestBuilder.header("X-Webhook-Signature", signature)
                }

                val response = client.newCall(requestBuilder.build()).execute()
                val code = response.code
                response.close()

                if (code in 200..299) {
                    Result.success("연결 성공 (HTTP $code)")
                } else {
                    Result.failure(Exception("HTTP $code"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun hmacSha256(secret: String, data: String): String {
        val algorithm = "HmacSHA256"
        val mac = Mac.getInstance(algorithm)
        val keySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), algorithm)
        mac.init(keySpec)
        val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
