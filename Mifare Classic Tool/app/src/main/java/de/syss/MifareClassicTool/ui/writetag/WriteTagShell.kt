package de.syss.MifareClassicTool.ui.writetag

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.components.MctTopAppBar

/**
 * Material 3 shell for the legacy WriteTag native form.
 * Only the top bar and a pure display-only safety notice live here; the native
 * XML form and every write entry point are preserved unchanged beneath it.
 */
@Composable
fun WriteTagShell(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("write_tag_shell")
    ) {
        MctTopAppBar(
            title = stringResource(R.string.action_write_tag),
            onBackClick = onBackClick
        )
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.write_tag_safety_notice),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("write_tag_safety_notice")
            )
        }
    }
}
