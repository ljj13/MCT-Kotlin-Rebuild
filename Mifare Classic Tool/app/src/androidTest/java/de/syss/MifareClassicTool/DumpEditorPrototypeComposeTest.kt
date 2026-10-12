package de.syss.MifareClassicTool

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.ui.dumpeditor.BenchmarkMetrics
import de.syss.MifareClassicTool.ui.dumpeditor.DumpEditorPrototypeScreen
import de.syss.MifareClassicTool.ui.dumpeditor.DumpSampleType
import de.syss.MifareClassicTool.ui.dumpeditor.EditorMode
import de.syss.MifareClassicTool.ui.dumpeditor.SectorData
import de.syss.MifareClassicTool.ui.theme.MctTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DumpEditorPrototypeComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDumpEditorPrototype1kNativeView() {
        val sectors1k = (0 until 16).map { s ->
            SectorData(
                sectorNumber = s,
                blocks = listOf(
                    "00000000000000000000000000000000",
                    "00000000000000000000000000000000",
                    "00000000000000000000000000000000",
                    "FFFFFFFFFFFFFF078069FFFFFFFFFFFF"
                )
            )
        }

        var fidelityRun = false
        val metrics = BenchmarkMetrics(
            initRenderTimeMs = 12,
            highlightTimeMs = 2,
            heapMemoryMb = 34.5,
            byteFidelityMatch = true,
            byteFidelityMessage = "PASS: SHA-256 完全匹配 (80 行)"
        )

        composeTestRule.setContent {
            MctTheme {
                DumpEditorPrototypeScreen(
                    mode = EditorMode.NATIVE_VIEW,
                    sampleType = DumpSampleType.DUMP_1K,
                    sectors = sectors1k,
                    metrics = metrics,
                    onModeChange = {},
                    onSampleChange = {},
                    onBlockEdited = { _, _, _ -> },
                    onNativeSectorChanged = { _, _, _ -> },
                    onRunFidelityCheck = { fidelityRun = true },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("card_prototype_controls").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_prototype_metrics").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chip_mode_view").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chip_sample_1k").assertIsDisplayed()
        composeTestRule.onNodeWithTag("native_view_editor").assertIsDisplayed()
        composeTestRule.onNodeWithTag("text_fidelity_status").assertTextContains("PASS")

        composeTestRule.onNodeWithTag("btn_verify_fidelity").performClick()
        assertTrue(fidelityRun)
    }

    @Test
    fun testDumpEditorPrototype4kComposeBlocks() {
        val sectors4k = (0 until 40).map { s ->
            val numBlocks = if (s < 32) 4 else 16
            SectorData(
                sectorNumber = s,
                blocks = (0 until numBlocks).map { b ->
                    if (b == numBlocks - 1) "FFFFFFFFFFFFFF078069FFFFFFFFFFFF"
                    else "00000000000000000000000000000000"
                }
            )
        }

        val metrics = BenchmarkMetrics(
            initRenderTimeMs = 45,
            highlightTimeMs = 10,
            heapMemoryMb = 48.2,
            byteFidelityMatch = true,
            byteFidelityMessage = "PASS: SHA-256 完全匹配 (296 行)"
        )

        composeTestRule.setContent {
            MctTheme {
                DumpEditorPrototypeScreen(
                    mode = EditorMode.COMPOSE_BLOCKS,
                    sampleType = DumpSampleType.DUMP_4K,
                    sectors = sectors4k,
                    metrics = metrics,
                    onModeChange = {},
                    onSampleChange = {},
                    onBlockEdited = { _, _, _ -> },
                    onNativeSectorChanged = { _, _, _ -> },
                    onRunFidelityCheck = {},
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("compose_blocks_editor").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_sector_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("text_fidelity_status").assertTextContains("296 行")
    }
}
