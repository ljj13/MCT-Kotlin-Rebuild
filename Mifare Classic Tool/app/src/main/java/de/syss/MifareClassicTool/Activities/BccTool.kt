/*
 * Copyright 2015 Gerhard Klostermeier
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
import android.view.View
import android.widget.Toast
import androidx.activity.compose.setContent
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.bcc.BccScreen
import de.syss.MifareClassicTool.ui.theme.MctTheme

/**
 * Calculate the BCC value of a given UID using Jetpack Compose + Material 3.
 * Retains BasicActivity NFC lifecycle and hardware contract.
 * @author Gerhard Klostermeier
 */
class BccTool : BasicActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContent {
            MctTheme {
                BccScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }

    /**
     * Preserved calculation method for backward compatibility and test verification.
     */
    fun calculateBcc(uidHex: String): String? {
        val data = uidHex.trim()
        if (data.length != 8) {
            Toast.makeText(this, R.string.info_invalid_uid_length, Toast.LENGTH_LONG).show()
            return null
        }
        if (!data.matches(Regex("[0-9A-Fa-f]+"))) {
            Toast.makeText(this, R.string.info_not_hex_data, Toast.LENGTH_LONG).show()
            return null
        }

        val uidBytes = Common.hex2Bytes(data) ?: return null
        val bcc = Common.calcBcc(uidBytes)
        return String.format("%02X", bcc)
    }

    /**
     * Legacy reflection stub preserved for XML compatibility.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onCalculate(view: View) {
        // Compose handles calculation internally via BccScreen
    }

    /**
     * Legacy reflection stub preserved for XML compatibility.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onCopyToClipboard(view: View) {
        // Compose handles copy internally via BccScreen
    }

    /**
     * Legacy reflection stub preserved for XML compatibility.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onPasteFromClipboard(view: View) {
        // Compose handles paste internally via BccScreen
    }
}
