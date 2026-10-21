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
import android.view.View
import androidx.activity.compose.setContent
import de.syss.MifareClassicTool.ui.theme.MctTheme
import de.syss.MifareClassicTool.ui.valueblock.ValueBlockScreen

/**
 * Decode MIFARE Classic Value Blocks from their hex format to an integer
 * and vice versa using Jetpack Compose Material 3.
 * Retains BasicActivity NFC lifecycle and hardware contract.
 * @author Gerhard Klostermeier
 */
class ValueBlockTool : BasicActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContent {
            MctTheme {
                ValueBlockScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }

    /**
     * Legacy reflection stub preserved for XML compatibility.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onDecode(view: View) {
        // Handled in Compose BccScreen/ValueBlockScreen
    }

    /**
     * Legacy reflection stub preserved for XML compatibility.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onEncode(view: View) {
        // Handled in Compose ValueBlockScreen
    }

    /**
     * Legacy reflection stub preserved for XML compatibility.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onCopyToClipboard(view: View) {
        // Handled in Compose ValueBlockScreen
    }

    /**
     * Legacy reflection stub preserved for XML compatibility.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onPasteFromClipboard(view: View) {
        // Handled in Compose ValueBlockScreen
    }
}
