package de.syss.MifareClassicTool.ui.filechooser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import de.syss.MifareClassicTool.ui.components.MctActionButton
import de.syss.MifareClassicTool.ui.components.MctSectionCard
import de.syss.MifareClassicTool.ui.components.MctTopAppBar
import java.io.File

data class FileChooserItem(
    val file: File,
    val name: String,
    val sizeText: String,
    val dateText: String
)

data class FileChooserUiState(
    val title: String,
    val chooserText: String,
    val buttonText: String,
    val allowNewFile: Boolean,
    val files: List<FileChooserItem> = emptyList(),
    val selectedFileName: String? = null
)

@Composable
fun FileChooserScreen(
    state: FileChooserUiState,
    onSelectFile: (String) -> Unit,
    onChooseClick: () -> Unit,
    onCancel: () -> Unit,
    onCreateNewFile: (String) -> Unit,
    onDeleteSelectedFile: () -> Unit,
    onImportExportClick: () -> Unit,
    prefillExtension: String = ""
) {
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var newFileNameInput by remember { mutableStateOf("") }
    var currentSelectedName by remember(state.selectedFileName) { mutableStateOf(state.selectedFileName) }

    Scaffold(
        topBar = {
            MctTopAppBar(
                title = state.title,
                onBackClick = onCancel,
                actions = {
                    if (state.allowNewFile) {
                        IconButton(
                            onClick = {
                                newFileNameInput = prefillExtension
                                showNewFileDialog = true
                            },
                            modifier = Modifier.testTag("btn_new_file")
                        ) {
                            Text(
                                text = "➕",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    if (state.files.isNotEmpty()) {
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.testTag("btn_delete_file")
                        ) {
                            Text(
                                text = "🗑",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    IconButton(
                        onClick = onImportExportClick,
                        modifier = Modifier.testTag("btn_import_export")
                    ) {
                        Text(
                            text = "📁",
                            style = MaterialTheme.typography.titleMedium
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
        ) {
            // Chooser Info Banner
            if (state.chooserText.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_chooser_info"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = state.chooserText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (state.files.isEmpty()) {
                // Empty state
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .testTag("empty_files_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.text_no_files_in_chooser),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.allowNewFile) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.text_file_chooser_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            } else {
                // List of files
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("list_files"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.files, key = { it.name }) { item ->
                        val isSelected = (currentSelectedName == item.name)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    role = Role.RadioButton,
                                    onClick = {
                                        currentSelectedName = item.name
                                        onSelectFile(item.name)
                                    }
                                )
                                .testTag("file_item_${item.name}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    if (item.sizeText.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${item.sizeText}  •  ${item.dateText}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Button
            MctActionButton(
                text = state.buttonText,
                onClick = onChooseClick,
                enabled = (currentSelectedName != null),
                modifier = Modifier.testTag("btn_choose")
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // New File Dialog
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text(stringResource(R.string.dialog_new_file_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.dialog_new_file),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newFileNameInput,
                        onValueChange = { newFileNameInput = it },
                        label = { Text(stringResource(R.string.text_file_name)) },
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_new_filename")
                    )
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                TextButton(
                    onClick = {
                        val input = newFileNameInput.trim()
                        if (input.isNotEmpty()) {
                            showNewFileDialog = false
                            onCreateNewFile(input)
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_new_file")
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNewFileDialog = false },
                    modifier = Modifier.testTag("btn_cancel_new_file")
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Delete File Confirmation Dialog
    if (showDeleteConfirmDialog && state.selectedFileName != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(R.string.action_delete_file),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = { Text(stringResource(R.string.dialog_delete_file, state.selectedFileName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteSelectedFile()
                    },
                    modifier = Modifier.testTag("btn_confirm_delete")
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier.testTag("btn_cancel_delete")
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
