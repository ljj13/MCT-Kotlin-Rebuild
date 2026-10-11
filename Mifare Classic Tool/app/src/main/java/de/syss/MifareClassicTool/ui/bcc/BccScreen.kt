package de.syss.MifareClassicTool.ui.bcc

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BccScreen(
    initialUid: String = "",
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var uidText by remember { mutableStateOf(initialUid) }
    var bccText by remember { mutableStateOf("") }

    val onCalculateClick = {
        val data = uidText.trim()
        if (data.length != 8) {
            Toast.makeText(context, R.string.info_invalid_uid_length, Toast.LENGTH_LONG).show()
        } else if (!data.matches(Regex("[0-9A-Fa-f]+"))) {
            Toast.makeText(context, R.string.info_not_hex_data, Toast.LENGTH_LONG).show()
        } else {
            val bytes = Common.hex2Bytes(data)
            if (bytes != null) {
                val bcc = Common.calcBcc(bytes)
                bccText = String.format(Locale.ROOT, "%02X", bcc)
            }
        }
    }

    val onPasteClick = {
        val clipText = Common.getFromClipboard(context)
        if (!clipText.isNullOrEmpty()) {
            uidText = clipText.trim().uppercase(Locale.ROOT)
        }
    }

    val onCopyClick = {
        if (bccText.isNotEmpty()) {
            Common.copyToClipboard(bccText, context, true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_activity_bcc_tool),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            // UID Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.text_uid_part),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uidText,
                            onValueChange = { newValue ->
                                if (newValue.length <= 8) {
                                    uidText = newValue.uppercase(Locale.ROOT)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("uid_input"),
                            placeholder = {
                                Text(text = stringResource(R.string.hint_hex_4_byte))
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                autoCorrectEnabled = false,
                                keyboardType = KeyboardType.Ascii,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { onCalculateClick() }
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = onPasteClick,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("paste_button"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Image(
                                painter = painterResource(R.drawable.paste),
                                contentDescription = stringResource(R.string.text_paste),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calculate Action Button
            Button(
                onClick = onCalculateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("calc_button")
            ) {
                Image(
                    painter = painterResource(R.drawable.arrow_down),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_calc_bcc),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BCC Output Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.text_bcc),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = bccText,
                            onValueChange = {},
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bcc_result"),
                            readOnly = true,
                            placeholder = {
                                Text(text = "--")
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = onCopyClick,
                            enabled = bccText.isNotEmpty(),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("copy_button"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Image(
                                painter = painterResource(R.drawable.copy),
                                contentDescription = stringResource(R.string.text_copy),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
