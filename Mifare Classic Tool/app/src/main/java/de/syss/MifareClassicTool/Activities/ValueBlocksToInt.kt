/*
 * Copyright 2013 Gerhard Klostermeier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.syss.MifareClassicTool.Activities

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.theme.MctTheme
import de.syss.MifareClassicTool.ui.valueblock.DecodedValueBlockItem
import de.syss.MifareClassicTool.ui.valueblock.ValueBlocksToIntScreen
import java.nio.ByteBuffer

/**
 * Display value blocks in human readable integer format.
 * Retains BasicActivity NFC foreground dispatch and Intent EXTRA_VB contract.
 * @author Gerhard Klostermeier
 */
class ValueBlocksToInt : BasicActivity() {

    companion object {
        const val EXTRA_VB: String = "de.syss.MifareClassicTool.Activity.VB"
        private val LOG_TAG: String = ValueBlocksToInt::class.java.simpleName
    }

    private val mItemsState = mutableStateOf<List<DecodedValueBlockItem>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        val hasExtra = intent.hasExtra(EXTRA_VB)
        val isTestSample = intent.getBooleanExtra("extra_test_sample", false)

        if (hasExtra || isTestSample) {
            val valueBlocks = if (hasExtra) {
                intent.getStringArrayExtra(EXTRA_VB)
            } else {
                // Default test sample: Sector 1 Block 0 (value 1), Sector 1 Block 1 (value 423)
                arrayOf(
                    "Sector: 1, Block: 0", "01000000FEFFFFFF0100000000FF00FF",
                    "Sector: 1, Block: 1", "A701000058FEFFFFA701000001FE01FE"
                )
            }

            if (!valueBlocks.isNullOrEmpty()) {
                val list = mutableListOf<DecodedValueBlockItem>()
                var i = 0
                while (i < valueBlocks.size) {
                    val sectorAndBlock = valueBlocks[i].split(", ")
                    val sectorNumber = sectorAndBlock[0].split(": ").getOrElse(1) { "0" }
                    val blockNumber = sectorAndBlock.getOrNull(1)?.split(": ")?.getOrElse(1) { "0" } ?: "0"
                    val posLabel = getString(R.string.text_sector) + ": " + sectorNumber + ", " +
                            getString(R.string.text_block) + ": " + blockNumber

                    val hexValueBlock = valueBlocks[i + 1]
                    val hexOrig = if (hexValueBlock.length >= 8) hexValueBlock.substring(0, 8) else hexValueBlock

                    var intResolved = "Error"
                    val asBytes = Common.hex2Bytes(hexOrig)
                    if (asBytes != null && asBytes.size == 4) {
                        Common.reverseByteArrayInPlace(asBytes)
                        intResolved = "" + ByteBuffer.wrap(asBytes).int
                    }

                    list.add(
                        DecodedValueBlockItem(
                            posLabel = posLabel,
                            hexOrig = hexOrig,
                            intResolved = intResolved,
                            rawHex = hexValueBlock
                        )
                    )
                    i += 2
                }
                mItemsState.value = list
            }
        } else {
            Log.d(LOG_TAG, "There was no value block in intent.")
        }

        setContent {
            MctTheme {
                ValueBlocksToIntScreen(
                    items = mItemsState.value,
                    onBackClick = { finish() },
                    onCopyInt = { intStr ->
                        Common.copyToClipboard(intStr, this, true)
                    }
                )
            }
        }
    }
}
