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
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R
import java.nio.ByteBuffer

/**
 * Display value blocks in a way a user can read easily (as integer).
 * NXP has PDFs describing what value blocks are.
 * This Activity will be shown from the [DumpEditor], if the user
 * clicks the corresponding menu item.
 * @author Gerhard Klostermeier
 */
class ValueBlocksToInt : BasicActivity() {

    companion object {
        const val EXTRA_VB: String = "de.syss.MifareClassicTool.Activity.VB"
        private val LOG_TAG: String = ValueBlocksToInt::class.java.simpleName
    }

    private var mLayout: TableLayout? = null

    /**
     * Get value blocks from Intent and initialize Activity to
     * displaying them. If there is no Intent with [EXTRA_VB], the Activity will be exited.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_value_blocks_to_int)

        var noValueBlocks = false
        if (intent.hasExtra(EXTRA_VB)) {
            mLayout = findViewById(R.id.tableLayoutValueBlocksToInt)
            val valueBlocks = intent.getStringArrayExtra(EXTRA_VB)
            if (!valueBlocks.isNullOrEmpty()) {
                var i = 0
                while (i < valueBlocks.size) {
                    val sectorAndBlock = valueBlocks[i].split(", ")
                    val sectorNumber = sectorAndBlock[0].split(": ")[1]
                    val blockNumber = sectorAndBlock[1].split(": ")[1]
                    addPosInfoRow(
                        getString(R.string.text_sector) + ": " + sectorNumber + ", " +
                                getString(R.string.text_block) + ": " + blockNumber
                    )
                    addValueBlock(valueBlocks[i + 1])
                    i += 2
                }
            } else {
                noValueBlocks = true
            }
        } else {
            noValueBlocks = true
        }

        if (noValueBlocks) {
            Log.d(LOG_TAG, "There was no value block in intent.")
            finish()
        }
    }

    /**
     * Add a row with position information to the layout table.
     * This row shows the user where the value block is located (sector, block).
     * @param value The position information (e.g. "Sector: 1, Block: 2").
     */
    private fun addPosInfoRow(value: String) {
        val header = TextView(this)
        header.setText(
            Common.colorString(value, Common.getThemeAccentColor(this)),
            TextView.BufferType.SPANNABLE
        )
        val tr = TableRow(this)
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
    }

    /**
     * Add full value block information (original
     * and integer format) to the layout table (two rows).
     * @param hexValueBlock The value block as hex string (32 chars.).
     */
    private fun addValueBlock(hexValueBlock: String) {
        var tr = TableRow(this)
        var what = TextView(this)
        var value = TextView(this)

        // Original.
        tr.layoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        what.setText(R.string.text_vb_orig)
        value.text = Common.colorString(
            hexValueBlock.substring(0, 8),
            ContextCompat.getColor(this, R.color.yellow)
        )
        tr.addView(what)
        tr.addView(value)
        mLayout?.addView(
            tr, TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // Resolved to int.
        tr = TableRow(this)
        tr.layoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        what = TextView(this)
        what.setText(R.string.text_vb_as_int_decoded)
        value = TextView(this)
        val asBytes = Common.hex2Bytes(hexValueBlock.substring(0, 8)) ?: return
        Common.reverseByteArrayInPlace(asBytes)
        val bb = ByteBuffer.wrap(asBytes)
        val i = bb.int
        val asInt = "" + i
        value.text = Common.colorString(
            asInt,
            ContextCompat.getColor(this, R.color.light_green)
        )
        tr.addView(what)
        tr.addView(value)
        mLayout?.addView(
            tr, TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }
}
