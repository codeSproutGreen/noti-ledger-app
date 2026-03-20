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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainUiState(
    val searchQuery: String = "",
    val selectedFilter: RecordFilter = RecordFilter.ALL,
    val backupStatus: String? = null,
    val webhookTestResult: String? = null,
    val manualSendStatus: String? = null,
    val presetStatus: String? = null
)

enum class RecordFilter(val label: String) {
    ALL("전체"),
    NOTIFICATION("알림"),
    SMS("SMS"),
    UNSENT("미전송")
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
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
                filter == RecordFilter.UNSENT -> repository.getUnsentRecords()
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

    private val _webhookUrl = MutableStateFlow("")
    val webhookUrl: StateFlow<String> = _webhookUrl.asStateFlow()

    private val _webhookSecret = MutableStateFlow("")
    val webhookSecret: StateFlow<String> = _webhookSecret.asStateFlow()

    private val _deviceName = MutableStateFlow("")
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    init {
        // Load initial values from DataStore
        viewModelScope.launch {
            settings.webhookUrl.first().let { _webhookUrl.value = it }
        }
        viewModelScope.launch {
            settings.webhookSecret.first().let { _webhookSecret.value = it }
        }
        viewModelScope.launch {
            settings.deviceName.first().let { _deviceName.value = it }
        }
        // Debounce saves to DataStore
        viewModelScope.launch {
            _webhookUrl.debounce(500).collectLatest { settings.setWebhookUrl(it) }
        }
        viewModelScope.launch {
            _webhookSecret.debounce(500).collectLatest { settings.setWebhookSecret(it) }
        }
        viewModelScope.launch {
            _deviceName.debounce(500).collectLatest { settings.setDeviceName(it) }
        }
    }
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

    fun deleteFilter(packageName: String) {
        viewModelScope.launch { repository.deleteFilter(packageName) }
    }

    // ── Settings actions ──
    fun setWebhookEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setWebhookEnabled(enabled) }
    }

    fun setWebhookUrl(url: String) {
        _webhookUrl.value = url
    }

    fun setWebhookSecret(secret: String) {
        _webhookSecret.value = secret
    }

    fun setDeviceName(name: String) {
        _deviceName.value = name
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

    // ── Manual webhook send ──
    fun sendRecordsManually(recordIds: Set<Long>) {
        viewModelScope.launch {
            val url = webhookUrl.value
            val secret = webhookSecret.value
            val device = _deviceName.value

            if (url.isBlank()) {
                _uiState.update { it.copy(manualSendStatus = "웹훅 URL을 설정해주세요") }
                return@launch
            }

            _uiState.update { it.copy(manualSendStatus = "전송 중... (0/${recordIds.size})") }

            var successCount = 0
            var failCount = 0
            val targetRecords = records.value.filter { it.id in recordIds }

            for (record in targetRecords) {
                val success = WebhookSender.send(url, record, secret, device)
                if (success) {
                    repository.markWebhookSent(record.id)
                    successCount++
                } else {
                    failCount++
                }
                _uiState.update {
                    it.copy(manualSendStatus = "전송 중... (${successCount + failCount}/${recordIds.size})")
                }
            }

            _uiState.update {
                it.copy(
                    manualSendStatus = if (failCount == 0) "전송 완료: ${successCount}건"
                    else "완료: 성공 ${successCount}건, 실패 ${failCount}건"
                )
            }
        }
    }

    fun clearManualSendStatus() {
        _uiState.update { it.copy(manualSendStatus = null) }
    }

    fun deleteRecordsByIds(ids: Set<Long>) {
        viewModelScope.launch {
            val targets = records.value.filter { it.id in ids }
            for (record in targets) {
                repository.deleteRecord(record)
            }
        }
    }

    // ── Filter presets ──
    fun addFilterPresets() {
        viewModelScope.launch {
            val presets = mapOf(
                // 은행
                "com.kbstar.kbbank" to Pair("KB국민은행", true),
                "com.shinhan.sbanking" to Pair("신한SOL뱅크", true),
                "com.wooribank.smart.banking" to Pair("우리WON뱅킹", true),
                "com.hanabank.ebk.channel.android.hananbank" to Pair("하나은행", true),
                "com.nh.cashcow" to Pair("NH스마트뱅킹", true),
                "com.ibk.neobanking" to Pair("IBK기업은행", true),
                "com.epost.psf.sdsi" to Pair("우체국금융", true),
                "com.kbankwith" to Pair("케이뱅크", true),
                "com.kakaobank.channel" to Pair("카카오뱅크", true),
                // 핀테크/페이
                "viva.republica.toss" to Pair("토스", true),
                "com.nhn.android.search" to Pair("네이버페이", true),
                "com.samsung.android.spay" to Pair("삼성페이", true),
                "com.ssg.serviceapp.android.egiftcertificate" to Pair("SSG페이", true),
                "com.lottemembers.android" to Pair("L.pay", true),
                "com.kftc.bankpay.android" to Pair("뱅크페이", true),
                "kvp.jjy.MispAndroid320" to Pair("ISP/페이북", true),
                // 카드
                "com.hyundaicard.appcard" to Pair("현대카드", true),
                "com.shinhancard.smartshinhan" to Pair("신한카드", true),
                "com.kbcard.cxh.appcard" to Pair("KB국민카드", true),
                "com.lottecard.lottesmartpay" to Pair("롯데카드", true),
                "com.samsungcard.mpocket" to Pair("삼성카드", true),
                "kr.co.bccard.vp" to Pair("BC카드", true),
                // 노이즈 앱 (기본 비활성화)
                "com.kakao.talk" to Pair("카카오톡", false),
                "com.android.systemui" to Pair("시스템 UI", false),
                "android" to Pair("Android 시스템", false),
                "com.samsung.android.incallui" to Pair("전화", false),
                "com.samsung.android.messaging" to Pair("메시지", false),
                "com.google.android.gm" to Pair("Gmail", false),
                "com.google.android.apps.messaging" to Pair("Google 메시지", false),
            )

            for ((pkg, info) in presets) {
                val (name, enabled) = info
                repository.registerApp(pkg, name)
                repository.setAppEnabled(pkg, enabled)
            }
            val enabledCount = presets.count { it.value.second }
            _uiState.update { it.copy(presetStatus = "${enabledCount}개 금융앱 프리셋이 추가되었습니다") }
        }
    }

    fun clearPresetStatus() {
        _uiState.update { it.copy(presetStatus = null) }
    }
}
