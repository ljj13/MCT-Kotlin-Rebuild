package de.syss.MifareClassicTool.ui.accesscondition

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
fun AccessConditionScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var acText by remember { mutableStateOf("") }
    var isKeyBReadable by remember { mutableStateOf(true) }
    var wasKeyBReadable by remember { mutableStateOf(false) }

    // mACMatrix: 3 rows (C1-C3) x 4 columns (Blocks 0-2, Block 3 Sector Trailer)
    var acMatrix by remember {
        mutableStateOf(
            arrayOf(
                byteArrayOf(0, 0, 0, 0),
                byteArrayOf(0, 0, 0, 0),
                byteArrayOf(0, 0, 0, 1)
            )
        )
    }

    val getResourceForAccessCondition = { prefix: String, rowNr: Int ->
        context.resources.getIdentifier(prefix + rowNr, "string", context.packageName)
    }

    val getResourceForSectorTrailer = { rowNr: Int ->
        getResourceForAccessCondition("ac_sector_trailer_", rowNr)
    }

    val getResourceForDataBlock = { rowNr: Int, keyBReadable: Boolean ->
        val prefix = if (keyBReadable) "ac_data_block_no_keyb_" else "ac_data_block_"
        getResourceForAccessCondition(prefix, rowNr)
    }

    val acRowNrToACBits = { rowNr: Int, isSectorTrailer: Boolean, keyBReadable: Boolean ->
        if (!isSectorTrailer && keyBReadable && rowNr > 1) {
            when (rowNr) {
                2 -> byteArrayOf(0, 0, 1)
                3 -> byteArrayOf(1, 1, 1)
                else -> null
            }
        } else {
            when (rowNr) {
                0 -> byteArrayOf(0, 0, 0)
                1 -> byteArrayOf(0, 1, 0)
                2 -> byteArrayOf(1, 0, 0)
                3 -> byteArrayOf(1, 1, 0)
                4 -> byteArrayOf(0, 0, 1)
                5 -> byteArrayOf(0, 1, 1)
                6 -> byteArrayOf(1, 0, 1)
                7 -> byteArrayOf(1, 1, 1)
                else -> null
            }
        }
    }

    val acBitsToACRowNr = { acBits: ByteArray?, isSectorTrailer: Boolean, keyBReadable: Boolean ->
        if (acBits == null || acBits.size != 3) {
            -1
        } else {
            val b0 = acBits[0].toInt()
            val b1 = acBits[1].toInt()
            val b2 = acBits[2].toInt()
            if (!isSectorTrailer && keyBReadable) {
                when {
                    b0 == 0 && b1 == 0 && b2 == 0 -> 0
                    b0 == 0 && b1 == 1 && b2 == 0 -> 1
                    b0 == 0 && b1 == 0 && b2 == 1 -> 2
                    b0 == 1 && b1 == 1 && b2 == 1 -> 3
                    else -> -1
                }
            } else {
                when {
                    b0 == 0 && b1 == 0 && b2 == 0 -> 0
                    b0 == 0 && b1 == 1 && b2 == 0 -> 1
                    b0 == 1 && b1 == 0 && b2 == 0 -> 2
                    b0 == 1 && b1 == 1 && b2 == 0 -> 3
                    b0 == 0 && b1 == 0 && b2 == 1 -> 4
                    b0 == 0 && b1 == 1 && b2 == 1 -> 5
                    b0 == 1 && b1 == 0 && b2 == 1 -> 6
                    b0 == 1 && b1 == 1 && b2 == 1 -> 7
                    else -> -1
                }
            }
        }
    }

    var blockTexts by remember {
        mutableStateOf(
            listOf(
                context.getString(getResourceForDataBlock(0, true)),
                context.getString(getResourceForDataBlock(0, true)),
                context.getString(getResourceForDataBlock(0, true)),
                context.getString(getResourceForSectorTrailer(0))
            )
        )
    }

    // Dialog state for choosing permissions
    var activeBlockDialogIndex by remember { mutableStateOf<Int?>(null) }

    val onDecodeClick = {
        val ac = acText.trim()
        if (ac.length != 6) {
            Toast.makeText(context, R.string.info_ac_not_3_byte, Toast.LENGTH_LONG).show()
        } else if (!ac.matches(Regex("[0-9A-Fa-f]+"))) {
            Toast.makeText(context, R.string.info_ac_not_hex, Toast.LENGTH_LONG).show()
        } else {
            val decodedMatrix = Common.acBytesToACMatrix(Common.hex2Bytes(ac))
            if (decodedMatrix != null) {
                val trailerBits = byteArrayOf(decodedMatrix[0][3], decodedMatrix[1][3], decodedMatrix[2][3])
                val trailerRow = acBitsToACRowNr(trailerBits, true, false)
                if (trailerRow != -1) {
                    val newKeyBReadable = trailerRow < 2 || trailerRow == 4
                    isKeyBReadable = newKeyBReadable
                    val newBlockTexts = mutableListOf<String>()

                    var error = false
                    for (i in 0 until 3) {
                        val dataBits = byteArrayOf(decodedMatrix[0][i], decodedMatrix[1][i], decodedMatrix[2][i])
                        val dataRow = acBitsToACRowNr(dataBits, false, newKeyBReadable)
                        if (dataRow == -1) {
                            error = true
                            break
                        }
                        newBlockTexts.add(context.getString(getResourceForDataBlock(dataRow, newKeyBReadable)))
                    }

                    if (!error) {
                        newBlockTexts.add(context.getString(getResourceForSectorTrailer(trailerRow)))
                        blockTexts = newBlockTexts
                        acMatrix = decodedMatrix
                    } else {
                        Toast.makeText(context, R.string.info_ac_format_error, Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, R.string.info_ac_format_error, Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, R.string.info_ac_format_error, Toast.LENGTH_LONG).show()
            }
        }
    }

    val onEncodeClick = {
        val encodedBytes = Common.acMatrixToACBytes(acMatrix)
        if (encodedBytes != null) {
            acText = Common.bytes2Hex(encodedBytes)
        }
    }

    Scaffold(
        topBar = {
            MctTopAppBar(
                title = stringResource(R.string.title_activity_access_condition_tool),
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
            // AC Hex Card
            MctSectionCard(
                title = stringResource(R.string.text_ac),
                modifier = Modifier.testTag("ac_hex_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = acText,
                        onValueChange = { if (it.length <= 6) acText = it.uppercase(Locale.ROOT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ac_input"),
                        placeholder = { Text(text = stringResource(R.string.hint_hex_3_byte)) },
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
                                acText = clip.trim().uppercase(Locale.ROOT)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("ac_paste_button"),
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
                            if (acText.isNotEmpty()) {
                                Common.copyToClipboard(acText, context, true)
                            }
                        },
                        enabled = acText.isNotEmpty(),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("ac_copy_button"),
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

            // Action Buttons Row (Decode & Encode)
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

            // Sector Trailer (Block 3) Card
            MctSectionCard(
                title = stringResource(R.string.text_step_1_sector_trailer),
                modifier = Modifier.testTag("sector_trailer_card")
            ) {
                OutlinedButton(
                    onClick = { activeBlockDialogIndex = 3 },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("block_3_button")
                ) {
                    Text(
                        text = blockTexts.getOrElse(3) { "--" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Data Blocks (0, 1, 2) Card
            MctSectionCard(
                title = stringResource(R.string.text_step_2_data_blocks),
                modifier = Modifier.testTag("data_blocks_card")
            ) {
                for (bIndex in 0..2) {
                    Text(
                        text = "${stringResource(R.string.text_block)} $bIndex:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { activeBlockDialogIndex = bIndex },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("block_${bIndex}_button")
                    ) {
                        Text(
                            text = blockTexts.getOrElse(bIndex) { "--" },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (bIndex < 2) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // Permission Chooser Dialog
    activeBlockDialogIndex?.let { blockIndex ->
        val isSectorTrailer = blockIndex == 3
        val itemCount = if (isSectorTrailer) {
            8
        } else if (isKeyBReadable) {
            4
        } else {
            8
        }

        val items = List(itemCount) { i ->
            if (isSectorTrailer) {
                context.getString(getResourceForSectorTrailer(i))
            } else {
                context.getString(getResourceForDataBlock(i, isKeyBReadable))
            }
        }

        AlertDialog(
            onDismissRequest = { activeBlockDialogIndex = null },
            title = {
                Text(
                    text = stringResource(R.string.dialog_choose_ac_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(items) { position, itemText ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    val newTexts = blockTexts.toMutableList()
                                    newTexts[blockIndex] = itemText
                                    blockTexts = newTexts

                                    val newMatrix = arrayOf(
                                        acMatrix[0].clone(),
                                        acMatrix[1].clone(),
                                        acMatrix[2].clone()
                                    )

                                    if (isSectorTrailer) {
                                        val acBits = acRowNrToACBits(position, true, false)
                                        if (acBits != null) {
                                            newMatrix[0][3] = acBits[0]
                                            newMatrix[1][3] = acBits[1]
                                            newMatrix[2][3] = acBits[2]
                                        }
                                        val newKeyB = position < 2 || position == 4
                                        if (newKeyB != isKeyBReadable) {
                                            isKeyBReadable = newKeyB
                                            // Reset blocks 0..2
                                            for (k in 0..2) {
                                                newMatrix[0][k] = 0
                                                newMatrix[1][k] = 0
                                                newMatrix[2][k] = 0
                                                newTexts[k] = context.getString(getResourceForDataBlock(0, newKeyB))
                                            }
                                            val r = if (newKeyB) {
                                                R.string.info_ac_reset_keyb_readable
                                            } else {
                                                R.string.info_ac_reset_keyb_not_readable
                                            }
                                            Toast.makeText(context, r, Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        val acBits = acRowNrToACBits(position, false, isKeyBReadable)
                                        if (acBits != null) {
                                            newMatrix[0][blockIndex] = acBits[0]
                                            newMatrix[1][blockIndex] = acBits[1]
                                            newMatrix[2][blockIndex] = acBits[2]
                                        }
                                    }

                                    acMatrix = newMatrix
                                    activeBlockDialogIndex = null
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = itemText,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { activeBlockDialogIndex = null }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
