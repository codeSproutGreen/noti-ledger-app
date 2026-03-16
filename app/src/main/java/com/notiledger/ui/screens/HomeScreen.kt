package com.notiledger.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notiledger.data.model.NotiRecord
import com.notiledger.ui.viewmodel.MainViewModel
import com.notiledger.ui.viewmodel.RecordFilter
import com.notiledger.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val records by viewModel.records.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    var showSearch by remember { mutableStateOf(false) }
    var expandedRecordId by remember { mutableStateOf<Long?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<NotiRecord?>(null) }

    Scaffold { scaffoldPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {
        // 상단 바
        TopAppBar(
            title = {
                if (showSearch) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("검색...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                } else {
                    Column {
                        Text("NotiLedger", fontWeight = FontWeight.Bold)
                        if (unreadCount > 0) {
                            Text(
                                "읽지 않은 알림 ${unreadCount}건",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            actions = {
                IconButton(onClick = {
                    showSearch = !showSearch
                    if (!showSearch) viewModel.setSearchQuery("")
                }) {
                    Icon(
                        if (showSearch) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = "검색"
                    )
                }
            }
        )

        // 필터 탭
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RecordFilter.entries.forEach { filter ->
                FilterChip(
                    selected = uiState.selectedFilter == filter,
                    onClick = { viewModel.setFilter(filter) },
                    label = { Text(filter.label) },
                    leadingIcon = {
                        if (uiState.selectedFilter == filter) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "${records.size}건",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }

        // 기록 목록
        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (uiState.searchQuery.isNotBlank()) "검색 결과가 없습니다"
                        else "수집된 알림이 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "알림 접근 권한을 확인해주세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    NotiRecordCard(
                        record = record,
                        isExpanded = expandedRecordId == record.id,
                        onClick = {
                            expandedRecordId = if (expandedRecordId == record.id) null else record.id
                            if (!record.isRead) viewModel.markAsRead(record.id)
                        },
                        onDelete = {
                            recordToDelete = record
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog && recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("기록 삭제") },
            text = { Text("이 기록을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        recordToDelete?.let { viewModel.deleteRecord(it) }
                        showDeleteDialog = false
                        recordToDelete = null
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
    } // Scaffold
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotiRecordCard(
    record: NotiRecord,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!record.isRead)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (record.type == "SMS")
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (record.type == "SMS") Icons.Outlined.Sms
                        else Icons.Outlined.Notifications,
                        contentDescription = record.type,
                        modifier = Modifier.size(18.dp),
                        tint = if (record.type == "SMS")
                            MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        record.sourceName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (!record.isRead) FontWeight.Bold else FontWeight.Normal
                    )
                    if (record.title.isNotBlank() && record.title != record.sourceName) {
                        Text(
                            record.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        DateUtils.formatSmart(record.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (record.webhookSent) {
                        Icon(
                            Icons.Filled.CloudDone,
                            contentDescription = "웹훅 전송됨",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 본문 미리보기 / 전체 표시
            Text(
                record.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis
            )

            // 확장 시 삭제 버튼
            AnimatedVisibility(visible = isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("삭제")
                    }
                }
            }
        }
    }
}
