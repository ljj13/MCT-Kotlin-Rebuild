/*
 * Copyright 2015 Gerhard Klostermeier
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
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.nfc.tech.MifareClassic
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.MCReader
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.cloneuid.CloneUidScreen
import de.syss.MifareClassicTool.ui.cloneuid.CloneUidUiState
import de.syss.MifareClassicTool.ui.theme.MctTheme
import java.util.Random

/**
 * Clone UID to "magic tag gen2". The gen2 magic tags allow direct write to
 * block 0 without the need for special "backdoor" commands.
 * Retains BasicActivity NFC foreground dispatch and zero automated physical write guarantees.
 * @author Slawomir Jasek and Gerhard Klostermeier
 */
class CloneUidTool : BasicActivity() {

    private var mUidLen: Int = 0
    private var mBlock0Complete = ""
    private var mBlock0Rest = "080400475955D141103607"
    private var mBlock0Key = MCReader.DEFAULT_KEY
    private var mIgnoreIncorrectBlock0 = false

    private enum class Status { INIT, BLOCK0_CALCULATED, CLONED }
    private var mStatus = Status.INIT

    private val mUiState = mutableStateOf(CloneUidUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        var initialUid = ""
        val initialLogs = mutableListOf<String>()

        val testUid = intent.getStringExtra("extra_test_uid")
        if (testUid != null) {
            initialUid = testUid
            initialLogs.add(getString(R.string.text_use_uid_of_scanned_tag) + " ($testUid)")
        } else if (Common.getTag() != null) {
            val uid = Common.bytes2Hex(Common.getUID())
            initialUid = uid
            initialLogs.add(getString(R.string.text_use_uid_of_scanned_tag) + " ($uid)")
        }

        mUiState.value = CloneUidUiState(
            uidInput = initialUid,
            block0RestInput = mBlock0Rest,
            writeKeyInput = mBlock0Key,
            useKeyB = false,
            autoCalcSakAtqa = false,
            showOptions = false,
            block0Complete = null,
            statusLogs = initialLogs
        )

        setContent {
            MctTheme {
                CloneUidScreen(
                    state = mUiState.value,
                    onStateChange = { mUiState.value = it },
                    onBackClick = { finish() },
                    onPasteUid = { onPasteUidFromClipboard(null) },
                    onRandomUid = { onRandomUid(null) },
                    onCalculateBlock0 = { onCalculateBlock0(null) },
                    onShowInfo = { onShowInfo(null) }
                )
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent) {
        val typeCheck = Common.treatAsNewTag(intent, this)
        if (typeCheck == -1 || typeCheck == -2) {
            val i = Intent(this, TagInfoTool::class.java)
            startActivity(i)
        }
        if (typeCheck != -4) {
            val uid = Common.bytes2Hex(Common.getUID())
            when (mStatus) {
                Status.INIT -> {
                    mUiState.value = mUiState.value.copy(uidInput = uid)
                    appendToLog(getString(R.string.text_use_uid_of_scanned_tag) + " ($uid)")
                }
                Status.BLOCK0_CALCULATED -> {
                    writeManufacturerBlock()
                }
                Status.CLONED -> {
                    appendToLog(getString(R.string.text_checking_clone))
                    val uidOriginal = mUiState.value.uidInput
                    if (uid == uidOriginal) {
                        appendToLog(getString(R.string.text_clone_successfully))
                        appendToLog(getString(R.string.text_reset_clone_process))
                        mStatus = Status.INIT
                        mIgnoreIncorrectBlock0 = false
                    } else {
                        appendToLog(getString(R.string.text_uid_match_error) + " ($uidOriginal <-> $uid)")
                        appendToLog(getString(R.string.text_clone_error))
                        mStatus = Status.BLOCK0_CALCULATED
                    }
                }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCalculateBlock0(view: View? = null) {
        val state = mUiState.value
        val uid = state.uidInput.trim()
        android.util.Log.d("CloneUidTool", "onCalculateBlock0 called with uid: '$uid'")
        mUidLen = uid.length
        mBlock0Rest = state.block0RestInput.trim()
        mBlock0Key = state.writeKeyInput.trim()

        if (!mBlock0Rest.matches(Regex("[0-9A-Fa-f]+"))
            || !uid.matches(Regex("[0-9A-Fa-f]+"))
            || !mBlock0Key.matches(Regex("[0-9A-Fa-f]+"))
        ) {
            Toast.makeText(this, R.string.info_not_hex_data, Toast.LENGTH_LONG).show()
            return
        }

        if (mBlock0Key.length != 12) {
            Toast.makeText(this, R.string.info_valid_keys_not_6_byte, Toast.LENGTH_LONG).show()
            return
        }

        if (mUidLen != 8 && mUidLen != 14 && mUidLen != 20) {
            Toast.makeText(this, R.string.info_invalid_uid_length, Toast.LENGTH_LONG).show()
            return
        }
        mUidLen /= 2

        val block0RestLen = if (mUidLen == 4) 22 else 32 - mUidLen * 2
        if (mBlock0Rest.length < block0RestLen) {
            Toast.makeText(this, R.string.info_rest_of_block_0_length, Toast.LENGTH_LONG).show()
            return
        }

        if (mUidLen == 4) {
            val bcc = Common.calcBcc(Common.hex2Bytes(uid)!!)
            mBlock0Complete = uid + String.format("%02X", bcc) + mBlock0Rest
        } else {
            mBlock0Complete = uid + mBlock0Rest.substring(mBlock0Rest.length - block0RestLen)
        }

        mStatus = Status.BLOCK0_CALCULATED
        val updatedLogs = mUiState.value.statusLogs +
                getString(R.string.text_block_0_generated) +
                getString(R.string.text_waiting_for_magic_tag)

        mUiState.value = mUiState.value.copy(
            block0Complete = mBlock0Complete,
            statusLogs = updatedLogs,
            showOptions = false
        )
    }

    private fun writeManufacturerBlock() {
        val state = mUiState.value
        val keyB = state.useKeyB
        val key = Common.hex2Bytes(state.writeKeyInput) ?: return

        val reader = Common.checkForTagAndCreateReader(this) ?: return

        val currentUid = Common.getUID()
        val uidLen = currentUid?.size ?: 0
        if (uidLen != mUidLen) {
            appendToLog(getString(R.string.text_uid_length_error))
            reader.close()
            return
        }

        if (state.autoCalcSakAtqa) {
            val sakAndAtqa = calcSakAtqa(uidLen, reader.size)
            if (sakAndAtqa == null) {
                appendToLog(getString(R.string.text_sak_atqa_calc_warning))
            } else {
                val sakStart = if (uidLen == 4) uidLen * 2 + 2 else uidLen * 2
                mBlock0Complete = mBlock0Complete.substring(0, sakStart) +
                        sakAndAtqa +
                        mBlock0Complete.substring(sakStart + sakAndAtqa.length)
            }
        }
        appendToLog(getString(R.string.text_data_to_write) + " " + mBlock0Complete)

        if (!mIgnoreIncorrectBlock0 && !Common.isValidBlock0(
                mBlock0Complete, uidLen, reader.size, false
            )
        ) {
            appendToLog(getString(R.string.text_block0_warning))
            showBlock0Warning()
            reader.close()
            return
        }

        appendToLog(getString(R.string.text_writing_block_0))
        val block0Bytes = Common.hex2Bytes(mBlock0Complete) ?: return
        val result = reader.writeBlock(0, 0, block0Bytes, key, keyB)

        when (result) {
            4 -> {
                appendToLog(getString(R.string.info_incorrect_key))
                return
            }
            -1 -> {
                Toast.makeText(this, R.string.info_write_error, Toast.LENGTH_LONG).show()
                appendToLog(getString(R.string.text_clone_error))
                return
            }
        }

        appendToLog(getString(R.string.text_no_errors_on_write))
        appendToLog(getString(R.string.text_rescan_tag_to_check))
        mStatus = Status.CLONED
        reader.close()
    }

    private fun calcSakAtqa(uidLen: Int, tagSize: Int): String? {
        if ((uidLen != 4 && uidLen != 7) || (
                    tagSize != MifareClassic.SIZE_MINI &&
                            tagSize != MifareClassic.SIZE_1K &&
                            tagSize != MifareClassic.SIZE_2K &&
                            tagSize != MifareClassic.SIZE_4K)
        ) {
            return null
        }

        var atqa: String? = null
        if (uidLen == 4 && (tagSize == MifareClassic.SIZE_1K ||
                    tagSize == MifareClassic.SIZE_2K ||
                    tagSize == MifareClassic.SIZE_MINI)
        ) {
            atqa = "0400"
        } else if (uidLen == 4 && tagSize == MifareClassic.SIZE_4K) {
            atqa = "0200"
        } else if (uidLen == 7 && (tagSize == MifareClassic.SIZE_1K ||
                    tagSize == MifareClassic.SIZE_2K ||
                    tagSize == MifareClassic.SIZE_MINI)
        ) {
            atqa = "4400"
        } else if (uidLen == 7 && tagSize == MifareClassic.SIZE_4K) {
            atqa = "4200"
        }

        var sak: String? = null
        if (tagSize == MifareClassic.SIZE_MINI) {
            sak = "09"
        } else if (tagSize == MifareClassic.SIZE_1K || tagSize == MifareClassic.SIZE_2K) {
            sak = "08"
        } else if (tagSize == MifareClassic.SIZE_4K) {
            sak = "18"
        }

        if (sak == null || atqa == null) {
            return null
        }
        return sak + atqa
    }

    private fun showBlock0Warning() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_block0_data_warning_title)
            .setMessage(R.string.dialog_block0_data_warning)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(R.string.action_i_know_what_i_am_doing) { _, _ ->
                mIgnoreIncorrectBlock0 = true
                writeManufacturerBlock()
            }
            .setNegativeButton(R.string.action_cancel) { dialog, _ -> dialog.cancel() }
            .setOnCancelListener {
                mStatus = Status.INIT
                appendToLog(getString(R.string.text_reset_clone_process))
            }
            .show()
    }

    private fun appendToLog(text: String) {
        val currentLogs = mUiState.value.statusLogs
        mUiState.value = mUiState.value.copy(statusLogs = currentLogs + text)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onShowOptions(view: View? = null) {
        mUiState.value = mUiState.value.copy(showOptions = !mUiState.value.showOptions)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onPasteUidFromClipboard(view: View? = null) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            val clean = text.filter { it.isDigit() || (it in 'a'..'f') || (it in 'A'..'F') }.uppercase()
            if (clean.length == 8 || clean.length == 14 || clean.length == 20) {
                mUiState.value = mUiState.value.copy(uidInput = clean)
            } else {
                Toast.makeText(this, R.string.info_invalid_uid_length, Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onRandomUid(view: View? = null) {
        val bytes = ByteArray(4)
        Random().nextBytes(bytes)
        val uid = Common.bytes2Hex(bytes)
        mUiState.value = mUiState.value.copy(uidInput = uid)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onShowInfo(view: View? = null) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_gen2_tags_info_title)
            .setMessage(R.string.dialog_gen2_tags_info)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton(R.string.action_ok) { _, _ -> }
            .show()
    }
}
