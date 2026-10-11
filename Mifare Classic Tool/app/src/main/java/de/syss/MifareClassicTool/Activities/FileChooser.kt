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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.filechooser.FileChooserItem
import de.syss.MifareClassicTool.ui.filechooser.FileChooserScreen
import de.syss.MifareClassicTool.ui.filechooser.FileChooserUiState
import de.syss.MifareClassicTool.ui.theme.MctTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import java.util.Locale

/**
 * A simple generic file chooser that lets the user choose a file from
 * a given directory.
 * Retains BasicActivity NFC foreground dispatch and Intent contract (EXTRA_CHOSEN_FILE, EXTRA_CHOSEN_FILENAME).
 * @author Gerhard Klostermeier
 */
class FileChooser : BasicActivity() {

    companion object {
        const val EXTRA_DIR: String = "de.syss.MifareClassicTool.Activity.FileChooser.DIR"
        const val EXTRA_TITLE: String = "de.syss.MifareClassicTool.Activity.FileChooser.TITLE"
        const val EXTRA_CHOOSER_TEXT: String = "de.syss.MifareClassicTool.Activity.FileChooser.CHOOSER_TEXT"
        const val EXTRA_BUTTON_TEXT: String = "de.syss.MifareClassicTool.Activity.FileChooser.BUTTON_TEXT"
        const val EXTRA_ALLOW_NEW_FILE: String = "de.syss.MifareClassicTool.Activity.FileChooser.ALLOW_NEW_FILE"
        const val EXTRA_CHOSEN_FILE: String = "de.syss.MifareClassicTool.Activity.CHOSEN_FILE"
        const val EXTRA_CHOSEN_FILENAME: String = "de.syss.MifareClassicTool.Activity.EXTRA_CHOSEN_FILENAME"

        private val LOG_TAG: String = FileChooser::class.java.simpleName
    }

    private var mDir: File? = null
    private var mIsAllowNewFile: Boolean = false
    private val mUiState = mutableStateOf(
        FileChooserUiState(
            title = "",
            chooserText = "",
            buttonText = "",
            allowNewFile = false
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        val currentIntent = intent

        // Check path and initialize file list.
        if (currentIntent.hasExtra(EXTRA_DIR)) {
            val pathStr = currentIntent.getStringExtra(EXTRA_DIR)
            val path = if (pathStr != null) File(pathStr) else null
            if (path != null && path.exists()) {
                if (!path.isDirectory) {
                    setResult(4)
                    finish()
                    return
                }
                mDir = path
            } else {
                Log.e(LOG_TAG, "Directory for FileChooser does not exist.")
                setResult(1)
                finish()
                return
            }
        } else {
            Log.d(LOG_TAG, "Directory for FileChooser was not in intent.")
            setResult(2)
            finish()
            return
        }

        if (currentIntent.hasExtra(EXTRA_ALLOW_NEW_FILE)) {
            mIsAllowNewFile = currentIntent.getBooleanExtra(EXTRA_ALLOW_NEW_FILE, false)
        }

        refreshFiles()

        val prefill = if (mDir?.name == Common.KEYS_DIR) ".keys" else ""

        setContent {
            MctTheme {
                FileChooserScreen(
                    state = mUiState.value,
                    onSelectFile = { fileName ->
                        mUiState.value = mUiState.value.copy(selectedFileName = fileName)
                    },
                    onChooseClick = { chooseSelectedFile() },
                    onCancel = { finish() },
                    onCreateNewFile = { newName -> createNewFile(newName) },
                    onDeleteSelectedFile = { deleteSelectedFile() },
                    onImportExportClick = { onImportExportFiles() },
                    prefillExtension = prefill
                )
            }
        }
    }

    private fun refreshFiles() {
        val path = mDir ?: return
        val currentIntent = intent

        val screenTitle = if (currentIntent.hasExtra(EXTRA_TITLE)) {
            currentIntent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.title_activity_file_chooser)
        } else {
            getString(R.string.title_activity_file_chooser)
        }

        val buttonText = if (currentIntent.hasExtra(EXTRA_BUTTON_TEXT)) {
            currentIntent.getStringExtra(EXTRA_BUTTON_TEXT) ?: getString(R.string.action_choose_file)
        } else {
            getString(R.string.action_choose_file)
        }

        val files = path.listFiles()
        val itemList = mutableListOf<FileChooserItem>()
        var isEmpty = true

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        if (files != null && files.isNotEmpty()) {
            Arrays.sort(files)
            for (f in files) {
                if (f.isFile) {
                    val sizeText = if (f.length() < 1024) "${f.length()} B" else "${f.length() / 1024} KB"
                    val dateText = dateFormat.format(Date(f.lastModified()))
                    itemList.add(
                        FileChooserItem(
                            file = f,
                            name = f.name,
                            sizeText = sizeText,
                            dateText = dateText
                        )
                    )
                }
            }
            isEmpty = itemList.isEmpty()
        }

        var chooserText = ""
        if ((!Common.isFirstInstall() && isEmpty) ||
            (!Common.isFirstInstall() && files != null && files.size == 3 &&
                    files[0].name == Common.STD_KEYS_EXTENDED &&
                    files[2].name == Common.STD_KEYS &&
                    files[1].name == Common.STD_HOTEL_KEYS)
        ) {
            chooserText += getString(R.string.text_missing_files_update) + "\n\n"
        }
        if (currentIntent.hasExtra(EXTRA_CHOOSER_TEXT)) {
            chooserText += currentIntent.getStringExtra(EXTRA_CHOOSER_TEXT)
        } else {
            chooserText += getString(R.string.text_chooser_info_text)
        }

        val currentSelected = mUiState.value.selectedFileName
        val newSelected = if (itemList.any { it.name == currentSelected }) {
            currentSelected
        } else {
            itemList.firstOrNull()?.name
        }

        mUiState.value = FileChooserUiState(
            title = screenTitle,
            chooserText = chooserText,
            buttonText = buttonText,
            allowNewFile = mIsAllowNewFile,
            files = itemList,
            selectedFileName = newSelected
        )
    }

    private fun chooseSelectedFile() {
        val selectedName = mUiState.value.selectedFileName ?: return
        val currentDir = mDir ?: return
        val file = File(currentDir.path, selectedName)
        val returnIntent = Intent().apply {
            putExtra(EXTRA_CHOSEN_FILE, file.path)
            putExtra(EXTRA_CHOSEN_FILENAME, file.name)
        }
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

    private fun createNewFile(fileName: String) {
        val currentDir = mDir ?: return
        if (fileName.contains("/")) {
            Toast.makeText(this, R.string.info_invalid_file_name, Toast.LENGTH_LONG).show()
            return
        }
        val file = File(currentDir.path, fileName)
        if (file.exists()) {
            Toast.makeText(this, R.string.info_file_already_exists, Toast.LENGTH_LONG).show()
            return
        }
        val returnIntent = Intent().apply {
            putExtra(EXTRA_CHOSEN_FILE, file.path)
            putExtra(EXTRA_CHOSEN_FILENAME, file.name)
        }
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

    private fun deleteSelectedFile() {
        val selectedName = mUiState.value.selectedFileName ?: return
        val currentDir = mDir ?: return
        val file = File(currentDir.path, selectedName)
        file.delete()
        refreshFiles()
    }

    private fun onImportExportFiles() {
        val returnIntent = Intent(this, ImportExportTool::class.java)
        startActivity(returnIntent)
    }

    // Legacy XML reflection stub
    @Suppress("UNUSED_PARAMETER")
    fun onFileChosen(view: View) = chooseSelectedFile()
}
