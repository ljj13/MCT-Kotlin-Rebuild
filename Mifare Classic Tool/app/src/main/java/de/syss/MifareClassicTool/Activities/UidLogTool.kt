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
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.theme.MctTheme
import de.syss.MifareClassicTool.ui.uidlog.UidLogItem
import de.syss.MifareClassicTool.ui.uidlog.UidLogScreen
import java.io.File
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections

/**
 * Tool to display and share the UIDs of previously detected tags.
 * Retains BasicActivity NFC foreground dispatch and uid.log file operations.
 * @author Gerhard Klostermeier
 * @see Common.treatAsNewTag
 * @see Common.logUid
 * @see Common.UID_LOG_FILE
 */
class UidLogTool : BasicActivity() {

    private val mLogEntriesState = mutableStateOf<List<UidLogItem>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        updateUidLog()

        setContent {
            MctTheme {
                UidLogScreen(
                    logEntries = mLogEntriesState.value,
                    onBackClick = { finish() },
                    onShareClick = { shareUidLog() },
                    onClearClick = { clearUidLog() },
                    onCopyUid = { uid ->
                        Common.copyToClipboard(uid, this, true)
                    },
                    onCopyAll = {
                        val allText = mLogEntriesState.value.joinToString("\n") { it.rawLine }
                        if (allText.isNotEmpty()) {
                            Common.copyToClipboard(allText, this, true)
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        updateUidLog()
    }

    private fun getLogFile(): File {
        return Common.getFile(Common.UID_LOG_FILE)
    }

    /**
     * Read the UID log file [Common.UID_LOG_FILE] and update observable state.
     */
    private fun updateUidLog() {
        val log = getLogFile()
        android.util.Log.d("UidLogTool", "Log file: ${log.absolutePath}, exists: ${log.exists()}")
        val logEntries = Common.readFileLineByLine(log, false, this)
        android.util.Log.d("UidLogTool", "Log entries count: ${logEntries?.size}")
        if (logEntries != null) {
            val tempEntries = ArrayList(Arrays.asList(*logEntries))
            Collections.reverse(tempEntries)
            mLogEntriesState.value = tempEntries.map { line ->
                if (line.contains(": ")) {
                    val parts = line.split(": ", limit = 2)
                    UidLogItem(timestamp = parts[0], uid = parts[1], rawLine = line)
                } else {
                    UidLogItem(timestamp = "", uid = line, rawLine = line)
                }
            }
        } else {
            mLogEntriesState.value = emptyList()
        }
    }

    /**
     * Delete the UID log file [Common.UID_LOG_FILE] and update the UI.
     */
    private fun clearUidLog() {
        val log = getLogFile()
        if (log.exists()) {
            log.delete()
        }
        updateUidLog()
        Toast.makeText(this, R.string.action_clear_logs, Toast.LENGTH_SHORT).show()
    }

    /**
     * Share the UID log file [Common.UID_LOG_FILE] as text file.
     */
    private fun shareUidLog() {
        val log = getLogFile()
        if (log.exists()) {
            Common.shareTextFile(this, log)
        }
    }
}
