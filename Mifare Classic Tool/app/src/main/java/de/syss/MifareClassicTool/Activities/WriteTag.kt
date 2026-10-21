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
import android.nfc.tech.MifareClassic
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.SparseArray
import android.view.Gravity
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.core.widget.TextViewCompat
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.MCReader
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.theme.MctTheme
import de.syss.MifareClassicTool.ui.writetag.WriteTagShell
import java.io.File
import java.io.Serializable
import java.nio.ByteBuffer
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashMap
import java.util.HashSet

/**
 * Write data to tag. The user can choose to write a single block of data,
 * a dump, or factory format a tag.
 * @author Gerhard Klostermeier
 */
class WriteTag : BasicActivity() {

    companion object {
        const val EXTRA_DUMP: String = "de.syss.MifareClassicTool.Activity.DUMP"

        private const val FC_WRITE_DUMP: Int = 1
        private const val CKM_WRITE_DUMP: Int = 2
        private const val CKM_WRITE_BLOCK: Int = 3
        private const val CKM_FACTORY_FORMAT: Int = 4
        private const val CKM_WRITE_NEW_VALUE: Int = 5
        private const val CKM_VALUE_TRANSFER_RESTORE: Int = 6
    }

    private lateinit var mSectorTextBlock: EditText
    private lateinit var mBlockTextBlock: EditText
    private lateinit var mDataText: EditText
    private lateinit var mSectorTextVB: EditText
    private lateinit var mBlockTextVB: EditText
    private lateinit var mNewValueTextVB: EditText
    private lateinit var mIncreaseVB: RadioButton
    private lateinit var mStaticAC: EditText
    private lateinit var mWriteModeLayouts: ArrayList<View>
    private lateinit var mWriteManufBlock: CheckBox
    private lateinit var mEnableStaticAC: CheckBox
    private var mDumpWithPos: HashMap<Int, HashMap<Int, ByteArray>>? = null
    private var mKeysFromDump: HashSet<String>? = null
    private var mWriteDumpFromEditor: Boolean = false
    private var mDumpFromEditor: Array<String>? = null
    private lateinit var mVtrStageSector: EditText
    private lateinit var mVtrStageBlock: EditText
    private lateinit var mVtrDestSector: EditText
    private lateinit var mVtrDestBlock: EditText
    private lateinit var mVtrValue: EditText
    private lateinit var mVtrAddr: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write_tag)
        supportActionBar?.hide()

        mSectorTextBlock = findViewById(R.id.editTextWriteTagSector)
        mBlockTextBlock = findViewById(R.id.editTextWriteTagBlock)
        mDataText = findViewById(R.id.editTextWriteTagData)
        mSectorTextVB = findViewById(R.id.editTextWriteTagValueBlockSector)
        mBlockTextVB = findViewById(R.id.editTextWriteTagValueBlockBlock)
        mNewValueTextVB = findViewById(R.id.editTextWriteTagValueBlockValue)
        mIncreaseVB = findViewById(R.id.radioButtonWriteTagWriteValueBlockIncr)
        mStaticAC = findViewById(R.id.editTextWriteTagDumpStaticAC)
        mEnableStaticAC = findViewById(R.id.checkBoxWriteTagDumpStaticAC)
        mWriteManufBlock = findViewById(R.id.checkBoxWriteTagDumpWriteManuf)

        mWriteModeLayouts = ArrayList()
        mWriteModeLayouts.add(findViewById(R.id.relativeLayoutWriteTagWriteBlock))
        mWriteModeLayouts.add(findViewById(R.id.linearLayoutWriteTagDump))
        mWriteModeLayouts.add(findViewById(R.id.linearLayoutWriteTagCloneUid))
        mWriteModeLayouts.add(findViewById(R.id.linearLayoutWriteTagFactoryFormat))
        mWriteModeLayouts.add(findViewById(R.id.relativeLayoutWriteTagValueBlock))

        mVtrStageSector = findViewById(R.id.editTextWriteTagTransferRestoreStagingSector)
        mVtrStageBlock = findViewById(R.id.editTextWriteTagTransferRestoreStagingBlock)
        mVtrDestSector = findViewById(R.id.editTextWriteTagTransferRestoreDestinationSector)
        mVtrDestBlock = findViewById(R.id.editTextWriteTagTransferRestoreDestinationBlock)
        mVtrValue = findViewById(R.id.editTextWriteTagTransferRestoreValue)
        mVtrAddr = findViewById(R.id.editTextWriteTagTransferRestoreAddress)

        mWriteModeLayouts.add(findViewById(R.id.relativeLayoutWriteTagTransferRestore))

        if (savedInstanceState != null) {
            mWriteManufBlock.isChecked = savedInstanceState.getBoolean("write_manuf_block", false)
            val s: Serializable? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getSerializable("dump_with_pos", HashMap::class.java)
            } else {
                @Suppress("DEPRECATION")
                savedInstanceState.getSerializable("dump_with_pos")
            }
            if (s is HashMap<*, *>) {
                @Suppress("UNCHECKED_CAST")
                mDumpWithPos = s as HashMap<Int, HashMap<Int, ByteArray>>
            }
        }

        val i = intent
        if (i != null && i.hasExtra(EXTRA_DUMP)) {
            mDumpFromEditor = i.getStringArrayExtra(EXTRA_DUMP)
            mWriteDumpFromEditor = true
            val writeBlock = findViewById<RadioButton>(R.id.radioButtonWriteTagWriteBlock)
            val factoryFormat = findViewById<RadioButton>(R.id.radioButtonWriteTagFactoryFormat)
            val writeDump = findViewById<RadioButton>(R.id.radioButtonWriteTagWriteDump)
            writeDump.performClick()
            writeBlock.isEnabled = false
            factoryFormat.isEnabled = false
            val writeDumpButton = findViewById<Button>(R.id.buttonWriteTagDump)
            writeDumpButton.setText(R.string.action_write_dump)
        }

        findViewById<ComposeView>(R.id.composeViewWriteTagShell).setContent {
            MctTheme {
                WriteTagShell(onBackClick = { finish() })
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("write_manuf_block", mWriteManufBlock.isChecked)
        outState.putSerializable("dump_with_pos", mDumpWithPos)
    }

    fun onChangeWriteMode(view: View) {
        for (layout in mWriteModeLayouts) {
            layout.visibility = View.GONE
        }
        val parent = findViewById<View>(R.id.linearLayoutWriteTag)
        parent.findViewWithTag<View>(view.tag.toString() + "_layout").visibility = View.VISIBLE
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var ckmError = -1

        when (requestCode) {
            FC_WRITE_DUMP -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val filePath = data.getStringExtra(FileChooser.EXTRA_CHOSEN_FILE)
                    if (filePath != null) {
                        readDumpFromFile(filePath)
                    }
                }
            }
            CKM_WRITE_DUMP -> {
                if (resultCode != Activity.RESULT_OK) {
                    ckmError = resultCode
                } else {
                    checkDumpAgainstTag()
                }
            }
            CKM_FACTORY_FORMAT -> {
                if (resultCode != Activity.RESULT_OK) {
                    ckmError = resultCode
                } else {
                    createFactoryFormattedDump()
                }
            }
            CKM_WRITE_BLOCK -> {
                if (resultCode != Activity.RESULT_OK) {
                    ckmError = resultCode
                } else {
                    writeBlock()
                }
            }
            CKM_WRITE_NEW_VALUE -> {
                if (resultCode != Activity.RESULT_OK) {
                    ckmError = resultCode
                } else {
                    writeValueBlock()
                }
            }
            CKM_VALUE_TRANSFER_RESTORE -> {
                if (resultCode != Activity.RESULT_OK) {
                    ckmError = resultCode
                } else {
                    runValueTransferRestore()
                }
            }
        }

        if (ckmError == 4) {
            Toast.makeText(this, R.string.info_strange_error, Toast.LENGTH_LONG).show()
        }
    }

    fun onWriteBlock(view: View) {
        if (!checkSectorAndBlock(mSectorTextBlock, mBlockTextBlock)) {
            return
        }
        val data = mDataText.text.toString()
        if (!Common.isHexAnd16Byte(data, this)) {
            return
        }

        val sector = mSectorTextBlock.text.toString().toInt()
        val block = mBlockTextBlock.text.toString().toInt()

        if (!isSectorInRange(this, true)) {
            return
        }

        if (block == 3 || block == 15) {
            val acCheck = checkAccessConditions(data, true)
            if (acCheck == 1) {
                return
            }
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_sector_trailer_warning_title)
                .setMessage(R.string.dialog_sector_trailer_warning)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.action_i_know_what_i_am_doing) { _, _ ->
                    createKeyMapForBlock(sector, false)
                }
                .setNegativeButton(R.string.action_cancel) { _, _ -> }
                .show()
        } else if (sector == 0 && block == 0) {
            val block0Check = checkBlock0(data, true)
            if (block0Check == 1 || block0Check == 2) {
                return
            }
            showWriteManufInfo(true)
        } else {
            createKeyMapForBlock(sector, false)
        }
    }

    private fun checkSectorAndBlock(sector: EditText, block: EditText): Boolean {
        if (sector.text.toString().isEmpty() || block.text.toString().isEmpty()) {
            Toast.makeText(this, R.string.info_data_location_not_set, Toast.LENGTH_LONG).show()
            return false
        }
        val sectorNr = sector.text.toString().toInt()
        val blockNr = block.text.toString().toInt()
        if (sectorNr > KeyMapCreator.MAX_SECTOR_COUNT - 1 || sectorNr < 0) {
            Toast.makeText(this, R.string.info_sector_out_of_range, Toast.LENGTH_LONG).show()
            return false
        }
        if (blockNr > KeyMapCreator.MAX_BLOCK_COUNT_PER_SECTOR - 1 || blockNr < 0) {
            Toast.makeText(this, R.string.info_block_out_of_range, Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    @Suppress("UNUSED_PARAMETER")
    fun onShowOptions(view: View) {
        val ll = findViewById<LinearLayout>(R.id.linearLayoutWriteTagDumpOptions)
        val cb = findViewById<CheckBox>(R.id.checkBoxWriteTagDumpOptions)
        if (cb.isChecked) {
            ll.visibility = View.VISIBLE
        } else {
            ll.visibility = View.GONE
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onShowWriteManufInfo(view: View) {
        showWriteManufInfo(false)
    }

    private fun showWriteManufInfo(createKeyMap: Boolean) {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle(R.string.dialog_block0_writing_title)
        dialog.setMessage(R.string.dialog_block0_writing)
        dialog.setIcon(android.R.drawable.ic_dialog_info)

        var buttonID = R.string.action_ok
        if (createKeyMap) {
            buttonID = R.string.action_i_know_what_i_am_doing
            dialog.setNegativeButton(R.string.action_cancel) { _, _ -> }
        }
        dialog.setPositiveButton(buttonID) { _, _ ->
            if (createKeyMap) {
                createKeyMapForBlock(0, false)
            }
        }
        dialog.show()
    }

    private fun checkBlock0(block0: String, showToasts: Boolean): Int {
        val reader = Common.checkForTagAndCreateReader(this) ?: return 1
        reader.close()
        val currentUid = Common.getUID()
        val uidLen = currentUid?.size ?: 0

        if (uidLen == 4) {
            val bccBytes = Common.hex2Bytes(block0.substring(8, 10))
            val uidBytes = Common.hex2Bytes(block0.substring(0, 8))
            if (bccBytes == null || uidBytes == null || !Common.isValidBcc(uidBytes, bccBytes[0])) {
                if (showToasts) {
                    Toast.makeText(this, R.string.info_bcc_not_valid, Toast.LENGTH_LONG).show()
                }
                return 2
            }
        }

        val isValidBlock0 = Common.isValidBlock0(block0, uidLen, reader.size, true)
        if (!isValidBlock0) {
            if (showToasts) {
                Toast.makeText(this, R.string.text_block0_warning, Toast.LENGTH_LONG).show()
            }
            return 3
        }

        return 0
    }

    private fun checkAccessConditions(sectorTrailer: String, showToasts: Boolean): Int {
        val acBytes = Common.hex2Bytes(sectorTrailer.substring(12, 18))
        val acMatrix = Common.acBytesToACMatrix(acBytes)
        if (acMatrix == null) {
            if (showToasts) {
                Toast.makeText(this, R.string.info_ac_format_error, Toast.LENGTH_LONG).show()
            }
            return 1
        }
        val keyBReadable = Common.isKeyBReadable(acMatrix[0][3], acMatrix[1][3], acMatrix[2][3])
        val writeAC = Common.getOperationRequirements(
            acMatrix[0][3], acMatrix[1][3], acMatrix[2][3],
            Common.Operation.WriteAC, true, keyBReadable
        )
        if (writeAC == 0) {
            if (showToasts) {
                Toast.makeText(this, R.string.info_irreversible_acs, Toast.LENGTH_LONG).show()
            }
            return 2
        }
        return 0
    }

    @Suppress("UNUSED_PARAMETER")
    fun onShowStaticACInfo(view: View) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_static_ac_title)
            .setMessage(R.string.dialog_static_ac)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton(R.string.action_ok) { _, _ -> }
            .show()
    }

    private fun createKeyMapForBlock(sector: Int, isValueBlock: Boolean) {
        val intent = Intent(this, KeyMapCreator::class.java)
        intent.putExtra(KeyMapCreator.EXTRA_KEYS_DIR, Common.getFile(Common.KEYS_DIR).absolutePath)
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER, false)
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_FROM, sector)
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_TO, sector)
        if (isValueBlock) {
            intent.putExtra(
                KeyMapCreator.EXTRA_BUTTON_TEXT,
                getString(R.string.action_create_key_map_and_write_value_block)
            )
            @Suppress("DEPRECATION")
            startActivityForResult(intent, CKM_WRITE_NEW_VALUE)
        } else {
            intent.putExtra(
                KeyMapCreator.EXTRA_BUTTON_TEXT,
                getString(R.string.action_create_key_map_and_write_block)
            )
            @Suppress("DEPRECATION")
            startActivityForResult(intent, CKM_WRITE_BLOCK)
        }
    }

    private fun createKeyMapForRange(
        fromSector: Int, toSector: Int, requestCode: Int,
        buttonText: String
    ) {
        val intent = Intent(this, KeyMapCreator::class.java)
        intent.putExtra(KeyMapCreator.EXTRA_KEYS_DIR, Common.getFile(Common.KEYS_DIR).absolutePath)
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER, false)
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_FROM, Math.min(fromSector, toSector))
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_TO, Math.max(fromSector, toSector))
        intent.putExtra(KeyMapCreator.EXTRA_BUTTON_TEXT, buttonText)
        @Suppress("DEPRECATION")
        startActivityForResult(intent, requestCode)
    }

    private fun writeBlock() {
        val reader = Common.checkForTagAndCreateReader(this) ?: return
        val sector = mSectorTextBlock.text.toString().toInt()
        val block = mBlockTextBlock.text.toString().toInt()
        val data = mDataText.text.toString()
        val keys = Common.getKeyMap()?.get(sector) ?: run {
            reader.close()
            return
        }
        var result = -1
        var resultKeyA = -1
        var resultKeyB = -1

        val blockData = Common.hex2Bytes(data) ?: run {
            reader.close()
            return
        }
        if (keys[1] != null) {
            resultKeyB = reader.writeBlock(sector, block, blockData, keys[1]!!, true)
        }
        if (keys[0] != null) {
            resultKeyA = reader.writeBlock(sector, block, blockData, keys[0]!!, false)
        }

        reader.close()
        if (resultKeyA == 0 || resultKeyB == 0) {
            result = 0
        } else if (resultKeyA == 2 || resultKeyB == 2) {
            result = 2
        } else if (resultKeyA == -1 || resultKeyB == -1) {
            result = -1
        }

        when (result) {
            2 -> {
                Toast.makeText(this, R.string.info_block_not_in_sector, Toast.LENGTH_LONG).show()
                return
            }
            -1 -> {
                Toast.makeText(this, R.string.info_error_writing_block, Toast.LENGTH_LONG).show()
                return
            }
        }
        Toast.makeText(this, R.string.info_write_successful, Toast.LENGTH_LONG).show()
        finish()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onWriteDump(view: View) {
        if (mEnableStaticAC.isChecked) {
            val ac = mStaticAC.text.toString()
            if (!ac.matches(Regex("[0-9A-Fa-f]+"))) {
                Toast.makeText(this, R.string.info_ac_not_hex, Toast.LENGTH_LONG).show()
                return
            }
            if (ac.length != 6) {
                Toast.makeText(this, R.string.info_ac_not_3_byte, Toast.LENGTH_LONG).show()
                return
            }
        }

        if (mWriteDumpFromEditor) {
            val dumpFromEditor = mDumpFromEditor
            if (dumpFromEditor != null) {
                checkDumpAndShowSectorChooserDialog(dumpFromEditor)
            }
        } else {
            val intent = Intent(this, FileChooser::class.java)
            intent.putExtra(FileChooser.EXTRA_DIR, Common.getFile(Common.DUMPS_DIR).absolutePath)
            intent.putExtra(FileChooser.EXTRA_TITLE, getString(R.string.text_open_dump_title))
            intent.putExtra(FileChooser.EXTRA_CHOOSER_TEXT, getString(R.string.text_choose_dump_to_write))
            intent.putExtra(FileChooser.EXTRA_BUTTON_TEXT, getString(R.string.action_write_full_dump))
            @Suppress("DEPRECATION")
            startActivityForResult(intent, FC_WRITE_DUMP)
        }
    }

    private fun readDumpFromFile(pathToDump: String) {
        val file = File(pathToDump)
        val dump = Common.readFileLineByLine(file, false, this)
        if (dump != null) {
            checkDumpAndShowSectorChooserDialog(dump)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun checkDumpAndShowSectorChooserDialog(dump: Array<String>) {
        val err = Common.isValidDump(dump, false)
        if (err != 0) {
            Common.isValidDumpErrorToast(err, this)
            return
        }

        initDumpWithPosAndKeysFromDump(dump)

        val dialogLayout = layoutInflater.inflate(
            R.layout.dialog_write_sectors,
            findViewById(android.R.id.content), false
        )
        val llCheckBoxes = dialogLayout.findViewById<LinearLayout>(
            R.id.linearLayoutWriteSectorsCheckBoxes
        )
        val selectAll = dialogLayout.findViewById<Button>(
            R.id.buttonWriteSectorsSelectAll
        )
        val selectNone = dialogLayout.findViewById<Button>(
            R.id.buttonWriteSectorsSelectNone
        )
        val dumpWithPos = mDumpWithPos ?: return
        val sectors = dumpWithPos.keys.toTypedArray()
        Arrays.sort(sectors)
        val context = this
        val sectorBoxes = arrayOfNulls<CheckBox>(dumpWithPos.size)
        for (i in sectors.indices) {
            val cb = CheckBox(this)
            cb.isChecked = true
            cb.tag = sectors[i]
            cb.text = getString(R.string.text_sector) + " " + sectors[i]
            sectorBoxes[i] = cb
            llCheckBoxes.addView(cb)
        }
        val listener = OnClickListener { v ->
            val tag = v.tag.toString()
            for (box in sectorBoxes) {
                box?.isChecked = (tag == "all")
            }
        }
        selectAll.setOnClickListener(listener)
        selectNone.setOnClickListener(listener)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_write_sectors_title)
            .setIcon(android.R.drawable.ic_menu_edit)
            .setView(dialogLayout)
            .setPositiveButton(R.string.action_ok) { _, _ -> }
            .setNegativeButton(R.string.action_cancel) { _, _ -> }
            .create()
        dialog.show()
        val con = this

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            initDumpWithPosAndKeysFromDump(dump)
            val currentDumpWithPos = mDumpWithPos
            if (currentDumpWithPos != null) {
                for (box in sectorBoxes) {
                    if (box != null) {
                        val sector = box.tag.toString().toInt()
                        if (!box.isChecked) {
                            currentDumpWithPos.remove(sector)
                        }
                    }
                }
                if (currentDumpWithPos.isEmpty()) {
                    Toast.makeText(context, R.string.info_nothing_to_write, Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }

            if (!isSectorInRange(con, false)) {
                return@setOnClickListener
            }

            createKeyMapForDump()
            dialog.dismiss()
        }
    }

    private fun isSectorInRange(context: Context, isWriteBlock: Boolean): Boolean {
        val reader = Common.checkForTagAndCreateReader(this) ?: return false
        val lastValidSector = reader.sectorCount - 1
        val lastSector: Int
        reader.close()
        if (isWriteBlock) {
            lastSector = mSectorTextBlock.text.toString().toInt()
        } else {
            val currentDump = mDumpWithPos ?: return false
            lastSector = Collections.max(currentDump.keys)
        }

        if (lastSector > lastValidSector) {
            Toast.makeText(context, R.string.info_tag_too_small, Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun initDumpWithPosAndKeysFromDump(dump: Array<String>) {
        mDumpWithPos = HashMap()
        mKeysFromDump = HashSet()
        var sector = 0
        var block = 0
        val targetDump = dump.clone()

        for (i in targetDump.indices) {
            val line = targetDump[i]
            if (line.startsWith("+")) {
                val tmp = line.split(": ")
                sector = tmp[tmp.size - 1].toInt()
                block = 0
                mDumpWithPos?.put(sector, HashMap())
            } else {
                if (i + 1 == targetDump.size || targetDump[i + 1].startsWith("+")) {
                    val keyA = line.substring(0, 12)
                    val keyB = line.substring(20)
                    if (!keyA.contains("-")) {
                        mKeysFromDump?.add(keyA)
                    }
                    if (!keyB.contains("-")) {
                        mKeysFromDump?.add(keyB)
                    }
                }
                if (!line.contains("-")) {
                    var blockData = line
                    if (mEnableStaticAC.isChecked && (i + 1 == targetDump.size || targetDump[i + 1].startsWith("+"))) {
                        blockData = line.substring(0, 12) + mStaticAC.text.toString() + line.substring(18)
                    }
                    val blockBytes = Common.hex2Bytes(blockData)
                    if (blockBytes != null) {
                        mDumpWithPos?.get(sector)?.put(block++, blockBytes)
                    } else {
                        block++
                    }
                } else {
                    block++
                }
            }
        }
    }

    private fun createKeyMapForDump() {
        val currentDump = mDumpWithPos ?: return
        val intent = Intent(this, KeyMapCreator::class.java)
        intent.putExtra(KeyMapCreator.EXTRA_KEYS_DIR, Common.getFile(Common.KEYS_DIR).absolutePath)
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER, false)
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_FROM, Collections.min(currentDump.keys))
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_TO, Collections.max(currentDump.keys))
        intent.putExtra(
            KeyMapCreator.EXTRA_BUTTON_TEXT,
            getString(R.string.action_create_key_map_and_write_dump)
        )
        saveKeysFromDumpToTempKeyFile()
        intent.putExtra(KeyMapCreator.EXTRA_USE_KEYS_FROM_DUMP, true)
        @Suppress("DEPRECATION")
        startActivityForResult(intent, CKM_WRITE_DUMP)
    }

    private fun saveKeysFromDumpToTempKeyFile() {
        val keysFromDump = mKeysFromDump
        if (keysFromDump == null || keysFromDump.isEmpty()) {
            return
        }
        val keys = keysFromDump.toTypedArray()
        val file = Common.getFile(Common.TMP_DIR + "/keys_from_dump.keys")
        if (!Common.saveFile(file, keys, false)) {
            Toast.makeText(this, R.string.info_save_error, Toast.LENGTH_LONG).show()
        }
    }

    private fun checkDumpAgainstTag() {
        val reader = Common.checkForTagAndCreateReader(this)
        if (reader == null) {
            Toast.makeText(this, R.string.info_tag_lost_check_dump, Toast.LENGTH_LONG).show()
            return
        }

        val currentDump = mDumpWithPos
        if (currentDump == null || reader.sectorCount - 1 < Collections.max(currentDump.keys)) {
            Toast.makeText(this, R.string.info_tag_too_small, Toast.LENGTH_LONG).show()
            reader.close()
            return
        }

        val keyMap = Common.getKeyMap()
        if (keyMap == null) {
            reader.close()
            return
        }
        val dataPos = HashMap<Int, IntArray>(currentDump.size)
        for (sector in currentDump.keys) {
            val sectorBlocks = currentDump[sector]?.keys ?: continue
            val blocks = IntArray(sectorBlocks.size)
            var i = 0
            for (b in sectorBlocks) {
                blocks[i++] = b
            }
            dataPos[sector] = blocks
        }
        val writeOnPos = reader.isWritableOnPositions(dataPos, keyMap)
        reader.close()

        if (writeOnPos == null) {
            Toast.makeText(this, R.string.info_tag_lost_check_dump, Toast.LENGTH_LONG).show()
            return
        }

        val list = ArrayList<HashMap<String, String>>()
        val writeOnPosSafe = HashMap<Int, HashMap<Int, Int>>()

        val sectors = HashSet<Int>()
        for (sector in currentDump.keys) {
            if (keyMap.indexOfKey(sector) < 0) {
                addToList(
                    list, getString(R.string.text_sector) + ": " + sector,
                    getString(R.string.text_keys_not_known)
                )
            } else {
                sectors.add(sector)
            }
        }

        for (sector in sectors) {
            val sectorWriteInfo = writeOnPos[sector]
            if (sectorWriteInfo == null) {
                addToList(
                    list, getString(R.string.text_sector) + ": " + sector,
                    getString(R.string.text_invalid_ac_or_sector_dead)
                )
                continue
            }
            val keys = keyMap.get(sector)
            val blocks = currentDump[sector]?.keys ?: continue
            for (block in blocks) {
                var isSafeForWriting = true
                val position = getString(R.string.text_sector) + ": " +
                        sector + ", " + getString(R.string.text_block) + ": " + block

                if (!mWriteManufBlock.isChecked && sector == 0 && block == 0) {
                    continue
                } else if (mWriteManufBlock.isChecked && sector == 0 && block == 0) {
                    val block0Bytes = currentDump[0]?.get(0)
                    if (block0Bytes != null) {
                        val block0 = Common.bytes2Hex(block0Bytes)
                        when (checkBlock0(block0, false)) {
                            1 -> {
                                Toast.makeText(this, R.string.info_tag_lost_check_dump, Toast.LENGTH_LONG).show()
                                return
                            }
                            2 -> {
                                Toast.makeText(this, R.string.info_bcc_not_valid, Toast.LENGTH_LONG).show()
                                return
                            }
                            3 -> addToList(list, position, getString(R.string.text_block0_warning))
                        }
                    }
                }

                if ((sector < 31 && block == 3) || (sector >= 31 && block == 15)) {
                    val trailerBytes = currentDump[sector]?.get(block)
                    if (trailerBytes != null) {
                        val sectorTrailer = Common.bytes2Hex(trailerBytes)
                        when (checkAccessConditions(sectorTrailer, false)) {
                            1 -> {
                                Toast.makeText(this, R.string.info_ac_format_error, Toast.LENGTH_LONG).show()
                                return
                            }
                            2 -> addToList(list, position, getString(R.string.info_irreversible_acs))
                        }
                    }
                }

                var writeInfo = sectorWriteInfo[block] ?: -1
                when (writeInfo) {
                    0 -> {
                        addToList(list, position, getString(R.string.text_block_read_only))
                        isSafeForWriting = false
                    }
                    1 -> {
                        if (keys[0] == null) {
                            addToList(list, position, getString(R.string.text_write_key_a_not_known))
                            isSafeForWriting = false
                        }
                    }
                    2 -> {
                        if (keys[1] == null) {
                            addToList(list, position, getString(R.string.text_write_key_b_not_known))
                            isSafeForWriting = false
                        }
                    }
                    3 -> {
                        writeInfo = if (keys[0] != null) 1 else 2
                    }
                    4 -> {
                        if (keys[0] == null) {
                            addToList(list, position, getString(R.string.text_write_key_a_not_known))
                            isSafeForWriting = false
                        } else {
                            addToList(list, position, getString(R.string.text_ac_read_only))
                        }
                    }
                    5 -> {
                        if (keys[1] == null) {
                            addToList(list, position, getString(R.string.text_write_key_b_not_known))
                            isSafeForWriting = false
                        } else {
                            addToList(list, position, getString(R.string.text_ac_read_only))
                        }
                    }
                    6 -> {
                        if (keys[1] == null) {
                            addToList(list, position, getString(R.string.text_write_key_b_not_known))
                            isSafeForWriting = false
                        } else {
                            addToList(list, position, getString(R.string.text_keys_read_only))
                        }
                    }
                    -1 -> {
                        addToList(list, position, getString(R.string.text_strange_error))
                        isSafeForWriting = false
                    }
                }
                if (isSafeForWriting) {
                    if (writeOnPosSafe[sector] == null) {
                        val blockInfo = HashMap<Int, Int>()
                        blockInfo[block] = writeInfo
                        writeOnPosSafe[sector] = blockInfo
                    } else {
                        writeOnPosSafe[sector]?.put(block, writeInfo)
                    }
                }
            }
        }

        if (list.isNotEmpty()) {
            val ll = LinearLayout(this)
            val pad = Common.dpToPx(5)
            ll.setPadding(pad, pad, pad, pad)
            ll.orientation = LinearLayout.VERTICAL
            val textView = TextView(this)
            textView.setText(R.string.dialog_write_issues)
            textView.setPadding(0, 0, 0, Common.dpToPx(5))
            TextViewCompat.setTextAppearance(textView, android.R.style.TextAppearance_Medium)
            val listView = ListView(this)
            ll.addView(textView)
            ll.addView(listView)
            val from = arrayOf("position", "reason")
            val to = intArrayOf(android.R.id.text1, android.R.id.text2)
            val adapter: ListAdapter = SimpleAdapter(this, list, android.R.layout.two_line_list_item, from, to)
            listView.adapter = adapter

            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_write_issues_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setView(ll)
                .setPositiveButton(R.string.action_skip_blocks) { _, _ ->
                    writeDump(writeOnPosSafe, keyMap)
                }
                .setNegativeButton(R.string.action_cancel_all) { _, _ -> }
                .show()
        } else {
            writeDump(writeOnPosSafe, keyMap)
        }
    }

    private fun addToList(
        list: MutableList<HashMap<String, String>>,
        position: String, reason: String
    ) {
        val item = HashMap<String, String>()
        item["position"] = position
        item["reason"] = reason
        list.add(item)
    }

    private fun writeDump(
        writeOnPos: HashMap<Int, HashMap<Int, Int>>,
        keyMap: SparseArray<Array<ByteArray?>>
    ) {
        if (writeOnPos.isEmpty()) {
            Toast.makeText(this, R.string.info_nothing_to_write, Toast.LENGTH_LONG).show()
            return
        }

        val reader = Common.checkForTagAndCreateReader(this) ?: return

        val ll = LinearLayout(this)
        var pad = Common.dpToPx(20)
        ll.setPadding(pad, pad, pad, pad)
        ll.gravity = Gravity.CENTER
        val progressBar = ProgressBar(this)
        progressBar.isIndeterminate = true
        pad = Common.dpToPx(20)
        progressBar.setPadding(0, 0, pad, 0)
        val tv = TextView(this)
        tv.text = getString(R.string.dialog_wait_write_tag)
        tv.textSize = 18f
        ll.addView(progressBar)
        ll.addView(tv)
        val warning = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_wait_write_tag_title)
            .setView(ll)
            .create()
        warning.show()

        val a: Activity = this
        val handler = Handler(Looper.getMainLooper())
        Thread {
            for (sector in writeOnPos.keys) {
                val keys = keyMap.get(sector)
                val blockMap = writeOnPos[sector] ?: continue
                for (block in blockMap.keys) {
                    var writeKey: ByteArray? = null
                    var useAsKeyB = true
                    val wi = blockMap[block]
                    if (wi == 1 || wi == 4) {
                        writeKey = keys[0]
                        useAsKeyB = false
                    } else if (wi == 2 || wi == 5 || wi == 6) {
                        writeKey = keys[1]
                    }

                    if (writeKey == null) {
                        continue
                    }

                    var result = 0
                    val dumpData = mDumpWithPos?.get(sector)?.get(block)
                    if (dumpData != null) {
                        for (i in 0 until 2) {
                            result = reader.writeBlock(sector, block, dumpData, writeKey, useAsKeyB)
                            if (result == 0) {
                                break
                            }
                        }
                    }

                    if (result != 0) {
                        handler.post {
                            Toast.makeText(a, R.string.info_write_error, Toast.LENGTH_LONG).show()
                        }
                        reader.close()
                        warning.cancel()
                        return@Thread
                    }
                }
            }
            reader.close()
            warning.cancel()
            handler.post {
                Toast.makeText(a, R.string.info_write_successful, Toast.LENGTH_LONG).show()
            }
            a.finish()
        }.start()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onWriteValueBlockTransferRestore(view: View) {
        if (!checkSectorAndBlock(mVtrStageSector, mVtrStageBlock)) return
        val stagingSector = mVtrStageSector.text.toString().toInt()
        val stagingBlock = mVtrStageBlock.text.toString().toInt()

        if (!checkSectorAndBlock(mVtrDestSector, mVtrDestBlock)) return
        val destinationSector = mVtrDestSector.text.toString().toInt()
        val destinationBlock = mVtrDestBlock.text.toString().toInt()

        if (stagingSector == destinationSector && stagingBlock == destinationBlock) {
            Toast.makeText(this, R.string.info_staging_equal_destination, Toast.LENGTH_LONG).show()
            return
        }

        if (stagingBlock == 3 || stagingBlock == 15 || (stagingSector == 0 && stagingBlock == 0)) {
            Toast.makeText(this, R.string.info_staging_sector_trailer_block0, Toast.LENGTH_LONG).show()
            return
        }
        if (destinationBlock == 3 || destinationBlock == 15 || (destinationSector == 0 && destinationBlock == 0)) {
            Toast.makeText(this, R.string.info_destination_sector_trailer_block0, Toast.LENGTH_LONG).show()
            return
        }

        if (mVtrValue.text.toString() == "") {
            Toast.makeText(this, R.string.info_no_int_to_encode, Toast.LENGTH_LONG).show()
            return
        }
        val value = mVtrValue.text.toString().toLong()
        if (value > Int.MAX_VALUE) {
            Toast.makeText(this, R.string.info_value_too_big, Toast.LENGTH_LONG).show()
            return
        }
        if (value < Int.MIN_VALUE) {
            Toast.makeText(this, R.string.info_value_too_small, Toast.LENGTH_LONG).show()
            return
        }

        val addrText = mVtrAddr.text.toString()
        if (!addrText.matches(Regex("[0-9A-Fa-f]{2}"))) {
            Toast.makeText(this, R.string.info_addr_not_hex_byte, Toast.LENGTH_SHORT).show()
            return
        }

        if (stagingSector < destinationSector) {
            createKeyMapForRange(
                stagingSector, destinationSector, CKM_VALUE_TRANSFER_RESTORE,
                getString(R.string.action_create_key_map_and_write_value_block)
            )
        } else {
            createKeyMapForRange(
                destinationSector, stagingSector, CKM_VALUE_TRANSFER_RESTORE,
                getString(R.string.action_create_key_map_and_write_value_block)
            )
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCloneUid(view: View) {
        val intent = Intent(this, CloneUidTool::class.java)
        startActivity(intent)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onFactoryFormat(view: View) {
        val intent = Intent(this, KeyMapCreator::class.java)
        intent.putExtra(KeyMapCreator.EXTRA_KEYS_DIR, Common.getFile(Common.KEYS_DIR).absolutePath)
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER, false)
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_FROM, -1)
        intent.putExtra(
            KeyMapCreator.EXTRA_BUTTON_TEXT,
            getString(R.string.action_create_key_map_and_factory_format)
        )
        @Suppress("DEPRECATION")
        startActivityForResult(intent, CKM_FACTORY_FORMAT)
    }

    private fun createFactoryFormattedDump() {
        mDumpWithPos = HashMap()
        val currentTag = Common.getTag()
        val sectors = if (currentTag != null) MifareClassic.get(currentTag).sectorCount else 16
        val emptyBlock = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val normalSectorTrailer = byteArrayOf(-1, -1, -1, -1, -1, -1, -1, 7, -128, 105, -1, -1, -1, -1, -1, -1)
        val lastSectorTrailer = byteArrayOf(-1, -1, -1, -1, -1, -1, -1, 7, -128, -68, -1, -1, -1, -1, -1, -1)

        val empty4BlockSector = HashMap<Int, ByteArray>(4)
        for (i in 0 until 3) {
            empty4BlockSector[i] = emptyBlock
        }
        empty4BlockSector[3] = normalSectorTrailer

        val empty16BlockSector = HashMap<Int, ByteArray>(16)
        for (i in 0 until 15) {
            empty16BlockSector[i] = emptyBlock
        }
        empty16BlockSector[15] = normalSectorTrailer

        val firstSector = HashMap<Int, ByteArray>(4)
        firstSector[1] = emptyBlock
        firstSector[2] = emptyBlock
        firstSector[3] = normalSectorTrailer
        mDumpWithPos?.put(0, firstSector)

        for (i in 1 until sectors) {
            if (i < 32) {
                mDumpWithPos?.put(i, empty4BlockSector)
            }
        }

        val lastSector: HashMap<Int, ByteArray>
        if (sectors == 40) {
            for (i in 32 until sectors) {
                if (i < 39) {
                    mDumpWithPos?.put(i, empty16BlockSector)
                }
            }
            lastSector = HashMap(empty16BlockSector)
            lastSector[15] = lastSectorTrailer
        } else {
            lastSector = HashMap(empty4BlockSector)
            lastSector[3] = lastSectorTrailer
        }
        mDumpWithPos?.put(sectors - 1, lastSector)
        checkDumpAgainstTag()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onWriteValue(view: View) {
        if (!checkSectorAndBlock(mSectorTextVB, mBlockTextVB)) {
            return
        }

        val sector = mSectorTextVB.text.toString().toInt()
        val block = mBlockTextVB.text.toString().toInt()
        if (block == 3 || block == 15 || (sector == 0 && block == 0)) {
            Toast.makeText(this, R.string.info_not_vb, Toast.LENGTH_LONG).show()
            return
        }

        try {
            mNewValueTextVB.text.toString().toInt()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.info_value_too_big, Toast.LENGTH_LONG).show()
            return
        }

        createKeyMapForBlock(sector, true)
    }

    private fun writeValueBlock() {
        val reader = Common.checkForTagAndCreateReader(this) ?: return
        val value = mNewValueTextVB.text.toString().toInt()
        val sector = mSectorTextVB.text.toString().toInt()
        val block = mBlockTextVB.text.toString().toInt()
        val keys = Common.getKeyMap()?.get(sector) ?: run {
            reader.close()
            return
        }
        var result = -1

        if (keys[1] != null) {
            result = reader.writeValueBlock(
                sector, block, value, mIncreaseVB.isChecked, keys[1]!!, true
            )
        }
        if (result == -1 && keys[0] != null) {
            result = reader.writeValueBlock(
                sector, block, value, mIncreaseVB.isChecked, keys[0]!!, false
            )
        }
        reader.close()

        when (result) {
            2 -> {
                Toast.makeText(this, R.string.info_block_not_in_sector, Toast.LENGTH_LONG).show()
                return
            }
            -1 -> {
                Toast.makeText(this, R.string.info_error_writing_value_block, Toast.LENGTH_LONG).show()
                return
            }
        }
        Toast.makeText(this, R.string.info_write_successful, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun encodeValueBlock(value: Int, addr: Int): ByteArray {
        val v = ByteBuffer.allocate(4).putInt(Integer.reverseBytes(value)).array()
        val nv = ByteBuffer.allocate(4).putInt(Integer.reverseBytes(value.inv())).array()
        val a = (addr and 0xFF).toByte()
        val na = a.toInt().inv().toByte()
        val out = ByteArray(16)
        System.arraycopy(v, 0, out, 0, 4)
        System.arraycopy(nv, 0, out, 4, 4)
        System.arraycopy(v, 0, out, 8, 4)
        out[12] = a
        out[13] = na
        out[14] = a
        out[15] = na
        return out
    }

    private fun runValueTransferRestore() {
        val stagingSector = mVtrStageSector.text.toString().toInt()
        val stagingBlock = mVtrStageBlock.text.toString().toInt()
        val destinationSector = mVtrDestSector.text.toString().toInt()
        val destinationBlock = mVtrDestBlock.text.toString().toInt()
        val value = mVtrValue.text.toString().toInt()
        val address = mVtrAddr.text.toString().toInt(16)
        val vb = encodeValueBlock(value, address)

        val keyMap = Common.getKeyMap() ?: return
        val stagingKeys = keyMap.get(stagingSector)
        val destinationKeys = keyMap.get(destinationSector)
        if (stagingKeys == null || (stagingKeys[0] == null && stagingKeys[1] == null)) {
            Toast.makeText(this, R.string.info_no_key_for_staging, Toast.LENGTH_LONG).show()
            return
        }
        if (destinationKeys == null || (destinationKeys[0] == null && destinationKeys[1] == null)) {
            Toast.makeText(this, R.string.info_no_key_for_destination, Toast.LENGTH_LONG).show()
            return
        }

        val reader = Common.checkForTagAndCreateReader(this) ?: return
        val pos = HashMap<Int, IntArray>()
        if (stagingSector == destinationSector) {
            pos[stagingSector] = intArrayOf(stagingBlock, destinationBlock)
        } else {
            pos[stagingSector] = intArrayOf(stagingBlock)
            pos[destinationSector] = intArrayOf(destinationBlock)
        }
        val writable = reader.isWritableOnPositions(pos, keyMap)
        if (writable == null || writable[stagingSector] == null || writable[stagingSector]?.get(stagingBlock) == null) {
            Toast.makeText(this, R.string.text_strange_error, Toast.LENGTH_LONG).show()
            reader.close()
            return
        }
        val stagingKey: ByteArray
        var useAsKeyB = false
        when (writable[stagingSector]?.get(stagingBlock)) {
            1 -> {
                if (stagingKeys[0] == null) {
                    Toast.makeText(this, R.string.info_no_write_key_for_staging, Toast.LENGTH_LONG).show()
                    reader.close()
                    return
                }
                stagingKey = stagingKeys[0]!!
            }
            2 -> {
                if (stagingKeys[1] == null) {
                    Toast.makeText(this, R.string.info_no_write_key_for_staging, Toast.LENGTH_LONG).show()
                    reader.close()
                    return
                }
                stagingKey = stagingKeys[1]!!
                useAsKeyB = true
            }
            3 -> {
                if (stagingKeys[1] != null) {
                    stagingKey = stagingKeys[1]!!
                    useAsKeyB = true
                } else {
                    stagingKey = stagingKeys[0]!!
                }
            }
            else -> {
                Toast.makeText(this, R.string.info_strange_error, Toast.LENGTH_LONG).show()
                reader.close()
                return
            }
        }

        val hasRestorePermission = reader.hasDecTransRestPermission(
            stagingSector, stagingBlock, stagingKey, useAsKeyB
        )
        if (hasRestorePermission == 0 || hasRestorePermission == -1) {
            Toast.makeText(this, R.string.info_no_restore_staging, Toast.LENGTH_LONG).show()
            reader.close()
            return
        }

        var result = -1
        for (i in 0 until 2) {
            val destKey = destinationKeys[i]
            if (destKey != null) {
                result = reader.valueTransferRestore(
                    stagingSector, stagingBlock, destinationSector,
                    destinationBlock, vb, stagingKey, useAsKeyB, destKey, (i == 1)
                )
                if (result == 0) {
                    break
                }
            }
        }
        reader.close()

        when (result) {
            2 -> {
                Toast.makeText(this, R.string.info_block_not_in_sector, Toast.LENGTH_LONG).show()
                return
            }
            -1 -> {
                Toast.makeText(this, R.string.info_error_writing_value_block, Toast.LENGTH_LONG).show()
                return
            }
        }
        Toast.makeText(this, R.string.info_write_successful, Toast.LENGTH_LONG).show()
        finish()
    }
}
