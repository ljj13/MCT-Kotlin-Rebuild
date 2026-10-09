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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.SparseArray
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.MCDiffUtils
import de.syss.MifareClassicTool.R
import java.io.File

/**
 * A tool to show the difference between two dumps.
 * @author Gerhard Klostermeier
 */
class DiffTool : BasicActivity() {

    companion object {
        /**
         * The corresponding Intent will contain a dump. Each field of the
         * String Array is one line of the dump. Headers (e.g. "Sector:1")
         * are marked with a "+"-symbol (e.g. "+Sector: 1").
         */
        const val EXTRA_DUMP: String = "de.syss.MifareClassicTool.Activity.DUMP"

        private const val FILE_CHOOSER_DUMP_FILE_1: Int = 1
        private const val FILE_CHOOSER_DUMP_FILE_2: Int = 2

        /**
         * Convert the format of a dump.
         */
        @JvmStatic
        private fun convertDumpFormat(dump: Array<String>): SparseArray<Array<String>> {
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

    private lateinit var mDiffContent: LinearLayout
    private lateinit var mDumpFileButton1: Button
    private lateinit var mDumpFileButton2: Button
    private lateinit var mDumpHideIdentical: CheckBox
    private var mDump1: SparseArray<Array<String>>? = null
    private var mDump2: SparseArray<Array<String>>? = null

    /**
     * Process [EXTRA_DUMP] if they are part of the Intent and initialize some member variables.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diff_tool)

        mDiffContent = findViewById(R.id.linearLayoutDiffTool)
        mDumpFileButton1 = findViewById(R.id.buttonDiffToolDump1)
        mDumpFileButton2 = findViewById(R.id.buttonDiffToolDump2)
        mDumpHideIdentical = findViewById(R.id.checkBoxDiffToolHideIdentical)

        // Check if one or both dumps are already chosen via Intent (from DumpEditor).
        if (intent.hasExtra(EXTRA_DUMP)) {
            val dumpExtra = intent.getStringArrayExtra(EXTRA_DUMP)
            if (dumpExtra != null) {
                mDump1 = convertDumpFormat(dumpExtra)
            }
            mDumpFileButton1.setText(R.string.text_dump_from_editor)
            mDumpFileButton1.isEnabled = false
            onChooseDump2(null)
        }
        runDiff()
    }

    /**
     * Handle the [FileChooser] results.
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            FILE_CHOOSER_DUMP_FILE_1 -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val fileName = data.getStringExtra(FileChooser.EXTRA_CHOSEN_FILENAME)
                    mDumpFileButton1.text = fileName
                    mDump1 = processChosenDump(data)
                    runDiff()
                }
            }
            FILE_CHOOSER_DUMP_FILE_2 -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val fileName = data.getStringExtra(FileChooser.EXTRA_CHOSEN_FILENAME)
                    mDumpFileButton2.text = fileName
                    mDump2 = processChosenDump(data)
                    runDiff()
                }
            }
        }
    }

    /**
     * Run diff if there are two dumps and show the result in the GUI.
     */
    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun runDiff() {
        val dump1 = mDump1
        val dump2 = mDump2
        if (dump1 != null && dump2 != null) {
            mDiffContent.removeAllViews()
            val diff = MCDiffUtils.diffIndices(dump1, dump2)

            // Add the difference between dumps.
            var totalLength = 0
            var diffLength = 0
            val difference = TextView(this)
            difference.setPadding(0, Common.dpToPx(10), 0, 0)
            mDiffContent.addView(difference)

            // Walk through all possible sectors.
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
                }

                // If the Hide Identical Sectors option is checked, do not display identical sector.
                if (mDumpHideIdentical.isChecked && blocks.size > 1) {
                    var block = 0
                    while (block < blocks.size) {
                        if (blocks[block].isNotEmpty()) {
                            break
                        }
                        block++
                    }
                    if (block == blocks.size) {
                        val bs = dump1.get(sector)
                        if (bs != null) {
                            for (b in bs) {
                                totalLength += b.length
                            }
                        }
                        continue
                    }
                }

                // Add sector header.
                val header = TextView(this)
                TextViewCompat.setTextAppearance(header, android.R.style.TextAppearance_Medium)
                header.setPadding(0, Common.dpToPx(20), 0, 0)
                header.setTextColor(Common.getThemeAccentColor(this))
                header.text = getString(R.string.text_sector) + ": " + sector
                mDiffContent.addView(header)

                if (blocks.isEmpty() || blocks.size == 1) {
                    val tv = TextView(this)
                    if (blocks.isEmpty()) {
                        tv.setText(R.string.text_only_in_dump1)
                    } else {
                        tv.setText(R.string.text_only_in_dump2)
                    }
                    mDiffContent.addView(tv)
                    continue
                }

                // Walk through all blocks.
                for (block in blocks.indices) {
                    val rl = layoutInflater.inflate(
                        R.layout.list_item_diff_block,
                        findViewById(android.R.id.content), false
                    ) as RelativeLayout
                    val tvDump1 = rl.findViewById<TextView>(R.id.textViewDiffBlockDump1)
                    val tvDump2 = rl.findViewById<TextView>(R.id.textViewDiffBlockDump2)
                    val diffIndex = rl.findViewById<TextView>(R.id.textViewDiffBlockDiff)

                    tvDump1.typeface = Typeface.MONOSPACE
                    tvDump2.typeface = Typeface.MONOSPACE
                    diffIndex.typeface = Typeface.MONOSPACE

                    val diffString: StringBuilder
                    diffIndex.setTextColor(Color.RED)
                    tvDump1.text = dump1.get(sector)[block]
                    tvDump2.text = dump2.get(sector)[block]

                    totalLength += dump1.get(sector)[block].length

                    if (blocks[block].isEmpty()) {
                        diffIndex.setTextColor(Color.GREEN)
                        diffString = StringBuilder("--------------------------------")
                    } else {
                        diffString = StringBuilder("                                ")
                        for (i in blocks[block]) {
                            diffString.setCharAt(i, 'X')
                            diffLength++
                        }
                    }
                    diffIndex.text = diffString
                    mDiffContent.addView(rl)
                }
            }

            var percentage = 0f
            if (totalLength > 0) {
                percentage = diffLength.toFloat() / totalLength * 100f
            }
            difference.text = getString(R.string.text_difference_between_dumps) +
                    ": " + String.format("%.2f", percentage) + " %"
        }
    }

    /**
     * Open [FileChooser] to select the first dump.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onChooseDump1(view: View?) {
        val intent = prepareFileChooserForDump()
        @Suppress("DEPRECATION")
        startActivityForResult(intent, FILE_CHOOSER_DUMP_FILE_1)
    }

    /**
     * Open [FileChooser] to select the second dump.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onChooseDump2(view: View?) {
        val intent = prepareFileChooserForDump()
        @Suppress("DEPRECATION")
        startActivityForResult(intent, FILE_CHOOSER_DUMP_FILE_2)
    }

    /**
     * Run diff if the Hide Identical Sectors option is changed.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onHideIdenticalChanged(view: View) {
        runDiff()
    }

    /**
     * Helper function to process chosen dump.
     */
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

    /**
     * Create an Intent for opening [FileChooser].
     */
    private fun prepareFileChooserForDump(): Intent {
        val intent = Intent(this, FileChooser::class.java)
        intent.putExtra(FileChooser.EXTRA_DIR, Common.getFile(Common.DUMPS_DIR).absolutePath)
        intent.putExtra(FileChooser.EXTRA_TITLE, getString(R.string.text_open_dump_title))
        intent.putExtra(FileChooser.EXTRA_BUTTON_TEXT, getString(R.string.action_open_dump_file))
        return intent
    }
}
