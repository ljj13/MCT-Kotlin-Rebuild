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
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.SparseArray
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.MCReader
import de.syss.MifareClassicTool.R
import java.util.ArrayList
import java.util.Collections

/**
 * Create a key map with the [KeyMapCreator] and then read the tag.
 * @author Gerhard Klostermeier
 */
class ReadTag : AppCompatActivity() {

    companion object {
        private const val KEY_MAP_CREATOR = 1
    }

    private val mHandler = Handler(Looper.getMainLooper())
    private var mRawDump: SparseArray<Array<String>>? = null

    /**
     * Show the [KeyMapCreator].
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_tag)

        val intent = Intent(this, KeyMapCreator::class.java)
        intent.putExtra(KeyMapCreator.EXTRA_KEYS_DIR, Common.getFile(Common.KEYS_DIR).absolutePath)
        intent.putExtra(KeyMapCreator.EXTRA_BUTTON_TEXT, getString(R.string.action_create_key_map_and_read))
        @Suppress("DEPRECATION")
        startActivityForResult(intent, KEY_MAP_CREATOR)
    }

    /**
     * Checks the result code of the key mapping process.
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == KEY_MAP_CREATOR) {
            if (resultCode != Activity.RESULT_OK) {
                if (resultCode == 4) {
                    Toast.makeText(this, R.string.info_strange_error, Toast.LENGTH_LONG).show()
                }
                finish()
                return
            } else {
                readTag()
            }
        }
    }

    /**
     * Start a worker thread to read tag.
     */
    private fun readTag() {
        val reader = Common.checkForTagAndCreateReader(this) ?: return
        Thread {
            mRawDump = reader.readAsMuchAsPossible(Common.getKeyMap())
            reader.close()
            mHandler.post { createTagDump(mRawDump) }
        }.start()
    }

    /**
     * Create a tag dump in a format [DumpEditor] can read and start DumpEditor.
     */
    private fun createTagDump(rawDump: SparseArray<Array<String>>?) {
        val tmpDump = ArrayList<String>()
        if (rawDump != null) {
            if (rawDump.size() != 0) {
                for (i in Common.getKeyMapRangeFrom()..Common.getKeyMapRangeTo()) {
                    val value = rawDump.get(i)
                    tmpDump.add("+Sector: $i")
                    if (value != null) {
                        Collections.addAll(tmpDump, *value)
                    } else {
                        tmpDump.add("*No keys found or dead sector")
                    }
                }
                val dump = tmpDump.toTypedArray()
                val intent = Intent(this, DumpEditor::class.java)
                intent.putExtra(DumpEditor.EXTRA_DUMP, dump)
                startActivity(intent)
            } else {
                Toast.makeText(this, R.string.info_none_key_valid_for_reading, Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, R.string.info_tag_removed_while_reading, Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
