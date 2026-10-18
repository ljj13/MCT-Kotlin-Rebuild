package de.syss.MifareClassicTool.ui.diff

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.components.MctSectionCard
import de.syss.MifareClassicTool.ui.components.MctTopAppBar

data class DiffBlockItem(
    val blockIndex: Int,
    val blockHeader: String,
    val dump1Line: String,
    val dump2Line: String,
    val diffIndices: Set<Int>,
    val isIdentical: Boolean
)

data class DiffSectorItem(
    val sectorNumber: Int,
    val title: String,
    val statusText: String? = null,
    val isOnlyInDump1: Boolean = false,
    val isOnlyInDump2: Boolean = false,
    val isIdentical: Boolean = false,
    val diffCount: Int = 0,
    val blocks: List<DiffBlockItem> = emptyList()
)

data class DiffToolUiState(
    val dump1Name: String? = null,
    val dump2Name: String? = null,
    val isDump1FromEditor: Boolean = false,
    val hideIdenticalSectors: Boolean = false,
    val diffPercentageText: String? = null,
    val sectors: List<DiffSectorItem> = emptyList()
)

@Composable
fun DiffToolScreen(
    state: DiffToolUiState,
    onBackClick: () -> Unit,
    onChooseDump1Click: () -> Unit,
    onChooseDump2Click: () -> Unit,
    onHideIdenticalChanged: (Boolean) -> Unit
) {
    var expandedSectors by remember { mutableStateOf(setOf<Int>()) }

    Scaffold(
        topBar = {
            MctTopAppBar(
                title = stringResource(R.string.title_activity_diff_tool),
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 1. Dumps Selection and Options Panel
            MctSectionCard(
                title = stringResource(R.string.text_open_dump_title),
                modifier = Modifier.testTag("card_dump_selection")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onChooseDump1Click,
                        enabled = !state.isDump1FromEditor,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("btn_choose_dump1")
                    ) {
                        Text(
                            text = if (state.isDump1FromEditor) {
                                stringResource(R.string.text_dump_from_editor)
                            } else {
                                state.dump1Name ?: stringResource(R.string.text_dump_1)
                            },
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Button(
                        onClick = onChooseDump2Click,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("btn_choose_dump2")
                    ) {
                        Text(
                            text = state.dump2Name ?: stringResource(R.string.text_dump_2),
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Hide identical sectors checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onHideIdenticalChanged(!state.hideIdenticalSectors) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.hideIdenticalSectors,
                        onCheckedChange = null,
                        modifier = Modifier.testTag("cb_hide_identical")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_hide_identical_sectors),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Diff Percentage Banner & Expand controls
                if (state.diffPercentageText != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.diffPercentageText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("text_diff_percentage")
                        )
                        Row {
                            TextButton(
                                onClick = {
                                    expandedSectors = state.sectors.map { it.sectorNumber }.toSet()
                                },
                                modifier = Modifier.testTag("btn_expand_all")
                            ) {
                                Text("全展开", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(
                                onClick = { expandedSectors = emptySet() },
                                modifier = Modifier.testTag("btn_collapse_all")
                            ) {
                                Text("全折叠", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Comparison Result List
            if (state.sectors.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .testTag("card_empty_diff"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (state.dump1Name != null && state.dump2Name != null) {
                                "无不同扇区 (或已勾选隐藏相同扇区)"
                            } else {
                                "请选择两个转储文件"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击上方按钮选择两个转储文件（Dump 1 与 Dump 2），MCT 将逐扇区、逐字节精准比对差异。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("list_diff_sectors"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.sectors, key = { it.sectorNumber }) { sector ->
                        val isExpanded = expandedSectors.contains(sector.sectorNumber) ||
                                (expandedSectors.isEmpty() && !sector.isIdentical)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sector_card_${sector.sectorNumber}"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Header row (Click to toggle expand)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedSectors = if (isExpanded) {
                                                expandedSectors - sector.sectorNumber
                                            } else {
                                                expandedSectors + sector.sectorNumber
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = sector.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // Sector Status Badge
                                        when {
                                            sector.isOnlyInDump1 -> {
                                                SectorBadge("仅在 Dump 1", Color(0xFFFFCCBC), Color(0xFFD84315))
                                            }
                                            sector.isOnlyInDump2 -> {
                                                SectorBadge("仅在 Dump 2", Color(0xFFFFCCBC), Color(0xFFD84315))
                                            }
                                            sector.isIdentical -> {
                                                SectorBadge("完全一致", Color(0xFFC8E6C9), Color(0xFF2E7D32))
                                            }
                                            else -> {
                                                SectorBadge("${sector.diffCount} 处不同", Color(0xFFFFCDD2), Color(0xFFC62828))
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (isExpanded) "▲" else "▼",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                if (sector.statusText != null && (sector.isOnlyInDump1 || sector.isOnlyInDump2)) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = sector.statusText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                // Expanded Blocks Diff View
                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    ) {
                                        sector.blocks.forEach { block ->
                                            BlockDiffView(block = block)
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectorBadge(text: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .background(bgColor, shape = MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun BlockDiffView(block: DiffBlockItem) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("block_diff_${block.blockIndex}")
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = block.blockHeader,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Horizontally scrollable container for 32 monospace characters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Column {
                    // Line 1: Dump 1
                    DiffMonospaceRow(
                        prefix = "A: ",
                        line = block.dump1Line,
                        diffIndices = block.diffIndices,
                        labelColor = MaterialTheme.colorScheme.tertiary
                    )
                    // Line 2: Dump 2
                    DiffMonospaceRow(
                        prefix = "B: ",
                        line = block.dump2Line,
                        diffIndices = block.diffIndices,
                        labelColor = MaterialTheme.colorScheme.secondary
                    )
                    // Line 3: Difference Mask Line
                    DiffMaskRow(
                        prefix = "Δ: ",
                        diffIndices = block.diffIndices,
                        isIdentical = block.isIdentical,
                        length = block.dump1Line.length.coerceAtLeast(block.dump2Line.length)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiffMonospaceRow(
    prefix: String,
    line: String,
    diffIndices: Set<Int>,
    labelColor: Color
) {
    val annotated = buildAnnotatedString {
        append(prefix)
        for (i in line.indices) {
            val char = line[i]
            if (diffIndices.contains(i)) {
                pushStyle(
                    SpanStyle(
                        color = Color(0xFFD32F2F),
                        background = Color(0xFFFFEBEE),
                        fontWeight = FontWeight.Bold
                    )
                )
                append(char)
                pop()
            } else {
                append(char)
            }
        }
    }

    Text(
        text = annotated,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        maxLines = 1
    )
}

@Composable
private fun DiffMaskRow(
    prefix: String,
    diffIndices: Set<Int>,
    isIdentical: Boolean,
    length: Int
) {
    val maskText = if (isIdentical) {
        "-".repeat(length.coerceAtLeast(32))
    } else {
        val chars = CharArray(length.coerceAtLeast(32)) { ' ' }
        for (idx in diffIndices) {
            if (idx in chars.indices) {
                chars[idx] = 'X'
            }
        }
        String(chars)
    }

    Text(
        text = "$prefix$maskText",
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = if (isIdentical) Color(0xFF2E7D32) else Color(0xFFD32F2F),
        maxLines = 1
    )
}
