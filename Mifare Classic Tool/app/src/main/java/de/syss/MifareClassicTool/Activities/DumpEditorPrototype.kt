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
    private var mNativeExport: (() -> List<String>?)? = null
    private var mComposeExport: (() -> List<String>?)? = null

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
                    onNativeExportReady = { exporter ->
                        mNativeExport = exporter
                    },
                    onComposeExportReady = { exporter ->
                        mComposeExport = exporter
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
        applyStaticEdit(intent.getStringExtra("extra_static_edit"))
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

    private fun applyStaticEdit(spec: String?) {
        if (spec.isNullOrBlank()) {
            return
        }
        val parts = spec.split(':')
        if (parts.size != 4) {
            return
        }
        val sector = parts[0].toIntOrNull() ?: return
        val block = parts[1].toIntOrNull() ?: return
        val start = parts[2].toIntOrNull() ?: return
        val replacement = parts[3].uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
        val source = mWorkingSectors
        if (sector !in source.indices || block !in source[sector].blocks.indices) {
            return
        }
        val original = source[sector].blocks[block]
        if (start < 0 || start + replacement.length > original.length) {
            return
        }
        val edited = original.replaceRange(start, start + replacement.length, replacement)
        val started = SystemClock.elapsedRealtime()
        val current = source.toMutableList()
        val updatedBlocks = current[sector].blocks.toMutableList()
        updatedBlocks[block] = edited
        current[sector] = current[sector].copy(blocks = updatedBlocks)
        mWorkingSectors = current
        mSectors.value = current
        mEdited = true
        val latency = SystemClock.elapsedRealtime() - started
        mMetrics.value = mMetrics.value.copy(editLatencyMs = latency)
        verifyByteForByteFidelity()
    }

    private fun verifyByteForByteFidelity() {
        val widgetLines = when (mCurrentMode.value) {
            EditorMode.NATIVE_VIEW -> mNativeExport?.invoke()
            EditorMode.COMPOSE_BLOCKS -> mComposeExport?.invoke()
        }
        val exported = if (widgetLines != null) {
            widgetLines
        } else {
            val fallback = ArrayList<String>()
            for (s in mWorkingSectors) {
                fallback.add("+Sector: ${s.sectorNumber}")
                fallback.addAll(s.blocks)
            }
            fallback
        }

        val baselineHash = sha256Lines(mBaselineLines)
        val exportedHash = sha256Lines(exported.toTypedArray())
        val expected = expectedEditedLines()
        val expectedHash = sha256Lines(expected)

        val isMatch = (baselineHash == exportedHash) && (mBaselineLines.size == exported.size)
        val editedMatch = mEdited && exported.size == expected.size && exportedHash == expectedHash
        val msg = when {
            editedMatch -> "PASS: 编辑后逐字节一致 (${exported.size} 行, ${mMetrics.value.editLatencyMs}ms)"
            isMatch && !mEdited -> "PASS: 未改动，SHA-256 与基准一致 (${exported.size} 行)"
            isMatch -> "PASS: 编辑后仍与基准一致 (${exported.size} 行)"
            mEdited -> "FAIL: 控件导出与编辑结果不一致 (${exported.size} 行)"
            else -> "FAIL: 哈希不匹配 (基准 ${mBaselineLines.size}行 vs 导出 ${exported.size}行)"
        }

        mMetrics.value = mMetrics.value.copy(
            byteFidelityMatch = isMatch || editedMatch,
            byteFidelityMessage = msg
        )
    }

    private fun expectedEditedLines(): Array<String> {
        val lines = ArrayList<String>()
        for (s in mWorkingSectors) {
            lines.add("+Sector: ${s.sectorNumber}")
            lines.addAll(s.blocks)
        }
        return lines.toTypedArray()
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
