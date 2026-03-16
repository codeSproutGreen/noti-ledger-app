package com.notiledger.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notiledger.NotiLedgerApp
import com.notiledger.data.model.AppFilter
import com.notiledger.data.model.NotiRecord
import com.notiledger.webhook.WebhookSender
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainUiState(
    val searchQuery: String = "",
    val selectedFilter: RecordFilter = RecordFilter.ALL,
    val backupStatus: String? = null,
    val webhookTestResult: String? = null
)

enum class RecordFilter(val label: String) {
    ALL("전체"),
    NOTIFICATION("알림"),
    SMS("SMS")
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NotiLedgerApp
    private val repository = app.repository
    private val settings = app.settingsRepository
    private val backupManager = app.backupManager

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // ── Records ──
    val records: StateFlow<List<NotiRecord>> = combine(
        _uiState.map { it.searchQuery }.distinctUntilChanged(),
        _uiState.map { it.selectedFilter }.distinctUntilChanged()
    ) { query, filter -> Pair(query, filter) }
        .flatMapLatest { (query, filter) ->
            when {
                query.isNotBlank() -> repository.searchRecords(query)
                filter == RecordFilter.NOTIFICATION -> repository.getRecordsByType("NOTIFICATION")
                filter == RecordFilter.SMS -> repository.getRecordsByType("SMS")
                else -> repository.getAllRecords()
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val unreadCount: StateFlow<Int> = repository.getUnreadCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // ── Filters ──
    val appFilters: StateFlow<List<AppFilter>> = repository.getAllFilters()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ── Settings ──
    val webhookEnabled = settings.webhookEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, false)
    val webhookUrl = settings.webhookUrl
        .stateIn(viewModelScope, SharingStarted.Lazily, "")
    val webhookSecret = settings.webhookSecret
        .stateIn(viewModelScope, SharingStarted.Lazily, "")
    val collectNotifications = settings.collectNotifications
        .stateIn(viewModelScope, SharingStarted.Lazily, true)
    val collectSms = settings.collectSms
        .stateIn(viewModelScope, SharingStarted.Lazily, true)
    val smsFilterAuthOnly = settings.smsFilterAuthOnly
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    // ── Actions ──
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFilter(filter: RecordFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun markAsRead(id: Long) {
        viewModelScope.launch { repository.markAsRead(id) }
    }

    fun deleteRecord(record: NotiRecord) {
        viewModelScope.launch { repository.deleteRecord(record) }
    }

    fun deleteAllRecords() {
        viewModelScope.launch { repository.deleteAllRecords() }
    }

    // ── Filter actions ──
    fun setAppEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch { repository.setAppEnabled(packageName, enabled) }
    }

    // ── Settings actions ──
    fun setWebhookEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setWebhookEnabled(enabled) }
    }

    fun setWebhookUrl(url: String) {
        viewModelScope.launch { settings.setWebhookUrl(url) }
    }

    fun setWebhookSecret(secret: String) {
        viewModelScope.launch { settings.setWebhookSecret(secret) }
    }

    fun setCollectNotifications(enabled: Boolean) {
        viewModelScope.launch { settings.setCollectNotifications(enabled) }
    }

    fun setCollectSms(enabled: Boolean) {
        viewModelScope.launch { settings.setCollectSms(enabled) }
    }

    fun setSmsFilterAuthOnly(authOnly: Boolean) {
        viewModelScope.launch { settings.setSmsFilterAuthOnly(authOnly) }
    }

    fun testWebhook() {
        viewModelScope.launch {
            _uiState.update { it.copy(webhookTestResult = "테스트 중...") }
            val url = webhookUrl.value
            val secret = webhookSecret.value

            if (url.isBlank()) {
                _uiState.update { it.copy(webhookTestResult = "웹훅 URL을 입력해주세요") }
                return@launch
            }

            val result = WebhookSender.testConnection(url, secret)
            _uiState.update {
                it.copy(
                    webhookTestResult = result.fold(
                        onSuccess = { msg -> "✓ $msg" },
                        onFailure = { err -> "✗ 실패: ${err.message}" }
                    )
                )
            }
        }
    }

    fun clearWebhookTestResult() {
        _uiState.update { it.copy(webhookTestResult = null) }
    }

    // ── Backup ──
    fun createBackup(onComplete: (Result<java.io.File>) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(backupStatus = "백업 생성 중...") }
            val result = backupManager.createBackup()
            _uiState.update {
                it.copy(
                    backupStatus = result.fold(
                        onSuccess = { file -> "백업 완료: ${file.name}" },
                        onFailure = { err -> "백업 실패: ${err.message}" }
                    )
                )
            }
            onComplete(result)
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(backupStatus = "복원 중...") }
            val result = backupManager.restoreBackup(uri)
            _uiState.update {
                it.copy(
                    backupStatus = result.fold(
                        onSuccess = { data ->
                            "복원 완료: ${data.recordCount}건의 기록, ${data.filterCount}개의 필터"
                        },
                        onFailure = { err -> "복원 실패: ${err.message}" }
                    )
                )
            }
        }
    }

    fun clearBackupStatus() {
        _uiState.update { it.copy(backupStatus = null) }
    }
}
