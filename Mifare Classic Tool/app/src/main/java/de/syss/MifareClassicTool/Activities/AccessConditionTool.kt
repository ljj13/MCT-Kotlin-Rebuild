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
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.Toast
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R

/**
 * Decode MIFARE Classic Access Conditions from their hex format
 * to a more human readable format and vice versa.
 * @author Gerhard Klostermeier
 */
class AccessConditionTool : BasicActivity() {

    private lateinit var mAC: EditText
    private val mBlockButtons = arrayOfNulls<Button>(4)
    private var mWasKeyBReadable: Boolean = false

    /**
     * True if the Access Conditions of the Sector Trailer state that key B is readable.
     */
    private var mIsKeyBReadable: Boolean = false

    /**
     * Matrix of access conditions bits (C1-C3) where the first
     * dimension is the "C" parameter (C1-C3, Index 0-2) and the second
     * dimension is the block number (Index 0-3).
     */
    private var mACMatrix: Array<ByteArray> = Array(3) { ByteArray(4) }

    /**
     * The last clicked "choose Access Conditions for data block"-button.
     */
    private var mSelectedButton: Button? = null

    /**
     * A dialog which allows the user to choose between all possible
     * Access Conditions for a data block.
     */
    private var mDataBlockDialog: AlertDialog? = null

    /**
     * A dialog which allows the user to choose between all possible
     * Access Conditions for a Sector Trailer.
     */
    private var mSectorTrailerDialog: AlertDialog? = null

    /**
     * Build the two dialogs for choosing the Access Conditions
     * ([mDataBlockDialog] and [mSectorTrailerDialog]) and
     * initialize some member variables.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_access_condition_tool)

        // Init. member vars.
        mAC = findViewById(R.id.editTextAccessConditionToolAC)
        mBlockButtons[0] = findViewById(R.id.buttonAccessConditionToolBlock0)
        mBlockButtons[1] = findViewById(R.id.buttonAccessConditionToolBlock1)
        mBlockButtons[2] = findViewById(R.id.buttonAccessConditionToolBlock2)
        mBlockButtons[3] = findViewById(R.id.buttonAccessConditionToolBlock3)

        // Init AC matrix with factory setting/transport configuration.
        mACMatrix = arrayOf(
            byteArrayOf(0, 0, 0, 0),
            byteArrayOf(0, 0, 0, 0),
            byteArrayOf(0, 0, 0, 1)
        )

        // Build the dialog with Access Conditions for the Sector Trailer.
        val items = Array(8) { i -> getString(getResourceForSectorTrailersByRowNr(i)) }
        val adapter = ArrayAdapter(this, R.layout.list_item_small_text, items)
        val lv = ListView(this)
        lv.adapter = adapter
        lv.setOnItemClickListener { _, _, position, _ ->
            // Change button text to selected Access Conditions.
            mBlockButtons[3]?.text = getString(getResourceForSectorTrailersByRowNr(position))
            // Set Access Condition bits for sector trailer.
            val acBits = acRowNrToACBits(position, true)
            if (acBits != null) {
                mACMatrix[0][3] = acBits[0]
                mACMatrix[1][3] = acBits[1]
                mACMatrix[2][3] = acBits[2]
            }
            // Rebuild the data block dialog based on the readability of key B.
            mIsKeyBReadable = position < 2 || position == 4
            buildDataBlockDialog(true)
            // Close dialog.
            mSectorTrailerDialog?.dismiss()
        }
        mSectorTrailerDialog = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_choose_ac_title)
            .setView(lv)
            .create()

        // Build the dialog with Access Conditions for data blocks.
        // Key B is readable in the default configuration.
        mIsKeyBReadable = true
        buildDataBlockDialog(false)
    }

    /**
     * Convert the 3 Access Condition bytes into a more human readable format.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onDecode(view: View) {
        val ac = mAC.text.toString()
        if (ac.length != 6) {
            // Error. Access Conditions are not 3 byte (6 characters) long.
            Toast.makeText(this, R.string.info_ac_not_3_byte, Toast.LENGTH_LONG).show()
            return
        }
        if (!ac.matches(Regex("[0-9A-Fa-f]+"))) {
            // Error. Not hex.
            Toast.makeText(this, R.string.info_ac_not_hex, Toast.LENGTH_LONG).show()
            return
        }

        val acMatrix = Common.acBytesToACMatrix(Common.hex2Bytes(ac))
        var error = false
        if (acMatrix != null) {
            // First check & set Sector Trailer.
            val acBits = byteArrayOf(acMatrix[0][3], acMatrix[1][3], acMatrix[2][3])
            val rowNr = acBitsToACRowNr(acBits, true)
            if (rowNr != -1) {
                // Check if key B is readable.
                mIsKeyBReadable = rowNr < 2 || rowNr == 4
                mBlockButtons[3]?.text = getString(getResourceForSectorTrailersByRowNr(rowNr))

                // Now check & set Data blocks.
                for (i in 0 until 3) {
                    val blockAcBits = byteArrayOf(acMatrix[0][i], acMatrix[1][i], acMatrix[2][i])
                    val blockRowNr = acBitsToACRowNr(blockAcBits, false)
                    if (blockRowNr == -1) {
                        // Error.
                        error = true
                        break
                    }
                    mBlockButtons[i]?.text = getString(getResourceForDataBlocksByRowNr(blockRowNr))
                }
            } else {
                // Error.
                error = true
            }
        } else {
            // Error.
            error = true
        }

        // Were there some errors during this process?
        if (error) {
            // Display an error message.
            Toast.makeText(this, R.string.info_ac_format_error, Toast.LENGTH_LONG).show()
            return
        }
        mACMatrix = acMatrix!!
        buildDataBlockDialog(false)
    }

    /**
     * Convert the [mACMatrix] to 3 Access Condition bytes using
     * [Common.acMatrixToACBytes] and display them.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onEncode(view: View) {
        mAC.setText(Common.bytes2Hex(Common.acMatrixToACBytes(mACMatrix)))
    }

    /**
     * Backup the button which triggered this method to [mSelectedButton]
     * to change its text later and show the Access Condition chooser dialog
     * for data blocks ([mDataBlockDialog]).
     */
    fun onChooseACforDataBock(view: View) {
        mSelectedButton = view as Button
        mDataBlockDialog?.show()
    }

    /**
     * Show the Access Condition chooser dialog for Sector Trailers ([mSectorTrailerDialog]).
     */
    @Suppress("UNUSED_PARAMETER")
    fun onChooseACforSectorTrailer(view: View) {
        mSectorTrailerDialog?.show()
    }

    /**
     * Copy the MIFARE Classic Access Conditions to the Android clipboard.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onCopyToClipboard(view: View) {
        Common.copyToClipboard(mAC.text.toString(), this, true)
    }

    /**
     * Paste the content of the Android clipboard (if plain text) to the access conditions edit text.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onPasteFromClipboard(view: View) {
        val text = Common.getFromClipboard(this)
        if (text != null) {
            mAC.setText(text)
        }
    }

    /**
     * Return the resource ID of an Access Condition string for data blocks.
     */
    private fun getResourceForDataBlocksByRowNr(rowNr: Int): Int {
        var prefix = "ac_data_block_"
        if (mIsKeyBReadable) {
            prefix = "ac_data_block_no_keyb_"
        }
        return getResourceForAccessCondition(prefix, rowNr)
    }

    /**
     * Return the resource ID of an Access Condition string for Sector Trailers.
     */
    private fun getResourceForSectorTrailersByRowNr(rowNr: Int): Int {
        return getResourceForAccessCondition("ac_sector_trailer_", rowNr)
    }

    /**
     * A helper function for resource lookup.
     */
    private fun getResourceForAccessCondition(prefix: String, rowNr: Int): Int {
        return resources.getIdentifier(prefix + rowNr, "string", packageName)
    }

    /**
     * Convert the row number of the Access Condition table to its corresponding access bits C1, C2 and C3.
     */
    private fun acRowNrToACBits(rowNr: Int, isSectorTrailer: Boolean): ByteArray? {
        if (!isSectorTrailer && mIsKeyBReadable && rowNr > 1) {
            return when (rowNr) {
                2 -> byteArrayOf(0, 0, 1)
                3 -> byteArrayOf(1, 1, 1)
                else -> null
            }
        }

        return when (rowNr) {
            0 -> byteArrayOf(0, 0, 0)
            1 -> byteArrayOf(0, 1, 0)
            2 -> byteArrayOf(1, 0, 0)
            3 -> byteArrayOf(1, 1, 0)
            4 -> byteArrayOf(0, 0, 1)
            5 -> byteArrayOf(0, 1, 1)
            6 -> byteArrayOf(1, 0, 1)
            7 -> byteArrayOf(1, 1, 1)
            else -> null
        }
    }

    /**
     * Convert the access bits C1, C2 and C3 to its corresponding row number in the Access Condition table.
     */
    private fun acBitsToACRowNr(acBits: ByteArray?, isSectorTrailer: Boolean): Int {
        if (acBits == null || acBits.size != 3) {
            return -1
        }

        val b0 = acBits[0].toInt()
        val b1 = acBits[1].toInt()
        val b2 = acBits[2].toInt()

        if (!isSectorTrailer && mIsKeyBReadable) {
            if (b0 == 0 && b1 == 0 && b2 == 0) {
                return 0
            } else if (b0 == 0 && b1 == 1 && b2 == 0) {
                return 1
            } else if (b0 == 0 && b1 == 0 && b2 == 1) {
                return 2
            } else if (b0 == 1 && b1 == 1 && b2 == 1) {
                return 3
            }
        } else {
            if (b0 == 0 && b1 == 0 && b2 == 0) {
                return 0
            } else if (b0 == 0 && b1 == 1 && b2 == 0) {
                return 1
            } else if (b0 == 1 && b1 == 0 && b2 == 0) {
                return 2
            } else if (b0 == 1 && b1 == 1 && b2 == 0) {
                return 3
            } else if (b0 == 0 && b1 == 0 && b2 == 1) {
                return 4
            } else if (b0 == 0 && b1 == 1 && b2 == 1) {
                return 5
            } else if (b0 == 1 && b1 == 0 && b2 == 1) {
                return 6
            } else if (b0 == 1 && b1 == 1 && b2 == 1) {
                return 7
            }
        }

        return -1
    }

    /**
     * Rebuild the [mDataBlockDialog] based on [mIsKeyBReadable].
     */
    private fun buildDataBlockDialog(resetBlockACs: Boolean) {
        val items: Array<String>
        if (mIsKeyBReadable && !mWasKeyBReadable) {
            // Rebuild dialog (because key B is now readable).
            items = Array(4) { i -> getString(getResourceForDataBlocksByRowNr(i)) }
            mWasKeyBReadable = true
        } else if (!mIsKeyBReadable && mWasKeyBReadable) {
            // Rebuild dialog (because key B is no longer readable).
            items = Array(8) { i -> getString(getResourceForDataBlocksByRowNr(i)) }
            mWasKeyBReadable = false
        } else {
            // No build is needed.
            return
        }

        if (resetBlockACs) {
            // Reset mACMatrix and update button text.
            for (i in 0 until 3) {
                mBlockButtons[i]?.text = items[0]

                mACMatrix[0][i] = 0
                mACMatrix[1][i] = 0
                mACMatrix[2][i] = 0
            }
            val r = if (mIsKeyBReadable) {
                R.string.info_ac_reset_keyb_readable
            } else {
                R.string.info_ac_reset_keyb_not_readable
            }
            Toast.makeText(this, r, Toast.LENGTH_LONG).show()
        }

        val adapter: ListAdapter = ArrayAdapter(this, R.layout.list_item_small_text, items)
        val lv = ListView(this)
        lv.adapter = adapter
        lv.setOnItemClickListener { _, _, position, _ ->
            // Change button text to selected Access Conditions.
            mSelectedButton?.text = getString(getResourceForDataBlocksByRowNr(position))
            // Set Access Condition bits for this block.
            val acBits = acRowNrToACBits(position, false)
            val selectedBtn = mSelectedButton
            if (selectedBtn != null && acBits != null) {
                val blockNr = selectedBtn.tag.toString().toInt()
                mACMatrix[0][blockNr] = acBits[0]
                mACMatrix[1][blockNr] = acBits[1]
                mACMatrix[2][blockNr] = acBits[2]
            }
            // Close dialog.
            mDataBlockDialog?.dismiss()
        }
        mDataBlockDialog = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_choose_ac_title)
            .setView(lv)
            .create()
    }
}
