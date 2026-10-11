package de.syss.MifareClassicTool.ui.hextoascii

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.components.MctTopAppBar

@Composable
fun HexToAsciiScreen(
    dump: Array<String>?,
    onBackClick: () -> Unit
) {
    val sectorText = stringResource(R.string.text_sector)
    val invalidDataText = stringResource(R.string.text_invalid_data)
    val primaryColor = MaterialTheme.colorScheme.primary

    val annotatedAscii = buildAnnotatedString {
        if (!dump.isNullOrEmpty()) {
            for (rawLine in dump) {
                val line = rawLine.trim()
                if (line.startsWith("+")) {
                    val sectorNumber = line.split(":").getOrNull(1)?.trim() ?: ""
                    withStyle(
                        style = SpanStyle(
                            color = primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("$sectorText: $sectorNumber\n")
                    }
                } else {
                    val converted = Common.hex2Ascii(line) ?: invalidDataText
                    append(" $converted\n")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            MctTopAppBar(
                title = stringResource(R.string.title_activity_hex_to_ascii),
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ascii_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                SelectionContainer {
                    Text(
                        text = annotatedAscii,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("ascii_content"),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
