package com.notiledger.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notiledger.data.model.AppFilter
import com.notiledger.ui.viewmodel.MainViewModel
import com.notiledger.util.DateUtils
import com.notiledger.util.FinanceApps

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(viewModel: MainViewModel) {
    val filters by viewModel.appFilters.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var filterToDelete by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.presetStatus) {
        uiState.presetStatus?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearPresetStatus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        TopAppBar(
            title = {
                Column {
                    Text("앱 필터", fontWeight = FontWeight.Bold)
                    Text(
                        "알림을 수집할 앱을 선택하세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.addFilterPresets() }) {
                    Icon(Icons.Outlined.AddCircle, contentDescription = "프리셋 추가")
                }
            }
        )

        if (filters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "등록된 앱이 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "알림이 수신되면 자동으로 앱이 등록됩니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            val sortedFilters = filters.sortedWith(
                compareByDescending<AppFilter> {
                    FinanceApps.isFinanceApp(it.packageName)
                }.thenBy { it.appName }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val financeApps = sortedFilters.filter { FinanceApps.isFinanceApp(it.packageName) }
                if (financeApps.isNotEmpty()) {
                    item {
                        Text(
                            "금융 앱",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(financeApps, key = { it.packageName }) { filter ->
                        FilterCard(
                            appName = filter.appName,
                            packageName = filter.packageName,
                            isEnabled = filter.isEnabled,
                            lastSeen = filter.lastSeen,
                            isFinance = true,
                            onToggle = { viewModel.setAppEnabled(filter.packageName, it) },
                            onLongClick = { filterToDelete = filter.packageName }
                        )
                    }
                }

                val otherApps = sortedFilters.filter { !FinanceApps.isFinanceApp(it.packageName) }
                if (otherApps.isNotEmpty()) {
                    item {
                        Text(
                            "기타 앱",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(otherApps, key = { it.packageName }) { filter ->
                        FilterCard(
                            appName = filter.appName,
                            packageName = filter.packageName,
                            isEnabled = filter.isEnabled,
                            lastSeen = filter.lastSeen,
                            isFinance = false,
                            onToggle = { viewModel.setAppEnabled(filter.packageName, it) },
                            onLongClick = { filterToDelete = filter.packageName }
                        )
                    }
                }
            }
        }
    } // Column
    } // Scaffold

    // 삭제 확인 다이얼로그
    filterToDelete?.let { pkg ->
        val name = filters.find { it.packageName == pkg }?.appName ?: pkg
        AlertDialog(
            onDismissRequest = { filterToDelete = null },
            title = { Text("필터 삭제") },
            text = { Text("\"${name}\"을(를) 필터 목록에서 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFilter(pkg)
                    filterToDelete = null
                }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { filterToDelete = null }) {
                    Text("취소")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilterCard(
    appName: String,
    packageName: String,
    isEnabled: Boolean,
    lastSeen: Long,
    isFinance: Boolean,
    onToggle: (Boolean) -> Unit,
    onLongClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.combinedClickable(
            onClick = {},
            onLongClick = onLongClick
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        appName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (isFinance) {
                        Spacer(modifier = Modifier.width(6.dp))
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    "금융",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier
                        )
                    }
                }
                Text(
                    packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    "마지막 알림: ${DateUtils.formatSmart(lastSeen)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}
