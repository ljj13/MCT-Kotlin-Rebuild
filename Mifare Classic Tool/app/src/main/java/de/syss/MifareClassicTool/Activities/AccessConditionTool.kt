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
import de.syss.MifareClassicTool.ui.accesscondition.AccessConditionScreen
import de.syss.MifareClassicTool.ui.theme.MctTheme

/**
 * Decode MIFARE Classic Access Conditions from their hex format to a more human readable
 * format and vice versa using Jetpack Compose Material 3.
 * Retains BasicActivity NFC lifecycle and hardware contract.
 * @author Gerhard Klostermeier
 */
class AccessConditionTool : BasicActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContent {
            MctTheme {
                AccessConditionScreen(
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
        // Handled in Compose AccessConditionScreen
    }

    /**
     * Legacy reflection stub preserved for XML compatibility.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onEncode(view: View) {
        // Handled in Compose AccessConditionScreen
    }

    /**
     * Legacy reflection stub preserved for XML compatibility.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onChooseACforDataBock(view: View) {
        // Handled in Compose AccessConditionScreen
    }

    /**
     * Legacy reflection stub preserved for XML compatibility.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onChooseACforSectorTrailer(view: View) {
        // Handled in Compose AccessConditionScreen
    }

    /**
     * Legacy reflection stub preserved for XML compatibility.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onCopyToClipboard(view: View) {
        // Handled in Compose AccessConditionScreen
    }

    /**
     * Legacy reflection stub preserved for XML compatibility.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onPasteFromClipboard(view: View) {
        // Handled in Compose AccessConditionScreen
    }
}
