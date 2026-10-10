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

package de.syss.MifareClassicTool

import android.content.Context
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.MifareClassic
import android.nfc.tech.NfcA
import android.util.Log
import android.util.SparseArray
import android.widget.Toast
import de.syss.MifareClassicTool.Activities.Preferences.Preference
import de.syss.MifareClassicTool.Common.Operation
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.HashSet
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Provides functions to read/write/analyze a MIFARE Classic tag.
 * @author Gerhard Klostermeier
 */
class MCReader private constructor(tag: Tag) {

    companion object {
        private val LOG_TAG: String = MCReader::class.java.simpleName

        /**
         * Placeholder for not found keys.
         */
        const val NO_KEY: String = "------------"

        /**
         * Placeholder for unreadable blocks.
         */
        const val NO_DATA: String = "--------------------------------"

        /**
         * Default key of MIFARE Classic tags.
         */
        const val DEFAULT_KEY: String = "FFFFFFFFFFFF"

        /**
         * Get new instance of [MCReader].
         * If the tag is "null" or if it is not a MIFARE Classic tag, "null" will be returned.
         */
        @JvmStatic
        fun get(tag: Tag?): MCReader? {
            var mcr: MCReader? = null
            if (tag != null) {
                try {
                    mcr = MCReader(tag)
                    if (!mcr.isMifareClassic) {
                        return null
                    }
                } catch (ex: RuntimeException) {
                    return null
                }
            }
            return mcr
        }

        /**
         * Return the sector that contains a given block.
         */
        @JvmStatic
        fun blockToSector(blockIndex: Int): Int {
            if (blockIndex < 0 || blockIndex >= 256) {
                throw IndexOutOfBoundsException("Block out of bounds: $blockIndex")
            }
            return if (blockIndex < 32 * 4) {
                blockIndex / 4
            } else {
                32 + (blockIndex - 32 * 4) / 16
            }
        }
    }

    private val mMFC: MifareClassic? = try {
        MifareClassic.get(tag)
    } catch (e: Exception) {
        Log.e(
            LOG_TAG,
            "Could not create MIFARE Classic reader for the provided tag (even after patching it)."
        )
        throw e
    }

    private var mKeyMap: SparseArray<Array<ByteArray?>>? = SparseArray()
    private var mKeyMapStatus: Int = 0
    private var mLastSector: Int = -1
    private var mFirstSector: Int = 0
    private var mKeysWithOrder: ArrayList<String>? = null
    private var mHasAllZeroKey: Boolean = false
    private var cancelCreateKeyMap: Boolean = false

    fun cancelCreateKeyMap() {
        cancelCreateKeyMap = true
    }

    /**
     * Read as much as possible from the tag with the given key information.
     */
    fun readAsMuchAsPossible(keyMap: SparseArray<Array<ByteArray?>>?): SparseArray<Array<String>>? {
        val resultSparseArray: SparseArray<Array<String>>
        if (keyMap != null && keyMap.size() > 0) {
            resultSparseArray = SparseArray(keyMap.size())
            for (i in 0 until keyMap.size()) {
                val results = arrayOfNulls<Array<String>>(2)
                try {
                    val keyA = keyMap.valueAt(i)[0]
                    if (keyA != null) {
                        results[0] = readSector(keyMap.keyAt(i), keyA, false)
                    }
                    val keyB = keyMap.valueAt(i)[1]
                    if (keyB != null) {
                        results[1] = readSector(keyMap.keyAt(i), keyB, true)
                    }
                } catch (e: TagLostException) {
                    return null
                }
                if (results[0] != null || results[1] != null) {
                    val merged = mergeSectorData(results[0], results[1])
                    if (merged != null) {
                        resultSparseArray.put(keyMap.keyAt(i), merged)
                    }
                }
            }
            return resultSparseArray
        }
        return null
    }

    /**
     * Read as much as possible from the tag depending on mapping range and keys.
     */
    fun readAsMuchAsPossible(): SparseArray<Array<String>>? {
        mKeyMapStatus = sectorCount
        while (buildNextKeyMapPart() < sectorCount - 1);
        return readAsMuchAsPossible(mKeyMap)
    }

    /**
     * Read as much as possible from a sector with the given key.
     */
    @Throws(TagLostException::class)
    fun readSector(sectorIndex: Int, key: ByteArray, useAsKeyB: Boolean): Array<String>? {
        val mfc = mMFC ?: return null
        val auth = authenticate(sectorIndex, key, useAsKeyB)
        var ret: Array<String>? = null
        if (auth) {
            val blocks = ArrayList<String>()
            val firstBlock = mfc.sectorToBlock(sectorIndex)
            var lastBlock = firstBlock + 4
            if (mfc.size == MifareClassic.SIZE_4K && sectorIndex > 31) {
                lastBlock = firstBlock + 16
            }
            for (i in firstBlock until lastBlock) {
                try {
                    var blockBytes = mfc.readBlock(i)
                    if (blockBytes.size < 16) {
                        throw IOException()
                    }
                    if (blockBytes.size > 16) {
                        blockBytes = Arrays.copyOf(blockBytes, 16)
                    }
                    blocks.add(Common.bytes2Hex(blockBytes))
                } catch (e: TagLostException) {
                    throw e
                } catch (e: IOException) {
                    Log.d(LOG_TAG, "(Recoverable) Error while reading block $i from tag.")
                    blocks.add(NO_DATA)
                    if (!isConnected) {
                        throw TagLostException("Tag removed during readSector(...)")
                    }
                    authenticate(sectorIndex, key, useAsKeyB)
                }
            }
            ret = blocks.toTypedArray()
            val last = ret.size - 1

            var noData = true
            for (s in ret) {
                if (s != NO_DATA) {
                    noData = false
                    break
                }
            }
            if (noData) {
                ret = null
            } else {
                if (!useAsKeyB) {
                    if (isKeyBReadable(Common.hex2Bytes(ret[last].substring(12, 20)))) {
                        ret[last] = Common.bytes2Hex(key) + ret[last].substring(12, 32)
                    } else {
                        ret[last] = Common.bytes2Hex(key) + ret[last].substring(12, 20) + NO_KEY
                    }
                } else {
                    ret[last] = NO_KEY + ret[last].substring(12, 20) + Common.bytes2Hex(key)
                }
            }
        }
        return ret
    }

    /**
     * Write a block of 16 byte data to tag.
     */
    fun writeBlock(
        sectorIndex: Int, blockIndex: Int, data: ByteArray,
        key: ByteArray, useAsKeyB: Boolean
    ): Int {
        val mfc = mMFC ?: return -1
        if (sectorCount - 1 < sectorIndex) {
            return 1
        }
        if (mfc.getBlockCountInSector(sectorIndex) - 1 < blockIndex) {
            return 2
        }
        if (data.size != 16) {
            return 3
        }
        if (!authenticate(sectorIndex, key, useAsKeyB)) {
            return 4
        }
        val block = mfc.sectorToBlock(sectorIndex) + blockIndex
        try {
            mfc.writeBlock(block, data)
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Error while writing block to tag.", e)
            return -1
        }
        return 0
    }

    /**
     * Write block 0 of a gen3 card using an APDU (no authentication needed).
     */
    fun writeBlock0Gen3(data: ByteArray): Int {
        if (data.size != 16) {
            return 1
        }
        val mfc = mMFC ?: return -1
        val writeCommand = byteArrayOf(0x90.toByte(), 0xF0.toByte(), 0xCC.toByte(), 0xCC.toByte(), 0x10.toByte())
        val fullCommand = ByteArray(writeCommand.size + data.size)
        System.arraycopy(writeCommand, 0, fullCommand, 0, writeCommand.size)
        System.arraycopy(data, 0, fullCommand, writeCommand.size, data.size)
        try {
            val gen3Tag = NfcA.get(mfc.tag) ?: throw IOException("Tag is not IsoDep compatible.")
            mfc.close()
            gen3Tag.connect()
            gen3Tag.transceive(fullCommand)
            gen3Tag.close()
            mfc.connect()
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Error while writing block to tag.", e)
            return -1
        }
        return 0
    }

    /**
     * Increase or decrease a Value Block.
     */
    fun writeValueBlock(
        sectorIndex: Int, blockIndex: Int, value: Int,
        increment: Boolean, key: ByteArray, useAsKeyB: Boolean
    ): Int {
        val mfc = mMFC ?: return -1
        if (sectorCount - 1 < sectorIndex) {
            return 1
        }
        if (mfc.getBlockCountInSector(sectorIndex) - 1 < blockIndex) {
            return 2
        }
        if (!authenticate(sectorIndex, key, useAsKeyB)) {
            return 3
        }
        val block = mfc.sectorToBlock(sectorIndex) + blockIndex
        try {
            if (increment) {
                mfc.increment(block, value)
            } else {
                mfc.decrement(block, value)
            }
            mfc.transfer(block)
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Error while writing Value Block to tag.", e)
            return -1
        }
        return 0
    }

    /**
     * Build Key-Value Pairs in which keys represent the sector and values are keys (A/B).
     */
    fun buildNextKeyMapPart(): Int {
        val mfc = mMFC ?: return -1
        var error = false
        cancelCreateKeyMap = false
        val keysWithOrder = mKeysWithOrder
        if (keysWithOrder != null && mLastSector != -1) {
            if (mKeyMapStatus == mLastSector + 1) {
                mKeyMapStatus = mFirstSector
                mKeyMap = SparseArray()
            }

            val autoReconnect = Common.getPreferences().getBoolean(
                Preference.AutoReconnect.toString(), false
            )
            val retryAuth = Common.getPreferences().getBoolean(
                Preference.UseRetryAuthentication.toString(), false
            )
            val retryAuthCount = Common.getPreferences().getInt(
                Preference.RetryAuthenticationCount.toString(), 1
            )

            val keys = arrayOfNulls<String>(2)
            val foundKeys = booleanArrayOf(false, false)
            var auth: Boolean

            var i = 0
            keysloop@ while (i < keysWithOrder.size) {
                val key = keysWithOrder[i]
                val bytesKey = Common.hex2Bytes(key)
                if (bytesKey != null) {
                    var j = 0
                    while (j < retryAuthCount + 1) {
                        try {
                            if (cancelCreateKeyMap) {
                                return -1
                            }
                            if (!foundKeys[0]) {
                                auth = mfc.authenticateSectorWithKeyA(mKeyMapStatus, bytesKey)
                                if (auth) {
                                    keys[0] = key
                                    foundKeys[0] = true
                                }
                            }
                            if (!foundKeys[1]) {
                                auth = mfc.authenticateSectorWithKeyB(mKeyMapStatus, bytesKey)
                                if (auth) {
                                    keys[1] = key
                                    foundKeys[1] = true
                                }
                            }
                        } catch (e: Exception) {
                            Log.d(LOG_TAG, "Error while building next key map part")
                            if (autoReconnect) {
                                if (isConnectedButTagLost) {
                                    close()
                                }
                                while (!isConnected) {
                                    try {
                                        Thread.sleep(500)
                                    } catch (ex: InterruptedException) {
                                    }
                                    try {
                                        connect()
                                    } catch (ex: Exception) {
                                    }
                                }
                                continue
                            } else {
                                error = true
                                break@keysloop
                            }
                        }
                        if ((foundKeys[0] && foundKeys[1]) || !retryAuth) {
                            break
                        }
                        j++
                    }
                }
                if (foundKeys[0] && foundKeys[1]) {
                    break
                }
                i++
            }

            if (!error && (foundKeys[0] || foundKeys[1])) {
                val bytesKeys = arrayOfNulls<ByteArray>(2)
                bytesKeys[0] = Common.hex2Bytes(keys[0])
                bytesKeys[1] = Common.hex2Bytes(keys[1])
                @Suppress("UNCHECKED_CAST")
                mKeyMap?.put(mKeyMapStatus, bytesKeys as Array<ByteArray?>)
                if (keysWithOrder.size > 2) {
                    if (foundKeys[0] && keys[0] != null) {
                        keysWithOrder.remove(keys[0])
                        if (mHasAllZeroKey && keys[0] != DEFAULT_KEY) {
                            keysWithOrder.add(1, keys[0]!!)
                        } else {
                            keysWithOrder.add(0, keys[0]!!)
                        }
                    }
                    if (foundKeys[1] && keys[1] != null) {
                        keysWithOrder.remove(keys[1])
                        if (mHasAllZeroKey && keys[1] != DEFAULT_KEY) {
                            keysWithOrder.add(1, keys[1]!!)
                        } else {
                            keysWithOrder.add(0, keys[1]!!)
                        }
                    }
                }
            }
            mKeyMapStatus++
        } else {
            error = true
        }

        if (error) {
            mKeyMapStatus = 0
            mKeyMap = null
            return -1
        }
        return mKeyMapStatus - 1
    }

    /**
     * Merge the result of two readSector calls on the same sector.
     */
    fun mergeSectorData(firstResult: Array<String>?, secondResult: Array<String>?): Array<String>? {
        var ret: Array<String>? = null
        if (firstResult != null || secondResult != null) {
            if (firstResult != null && secondResult != null && firstResult.size != secondResult.size) {
                return null
            }
            val length = firstResult?.size ?: secondResult!!.size
            val blocks = ArrayList<String>()
            for (i in 0 until length - 1) {
                if (firstResult != null && firstResult[i] != NO_DATA) {
                    blocks.add(firstResult[i])
                } else if (secondResult != null && secondResult[i] != NO_DATA) {
                    blocks.add(secondResult[i])
                } else {
                    blocks.add(NO_DATA)
                }
            }
            val retList = ArrayList<String>(blocks)
            val last = length - 1
            if (firstResult != null && firstResult[last] != NO_DATA) {
                var sectorTrailer = firstResult[last]
                if (secondResult != null && secondResult[last] != NO_DATA) {
                    sectorTrailer = sectorTrailer.substring(0, 20) + secondResult[last].substring(20)
                }
                retList.add(sectorTrailer)
            } else if (secondResult != null && secondResult[last] != NO_DATA) {
                retList.add(secondResult[last])
            } else {
                retList.add(NO_DATA)
            }
            ret = retList.toTypedArray()
        }
        return ret
    }

    /**
     * Check if the present tag is writable with the provided keys at the given positions.
     */
    fun isWritableOnPositions(
        pos: HashMap<Int, IntArray>,
        keyMap: SparseArray<Array<ByteArray?>>
    ): HashMap<Int, HashMap<Int, Int>?>? {
        val mfc = mMFC ?: return null
        val ret = HashMap<Int, HashMap<Int, Int>?>()
        for (i in 0 until keyMap.size()) {
            val sector = keyMap.keyAt(i)
            if (pos.containsKey(sector)) {
                val keys = keyMap.get(sector)
                val acBlockBytes: ByteArray
                if (keys[0] != null) {
                    if (!authenticate(sector, keys[0], false)) {
                        return null
                    }
                } else if (keys[1] != null) {
                    if (!authenticate(sector, keys[1], true)) {
                        return null
                    }
                } else {
                    return null
                }

                val acBlock = mfc.sectorToBlock(sector) + mfc.getBlockCountInSector(sector) - 1
                try {
                    acBlockBytes = mfc.readBlock(acBlock)
                } catch (e: Exception) {
                    ret[sector] = null
                    continue
                }
                if (acBlockBytes.size < 16) {
                    ret[sector] = null
                    continue
                }

                val ac = Arrays.copyOfRange(acBlockBytes, 6, 9)
                val acMatrix = Common.acBytesToACMatrix(ac)
                if (acMatrix == null) {
                    ret[sector] = null
                    continue
                }
                val isKeyBReadable = Common.isKeyBReadable(
                    acMatrix[0][3], acMatrix[1][3], acMatrix[2][3]
                )

                val blockWithWriteInfo = HashMap<Int, Int>()
                val blocksToCheck = pos[sector]
                if (blocksToCheck != null) {
                    for (block in blocksToCheck) {
                        if ((block == 3 && sector <= 31) || (block == 15 && sector >= 32)) {
                            val acValue = Common.getOperationRequirements(
                                acMatrix[0][3], acMatrix[1][3], acMatrix[2][3],
                                Operation.WriteAC, true, isKeyBReadable
                            )
                            val keyABValue = Common.getOperationRequirements(
                                acMatrix[0][3], acMatrix[1][3], acMatrix[2][3],
                                Operation.WriteKeyA, true, isKeyBReadable
                            )
                            var result = keyABValue
                            if (acValue == 0 && keyABValue != 0) {
                                result += 3
                            } else if (acValue == 2 && keyABValue == 0) {
                                result = 6
                            }
                            blockWithWriteInfo[block] = result
                        } else {
                            var acBitsForBlock = block
                            if (sector >= 32) {
                                if (block in 0..4) {
                                    acBitsForBlock = 0
                                } else if (block in 5..9) {
                                    acBitsForBlock = 1
                                } else if (block in 10..14) {
                                    acBitsForBlock = 2
                                }
                            }
                            blockWithWriteInfo[block] = Common.getOperationRequirements(
                                acMatrix[0][acBitsForBlock],
                                acMatrix[1][acBitsForBlock],
                                acMatrix[2][acBitsForBlock],
                                Operation.Write, false, isKeyBReadable
                            )
                        }
                    }
                }
                if (blockWithWriteInfo.isNotEmpty()) {
                    ret[sector] = blockWithWriteInfo
                }
            }
        }
        return ret
    }

    /**
     * Check if the provided key has decrement/transfer/restore permission.
     */
    fun hasDecTransRestPermission(sector: Int, block: Int, key: ByteArray?, useAsKeyB: Boolean): Int {
        if ((block == 3 && sector <= 31) || (block == 15 && sector >= 32)) {
            return 0
        }
        val mfc = mMFC ?: return -1
        if (key == null || !authenticate(sector, key, useAsKeyB)) {
            return -1
        }
        val acBlock = mfc.sectorToBlock(sector) + mfc.getBlockCountInSector(sector) - 1
        val acBlockBytes: ByteArray
        try {
            acBlockBytes = mfc.readBlock(acBlock)
        } catch (e: Exception) {
            return -1
        }
        if (acBlockBytes.size < 16) {
            return -1
        }
        val ac = Arrays.copyOfRange(acBlockBytes, 6, 9)
        val acMatrix = Common.acBytesToACMatrix(ac) ?: return -1
        val isKeyBReadable = Common.isKeyBReadable(
            acMatrix[0][3], acMatrix[1][3], acMatrix[2][3]
        )

        var acBitsForBlock = block
        if (sector >= 32) {
            if (block in 0..4) {
                acBitsForBlock = 0
            } else if (block in 5..9) {
                acBitsForBlock = 1
            } else if (block in 10..14) {
                acBitsForBlock = 2
            }
        }

        return Common.getOperationRequirements(
            acMatrix[0][acBitsForBlock],
            acMatrix[1][acBitsForBlock],
            acMatrix[2][acBitsForBlock],
            Operation.DecTransRest, false, isKeyBReadable
        )
    }

    /**
     * Set key files for [buildNextKeyMapPart].
     */
    fun setKeyFile(keyFiles: Array<File>?, context: Context?): Int {
        if (keyFiles == null || keyFiles.isEmpty() || context == null) {
            return -1
        }
        val keys = HashSet<String>()
        for (file in keyFiles) {
            val lines = Common.readFileLineByLine(file, false, context)
            if (lines != null) {
                for (line in lines) {
                    if (line.isNotEmpty() && line.length == 12 && line.matches(Regex("[0-9A-Fa-f]+"))) {
                        try {
                            keys.add(line)
                        } catch (e: OutOfMemoryError) {
                            Toast.makeText(context, R.string.info_too_many_keys, Toast.LENGTH_LONG).show()
                            return -1
                        }
                    }
                }
            }
        }
        if (keys.isNotEmpty()) {
            mHasAllZeroKey = keys.contains("000000000000")
            mKeysWithOrder = ArrayList(keys)
            if (mHasAllZeroKey) {
                mKeysWithOrder?.remove(DEFAULT_KEY)
                mKeysWithOrder?.add(0, DEFAULT_KEY)
            }
            return keys.size
        }
        return 0
    }

    /**
     * Set mapping range for [buildNextKeyMapPart].
     */
    fun setMappingRange(firstSector: Int, lastSector: Int): Boolean {
        if (firstSector >= 0 && lastSector < sectorCount && firstSector <= lastSector) {
            mFirstSector = firstSector
            mLastSector = lastSector
            mKeyMapStatus = lastSector + 1
            return true
        }
        return false
    }

    /**
     * Value block transfer restore logic.
     */
    fun valueTransferRestore(
        stageSector: Int, stageBlock: Int, destSector: Int, destBlock: Int,
        valueBlock16: ByteArray,
        keyStage: ByteArray, useKeyBStage: Boolean,
        keyDest: ByteArray, useKeyBDest: Boolean
    ): Int {
        val mfc = mMFC ?: return -1
        try {
            if (!isConnected) {
                mfc.connect()
            }

            val stageBlocks = mfc.getBlockCountInSector(stageSector)
            val destBlocks = mfc.getBlockCountInSector(destSector)
            if (stageBlock < 0 || stageBlock >= stageBlocks) return 2
            if (destBlock < 0 || destBlock >= destBlocks) return 2

            if (stageBlock == stageBlocks - 1 || (stageSector == 0 && stageBlock == 0)) return 1
            if (destBlock == destBlocks - 1 || (destSector == 0 && destBlock == 0)) return 1

            val stageAbs = mfc.sectorToBlock(stageSector) + stageBlock
            val destAbs = mfc.sectorToBlock(destSector) + destBlock

            var ok = if (useKeyBStage) {
                mfc.authenticateSectorWithKeyB(stageSector, keyStage)
            } else {
                mfc.authenticateSectorWithKeyA(stageSector, keyStage)
            }
            if (!ok) return -1

            val original = mfc.readBlock(stageAbs)
            mfc.writeBlock(stageAbs, valueBlock16)
            mfc.restore(stageAbs)

            ok = if (useKeyBDest) {
                mfc.authenticateSectorWithKeyB(destSector, keyDest)
            } else {
                mfc.authenticateSectorWithKeyA(destSector, keyDest)
            }
            if (!ok) return -1

            mfc.transfer(destAbs)

            if (stageSector != destSector) {
                ok = if (useKeyBStage) {
                    mfc.authenticateSectorWithKeyB(stageSector, keyStage)
                } else {
                    mfc.authenticateSectorWithKeyA(stageSector, keyStage)
                }
                if (!ok) return -1
            }
            mfc.writeBlock(stageAbs, original)

            return 0
        } catch (e: Exception) {
            return -1
        } finally {
            try {
                mfc.close()
            } catch (ignored: Exception) {
            }
        }
    }

    private fun authenticate(sectorIndex: Int, key: ByteArray?, useAsKeyB: Boolean): Boolean {
        val mfc = mMFC ?: return false
        val retryAuth = Common.getPreferences().getBoolean(
            Preference.UseRetryAuthentication.toString(), false
        )
        val retryCount = Common.getPreferences().getInt(
            Preference.RetryAuthenticationCount.toString(), 1
        )
        if (key == null) {
            return false
        }
        var ret = false
        for (i in 0 until retryCount + 1) {
            try {
                ret = if (!useAsKeyB) {
                    mfc.authenticateSectorWithKeyA(sectorIndex, key)
                } else {
                    mfc.authenticateSectorWithKeyB(sectorIndex, key)
                }
            } catch (e: Exception) {
                when (e) {
                    is IOException, is ArrayIndexOutOfBoundsException, is SecurityException -> {
                        Log.d(LOG_TAG, "Error authenticating with tag.")
                        return false
                    }
                    else -> throw e
                }
            }
            if (ret || !retryAuth) {
                break
            }
        }
        return ret
    }

    private fun isKeyBReadable(ac: ByteArray?): Boolean {
        if (ac == null || ac.size < 3) {
            return false
        }
        val c1 = ((ac[1].toInt() and 0x80) ushr 7).toByte()
        val c2 = ((ac[2].toInt() and 0x08) ushr 3).toByte()
        val c3 = ((ac[2].toInt() and 0x80) ushr 7).toByte()
        val b1 = c1.toInt()
        val b2 = c2.toInt()
        val b3 = c3.toInt()
        return b1 == 0 && (b2 == 0 && b3 == 0 || b2 == 1 && b3 == 0 || b2 == 0 && b3 == 1)
    }

    fun getKeyMap(): SparseArray<Array<ByteArray?>>? {
        return mKeyMap
    }

    val isMifareClassic: Boolean
        get() = mMFC != null

    val size: Int
        get() = mMFC?.size ?: 0

    val sectorCount: Int
        get() {
            val useCustomSectorCount = Common.getPreferences().getBoolean(
                Preference.UseCustomSectorCount.toString(), false
            )
            return if (useCustomSectorCount) {
                Common.getPreferences().getInt(
                    Preference.CustomSectorCount.toString(), 16
                )
            } else {
                mMFC?.sectorCount ?: 0
            }
        }

    val blockCount: Int
        get() = mMFC?.blockCount ?: 0

    fun getBlockCountInSector(sectorIndex: Int): Int {
        return mMFC?.getBlockCountInSector(sectorIndex) ?: 0
    }

    val isConnected: Boolean
        get() {
            var connected = false
            try {
                connected = mMFC?.isConnected == true
            } catch (ex: RuntimeException) {
                connected = false
            }
            return connected
        }

    val isConnectedButTagLost: Boolean
        get() {
            if (isConnected) {
                try {
                    mMFC?.readBlock(0)
                } catch (e: IOException) {
                    return true
                }
            }
            return false
        }

    @Throws(Exception::class)
    fun connect() {
        val error = AtomicBoolean(false)
        val mfc = mMFC
        if (mfc == null || mfc.tag == null) {
            error.set(true)
        }

        if (!error.get() && isConnected) {
            return
        }

        if (!error.get()) {
            val t = Thread {
                try {
                    mfc?.connect()
                } catch (ex: Exception) {
                    when (ex) {
                        is IOException, is RuntimeException -> error.set(true)
                        else -> throw ex
                    }
                }
            }
            t.start()
            try {
                t.join(500)
            } catch (ex: InterruptedException) {
                error.set(true)
            }
        }

        if (error.get()) {
            Log.d(LOG_TAG, "Error while connecting to tag.")
            throw Exception("Error while connecting to tag.")
        }
    }

    fun close() {
        try {
            mMFC?.close()
        } catch (ex: Exception) {
            when (ex) {
                is IOException, is RuntimeException -> Log.d(LOG_TAG, "Error on closing tag.")
                else -> throw ex
            }
        }
    }
}
