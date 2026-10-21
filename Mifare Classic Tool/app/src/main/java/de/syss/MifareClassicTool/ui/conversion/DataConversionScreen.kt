package de.syss.MifareClassicTool.ui.conversion

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
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
import androidx.compose.material3.OutlinedButton
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
import java.util.Locale

@Composable
fun DataConversionScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var asciiText by remember { mutableStateOf("") }
    var hexText by remember { mutableStateOf("") }
    var binText by remember { mutableStateOf("") }
    var decBEText by remember { mutableStateOf("") }
    var decLEText by remember { mutableStateOf("") }

    val convertData: (String?) -> Unit = { hex ->
        if (hex.isNullOrEmpty()) {
            Toast.makeText(context, R.string.info_convert_error, Toast.LENGTH_SHORT).show()
        } else {
            hexText = hex.uppercase(Locale.ROOT)
            val ascii = Common.hex2Ascii(hex)
            asciiText = ascii ?: context.getString(R.string.text_not_ascii)
            binText = Common.hex2Bin(hex) ?: ""
            decBEText = Common.hex2DecBE(hex) ?: ""
            decLEText = Common.hex2DecLE(hex) ?: ""
        }
    }

    val isBin: (String) -> Boolean = { bin ->
        if (bin.length % 8 == 0 && bin.matches(Regex("[0-1]+"))) {
            true
        } else {
            Toast.makeText(context, R.string.info_not_bin_data, Toast.LENGTH_LONG).show()
            false
        }
    }

    val isDec: (String) -> Boolean = { dec ->
        if (dec.isNotEmpty() && dec.matches(Regex("\\d+"))) {
            true
        } else {
            Toast.makeText(context, R.string.info_not_dec_data, Toast.LENGTH_LONG).show()
            false
        }
    }

    Scaffold(
        topBar = {
            MctTopAppBar(
                title = stringResource(R.string.title_activity_data_conversion_tool),
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
            // Conversions Card
            MctSectionCard(
                title = stringResource(R.string.title_activity_data_conversion_tool),
                modifier = Modifier.testTag("conversion_card")
            ) {
                // ASCII
                ConversionFieldRow(
                    label = stringResource(R.string.text_ascii),
                    value = asciiText,
                    onValueChange = { asciiText = it },
                    onConvert = { convertData(Common.ascii2Hex(asciiText)) },
                    testTag = "ascii",
                    keyboardType = KeyboardType.Text
                )

                Spacer(modifier = Modifier.height(10.dp))

                // HEX
                ConversionFieldRow(
                    label = stringResource(R.string.text_hex),
                    value = hexText,
                    onValueChange = { hexText = it.uppercase(Locale.ROOT) },
                    onConvert = {
                        if (Common.isHex(hexText, context)) {
                            convertData(hexText)
                        }
                    },
                    testTag = "hex",
                    isHex = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // BIN
                ConversionFieldRow(
                    label = stringResource(R.string.text_bin),
                    value = binText,
                    onValueChange = { binText = it },
                    onConvert = {
                        if (isBin(binText)) {
                            convertData(Common.bin2Hex(binText))
                        }
                    },
                    testTag = "bin",
                    keyboardType = KeyboardType.Number
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Dec (BE)
                ConversionFieldRow(
                    label = stringResource(R.string.text_dec_be),
                    value = decBEText,
                    onValueChange = { decBEText = it },
                    onConvert = {
                        if (isDec(decBEText)) {
                            convertData(Common.decBE2Hex(decBEText))
                        }
                    },
                    testTag = "dec_be",
                    keyboardType = KeyboardType.Number
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Dec (LE)
                ConversionFieldRow(
                    label = stringResource(R.string.text_dec_le),
                    value = decLEText,
                    onValueChange = { decLEText = it },
                    onConvert = {
                        if (isDec(decLEText)) {
                            convertData(Common.decLE2Hex(decLEText))
                        }
                    },
                    testTag = "dec_le",
                    keyboardType = KeyboardType.Number
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // External Online Tools Card
            MctSectionCard(
                title = stringResource(R.string.text_more_converter),
                modifier = Modifier.testTag("online_tools_card")
            ) {
                OutlinedButton(
                    onClick = {
                        val hex = hexText.trim()
                        if (hex.isEmpty() || Common.isHex(hex, context)) {
                            val url = "https://hexconverter.scadacore.com/?HexString=$hex"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_generic_converter")
                ) {
                    Text(text = stringResource(R.string.action_generic_converter))
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val url = "https://cryptii.com/pipes/integer-encoder"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_multipurpose_converter")
                ) {
                    Text(text = stringResource(R.string.action_multi_purpose_converter))
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val hex = hexText.trim()
                        if (hex.isEmpty() || Common.isHex(hex, context)) {
                            var base64 = Base64.encodeToString(hex.toByteArray(), Base64.DEFAULT)
                            base64 = base64.trim().replace("=", "")
                            val url = "https://gchq.github.io/CyberChef/#recipe=From_Hex('Auto')&input=$base64"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_cyberchef")
                ) {
                    Text(text = stringResource(R.string.action_cyber_chef))
                }
            }
        }
    }
}

@Composable
private fun ConversionFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConvert: () -> Unit,
    testTag: String,
    keyboardType: KeyboardType = KeyboardType.Ascii,
    isHex: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("${testTag}_input"),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = if (isHex) KeyboardCapitalization.Characters else KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onConvert() })
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onConvert,
                modifier = Modifier.testTag("${testTag}_convert_btn")
            ) {
                Text(
                    text = "⇄",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
