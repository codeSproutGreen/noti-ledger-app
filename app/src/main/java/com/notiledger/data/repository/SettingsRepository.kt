package com.notiledger.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        // 웹훅 설정
        val WEBHOOK_ENABLED = booleanPreferencesKey("webhook_enabled")
        val WEBHOOK_URL = stringPreferencesKey("webhook_url")
        val WEBHOOK_SECRET = stringPreferencesKey("webhook_secret")

        // 수집 설정
        val COLLECT_NOTIFICATIONS = booleanPreferencesKey("collect_notifications")
        val COLLECT_SMS = booleanPreferencesKey("collect_sms")

        // SMS 수집 필터 (모두 / 인증번호만)
        val SMS_FILTER_AUTH_ONLY = booleanPreferencesKey("sms_filter_auth_only")

        // 서비스 상태
        val SERVICE_RUNNING = booleanPreferencesKey("service_running")
    }

    val webhookEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[WEBHOOK_ENABLED] ?: false
    }

    val webhookUrl: Flow<String> = context.dataStore.data.map {
        it[WEBHOOK_URL] ?: ""
    }

    val webhookSecret: Flow<String> = context.dataStore.data.map {
        it[WEBHOOK_SECRET] ?: ""
    }

    val collectNotifications: Flow<Boolean> = context.dataStore.data.map {
        it[COLLECT_NOTIFICATIONS] ?: true
    }

    val collectSms: Flow<Boolean> = context.dataStore.data.map {
        it[COLLECT_SMS] ?: true
    }

    val smsFilterAuthOnly: Flow<Boolean> = context.dataStore.data.map {
        it[SMS_FILTER_AUTH_ONLY] ?: false
    }

    val serviceRunning: Flow<Boolean> = context.dataStore.data.map {
        it[SERVICE_RUNNING] ?: false
    }

    suspend fun setWebhookEnabled(enabled: Boolean) {
        context.dataStore.edit { it[WEBHOOK_ENABLED] = enabled }
    }

    suspend fun setWebhookUrl(url: String) {
        context.dataStore.edit { it[WEBHOOK_URL] = url }
    }

    suspend fun setWebhookSecret(secret: String) {
        context.dataStore.edit { it[WEBHOOK_SECRET] = secret }
    }

    suspend fun setCollectNotifications(enabled: Boolean) {
        context.dataStore.edit { it[COLLECT_NOTIFICATIONS] = enabled }
    }

    suspend fun setCollectSms(enabled: Boolean) {
        context.dataStore.edit { it[COLLECT_SMS] = enabled }
    }

    suspend fun setSmsFilterAuthOnly(authOnly: Boolean) {
        context.dataStore.edit { it[SMS_FILTER_AUTH_ONLY] = authOnly }
    }

    suspend fun setServiceRunning(running: Boolean) {
        context.dataStore.edit { it[SERVICE_RUNNING] = running }
    }

    suspend fun getWebhookConfig(): WebhookConfig {
        var config = WebhookConfig()
        context.dataStore.data.collect { prefs ->
            config = WebhookConfig(
                enabled = prefs[WEBHOOK_ENABLED] ?: false,
                url = prefs[WEBHOOK_URL] ?: "",
                secret = prefs[WEBHOOK_SECRET] ?: ""
            )
        }
        return config
    }
}

data class WebhookConfig(
    val enabled: Boolean = false,
    val url: String = "",
    val secret: String = ""
)
