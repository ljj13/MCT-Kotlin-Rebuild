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

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.compose.setContent
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.MCReader
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.importexport.DumpFormatChoice
import de.syss.MifareClassicTool.ui.importexport.ImportExportScreen
import de.syss.MifareClassicTool.ui.importexport.KeysFormatChoice
import de.syss.MifareClassicTool.ui.theme.MctTheme
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Arrays
import java.util.GregorianCalendar
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * A simple tool to import and export dump files in and from different file formats.
 * @author Gerhard Klostermeier
 */
class ImportExportTool : BasicActivity() {

    companion object {
        /**
         * Boolean value to tell whether the file to export is a key file or a dump file.
         */
        const val EXTRA_IS_DUMP_FILE: String =
            "de.syss.MifareClassicTool.Activity.ImportExportTool.IS_DUMP_FILE"

        /**
         * Path to the file which should be exported.
         */
        const val EXTRA_FILE_PATH: String =
            "de.syss.MifareClassicTool.Activity.ImportExportTool.FILE_PATH"

        private const val IMPORT_FILE_CHOSEN = 1
        private const val EXPORT_FILE_CHOSEN = 2
        private const val EXPORT_LOCATION_CHOSEN = 3
        private const val BACKUP_LOCATION_CHOSEN = 4
    }

    private var mIsCalledWithExportFile = false
    private var mIsExport = false
    private var mIsDumpFile = false
    private var mFile: String? = null
    private var mConvertedContent: Array<String>? = null
    private var mFileType: FileType? = null

    private enum class FileType(private val text: String) {
        MCT(".mct"),
        KEYS(".keys"),
        JSON(".json"),
        BIN(".bin"),
        EML(".eml");

        override fun toString(): String {
            return text
        }
    }

    /**
     * Initialize the activity layout and state if there is one.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        if (savedInstanceState != null) {
            mIsDumpFile = savedInstanceState.getBoolean("is_dump_file")
            mIsExport = savedInstanceState.getBoolean("is_export")
            mIsCalledWithExportFile = savedInstanceState.getBoolean("is_called_with_export_file")
            mFile = savedInstanceState.getString("file")
            mConvertedContent = savedInstanceState.getStringArray("converted_content")
        }

        setContent {
            MctTheme {
                ImportExportScreen(
                    onBackClick = { finish() },
                    onImportDumpFormatSelected = { format ->
                        mFileType = when (format) {
                            DumpFormatChoice.MCT -> FileType.MCT
                            DumpFormatChoice.JSON -> FileType.JSON
                            DumpFormatChoice.BIN -> FileType.BIN
                            DumpFormatChoice.EML -> FileType.EML
                        }
                        mIsExport = false
                        mIsDumpFile = true
                        showImportFileChooser()
                    },
                    onExportDumpClick = { onExportDump(null) },
                    onImportKeysFormatSelected = { format ->
                        mFileType = when (format) {
                            KeysFormatChoice.KEYS -> FileType.KEYS
                            KeysFormatChoice.BIN -> FileType.BIN
                        }
                        mIsExport = false
                        mIsDumpFile = false
                        showImportFileChooser()
                    },
                    onExportKeysClick = { onExportKeys(null) },
                    onBackupAllClick = { onBackupAll(null) }
                )
            }
        }
    }

    /**
     * Check if there was a file appended to the intent calling this activity.
     */
    override fun onResume() {
        super.onResume()

        val currentIntent = intent
        if (!mIsCalledWithExportFile && currentIntent != null && currentIntent.hasExtra(EXTRA_FILE_PATH)) {
            val pathStr = currentIntent.getStringExtra(EXTRA_FILE_PATH)
            val path = if (pathStr != null) File(pathStr) else null
            if (path != null && path.exists() && !path.isDirectory) {
                mIsCalledWithExportFile = true
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    mIsExport = true
                    if (intent.hasExtra(EXTRA_IS_DUMP_FILE)) {
                        mIsDumpFile = intent.getBooleanExtra(EXTRA_IS_DUMP_FILE, false)
                    }
                    val intent1 = Intent()
                    intent1.putExtra(FileChooser.EXTRA_CHOSEN_FILE, path.absolutePath)
                    @Suppress("DEPRECATION")
                    onActivityResult(EXPORT_FILE_CHOSEN, RESULT_OK, intent1)
                }, 300)
            }
        }
    }

    /**
     * Save important state data before this activity gets destroyed.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("is_dump_file", mIsDumpFile)
        outState.putBoolean("is_export", mIsExport)
        outState.putBoolean("is_called_with_export_file", mIsCalledWithExportFile)
        outState.putString("file", mFile)
        outState.putStringArray("converted_content", mConvertedContent)
    }

    /**
     * Create the context menu with the supported dump/keys file types.
     */
    override fun onCreateContextMenu(
        menu: ContextMenu, v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater = menuInflater
        if (v.id == R.id.buttonImportExportToolImportDump) {
            inflater.inflate(R.menu.dump_file_types, menu)
        } else if (v.id == R.id.buttonImportExportToolImportKeys) {
            inflater.inflate(R.menu.keys_file_types, menu)
        }
    }

    /**
     * Saves the selected file type in [mFileType] and continues.
     */
    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuDumpFileTypesMct -> mFileType = FileType.MCT
            R.id.menuDumpFileTypesJson -> mFileType = FileType.JSON
            R.id.menuDumpFileTypesBinMfd -> mFileType = FileType.BIN
            R.id.menuDumpFileTypesEml -> mFileType = FileType.EML
            R.id.menuKeysFileTypesKeys -> mFileType = FileType.KEYS
            R.id.menuKeysFileTypesBin -> mFileType = FileType.BIN
            else -> return super.onContextItemSelected(item)
        }

        if (mIsExport) {
            val fileToExport = mFile
            if (fileToExport != null) {
                readAndConvertExportData(fileToExport)
            }
        } else {
            showImportFileChooser()
        }
        return true
    }

    /**
     * Get the file chooser result and continue.
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            IMPORT_FILE_CHOSEN -> {
                if (resultCode == RESULT_OK && data != null) {
                    val clipData = data.clipData
                    val uris: Array<Uri?> = if (clipData != null) {
                        Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                    } else {
                        arrayOf(data.data)
                    }
                    val nonNullUris = uris.filterNotNull().toTypedArray()
                    readConvertAndSaveImportData(nonNullUris)
                }
            }
            EXPORT_FILE_CHOSEN -> {
                if (resultCode == RESULT_OK && data != null) {
                    mFile = data.getStringExtra(FileChooser.EXTRA_CHOSEN_FILE)
                    if (mIsDumpFile) {
                        showDumpFileTypeChooserMenu()
                    } else {
                        showKeysFileTypeChooserMenu()
                    }
                }
            }
            EXPORT_LOCATION_CHOSEN -> {
                if (resultCode == RESULT_OK && data != null) {
                    val uri = data.data
                    if (uri != null) {
                        saveConvertedDataToContent(mConvertedContent, uri)
                    }
                }
            }
            BACKUP_LOCATION_CHOSEN -> {
                if (resultCode == RESULT_OK && data != null) {
                    val uri = data.data
                    if (uri != null) {
                        backupDumpsAndKeys(uri)
                    }
                }
            }
        }
    }

    /**
     * Start the dump import process.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onImportDump(view: View? = null) {
        mIsExport = false
        mIsDumpFile = true
        showDumpFileTypeChooserMenu()
    }

    /**
     * Start the dump export process.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onExportDump(view: View? = null) {
        mIsExport = true
        mIsDumpFile = true
        val intent = Intent(this, FileChooser::class.java)
        intent.putExtra(FileChooser.EXTRA_DIR, Common.getFile(Common.DUMPS_DIR).absolutePath)
        intent.putExtra(FileChooser.EXTRA_TITLE, getString(R.string.text_choose_dump_file))
        intent.putExtra(FileChooser.EXTRA_BUTTON_TEXT, getString(R.string.action_export_dump))
        @Suppress("DEPRECATION")
        startActivityForResult(intent, EXPORT_FILE_CHOSEN)
    }

    /**
     * Start the keys import process.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onImportKeys(view: View? = null) {
        mIsExport = false
        mIsDumpFile = false
        showKeysFileTypeChooserMenu()
    }

    /**
     * Start the keys export process.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onExportKeys(view: View? = null) {
        mIsExport = true
        mIsDumpFile = false
        val intent = Intent(this, FileChooser::class.java)
        intent.putExtra(FileChooser.EXTRA_DIR, Common.getFile(Common.KEYS_DIR).absolutePath)
        intent.putExtra(FileChooser.EXTRA_TITLE, getString(R.string.text_choose_key_file))
        intent.putExtra(FileChooser.EXTRA_BUTTON_TEXT, getString(R.string.action_export_keys))
        @Suppress("DEPRECATION")
        startActivityForResult(intent, EXPORT_FILE_CHOSEN)
    }

    /**
     * Create a full backup of all dump and key files.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onBackupAll(view: View? = null) {
        val calendar = GregorianCalendar()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        fmt.calendar = calendar
        val dateFormatted = fmt.format(calendar.time)
        showExportFileChooser("MCT-Backup_$dateFormatted.zip", BACKUP_LOCATION_CHOSEN)
    }

    /**
     * Import the file(s) by reading, converting and saving them.
     */
    private fun readConvertAndSaveImportData(files: Array<Uri>) {
        var content: Array<String>?
        val fileType = mFileType ?: return
        for (file in files) {
            try {
                if (fileType != FileType.BIN) {
                    content = Common.readUriLineByLine(file, !mIsDumpFile, this)
                } else {
                    val bytes = Common.readUriRaw(file, this)
                    if (bytes != null) {
                        val sb = StringBuilder()
                        for (b in bytes) {
                            sb.append(b.toInt().toChar())
                        }
                        content = arrayOf(sb.toString())
                    } else {
                        content = null
                    }
                }
                if (content == null) {
                    Toast.makeText(this, R.string.info_error_reading_file, Toast.LENGTH_LONG).show()
                    continue
                }

                var fileName = Common.getFileName(file, this) ?: "imported"
                if (fileName.contains(".")) {
                    fileName = fileName.substring(0, fileName.lastIndexOf('.'))
                }
                var destFileName = fileName
                val destPath: String

                val convertedContent: Array<String>?
                if (mIsDumpFile) {
                    convertedContent = convertDump(content, fileType, FileType.MCT)
                    destFileName += FileType.MCT.toString()
                    destPath = Common.DUMPS_DIR
                } else {
                    convertedContent = convertKeys(content, fileType, FileType.KEYS)
                    destFileName += FileType.KEYS.toString()
                    destPath = Common.KEYS_DIR
                }
                if (convertedContent == null) {
                    continue
                }

                val destination = Common.getFile("$destPath/$destFileName")
                if (Common.saveFile(destination, convertedContent, false)) {
                    Toast.makeText(this, R.string.info_file_imported, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.info_save_error, Toast.LENGTH_LONG).show()
                    continue
                }
            } catch (e: OutOfMemoryError) {
                Toast.makeText(this, R.string.info_file_too_big, Toast.LENGTH_LONG).show()
                continue
            }
        }
    }

    /**
     * Export the file by reading, converting and showing destination chooser.
     */
    private fun readAndConvertExportData(path: String) {
        val fileType = mFileType ?: return
        val source = File(path)
        val includeComments = !mIsDumpFile && fileType == FileType.KEYS
        val content = Common.readFileLineByLine(source, includeComments, this) ?: return

        var fileName = source.name
        if (fileName.contains(".")) {
            fileName = fileName.substring(0, fileName.lastIndexOf('.'))
        }
        val destFileName = fileName + fileType.toString()

        val convertedContent = if (mIsDumpFile) {
            convertDump(content, FileType.MCT, fileType)
        } else {
            convertKeys(content, FileType.KEYS, fileType)
        }
        if (convertedContent == null) {
            return
        }

        mConvertedContent = convertedContent
        showExportFileChooser(destFileName, EXPORT_LOCATION_CHOSEN)
    }

    /**
     * Save the converted content to a content URI.
     */
    private fun saveConvertedDataToContent(convertedContent: Array<String>?, contentDestination: Uri) {
        if (convertedContent == null) {
            Toast.makeText(this, R.string.info_convert_error, Toast.LENGTH_LONG).show()
            return
        }
        val fileType = mFileType ?: return
        val success: Boolean = if (fileType != FileType.BIN) {
            Common.saveFile(contentDestination, convertedContent, this)
        } else {
            val bytes = ByteArray(convertedContent[0].length)
            for (i in 0 until convertedContent[0].length) {
                bytes[i] = convertedContent[0][i].code.toByte()
            }
            Common.saveFile(contentDestination, bytes, this)
        }
        if (success) {
            Toast.makeText(this, R.string.info_file_exported, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, R.string.info_save_error, Toast.LENGTH_LONG).show()
        }

        if (mIsCalledWithExportFile) {
            finish()
        }
    }

    /**
     * Convert dump source from srcType to destType.
     */
    @SuppressLint("DefaultLocale")
    private fun convertDump(source: Array<String>?, srcType: FileType?, destType: FileType?): Array<String>? {
        if (source == null || srcType == null || destType == null) {
            return null
        }
        var json = ArrayList<String>()
        var block: String? = null
        if (srcType != FileType.JSON) {
            json.add("{")
            json.add("  \"Created\": \"MifareClassicTool\",")
            json.add("  \"FileType\": \"mfcard\",")
            json.add("  \"blocks\": {")
        }
        when (srcType) {
            FileType.JSON -> json = ArrayList(Arrays.asList(*source))
            FileType.MCT -> {
                val err = Common.isValidDump(source, true)
                if (err != 0) {
                    Common.isValidDumpErrorToast(err, this)
                    return null
                }
                var blockNumber = 0
                for (line in source) {
                    if (line.startsWith("+")) {
                        val sectorNumber = line.split(": ")[1].toInt()
                        blockNumber = if (sectorNumber < 32) {
                            sectorNumber * 4
                        } else {
                            32 * 4 + (sectorNumber - 32) * 16
                        }
                        continue
                    }
                    block = "    \"$blockNumber\": \"$line\","
                    json.add(block)
                    blockNumber += 1
                }
            }
            FileType.BIN -> {
                val binary = source[0]
                if (binary.length != 320 && binary.length != 1024 &&
                    binary.length != 2048 && binary.length != 4096
                ) {
                    Toast.makeText(this, R.string.info_incomplete_dump, Toast.LENGTH_LONG).show()
                    return null
                }
                var i = 0
                while (i < binary.length) {
                    val blockBytes = ByteArray(16)
                    for (j in 0 until 16) {
                        blockBytes[j] = binary[i + j].code.toByte()
                    }
                    block = "    \"" + (i / 16) + "\": \"" + Common.bytes2Hex(blockBytes) + "\","
                    json.add(block)
                    i += 16
                }
            }
            FileType.EML -> {
                if (source.size != 20 && source.size != 64 &&
                    source.size != 128 && source.size != 256
                ) {
                    Toast.makeText(this, R.string.info_incomplete_dump, Toast.LENGTH_LONG).show()
                    return null
                }
                for (i in source.indices) {
                    if (source[i].isEmpty()) {
                        Toast.makeText(this, R.string.info_incomplete_dump, Toast.LENGTH_LONG).show()
                        return null
                    }
                    block = "    \"$i\": \"" + source[i] + "\","
                    json.add(block)
                }
            }
            else -> {}
        }
        if (srcType != FileType.JSON) {
            if (block != null) {
                block = block.replace(",", "")
                json.removeAt(json.size - 1)
                json.add(block)
            }
            json.add("  }")
            json.add("}")
        }

        if (json.size <= 6) {
            Toast.makeText(this, R.string.info_convert_error, Toast.LENGTH_LONG).show()
            return null
        }

        val blocks: JSONObject
        try {
            val parsedJson = JSONObject(TextUtils.join("", json))
            blocks = parsedJson.getJSONObject("blocks")
            if (blocks.length() < 1) {
                throw JSONException("No blocks in source file")
            }
        } catch (e: JSONException) {
            Toast.makeText(this, R.string.info_convert_error, Toast.LENGTH_LONG).show()
            return null
        }

        var dest: Array<String>? = null
        when (destType) {
            FileType.JSON -> dest = json.toTypedArray()
            FileType.MCT -> {
                val export = ArrayList<String>()
                var lastKnownSector = -1
                for (blockNr in 0 until 256) {
                    val blockKey = blockNr.toString()
                    if (!blocks.has(blockKey)) {
                        continue
                    }
                    val sector = MCReader.blockToSector(blockNr)
                    if (lastKnownSector != sector) {
                        lastKnownSector = sector
                        export.add("+Sector: $sector")
                    }
                    try {
                        block = blocks.getString(blockKey)
                        export.add(block)
                    } catch (ex: JSONException) {
                        continue
                    }
                }
                dest = export.toTypedArray()
            }
            FileType.BIN -> {
                if (blocks.length() != 20 && blocks.length() != 64 &&
                    blocks.length() != 128 && blocks.length() != 256
                ) {
                    Toast.makeText(this, R.string.info_incomplete_dump, Toast.LENGTH_LONG).show()
                    return null
                }
                val sb = StringBuilder()
                for (i in 0 until blocks.length()) {
                    try {
                        block = blocks.getString(String.format("%d", i))
                    } catch (e: JSONException) {
                        Toast.makeText(this, R.string.info_incomplete_dump, Toast.LENGTH_LONG).show()
                        return null
                    }
                    val bytes = Common.hex2Bytes(block)
                    if (bytes == null) {
                        Toast.makeText(this, R.string.info_convert_error, Toast.LENGTH_LONG).show()
                        return null
                    }
                    for (b in bytes) {
                        sb.append(b.toInt().toChar())
                    }
                }
                dest = arrayOf(sb.toString())
            }
            FileType.EML -> {
                if (blocks.length() != 20 && blocks.length() != 64 &&
                    blocks.length() != 128 && blocks.length() != 256
                ) {
                    Toast.makeText(this, R.string.info_incomplete_dump, Toast.LENGTH_LONG).show()
                    return null
                }
                val emlArray = arrayOfNulls<String>(blocks.length())
                for (i in 0 until blocks.length()) {
                    try {
                        emlArray[i] = blocks.getString(String.format("%d", i))
                    } catch (e: JSONException) {
                        Toast.makeText(this, R.string.info_incomplete_dump, Toast.LENGTH_LONG).show()
                        return null
                    }
                }
                @Suppress("UNCHECKED_CAST")
                dest = emlArray as Array<String>
            }
            else -> {}
        }

        return dest
    }

    /**
     * Convert keys source from srcType to destType.
     */
    @SuppressLint("DefaultLocale")
    private fun convertKeys(source: Array<String>?, srcType: FileType?, destType: FileType?): Array<String>? {
        if (source == null || srcType == null || destType == null) {
            return null
        }
        var keys: Array<String>? = null
        when (srcType) {
            FileType.KEYS -> {
                val err = Common.isValidKeyFile(source)
                if (err != 0) {
                    Common.isValidKeyFileErrorToast(err, this)
                    return null
                }
                keys = source
            }
            FileType.BIN -> {
                val binary = source[0]
                val len = binary.length
                if (len > 0 && len % 6 != 0) {
                    Toast.makeText(this, R.string.info_invalid_key_file, Toast.LENGTH_LONG).show()
                    return null
                }
                val keyList = arrayOfNulls<String>(binary.length / 6)
                var i = 0
                while (i < len) {
                    val keyBytes = ByteArray(6)
                    for (j in 0 until 6) {
                        keyBytes[j] = binary[i + j].code.toByte()
                    }
                    keyList[i / 6] = Common.bytes2Hex(keyBytes)
                    i += 6
                }
                @Suppress("UNCHECKED_CAST")
                keys = keyList as Array<String>
            }
            else -> {}
        }

        if (keys == null) {
            Toast.makeText(this, R.string.info_convert_error, Toast.LENGTH_LONG).show()
            return null
        }

        var dest: Array<String>? = null
        when (destType) {
            FileType.KEYS -> dest = keys
            FileType.BIN -> {
                val sb = StringBuilder()
                for (key in keys) {
                    val bytes = Common.hex2Bytes(key)
                    if (bytes == null) {
                        Toast.makeText(this, R.string.info_convert_error, Toast.LENGTH_LONG).show()
                        return null
                    }
                    for (b in bytes) {
                        sb.append(b.toInt().toChar())
                    }
                }
                dest = arrayOf(sb.toString())
            }
            else -> {}
        }
        return dest
    }

    /**
     * Show save-as dialog to let user choose destination for exported files.
     */
    private fun showExportFileChooser(fileName: String, context: Int) {
        val exportIntent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        exportIntent.addCategory(Intent.CATEGORY_OPENABLE)
        exportIntent.type = "*/*"
        exportIntent.putExtra(Intent.EXTRA_TITLE, fileName)
        @Suppress("DEPRECATION")
        startActivityForResult(exportIntent, context)
    }

    private fun showDumpFileTypeChooserMenu() {
        val formats = arrayOf(
            getString(R.string.action_file_type_mct),
            getString(R.string.action_file_type_json),
            getString(R.string.action_file_type_mdf_bin),
            getString(R.string.action_file_type_eml)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.text_select_file)
            .setItems(formats) { _, which ->
                when (which) {
                    0 -> mFileType = FileType.MCT
                    1 -> mFileType = FileType.JSON
                    2 -> mFileType = FileType.BIN
                    3 -> mFileType = FileType.EML
                }
                if (mIsExport) {
                    val fileToExport = mFile
                    if (fileToExport != null) {
                        readAndConvertExportData(fileToExport)
                    }
                } else {
                    showImportFileChooser()
                }
            }
            .show()
    }

    private fun showKeysFileTypeChooserMenu() {
        val formats = arrayOf(
            getString(R.string.action_file_type_keys),
            getString(R.string.action_file_type_bin)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.text_select_file)
            .setItems(formats) { _, which ->
                when (which) {
                    0 -> mFileType = FileType.KEYS
                    1 -> mFileType = FileType.BIN
                }
                if (mIsExport) {
                    val fileToExport = mFile
                    if (fileToExport != null) {
                        readAndConvertExportData(fileToExport)
                    }
                } else {
                    showImportFileChooser()
                }
            }
            .show()
    }

    private fun showImportFileChooser() {
        val importIntent = Intent()
        importIntent.type = "*/*"
        importIntent.action = Intent.ACTION_GET_CONTENT
        importIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        val title = getString(R.string.text_select_file)
        @Suppress("DEPRECATION")
        startActivityForResult(Intent.createChooser(importIntent, title), IMPORT_FILE_CHOSEN)
    }

    /**
     * Create a ZIP file containing all keys and dumps and save it to the content URI.
     */
    private fun backupDumpsAndKeys(contentDestUri: Uri): Boolean {
        val bufferSize = 2048
        val dirs = arrayOf(
            Common.getFile(Common.KEYS_DIR),
            Common.getFile(Common.DUMPS_DIR)
        )
        val commonPathLen = Common.getFile("").absolutePath.lastIndexOf("/")
        try {
            val dest: OutputStream = contentResolver.openOutputStream(contentDestUri, "rw") ?: return false
            val out = ZipOutputStream(BufferedOutputStream(dest))
            for (dir in dirs) {
                val fileList = dir.listFiles()
                if (fileList == null || fileList.isEmpty()) {
                    continue
                }
                for (file in fileList) {
                    val data = ByteArray(bufferSize)
                    val fi = FileInputStream(file)
                    val origin = BufferedInputStream(fi, bufferSize)
                    val entry = ZipEntry(file.absolutePath.substring(commonPathLen))
                    entry.time = file.lastModified()
                    out.putNextEntry(entry)
                    var count: Int
                    while (origin.read(data, 0, bufferSize).also { count = it } != -1) {
                        out.write(data, 0, count)
                    }
                    origin.close()
                    fi.close()
                }
            }
            out.close()
        } catch (ex: Exception) {
            Toast.makeText(this, R.string.info_backup_error, Toast.LENGTH_LONG).show()
            return false
        }
        Toast.makeText(this, R.string.info_backup_created, Toast.LENGTH_LONG).show()
        return true
    }
}
