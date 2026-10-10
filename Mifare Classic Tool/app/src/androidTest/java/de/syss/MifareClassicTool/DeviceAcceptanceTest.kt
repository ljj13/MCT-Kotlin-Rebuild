package de.syss.MifareClassicTool

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.syss.MifareClassicTool.Activities.*
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DeviceAcceptanceTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            val context: Context = ApplicationProvider.getApplicationContext()
            // Set shared preferences to bypass blocking modal dialogs
            val sharedPref = context.getSharedPreferences("MainMenu", Context.MODE_PRIVATE)
            sharedPref.edit()
                .putBoolean("is_first_run", false)
                .putBoolean("show_donate_dialog", false)
                .commit()
        }

        @JvmStatic
        @AfterClass
        fun tearDownClass() {
        }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        val sharedPref = context.getSharedPreferences("MainMenu", Context.MODE_PRIVATE)
        sharedPref.edit()
            .putBoolean("is_first_run", false)
            .putBoolean("show_donate_dialog", false)
            .commit()
    }

    @Test
    fun test01_AppDataInitialization() {
        val keysDir = Common.getFile(Common.KEYS_DIR)
        val dumpsDir = Common.getFile(Common.DUMPS_DIR)
        val tmpDir = Common.getFile(Common.TMP_DIR)

        assertTrue("Keys directory should exist", keysDir.exists())
        assertTrue("Dumps directory should exist", dumpsDir.exists())
        assertTrue("Tmp directory should exist", tmpDir.exists())

        val stdKeys = File(keysDir, Common.STD_KEYS)
        val extKeys = File(keysDir, Common.STD_KEYS_EXTENDED)
        val hotelKeys = File(keysDir, Common.STD_HOTEL_KEYS)

        assertTrue("std.keys should be copied from assets", stdKeys.exists() && stdKeys.length() > 0)
        assertTrue("extended-std.keys should be copied", extKeys.exists() && extKeys.length() > 0)
        assertTrue("hotel-std.keys should be copied", hotelKeys.exists() && hotelKeys.length() > 0)
    }

    @Test
    fun test02_LaunchMainMenu() {
        ActivityScenario.launch(MainMenu::class.java).use { scenario ->
            assertNotNull(scenario)
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse("MainMenu should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun test03_LaunchHelpAndInfo() {
        ActivityScenario.launch(HelpAndInfo::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse("HelpAndInfo should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun test04_LaunchPreferences() {
        ActivityScenario.launch(Preferences::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse("Preferences should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun test05_LaunchBccTool() {
        ActivityScenario.launch(BccTool::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse("BccTool should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun test06_LaunchValueBlockTool() {
        ActivityScenario.launch(ValueBlockTool::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse("ValueBlockTool should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun test07_LaunchDataConversionTool() {
        ActivityScenario.launch(DataConversionTool::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse("DataConversionTool should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun test08_LaunchAccessConditionTool() {
        ActivityScenario.launch(AccessConditionTool::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse("AccessConditionTool should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun test09_LaunchTagInfoTool() {
        ActivityScenario.launch(TagInfoTool::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse("TagInfoTool should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun test10_LaunchUidLogTool() {
        ActivityScenario.launch(UidLogTool::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse("UidLogTool should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun test11_LaunchCloneUidTool() {
        ActivityScenario.launch(CloneUidTool::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse("CloneUidTool should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun test12_LaunchImportExportTool() {
        ActivityScenario.launch(ImportExportTool::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse("ImportExportTool should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun test13_LaunchFileChooserForKeys() {
        val intent = Intent(context, FileChooser::class.java).apply {
            putExtra(FileChooser.EXTRA_DIR, Common.getFile(Common.KEYS_DIR).absolutePath)
            putExtra(FileChooser.EXTRA_TITLE, "Choose Key File")
            putExtra(FileChooser.EXTRA_ALLOW_NEW_FILE, true)
        }
        ActivityScenario.launch<FileChooser>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse("FileChooser should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun test14_LaunchKeyEditor() {
        val keyFile = File(Common.getFile(Common.KEYS_DIR), Common.STD_KEYS)
        val intent = Intent(context, KeyEditor::class.java).apply {
            putExtra(FileChooser.EXTRA_CHOSEN_FILE, keyFile.absolutePath)
        }
        ActivityScenario.launch<KeyEditor>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse("KeyEditor should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun test15_LaunchDumpEditorWithData() {
        val sampleDump = arrayOf(
            "+Sector: 0",
            "01020304040804000000000000000000",
            "00000000000000000000000000000000",
            "00000000000000000000000000000000",
            "A0B1C2D3E4F5FF078069FFFFFFFFFFFF"
        )
        val intent = Intent(context, DumpEditor::class.java).apply {
            putExtra(DumpEditor.EXTRA_DUMP, sampleDump)
        }
        ActivityScenario.launch<DumpEditor>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse("DumpEditor should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun test16_LaunchDiffToolWithData() {
        val sampleDump = arrayOf(
            "+Sector: 0",
            "01020304040804000000000000000000",
            "00000000000000000000000000000000",
            "00000000000000000000000000000000",
            "A0B1C2D3E4F5FF078069FFFFFFFFFFFF"
        )
        val intent = Intent(context, DiffTool::class.java).apply {
            putExtra(DiffTool.EXTRA_DUMP, sampleDump)
        }
        ActivityScenario.launch<DiffTool>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse("DiffTool should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun test17_LaunchAccessConditionDecoder() {
        val sampleAc = arrayOf(
            "+Sector: 0",
            "FF078069"
        )
        val intent = Intent(context, AccessConditionDecoder::class.java).apply {
            putExtra(AccessConditionDecoder.EXTRA_AC, sampleAc)
        }
        ActivityScenario.launch<AccessConditionDecoder>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse("AccessConditionDecoder should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun test18_LaunchValueBlocksToInt() {
        val sampleVb = arrayOf(
            "Sector: 0, Block: 1",
            "01000000FEFFFFFF0100000005FA05FA"
        )
        val intent = Intent(context, ValueBlocksToInt::class.java).apply {
            putExtra(ValueBlocksToInt.EXTRA_VB, sampleVb)
        }
        ActivityScenario.launch<ValueBlocksToInt>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse("ValueBlocksToInt should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun test19_LaunchHexToAscii() {
        val sampleDump = arrayOf(
            "+Sector: 0",
            "48656C6C6F20576F726C642100000000"
        )
        val intent = Intent(context, HexToAscii::class.java).apply {
            putExtra(DumpEditor.EXTRA_DUMP, sampleDump)
        }
        ActivityScenario.launch<HexToAscii>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse("HexToAscii should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun test20_LaunchKeyMapCreator() {
        val intent = Intent(context, KeyMapCreator::class.java).apply {
            putExtra(KeyMapCreator.EXTRA_KEYS_DIR, Common.getFile(Common.KEYS_DIR).absolutePath)
            putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER, true)
            putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_FROM, 0)
            putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_TO, 1)
        }
        ActivityScenario.launch<KeyMapCreator>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse("KeyMapCreator should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun test21_LaunchWriteTagWithDump() {
        val sampleDump = arrayOf(
            "+Sector: 0",
            "01020304040804000000000000000000",
            "00000000000000000000000000000000",
            "00000000000000000000000000000000",
            "A0B1C2D3E4F5FF078069FFFFFFFFFFFF"
        )
        val intent = Intent(context, WriteTag::class.java).apply {
            putExtra(WriteTag.EXTRA_DUMP, sampleDump)
        }
        ActivityScenario.launch<WriteTag>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse("WriteTag should not be finishing", activity.isFinishing)
            }
        }
    }

    @Test
    fun test22_NonNfcIntentHandling() {
        // Test handling of invalid/generic intent without NFC extra
        val dummyIntent = Intent("android.intent.action.VIEW")
        val result = Common.treatAsNewTag(dummyIntent, context)
        assertEquals("Non-NFC action should return -4", -4, result)
    }

    @Test
    fun test23_PreferencesPersistenceOnDevice() {
        val prefs = Common.getPreferences()
        val originalAutoReconnect = prefs.getBoolean(Preferences.Preference.AutoReconnect.toString(), false)

        prefs.edit().putBoolean(Preferences.Preference.AutoReconnect.toString(), !originalAutoReconnect).commit()
        val toggled = prefs.getBoolean(Preferences.Preference.AutoReconnect.toString(), false)
        assertEquals(!originalAutoReconnect, toggled)

        // Restore
        prefs.edit().putBoolean(Preferences.Preference.AutoReconnect.toString(), originalAutoReconnect).commit()
    }
}
