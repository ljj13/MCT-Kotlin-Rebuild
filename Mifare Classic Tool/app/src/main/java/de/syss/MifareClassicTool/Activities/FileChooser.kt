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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatRadioButton
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R
import java.io.File
import java.util.Arrays

/**
 * A simple generic file chooser that lets the user choose a file from
 * a given directory.
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

    private lateinit var mGroupOfFiles: RadioGroup
    private lateinit var mChooserButton: Button
    private lateinit var mChooserText: TextView
    private var mDeleteFile: MenuItem? = null
    private var mDir: File? = null
    private var mIsDirEmpty: Boolean = false
    private var mIsAllowNewFile: Boolean = false

    /**
     * Initialize class variables.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_chooser)
        mGroupOfFiles = findViewById(R.id.radioGroupFileChooser)
    }

    /**
     * Initialize the file chooser with the data from the calling Intent.
     */
    override fun onStart() {
        super.onStart()

        mChooserText = findViewById(R.id.textViewFileChooser)
        mChooserButton = findViewById(R.id.buttonFileChooserChoose)
        val currentIntent = intent

        // Set title.
        if (currentIntent.hasExtra(EXTRA_TITLE)) {
            title = currentIntent.getStringExtra(EXTRA_TITLE)
        }
        // Set chooser text.
        if (currentIntent.hasExtra(EXTRA_CHOOSER_TEXT)) {
            mChooserText.text = currentIntent.getStringExtra(EXTRA_CHOOSER_TEXT)
        }
        // Set button text.
        if (currentIntent.hasExtra(EXTRA_BUTTON_TEXT)) {
            mChooserButton.text = currentIntent.getStringExtra(EXTRA_BUTTON_TEXT)
        }
        // Check file creation.
        if (currentIntent.hasExtra(EXTRA_ALLOW_NEW_FILE)) {
            mIsAllowNewFile = currentIntent.getBooleanExtra(EXTRA_ALLOW_NEW_FILE, false)
        }

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
                mIsDirEmpty = updateFileIndex(path)
            } else {
                // Path does not exist.
                Log.e(LOG_TAG, "Directory for FileChooser does not exist.")
                setResult(1)
                finish()
            }
        } else {
            Log.d(LOG_TAG, "Directory for FileChooser was not in intent.")
            setResult(2)
            finish()
        }
    }

    /**
     * Add the menu to the Activity.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.file_chooser_functions, menu)
        mDeleteFile = menu.findItem(R.id.menuFileChooserDeleteFile)
        val newFile = menu.findItem(R.id.menuFileChooserNewFile)

        // Enable/disable the delete menu item if there is a least one file.
        mDeleteFile?.isEnabled = !mIsDirEmpty

        // Enable/disable the new file menu item according to mIsAllowNewFile.
        newFile?.isEnabled = mIsAllowNewFile
        newFile?.isVisible = mIsAllowNewFile

        return true
    }

    /**
     * Handle selected function form the menu.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuFileChooserNewFile -> {
                onNewFile()
                true
            }
            R.id.menuFileChooserDeleteFile -> {
                onDeleteFile()
                true
            }
            R.id.menuFileChooserImportExportFiles -> {
                onImportExportFiles()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Finish the Activity with an Intent containing [EXTRA_CHOSEN_FILE] and [EXTRA_CHOSEN_FILENAME].
     */
    @Suppress("UNUSED_PARAMETER")
    fun onFileChosen(view: View) {
        val selected = findViewById<AppCompatRadioButton>(mGroupOfFiles.checkedRadioButtonId) ?: return
        val currentDir = mDir ?: return
        val returnIntent = Intent()
        val file = File(currentDir.path, selected.text.toString())
        returnIntent.putExtra(EXTRA_CHOSEN_FILE, file.path)
        returnIntent.putExtra(EXTRA_CHOSEN_FILENAME, file.name)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

    /**
     * Update the file list and the components that depend on it.
     */
    @SuppressLint("SetTextI18n")
    private fun updateFileIndex(path: File?): Boolean {
        var isEmpty = true
        var files: Array<File>? = null
        var chooserText = ""

        if (path != null) {
            files = path.listFiles()
        }
        mGroupOfFiles.removeAllViews()

        // Refresh file list.
        if (files != null && files.isNotEmpty()) {
            Arrays.sort(files)
            for (f in files) {
                if (f.isFile) { // Do not list directories.
                    val r = AppCompatRadioButton(this)
                    r.text = f.name
                    mGroupOfFiles.addView(r)
                }
            }
            if (mGroupOfFiles.childCount > 0) {
                isEmpty = false
                (mGroupOfFiles.getChildAt(0) as AppCompatRadioButton).isChecked = true
            }
        } else {
            // No files in directory.
            isEmpty = true
        }

        // Update chooser text.
        if ((!Common.isFirstInstall() && isEmpty) ||
            (!Common.isFirstInstall() && files != null && files.size == 3 &&
                    files[0].name == Common.STD_KEYS_EXTENDED &&
                    files[2].name == Common.STD_KEYS &&
                    files[1].name == Common.STD_HOTEL_KEYS)
        ) {
            chooserText += getString(R.string.text_missing_files_update) + "\n\n"
        }
        val currentIntent = intent
        if (currentIntent != null && currentIntent.hasExtra(EXTRA_CHOOSER_TEXT)) {
            chooserText += currentIntent.getStringExtra(EXTRA_CHOOSER_TEXT)
        } else {
            chooserText += getString(R.string.text_chooser_info_text)
        }
        if (isEmpty) {
            chooserText += "\n\n   --- " + getString(R.string.text_no_files_in_chooser) + " ---"
        }
        mChooserText.text = chooserText

        mChooserButton.isEnabled = !isEmpty
        mDeleteFile?.isEnabled = !isEmpty

        return isEmpty
    }

    /**
     * Ask the user for a file name, create this file and choose it.
     */
    private fun onNewFile() {
        val cont: Context = this
        val currentDir = mDir ?: return
        var prefill = ""
        if (currentDir.name == Common.KEYS_DIR) {
            prefill = ".keys"
        }
        // Init layout.
        val dialogLayout = layoutInflater.inflate(
            R.layout.dialog_save_file,
            findViewById(android.R.id.content), false
        )
        val message = dialogLayout.findViewById<TextView>(R.id.textViewDialogSaveFileMessage)
        val input = dialogLayout.findViewById<EditText>(R.id.editTextDialogSaveFileName)
        message.setText(R.string.dialog_new_file)
        input.setText(prefill)
        input.requestFocus()
        input.setSelection(0)

        // Show keyboard.
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        input.postDelayed({
            input.requestFocus()
            imm.showSoftInput(input, 0)
        }, 100)

        // Ask user for filename.
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_new_file_title)
            .setIcon(android.R.drawable.ic_menu_add)
            .setView(dialogLayout)
            .setPositiveButton(R.string.action_ok) { _, _ ->
                val inputText = input.text?.toString()
                if (!inputText.isNullOrEmpty() && !inputText.contains("/")) {
                    val file = File(currentDir.path, inputText)
                    if (file.exists()) {
                        Toast.makeText(cont, R.string.info_file_already_exists, Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                    val returnIntent = Intent()
                    returnIntent.putExtra(EXTRA_CHOSEN_FILE, file.path)
                    setResult(Activity.RESULT_OK, returnIntent)
                    finish()
                } else {
                    // Invalid file name.
                    Toast.makeText(cont, R.string.info_invalid_file_name, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.action_cancel) { _, _ -> }
            .show()
    }

    /**
     * Delete the selected file and update the file list.
     */
    private fun onDeleteFile() {
        val selected = findViewById<AppCompatRadioButton>(mGroupOfFiles.checkedRadioButtonId) ?: return
        val currentDir = mDir ?: return
        val file = File(currentDir.path, selected.text.toString())
        file.delete()
        mIsDirEmpty = updateFileIndex(currentDir)
    }

    /**
     * Open the [ImportExportTool].
     */
    private fun onImportExportFiles() {
        val returnIntent = Intent(this, ImportExportTool::class.java)
        startActivity(returnIntent)
    }
}
