/*
 * Copyright 2013 Gerhard Klostermeier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.syss.MifareClassicTool.Activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.AssetManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.text.HtmlCompat
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.main.MainMenuScreen
import de.syss.MifareClassicTool.ui.main.NfcHardwareState
import de.syss.MifareClassicTool.ui.theme.MctTheme
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Main App entry point showing the modernized Material 3 main menu.
 * Retains all original NFC lifecycle, onNewIntent, StartUpNode state machine,
 * and Activity dispatch logic.
 * @author Gerhard Klostermeier
 */
class MainMenu : AppCompatActivity() {

    companion object {
        private val LOG_TAG: String = MainMenu::class.java.simpleName
        private const val FILE_CHOOSER_DUMP_FILE: Int = 1
        private const val FILE_CHOOSER_KEY_FILE: Int = 2
    }

    private var mDonateDialogWasShown = false
    private var mInfoExternalNfcDialogWasShown = false
    private var mHasNoNfc = false
    private var mOldIntent: Intent? = null

    // Compose observable state
    private val mIsEditorOnlyState = mutableStateOf(false)
    private val mNfcState = mutableStateOf(NfcHardwareState.Ready)

    /**
     * Nodes (states) MCT passes through during its startup.
     */
    private enum class StartUpNode {
        FirstUseDialog, DonateDialog, HasNfc, HasMifareClassicSupport,
        HasNfcEnabled, HasExternalNfc, ExternalNfcServiceRunning,
        HandleNewIntent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme()
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        // Restore state.
        if (savedInstanceState != null) {
            mDonateDialogWasShown = savedInstanceState.getBoolean("donate_dialog_was_shown")
            mInfoExternalNfcDialogWasShown = savedInstanceState.getBoolean("info_external_nfc_dialog_was_shown")
            mHasNoNfc = savedInstanceState.getBoolean("has_no_nfc")
            mOldIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getParcelable("old_intent", Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                savedInstanceState.getParcelable("old_intent")
            }
        }

        mIsEditorOnlyState.value = Common.useAsEditorOnly()
        mNfcState.value = getNfcHardwareState()

        setContent {
            MctTheme {
                MainMenuScreen(
                    versionCode = Common.getVersionCode() ?: "",
                    isEditorOnly = mIsEditorOnlyState.value,
                    nfcState = mNfcState.value,
                    onReadTagClick = { onShowReadTag(null) },
                    onWriteTagClick = { onShowWriteTag(null) },
                    onOpenTagDumpEditorClick = { onOpenTagDumpEditor(null) },
                    onOpenKeyEditorClick = { onOpenKeyEditor(null) },
                    onHelpClick = { onShowHelp(null) },
                    onPreferencesClick = { onShowPreferences() },
                    onAboutClick = { onShowAboutDialog() },
                    onEnableNfcClick = { startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                    onTagInfoClick = { startActivity(Intent(this, TagInfoTool::class.java)) },
                    onCloneUidClick = { startActivity(Intent(this, CloneUidTool::class.java)) },
                    onDiffToolClick = { startActivity(Intent(this, DiffTool::class.java)) },
                    onAccessConditionToolClick = { startActivity(Intent(this, AccessConditionTool::class.java)) },
                    onValueBlockToolClick = { startActivity(Intent(this, ValueBlockTool::class.java)) },
                    onBccToolClick = { startActivity(Intent(this, BccTool::class.java)) },
                    onDataConversionToolClick = { startActivity(Intent(this, DataConversionTool::class.java)) },
                    onUidLogToolClick = { startActivity(Intent(this, UidLogTool::class.java)) },
                    onImportExportToolClick = { startActivity(Intent(this, ImportExportTool::class.java)) }
                )
            }
        }

        initFolders()
        copyStdKeysFiles()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("donate_dialog_was_shown", mDonateDialogWasShown)
        outState.putBoolean("info_external_nfc_dialog_was_shown", mInfoExternalNfcDialogWasShown)
        outState.putBoolean("has_no_nfc", mHasNoNfc)
        outState.putParcelable("old_intent", mOldIntent)
    }

    private fun getNfcHardwareState(): NfcHardwareState {
        val adapter = Common.getNfcAdapter() ?: NfcAdapter.getDefaultAdapter(this)
        return when {
            Common.useAsEditorOnly() -> NfcHardwareState.EditorOnly
            adapter == null -> NfcHardwareState.Unsupported
            !adapter.isEnabled -> NfcHardwareState.Disabled
            !Common.hasMifareClassicSupport() -> NfcHardwareState.Unsupported
            else -> NfcHardwareState.Ready
        }
    }

    private fun runStartUpNode(startUpNode: StartUpNode) {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val sharedEditor = sharedPref.edit()
        when (startUpNode) {
            StartUpNode.FirstUseDialog -> {
                val isFirstRun = sharedPref.getBoolean("is_first_run", true)
                if (isFirstRun) {
                    createFirstUseDialog().show()
                } else {
                    runStartUpNode(StartUpNode.HasNfc)
                }
            }
            StartUpNode.HasNfc -> {
                Common.setNfcAdapter(NfcAdapter.getDefaultAdapter(this))
                if (Common.getNfcAdapter() == null) {
                    mHasNoNfc = true
                    runStartUpNode(StartUpNode.HasExternalNfc)
                } else {
                    runStartUpNode(StartUpNode.HasMifareClassicSupport)
                }
            }
            StartUpNode.HasMifareClassicSupport -> {
                if (!Common.hasMifareClassicSupport() && !Common.useAsEditorOnly()) {
                    runStartUpNode(StartUpNode.HasExternalNfc)
                } else {
                    runStartUpNode(StartUpNode.HasNfcEnabled)
                }
            }
            StartUpNode.HasNfcEnabled -> {
                Common.setNfcAdapter(NfcAdapter.getDefaultAdapter(this))
                val nfcAdapter = Common.getNfcAdapter()
                if (nfcAdapter == null || !nfcAdapter.isEnabled) {
                    if (!Common.useAsEditorOnly()) {
                        createNfcEnableDialog().show()
                    } else {
                        runStartUpNode(StartUpNode.DonateDialog)
                    }
                } else {
                    useAsEditorOnly(false)
                    Common.enableNfcForegroundDispatch(this)
                    runStartUpNode(StartUpNode.DonateDialog)
                }
            }
            StartUpNode.HasExternalNfc -> {
                if (!Common.hasExternalNfcInstalled(this) && !Common.useAsEditorOnly()) {
                    if (mHasNoNfc) {
                        createInstallExternalNfcDialog().show()
                    } else {
                        val ad = createHasNoMifareClassicSupportDialog()
                        ad.show()
                        (ad.findViewById<TextView>(android.R.id.message))?.movementMethod =
                            LinkMovementMethod.getInstance()
                    }
                } else {
                    runStartUpNode(StartUpNode.ExternalNfcServiceRunning)
                }
            }
            StartUpNode.ExternalNfcServiceRunning -> {
                val isExternalNfcRunning = Common.isExternalNfcServiceRunning(this)
                if (isExternalNfcRunning == 0) {
                    if (!Common.useAsEditorOnly()) {
                        createStartExternalNfcServiceDialog().show()
                    } else {
                        runStartUpNode(StartUpNode.DonateDialog)
                    }
                } else if (isExternalNfcRunning == 1) {
                    useAsEditorOnly(false)
                    runStartUpNode(StartUpNode.DonateDialog)
                } else {
                    if (!Common.useAsEditorOnly() && !mInfoExternalNfcDialogWasShown) {
                        createInfoExternalNfcServiceDialog().show()
                        mInfoExternalNfcDialogWasShown = true
                    } else {
                        runStartUpNode(StartUpNode.DonateDialog)
                    }
                }
            }
            StartUpNode.DonateDialog -> {
                if (Common.IS_DONATE_VERSION || mDonateDialogWasShown) {
                    runStartUpNode(StartUpNode.HandleNewIntent)
                    return
                }
                var currentVersion = 0
                try {
                    currentVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PackageInfoCompat.getLongVersionCode(
                            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                        ).toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        PackageInfoCompat.getLongVersionCode(
                            packageManager.getPackageInfo(packageName, 0)
                        ).toInt()
                    }
                } catch (e: NameNotFoundException) {
                    Log.d(LOG_TAG, "Version not found.")
                }
                val lastVersion = sharedPref.getInt("mct_version", currentVersion - 1)
                val showDonateDialog = sharedPref.getBoolean("show_donate_dialog", true)

                if (lastVersion < currentVersion || showDonateDialog) {
                    if (lastVersion < currentVersion) {
                        sharedEditor.putInt("mct_version", currentVersion)
                        sharedEditor.putBoolean("show_donate_dialog", true)
                        sharedEditor.apply()
                    }
                    createDonateDialog().show()
                    mDonateDialogWasShown = true
                } else {
                    runStartUpNode(StartUpNode.HandleNewIntent)
                }
            }
            StartUpNode.HandleNewIntent -> {
                Common.setPendingComponentName(null)
                val intent = intent
                if (intent != null && intent.action != null) {
                    val isIntentWithTag = intent.action == NfcAdapter.ACTION_TECH_DISCOVERED
                    if (isIntentWithTag && intent != mOldIntent) {
                        mOldIntent = intent
                        onNewIntent(intent)
                    }
                }
            }
        }
    }

    private fun useAsEditorOnly(useAsEditorOnly: Boolean) {
        Common.setUseAsEditorOnly(useAsEditorOnly)
        mIsEditorOnlyState.value = useAsEditorOnly
        mNfcState.value = getNfcHardwareState()
    }

    private fun setTheme() {
        val pref = Common.getPreferences()
        val themeID: Int = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            pref.getInt(Preferences.Preference.CustomAppTheme.toString(), 0)
        } else {
            pref.getInt(Preferences.Preference.CustomAppTheme.toString(), 2)
        }
        when (themeID) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun createFirstUseDialog(): AlertDialog {
        return AlertDialog.Builder(this)
            .setTitle(R.string.dialog_first_run_title)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setMessage(R.string.dialog_first_run)
            .setPositiveButton(R.string.action_ok) { dialog, _ -> dialog.cancel() }
            .setOnCancelListener {
                val sharedPref = getPreferences(Context.MODE_PRIVATE)
                val sharedEditor = sharedPref.edit()
                sharedEditor.putBoolean("is_first_run", false)
                sharedEditor.apply()
                runStartUpNode(StartUpNode.HasNfc)
            }
            .create()
    }

    private fun createHasNoMifareClassicSupportDialog(): AlertDialog {
        val styledText = HtmlCompat.fromHtml(
            getString(R.string.dialog_no_mfc_support_device),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        return AlertDialog.Builder(this)
            .setTitle(R.string.dialog_no_mfc_support_device_title)
            .setMessage(styledText)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(R.string.action_install_external_nfc) { _, _ ->
                val uri = Uri.parse("market://details?id=eu.dedb.nfc.service")
                val goToMarket = Intent(Intent.ACTION_VIEW, uri)
                try {
                    startActivity(goToMarket)
                } catch (e: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=eu.dedb.nfc.service")
                        )
                    )
                }
            }
            .setNeutralButton(R.string.action_editor_only) { _, _ ->
                useAsEditorOnly(true)
                runStartUpNode(StartUpNode.DonateDialog)
            }
            .setNegativeButton(R.string.action_exit_app) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .create()
    }

    private fun createNfcEnableDialog(): AlertDialog {
        return AlertDialog.Builder(this)
            .setTitle(R.string.dialog_nfc_not_enabled_title)
            .setMessage(R.string.dialog_nfc_not_enabled)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton(R.string.action_nfc) { _, _ ->
                startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
            }
            .setNeutralButton(R.string.action_editor_only) { _, _ ->
                useAsEditorOnly(true)
                runStartUpNode(StartUpNode.DonateDialog)
            }
            .setNegativeButton(R.string.action_exit_app) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .create()
    }

    private fun createInstallExternalNfcDialog(): AlertDialog {
        return AlertDialog.Builder(this)
            .setTitle(R.string.dialog_no_nfc_support_title)
            .setMessage(R.string.dialog_no_nfc_support)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(R.string.action_install_external_nfc) { _, _ ->
                val uri = Uri.parse("market://details?id=eu.dedb.nfc.service")
                val goToMarket = Intent(Intent.ACTION_VIEW, uri)
                try {
                    startActivity(goToMarket)
                } catch (e: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=eu.dedb.nfc.service")
                        )
                    )
                }
            }
            .setNeutralButton(R.string.action_editor_only) { _, _ ->
                useAsEditorOnly(true)
                runStartUpNode(StartUpNode.DonateDialog)
            }
            .setNegativeButton(R.string.action_exit_app) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .create()
    }

    private fun createStartExternalNfcServiceDialog(): AlertDialog {
        val context = this
        return AlertDialog.Builder(this)
            .setTitle(R.string.dialog_start_external_nfc_title)
            .setMessage(R.string.dialog_start_external_nfc)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(R.string.action_start_external_nfc) { _, _ ->
                useAsEditorOnly(true)
                Common.openApp(context, "eu.dedb.nfc.service")
            }
            .setNeutralButton(R.string.action_editor_only) { _, _ ->
                useAsEditorOnly(true)
                runStartUpNode(StartUpNode.DonateDialog)
            }
            .setNegativeButton(R.string.action_exit_app) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .create()
    }

    private fun createInfoExternalNfcServiceDialog(): AlertDialog {
        val context = this
        return AlertDialog.Builder(this)
            .setTitle(R.string.dialog_info_external_nfc_title)
            .setMessage(R.string.dialog_info_external_nfc)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(R.string.action_external_nfc_is_running) { _, _ ->
                runStartUpNode(StartUpNode.DonateDialog)
            }
            .setNeutralButton(R.string.action_start_external_nfc) { _, _ ->
                Common.openApp(context, "eu.dedb.nfc.service")
            }
            .setNegativeButton(R.string.action_editor_only) { _, _ ->
                useAsEditorOnly(true)
                runStartUpNode(StartUpNode.DonateDialog)
            }
            .setOnCancelListener {
                useAsEditorOnly(true)
                runStartUpNode(StartUpNode.DonateDialog)
            }
            .create()
    }

    private fun createDonateDialog(): AlertDialog {
        val dialogLayout = layoutInflater.inflate(
            R.layout.dialog_donate,
            findViewById(android.R.id.content), false
        )
        val textView = dialogLayout.findViewById<TextView>(R.id.textViewDonateDialog)
        textView.movementMethod = LinkMovementMethod.getInstance()
        val showDonateDialogCheckBox = dialogLayout.findViewById<CheckBox>(R.id.checkBoxDonateDialog)
        return AlertDialog.Builder(this)
            .setTitle(R.string.dialog_donate_title)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setView(dialogLayout)
            .setPositiveButton(R.string.action_ok) { dialog, _ -> dialog.cancel() }
            .setOnCancelListener {
                if (showDonateDialogCheckBox.isChecked) {
                    val sharedPref = getPreferences(Context.MODE_PRIVATE)
                    val sharedEditor = sharedPref.edit()
                    sharedEditor.putBoolean("show_donate_dialog", false)
                    sharedEditor.apply()
                }
                runStartUpNode(StartUpNode.HandleNewIntent)
            }
            .create()
    }

    @SuppressLint("ApplySharedPref")
    private fun initFolders() {
        var path = Common.getFile(Common.KEYS_DIR)
        if (!path.exists() && !path.mkdirs()) {
            Log.e(LOG_TAG, "Error while creating '" + Common.HOME_DIR + "/" + Common.KEYS_DIR + "' directory.")
            return
        }

        path = Common.getFile(Common.DUMPS_DIR)
        if (!path.exists() && !path.mkdirs()) {
            Log.e(LOG_TAG, "Error while creating '" + Common.HOME_DIR + "/" + Common.DUMPS_DIR + "' directory.")
            return
        }

        path = Common.getFile(Common.TMP_DIR)
        if (!path.exists() && !path.mkdirs()) {
            Log.e(LOG_TAG, "Error while creating '" + Common.HOME_DIR + Common.TMP_DIR + "' directory.")
            return
        }
        val tmpFiles = path.listFiles()
        if (tmpFiles != null) {
            for (file in tmpFiles) {
                file.delete()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu_functions, menu)
        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater = menuInflater
        menu.setHeaderTitle(R.string.dialog_tools_menu_title)
        menu.setHeaderIcon(android.R.drawable.ic_menu_preferences)
        inflater.inflate(R.menu.tools, menu)
        menu.findItem(R.id.menuMainTagInfo).isEnabled = !Common.useAsEditorOnly()
        menu.findItem(R.id.menuMainCloneUidTool).isEnabled = !Common.useAsEditorOnly()
    }

    override fun onResume() {
        super.onResume()
        useAsEditorOnly(Common.useAsEditorOnly())
        mNfcState.value = getNfcHardwareState()
        runStartUpNode(StartUpNode.FirstUseDialog)
    }

    override fun onPause() {
        super.onPause()
        Common.disableNfcForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Common.getPendingComponentName() != null) {
            intent.component = Common.getPendingComponentName()
            startActivity(intent)
        } else {
            val typeCheck = Common.treatAsNewTag(intent, this)
            if (typeCheck == -1 || typeCheck == -2) {
                val i = Intent(this, TagInfoTool::class.java)
                startActivity(i)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onShowReadTag(view: View? = null) {
        val intent = Intent(this, ReadTag::class.java)
        startActivity(intent)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onShowWriteTag(view: View? = null) {
        val intent = Intent(this, WriteTag::class.java)
        startActivity(intent)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onShowHelp(view: View? = null) {
        val intent = Intent(this, HelpAndInfo::class.java)
        startActivity(intent)
    }

    fun onShowTools(view: View? = null) {
        if (view != null) {
            openContextMenu(view)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onOpenTagDumpEditor(view: View? = null) {
        val file = Common.getFile(Common.DUMPS_DIR)
        val files = file.listFiles()
        if (file.isDirectory && (files == null || files.isEmpty())) {
            Toast.makeText(this, R.string.info_no_dumps, Toast.LENGTH_LONG).show()
        }
        val intent = Intent(this, FileChooser::class.java)
        intent.putExtra(FileChooser.EXTRA_DIR, file.absolutePath)
        intent.putExtra(FileChooser.EXTRA_TITLE, getString(R.string.text_open_dump_title))
        intent.putExtra(FileChooser.EXTRA_BUTTON_TEXT, getString(R.string.action_open_dump_file))
        @Suppress("DEPRECATION")
        startActivityForResult(intent, FILE_CHOOSER_DUMP_FILE)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onOpenKeyEditor(view: View? = null) {
        val intent = Intent(this, FileChooser::class.java)
        intent.putExtra(FileChooser.EXTRA_DIR, Common.getFile(Common.KEYS_DIR).absolutePath)
        intent.putExtra(FileChooser.EXTRA_TITLE, getString(R.string.text_open_key_file_title))
        intent.putExtra(FileChooser.EXTRA_BUTTON_TEXT, getString(R.string.action_open_key_file))
        intent.putExtra(FileChooser.EXTRA_ALLOW_NEW_FILE, true)
        @Suppress("DEPRECATION")
        startActivityForResult(intent, FILE_CHOOSER_KEY_FILE)
    }

    private fun onShowPreferences() {
        val intent = Intent(this, Preferences::class.java)
        startActivity(intent)
    }

    private fun onShowAboutDialog() {
        val styledText = HtmlCompat.fromHtml(
            getString(R.string.dialog_about_mct, Common.getVersionCode()),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        val ad = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_about_mct_title)
            .setMessage(styledText)
            .setIcon(R.mipmap.ic_launcher)
            .setPositiveButton(R.string.action_ok) { _, _ -> }
            .create()
        ad.show()
        (ad.findViewById<TextView>(android.R.id.message))?.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuMainPreferences -> {
                onShowPreferences()
                true
            }
            R.id.menuMainAbout -> {
                onShowAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val intent: Intent
        when (item.itemId) {
            R.id.menuMainTagInfo -> {
                intent = Intent(this, TagInfoTool::class.java)
                startActivity(intent)
                return true
            }
            R.id.menuMainValueBlockTool -> {
                intent = Intent(this, ValueBlockTool::class.java)
                startActivity(intent)
                return true
            }
            R.id.menuMainAccessConditionTool -> {
                intent = Intent(this, AccessConditionTool::class.java)
                startActivity(intent)
                return true
            }
            R.id.menuMainDiffTool -> {
                intent = Intent(this, DiffTool::class.java)
                startActivity(intent)
                return true
            }
            R.id.menuMainBccTool -> {
                intent = Intent(this, BccTool::class.java)
                startActivity(intent)
                return true
            }
            R.id.menuMainCloneUidTool -> {
                intent = Intent(this, CloneUidTool::class.java)
                startActivity(intent)
                return true
            }
            R.id.menuMainImportExportTool -> {
                intent = Intent(this, ImportExportTool::class.java)
                startActivity(intent)
                return true
            }
            R.id.menuMainUidLogTool -> {
                intent = Intent(this, UidLogTool::class.java)
                startActivity(intent)
                return true
            }
            R.id.menuMainDataConversionTool -> {
                intent = Intent(this, DataConversionTool::class.java)
                startActivity(intent)
                return true
            }
            else -> return super.onContextItemSelected(item)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FILE_CHOOSER_DUMP_FILE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val intent = Intent(this, DumpEditor::class.java)
                    intent.putExtra(FileChooser.EXTRA_CHOSEN_FILE, data.getStringExtra(FileChooser.EXTRA_CHOSEN_FILE))
                    startActivity(intent)
                }
            }
            FILE_CHOOSER_KEY_FILE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val intent = Intent(this, KeyEditor::class.java)
                    intent.putExtra(FileChooser.EXTRA_CHOSEN_FILE, data.getStringExtra(FileChooser.EXTRA_CHOSEN_FILE))
                    startActivity(intent)
                }
            }
        }
    }

    private fun copyStdKeysFiles() {
        val assetManager = assets
        try {
            val keyList = assetManager.list(Common.KEYS_DIR) ?: return
            for (file in keyList) {
                val filePath = Common.KEYS_DIR + "/" + file
                val `in` = assetManager.open(filePath)
                val out: OutputStream = FileOutputStream(Common.getFile(filePath))
                Common.copyFile(`in`, out)
                `in`.close()
                out.flush()
                out.close()
            }
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Error while copying files from assets.")
        }
    }
}
