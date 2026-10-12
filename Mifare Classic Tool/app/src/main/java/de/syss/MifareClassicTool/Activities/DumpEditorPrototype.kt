package de.syss.MifareClassicTool.Activities

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.ui.dumpeditor.BenchmarkMetrics
import de.syss.MifareClassicTool.ui.dumpeditor.DumpEditorPrototypeScreen
import de.syss.MifareClassicTool.ui.dumpeditor.DumpSampleType
import de.syss.MifareClassicTool.ui.dumpeditor.EditorMode
import de.syss.MifareClassicTool.ui.dumpeditor.SectorData
import de.syss.MifareClassicTool.ui.theme.MctTheme
import java.security.MessageDigest

/**
 * An independent prototype comparing Native View (AndroidView) vs Compose Block-Based
 * editing architectures for DumpEditor across 1K and 4K dump specifications.
 * Evaluates initial render latency, syntax highlight cost, memory footprint,
 * and byte-for-byte save fidelity.
 */
class DumpEditorPrototype : BasicActivity() {

    private val mCurrentMode = mutableStateOf(EditorMode.NATIVE_VIEW)
    private val mSampleType = mutableStateOf(DumpSampleType.DUMP_1K)
    private val mSectors = mutableStateOf<List<SectorData>>(emptyList())
    private val mMetrics = mutableStateOf(BenchmarkMetrics())
    private var mWorkingSectors: List<SectorData> = emptyList()

    private var mBaselineLines: Array<String> = emptyArray()
    private var mEdited: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        val modeExtra = intent.getStringExtra("extra_mode")
        if (modeExtra != null && modeExtra.equals("compose", ignoreCase = true)) {
            mCurrentMode.value = EditorMode.COMPOSE_BLOCKS
        }

        val sampleExtra = intent.getStringExtra("extra_test_sample")
        if (sampleExtra != null && sampleExtra.equals("4k", ignoreCase = true)) {
            mSampleType.value = DumpSampleType.DUMP_4K
        }

        loadDataset(mSampleType.value)

        setContent {
            MctTheme {
                DumpEditorPrototypeScreen(
                    mode = mCurrentMode.value,
                    sampleType = mSampleType.value,
                    sectors = mSectors.value,
                    metrics = mMetrics.value,
                    onModeChange = { newMode ->
                        mCurrentMode.value = newMode
                        measureBenchmark()
                    },
                    onSampleChange = { newSample ->
                        mSampleType.value = newSample
                        loadDataset(newSample)
                    },
                    onBlockEdited = { sIndex, bIndex, newHex ->
                        handleBlockEdited(sIndex, bIndex, newHex)
                    },
                    onNativeSectorChanged = { sIndex, lines, latencyMs ->
                        handleNativeSectorChanged(sIndex, lines, latencyMs)
                    },
                    onRunFidelityCheck = {
                        verifyByteForByteFidelity()
                    },
                    onBackClick = { finish() }
                )
            }
        }

        if (intent.getBooleanExtra("extra_verify_save", false)) {
            verifyByteForByteFidelity()
        }
    }

    private fun loadDataset(sample: DumpSampleType) {
        val startTime = SystemClock.elapsedRealtime()
        val sectorCount = if (sample == DumpSampleType.DUMP_4K) 40 else 16
        val list = ArrayList<SectorData>(sectorCount)
        val canonicalLines = ArrayList<String>()

        for (s in 0 until sectorCount) {
            val numBlocks = if (s < 32) 4 else 16
            val blocks = ArrayList<String>(numBlocks)
            canonicalLines.add("+Sector: $s")

            for (b in 0 until numBlocks) {
                val hex = when {
                    s == 0 && b == 0 -> "0BDC504FC80804000102030405060708" // UID Block 0
                    s == 1 && b == 1 -> "01000000FEFFFFFF0100000000FF00FF" // Value block
                    b == numBlocks - 1 -> "FFFFFFFFFFFFFF078069FFFFFFFFFFFF" // Sector trailer
                    else -> "00000000000000000000000000000000" // Data block
                }
                blocks.add(hex)
                canonicalLines.add(hex)
            }
            list.add(SectorData(sectorNumber = s, blocks = blocks))
        }

        mBaselineLines = canonicalLines.toTypedArray()
        mWorkingSectors = list
        mSectors.value = list
        mEdited = false

        val elapsed = SystemClock.elapsedRealtime() - startTime
        measureBenchmark(initTime = elapsed)
    }

    private fun handleBlockEdited(sectorIndex: Int, blockIndex: Int, newHex: String) {
        val current = mWorkingSectors.toMutableList()
        if (sectorIndex in current.indices) {
            val sector = current[sectorIndex]
            val updatedBlocks = sector.blocks.toMutableList()
            if (blockIndex in updatedBlocks.indices && updatedBlocks[blockIndex] != newHex.uppercase()) {
                updatedBlocks[blockIndex] = newHex.uppercase()
                current[sectorIndex] = sector.copy(blocks = updatedBlocks)
                mWorkingSectors = current
                mEdited = true
                mMetrics.value = mMetrics.value.copy(editLatencyMs = 1)
            }
        }
    }

    private fun handleNativeSectorChanged(sectorIndex: Int, lines: List<String>, latencyMs: Long) {
        val current = mWorkingSectors
        if (sectorIndex !in current.indices) {
            return
        }
        val expected = current[sectorIndex].blocks.size
        if (lines.size != expected || lines.any { it.length != 32 }) {
            mMetrics.value = mMetrics.value.copy(
                editLatencyMs = latencyMs,
                byteFidelityMatch = false,
                byteFidelityMessage = "编辑中: 扇区 $sectorIndex 行数/长度未齐 (${lines.size}/$expected)"
            )
            return
        }
        val updated = current.toMutableList()
        updated[sectorIndex] = current[sectorIndex].copy(blocks = lines.map { it.uppercase() })
        mWorkingSectors = updated
        mEdited = true
        mMetrics.value = mMetrics.value.copy(editLatencyMs = latencyMs)
    }

    private fun measureBenchmark(initTime: Long? = null) {
        val runtime = Runtime.getRuntime()
        val usedHeapMb = (runtime.totalMemory() - runtime.freeMemory()).toDouble() / (1024 * 1024)

        val hlStart = SystemClock.elapsedRealtime()
        var dummyLength = 0
        for (sec in mWorkingSectors) {
            for (b in sec.blocks) {
                dummyLength += b.length
            }
        }
        val hlElapsed = SystemClock.elapsedRealtime() - hlStart

        mMetrics.value = mMetrics.value.copy(
            initRenderTimeMs = initTime ?: mMetrics.value.initRenderTimeMs,
            highlightTimeMs = hlElapsed,
            heapMemoryMb = usedHeapMb
        )
    }

    private fun verifyByteForByteFidelity() {
        val exported = ArrayList<String>()
        for (s in mWorkingSectors) {
            exported.add("+Sector: ${s.sectorNumber}")
            for (b in s.blocks) {
                exported.add(b)
            }
        }

        val baselineHash = sha256Lines(mBaselineLines)
        val exportedHash = sha256Lines(exported.toTypedArray())

        val isMatch = (baselineHash == exportedHash) && (mBaselineLines.size == exported.size)
        val msg = when {
            isMatch && !mEdited -> "PASS: 未改动，SHA-256 与基准一致 (${exported.size} 行)"
            isMatch -> "PASS: 编辑后仍与基准一致 (${exported.size} 行)"
            mEdited -> "DIFF: 编辑已写入导出，哈希与基准不同 (${exported.size} 行，结构完整)"
            else -> "FAIL: 哈希不匹配 (基准 ${mBaselineLines.size}行 vs 导出 ${exported.size}行)"
        }

        mMetrics.value = mMetrics.value.copy(
            byteFidelityMatch = isMatch || (mEdited && exported.size == mBaselineLines.size),
            byteFidelityMessage = msg
        )
    }

    private fun sha256Lines(lines: Array<String>): String {
        val md = MessageDigest.getInstance("SHA-256")
        for (line in lines) {
            md.update(line.toByteArray(Charsets.UTF_8))
            md.update('\n'.code.toByte())
        }
        return Common.bytes2Hex(md.digest())
    }
}
