package de.syss.MifareClassicTool.ui.valueblock

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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import de.syss.MifareClassicTool.ui.components.MctSectionCard
import de.syss.MifareClassicTool.ui.components.MctTopAppBar
import java.nio.ByteBuffer
import java.util.Locale

@Composable
fun ValueBlockScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var vbText by remember { mutableStateOf("") }
    var vBasIntText by remember { mutableStateOf("") }
    var addrText by remember { mutableStateOf("") }

    val onDecodeClick = {
        val data = vbText.trim()
        if (Common.isHexAnd16Byte(data, context)) {
            if (!Common.isValueBlock(data)) {
                Toast.makeText(context, R.string.info_is_not_vb, Toast.LENGTH_LONG).show()
            } else {
                val vbAsBytes = Common.hex2Bytes(data.substring(0, 8))
                if (vbAsBytes != null) {
                    val vbAsInt = Integer.reverseBytes(ByteBuffer.wrap(vbAsBytes).int)
                    vBasIntText = vbAsInt.toString()
                    addrText = data.substring(24, 26).uppercase(Locale.ROOT)
                }
            }
        }
    }

    val onEncodeClick = {
        val intStr = vBasIntText.trim()
        val addrStr = addrText.trim().uppercase(Locale.ROOT)
        if (intStr.isEmpty()) {
            Toast.makeText(context, R.string.info_no_int_to_encode, Toast.LENGTH_LONG).show()
        } else if (!addrStr.matches(Regex("[0-9A-Fa-f]{2}"))) {
            Toast.makeText(context, R.string.info_addr_not_hex_byte, Toast.LENGTH_LONG).show()
        } else {
            try {
                val vbAsInt = intStr.toInt()
                val vb = Common.bytes2Hex(
                    ByteBuffer.allocate(4).putInt(Integer.reverseBytes(vbAsInt)).array()
                )
                val vbInverted = Common.bytes2Hex(
                    ByteBuffer.allocate(4).putInt(Integer.reverseBytes(vbAsInt.inv())).array()
                )
                val addrInverted = Integer.toHexString(
                    addrStr.toInt(16).inv()
                ).uppercase(Locale.ROOT).substring(6, 8)
                vbText = vb + vbInverted + vb + addrStr + addrInverted + addrStr + addrInverted
            } catch (e: NumberFormatException) {
                val msg = context.getString(R.string.info_invalid_int) +
                        " (Max: ${Int.MAX_VALUE}, Min: ${Int.MIN_VALUE})"
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            MctTopAppBar(
                title = stringResource(R.string.title_activity_value_block_tool),
                onBackClick = onBackClick
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
            // Value Block (Hex) Card
            MctSectionCard(
                title = stringResource(R.string.text_vb),
                modifier = Modifier.testTag("vb_hex_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = vbText,
                        onValueChange = { if (it.length <= 32) vbText = it.uppercase(Locale.ROOT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vb_input"),
                        placeholder = { Text(text = stringResource(R.string.hint_hex_16_byte)) },
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
                        keyboardActions = KeyboardActions(onDone = { onDecodeClick() })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            val clip = Common.getFromClipboard(context)
                            if (!clip.isNullOrEmpty()) {
                                vbText = clip.trim().uppercase(Locale.ROOT)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("vb_paste_button"),
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
                    Spacer(modifier = Modifier.width(4.dp))
                    FilledIconButton(
                        onClick = {
                            if (vbText.isNotEmpty()) {
                                Common.copyToClipboard(vbText, context, true)
                            }
                        },
                        enabled = vbText.isNotEmpty(),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("vb_copy_button"),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Decode & Encode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDecodeClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("decode_button")
                ) {
                    Image(
                        painter = painterResource(R.drawable.arrow_down),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_decode),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onEncodeClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("encode_button")
                ) {
                    Image(
                        painter = painterResource(R.drawable.arrow_up),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_encode),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Decoded Values Card: Int + Addr
            MctSectionCard(
                title = stringResource(R.string.text_vb_as_int),
                modifier = Modifier.testTag("vb_int_card")
            ) {
                // Value as Int
                Text(
                    text = stringResource(R.string.text_value),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = vBasIntText,
                    onValueChange = { vBasIntText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("int_input"),
                    placeholder = { Text(text = stringResource(R.string.hint_int_423)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Block Addr
                Text(
                    text = stringResource(R.string.text_addr),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = addrText,
                    onValueChange = { if (it.length <= 2) addrText = it.uppercase(Locale.ROOT) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("addr_input"),
                    placeholder = { Text(text = stringResource(R.string.hint_hex_00)) },
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
                    keyboardActions = KeyboardActions(onDone = { onEncodeClick() })
                )
            }
        }
    }
}
