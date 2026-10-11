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
import android.text.TextUtils
import android.widget.TextView
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R

/**
 * Display a (hex) dump as 7-Bit US-ASCII.
 * This Activity will be shown from the [DumpEditor], if the user
 * clicks the corresponding menu item.
 * @author Gerhard Klostermeier
 */
class HexToAscii : BasicActivity() {

    /**
     * Initialize the activity with the data from the Intent
     * ([DumpEditor.EXTRA_DUMP]) by displaying them as
     * US-ASCII. Non printable ASCII characters will be displayed as ".".
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hex_to_ascii)

        if (intent.hasExtra(DumpEditor.EXTRA_DUMP)) {
            val dump = intent.getStringArrayExtra(DumpEditor.EXTRA_DUMP)
            if (!dump.isNullOrEmpty()) {
                val s = System.getProperty("line.separator") ?: "\n"
                var ascii: CharSequence = ""
                for (line in dump) {
                    if (line.startsWith("+")) {
                        // Header.
                        val sectorNumber = line.split(": ")[1]
                        ascii = TextUtils.concat(
                            ascii, Common.colorString(
                                getString(R.string.text_sector) + ": " + sectorNumber,
                                Common.getThemeAccentColor(this)
                            ), s
                        )
                    } else {
                        // Data.
                        var converted = Common.hex2Ascii(line)
                        if (converted == null) {
                            converted = getString(R.string.text_invalid_data)
                        }
                        ascii = TextUtils.concat(ascii, " ", converted, s)
                    }
                }
                val tv = findViewById<TextView>(R.id.textViewHexToAscii)
                tv.text = ascii
            }
            intent = null
        }
    }
}
