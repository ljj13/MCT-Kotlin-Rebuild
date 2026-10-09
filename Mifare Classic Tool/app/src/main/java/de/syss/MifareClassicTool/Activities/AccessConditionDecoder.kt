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
import android.text.SpannableString
import android.util.Log
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.Common.Operation
import de.syss.MifareClassicTool.R

/**
 * Display the MIFARE Access Conditions in a way a user can read.
 * NXP has PDFs describing what access conditions are.
 * This Activity will be shown from the [DumpEditor], if the user
 * clicks the corresponding menu item.
 * @author Gerhard Klostermeier
 */
class AccessConditionDecoder : BasicActivity() {

    companion object {
        const val EXTRA_AC: String = "de.syss.MifareClassicTool.Activity.AC"
        private val LOG_TAG: String = AccessConditionDecoder::class.java.simpleName
    }

    private var mLayout: TableLayout? = null

    /**
     * Get access conditions from Intent and initialize Activity to
     * displaying them. If there is no Intent with [EXTRA_AC], the Activity will be exited.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_access_condition_decoder)

        if (intent.hasExtra(EXTRA_AC)) {
            mLayout = findViewById(R.id.tableLayoutAccessConditionDecoder)
            val accessConditions = intent.getStringArrayExtra(EXTRA_AC)
            if (accessConditions != null) {
                var j = 0
                while (j < accessConditions.size) {
                    var hasMoreThan4Blocks = false
                    var acHex = accessConditions[j + 1]
                    if (acHex.startsWith("*")) {
                        hasMoreThan4Blocks = true
                        acHex = acHex.substring(1)
                    }

                    // b6 = bAC[0], b7 = bAC[1], ...
                    val bAC = Common.hex2Bytes(acHex)

                    // acMatrix[C1-C3][Block1-Block3 + Sector Trailer]
                    val acMatrix = Common.acBytesToACMatrix(bAC)
                    val sectorNumber = accessConditions[j].split(": ")[1]
                    addSectorAC(
                        acMatrix, getString(R.string.text_sector) + ": " + sectorNumber,
                        hasMoreThan4Blocks
                    )
                    j += 2
                }
            }
        } else {
            Log.d(LOG_TAG, "There were no access conditions in intent.")
            finish()
        }
    }

    /**
     * Add full access condition information about one sector to the layout table.
     */
    private fun addSectorAC(
        acMatrix: Array<ByteArray>?,
        sectorHeader: String,
        hasMoreThan4Blocks: Boolean
    ) {
        // Add sector header.
        val header = TextView(this)
        header.setText(
            Common.colorString(sectorHeader, Common.getThemeAccentColor(this)),
            TextView.BufferType.SPANNABLE
        )
        var tr = TableRow(this)
        tr.layoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        tr.addView(header)
        mLayout?.addView(
            tr, TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
        )

        if (acMatrix == null) {
            val error = TextView(this)
            val errorText = getString(R.string.text_invalid_ac)
            error.setText(
                Common.colorString(errorText, ContextCompat.getColor(this, R.color.red)),
                TextView.BufferType.SPANNABLE
            )
            tr = TableRow(this)
            tr.layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
            tr.addView(error)
            mLayout?.addView(
                tr, TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT
                )
            )
        } else {
            // Add Block 0-2.
            addBlockAC(acMatrix, hasMoreThan4Blocks)
            // Add Sector Trailer.
            addSectorTrailerAC(acMatrix)
        }
    }

    /**
     * Add full access condition information of the 3 data blocks to the table.
     */
    private fun addBlockAC(acMatrix: Array<ByteArray>, hasMoreThan4Blocks: Boolean) {
        val isKeyBReadable = Common.isKeyBReadable(
            acMatrix[0][3], acMatrix[1][3], acMatrix[2][3]
        )

        for (i in 0 until 3) {
            val c1 = acMatrix[0][i]
            val c2 = acMatrix[1][i]
            val c3 = acMatrix[2][i]
            // Create row and header.
            val tr = TableRow(this)
            val blockHeader: String = if (hasMoreThan4Blocks) {
                getString(R.string.text_block) + ": " + (i * 4 + i) + "-" + (i * 4 + 4 + i)
            } else {
                getString(R.string.text_block) + ": " + i
            }
            tr.layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
            // Create cells.
            val location = TextView(this)
            location.text = blockHeader
            val read = TextView(this)
            val write = TextView(this)
            val incr = TextView(this)
            val decr = TextView(this)

            // Set cell texts to colored permissions.
            read.text = getColoredPermissionText(c1, c2, c3, Operation.Read, false, isKeyBReadable)
            write.text = getColoredPermissionText(c1, c2, c3, Operation.Write, false, isKeyBReadable)
            incr.text = getColoredPermissionText(c1, c2, c3, Operation.Increment, false, isKeyBReadable)
            decr.text = getColoredPermissionText(c1, c2, c3, Operation.DecTransRest, false, isKeyBReadable)

            // Add cells to row.
            tr.addView(location)
            tr.addView(read)
            tr.addView(write)
            tr.addView(incr)
            tr.addView(decr)
            // Add row to layout.
            mLayout?.addView(
                tr, TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    /**
     * Add full access condition information of the sector trailer (last block) to the table.
     */
    private fun addSectorTrailerAC(acMatrix: Array<ByteArray>) {
        val c1 = acMatrix[0][3]
        val c2 = acMatrix[1][3]
        val c3 = acMatrix[2][3]
        // Create rows.
        val read = Array(3) { TextView(this) }
        val write = Array(3) { TextView(this) }

        // Set row texts to colored permissions.
        read[0].text = getColoredPermissionText(c1, c2, c3, Operation.ReadKeyA, true, false)
        write[0].text = getColoredPermissionText(c1, c2, c3, Operation.WriteKeyA, true, false)
        read[1].text = getColoredPermissionText(c1, c2, c3, Operation.ReadAC, true, false)
        write[1].text = getColoredPermissionText(c1, c2, c3, Operation.WriteAC, true, false)
        read[2].text = getColoredPermissionText(c1, c2, c3, Operation.ReadKeyB, true, false)
        write[2].text = getColoredPermissionText(c1, c2, c3, Operation.WriteKeyB, true, false)

        // Add rows to layout.
        val headers = arrayOf("Key A:", "AC Bits:", "Key B:")
        for (i in 0 until 3) {
            val tr = TableRow(this)
            tr.layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
            val location = TextView(this)
            location.text = headers[i]
            tr.addView(location)
            tr.addView(read[i])
            tr.addView(write[i])
            mLayout?.addView(
                tr, TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    /**
     * A helper function creating a colored text depending on the Access Conditions and requested operation.
     */
    private fun getColoredPermissionText(
        c1: Byte,
        c2: Byte,
        c3: Byte,
        op: Operation,
        isSectorTrailer: Boolean,
        isKeyBReadable: Boolean
    ): SpannableString {
        return when (Common.getOperationRequirements(c1, c2, c3, op, isSectorTrailer, isKeyBReadable)) {
            0 -> Common.colorString(
                getString(R.string.text_never),
                ContextCompat.getColor(this, R.color.orange)
            )
            1 -> Common.colorString(
                getString(R.string.text_key_a),
                ContextCompat.getColor(this, R.color.yellow)
            )
            2 -> Common.colorString(
                getString(R.string.text_key_b),
                ContextCompat.getColor(this, R.color.yellow)
            )
            3 -> Common.colorString(
                getString(R.string.text_key_ab),
                ContextCompat.getColor(this, R.color.light_green)
            )
            4 -> Common.colorString(
                getString(R.string.text_ac_error),
                ContextCompat.getColor(this, R.color.red)
            )
            else -> SpannableString("")
        }
    }
}
