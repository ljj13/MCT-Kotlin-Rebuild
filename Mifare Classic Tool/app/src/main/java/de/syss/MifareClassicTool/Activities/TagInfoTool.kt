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
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.NfcA
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.text.HtmlCompat
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.taginfo.TagInfoData
import de.syss.MifareClassicTool.ui.taginfo.TagInfoScreen
import de.syss.MifareClassicTool.ui.theme.MctTheme

/**
 * Display tag info like technology, size, sector count, etc.
 * Retains BasicActivity NFC foreground dispatch, onNewIntent tag parsing,
 * and MIFARE Classic hardware support validation.
 * @author Gerhard Klostermeier
 */
class TagInfoTool : BasicActivity() {

    private val mTagInfoState = mutableStateOf<TagInfoData?>(null)
    private val mMfcSupportState = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        updateTagInfo(Common.getTag())

        setContent {
            MctTheme {
                TagInfoScreen(
                    tagInfo = mTagInfoState.value,
                    mfcSupport = mMfcSupportState.intValue,
                    onBackClick = { finish() },
                    onReadMoreClick = { showReadMoreDialog() },
                    onCopyUid = { uid ->
                        Common.copyToClipboard(uid, this, true)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            updateTagInfo(Common.getTag())
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onReadMore(view: View? = null) {
        showReadMoreDialog()
    }

    private fun showReadMoreDialog() {
        val support = mMfcSupportState.intValue
        var titleID = 0
        var messageID = 0
        if (support == -1) {
            // Device does not support MIFARE Classic.
            titleID = R.string.dialog_no_mfc_support_device_title
            messageID = R.string.dialog_no_mfc_support_device
        } else if (support == -2) {
            // Tag does not support MIFARE Classic.
            titleID = R.string.dialog_no_mfc_support_tag_title
            messageID = R.string.dialog_no_mfc_support_tag
        }
        if (messageID == 0) {
            return
        }
        val styledText = HtmlCompat.fromHtml(
            getString(messageID), HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        val ad = AlertDialog.Builder(this)
            .setTitle(titleID)
            .setMessage(styledText)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(R.string.action_ok) { _, _ -> }
            .show()
        (ad.findViewById<TextView>(android.R.id.message))?.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun updateTagInfo(tag: Tag?) {
        if (tag != null) {
            val nfca = NfcA.get(tag)
            val mfcSupport = Common.checkMifareClassicSupport(tag, this)
            mMfcSupportState.intValue = mfcSupport

            val uidRaw = Common.bytes2Hex(tag.id)
            var uid = uidRaw
            val uidLen = tag.id.size
            uid += " ($uidLen byte"
            if (uidLen == 7) {
                uid += ", CL2"
            } else if (uidLen == 10) {
                uid += ", CL3"
            }
            uid += ")"

            // Swap ATQA to match standard order
            var atqaBytes = nfca.atqa
            atqaBytes = byteArrayOf(atqaBytes[1], atqaBytes[0])
            val atqa = Common.bytes2Hex(atqaBytes)

            // SAK in big endian
            val sakBytes = byteArrayOf(
                ((nfca.sak.toInt() shr 8) and 0xFF).toByte(),
                (nfca.sak.toInt() and 0xFF).toByte()
            )
            val sak = if (sakBytes[0].toInt() != 0) {
                Common.bytes2Hex(sakBytes)
            } else {
                Common.bytes2Hex(byteArrayOf(sakBytes[1]))
            }

            var ats = "-"
            val iso = IsoDep.get(tag)
            if (iso != null) {
                val atsBytes = iso.historicalBytes
                if (atsBytes != null && atsBytes.isNotEmpty()) {
                    ats = Common.bytes2Hex(atsBytes)
                }
            }

            // Identify tag type
            val tagTypeResourceID = getTagIdentifier(atqa, sak, ats)
            val tagType: String = if (tagTypeResourceID == R.string.tag_unknown && mfcSupport > -2) {
                getString(R.string.tag_unknown_mf_classic)
            } else {
                getString(tagTypeResourceID)
            }
            val isGuessed = (tagTypeResourceID != R.string.tag_unknown)

            var memSize: String? = null
            var blockSize: String? = null
            var sectorCount: String? = null
            var blockCount: String? = null

            if (mfcSupport == 0) {
                val mfc = MifareClassic.get(tag)
                if (mfc != null) {
                    memSize = "${mfc.size}"
                    blockSize = "${MifareClassic.BLOCK_SIZE}"
                    sectorCount = "${mfc.sectorCount}"
                    blockCount = "${mfc.blockCount}"
                }
            }

            mTagInfoState.value = TagInfoData(
                uid = uid,
                uidRaw = uidRaw,
                rfTech = getString(R.string.text_rf_tech_14a),
                atqa = atqa,
                sak = sak,
                ats = ats,
                tagType = tagType,
                isTagTypeGuessed = isGuessed,
                memSize = memSize,
                blockSize = blockSize,
                sectorCount = sectorCount,
                blockCount = blockCount
            )
        } else {
            mMfcSupportState.intValue = 0
            mTagInfoState.value = null
        }
    }

    private fun getTagIdentifier(atqa: String, sak: String, ats: String): Int {
        val prefix = "tag_"
        val cleanAts = ats.replace("-", "")

        var ret = resources.getIdentifier(
            prefix + atqa + sak + cleanAts, "string", packageName
        )
        if (ret == 0) {
            ret = resources.getIdentifier(
                prefix + atqa + sak, "string", packageName
            )
        }
        if (ret == 0) {
            ret = resources.getIdentifier(
                prefix + atqa, "string", packageName
            )
        }
        if (ret == 0) {
            return R.string.tag_unknown
        }
        return ret
    }
}
