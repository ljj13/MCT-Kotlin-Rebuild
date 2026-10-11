package de.syss.MifareClassicTool.ui.accesscondition

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.components.MctSectionCard
import de.syss.MifareClassicTool.ui.components.MctTopAppBar

data class BlockAcPermission(
    val blockLabel: String,
    val read: String,
    val write: String,
    val incr: String,
    val decr: String
)

data class TrailerAcPermission(
    val target: String,
    val read: String,
    val write: String
)

data class DecodedSectorAc(
    val sectorHeader: String,
    val isValid: Boolean,
    val blocks: List<BlockAcPermission> = emptyList(),
    val trailer: List<TrailerAcPermission> = emptyList()
)

@Composable
fun AccessConditionDecoderScreen(
    sectors: List<DecodedSectorAc>,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            MctTopAppBar(
                title = stringResource(R.string.title_activity_access_conditions),
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        if (sectors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_ac_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "无访问控制条件数据",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "未接收到有效的转储扇区控制位参数，请从转储编辑器中进入。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("list_decoded_sectors"),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(sectors) { sector ->
                    MctSectionCard(
                        title = sector.sectorHeader,
                        modifier = Modifier.testTag("sector_card_${sector.sectorHeader}")
                    ) {
                        if (!sector.isValid) {
                            Text(
                                text = stringResource(R.string.text_invalid_ac),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            // 1. Data blocks
                            Text(
                                text = "数据块控制位",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // Table header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        shape = MaterialTheme.shapes.extraSmall
                                    )
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.text_ac_pos),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1.3f)
                                )
                                Text(
                                    text = stringResource(R.string.text_ac_read),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = stringResource(R.string.text_ac_write),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = stringResource(R.string.text_ac_incr),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "减/存",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                            }

                            sector.blocks.forEach { block ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = block.blockLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1.3f)
                                    )
                                    PermissionBadge(text = block.read, modifier = Modifier.weight(1f))
                                    PermissionBadge(text = block.write, modifier = Modifier.weight(1f))
                                    PermissionBadge(text = block.incr, modifier = Modifier.weight(1f))
                                    PermissionBadge(text = block.decr, modifier = Modifier.weight(1f))
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            // 2. Sector trailer
                            Text(
                                text = "扇区尾块控制位",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        shape = MaterialTheme.shapes.extraSmall
                                    )
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "控制目标",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1.3f)
                                )
                                Text(
                                    text = stringResource(R.string.text_ac_read),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1.5f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = stringResource(R.string.text_ac_write),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1.5f),
                                    textAlign = TextAlign.Center
                                )
                            }

                            sector.trailer.forEach { trailerItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = trailerItem.target,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1.3f)
                                    )
                                    PermissionBadge(text = trailerItem.read, modifier = Modifier.weight(1.5f))
                                    PermissionBadge(text = trailerItem.write, modifier = Modifier.weight(1.5f))
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
private fun PermissionBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (text) {
        "Never" -> Pair(Color(0xFFFFE0B2), Color(0xFFE65100)) // Orange
        "Key A", "Key B" -> Pair(Color(0xFFFFF9C4), Color(0xFFF57F17)) // Yellow
        "Key A|B" -> Pair(Color(0xFFC8E6C9), Color(0xFF1B5E20)) // Light Green
        "Error" -> Pair(Color(0xFFFFCDD2), Color(0xFFB71C1C)) // Red
        else -> Pair(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
    }

    Box(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .background(bgColor, shape = MaterialTheme.shapes.extraSmall)
            .padding(vertical = 2.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
