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

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import de.syss.MifareClassicTool.Common

/**
 * An Activity implementing the NFC foreground dispatch system overwriting
 * onResume() and onPause(). New Intents will be treated as new Tags.
 * @see Common.enableNfcForegroundDispatch
 * @see Common.disableNfcForegroundDispatch
 * @see Common.treatAsNewTag
 * @author Gerhard Klostermeier
 */
abstract class BasicActivity : AppCompatActivity() {

    /**
     * Enable NFC foreground dispatch system.
     */
    public override fun onResume() {
        super.onResume()
        Common.setPendingComponentName(componentName)
        Common.enableNfcForegroundDispatch(this)
    }

    /**
     * Disable NFC foreground dispatch system.
     */
    public override fun onPause() {
        super.onPause()
        Common.disableNfcForegroundDispatch(this)
    }

    /**
     * Handle new Intent as a new tag Intent and if the tag/device does not
     * support MIFARE Classic, then run [TagInfoTool].
     */
    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val typeCheck = Common.treatAsNewTag(intent, this)
        if (typeCheck == -1 || typeCheck == -2) {
            // Device or tag does not support MIFARE Classic.
            // Run the only thing that is possible: The tag info tool.
            val i = Intent(this, TagInfoTool::class.java)
            startActivity(i)
        }
    }
}
