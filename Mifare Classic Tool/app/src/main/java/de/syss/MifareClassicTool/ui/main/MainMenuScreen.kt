package de.syss.MifareClassicTool.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.syss.MifareClassicTool.R

enum class NfcHardwareState {
    Ready,
    Disabled,
    Unsupported,
    EditorOnly
}

private data class ToolItem(
    val titleRes: Int,
    val iconRes: Int?,
    val isEnabled: Boolean,
    val testTag: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    versionCode: String,
    isEditorOnly: Boolean,
    nfcState: NfcHardwareState,
    onReadTagClick: () -> Unit,
    onWriteTagClick: () -> Unit,
    onOpenTagDumpEditorClick: () -> Unit,
    onOpenKeyEditorClick: () -> Unit,
    onHelpClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    onAboutClick: () -> Unit,
    onEnableNfcClick: () -> Unit,
    onTagInfoClick: () -> Unit,
    onCloneUidClick: () -> Unit,
    onDiffToolClick: () -> Unit,
    onAccessConditionToolClick: () -> Unit,
    onValueBlockToolClick: () -> Unit,
    onBccToolClick: () -> Unit,
    onDataConversionToolClick: () -> Unit,
    onUidLogToolClick: () -> Unit,
    onImportExportToolClick: () -> Unit
) {
    var showToolsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_activity_main),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = onPreferencesClick,
                        modifier = Modifier.testTag("btn_preferences")
                    ) {
                        Image(
                            painter = painterResource(R.drawable.preferences),
                            contentDescription = stringResource(R.string.title_activity_preferences),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = onAboutClick,
                        modifier = Modifier.testTag("btn_about")
                    ) {
                        Text(
                            text = "ⓘ",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            // 1. NFC Hardware & Mode Banner
            NfcStatusBanner(
                nfcState = nfcState,
                onEnableNfcClick = onEnableNfcClick
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Core NFC Operations (Read & Write)
            Text(
                text = stringResource(R.string.text_rf_tech),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MainActionCard(
                    title = stringResource(R.string.action_read_tag),
                    icon = painterResource(R.drawable.read_tag),
                    enabled = !isEditorOnly && (nfcState == NfcHardwareState.Ready),
                    modifier = Modifier.weight(1f),
                    testTag = "btn_read_tag",
                    onClick = onReadTagClick
                )

                MainActionCard(
                    title = stringResource(R.string.action_write_tag),
                    icon = painterResource(R.drawable.write_tag),
                    enabled = !isEditorOnly && (nfcState == NfcHardwareState.Ready),
                    modifier = Modifier.weight(1f),
                    testTag = "btn_write_tag",
                    onClick = onWriteTagClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Editors (Dump & Keys)
            Text(
                text = stringResource(R.string.action_editor_only),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MainActionCard(
                    title = stringResource(R.string.action_edit_tag_dump),
                    icon = painterResource(R.drawable.edit_dump),
                    enabled = true,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_edit_dump",
                    onClick = onOpenTagDumpEditorClick
                )

                MainActionCard(
                    title = stringResource(R.string.action_edit_key_dump),
                    icon = painterResource(R.drawable.edit_keys),
                    enabled = true,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_edit_keys",
                    onClick = onOpenKeyEditorClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Tools & Help
            Text(
                text = stringResource(R.string.action_tools),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MainActionCard(
                    title = stringResource(R.string.action_tools),
                    icon = painterResource(R.drawable.tools),
                    enabled = true,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_tools",
                    onClick = { showToolsDialog = true }
                )

                MainActionCard(
                    title = stringResource(R.string.action_help),
                    icon = painterResource(R.drawable.help_and_info),
                    enabled = true,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_help",
                    onClick = onHelpClick
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. App Footer Info
            Text(
                text = "${stringResource(R.string.app_version)}: $versionCode",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_version_footer"),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Tools Dialog (Listing all 9 sub-tools with Material 3 styling)
    if (showToolsDialog) {
        val toolsList = listOf(
            ToolItem(R.string.action_show_tag_info, R.drawable.tag_info, !isEditorOnly, "tool_tag_info", onTagInfoClick),
            ToolItem(R.string.action_show_clone_uid, R.drawable.clone_uid, !isEditorOnly, "tool_clone_uid", onCloneUidClick),
            ToolItem(R.string.action_show_diff_tool, R.drawable.diff_tool, true, "tool_diff", onDiffToolClick),
            ToolItem(R.string.action_show_ac_tool, R.drawable.access_condition_tool, true, "tool_ac", onAccessConditionToolClick),
            ToolItem(R.string.action_show_vb_tool, R.drawable.value_block_tool, true, "tool_vb", onValueBlockToolClick),
            ToolItem(R.string.action_show_bcc_tool, R.drawable.bcc_tool, true, "tool_bcc", onBccToolClick),
            ToolItem(R.string.action_show_data_conversion_tool, null, true, "tool_data_conversion", onDataConversionToolClick),
            ToolItem(R.string.action_show_uid_log_tool, null, true, "tool_uid_log", onUidLogToolClick),
            ToolItem(R.string.action_show_import_export_tool, null, true, "tool_import_export", onImportExportToolClick)
        )

        AlertDialog(
            onDismissRequest = { showToolsDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_tools_menu_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(toolsList) { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = item.isEnabled) {
                                    showToolsDialog = false
                                    item.onClick()
                                }
                                .testTag(item.testTag),
                            shape = MaterialTheme.shapes.small,
                            color = if (item.isEnabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.iconRes != null) {
                                    Image(
                                        painter = painterResource(item.iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Text(
                                    text = stringResource(item.titleRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (item.isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showToolsDialog = false },
                    modifier = Modifier.testTag("btn_close_tools")
                ) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun NfcStatusBanner(
    nfcState: NfcHardwareState,
    onEnableNfcClick: () -> Unit
) {
    val cardColor = when (nfcState) {
        NfcHardwareState.Ready -> MaterialTheme.colorScheme.primaryContainer
        NfcHardwareState.Disabled -> MaterialTheme.colorScheme.errorContainer
        NfcHardwareState.EditorOnly -> MaterialTheme.colorScheme.surfaceVariant
        NfcHardwareState.Unsupported -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (nfcState) {
        NfcHardwareState.Ready -> MaterialTheme.colorScheme.onPrimaryContainer
        NfcHardwareState.Disabled -> MaterialTheme.colorScheme.onErrorContainer
        NfcHardwareState.EditorOnly -> MaterialTheme.colorScheme.onSurfaceVariant
        NfcHardwareState.Unsupported -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("nfc_status_card"),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val statusTitle = when (nfcState) {
                    NfcHardwareState.Ready -> "NFC 已就绪"
                    NfcHardwareState.Disabled -> "NFC 未启用"
                    NfcHardwareState.EditorOnly -> "仅编辑器模式"
                    NfcHardwareState.Unsupported -> "设备不支持 MIFARE Classic"
                }
                val statusDesc = when (nfcState) {
                    NfcHardwareState.Ready -> "贴附 MIFARE Classic 卡片即可读取转储"
                    NfcHardwareState.Disabled -> "请在系统设置中开启 NFC 开关"
                    NfcHardwareState.EditorOnly -> "NFC 功能已关闭，支持转储与密钥离线编辑"
                    NfcHardwareState.Unsupported -> "硬件缺少 MIFARE Classic 控制器支持"
                }
                Text(
                    text = statusTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statusDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.85f)
                )
            }
            if (nfcState == NfcHardwareState.Disabled) {
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onEnableNfcClick,
                    modifier = Modifier.testTag("btn_enable_nfc")
                ) {
                    Text(text = "去开启", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun MainActionCard(
    title: String,
    icon: Painter,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(118.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                alpha = if (enabled) 1f else 0.35f
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
        }
    }
}
