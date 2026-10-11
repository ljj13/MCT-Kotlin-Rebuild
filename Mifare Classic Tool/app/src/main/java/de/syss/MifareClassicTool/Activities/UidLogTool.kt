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

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R
import java.io.File
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections

/**
 * Tool to display and share the UIDs of previously detected tags.
 * @author Gerhard Klostermeier
 * @see Common.treatAsNewTag
 * @see Common.logUid
 * @see Common.UID_LOG_FILE
 */
class UidLogTool : BasicActivity() {

    private lateinit var mUidLog: TextView

    /**
     * Calls [updateUidLog] (and initializes some member variables).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uid_log_tool)
        mUidLog = findViewById(R.id.textViewUidLogToolUids)
        updateUidLog()
    }

    /**
     * Calls [BasicActivity.onNewIntent] and then calls [updateUidLog].
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        updateUidLog()
    }

    /**
     * Add the menu with the share/clear functions to the Activity.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.uid_log_tool_functions, menu)
        return true
    }

    /**
     * Handle the selected function from the menu.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuUidLogToolShare -> {
                shareUidLog()
                true
            }
            R.id.menuUidLogToolClear -> {
                clearUidLog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Read the UID log file [Common.UID_LOG_FILE] and display its content.
     */
    private fun updateUidLog() {
        val log = File(filesDir, Common.HOME_DIR + File.separator + Common.UID_LOG_FILE)
        val logEntries = Common.readFileLineByLine(log, false, this)
        if (logEntries != null) {
            // Reverse order (newest top).
            val tempEntries = ArrayList(Arrays.asList(*logEntries))
            Collections.reverse(tempEntries)
            mUidLog.text = TextUtils.join(
                System.getProperty("line.separator") ?: "\n", tempEntries
            )
        } else {
            // No log yet.
            mUidLog.setText(R.string.text_no_uid_logs)
        }
    }

    /**
     * Delete the UID log file [Common.UID_LOG_FILE] and update the UI (call [updateUidLog]).
     */
    private fun clearUidLog() {
        val log = File(filesDir, Common.HOME_DIR + File.separator + Common.UID_LOG_FILE)
        if (log.exists()) {
            log.delete()
        }
        updateUidLog()
    }

    /**
     * Share the UID log file [Common.UID_LOG_FILE] as text file.
     */
    private fun shareUidLog() {
        val log = File(filesDir, Common.HOME_DIR + File.separator + Common.UID_LOG_FILE)
        if (log.exists()) {
            Common.shareTextFile(this, log)
        }
    }
}
