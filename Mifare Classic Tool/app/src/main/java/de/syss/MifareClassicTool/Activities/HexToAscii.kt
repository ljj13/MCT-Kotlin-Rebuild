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
import androidx.activity.compose.setContent
import de.syss.MifareClassicTool.ui.hextoascii.HexToAsciiScreen
import de.syss.MifareClassicTool.ui.theme.MctTheme

/**
 * Display a (hex) dump as 7-Bit US-ASCII using Jetpack Compose Material 3.
 * Retains BasicActivity NFC lifecycle and hardware contract.
 * @author Gerhard Klostermeier
 */
class HexToAscii : BasicActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        val dump = if (intent != null && intent.hasExtra(DumpEditor.EXTRA_DUMP)) {
            val d = intent.getStringArrayExtra(DumpEditor.EXTRA_DUMP)
            intent = null
            d
        } else {
            null
        }

        setContent {
            MctTheme {
                HexToAsciiScreen(
                    dump = dump,
                    onBackClick = { finish() }
                )
            }
        }
    }
}
