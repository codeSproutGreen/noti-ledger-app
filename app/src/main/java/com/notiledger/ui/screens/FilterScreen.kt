package com.notiledger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

    Column(modifier = Modifier.fillMaxSize()) {
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
                            onToggle = { viewModel.setAppEnabled(filter.packageName, it) }
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
                            onToggle = { viewModel.setAppEnabled(filter.packageName, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterCard(
    appName: String,
    packageName: String,
    isEnabled: Boolean,
    lastSeen: Long,
    isFinance: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
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
