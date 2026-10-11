package de.syss.MifareClassicTool.ui.taginfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.components.MctSectionCard
import de.syss.MifareClassicTool.ui.components.MctTopAppBar

data class TagInfoData(
    val uid: String,
    val uidRaw: String,
    val rfTech: String,
    val atqa: String,
    val sak: String,
    val ats: String,
    val tagType: String,
    val isTagTypeGuessed: Boolean,
    val memSize: String? = null,
    val blockSize: String? = null,
    val sectorCount: String? = null,
    val blockCount: String? = null
)

@Composable
fun TagInfoScreen(
    tagInfo: TagInfoData?,
    mfcSupport: Int, // 0 = OK, -1 = Device unsupported, -2 = Tag unsupported
    onBackClick: () -> Unit,
    onReadMoreClick: () -> Unit,
    onCopyUid: (String) -> Unit
) {
    Scaffold(
        topBar = {
            MctTopAppBar(
                title = stringResource(R.string.title_activity_tag_info),
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Unsupported MFC Warning Banner
            if (mfcSupport != 0 && tagInfo != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mfc_warning_banner"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (mfcSupport == -1) {
                                    stringResource(R.string.text_no_mfc_support_device)
                                } else {
                                    stringResource(R.string.text_no_mfc_support_tag)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = onReadMoreClick,
                            modifier = Modifier.testTag("btn_read_more")
                        ) {
                            Text(text = stringResource(R.string.action_more))
                        }
                    }
                }
            }

            if (tagInfo == null) {
                // Empty State: No Tag Detected
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .testTag("no_tag_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.text_no_tag),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "请贴附 MIFARE Classic 或 NFC 标签以读取芯片参数",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                // 1. Generic Tag Parameters
                MctSectionCard(
                    title = stringResource(R.string.text_generic_info),
                    modifier = Modifier.testTag("card_generic_info")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.text_uid),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tagInfo.uid,
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("text_uid_val")
                            )
                        }
                        FilledTonalButton(
                            onClick = { onCopyUid(tagInfo.uidRaw) },
                            modifier = Modifier.testTag("btn_copy_uid")
                        ) {
                            Text(text = stringResource(R.string.text_copy))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    TagInfoRow(label = stringResource(R.string.text_rf_tech), value = tagInfo.rfTech)
                    TagInfoRow(label = stringResource(R.string.text_atqa), value = tagInfo.atqa, isMono = true)
                    TagInfoRow(label = stringResource(R.string.text_sak), value = tagInfo.sak, isMono = true)
                    TagInfoRow(label = stringResource(R.string.text_ats), value = tagInfo.ats, isMono = true)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Text(
                        text = stringResource(R.string.text_tag_type_and_manuf),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tagInfo.tagType,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("text_tag_type")
                    )
                    if (tagInfo.isTagTypeGuessed) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "(${stringResource(R.string.text_tag_type_guess)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // 2. MIFARE Classic Architecture (if supported)
                if (mfcSupport == 0 && tagInfo.memSize != null) {
                    MctSectionCard(
                        title = stringResource(R.string.text_mf_info),
                        modifier = Modifier.testTag("card_mfc_info")
                    ) {
                        TagInfoRow(
                            label = stringResource(R.string.text_mem_size),
                            value = "${tagInfo.memSize} byte"
                        )
                        TagInfoRow(
                            label = stringResource(R.string.text_block_size),
                            value = "${tagInfo.blockSize} byte"
                        )
                        TagInfoRow(
                            label = stringResource(R.string.text_sector_count),
                            value = tagInfo.sectorCount ?: "-"
                        )
                        TagInfoRow(
                            label = stringResource(R.string.text_block_count),
                            value = tagInfo.blockCount ?: "-"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagInfoRow(
    label: String,
    value: String,
    isMono: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default
        )
    }
}
