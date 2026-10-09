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
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.InputType
import android.text.TextUtils.TruncateAt
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatEditText
import de.syss.MifareClassicTool.Activities.Preferences.Preference
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.MCReader
import de.syss.MifareClassicTool.R
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

    private lateinit var mCreateKeyMap: Button
    private lateinit var mCancel: Button
    private lateinit var mKeyFilesGroup: LinearLayout
    private lateinit var mSectorRange: TextView
    private val mHandler = Handler(Looper.getMainLooper())
    private var mProgressStatus: Int = 0
    private lateinit var mProgressBar: ProgressBar
    private var mIsCreatingKeyMap: Boolean = false
    private var mKeyDirPath: File? = null
    private var mFirstSector: Int = 0
    private var mLastSector: Int = 0
    private var mUseKeysFromDump: Boolean = false
    private var currentReader: MCReader? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_key_map)
        mCreateKeyMap = findViewById(R.id.buttonCreateKeyMap)
        mCancel = findViewById(R.id.buttonCreateKeyMapCancel)
        mSectorRange = findViewById(R.id.textViewCreateKeyMapFromTo)
        mKeyFilesGroup = findViewById(R.id.linearLayoutCreateKeyMapKeyFiles)
        mProgressBar = findViewById(R.id.progressBarCreateKeyMap)

        val currentIntent = intent
        if (currentIntent.hasExtra(EXTRA_SECTOR_CHOOSER)) {
            val changeSectorRange = findViewById<Button>(R.id.buttonCreateKeyMapChangeRange)
            val value = currentIntent.getBooleanExtra(EXTRA_SECTOR_CHOOSER, true)
            changeSectorRange.isEnabled = value
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
        if (custom) {
            if (from == "-1") {
                mSectorRange.setText(R.string.text_sector_range_all)
            } else {
                mSectorRange.text = "$from - $to"
            }
        }

        if (currentIntent.hasExtra(EXTRA_TITLE)) {
            title = currentIntent.getStringExtra(EXTRA_TITLE)
        }
        if (currentIntent.hasExtra(EXTRA_BUTTON_TEXT)) {
            mCreateKeyMap.text = currentIntent.getStringExtra(EXTRA_BUTTON_TEXT)
        }
        if (currentIntent.hasExtra(EXTRA_USE_KEYS_FROM_DUMP)) {
            mUseKeysFromDump = currentIntent.getBooleanExtra(EXTRA_USE_KEYS_FROM_DUMP, false)
        }
    }

    override fun onPause() {
        super.onPause()
        val autoReconnect = Common.getPreferences().getBoolean(
            Preference.AutoReconnect.toString(), false
        )
        if (!autoReconnect) {
            mIsCreatingKeyMap = false
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
        mKeyFilesGroup.removeAllViews()
        val keyFiles = keyDir.listFiles()
        if (keyFiles != null) {
            Arrays.sort(keyFiles)
            for (f in keyFiles) {
                val c = AppCompatCheckBox(this)
                c.text = f.name
                if (selectLastUsedKeyFiles && selectedFiles != null && selectedFiles.contains(f.name)) {
                    c.isChecked = true
                }
                mKeyFilesGroup.addView(c)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onSelectAll(view: View) {
        selectKeyFiles(true)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onSelectNone(view: View) {
        selectKeyFiles(false)
    }

    private fun selectKeyFiles(allOrNone: Boolean) {
        for (i in 0 until mKeyFilesGroup.childCount) {
            val c = mKeyFilesGroup.getChildAt(i) as AppCompatCheckBox
            c.isChecked = allOrNone
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCancelCreateKeyMap(view: View) {
        if (mIsCreatingKeyMap) {
            mIsCreatingKeyMap = false
            currentReader?.cancelCreateKeyMap()
            mCancel.isEnabled = false
        } else {
            finish()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCreateKeyMap(view: View) {
        val saveLastUsedKeyFiles = Common.getPreferences().getBoolean(
            Preference.SaveLastUsedKeyFiles.toString(), true
        )
        val lastSelectedKeyFiles = StringBuilder()
        val fileNames = ArrayList<String>()
        for (i in 0 until mKeyFilesGroup.childCount) {
            val c = mKeyFilesGroup.getChildAt(i) as AppCompatCheckBox
            if (c.isChecked) {
                fileNames.add(c.text.toString())
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

                if (mSectorRange.text.toString() == getString(R.string.text_sector_range_all)) {
                    mFirstSector = 0
                    mLastSector = reader.sectorCount - 1
                } else {
                    val fromAndTo = mSectorRange.text.toString().split(" ")
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
                mProgressBar.max = (mLastSector - mFirstSector) + 1
                mCreateKeyMap.isEnabled = false
                mIsCreatingKeyMap = true
                val message = "$numberOfLoadedKeys " + getString(R.string.info_keys_loaded_please_wait)
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
                    mProgressBar.progress = (mProgressStatus - mFirstSector) + 1
                }
            }

            mHandler.post {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                mProgressBar.progress = 0
                mCreateKeyMap.isEnabled = true
                reader.close()
                if (mIsCreatingKeyMap && mProgressStatus != -1) {
                    keyMapCreated(reader)
                } else if (mIsCreatingKeyMap && mProgressStatus == -1) {
                    Common.setKeyMap(null)
                    Common.setKeyMapRange(-1, -1)
                    mCancel.isEnabled = true
                    Toast.makeText(context, R.string.info_key_map_error, Toast.LENGTH_LONG).show()
                } else {
                    Common.setKeyMap(null)
                    Common.setKeyMapRange(-1, -1)
                    mCancel.isEnabled = true
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

    @SuppressLint("SetTextI18n")
    @Suppress("UNUSED_PARAMETER")
    fun onChangeSectorRange(view: View) {
        val ll = LinearLayout(this)
        val llv = LinearLayout(this)
        val pad = Common.dpToPx(10)
        llv.setPadding(pad, pad, pad, pad)
        llv.orientation = LinearLayout.VERTICAL
        llv.gravity = Gravity.CENTER
        ll.gravity = Gravity.CENTER

        val tvFrom = TextView(this)
        tvFrom.text = getString(R.string.text_from) + ": "
        tvFrom.textSize = 18f
        val tvTo = TextView(this)
        tvTo.text = " " + getString(R.string.text_to) + ": "
        tvTo.textSize = 18f

        val saveAsDefault = AppCompatCheckBox(this)
        saveAsDefault.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        saveAsDefault.setText(R.string.action_save_as_default)
        saveAsDefault.textSize = 18f
        tvFrom.setTextColor(saveAsDefault.currentTextColor)
        tvTo.setTextColor(saveAsDefault.currentTextColor)

        val f = arrayOf<InputFilter>(InputFilter.LengthFilter(2))
        val from = AppCompatEditText(this)
        from.ellipsize = TruncateAt.END
        from.maxLines = 1
        from.setSingleLine()
        from.inputType = InputType.TYPE_CLASS_NUMBER
        from.minimumWidth = 60
        from.filters = f
        from.gravity = Gravity.CENTER_HORIZONTAL

        val to = AppCompatEditText(this)
        to.ellipsize = TruncateAt.END
        to.maxLines = 1
        to.setSingleLine()
        to.inputType = InputType.TYPE_CLASS_NUMBER
        to.minimumWidth = 60
        to.filters = f
        to.gravity = Gravity.CENTER_HORIZONTAL

        ll.addView(tvFrom)
        ll.addView(from)
        ll.addView(tvTo)
        ll.addView(to)
        llv.addView(ll)
        llv.addView(saveAsDefault)
        val err = Toast.makeText(this, R.string.info_invalid_range, Toast.LENGTH_LONG)

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_mapping_range_title)
            .setMessage(R.string.dialog_mapping_range)
            .setView(llv)
            .setPositiveButton(R.string.action_ok) { _, _ ->
                var txtFrom = "$DEFAULT_SECTOR_RANGE_FROM"
                var txtTo = "$DEFAULT_SECTOR_RANGE_TO"
                var noFrom = false
                val fromStr = from.text?.toString() ?: ""
                val toStr = to.text?.toString() ?: ""
                if (fromStr.isNotEmpty()) {
                    txtFrom = fromStr
                } else {
                    noFrom = true
                }
                if (toStr.isNotEmpty()) {
                    txtTo = toStr
                } else if (noFrom) {
                    mSectorRange.setText(R.string.text_sector_range_all)
                    if (saveAsDefault.isChecked) {
                        saveMappingRange("", "")
                    }
                    return@setPositiveButton
                }
                val intFrom = txtFrom.toInt()
                val intTo = txtTo.toInt()
                if (intFrom > intTo || intFrom < 0 || intTo > MAX_SECTOR_COUNT - 1) {
                    err.show()
                } else {
                    mSectorRange.text = "$txtFrom - $txtTo"
                    if (saveAsDefault.isChecked) {
                        saveMappingRange(txtFrom, txtTo)
                    }
                }
            }
            .setNeutralButton(R.string.action_read_all_sectors) { _, _ ->
                mSectorRange.setText(R.string.text_sector_range_all)
                if (saveAsDefault.isChecked) {
                    saveMappingRange("", "")
                }
            }
            .setNegativeButton(R.string.action_cancel) { _, _ -> }
            .show()
    }

    private fun saveMappingRange(from: String, to: String) {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val sharedEditor = sharedPref.edit()
        sharedEditor.putString("default_mapping_range_from", from)
        sharedEditor.putString("default_mapping_range_to", to)
        sharedEditor.apply()
    }
}
