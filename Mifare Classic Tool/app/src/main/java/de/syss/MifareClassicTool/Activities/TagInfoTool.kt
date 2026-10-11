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
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.NfcA
import android.os.Bundle
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.core.widget.TextViewCompat
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R

/**
 * Display tag info like technology, size, sector count, etc.
 * This is the only thing a user can do with a device that does not support
 * MIFARE Classic.
 * @author Gerhard Klostermeier
 */
class TagInfoTool : BasicActivity() {

    private lateinit var mLayout: LinearLayout
    private lateinit var mErrorMessage: TextView
    private var mMFCSupport: Int = 0

    /**
     * Calls [updateTagInfo] (and initializes some member variables).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_info_tool)

        mLayout = findViewById(R.id.linearLayoutTagInfoTool)
        mErrorMessage = findViewById(R.id.textTagInfoToolErrorMessage)
        updateTagInfo(Common.getTag())
    }

    /**
     * Calls [Common.treatAsNewTag] and then calls [updateTagInfo].
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            updateTagInfo(Common.getTag())
        }
    }

    /**
     * Show a dialog with further information.
     * @param view The View object that triggered the method
     * (in this case the read more button).
     */
    @Suppress("UNUSED_PARAMETER")
    fun onReadMore(view: View) {
        var titleID = 0
        var messageID = 0
        if (mMFCSupport == -1) {
            // Device does not support MIFARE Classic.
            titleID = R.string.dialog_no_mfc_support_device_title
            messageID = R.string.dialog_no_mfc_support_device
        } else if (mMFCSupport == -2) {
            // Tag does not support MIFARE Classic.
            titleID = R.string.dialog_no_mfc_support_tag_title
            messageID = R.string.dialog_no_mfc_support_tag
        }
        if (messageID == 0) {
            // Error.
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
        // Make links clickable.
        (ad.findViewById<TextView>(android.R.id.message))?.movementMethod = LinkMovementMethod.getInstance()
    }

    /**
     * Update and display the tag information.
     * If there is no MIFARE Classic support, a warning will be shown.
     * @param tag A Tag from an NFC Intent.
     */
    @SuppressLint("SetTextI18n")
    private fun updateTagInfo(tag: Tag?) {
        if (tag != null) {
            val nfca = NfcA.get(tag)
            // Check for MIFARE Classic support.
            mMFCSupport = Common.checkMifareClassicSupport(tag, this)

            mLayout.removeAllViews()
            // Display generic info.
            // Create views and add them to the layout.
            val headerGenericInfo = TextView(this)
            headerGenericInfo.text = Common.colorString(
                getString(R.string.text_generic_info), Common.getThemeAccentColor(this)
            )
            TextViewCompat.setTextAppearance(
                headerGenericInfo,
                android.R.style.TextAppearance_Large
            )
            headerGenericInfo.gravity = Gravity.CENTER_HORIZONTAL
            val pad = Common.dpToPx(5) // 5dp to px.
            headerGenericInfo.setPadding(pad, pad, pad, pad)
            mLayout.addView(headerGenericInfo)
            val genericInfo = TextView(this)
            genericInfo.setPadding(pad, pad, pad, pad)
            TextViewCompat.setTextAppearance(
                genericInfo,
                android.R.style.TextAppearance_Medium
            )
            mLayout.addView(genericInfo)
            // Get generic info and set these as text.
            var uid = Common.bytes2Hex(tag.id)
            val uidLen = tag.id.size
            uid += " ($uidLen byte"
            if (uidLen == 7) {
                uid += ", CL2"
            } else if (uidLen == 10) {
                uid += ", CL3"
            }
            uid += ")"
            // Swap ATQA to match the common order like shown here:
            // http://nfc-tools.org/index.php?title=ISO14443A
            var atqaBytes = nfca.atqa
            atqaBytes = byteArrayOf(atqaBytes[1], atqaBytes[0])
            val atqa = Common.bytes2Hex(atqaBytes)
            // SAK in big endian.
            val sakBytes = byteArrayOf(
                ((nfca.sak.toInt() shr 8) and 0xFF).toByte(),
                (nfca.sak.toInt() and 0xFF).toByte()
            )
            // Print the first SAK byte only if it is not 0.
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
            // Identify tag type.
            val tagTypeResourceID = getTagIdentifier(atqa, sak, ats)
            val tagType: String = if (tagTypeResourceID == R.string.tag_unknown && mMFCSupport > -2) {
                getString(R.string.tag_unknown_mf_classic)
            } else {
                getString(tagTypeResourceID)
            }

            val hc = Common.getThemeAccentColor(this)
            genericInfo.text = TextUtils.concat(
                Common.colorString(getString(R.string.text_uid) + ":", hc),
                "\n", uid, "\n",
                Common.colorString(getString(R.string.text_rf_tech) + ":", hc),
                "\n", getString(R.string.text_rf_tech_14a), "\n",
                Common.colorString(getString(R.string.text_atqa) + ":", hc),
                "\n", atqa, "\n",
                Common.colorString(getString(R.string.text_sak) + ":", hc),
                "\n", sak, "\n",
                Common.colorString(getString(R.string.text_ats) + ":", hc),
                "\n", ats, "\n",
                Common.colorString(getString(R.string.text_tag_type_and_manuf) + ":", hc),
                "\n", tagType
            )

            // Add message that the tag type might be wrong.
            if (tagTypeResourceID != R.string.tag_unknown) {
                val tagTypeInfo = TextView(this)
                tagTypeInfo.setPadding(pad, 0, pad, pad)
                tagTypeInfo.text = "(" + getString(R.string.text_tag_type_guess) + ")"
                mLayout.addView(tagTypeInfo)
            }

            val layout = findViewById<LinearLayout>(R.id.linearLayoutTagInfoToolSupport)
            // Check for MIFARE Classic support.
            if (mMFCSupport == 0) {
                // Display MIFARE Classic info.
                // Create views and add them to the layout.
                val headerMifareInfo = TextView(this)
                headerMifareInfo.text = Common.colorString(
                    getString(R.string.text_mf_info), Common.getThemeAccentColor(this)
                )
                TextViewCompat.setTextAppearance(
                    headerMifareInfo,
                    android.R.style.TextAppearance_Large
                )
                headerMifareInfo.gravity = Gravity.CENTER_HORIZONTAL
                headerMifareInfo.setPadding(pad, pad * 2, pad, pad)
                mLayout.addView(headerMifareInfo)
                val mifareInfo = TextView(this)
                mifareInfo.setPadding(pad, pad, pad, pad)
                TextViewCompat.setTextAppearance(
                    mifareInfo,
                    android.R.style.TextAppearance_Medium
                )
                mLayout.addView(mifareInfo)

                // Get MIFARE info and set these as text.
                val mfc = MifareClassic.get(tag)
                val size = "" + mfc.size
                val sectorCount = "" + mfc.sectorCount
                val blockCount = "" + mfc.blockCount
                mifareInfo.text = TextUtils.concat(
                    Common.colorString(getString(R.string.text_mem_size) + ":", hc),
                    "\n", size, " byte\n",
                    Common.colorString(getString(R.string.text_block_size) + ":", hc),
                    "\n", "" + MifareClassic.BLOCK_SIZE, " byte\n",
                    Common.colorString(getString(R.string.text_sector_count) + ":", hc),
                    "\n", sectorCount, "\n",
                    Common.colorString(getString(R.string.text_block_count) + ":", hc),
                    "\n", blockCount
                )
                layout.visibility = View.GONE
            } else if (mMFCSupport == -1) {
                // No MIFARE Classic Support (due to the device hardware).
                // Set error message.
                mErrorMessage.setText(R.string.text_no_mfc_support_device)
                layout.visibility = View.VISIBLE
            } else if (mMFCSupport == -2) {
                // The tag does not support MIFARE Classic.
                // Set error message.
                mErrorMessage.setText(R.string.text_no_mfc_support_tag)
                layout.visibility = View.VISIBLE
            }
        } else {
            // There is no Tag.
            val text = TextView(this)
            val pad = Common.dpToPx(5)
            text.setPadding(pad, pad, 0, 0)
            TextViewCompat.setTextAppearance(text, android.R.style.TextAppearance_Medium)
            text.text = getString(R.string.text_no_tag)
            mLayout.removeAllViews()
            mLayout.addView(text)
            Toast.makeText(this, R.string.info_no_tag_found, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Get (determine) the tag type resource ID from ATQA + SAK + ATS.
     */
    private fun getTagIdentifier(atqa: String, sak: String, ats: String): Int {
        val prefix = "tag_"
        val cleanAts = ats.replace("-", "")

        // First check on ATQA + SAK + ATS.
        var ret = resources.getIdentifier(
            prefix + atqa + sak + cleanAts, "string", packageName
        )

        if (ret == 0) {
            // Check on ATQA + SAK.
            ret = resources.getIdentifier(
                prefix + atqa + sak, "string", packageName
            )
        }

        if (ret == 0) {
            // Check on ATQA.
            ret = resources.getIdentifier(
                prefix + atqa, "string", packageName
            )
        }

        if (ret == 0) {
            // No match found return "Unknown".
            return R.string.tag_unknown
        }
        return ret
    }
}
