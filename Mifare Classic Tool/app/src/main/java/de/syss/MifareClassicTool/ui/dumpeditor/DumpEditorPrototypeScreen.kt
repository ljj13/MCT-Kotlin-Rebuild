package de.syss.MifareClassicTool.ui.dumpeditor

import android.graphics.Typeface
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableString
import android.text.TextUtils
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.components.MctTopAppBar

enum class EditorMode {
    NATIVE_VIEW,
    COMPOSE_BLOCKS
}

enum class DumpSampleType {
    DUMP_1K,
    DUMP_4K
}

data class SectorData(
    val sectorNumber: Int,
    var blocks: List<String>
)

data class BenchmarkMetrics(
    val initRenderTimeMs: Long = 0,
    val highlightTimeMs: Long = 0,
    val editLatencyMs: Long = 0,
    val heapMemoryMb: Double = 0.0,
    val byteFidelityMatch: Boolean = true,
    val byteFidelityMessage: String = "未校验"
)

@Composable
fun DumpEditorPrototypeScreen(
    mode: EditorMode,
    sampleType: DumpSampleType,
    sectors: List<SectorData>,
    metrics: BenchmarkMetrics,
    onModeChange: (EditorMode) -> Unit,
    onSampleChange: (DumpSampleType) -> Unit,
    onBlockEdited: (sectorIndex: Int, blockIndex: Int, newHex: String) -> Unit,
    onNativeSectorChanged: (sectorIndex: Int, lines: List<String>, latencyMs: Long) -> Unit,
    onRunFidelityCheck: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            MctTopAppBar(
                title = "DumpEditor 对比实验原型",
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Mode & Sample Selector Controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_prototype_controls"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "编辑器架构与测试规格选择:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = mode == EditorMode.NATIVE_VIEW,
                            onClick = { onModeChange(EditorMode.NATIVE_VIEW) },
                            label = { Text("方案 A: 原生 View") },
                            modifier = Modifier.testTag("chip_mode_view")
                        )
                        FilterChip(
                            selected = mode == EditorMode.COMPOSE_BLOCKS,
                            onClick = { onModeChange(EditorMode.COMPOSE_BLOCKS) },
                            label = { Text("方案 B: Compose 分块") },
                            modifier = Modifier.testTag("chip_mode_compose")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = sampleType == DumpSampleType.DUMP_1K,
                            onClick = { onSampleChange(DumpSampleType.DUMP_1K) },
                            label = { Text("1K (16 扇区, 64 块)") },
                            modifier = Modifier.testTag("chip_sample_1k")
                        )
                        FilterChip(
                            selected = sampleType == DumpSampleType.DUMP_4K,
                            onClick = { onSampleChange(DumpSampleType.DUMP_4K) },
                            label = { Text("4K (40 扇区, 256 块)") },
                            modifier = Modifier.testTag("chip_sample_4k")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metrics Dashboard Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_prototype_metrics"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "基准度量: 首帧 ${metrics.initRenderTimeMs}ms · 着色 ${metrics.highlightTimeMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Heap: %.1f MB".format(metrics.heapMemoryMb),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = metrics.byteFidelityMessage,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (metrics.byteFidelityMatch) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("text_fidelity_status")
                        )
                        OutlinedButton(
                            onClick = onRunFidelityCheck,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .height(32.dp)
                                .testTag("btn_verify_fidelity")
                        ) {
                            Text("校验", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Editor Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("container_editor_content")
            ) {
                if (mode == EditorMode.NATIVE_VIEW) {
                    NativeViewEditorContainer(
                        sectors = sectors,
                        onSectorTextChanged = onNativeSectorChanged
                    )
                } else {
                    ComposeBlocksEditorContainer(
                        sectors = sectors,
                        onBlockEdited = onBlockEdited
                    )
                }
            }
        }
    }
}

@Composable
fun NativeViewEditorContainer(
    sectors: List<SectorData>,
    onSectorTextChanged: (sectorIndex: Int, lines: List<String>, latencyMs: Long) -> Unit
) {
    val context = LocalContext.current
    val generation = remember(sectors) { sectors.hashCode() }
    AndroidView(
        factory = { ctx ->
            val scroll = ScrollView(ctx)
            val horizontal = HorizontalScrollView(ctx)
            val layout = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
            }
            populateNativeSectors(ctx, layout, sectors, onSectorTextChanged)
            horizontal.addView(layout)
            scroll.addView(horizontal)
            scroll.tag = generation
            scroll
        },
        update = { scroll ->
            if (scroll.tag == generation) {
                return@AndroidView
            }
            val horizontal = scroll.getChildAt(0) as HorizontalScrollView
            val layout = horizontal.getChildAt(0) as LinearLayout
            layout.removeAllViews()
            populateNativeSectors(context, layout, sectors, onSectorTextChanged)
            scroll.tag = generation
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("native_view_editor")
    )
}

private fun populateNativeSectors(
    context: android.content.Context,
    layout: LinearLayout,
    sectors: List<SectorData>,
    onSectorTextChanged: (sectorIndex: Int, lines: List<String>, latencyMs: Long) -> Unit
) {
    val accentColor = Common.getThemeAccentColor(context)
    val colorKeyA = ContextCompat.getColor(context, R.color.light_green)
    val colorKeyB = ContextCompat.getColor(context, R.color.dark_green)
    val colorAC = ContextCompat.getColor(context, R.color.orange)
    val colorUID = ContextCompat.getColor(context, R.color.purple)
    val colorVB = ContextCompat.getColor(context, R.color.yellow)

    for (sector in sectors) {
        val tvHeader = TextView(context).apply {
            setTextColor(accentColor)
            text = "Sector: ${sector.sectorNumber}"
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 12, 0, 4)
        }
        layout.addView(tvHeader)

        val et = AppCompatEditText(context).apply {
            typeface = Typeface.MONOSPACE
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            filters = arrayOf(InputFilter.AllCaps())
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11f)
            setHorizontallyScrolling(true)
            setPadding(16, 8, 16, 8)
        }

        // Build colored text for sector
        var text: CharSequence = ""
        val blockCount = sector.blocks.size
        for (i in 0 until blockCount) {
            val line = sector.blocks[i]
            val colored = if (i == blockCount - 1) {
                // Trailer block
                try {
                    val keyA = Common.colorString(line.substring(0, 12), colorKeyA)
                    val keyB = Common.colorString(line.substring(20), colorKeyB)
                    val ac = Common.colorString(line.substring(12, 18), colorAC)
                    SpannableString(TextUtils.concat(keyA, ac, line.substring(18, 20), keyB))
                } catch (e: Exception) {
                    SpannableString(line)
                }
            } else if (sector.sectorNumber == 0 && i == 0) {
                Common.colorString(line, colorUID)
            } else if (Common.isValueBlock(line)) {
                Common.colorString(line, colorVB)
            } else {
                SpannableString(line)
            }

            text = if (text.isEmpty()) colored else TextUtils.concat(text, "\n", colored)
        }

        et.setText(text, TextView.BufferType.SPANNABLE)
        val sectorIndex = layout.childCount / 2
        et.addTextChangedListener(object : android.text.TextWatcher {
            private var startedAt = 0L
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
            val started = android.os.SystemClock.elapsedRealtime()
            val lines = s?.toString().orEmpty().split('\n').map { it.trim() }
            val latency = android.os.SystemClock.elapsedRealtime() - started
            onSectorTextChanged(sectorIndex, lines, latency)
            }
        })
        layout.addView(et)
    }
}

@Composable
fun ComposeBlocksEditorContainer(
    sectors: List<SectorData>,
    onBlockEdited: (sectorIndex: Int, blockIndex: Int, newHex: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("compose_blocks_editor"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(sectors, key = { _, item -> item.sectorNumber }) { sIndex, sector ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_sector_${sector.sectorNumber}"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(6.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Sector: ${sector.sectorNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val blockCount = sector.blocks.size
                    for (bIndex in 0 until blockCount) {
                        val blockHex = sector.blocks[bIndex]
                        val isTrailer = bIndex == blockCount - 1
                        val isUidBlock = sector.sectorNumber == 0 && bIndex == 0
                        var draft by remember(sector.sectorNumber, bIndex, blockHex) {
                            mutableStateOf(blockHex)
                        }
                        val isVb = Common.isValueBlock(draft)
                        val annotated = remember(draft, isTrailer, isUidBlock, isVb) {
                            buildAnnotatedBlock(draft, isTrailer, isUidBlock, isVb)
                        }

                        Row(
                            modifier = Modifier.padding(vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "B$bIndex: ",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 11.sp
                            )

                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = annotated,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier.testTag("compose_highlight_${sector.sectorNumber}_$bIndex")
                                )
                                BasicTextField(
                                    value = draft,
                                    onValueChange = { raw ->
                                        draft = raw.uppercase()
                                            .filter { it in '0'..'9' || it in 'A'..'F' }
                                            .take(32)
                                    },
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color.Transparent
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters,
                                        keyboardType = KeyboardType.Ascii,
                                        autoCorrectEnabled = false
                                    ),
                                    singleLine = true,
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .onFocusChanged { focus ->
                                            if (!focus.isFocused && draft != blockHex) {
                                                onBlockEdited(sIndex, bIndex, draft)
                                            }
                                        }
                                        .testTag("compose_block_${sector.sectorNumber}_$bIndex")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildAnnotatedBlock(
    hex: String,
    isTrailer: Boolean,
    isUid: Boolean,
    isVb: Boolean
): AnnotatedString {
    val lightGreen = Color(0xFF2E7D32)
    val darkGreen = Color(0xFF388E3C)
    val orange = Color(0xFFFFB74D)
    val purple = Color(0xFFBA68C8)
    val yellow = Color(0xFFFFF176)

    return buildAnnotatedString {
        if (isTrailer && hex.length == 32) {
            // Key A (0..12)
            pushStyle(SpanStyle(color = lightGreen, fontWeight = FontWeight.Bold))
            append(hex.substring(0, 12))
            pop()
            // AC (12..18)
            pushStyle(SpanStyle(color = orange, fontWeight = FontWeight.Bold))
            append(hex.substring(12, 18))
            pop()
            // User byte (18..20)
            append(hex.substring(18, 20))
            // Key B (20..32)
            pushStyle(SpanStyle(color = darkGreen, fontWeight = FontWeight.Bold))
            append(hex.substring(20, 32))
            pop()
        } else if (isUid) {
            pushStyle(SpanStyle(color = purple, fontWeight = FontWeight.Bold))
            append(hex)
            pop()
        } else if (isVb) {
            pushStyle(SpanStyle(color = yellow, fontWeight = FontWeight.Bold))
            append(hex)
            pop()
        } else {
            append(hex)
        }
    }
}
