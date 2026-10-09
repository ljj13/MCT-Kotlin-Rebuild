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

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R
import java.nio.ByteBuffer
import java.util.Locale

/**
 * Decode MIFARE Classic Value Blocks from their hex format
 * to an integer and vice versa.
 * @author Gerhard Klostermeier
 */
class ValueBlockTool : BasicActivity() {

    private lateinit var mVB: EditText
    private lateinit var mVBasInt: EditText
    private lateinit var mAddr: EditText

    /**
     * Initialize some member variables.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_value_block_tool)

        mVB = findViewById(R.id.editTextValueBlockToolVB)
        mVBasInt = findViewById(R.id.editTextValueBlockToolVBasInt)
        mAddr = findViewById(R.id.editTextValueBlockAddr)
    }

    /**
     * Decode a MIFARE Classic Value Block into an integer and the Addr value.
     * @param view The View object that triggered the method
     * (in this case the decode button).
     */
    @SuppressLint("SetTextI18n")
    @Suppress("UNUSED_PARAMETER")
    fun onDecode(view: View) {
        val data = mVB.text.toString()
        if (!Common.isHexAnd16Byte(data, this)) {
            // Error. Not hex and 16 byte.
            return
        }
        if (!Common.isValueBlock(data)) {
            // Error. No value block.
            Toast.makeText(this, R.string.info_is_not_vb, Toast.LENGTH_LONG).show()
            return
        }
        // Decode.
        val vbAsBytes = Common.hex2Bytes(data.substring(0, 8)) ?: return
        // Bytes -> Int. -> reverse.
        val vbAsInt = Integer.reverseBytes(ByteBuffer.wrap(vbAsBytes).int)
        mVBasInt.setText("" + vbAsInt)
        mAddr.setText(data.substring(24, 26))
    }

    /**
     * Encode an integer (and addr.) into a MIFARE Classic Value Block.
     * @param view The View object that triggered the method
     * (in this case the encode button).
     */
    @SuppressLint("SetTextI18n")
    @Suppress("UNUSED_PARAMETER")
    fun onEncode(view: View) {
        val vbText = mVBasInt.text.toString()
        val addrText = mAddr.text.toString()
        if (vbText.isEmpty()) {
            // Error. There is no integer to encode.
            Toast.makeText(this, R.string.info_no_int_to_encode, Toast.LENGTH_LONG).show()
            return
        }
        if (!addrText.matches(Regex("[0-9A-Fa-f]{2}"))) {
            // Error. There is no valid value block addr.
            Toast.makeText(this, R.string.info_addr_not_hex_byte, Toast.LENGTH_LONG).show()
            return
        }
        // Encode.
        // String -> Int.
        val vbAsInt: Int
        try {
            vbAsInt = vbText.toInt()
        } catch (e: NumberFormatException) {
            // Error. Number was more than Integer.MAX_VALUE or less than Integer.MIN_VALUE.
            val message = getString(R.string.info_invalid_int) +
                    " (Max: " + Int.MAX_VALUE + ", Min: " + Int.MIN_VALUE + ")"
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            return
        }
        // Int. -> reverse -> byte array -> hex string.
        val vb = Common.bytes2Hex(
            ByteBuffer.allocate(4).putInt(Integer.reverseBytes(vbAsInt)).array()
        )
        // Int. -> invert -> reverse -> byte array -> hex string.
        val vbInverted = Common.bytes2Hex(
            ByteBuffer.allocate(4).putInt(Integer.reverseBytes(vbAsInt.inv())).array()
        )
        val addrInverted = Integer.toHexString(
            addrText.toInt(16).inv()
        ).uppercase(Locale.getDefault()).substring(6, 8)
        mVB.setText(vb + vbInverted + vb + addrText + addrInverted + addrText + addrInverted)
    }

    /**
     * Copy the MIFARE Classic Value Block to the Android clipboard.
     * @param view The View object that triggered the method
     * (in this case the copy button).
     */
    @Suppress("UNUSED_PARAMETER")
    fun onCopyToClipboard(view: View) {
        Common.copyToClipboard(mVB.text.toString(), this, true)
    }

    /**
     * Paste the content of the Android clipboard (if plain text) to the
     * value block edit text.
     * @param view The View object that triggered the method
     * (in this case the paste button).
     */
    @Suppress("UNUSED_PARAMETER")
    fun onPasteFromClipboard(view: View) {
        val text = Common.getFromClipboard(this)
        if (text != null) {
            mVB.setText(text)
        }
    }
}
