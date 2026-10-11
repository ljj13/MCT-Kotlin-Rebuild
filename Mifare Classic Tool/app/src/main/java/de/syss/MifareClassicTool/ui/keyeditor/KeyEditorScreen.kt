package de.syss.MifareClassicTool.ui.keyeditor

import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import androidx.activity.compose.BackHandler
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.components.MctTopAppBar

data class KeyEditorUiState(
    val fileName: String? = null,
    val hasChanges: Boolean = false,
    val initialContent: String = "",
    val lineCount: Int = 0,
    val validKeyCount: Int = 0,
    val showSaveDialog: Boolean = false,
    val saveFileNameInput: String = "",
    val showExitConfirmDialog: Boolean = false
)

@Composable
fun KeyEditorScreen(
    state: KeyEditorUiState,
    onStateChange: (KeyEditorUiState) -> Unit,
    editTextRef: (AppCompatEditText) -> Unit,
    onContentChanged: (String) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onConfirmSave: (String) -> Unit,
    onShareClick: () -> Unit,
    onRemoveDuplicatesClick: () -> Unit,
    onExportKeysClick: () -> Unit,
    onDiscardAndExit: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    BackHandler {
        onBackClick()
    }

    Scaffold(
        topBar = {
            MctTopAppBar(
                title = stringResource(R.string.title_activity_key_editor) +
                        (if (!state.fileName.isNullOrEmpty()) " (${state.fileName})" else ""),
                onBackClick = onBackClick,
                actions = {
                    // Save Button
                    IconButton(
                        onClick = onSaveClick,
                        modifier = Modifier.testTag("btn_key_save")
                    ) {
                        Text(
                            text = "💾",
                            fontSize = 20.sp
                        )
                    }

                    // Share Button
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.testTag("btn_key_share")
                    ) {
                        Text(
                            text = "↗",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // More Menu
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("btn_key_more")
                        ) {
                            Text(
                                text = "⋮",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.testTag("menu_key_dropdown")
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_remove_duplicates)) },
                                onClick = {
                                    menuExpanded = false
                                    onRemoveDuplicatesClick()
                                },
                                modifier = Modifier.testTag("menu_remove_duplicates")
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_export_keys)) },
                                onClick = {
                                    menuExpanded = false
                                    onExportKeysClick()
                                },
                                modifier = Modifier.testTag("menu_export_keys")
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
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Stats / File Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_key_editor_stats"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (!state.fileName.isNullOrEmpty()) state.fileName else "新建密钥文件",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.hasChanges) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "未保存",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "共 ${state.lineCount} 行 · ${state.validKeyCount} 个密钥",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Editing Core: AndroidView embedding AppCompatEditText
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("card_key_editor_content"),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                AndroidView(
                    factory = { context ->
                        AppCompatEditText(context).apply {
                            typeface = Typeface.MONOSPACE
                            inputType = InputType.TYPE_CLASS_TEXT or
                                    InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or
                                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                            gravity = Gravity.TOP or Gravity.START
                            background = null
                            isVerticalScrollBarEnabled = true
                            setPadding(24, 20, 24, 20)
                            setText(state.initialContent)
                            addTextChangedListener(object : android.text.TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                                override fun afterTextChanged(s: android.text.Editable?) {
                                    onContentChanged(s?.toString() ?: "")
                                }
                            })
                            editTextRef(this)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("edit_text_keys")
                )
            }
        }
    }

    // Save File Name Dialog
    if (state.showSaveDialog) {
        AlertDialog(
            onDismissRequest = {
                onStateChange(state.copy(showSaveDialog = false))
            },
            title = { Text(stringResource(R.string.dialog_save_keys_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.dialog_save_keys),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.saveFileNameInput,
                        onValueChange = {
                            onStateChange(state.copy(saveFileNameInput = it))
                        },
                        label = { Text("文件名") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_save_file_name")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmSave(state.saveFileNameInput.trim())
                    },
                    modifier = Modifier.testTag("btn_confirm_save")
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onStateChange(state.copy(showSaveDialog = false))
                    },
                    modifier = Modifier.testTag("btn_cancel_save")
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Exit With Unsaved Changes Dialog
    if (state.showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                onStateChange(state.copy(showExitConfirmDialog = false))
            },
            title = { Text(stringResource(R.string.dialog_save_before_quitting_title)) },
            text = { Text(stringResource(R.string.dialog_save_before_quitting)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onStateChange(state.copy(showExitConfirmDialog = false))
                        onSaveClick()
                    },
                    modifier = Modifier.testTag("btn_exit_save")
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            onStateChange(state.copy(showExitConfirmDialog = false))
                        },
                        modifier = Modifier.testTag("btn_exit_cancel")
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    TextButton(
                        onClick = {
                            onStateChange(state.copy(showExitConfirmDialog = false))
                            onDiscardAndExit()
                        },
                        modifier = Modifier.testTag("btn_exit_dont_save")
                    ) {
                        Text(stringResource(R.string.action_dont_save))
                    }
                }
            }
        )
    }
}
