package com.notiledger.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.notiledger.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val webhookEnabled by viewModel.webhookEnabled.collectAsState()
    val webhookUrl by viewModel.webhookUrl.collectAsState()
    val webhookSecret by viewModel.webhookSecret.collectAsState()
    val collectNotifications by viewModel.collectNotifications.collectAsState()
    val collectSms by viewModel.collectSms.collectAsState()
    val smsFilterAuthOnly by viewModel.smsFilterAuthOnly.collectAsState()

    var showSecretField by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val backupShareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    val restoreFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.restoreBackup(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text("설정", fontWeight = FontWeight.Bold) }
        )

        SettingsSection(title = "권한 설정") {
            SettingsButton(
                icon = Icons.Outlined.Notifications,
                title = "알림 접근 권한",
                description = "알림을 읽기 위해 필요합니다",
                onClick = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    context.startActivity(intent)
                }
            )

            SettingsButton(
                icon = Icons.Outlined.BatteryStd,
                title = "배터리 최적화 예외",
                description = "백그라운드에서 안정적으로 동작합니다",
                onClick = {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    context.startActivity(intent)
                }
            )
        }

        SettingsSection(title = "수집 설정") {
            SettingsSwitch(
                title = "앱 알림 수집",
                description = "금융앱 등의 푸시 알림을 수집합니다",
                checked = collectNotifications,
                onCheckedChange = { viewModel.setCollectNotifications(it) }
            )

            SettingsSwitch(
                title = "SMS 수집",
                description = "문자 메시지를 수집합니다",
                checked = collectSms,
                onCheckedChange = { viewModel.setCollectSms(it) }
            )

            if (collectSms) {
                SettingsSwitch(
                    title = "인증번호만 수집",
                    description = "인증/OTP 관련 SMS만 저장합니다",
                    checked = smsFilterAuthOnly,
                    onCheckedChange = { viewModel.setSmsFilterAuthOnly(it) }
                )
            }
        }

        SettingsSection(title = "웹훅 설정") {
            SettingsSwitch(
                title = "웹훅 전송",
                description = "수집된 데이터를 웹훅 URL로 전송합니다",
                checked = webhookEnabled,
                onCheckedChange = { viewModel.setWebhookEnabled(it) }
            )

            if (webhookEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = webhookUrl,
                            onValueChange = { viewModel.setWebhookUrl(it) },
                            label = { Text("Webhook URL") },
                            placeholder = { Text("https://example.com/api/webhook") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = webhookSecret,
                            onValueChange = { viewModel.setWebhookSecret(it) },
                            label = { Text("Secret (HMAC 서명용, 선택)") },
                            placeholder = { Text("비밀 키") },
                            singleLine = true,
                            visualTransformation = if (showSecretField)
                                VisualTransformation.None
                            else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showSecretField = !showSecretField }) {
                                    Icon(
                                        if (showSecretField) Icons.Outlined.VisibilityOff
                                        else Icons.Outlined.Visibility,
                                        contentDescription = "비밀번호 표시"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.testWebhook() }
                            ) {
                                Icon(
                                    Icons.Outlined.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("연결 테스트")
                            }
                        }

                        uiState.webhookTestResult?.let { result ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                result,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (result.startsWith("✓"))
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        SettingsSection(title = "백업 / 복원") {
            SettingsButton(
                icon = Icons.Outlined.CloudUpload,
                title = "백업 생성",
                description = "모든 데이터를 JSON 파일로 내보냅니다",
                onClick = {
                    viewModel.createBackup { result ->
                        result.onSuccess { file ->
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            backupShareLauncher.launch(
                                Intent.createChooser(shareIntent, "백업 파일 저장")
                            )
                        }
                    }
                }
            )

            SettingsButton(
                icon = Icons.Outlined.CloudDownload,
                title = "백업 복원",
                description = "JSON 백업 파일에서 데이터를 복원합니다",
                onClick = {
                    restoreFilePicker.launch("application/json")
                }
            )

            uiState.backupStatus?.let { status ->
                Text(
                    status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        SettingsSection(title = "데이터 관리") {
            SettingsButton(
                icon = Icons.Outlined.DeleteForever,
                title = "전체 데이터 삭제",
                description = "수집된 모든 알림과 SMS 기록을 삭제합니다",
                isDestructive = true,
                onClick = { showDeleteAllDialog = true }
            )
        }

        Text(
            "NotiLedger v1.0.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            icon = { Icon(Icons.Outlined.Warning, contentDescription = null) },
            title = { Text("전체 데이터 삭제") },
            text = { Text("수집된 모든 기록이 영구적으로 삭제됩니다.\n이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllRecords()
                        showDeleteAllDialog = false
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDestructive)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDestructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
