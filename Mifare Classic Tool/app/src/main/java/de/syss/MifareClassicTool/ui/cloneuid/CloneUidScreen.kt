package de.syss.MifareClassicTool.ui.cloneuid

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.components.MctActionButton
import de.syss.MifareClassicTool.ui.components.MctInfoCard
import de.syss.MifareClassicTool.ui.components.MctSectionCard
import de.syss.MifareClassicTool.ui.components.MctTopAppBar

data class CloneUidUiState(
    val uidInput: String = "",
    val block0RestInput: String = "080400475955D141103607",
    val writeKeyInput: String = "FFFFFFFFFFFF",
    val useKeyB: Boolean = false,
    val autoCalcSakAtqa: Boolean = false,
    val showOptions: Boolean = false,
    val block0Complete: String? = null,
    val statusLogs: List<String> = emptyList()
)

@Composable
fun CloneUidScreen(
    state: CloneUidUiState,
    onStateChange: (CloneUidUiState) -> Unit,
    onBackClick: () -> Unit,
    onPasteUid: () -> Unit,
    onRandomUid: () -> Unit,
    onCalculateBlock0: () -> Unit,
    onShowInfo: () -> Unit = {}
) {
    var showInfoDialog by remember { mutableStateOf(false) }
    var showBlock0RestInfoDialog by remember { mutableStateOf(false) }
    var showWriteKeyInfoDialog by remember { mutableStateOf(false) }
    var showSakAtqaInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MctTopAppBar(
                title = stringResource(R.string.title_activity_clone_uid),
                onBackClick = onBackClick,
                actions = {
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.testTag("btn_clone_info")
                    ) {
                        Text(
                            text = "ⓘ",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notice Card
            MctInfoCard(
                text = stringResource(R.string.text_clone_uid_info),
                isWarning = true,
                modifier = Modifier.testTag("card_clone_notice")
            )

            // 1. Target UID Input
            MctSectionCard(
                title = stringResource(R.string.text_uid_to_clone),
                modifier = Modifier.testTag("card_target_uid")
            ) {
                OutlinedTextField(
                    value = state.uidInput,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || (it in 'a'..'f') || (it in 'A'..'F') }.uppercase()
                        if (filtered.length <= 20) {
                            onStateChange(state.copy(uidInput = filtered))
                        }
                    },
                    label = { Text("4 / 7 / 10 字节十六进制 UID") },
                    placeholder = { Text(stringResource(R.string.hint_hex_4_7_10_byte)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = onPasteUid,
                                modifier = Modifier.testTag("btn_paste_uid")
                            ) {
                                Text("粘贴", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(
                                onClick = onRandomUid,
                                modifier = Modifier.testTag("btn_random_uid")
                            ) {
                                Text("随机", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_uid")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Options Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStateChange(state.copy(showOptions = !state.showOptions)) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.showOptions,
                        onCheckedChange = null,
                        modifier = Modifier.testTag("cb_show_options")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_show_options),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Advanced Options Section
                AnimatedVisibility(visible = state.showOptions) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = state.block0RestInput,
                            onValueChange = {
                                val filtered = it.filter { c -> c.isDigit() || (c in 'a'..'f') || (c in 'A'..'F') }.uppercase()
                                onStateChange(state.copy(block0RestInput = filtered))
                            },
                            label = { Text(stringResource(R.string.dialog_rest_of_block_0_title)) },
                            placeholder = { Text("080400475955D141103607") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = { showBlock0RestInfoDialog = true },
                                    modifier = Modifier.testTag("btn_info_block0_rest")
                                ) {
                                    Text(
                                        text = "ⓘ",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_block0_rest")
                        )

                        OutlinedTextField(
                            value = state.writeKeyInput,
                            onValueChange = {
                                val filtered = it.filter { c -> c.isDigit() || (c in 'a'..'f') || (c in 'A'..'F') }.uppercase()
                                if (filtered.length <= 12) {
                                    onStateChange(state.copy(writeKeyInput = filtered))
                                }
                            },
                            label = { Text(stringResource(R.string.dialog_key_for_block_0_title)) },
                            placeholder = { Text("FFFFFFFFFFFF") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = { showWriteKeyInfoDialog = true },
                                    modifier = Modifier.testTag("btn_info_write_key")
                                ) {
                                    Text(
                                        text = "ⓘ",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_write_key")
                        )

                        // Key A / Key B selection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .selectable(
                                        selected = !state.useKeyB,
                                        role = Role.RadioButton,
                                        onClick = { onStateChange(state.copy(useKeyB = false)) }
                                    )
                                    .testTag("rb_key_a")
                            ) {
                                RadioButton(selected = !state.useKeyB, onClick = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("使用 Key A")
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .selectable(
                                        selected = state.useKeyB,
                                        role = Role.RadioButton,
                                        onClick = { onStateChange(state.copy(useKeyB = true)) }
                                    )
                                    .testTag("rb_key_b")
                            ) {
                                RadioButton(selected = state.useKeyB, onClick = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("使用 Key B")
                            }
                        }

                        // SAK/ATQA checkbox
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStateChange(state.copy(autoCalcSakAtqa = !state.autoCalcSakAtqa)) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = state.autoCalcSakAtqa,
                                onCheckedChange = null,
                                modifier = Modifier.testTag("cb_sak_atqa")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.action_calc_sak_atqa),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { showSakAtqaInfoDialog = true },
                                modifier = Modifier.testTag("btn_info_sak_atqa")
                            ) {
                                Text(
                                    text = "ⓘ",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Calculate Button
                MctActionButton(
                    text = "计算 BCC 与生成 Block 0",
                    onClick = onCalculateBlock0,
                    modifier = Modifier.testTag("btn_calc_block0")
                )
            }

            // 2. Block 0 Result & Status Log
            MctSectionCard(
                title = "Block 0 预览与状态日志",
                modifier = Modifier.testTag("card_block0_log")
            ) {
                if (state.block0Complete != null) {
                    Text(
                        text = "已生成完整 Block 0:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.block0Complete,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(12.dp)
                                .testTag("text_block0_complete")
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                }

                Text(
                    text = "运行日志:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag("card_log_content")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (state.statusLogs.isEmpty()) {
                            Text(
                                text = "等待生成 Block 0 数据...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            state.statusLogs.forEach { logLine ->
                                Text(
                                    text = logLine,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(stringResource(R.string.dialog_gen2_tags_info_title)) },
            text = { Text(stringResource(R.string.dialog_gen2_tags_info)) },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        )
    }

    if (showBlock0RestInfoDialog) {
        AlertDialog(
            onDismissRequest = { showBlock0RestInfoDialog = false },
            title = { Text(stringResource(R.string.dialog_rest_of_block_0_title)) },
            text = { Text(stringResource(R.string.dialog_rest_of_block_0)) },
            confirmButton = {
                TextButton(onClick = { showBlock0RestInfoDialog = false }) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        )
    }

    if (showWriteKeyInfoDialog) {
        AlertDialog(
            onDismissRequest = { showWriteKeyInfoDialog = false },
            title = { Text(stringResource(R.string.dialog_key_for_block_0_title)) },
            text = { Text(stringResource(R.string.dialog_key_for_block_0)) },
            confirmButton = {
                TextButton(onClick = { showWriteKeyInfoDialog = false }) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        )
    }

    if (showSakAtqaInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSakAtqaInfoDialog = false },
            title = { Text(stringResource(R.string.dialog_auto_calc_sak_atqa_title)) },
            text = { Text(stringResource(R.string.dialog_auto_calc_sak_atqa)) },
            confirmButton = {
                TextButton(onClick = { showSakAtqaInfoDialog = false }) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        )
    }
}
