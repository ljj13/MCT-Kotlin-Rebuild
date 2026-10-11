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
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableString
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.UnderlineSpan
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import android.widget.TextView.BufferType
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.MCReader
import de.syss.MifareClassicTool.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.HashSet
import java.util.Locale

/**
 * A simple hex editor for showing and editing tag dumps.
 * @author Gerhard Klostermeier
 */
class DumpEditor : BasicActivity(), IActivityThatReactsToSave {

    companion object {
        /**
         * The corresponding Intent will contain a dump separated by new lines.
         */
        const val EXTRA_DUMP: String = "de.syss.MifareClassicTool.Activity.DUMP"

        private val LOG_TAG: String = DumpEditor::class.java.simpleName
    }

    private lateinit var mLayout: LinearLayout
    private var mDumpName: String? = null
    private var mKeysName: String? = null
    private var mUID: String? = null

    /**
     * All blocks containing valid data AND their headers.
     */
    private var mLines: Array<String>? = null

    /**
     * True if the user made changes to the dump.
     */
    private var mDumpChanged: Boolean = false

    /**
     * If true, the editor will close after a successful save.
     */
    private var mCloseAfterSuccessfulSave: Boolean = false

    /**
     * Initialize editor on a dump file or new dump.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dump_editor)

        mLayout = findViewById(R.id.linearLayoutDumpEditor)

        // Color caption.
        val keyA = Common.colorString(
            getString(R.string.text_keya),
            ContextCompat.getColor(this, R.color.light_green)
        )
        val keyB = Common.colorString(
            getString(R.string.text_keyb),
            ContextCompat.getColor(this, R.color.dark_green)
        )
        val ac = Common.colorString(
            getString(R.string.text_ac),
            ContextCompat.getColor(this, R.color.orange)
        )
        val uidAndManuf = Common.colorString(
            getString(R.string.text_uid_and_manuf),
            ContextCompat.getColor(this, R.color.purple)
        )
        val vb = Common.colorString(
            getString(R.string.text_valueblock),
            ContextCompat.getColor(this, R.color.yellow)
        )

        val caption = findViewById<TextView>(R.id.textViewDumpEditorCaption)
        caption.setText(
            TextUtils.concat(uidAndManuf, " | ", vb, " | ", keyA, " | ", keyB, " | ", ac),
            BufferType.SPANNABLE
        )
        val captionTitle = findViewById<TextView>(R.id.textViewDumpEditorCaptionTitle)
        val updateText = Common.colorString(
            getString(R.string.text_update_colors), Common.getThemeAccentColor(this)
        )
        updateText.setSpan(UnderlineSpan(), 0, updateText.length, 0)
        captionTitle.text = TextUtils.concat(
            getString(R.string.text_caption_title), ": (", updateText, ")"
        )

        val currentIntent = intent
        if (currentIntent != null && currentIntent.hasExtra(EXTRA_DUMP)) {
            val dump = currentIntent.getStringArrayExtra(EXTRA_DUMP)
            if (Common.getUID() != null) {
                mUID = Common.bytes2Hex(Common.getUID())
                title = "$title (UID: $mUID)"
            }
            if (dump != null) {
                initEditor(dump)
            }
            intent = null
        } else if (currentIntent != null && currentIntent.hasExtra(FileChooser.EXTRA_CHOSEN_FILE)) {
            val filePath = currentIntent.getStringExtra(FileChooser.EXTRA_CHOSEN_FILE)
            val file = if (filePath != null) File(filePath) else null
            if (file != null) {
                mDumpName = file.name
                title = "$title ($mDumpName)"
                val lines = Common.readFileLineByLine(file, false, this)
                if (lines != null) {
                    initEditor(lines)
                }
            }
            intent = null
        } else if (savedInstanceState != null) {
            mCloseAfterSuccessfulSave = savedInstanceState.getBoolean("close_after_successful_save")
            mDumpChanged = savedInstanceState.getBoolean("dump_changed")
            mKeysName = savedInstanceState.getString("keys_name")
            mUID = savedInstanceState.getString("uid")
            if (mUID != null) {
                title = "$title ($mUID)"
            }
            mDumpName = savedInstanceState.getString("dump_name")
            if (mDumpName != null) {
                title = "$title ($mDumpName)"
            }
            mLines = savedInstanceState.getStringArray("lines")
            if (mLines != null) {
                initEditor(mLines!!)
            }
        }
    }

    /**
     * Save important state data before activity gets destroyed.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("dump_changed", mDumpChanged)
        outState.putBoolean("close_after_successful_save", mCloseAfterSuccessfulSave)
        outState.putString("keys_name", mKeysName)
        outState.putString("dump_name", mDumpName)
        outState.putString("uid", mUID)
        outState.putStringArray("lines", mLines)
    }

    /**
     * Add a menu with editor functions to the Activity.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.dump_editor_functions, menu)
        menu.findItem(R.id.menuDumpEditorWriteDump).isEnabled = !Common.useAsEditorOnly()
        return true
    }

    /**
     * Handle the selected function from the editor menu.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuDumpEditorSave -> {
                saveDump()
                true
            }
            R.id.menuDumpEditorAscii -> {
                showAscii()
                true
            }
            R.id.menuDumpEditorAccessConditions -> {
                showAC()
                true
            }
            R.id.menuDumpEditorValueBlocksAsInt -> {
                decodeValueBlocks()
                true
            }
            R.id.menuDumpEditorShare -> {
                shareDump()
                true
            }
            R.id.menuDumpEditorOpenValueBlockTool -> {
                openValueBlockTool()
                true
            }
            R.id.menuDumpEditorOpenAccessConditionTool -> {
                openAccessConditionTool()
                true
            }
            R.id.menuDumpEditorOpenBccTool -> {
                openBccTool()
                true
            }
            R.id.menuDumpEditorDecodeDateOfManuf -> {
                decodeDateOfManuf()
                true
            }
            R.id.menuDumpEditorWriteDump -> {
                writeDump()
                true
            }
            R.id.menuDumpEditorDiffDump -> {
                diffDump()
                true
            }
            R.id.menuDumpEditorSaveKeys -> {
                saveKeys()
                true
            }
            R.id.menuDumpEditorExportDump -> {
                exportDump()
                true
            }
            R.id.menuDumpEditorCopyAllSectors -> {
                copyAllSectorsToClipboard()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Update coloring if all data are valid.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onUpdateColors(view: View?) {
        val err = checkDumpAndUpdateLines()
        if (err != 0) {
            Common.isValidDumpErrorToast(err, this)
            return
        }
        val focused = mLayout.focusedChild
        var focusIndex = -1
        if (focused != null) {
            focusIndex = mLayout.indexOfChild(focused)
        }
        initEditor(mLines!!)
        if (focusIndex != -1) {
            while (focusIndex >= 0 && mLayout.getChildAt(focusIndex) == null) {
                focusIndex--
            }
            if (focusIndex >= 0) {
                mLayout.getChildAt(focusIndex)?.requestFocus()
            }
        }
    }

    /**
     * Show save before quitting dialog if there are unsaved changes.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (mDumpChanged) {
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_save_before_quitting_title)
                .setMessage(R.string.dialog_save_before_quitting)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(R.string.action_save) { _, _ ->
                    mCloseAfterSuccessfulSave = true
                    saveDump()
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

    override fun onSaveSuccessful() {
        if (mCloseAfterSuccessfulSave) {
            finish()
        }
        mDumpChanged = false
    }

    override fun onSaveFailure() {
        mCloseAfterSuccessfulSave = false
    }

    /**
     * Check if valid dump, generate filename suggestion and save.
     */
    private fun saveDump() {
        val err = checkDumpAndUpdateLines()
        if (err != 0) {
            Common.isValidDumpErrorToast(err, this)
            return
        }

        if (mDumpName == null) {
            val calendar = GregorianCalendar()
            val fmt = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            fmt.calendar = calendar
            val dateFormatted = fmt.format(calendar.time)
            mDumpName = "UID_${mUID}_$dateFormatted.mct"
        }

        saveFile(
            mLines!!, mDumpName!!, true, R.string.dialog_save_dump_title,
            R.string.dialog_save_dump
        )
    }

    /**
     * Helper function to save file with dialog.
     */
    private fun saveFile(
        data: Array<String>, fileName: String,
        isDump: Boolean, titleId: Int, messageId: Int
    ) {
        val targetDir = if (isDump) Common.DUMPS_DIR else Common.KEYS_DIR
        val path = Common.getFile(targetDir)
        val context: Context = this
        val activity: IActivityThatReactsToSave = this

        val dialogLayout = layoutInflater.inflate(
            R.layout.dialog_save_file,
            findViewById(android.R.id.content), false
        )
        val message = dialogLayout.findViewById<TextView>(R.id.textViewDialogSaveFileMessage)
        val input = dialogLayout.findViewById<EditText>(R.id.editTextDialogSaveFileName)
        message.setText(messageId)
        input.setText(fileName)
        input.requestFocus()
        input.setSelection(0)

        AlertDialog.Builder(this)
            .setTitle(titleId)
            .setIcon(android.R.drawable.ic_menu_save)
            .setView(dialogLayout)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val inputText = input.text?.toString()
                if (!inputText.isNullOrEmpty() && !inputText.contains("/")) {
                    val file = File(path.path, inputText)
                    Common.checkFileExistenceAndSave(file, data, isDump, context, activity)
                    if (isDump) {
                        mDumpName = file.name
                    } else {
                        mKeysName = file.name
                    }
                } else {
                    Toast.makeText(context, R.string.info_invalid_file_name, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.action_cancel) { _, _ ->
                mCloseAfterSuccessfulSave = false
            }
            .show()
        onUpdateColors(null)
    }

    /**
     * Check if all sectors contain valid data.
     */
    private fun checkDumpAndUpdateLines(): Int {
        val checkedLines = ArrayList<String>()
        for (i in 0 until mLayout.childCount) {
            val child = mLayout.getChildAt(i)
            if (child is AppCompatEditText) {
                val lines = child.text.toString().split(
                    Regex(System.getProperty("line.separator") ?: "\n")
                )
                if (lines.size != 4 && lines.size != 16) {
                    return 1
                }
                for (j in lines.indices) {
                    if (!lines[j].matches(Regex("[0-9A-Fa-f-]+"))) {
                        return 2
                    }
                    if (lines[j].length != 32) {
                        return 3
                    }
                    checkedLines.add(lines[j].uppercase(Locale.getDefault()))
                }
            } else if (child is TextView) {
                val tag = child.tag as? String
                if (tag != null && tag == "real_header") {
                    checkedLines.add("+Sector: " + child.text.toString().split(": ")[1])
                }
            }
        }
        mLines = checkedLines.toTypedArray()
        return 0
    }

    /**
     * Initialize the editor with lines.
     */
    @SuppressLint("SetTextI18n")
    private fun initEditor(lines: Array<String>) {
        val err = Common.isValidDump(lines, true)
        if (err != 0) {
            Common.isValidDumpErrorToast(err, this)
            Toast.makeText(this, R.string.info_editor_init_error, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val tmpDumpChanged = mDumpChanged
        mLayout.removeAllViews()
        var isFirstBlock = false
        var et: AppCompatEditText? = null
        var blocks = ArrayList<SpannableString>(4)

        for (i in lines.indices) {
            val line = lines[i]
            if (line.startsWith("+")) {
                isFirstBlock = line.endsWith(" 0")
                val sectorNumber = line.split(": ")[1]
                val tv = TextView(this)
                tv.setTextColor(Common.getThemeAccentColor(this))
                tv.text = getString(R.string.text_sector) + ": " + sectorNumber
                mLayout.addView(tv)
                if (i + 1 != lines.size && !lines[i + 1].startsWith("*")) {
                    et = AppCompatEditText(this)
                    et.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                    et.filters = arrayOf(InputFilter.AllCaps())
                    et.inputType = (et.inputType
                            or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                            or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                            or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
                    et.typeface = Typeface.MONOSPACE
                    et.setTextSize(TypedValue.COMPLEX_UNIT_PX, TextView(this).textSize)
                    et.addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable?) {
                            mDumpChanged = true
                        }

                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    })
                    mLayout.addView(et)
                    tv.tag = "real_header"
                }
            } else if (line.startsWith("*")) {
                val tv = TextView(this)
                tv.setTextColor(ContextCompat.getColor(this, R.color.red))
                tv.text = "   " + getString(R.string.text_no_key_io_error)
                tv.tag = "error"
                mLayout.addView(tv)
            } else {
                if (i + 1 == lines.size || lines[i + 1].startsWith("+")) {
                    blocks.add(colorSectorTrailer(line))
                    var text: CharSequence = ""
                    for (j in 0 until blocks.size - 1) {
                        text = TextUtils.concat(text, blocks[j], "\n")
                    }
                    if (blocks.isNotEmpty()) {
                        text = TextUtils.concat(text, blocks[blocks.size - 1])
                    }
                    et?.setText(text, BufferType.SPANNABLE)
                    blocks = ArrayList(4)
                } else {
                    blocks.add(colorDataBlock(line, isFirstBlock))
                    isFirstBlock = false
                }
            }
        }
        mDumpChanged = tmpDumpChanged
    }

    /**
     * Display hex data as US-ASCII.
     */
    private fun showAscii() {
        val err = checkDumpAndUpdateLines()
        if (err != 0) {
            Common.isValidDumpErrorToast(err, this)
            return
        }
        val currentLines = mLines ?: return
        val tmpDump = ArrayList<String>()
        for (i in 0 until currentLines.size - 1) {
            if (i + 1 != currentLines.size && !currentLines[i + 1].startsWith("+")) {
                tmpDump.add(currentLines[i])
            }
        }
        val dump = tmpDump.toTypedArray()
        val intent = Intent(this, HexToAscii::class.java)
        intent.putExtra(EXTRA_DUMP, dump)
        startActivity(intent)
    }

    /**
     * Display Access Conditions.
     */
    private fun showAC() {
        val err = checkDumpAndUpdateLines()
        if (err != 0) {
            Common.isValidDumpErrorToast(err, this)
            return
        }
        val currentLines = mLines ?: return
        val tmpACs = ArrayList<String>()
        var lastSectorHeader = 0
        for (i in currentLines.indices) {
            if (currentLines[i].startsWith("+")) {
                tmpACs.add(currentLines[i])
                lastSectorHeader = i
            } else if (i + 1 == currentLines.size || currentLines[i + 1].startsWith("+")) {
                if (i - lastSectorHeader > 4) {
                    tmpACs.add("*" + currentLines[i].substring(12, 20))
                } else {
                    tmpACs.add(currentLines[i].substring(12, 20))
                }
            }
        }
        val ac = tmpACs.toTypedArray()
        val intent = Intent(this, AccessConditionDecoder::class.java)
        intent.putExtra(AccessConditionDecoder.EXTRA_AC, ac)
        startActivity(intent)
    }

    /**
     * Display value blocks as integers.
     */
    private fun decodeValueBlocks() {
        val err = checkDumpAndUpdateLines()
        if (err != 0) {
            Common.isValidDumpErrorToast(err, this)
            return
        }
        val currentLines = mLines ?: return
        val tmpVBs = ArrayList<String>()
        var header = ""
        var blockCounter = 0
        for (line in currentLines) {
            if (line.startsWith("+")) {
                header = line
                blockCounter = 0
            } else {
                if (Common.isValueBlock(line)) {
                    tmpVBs.add("$header, Block: $blockCounter")
                    tmpVBs.add(line)
                }
                blockCounter++
            }
        }

        if (tmpVBs.isNotEmpty()) {
            val vb = tmpVBs.toTypedArray()
            val intent = Intent(this, ValueBlocksToInt::class.java)
            intent.putExtra(ValueBlocksToInt.EXTRA_VB, vb)
            startActivity(intent)
        } else {
            Toast.makeText(this, R.string.info_no_vb_in_dump, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Decode date of manufacture from block 0.
     */
    private fun decodeDateOfManuf() {
        val err = checkDumpAndUpdateLines()
        if (err != 0) {
            Common.isValidDumpErrorToast(err, this)
            return
        }
        val currentLines = mLines ?: return
        if (currentLines[0] == "+Sector: 0" && !currentLines[1].contains("-")) {
            val sdf = SimpleDateFormat("yy", Locale.getDefault())
            var styledText: CharSequence
            try {
                val year = currentLines[1].substring(30, 32).toInt()
                val week = currentLines[1].substring(28, 30).toInt()
                val now = sdf.format(Date()).toInt()
                if (year in 0..now && week in 1..53) {
                    val calendar = Calendar.getInstance()
                    calendar.clear()
                    calendar.set(Calendar.WEEK_OF_YEAR, week)
                    calendar.set(Calendar.YEAR, year + 2000)
                    sdf.applyPattern("dd.MM.yyyy")
                    val startDate = sdf.format(calendar.time)
                    calendar.add(Calendar.DATE, 6)
                    val endDate = sdf.format(calendar.time)

                    styledText = HtmlCompat.fromHtml(
                        getString(R.string.dialog_date_of_manuf, startDate, endDate),
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                } else {
                    throw NumberFormatException()
                }
            } catch (ex: NumberFormatException) {
                styledText = getText(R.string.dialog_date_of_manuf_error)
            }
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_date_of_manuf_title)
                .setMessage(styledText)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(R.string.action_ok) { _, _ -> }
                .show()
        } else {
            Toast.makeText(this, R.string.info_block0_missing, Toast.LENGTH_LONG).show()
        }
    }

    private fun openValueBlockTool() {
        val intent = Intent(this, ValueBlockTool::class.java)
        startActivity(intent)
    }

    private fun openAccessConditionTool() {
        val intent = Intent(this, AccessConditionTool::class.java)
        startActivity(intent)
    }

    private fun openBccTool() {
        val intent = Intent(this, BccTool::class.java)
        startActivity(intent)
    }

    private fun writeDump() {
        val err = checkDumpAndUpdateLines()
        if (err != 0) {
            Common.isValidDumpErrorToast(err, this)
            return
        }
        val intent = Intent(this, WriteTag::class.java)
        intent.putExtra(WriteTag.EXTRA_DUMP, mLines)
        startActivity(intent)
    }

    private fun diffDump() {
        val err = checkDumpAndUpdateLines()
        if (err != 0) {
            Common.isValidDumpErrorToast(err, this)
            return
        }
        val intent = Intent(this, DiffTool::class.java)
        intent.putExtra(DiffTool.EXTRA_DUMP, mLines)
        startActivity(intent)
    }

    private fun shareDump() {
        val file = saveDumpToTemp()
        if (file == null || !file.exists() || file.isDirectory) {
            return
        }
        Common.shareTextFile(this, file)
    }

    private fun exportDump() {
        val file = saveDumpToTemp()
        if (file == null || !file.exists() || file.isDirectory) {
            return
        }
        val intent = Intent(this, ImportExportTool::class.java)
        intent.putExtra(ImportExportTool.EXTRA_FILE_PATH, file.absolutePath)
        intent.putExtra(ImportExportTool.EXTRA_IS_DUMP_FILE, true)
        startActivity(intent)
    }

    private fun copyAllSectorsToClipboard() {
        val err = checkDumpAndUpdateLines()
        if (err != 0) {
            Common.isValidDumpErrorToast(err, this)
            return
        }
        val currentLines = mLines ?: return
        val dumpString = StringBuilder()
        for (line in currentLines) {
            if (line.startsWith("+")) {
                continue
            }
            if (dumpString.isNotEmpty()) {
                dumpString.append(System.lineSeparator())
            }
            dumpString.append(line)
        }
        Common.copyToClipboard(dumpString.toString(), this, true)
    }

    private fun saveDumpToTemp(): File? {
        val err = checkDumpAndUpdateLines()
        if (err != 0) {
            Common.isValidDumpErrorToast(err, this)
            return null
        }
        val currentDumpName = mDumpName
        val fileName = if (currentDumpName == null) {
            val calendar = GregorianCalendar()
            val fmt = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            fmt.calendar = calendar
            fmt.format(calendar.time)
        } else {
            currentDumpName
        }
        val file = Common.getFile(Common.TMP_DIR + "/" + fileName)
        if (!Common.saveFile(file, mLines, false)) {
            Toast.makeText(this, R.string.info_save_error, Toast.LENGTH_LONG).show()
            return null
        }
        return file
    }

    private fun saveKeys() {
        val err = checkDumpAndUpdateLines()
        if (err != 0) {
            Common.isValidDumpErrorToast(err, this)
            return
        }
        val currentLines = mLines ?: return
        val tmpKeys = HashSet<String>()
        for (i in currentLines.indices) {
            if (i + 1 == currentLines.size || currentLines[i + 1].startsWith("+")) {
                val keyA = currentLines[i].substring(0, 12).uppercase(Locale.ROOT)
                val keyB = currentLines[i].substring(20).uppercase(Locale.ROOT)
                if (keyA != MCReader.NO_KEY) {
                    tmpKeys.add(keyA)
                }
                if (keyB != MCReader.NO_KEY) {
                    tmpKeys.add(keyB)
                }
            }
        }
        val keys = tmpKeys.toTypedArray()

        if (mKeysName == null) {
            mKeysName = if (mDumpName == null) {
                "UID_" + mUID
            } else {
                mDumpName
            }
        }
        mKeysName += ".keys"

        saveFile(
            keys, mKeysName!!, false, R.string.dialog_save_keys_title,
            R.string.dialog_save_keys
        )
    }

    private fun colorDataBlock(data: String, hasUID: Boolean): SpannableString {
        return if (hasUID) {
            SpannableString(
                TextUtils.concat(Common.colorString(data, ContextCompat.getColor(this, R.color.purple)))
            )
        } else {
            if (Common.isValueBlock(data)) {
                Common.colorString(data, ContextCompat.getColor(this, R.color.yellow))
            } else {
                SpannableString(data)
            }
        }
    }

    private fun colorSectorTrailer(data: String): SpannableString {
        val colorKeyA = ContextCompat.getColor(this, R.color.light_green)
        val colorKeyB = ContextCompat.getColor(this, R.color.dark_green)
        val colorAC = ContextCompat.getColor(this, R.color.orange)
        return try {
            val keyA = Common.colorString(data.substring(0, 12), colorKeyA)
            val keyB = Common.colorString(data.substring(20), colorKeyB)
            val ac = Common.colorString(data.substring(12, 18), colorAC)
            SpannableString(TextUtils.concat(keyA, ac, data.substring(18, 20), keyB))
        } catch (e: IndexOutOfBoundsException) {
            Log.d(LOG_TAG, "Error while coloring sector trailer")
            SpannableString(data)
        }
    }
}
