package de.syss.MifareClassicTool.ui.uidlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.components.MctTopAppBar

data class UidLogItem(
    val timestamp: String,
    val uid: String,
    val rawLine: String
)

@Composable
fun UidLogScreen(
    logEntries: List<UidLogItem>,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onClearClick: () -> Unit,
    onCopyUid: (String) -> Unit,
    onCopyAll: () -> Unit
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MctTopAppBar(
                title = stringResource(R.string.title_activity_uid_log_tool),
                onBackClick = onBackClick,
                actions = {
                    if (logEntries.isNotEmpty()) {
                        IconButton(
                            onClick = onShareClick,
                            modifier = Modifier.testTag("btn_share_logs")
                        ) {
                            Text(
                                text = "📤",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.testTag("btn_clear_logs")
                        ) {
                            Text(
                                text = "🗑",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (logEntries.isEmpty()) {
                // Empty state card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .testTag("empty_logs_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.text_no_uid_logs),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "扫描或靠近 NFC 标签时，芯片 UID 将自动按时间戳记录在此",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "共 ${logEntries.size} 条记录 (最新排前)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("text_log_count")
                    )
                    TextButton(
                        onClick = onCopyAll,
                        modifier = Modifier.testTag("btn_copy_all")
                    ) {
                        Text(text = "复制全部")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("list_uid_logs"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logEntries) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) { onCopyUid(item.uid) }
                                .testTag("log_item_${item.uid}"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.uid,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.timestamp,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                                Text(
                                    text = "复制",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(stringResource(R.string.action_clear_logs)) },
            text = { Text("确定要清除所有已记录的 UID 日志吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearClick()
                    },
                    modifier = Modifier.testTag("btn_confirm_clear")
                ) {
                    Text(text = stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearConfirmDialog = false },
                    modifier = Modifier.testTag("btn_cancel_clear")
                ) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
