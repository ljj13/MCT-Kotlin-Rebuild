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
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.runtime.mutableStateOf
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.keyeditor.KeyEditorScreen
import de.syss.MifareClassicTool.ui.keyeditor.KeyEditorUiState
import de.syss.MifareClassicTool.ui.theme.MctTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.GregorianCalendar
import java.util.Locale

/**
 * Show and edit key files using a hybrid architecture:
 * Compose Material 3 Shell + AndroidView(AppCompatEditText) editing core.
 * Preserves legacy FileChooser intent contracts, file IO, cursor, IME, and save callbacks.
 * @author Gerhard Klostermeier
 */
class KeyEditor : BasicActivity(), IActivityThatReactsToSave {

    private var mKeys: AppCompatEditText? = null
    private var mFileName: String? = null
    private var mLines: Array<String>? = null
    private var mKeysChanged: Boolean = false
    private var mCloseAfterSuccessfulSave: Boolean = false

    private val mUiState = mutableStateOf(KeyEditorUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        var initialText = ""
        val intent = intent

        if (savedInstanceState != null) {
            mCloseAfterSuccessfulSave = savedInstanceState.getBoolean("close_after_successful_save")
            mKeysChanged = savedInstanceState.getBoolean("keys_changed")
            mFileName = savedInstanceState.getString("file_name")
            mLines = savedInstanceState.getStringArray("lines")
            if (mLines != null) {
                initialText = mLines!!.joinToString("\n")
            }
        } else if (intent != null && intent.hasExtra(FileChooser.EXTRA_CHOSEN_FILE)) {
            val keyFilePath = intent.getStringExtra(FileChooser.EXTRA_CHOSEN_FILE)
            val keyFile = if (keyFilePath != null) File(keyFilePath) else null
            if (keyFile != null) {
                mFileName = keyFile.name
                if (keyFile.exists()) {
                    val keyDump = Common.readFileLineByLine(keyFile, true, this)
                    if (keyDump == null) {
                        finish()
                        return
                    }
                    mLines = keyDump
                    initialText = keyDump.joinToString("\n")
                }
            }
            setIntent(null)
        } else if (intent != null && intent.hasExtra("extra_test_content")) {
            val testContent = (intent.getStringExtra("extra_test_content") ?: "").replace("\\n", "\n")
            mFileName = intent.getStringExtra("extra_test_filename") ?: "test.keys"
            initialText = testContent
            mLines = testContent.split("\n").toTypedArray()
            setIntent(null)
        } else {
            finish()
            return
        }

        val lines = initialText.split("\n")
        val validKeyCount = calculateValidKeyCount(lines)

        mUiState.value = KeyEditorUiState(
            fileName = mFileName,
            hasChanges = mKeysChanged,
            initialContent = initialText,
            lineCount = lines.size,
            validKeyCount = validKeyCount,
            showSaveDialog = false,
            saveFileNameInput = mFileName ?: "",
            showExitConfirmDialog = false
        )

        setContent {
            MctTheme {
                KeyEditorScreen(
                    state = mUiState.value,
                    onStateChange = { mUiState.value = it },
                    editTextRef = { editText ->
                        mKeys = editText
                    },
                    onContentChanged = { newContent ->
                        mKeysChanged = true
                        val currentLines = newContent.split("\n")
                        val validKeys = calculateValidKeyCount(currentLines)
                        mUiState.value = mUiState.value.copy(
                            hasChanges = true,
                            lineCount = currentLines.size,
                            validKeyCount = validKeys
                        )
                    },
                    onBackClick = { handleBackPress() },
                    onSaveClick = { onSave() },
                    onConfirmSave = { fileName -> confirmSaveFile(fileName) },
                    onShareClick = { shareKeyFile() },
                    onRemoveDuplicatesClick = { removeDuplicates() },
                    onExportKeysClick = { exportKeys() },
                    onDiscardAndExit = { finish() }
                )
            }
        }

        if (intent.getBooleanExtra("extra_show_save_dialog", false)) {
            mUiState.value = mUiState.value.copy(
                showSaveDialog = true,
                saveFileNameInput = mFileName ?: ""
            )
        }
        if (intent.getBooleanExtra("extra_show_exit_dialog", false)) {
            mUiState.value = mUiState.value.copy(showExitConfirmDialog = true)
        }
    }

    private fun calculateValidKeyCount(lines: List<String>): Int {
        return lines.count { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                false
            } else {
                val hexKey = trimmed.split("#")[0].trim()
                hexKey.length == 12 && hexKey.matches(Regex("[0-9A-Fa-f]{12}"))
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val text = mKeys?.text?.toString()
        if (text != null) {
            mLines = text.split(Regex(System.getProperty("line.separator") ?: "\n")).toTypedArray()
        }
        outState.putBoolean("close_after_successful_save", mCloseAfterSuccessfulSave)
        outState.putBoolean("keys_changed", mKeysChanged)
        outState.putString("file_name", mFileName)
        outState.putStringArray("lines", mLines)
    }

    private fun handleBackPress() {
        if (mKeysChanged) {
            mUiState.value = mUiState.value.copy(showExitConfirmDialog = true)
        } else {
            finish()
        }
    }

    override fun onSaveSuccessful() {
        if (mCloseAfterSuccessfulSave) {
            finish()
        }
        mKeysChanged = false
        mUiState.value = mUiState.value.copy(hasChanges = false, fileName = mFileName)
    }

    override fun onSaveFailure() {
        mCloseAfterSuccessfulSave = false
    }

    private fun shareKeyFile() {
        val file = saveKeysToTemp()
        if (file == null || !file.exists() || file.isDirectory) {
            return
        }
        Common.shareTextFile(this, file)
    }

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

    private fun onSave() {
        val check = checkDumpAndUpdateLines()
        if (!Common.isValidKeyFileErrorToast(check, this)) {
            return
        }
        mUiState.value = mUiState.value.copy(
            showSaveDialog = true,
            saveFileNameInput = mFileName ?: ""
        )
    }

    private fun confirmSaveFile(fileName: String) {
        mUiState.value = mUiState.value.copy(showSaveDialog = false)
        val path = Common.getFile(Common.KEYS_DIR)
        saveFile(fileName, path)
    }

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
        mFileName = fileName
        Common.checkFileExistenceAndSave(file, currentLines, false, this, this)
    }

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
            val linesList = newLines.toList()
            val validKeyCount = calculateValidKeyCount(linesList)
            mUiState.value = mUiState.value.copy(
                lineCount = linesList.size,
                validKeyCount = validKeyCount,
                hasChanges = true
            )
            Toast.makeText(this, R.string.action_remove_duplicates, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setKeyArrayAsText(lines: Array<String>) {
        val s = System.getProperty("line.separator") ?: "\n"
        val keyText = lines.joinToString(s)
        mKeys?.setText(keyText)
    }

    private fun checkDumpAndUpdateLines(): Int {
        val editorContent = mKeys?.text ?: return 1
        val editorText = editorContent.toString()
        val lines = editorText.split(Regex(System.getProperty("line.separator") ?: "\n")).toTypedArray()
        val ret = Common.isValidKeyFile(lines)
        if (ret != 0) {
            return ret
        }

        val newLines = arrayOfNulls<String>(lines.size)
        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.startsWith("#")) {
                newLines[i] = lines[i]
                continue
            }
            val content = line.split("#")[0].trim()
            if (content.isEmpty()) {
                newLines[i] = lines[i]
                continue
            }
            val upperContent = content.uppercase(Locale.getDefault())
            newLines[i] = upperContent + line.substring(12)
        }

        @Suppress("UNCHECKED_CAST")
        mLines = newLines as Array<String>
        setKeyArrayAsText(mLines!!)
        mKeysChanged = false
        mUiState.value = mUiState.value.copy(hasChanges = false)
        return ret
    }
}
