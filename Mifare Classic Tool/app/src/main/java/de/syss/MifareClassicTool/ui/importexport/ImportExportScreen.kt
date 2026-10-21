package de.syss.MifareClassicTool.ui.importexport

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.components.MctSectionCard
import de.syss.MifareClassicTool.ui.components.MctTopAppBar

enum class DumpFormatChoice {
    MCT,
    JSON,
    BIN,
    EML
}

enum class KeysFormatChoice {
    KEYS,
    BIN
}

@Composable
fun ImportExportScreen(
    onBackClick: () -> Unit,
    onImportDumpFormatSelected: (DumpFormatChoice) -> Unit,
    onExportDumpClick: () -> Unit,
    onImportKeysFormatSelected: (KeysFormatChoice) -> Unit,
    onExportKeysClick: () -> Unit,
    onBackupAllClick: () -> Unit
) {
    var showDumpFormatDialog by remember { mutableStateOf(false) }
    var showKeysFormatDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MctTopAppBar(
                title = stringResource(R.string.title_activity_import_export_tool),
                onBackClick = onBackClick
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
            // 1. Dump Files
            MctSectionCard(
                title = stringResource(R.string.text_dump_files),
                modifier = Modifier.testTag("card_dump_operations")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showDumpFormatDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_import_dump")
                    ) {
                        Text(text = stringResource(R.string.action_import_dump))
                    }

                    Button(
                        onClick = onExportDumpClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_export_dump")
                    ) {
                        Text(text = stringResource(R.string.action_export_dump))
                    }
                }
            }

            // 2. Key Files
            MctSectionCard(
                title = stringResource(R.string.text_key_files),
                modifier = Modifier.testTag("card_key_operations")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showKeysFormatDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_import_keys")
                    ) {
                        Text(text = stringResource(R.string.action_import_keys))
                    }

                    Button(
                        onClick = onExportKeysClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_export_keys")
                    ) {
                        Text(text = stringResource(R.string.action_export_keys))
                    }
                }
            }

            // 3. Backup All
            MctSectionCard(
                title = stringResource(R.string.action_backup_all),
                modifier = Modifier.testTag("card_backup_operations")
            ) {
                OutlinedButton(
                    onClick = onBackupAllClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_backup_all")
                ) {
                    Text(text = stringResource(R.string.action_backup_all))
                }
            }

            // 4. Instructions
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_instructions"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = stringResource(R.string.text_import_export_tool_info),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Dump Format Selection Dialog
    if (showDumpFormatDialog) {
        val dumpFormats = listOf(
            Pair(DumpFormatChoice.MCT, stringResource(R.string.action_file_type_mct)),
            Pair(DumpFormatChoice.JSON, stringResource(R.string.action_file_type_json)),
            Pair(DumpFormatChoice.BIN, stringResource(R.string.action_file_type_mdf_bin)),
            Pair(DumpFormatChoice.EML, stringResource(R.string.action_file_type_eml))
        )

        AlertDialog(
            onDismissRequest = { showDumpFormatDialog = false },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.text_choose_file_type), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    dumpFormats.forEach { (format, label) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) {
                                    showDumpFormatDialog = false
                                    onImportDumpFormatSelected(format)
                                }
                                .testTag("format_dump_${format.name}"),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDumpFormatDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Keys Format Selection Dialog
    if (showKeysFormatDialog) {
        val keyFormats = listOf(
            Pair(KeysFormatChoice.KEYS, stringResource(R.string.action_file_type_keys)),
            Pair(KeysFormatChoice.BIN, stringResource(R.string.action_file_type_bin))
        )

        AlertDialog(
            onDismissRequest = { showKeysFormatDialog = false },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.text_choose_file_type), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    keyFormats.forEach { (format, label) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) {
                                    showKeysFormatDialog = false
                                    onImportKeysFormatSelected(format)
                                }
                                .testTag("format_keys_${format.name}"),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showKeysFormatDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
