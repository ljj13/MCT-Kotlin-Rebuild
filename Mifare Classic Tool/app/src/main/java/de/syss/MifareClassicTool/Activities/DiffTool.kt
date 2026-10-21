/*
 * Copyright 2014 Gerhard Klostermeier
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
import android.content.Intent
import android.os.Bundle
import android.util.SparseArray
import android.view.View
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.MCDiffUtils
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.diff.DiffBlockItem
import de.syss.MifareClassicTool.ui.diff.DiffSectorItem
import de.syss.MifareClassicTool.ui.diff.DiffToolScreen
import de.syss.MifareClassicTool.ui.diff.DiffToolUiState
import de.syss.MifareClassicTool.ui.theme.MctTheme
import java.io.File
import java.util.Locale

/**
 * A tool to show the difference between two dumps.
 * Retains BasicActivity NFC foreground dispatch, MCDiffUtils algorithm,
 * Intent EXTRA_DUMP contract, and FileChooser navigation.
 * @author Gerhard Klostermeier
 */
class DiffTool : BasicActivity() {

    companion object {
        const val EXTRA_DUMP: String = "de.syss.MifareClassicTool.Activity.DUMP"
        private const val FILE_CHOOSER_DUMP_FILE_1: Int = 1
        private const val FILE_CHOOSER_DUMP_FILE_2: Int = 2

        @JvmStatic
        fun convertDumpFormat(dump: Array<String>): SparseArray<Array<String>> {
            val ret = SparseArray<Array<String>>()
            var i = 0
            var sector = 0
            for (line in dump) {
                if (line.startsWith("+")) {
                    val tmp = line.split(": ")
                    sector = tmp[tmp.size - 1].toInt()
                    i = 0
                    if (sector < 32) {
                        ret.put(sector, Array(4) { "" })
                    } else {
                        ret.put(sector, Array(16) { "" })
                    }
                } else {
                    ret.get(sector)[i++] = line
                }
            }
            return ret
        }
    }

    private var mDump1: SparseArray<Array<String>>? = null
    private var mDump2: SparseArray<Array<String>>? = null
    private var mDump1Name: String? = null
    private var mDump2Name: String? = null
    private var mIsDump1FromEditor: Boolean = false
    private var mHideIdentical: Boolean = false

    private val mUiState = mutableStateOf(DiffToolUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        // Check test sample extra (1k or 4k)
        val sampleType = intent.getStringExtra("extra_test_sample")
        if (sampleType != null) {
            setupSampleData(sampleType)
        } else if (intent.hasExtra(EXTRA_DUMP)) {
            val dumpExtra = intent.getStringArrayExtra(EXTRA_DUMP)
            if (dumpExtra != null) {
                mDump1 = convertDumpFormat(dumpExtra)
            }
            mIsDump1FromEditor = true
            mDump1Name = getString(R.string.text_dump_from_editor)
            onChooseDump2(null)
        }

        runDiff()

        setContent {
            MctTheme {
                DiffToolScreen(
                    state = mUiState.value,
                    onBackClick = { finish() },
                    onChooseDump1Click = { onChooseDump1(null) },
                    onChooseDump2Click = { onChooseDump2(null) },
                    onHideIdenticalChanged = { hide ->
                        mHideIdentical = hide
                        runDiff()
                    }
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FILE_CHOOSER_DUMP_FILE_1 -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    mDump1Name = data.getStringExtra(FileChooser.EXTRA_CHOSEN_FILENAME)
                    mDump1 = processChosenDump(data)
                    runDiff()
                }
            }
            FILE_CHOOSER_DUMP_FILE_2 -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    mDump2Name = data.getStringExtra(FileChooser.EXTRA_CHOSEN_FILENAME)
                    mDump2 = processChosenDump(data)
                    runDiff()
                }
            }
        }
    }

    private fun runDiff() {
        val dump1 = mDump1
        val dump2 = mDump2

        if (dump1 == null || dump2 == null) {
            mUiState.value = DiffToolUiState(
                dump1Name = mDump1Name,
                dump2Name = mDump2Name,
                isDump1FromEditor = mIsDump1FromEditor,
                hideIdenticalSectors = mHideIdentical,
                diffPercentageText = null,
                sectors = emptyList()
            )
            return
        }

        val diff = MCDiffUtils.diffIndices(dump1, dump2)
        var totalLength = 0
        var diffLength = 0
        val sectorList = mutableListOf<DiffSectorItem>()

        for (sector in 0 until 40) {
            val blocks = diff.get(sector) ?: continue

            // A sector that exists only in one dump is counted as differences.
            if (blocks.isEmpty() || blocks.size == 1) {
                val bs = if (blocks.isEmpty()) dump1.get(sector) else dump2.get(sector)
                if (bs != null) {
                    for (b in bs) {
                        totalLength += b.length
                        diffLength += b.length
                    }
                }

                val title = getString(R.string.text_sector) + ": $sector"
                val statusText = if (blocks.isEmpty()) {
                    getString(R.string.text_only_in_dump1)
                } else {
                    getString(R.string.text_only_in_dump2)
                }
                sectorList.add(
                    DiffSectorItem(
                        sectorNumber = sector,
                        title = title,
                        statusText = statusText,
                        isOnlyInDump1 = blocks.isEmpty(),
                        isOnlyInDump2 = (blocks.size == 1)
                    )
                )
                continue
            }

            // Check if all blocks in this sector are identical
            var isSectorIdentical = true
            var sectorDiffCount = 0
            for (blockDiff in blocks) {
                if (blockDiff.isNotEmpty()) {
                    isSectorIdentical = false
                    sectorDiffCount += blockDiff.size
                }
            }

            // If Hide Identical Sectors is checked and sector is identical, skip displaying it
            if (mHideIdentical && isSectorIdentical) {
                val bs = dump1.get(sector)
                if (bs != null) {
                    for (b in bs) {
                        totalLength += b.length
                    }
                }
                continue
            }

            val blockItems = mutableListOf<DiffBlockItem>()
            for (block in blocks.indices) {
                val d1Line = dump1.get(sector)?.getOrNull(block) ?: ""
                val d2Line = dump2.get(sector)?.getOrNull(block) ?: ""
                totalLength += d1Line.length

                val diffSet = blocks[block].toSet()
                diffLength += diffSet.size

                val blockHeader = if (sector >= 32) {
                    getString(R.string.text_block) + ": $block"
                } else {
                    getString(R.string.text_block) + ": $block"
                }

                blockItems.add(
                    DiffBlockItem(
                        blockIndex = block,
                        blockHeader = blockHeader,
                        dump1Line = d1Line,
                        dump2Line = d2Line,
                        diffIndices = diffSet,
                        isIdentical = diffSet.isEmpty()
                    )
                )
            }

            sectorList.add(
                DiffSectorItem(
                    sectorNumber = sector,
                    title = getString(R.string.text_sector) + ": $sector",
                    isIdentical = isSectorIdentical,
                    diffCount = sectorDiffCount,
                    blocks = blockItems
                )
            )
        }

        var percentage = 0f
        if (totalLength > 0) {
            percentage = diffLength.toFloat() / totalLength * 100f
        }
        val diffPercentageText = getString(R.string.text_difference_between_dumps) +
                ": " + String.format(Locale.US, "%.2f", percentage) + " %"

        mUiState.value = DiffToolUiState(
            dump1Name = mDump1Name,
            dump2Name = mDump2Name,
            isDump1FromEditor = mIsDump1FromEditor,
            hideIdenticalSectors = mHideIdentical,
            diffPercentageText = diffPercentageText,
            sectors = sectorList
        )
    }

    private fun setupSampleData(sampleType: String) {
        val count = if (sampleType.equals("4k", ignoreCase = true)) 40 else 16
        mDump1Name = "Sample-1.dump"
        mDump2Name = "Sample-2.dump"
        val d1 = SparseArray<Array<String>>()
        val d2 = SparseArray<Array<String>>()

        for (s in 0 until count) {
            val numBlocks = if (s < 32) 4 else 16
            val b1 = Array(numBlocks) { "00000000000000000000000000000000" }
            val b2 = Array(numBlocks) { "00000000000000000000000000000000" }

            // Sector 0 Block 0 has UID difference
            if (s == 0) {
                b1[0] = "0BDC504FC80804000102030405060708"
                b2[0] = "A1B2C3D49C0804000102030405060708"
            }
            // Sector 1 Block 1 has value difference
            if (s == 1) {
                b1[1] = "01000000FEFFFFFF0100000000FF00FF"
                b2[1] = "A701000058FEFFFFA701000001FE01FE"
            }
            // Sector trailers
            b1[numBlocks - 1] = "FFFFFFFFFFFFFF078069FFFFFFFFFFFF"
            b2[numBlocks - 1] = "FFFFFFFFFFFFFF078069FFFFFFFFFFFF"

            d1.put(s, b1)
            d2.put(s, b2)
        }

        mDump1 = d1
        mDump2 = d2
    }

    @Suppress("UNUSED_PARAMETER")
    fun onChooseDump1(view: View? = null) {
        val intent = prepareFileChooserForDump()
        @Suppress("DEPRECATION")
        startActivityForResult(intent, FILE_CHOOSER_DUMP_FILE_1)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onChooseDump2(view: View? = null) {
        val intent = prepareFileChooserForDump()
        @Suppress("DEPRECATION")
        startActivityForResult(intent, FILE_CHOOSER_DUMP_FILE_2)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onHideIdenticalChanged(view: View? = null) {
        mHideIdentical = !mHideIdentical
        runDiff()
    }

    private fun processChosenDump(data: Intent): SparseArray<Array<String>>? {
        val path = data.getStringExtra(FileChooser.EXTRA_CHOSEN_FILE) ?: return null
        val file = File(path)
        val dump = Common.readFileLineByLine(file, false, this)
        val err = Common.isValidDump(dump, false)
        return if (err != 0) {
            Common.isValidDumpErrorToast(err, this)
            null
        } else {
            convertDumpFormat(dump!!)
        }
    }

    private fun prepareFileChooserForDump(): Intent {
        val intent = Intent(this, FileChooser::class.java)
        intent.putExtra(FileChooser.EXTRA_DIR, Common.getFile(Common.DUMPS_DIR).absolutePath)
        intent.putExtra(FileChooser.EXTRA_TITLE, getString(R.string.text_open_dump_title))
        intent.putExtra(FileChooser.EXTRA_BUTTON_TEXT, getString(R.string.action_open_dump_file))
        return intent
    }
}
