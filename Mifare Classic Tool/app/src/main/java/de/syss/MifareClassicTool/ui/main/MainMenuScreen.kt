package de.syss.MifareClassicTool.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.components.MctSectionHeader
import de.syss.MifareClassicTool.ui.components.MctStatusCard

enum class NfcHardwareState {
    Ready,
    Disabled,
    EditorOnly,
    Unsupported
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
    var showAboutDialog by remember { mutableStateOf(false) }

    val aboutLabel = stringResource(R.string.dialog_about_mct_title)

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
                        onClick = { showAboutDialog = true },
                        modifier = Modifier
                            .testTag("btn_about")
                            .semantics {
                                contentDescription = aboutLabel
                            }
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
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
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
            // 1. Hero NFC Status Card (KernelSU style)
            val (statusTitle, statusDesc, isReady, isWarning) = when (nfcState) {
                NfcHardwareState.Ready -> Quadruple(
                    stringResource(R.string.nfc_status_ready_title),
                    stringResource(R.string.nfc_status_ready_desc),
                    true,
                    false
                )
                NfcHardwareState.Disabled -> Quadruple(
                    stringResource(R.string.nfc_status_disabled_title),
                    stringResource(R.string.nfc_status_disabled_desc),
                    false,
                    true
                )
                NfcHardwareState.EditorOnly -> Quadruple(
                    stringResource(R.string.nfc_status_editor_title),
                    stringResource(R.string.nfc_status_editor_desc),
                    false,
                    false
                )
                NfcHardwareState.Unsupported -> Quadruple(
                    stringResource(R.string.nfc_status_unsupported_title),
                    stringResource(R.string.nfc_status_unsupported_desc),
                    false,
                    false
                )
            }

            MctStatusCard(
                title = statusTitle,
                subtitle = statusDesc,
                isReady = isReady,
                isWarning = isWarning,
                actionText = if (nfcState == NfcHardwareState.Disabled) stringResource(R.string.nfc_status_enable_button) else null,
                onActionClick = if (nfcState == NfcHardwareState.Disabled) onEnableNfcClick else null,
                iconPainter = painterResource(R.drawable.read_tag),
                modifier = Modifier
                    .testTag("nfc_status_card")
                    .semantics {
                        contentDescription = "$statusTitle, $statusDesc"
                    }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Core NFC Operations (Read & Write with Clear Visual Hierarchy)
            MctSectionHeader(title = stringResource(R.string.text_rf_tech))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MainActionCard(
                    title = stringResource(R.string.action_read_tag),
                    subtitle = stringResource(R.string.home_action_read_tag_desc),
                    icon = painterResource(R.drawable.read_tag),
                    enabled = !isEditorOnly && (nfcState == NfcHardwareState.Ready),
                    modifier = Modifier.weight(1f),
                    testTag = "btn_read_tag",
                    onClick = onReadTagClick,
                    accentColor = MaterialTheme.colorScheme.primary
                )

                MainActionCard(
                    title = stringResource(R.string.action_write_tag),
                    subtitle = stringResource(R.string.home_action_write_tag_desc),
                    icon = painterResource(R.drawable.write_tag),
                    enabled = !isEditorOnly && (nfcState == NfcHardwareState.Ready),
                    modifier = Modifier.weight(1f),
                    testTag = "btn_write_tag",
                    onClick = onWriteTagClick,
                    accentColor = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Editors & Management (Dump & Keys)
            MctSectionHeader(title = stringResource(R.string.home_group_manage))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MainActionCard(
                    title = stringResource(R.string.action_edit_tag_dump),
                    subtitle = null,
                    icon = painterResource(R.drawable.edit_dump),
                    enabled = true,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_edit_dump",
                    onClick = onOpenTagDumpEditorClick
                )

                MainActionCard(
                    title = stringResource(R.string.action_edit_key_dump),
                    subtitle = null,
                    icon = painterResource(R.drawable.edit_keys),
                    enabled = true,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_edit_keys",
                    onClick = onOpenKeyEditorClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Tools & Help
            MctSectionHeader(title = stringResource(R.string.home_group_tools))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MainActionCard(
                    title = stringResource(R.string.action_tools),
                    subtitle = null,
                    icon = painterResource(R.drawable.tools),
                    enabled = true,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_tools",
                    onClick = { showToolsDialog = true }
                )

                MainActionCard(
                    title = stringResource(R.string.action_help),
                    subtitle = null,
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

    // Modern Material 3 About Dialog
    if (showAboutDialog) {
        MctAboutDialog(onDismissRequest = { showAboutDialog = false })
    }

    // Tools Dialog (Listing all 9 sub-tools with KernelSU card styling)
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
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(R.string.dialog_tools_menu_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(toolsList) { item ->
                        val itemTitle = stringResource(item.titleRes)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    enabled = item.isEnabled,
                                    role = Role.Button,
                                    onClickLabel = itemTitle
                                ) {
                                    showToolsDialog = false
                                    item.onClick()
                                }
                                .testTag(item.testTag)
                                .semantics {
                                    contentDescription = itemTitle
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (item.isEnabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.iconRes != null) {
                                    Image(
                                        painter = painterResource(item.iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                }
                                Text(
                                    text = stringResource(item.titleRes),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = if (item.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
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
                    Text(
                        text = stringResource(R.string.action_cancel),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun MainActionCard(
    title: String,
    subtitle: String?,
    icon: Painter,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    testTag: String = "",
    accentColor: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(if (subtitle != null) 124.dp else 112.dp)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = title,
                onClick = onClick
            )
            .testTag(testTag)
            .semantics {
                contentDescription = title
            },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (enabled && accentColor != null) accentColor.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (enabled && accentColor != null) accentColor.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    alpha = if (enabled) 1f else 0.35f
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                textAlign = TextAlign.Center,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                maxLines = 1
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    textAlign = TextAlign.Center,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    maxLines = 1
                )
            }
        }
    }
}
