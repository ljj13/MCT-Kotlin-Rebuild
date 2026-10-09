/*
 * Copyright 2020 Gerhard Klostermeier
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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.Toast
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R
import java.util.Locale

/**
 * Convert data from formats like ASCII/hex/bin to each other.
 * @author Gerhard Klostermeier
 */
class DataConversionTool : BasicActivity() {

    private lateinit var mAscii: EditText
    private lateinit var mHex: EditText
    private lateinit var mBin: EditText
    private lateinit var mDecBE: EditText
    private lateinit var mDecLE: EditText

    /**
     * Initialize some member variables.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_conversion_tool)
        mAscii = findViewById(R.id.editTextDataConversionToolAscii)
        mHex = findViewById(R.id.editTextDataConversionToolHex)
        mBin = findViewById(R.id.editTextDataConversionToolBin)
        mDecBE = findViewById(R.id.editTextDataConversionToolDecBE)
        mDecLE = findViewById(R.id.editTextDataConversionToolDecLE)
    }

    /**
     * Convert the data from the source input (determined by the view /
     * the button) to a hex string and call [convertData].
     * @param view The View object that triggered the method
     * (in this case any of the convert button).
     * @see convertData
     */
    fun onConvert(view: View) {
        when (view.id) {
            R.id.imageButtonDataConversionToolAscii -> {
                val ascii = mAscii.text.toString()
                convertData(Common.ascii2Hex(ascii))
            }
            R.id.imageButtonDataConversionToolHex -> {
                val hex = mHex.text.toString()
                if (Common.isHex(hex, this)) {
                    convertData(hex)
                }
            }
            R.id.imageButtonDataConversionToolBin -> {
                val bin = mBin.text.toString()
                if (isBin(bin, this)) {
                    convertData(Common.bin2Hex(bin))
                }
            }
            R.id.imageButtonDataConversionToolDecBE -> {
                val dec = mDecBE.text.toString()
                if (isDec(dec, this)) {
                    convertData(Common.decBE2Hex(dec))
                }
            }
            R.id.imageButtonDataConversionToolDecLE -> {
                val dec = mDecLE.text.toString()
                if (isDec(dec, this)) {
                    convertData(Common.decLE2Hex(dec))
                }
            }
        }
    }

    /**
     * Convert the data from a hex string to different output
     * formats and update the corresponding UI object.
     * @param hex The hex string to be converted into different formats.
     */
    private fun convertData(hex: String?) {
        if (hex.isNullOrEmpty()) {
            Toast.makeText(this, R.string.info_convert_error, Toast.LENGTH_SHORT).show()
            return
        }
        // Hex.
        mHex.setText(hex.uppercase(Locale.ROOT))
        // ASCII.
        val ascii = Common.hex2Ascii(hex)
        if (ascii != null) {
            mAscii.setText(ascii)
        } else {
            mAscii.setText(R.string.text_not_ascii)
        }
        // Bin.
        mBin.setText(Common.hex2Bin(hex))
        // Dec (BE).
        mDecBE.setText(Common.hex2DecBE(hex))
        // Dec (LE).
        mDecLE.setText(Common.hex2DecLE(hex))
    }

    /**
     * Check if a string represents binary bytes (0/1, multiple of 8).
     * @param bin The binary string to check.
     * @param context The Context in which an error Toast will be shown.
     * @return True if string is binary. False otherwise.
     */
    private fun isBin(bin: String?, context: Context): Boolean {
        if (bin != null && bin.length % 8 == 0 && bin.matches(Regex("[0-1]+"))) {
            return true
        }
        Toast.makeText(context, R.string.info_not_bin_data, Toast.LENGTH_LONG).show()
        return false
    }

    /**
     * Check if a string represents a decimal number.
     * @param dec Decimal string to check.
     * @param context The Context in which an error Toast will be shown.
     * @return True if string is decimal. False otherwise.
     */
    private fun isDec(dec: String?, context: Context): Boolean {
        if (dec.isNullOrEmpty() || !dec.matches(Regex("\\d+"))) {
            Toast.makeText(context, R.string.info_not_dec_data, Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    /**
     * Open a generic online type converter with the input from [mHex].
     * @param view The View object that triggered the method
     */
    @Suppress("UNUSED_PARAMETER")
    fun onOpenGenericConverter(view: View) {
        val hex = mHex.text.toString()
        if (hex.isNotEmpty() && !Common.isHex(hex, this)) {
            return
        }
        val url = "https://hexconverter.scadacore.com/?HexString=$hex"
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    /**
     * Open a multi-purpose online format converter.
     * @param view The View object that triggered the method
     */
    @Suppress("UNUSED_PARAMETER")
    fun onOpenMultiPurposeConverter(view: View) {
        val url = "https://cryptii.com/pipes/integer-encoder"
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    /**
     * Open the CyberChef website with the input from [mHex].
     * @param view The View object that triggered the method
     */
    @Suppress("UNUSED_PARAMETER")
    fun onOpenCyberChef(view: View) {
        val hex = mHex.text.toString()
        if (hex.isNotEmpty() && !Common.isHex(hex, this)) {
            return
        }
        var base64 = Base64.encodeToString(hex.toByteArray(), Base64.DEFAULT)
        base64 = base64.trim()
        base64 = base64.replace("=", "")
        val url = "https://gchq.github.io/CyberChef/#recipe=From_Hex('Auto')&input=$base64"
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }
}
