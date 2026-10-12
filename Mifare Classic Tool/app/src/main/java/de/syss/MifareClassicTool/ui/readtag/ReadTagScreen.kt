package de.syss.MifareClassicTool.ui.readtag

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.components.MctActionButton
import de.syss.MifareClassicTool.ui.components.MctSectionCard
import de.syss.MifareClassicTool.ui.components.MctTopAppBar

enum class ReadTagPhase {
    WAITING_FOR_KEY_MAP,
    READING,
    FAILED,
    FINISHED
}

data class ReadTagUiState(
    val phase: ReadTagPhase = ReadTagPhase.WAITING_FOR_KEY_MAP,
    val message: String = "",
    val canCancel: Boolean = true
)

@Composable
fun ReadTagScreen(
    state: ReadTagUiState,
    onBackClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Scaffold(
        topBar = {
            MctTopAppBar(
                title = stringResource(R.string.action_read_tag),
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .testTag("read_tag_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MctSectionCard(title = stringResource(R.string.action_read_tag)) {
                if (state.phase == ReadTagPhase.READING || state.phase == ReadTagPhase.WAITING_FOR_KEY_MAP) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("read_tag_progress")
                    )
                }
                Text(
                    text = state.message.ifBlank { stringResource(R.string.text_wait_read_tag) },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("read_tag_message")
                )
            }
            MctActionButton(
                text = stringResource(R.string.action_cancel),
                onClick = onCancelClick,
                enabled = state.canCancel,
                testTag = "btn_read_tag_cancel"
            )
        }
    }
}
