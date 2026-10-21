package de.syss.MifareClassicTool

import android.view.View
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.Activities.WriteTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Non-write UI test of the WriteTag hybrid screen. It verifies the M3 shell and
 * the six native write-mode selectors plus mode switching. It never clicks any
 * write/confirm/format button, so no NFC write path can be triggered.
 */
@RunWith(AndroidJUnit4::class)
class WriteTagComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<WriteTag>()

    private fun visibility(id: Int): Int =
        composeTestRule.activity.findViewById<View>(id).visibility

    private fun click(id: Int) {
        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.findViewById<View>(id).performClick()
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun testShellAndSixModeSelectorsPresent() {
        composeTestRule.onNodeWithTag("write_tag_shell").assertIsDisplayed()
        composeTestRule.onNodeWithTag("write_tag_safety_notice").assertIsDisplayed()

        assertEquals(View.VISIBLE, visibility(R.id.radioButtonWriteTagWriteBlock))
        assertEquals(View.VISIBLE, visibility(R.id.radioButtonWriteTagWriteDump))
        assertEquals(View.VISIBLE, visibility(R.id.radioButtonWriteTagCloneUid))
        assertEquals(View.VISIBLE, visibility(R.id.radioButtonWriteTagFactoryFormat))
        assertEquals(View.VISIBLE, visibility(R.id.radioButtonWriteTagWriteValueBlock))
        assertEquals(View.VISIBLE, visibility(R.id.radioButtonWriteTagTransferRestore))
    }

    @Test
    fun testDefaultModeIsWriteBlock() {
        assertEquals(View.VISIBLE, visibility(R.id.relativeLayoutWriteTagWriteBlock))
        assertEquals(View.GONE, visibility(R.id.linearLayoutWriteTagDump))
        assertEquals(View.GONE, visibility(R.id.relativeLayoutWriteTagTransferRestore))
    }

    @Test
    fun testModeSwitchRevealsOnlyTargetLayout() {
        click(R.id.radioButtonWriteTagWriteDump)
        assertEquals(View.VISIBLE, visibility(R.id.linearLayoutWriteTagDump))
        assertEquals(View.GONE, visibility(R.id.relativeLayoutWriteTagWriteBlock))

        click(R.id.radioButtonWriteTagTransferRestore)
        assertEquals(View.VISIBLE, visibility(R.id.relativeLayoutWriteTagTransferRestore))
        assertEquals(View.GONE, visibility(R.id.linearLayoutWriteTagDump))
    }
}
