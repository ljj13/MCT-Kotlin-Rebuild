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

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import de.syss.MifareClassicTool.ui.conversion.DataConversionScreen
import de.syss.MifareClassicTool.ui.theme.MctTheme

/**
 * Convert data from formats like ASCII/hex/bin to each other using Jetpack Compose Material 3.
 * Retains BasicActivity NFC lifecycle and hardware contract.
 * @author Gerhard Klostermeier
 */
class DataConversionTool : BasicActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContent {
            MctTheme {
                DataConversionScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }

    /**
     * Legacy reflection stub preserved for XML compatibility.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onConvert(view: View) {
        // Handled in Compose DataConversionScreen
    }

    /**
     * Legacy reflection stub preserved for XML compatibility.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onOpenGenericConverter(view: View) {
        // Handled in Compose DataConversionScreen
    }

    /**
     * Legacy reflection stub preserved for XML compatibility.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onOpenMultiPurposeConverter(view: View) {
        // Handled in Compose DataConversionScreen
    }

    /**
     * Legacy reflection stub preserved for XML compatibility.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onOpenCyberChef(view: View) {
        // Handled in Compose DataConversionScreen
    }
}
