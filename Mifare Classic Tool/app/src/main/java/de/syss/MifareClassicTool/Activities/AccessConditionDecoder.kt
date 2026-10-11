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
import de.syss.MifareClassicTool.Common.Operation
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.accesscondition.AccessConditionDecoderScreen
import de.syss.MifareClassicTool.ui.accesscondition.BlockAcPermission
import de.syss.MifareClassicTool.ui.accesscondition.DecodedSectorAc
import de.syss.MifareClassicTool.ui.accesscondition.TrailerAcPermission
import de.syss.MifareClassicTool.ui.theme.MctTheme

/**
 * Display the MIFARE Access Conditions in a human readable table.
 * Retains BasicActivity NFC foreground dispatch and Intent EXTRA_AC data format.
 * @author Gerhard Klostermeier
 */
class AccessConditionDecoder : BasicActivity() {

    companion object {
        const val EXTRA_AC: String = "de.syss.MifareClassicTool.Activity.AC"
        private val LOG_TAG: String = AccessConditionDecoder::class.java.simpleName
    }

    private val mSectorsState = mutableStateOf<List<DecodedSectorAc>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        val hasExtra = intent.hasExtra(EXTRA_AC)
        val isTestSample = intent.getBooleanExtra("extra_test_sample", false)

        if (hasExtra || isTestSample) {
            val accessConditions = if (hasExtra) {
                intent.getStringArrayExtra(EXTRA_AC)
            } else {
                // Default test sample: Sector 0 standard transport AC (FF0780) and Sector 1
                arrayOf("Sector: 0", "FF0780", "Sector: 1", "*FF0780")
            }

            if (accessConditions != null) {
                val list = mutableListOf<DecodedSectorAc>()
                var j = 0
                while (j < accessConditions.size) {
                    var hasMoreThan4Blocks = false
                    var acHex = accessConditions[j + 1]
                    if (acHex.startsWith("*")) {
                        hasMoreThan4Blocks = true
                        acHex = acHex.substring(1)
                    }

                    val bAC = Common.hex2Bytes(acHex)
                    val acMatrix = Common.acBytesToACMatrix(bAC)
                    val sectorNumber = accessConditions[j].split(": ").getOrElse(1) { "$j" }
                    val sectorHeader = getString(R.string.text_sector) + ": " + sectorNumber

                    if (acMatrix == null) {
                        list.add(DecodedSectorAc(sectorHeader = sectorHeader, isValid = false))
                    } else {
                        val blocks = decodeBlocks(acMatrix, hasMoreThan4Blocks)
                        val trailer = decodeTrailer(acMatrix)
                        list.add(
                            DecodedSectorAc(
                                sectorHeader = sectorHeader,
                                isValid = true,
                                blocks = blocks,
                                trailer = trailer
                            )
                        )
                    }
                    j += 2
                }
                mSectorsState.value = list
            }
        } else {
            Log.d(LOG_TAG, "There were no access conditions in intent.")
            // Allow user to see empty state instead of immediate crash/finish if launched standalone
        }

        setContent {
            MctTheme {
                AccessConditionDecoderScreen(
                    sectors = mSectorsState.value,
                    onBackClick = { finish() }
                )
            }
        }
    }

    private fun decodeBlocks(acMatrix: Array<ByteArray>, hasMoreThan4Blocks: Boolean): List<BlockAcPermission> {
        val isKeyBReadable = Common.isKeyBReadable(
            acMatrix[0][3], acMatrix[1][3], acMatrix[2][3]
        )
        val blocks = mutableListOf<BlockAcPermission>()
        for (i in 0 until 3) {
            val c1 = acMatrix[0][i]
            val c2 = acMatrix[1][i]
            val c3 = acMatrix[2][i]
            val blockHeader: String = if (hasMoreThan4Blocks) {
                getString(R.string.text_block) + ": " + (i * 4 + i) + "-" + (i * 4 + 4 + i)
            } else {
                getString(R.string.text_block) + ": " + i
            }

            blocks.add(
                BlockAcPermission(
                    blockLabel = blockHeader,
                    read = getPermissionText(c1, c2, c3, Operation.Read, false, isKeyBReadable),
                    write = getPermissionText(c1, c2, c3, Operation.Write, false, isKeyBReadable),
                    incr = getPermissionText(c1, c2, c3, Operation.Increment, false, isKeyBReadable),
                    decr = getPermissionText(c1, c2, c3, Operation.DecTransRest, false, isKeyBReadable)
                )
            )
        }
        return blocks
    }

    private fun decodeTrailer(acMatrix: Array<ByteArray>): List<TrailerAcPermission> {
        val c1 = acMatrix[0][3]
        val c2 = acMatrix[1][3]
        val c3 = acMatrix[2][3]

        return listOf(
            TrailerAcPermission(
                target = "Key A:",
                read = getPermissionText(c1, c2, c3, Operation.ReadKeyA, true, false),
                write = getPermissionText(c1, c2, c3, Operation.WriteKeyA, true, false)
            ),
            TrailerAcPermission(
                target = "AC Bits:",
                read = getPermissionText(c1, c2, c3, Operation.ReadAC, true, false),
                write = getPermissionText(c1, c2, c3, Operation.WriteAC, true, false)
            ),
            TrailerAcPermission(
                target = "Key B:",
                read = getPermissionText(c1, c2, c3, Operation.ReadKeyB, true, false),
                write = getPermissionText(c1, c2, c3, Operation.WriteKeyB, true, false)
            )
        )
    }

    private fun getPermissionText(
        c1: Byte,
        c2: Byte,
        c3: Byte,
        op: Operation,
        isSectorTrailer: Boolean,
        isKeyBReadable: Boolean
    ): String {
        return when (Common.getOperationRequirements(c1, c2, c3, op, isSectorTrailer, isKeyBReadable)) {
            0 -> getString(R.string.text_never)
            1 -> getString(R.string.text_key_a)
            2 -> getString(R.string.text_key_b)
            3 -> getString(R.string.text_key_ab)
            4 -> getString(R.string.text_ac_error)
            else -> ""
        }
    }
}
