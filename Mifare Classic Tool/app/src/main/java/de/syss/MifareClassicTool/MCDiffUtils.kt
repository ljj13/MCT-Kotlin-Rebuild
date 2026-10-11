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

package de.syss.MifareClassicTool

import android.util.SparseArray
import java.util.ArrayList

/**
 * Provides functions to compare two dumps.
 * @author Gerhard Klostermeier
 */
object MCDiffUtils {

    /**
     * Compare two dumps and get a list of all indices where
     * they differ from each other.
     * @param dump1 The first dump. The sector number is key and the
     * string array represents the blocks.
     * @param dump2 The second dump. The sector number is key and the
     * string array represents the blocks.
     * @return Indices where the two dumps differ. The key represents
     * the sector number. The first dimension of the value represents the
     * block number and the second is a list of indices where dump2 is
     * different from dump1. If the value is Integer[0][0] then the sector
     * exists only in dump1. If the value is Integer[1][0] then the sector
     * exists only in dump2.
     */
    @JvmStatic
    fun diffIndices(
        dump1: SparseArray<Array<String>>,
        dump2: SparseArray<Array<String>>
    ): SparseArray<Array<Array<Int>>> {
        val ret = SparseArray<Array<Array<Int>>>()
        // Walk through all sectors of dump1.
        for (i in 0 until dump1.size()) {
            val sector1 = dump1.valueAt(i)
            val sectorNr = dump1.keyAt(i)
            val sector2 = dump2.get(sectorNr)

            // Check if dump2 has the current sector of dump1.
            if (sector2 == null) {
                ret.put(sectorNr, Array(0) { emptyArray() })
                continue
            }

            // Check the blocks.
            val diffSector = arrayOfNulls<Array<Int>>(sector1.size)
            // Walk through all blocks.
            for (j in sector1.indices) {
                val diffIndices = ArrayList<Int>()
                // Walk through all symbols.
                for (k in 0 until sector1[j].length) {
                    if (sector1[j][k] != sector2[j][k]) {
                        // Found different symbol at index k.
                        diffIndices.add(k)
                    }
                }
                if (diffIndices.isEmpty()) {
                    // Block was identical.
                    diffSector[j] = emptyArray()
                } else {
                    diffSector[j] = diffIndices.toTypedArray()
                }
            }
            @Suppress("UNCHECKED_CAST")
            ret.put(sectorNr, diffSector as Array<Array<Int>>)
        }

        // Are there sectors that occur only in dump2?
        for (i in 0 until dump2.size()) {
            val sectorNr = dump2.keyAt(i)
            if (dump1.get(sectorNr) == null) {
                // Sector only exists in dump2.
                ret.put(sectorNr, Array(1) { emptyArray() })
            }
        }

        return ret
    }
}
