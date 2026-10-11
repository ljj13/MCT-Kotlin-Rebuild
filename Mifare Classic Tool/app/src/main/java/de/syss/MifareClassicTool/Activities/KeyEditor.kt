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

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.GregorianCalendar
import java.util.Locale

/**
 * Show and edit key files.
 * @author Gerhard Klostermeier
 */
class KeyEditor : BasicActivity(), IActivityThatReactsToSave {

    private lateinit var mKeys: EditText
    private var mFileName: String? = null
    private var mLines: Array<String>? = null
    private var mKeysChanged: Boolean = false
    private var mCloseAfterSuccessfulSave: Boolean = false

    /**
     * Initialize the key editor with key data from intent or recreate its previously stored state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_key_editor)

        mKeys = findViewById(R.id.editTextKeyEditorKeys)
        mKeys.typeface = Typeface.MONOSPACE

        val intent = intent
        if (savedInstanceState != null) {
            mCloseAfterSuccessfulSave = savedInstanceState.getBoolean("close_after_successful_save")
            mKeysChanged = savedInstanceState.getBoolean("keys_changed")
            mFileName = savedInstanceState.getString("file_name")
            mLines = savedInstanceState.getStringArray("lines")
        } else if (intent != null && intent.hasExtra(FileChooser.EXTRA_CHOSEN_FILE)) {
            val keyFilePath = intent.getStringExtra(FileChooser.EXTRA_CHOSEN_FILE)
            val keyFile = if (keyFilePath != null) File(keyFilePath) else null
            if (keyFile != null) {
                mFileName = keyFile.name
                title = "$title ($mFileName)"
                if (keyFile.exists()) {
                    val keyDump = Common.readFileLineByLine(keyFile, true, this)
                    if (keyDump == null) {
                        finish()
                        return
                    }
                    setKeyArrayAsText(keyDump)
                }
            }

            mKeys.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    mKeysChanged = true
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            setIntent(null)
        } else {
            finish()
        }
    }

    /**
     * Save important state data before this activity gets destroyed.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("close_after_successful_save", mCloseAfterSuccessfulSave)
        outState.putBoolean("keys_changed", mKeysChanged)
        outState.putString("file_name", mFileName)
        outState.putStringArray("lines", mLines)
    }

    /**
     * Add the menu with the editor functions to the Activity.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.key_editor_functions, menu)
        return true
    }

    /**
     * Handle the selected function from the editor menu.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuKeyEditorSave -> {
                onSave()
                true
            }
            R.id.menuKeyEditorShare -> {
                shareKeyFile()
                true
            }
            R.id.menuKeyEditorRemoveDuplicates -> {
                removeDuplicates()
                true
            }
            R.id.menuKeyEditorExportKeys -> {
                exportKeys()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Show a dialog if there are unsaved changes.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (mKeysChanged) {
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_save_before_quitting_title)
                .setMessage(R.string.dialog_save_before_quitting)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(R.string.action_save) { _, _ ->
                    mCloseAfterSuccessfulSave = true
                    onSave()
                }
                .setNeutralButton(R.string.action_cancel) { _, _ -> }
                .setNegativeButton(R.string.action_dont_save) { _, _ ->
                    finish()
                }
                .show()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    /**
     * Callback for successful save.
     */
    override fun onSaveSuccessful() {
        if (mCloseAfterSuccessfulSave) {
            finish()
        }
        mKeysChanged = false
    }

    /**
     * Callback for failed save.
     */
    override fun onSaveFailure() {
        mCloseAfterSuccessfulSave = false
    }

    /**
     * Share a key file.
     */
    private fun shareKeyFile() {
        val file = saveKeysToTemp()
        if (file == null || !file.exists() || file.isDirectory) {
            return
        }
        Common.shareTextFile(this, file)
    }

    /**
     * Export the keys using [ImportExportTool].
     */
    private fun exportKeys() {
        val file = saveKeysToTemp()
        if (file == null || !file.exists() || file.isDirectory) {
            return
        }
        val intent = Intent(this, ImportExportTool::class.java)
        intent.putExtra(ImportExportTool.EXTRA_FILE_PATH, file.absolutePath)
        intent.putExtra(ImportExportTool.EXTRA_IS_DUMP_FILE, false)
        startActivity(intent)
    }

    /**
     * Save keys to temporary file in tmp dir.
     */
    private fun saveKeysToTemp(): File? {
        if (!Common.isValidKeyFileErrorToast(checkDumpAndUpdateLines(), this)) {
            return null
        }
        val currentFileName = mFileName
        val fileName = if (currentFileName.isNullOrEmpty()) {
            val calendar = GregorianCalendar()
            val fmt = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            fmt.calendar = calendar
            fmt.format(calendar.time)
        } else {
            currentFileName
        }
        val file = Common.getFile(Common.TMP_DIR + "/" + fileName)
        if (!Common.saveFile(file, mLines, false)) {
            Toast.makeText(this, R.string.info_save_error, Toast.LENGTH_LONG).show()
            return null
        }
        return file
    }

    /**
     * Check if valid key file, prompt for name, and save.
     */
    private fun onSave() {
        if (!Common.isValidKeyFileErrorToast(checkDumpAndUpdateLines(), this)) {
            return
        }
        val path = Common.getFile(Common.KEYS_DIR)
        val dialogLayout = layoutInflater.inflate(
            R.layout.dialog_save_file,
            findViewById(android.R.id.content), false
        )
        val message = dialogLayout.findViewById<TextView>(R.id.textViewDialogSaveFileMessage)
        val input = dialogLayout.findViewById<EditText>(R.id.editTextDialogSaveFileName)
        message.setText(R.string.dialog_save_keys)
        input.setText(mFileName)
        input.requestFocus()
        input.setSelection(0)

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_save_keys_title)
            .setIcon(android.R.drawable.ic_menu_save)
            .setView(dialogLayout)
            .setPositiveButton(R.string.action_ok) { _, _ ->
                saveFile(input.text.toString(), path)
            }
            .setNegativeButton(R.string.action_cancel) { _, _ ->
                mCloseAfterSuccessfulSave = false
            }
            .show()
    }

    /**
     * Save lines to file.
     */
    private fun saveFile(fileName: String?, path: File) {
        if (fileName.isNullOrEmpty() || fileName.contains("/")) {
            Toast.makeText(this, R.string.info_invalid_file_name, Toast.LENGTH_LONG).show()
            return
        }
        if (fileName == Common.STD_KEYS || fileName == Common.STD_KEYS_EXTENDED ||
            fileName == Common.STD_HOTEL_KEYS
        ) {
            Toast.makeText(this, R.string.info_std_key_overwrite, Toast.LENGTH_LONG).show()
            return
        }
        val file = File(path.path, fileName)
        val currentLines = mLines ?: return
        Common.checkFileExistenceAndSave(file, currentLines, false, this, this)
    }

    /**
     * Remove duplicates from key file.
     */
    private fun removeDuplicates() {
        if (Common.isValidKeyFileErrorToast(checkDumpAndUpdateLines(), this)) {
            val currentLines = mLines ?: return
            val newLines = ArrayList<String>()
            for (rawLine in currentLines) {
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) {
                    newLines.add(line)
                    continue
                }
                if (!newLines.contains(line)) {
                    newLines.add(line)
                }
            }
            mLines = newLines.toTypedArray()
            setKeyArrayAsText(mLines!!)
        }
    }

    /**
     * Update the keys text field with lines.
     */
    private fun setKeyArrayAsText(lines: Array<String>) {
        val keyText = StringBuilder()
        val s = System.getProperty("line.separator") ?: "\n"
        for (i in 0 until lines.size - 1) {
            keyText.append(lines[i])
            keyText.append(s)
        }
        keyText.append(lines[lines.size - 1])
        mKeys.setText(keyText)
    }

    /**
     * Validate and convert keys to uppercase.
     */
    private fun checkDumpAndUpdateLines(): Int {
        val editorContent = mKeys.text ?: return 1
        val editorText = editorContent.toString()
        val lines = editorText.split(Regex(System.getProperty("line.separator") ?: "\n")).toTypedArray()
        val ret = Common.isValidKeyFile(lines)
        if (ret != 0) {
            return ret
        }

        // Convert keys to uppercase.
        val newLines = arrayOfNulls<String>(lines.size)
        for (i in lines.indices) {
            var line = lines[i].trim()
            if (line.startsWith("#")) {
                newLines[i] = lines[i]
                continue
            }
            val content = line.split("#")[0].trim()
            if (content.isEmpty()) {
                newLines[i] = lines[i]
                continue
            }
            // Line is a key. Convert to uppercase.
            val upperContent = content.uppercase(Locale.getDefault())
            newLines[i] = upperContent + line.substring(12)
        }

        @Suppress("UNCHECKED_CAST")
        mLines = newLines as Array<String>
        setKeyArrayAsText(mLines!!)
        mKeysChanged = false
        return ret
    }
}
