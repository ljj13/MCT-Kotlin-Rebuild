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
import de.syss.MifareClassicTool.ui.help.HelpAndInfoScreen
import de.syss.MifareClassicTool.ui.theme.MctTheme

/**
 * In-App (offline) help and info.
 * Retains BasicActivity NFC foreground dispatch and offline HTML WebView rendering.
 * @author Gerhard Klostermeier
 */
class HelpAndInfo : BasicActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        setContent {
            MctTheme {
                HelpAndInfoScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}
