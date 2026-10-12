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
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import de.syss.MifareClassicTool.Activities.Preferences.Preference
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.MCReader
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.keymap.KeyFileChoice
import de.syss.MifareClassicTool.ui.keymap.KeyMapCreatorScreen
import de.syss.MifareClassicTool.ui.keymap.KeyMapCreatorUiState
import de.syss.MifareClassicTool.ui.theme.MctTheme
import java.io.File
import java.util.ArrayList
import java.util.Arrays

/**
 * Configure key map process and create key map.
 * @author Gerhard Klostermeier
 */
class KeyMapCreator : BasicActivity() {

    companion object {
        const val EXTRA_KEYS_DIR: String = "de.syss.MifareClassicTool.Activity.KEYS_DIR"
        const val EXTRA_SECTOR_CHOOSER: String = "de.syss.MifareClassicTool.Activity.SECTOR_CHOOSER"
        const val EXTRA_SECTOR_CHOOSER_FROM: String = "de.syss.MifareClassicTool.Activity.SECTOR_CHOOSER_FROM"
        const val EXTRA_SECTOR_CHOOSER_TO: String = "de.syss.MifareClassicTool.Activity.SECTOR_CHOOSER_TO"
        const val EXTRA_TITLE: String = "de.syss.MifareClassicTool.Activity.TITLE"
        const val EXTRA_BUTTON_TEXT: String = "de.syss.MifareClassicTool.Activity.BUTTON_TEXT"
        const val EXTRA_USE_KEYS_FROM_DUMP: String = "de.syss.MifareClassicTool.Activity.USE_KEYS_FROM_DUMP"

        const val MAX_SECTOR_COUNT: Int = 40
        const val MAX_BLOCK_COUNT_PER_SECTOR: Int = 16

        private val LOG_TAG: String = KeyMapCreator::class.java.simpleName
        private const val DEFAULT_SECTOR_RANGE_FROM: Int = 0
        private const val DEFAULT_SECTOR_RANGE_TO: Int = 15
    }

    private val mHandler = Handler(Looper.getMainLooper())
    private val mUiState = mutableStateOf(KeyMapCreatorUiState())
    private var mProgressStatus: Int = 0
    private var mIsCreatingKeyMap: Boolean = false
    private var mKeyDirPath: File? = null
    private var mFirstSector: Int = 0
    private var mLastSector: Int = 0
    private var mUseKeysFromDump: Boolean = false
    private var currentReader: MCReader? = null
    private var mSectorChooserEnabled: Boolean = true
    private var mCreateButtonText: String = ""
    private var mKeyFilesLoaded: Boolean = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        mCreateButtonText = getString(R.string.action_create_key_map)
        var sectorRange = getString(R.string.text_sector_range_all)

        val currentIntent = intent
        if (currentIntent.hasExtra(EXTRA_SECTOR_CHOOSER)) {
            mSectorChooserEnabled = currentIntent.getBooleanExtra(EXTRA_SECTOR_CHOOSER, true)
        }
        var custom = false
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        var from = sharedPref.getString("default_mapping_range_from", "") ?: ""
        var to = sharedPref.getString("default_mapping_range_to", "") ?: ""
        if (from.isNotEmpty()) {
            custom = true
        }
        if (to.isNotEmpty()) {
            custom = true
        }
        if (currentIntent.hasExtra(EXTRA_SECTOR_CHOOSER_FROM)) {
            from = "" + currentIntent.getIntExtra(EXTRA_SECTOR_CHOOSER_FROM, 0)
            custom = true
        }
        if (currentIntent.hasExtra(EXTRA_SECTOR_CHOOSER_TO)) {
            to = "" + currentIntent.getIntExtra(EXTRA_SECTOR_CHOOSER_TO, 15)
            custom = true
        }
        if (custom && from != "-1") {
            sectorRange = "$from - $to"
        }

        var title = getString(R.string.action_create_key_map)
        if (currentIntent.hasExtra(EXTRA_TITLE)) {
            title = currentIntent.getStringExtra(EXTRA_TITLE) ?: title
        }
        if (currentIntent.hasExtra(EXTRA_BUTTON_TEXT)) {
            mCreateButtonText = currentIntent.getStringExtra(EXTRA_BUTTON_TEXT) ?: mCreateButtonText
        }
        if (currentIntent.hasExtra(EXTRA_USE_KEYS_FROM_DUMP)) {
            mUseKeysFromDump = currentIntent.getBooleanExtra(EXTRA_USE_KEYS_FROM_DUMP, false)
        }
        mUiState.value = KeyMapCreatorUiState(
            title = title,
            sectorRange = sectorRange,
            sectorChooserEnabled = mSectorChooserEnabled,
            createButtonText = mCreateButtonText
        )
        setContent {
            MctTheme {
                val state = mUiState.value
                KeyMapCreatorScreen(
                    state = state,
                    onBackClick = { onCancelCreateKeyMap() },
                    onToggleKeyFile = { name, checked -> toggleKeyFile(name, checked) },
                    onSelectAll = { selectKeyFiles(true) },
                    onSelectNone = { selectKeyFiles(false) },
                    onChangeRangeClick = { openRangeDialog() },
                    onRangeFromChange = { value ->
                        mUiState.value = mUiState.value.copy(rangeFrom = value.filter(Char::isDigit).take(2))
                    },
                    onRangeToChange = { value ->
                        mUiState.value = mUiState.value.copy(rangeTo = value.filter(Char::isDigit).take(2))
                    },
                    onSaveRangeDefaultChange = { checked ->
                        mUiState.value = mUiState.value.copy(saveRangeAsDefault = checked)
                    },
                    onConfirmRange = { confirmRange() },
                    onReadAllSectors = { applyAllSectors() },
                    onDismissRange = { mUiState.value = mUiState.value.copy(showRangeDialog = false) },
                    onCreateClick = { onCreateKeyMap() },
                    onCancelClick = { onCancelCreateKeyMap() }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val autoReconnect = Common.getPreferences().getBoolean(
            Preference.AutoReconnect.toString(), false
        )
        if (!autoReconnect) {
            mIsCreatingKeyMap = false
            mHandler.removeCallbacksAndMessages(null)
        }
    }

    override fun onStart() {
        super.onStart()

        if (mKeyDirPath == null) {
            if (!intent.hasExtra(EXTRA_KEYS_DIR)) {
                setResult(2)
                finish()
                return
            }
            val path = intent.getStringExtra(EXTRA_KEYS_DIR)
            if (path == null) {
                setResult(4)
                finish()
                return
            }
            mKeyDirPath = File(path)
        }

        val keyDir = mKeyDirPath
        if (keyDir == null || !keyDir.exists()) {
            setResult(1)
            finish()
            return
        }

        val selectLastUsedKeyFiles = Common.getPreferences().getBoolean(
            Preference.SaveLastUsedKeyFiles.toString(), true
        )
        var selectedFiles: ArrayList<String>? = null
        if (selectLastUsedKeyFiles) {
            val sharedPref = getPreferences(Context.MODE_PRIVATE)
            val selectedFilesChain = sharedPref.getString("last_used_key_files", null)
            if (selectedFilesChain != null) {
                selectedFiles = ArrayList(Arrays.asList(*selectedFilesChain.split("/").toTypedArray()))
            }
        }
        if (mKeyFilesLoaded) {
            return
        }
        val choices = ArrayList<KeyFileChoice>()
        val keyFiles = keyDir.listFiles()
        if (keyFiles != null) {
            Arrays.sort(keyFiles)
            for (f in keyFiles) {
                val checked = selectLastUsedKeyFiles && selectedFiles != null && selectedFiles.contains(f.name)
                choices.add(KeyFileChoice(f.name, checked))
            }
        }
        mKeyFilesLoaded = true
        mUiState.value = mUiState.value.copy(keyFiles = choices)
    }

    private fun selectKeyFiles(allOrNone: Boolean) {
        if (mIsCreatingKeyMap) {
            return
        }
        mUiState.value = mUiState.value.copy(
            keyFiles = mUiState.value.keyFiles.map { it.copy(checked = allOrNone) }
        )
    }

    private fun toggleKeyFile(name: String, checked: Boolean) {
        if (mIsCreatingKeyMap) {
            return
        }
        mUiState.value = mUiState.value.copy(
            keyFiles = mUiState.value.keyFiles.map {
                if (it.name == name) it.copy(checked = checked) else it
            }
        )
    }

    private fun onCancelCreateKeyMap() {
        if (mIsCreatingKeyMap) {
            mIsCreatingKeyMap = false
            currentReader?.cancelCreateKeyMap()
            mUiState.value = mUiState.value.copy(cancelEnabled = false, statusMessage = getString(R.string.action_cancel))
        } else {
            finish()
        }
    }

    private fun onCreateKeyMap() {
        if (mIsCreatingKeyMap) {
            return
        }
        val saveLastUsedKeyFiles = Common.getPreferences().getBoolean(
            Preference.SaveLastUsedKeyFiles.toString(), true
        )
        val lastSelectedKeyFiles = StringBuilder()
        val fileNames = ArrayList<String>()
        for (choice in mUiState.value.keyFiles) {
            if (choice.checked) {
                fileNames.add(choice.name)
            }
        }
        if (fileNames.isNotEmpty()) {
            val keyFiles = ArrayList<File>()
            val keyDir = mKeyDirPath
            for (fileName in fileNames) {
                val keyFile = File(keyDir, fileName)
                if (keyFile.exists()) {
                    keyFiles.add(keyFile)
                    if (saveLastUsedKeyFiles) {
                        lastSelectedKeyFiles.append(fileName).append("/")
                    }
                } else {
                    Log.d(LOG_TAG, "Key file " + keyFile.absolutePath + " doesn't exist anymore.")
                }
            }
            if (keyFiles.isNotEmpty()) {
                if (saveLastUsedKeyFiles) {
                    val sharedPref = getPreferences(Context.MODE_PRIVATE)
                    val e = sharedPref.edit()
                    e.putString(
                        "last_used_key_files",
                        lastSelectedKeyFiles.substring(0, lastSelectedKeyFiles.length - 1)
                    )
                    e.apply()
                }

                val reader = Common.checkForTagAndCreateReader(this)
                currentReader = reader
                if (reader == null) {
                    return
                }

                if (mUseKeysFromDump) {
                    val tmpKeysFromDump = File(
                        Common.getFile(Common.TMP_DIR).absolutePath, "keys_from_dump.keys"
                    )
                    keyFiles.add(0, tmpKeysFromDump)
                }
                val keys = keyFiles.toTypedArray()
                val numberOfLoadedKeys = reader.setKeyFile(keys, this)
                if (numberOfLoadedKeys < 1) {
                    reader.close()
                    return
                }
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                if (mUiState.value.sectorRange == getString(R.string.text_sector_range_all)) {
                    mFirstSector = 0
                    mLastSector = reader.sectorCount - 1
                } else {
                    val fromAndTo = mUiState.value.sectorRange.split(" ")
                    mFirstSector = fromAndTo[0].toInt()
                    mLastSector = fromAndTo[2].toInt()
                }
                if (!reader.setMappingRange(mFirstSector, mLastSector)) {
                    Toast.makeText(this, R.string.info_mapping_sector_out_of_range, Toast.LENGTH_LONG).show()
                    reader.close()
                    return
                }
                Common.setKeyMapRange(mFirstSector, mLastSector)

                mProgressStatus = -1
                val progressMax = (mLastSector - mFirstSector) + 1
                mIsCreatingKeyMap = true
                mUiState.value = mUiState.value.copy(
                    scanning = true,
                    createEnabled = false,
                    cancelEnabled = true,
                    progress = 0,
                    progressMax = progressMax,
                    statusMessage = "$numberOfLoadedKeys " + getString(R.string.info_keys_loaded_please_wait)
                )
                createKeyMap(reader, this)
            } else {
                Toast.makeText(this, R.string.info_mapping_no_keyfile_found, Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, R.string.info_mapping_no_keyfile_selected, Toast.LENGTH_LONG).show()
        }
    }

    private fun createKeyMap(reader: MCReader, context: Context) {
        Thread {
            while (mProgressStatus < mLastSector) {
                mProgressStatus = reader.buildNextKeyMapPart()
                if (mProgressStatus == -1 || !mIsCreatingKeyMap) {
                    break
                }
                mHandler.post {
                    if (!isFinishing) {
                        mUiState.value = mUiState.value.copy(
                            progress = (mProgressStatus - mFirstSector) + 1
                        )
                    }
                }
            }

            mHandler.post {
                if (isFinishing) {
                    reader.close()
                    mIsCreatingKeyMap = false
                    return@post
                }
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                mUiState.value = mUiState.value.copy(
                    progress = 0,
                    scanning = false,
                    createEnabled = true,
                    cancelEnabled = true
                )
                reader.close()
                if (mIsCreatingKeyMap && mProgressStatus != -1) {
                    keyMapCreated(reader)
                } else if (mIsCreatingKeyMap && mProgressStatus == -1) {
                    Common.setKeyMap(null)
                    Common.setKeyMapRange(-1, -1)
                    mUiState.value = mUiState.value.copy(
                        statusMessage = getString(R.string.info_key_map_error)
                    )
                    Toast.makeText(context, R.string.info_key_map_error, Toast.LENGTH_LONG).show()
                } else {
                    Common.setKeyMap(null)
                    Common.setKeyMapRange(-1, -1)
                    mUiState.value = mUiState.value.copy(
                        statusMessage = getString(R.string.action_cancel)
                    )
                }
                mIsCreatingKeyMap = false
            }
        }.start()
    }

    private fun keyMapCreated(reader: MCReader) {
        val keyMap = reader.getKeyMap()
        if (keyMap == null || keyMap.size() == 0) {
            Common.setKeyMap(null)
            Toast.makeText(this, R.string.info_no_key_found, Toast.LENGTH_LONG).show()
        } else {
            Common.setKeyMap(keyMap)
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun openRangeDialog() {
        if (mIsCreatingKeyMap || !mSectorChooserEnabled) {
            return
        }
        val current = mUiState.value.sectorRange
        val parts = current.split(" ")
        val from = if (parts.size >= 3) parts[0] else ""
        val to = if (parts.size >= 3) parts[2] else ""
        mUiState.value = mUiState.value.copy(
            showRangeDialog = true,
            rangeFrom = from,
            rangeTo = to,
            saveRangeAsDefault = false
        )
    }

    private fun confirmRange() {
        val state = mUiState.value
        var txtFrom = "$DEFAULT_SECTOR_RANGE_FROM"
        var txtTo = "$DEFAULT_SECTOR_RANGE_TO"
        var noFrom = false
        if (state.rangeFrom.isNotEmpty()) {
            txtFrom = state.rangeFrom
        } else {
            noFrom = true
        }
        if (state.rangeTo.isNotEmpty()) {
            txtTo = state.rangeTo
        } else if (noFrom) {
            applyAllSectors()
            return
        }
        val intFrom = txtFrom.toIntOrNull()
        val intTo = txtTo.toIntOrNull()
        if (intFrom == null || intTo == null || intFrom > intTo || intFrom < 0 || intTo > MAX_SECTOR_COUNT - 1) {
            Toast.makeText(this, R.string.info_invalid_range, Toast.LENGTH_LONG).show()
            return
        }
        mUiState.value = state.copy(
            sectorRange = "$txtFrom - $txtTo",
            showRangeDialog = false
        )
        if (state.saveRangeAsDefault) {
            saveMappingRange(txtFrom, txtTo)
        }
    }

    private fun applyAllSectors() {
        val save = mUiState.value.saveRangeAsDefault
        mUiState.value = mUiState.value.copy(
            sectorRange = getString(R.string.text_sector_range_all),
            showRangeDialog = false
        )
        if (save) {
            saveMappingRange("", "")
        }
    }

    private fun saveMappingRange(from: String, to: String) {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val sharedEditor = sharedPref.edit()
        sharedEditor.putString("default_mapping_range_from", from)
        sharedEditor.putString("default_mapping_range_to", to)
        sharedEditor.apply()
    }
}
