/*
 * Copyright 2014 Gerhard Klostermeier
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

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.LocaleListCompat
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.preferences.LanguageHelper
import de.syss.MifareClassicTool.ui.preferences.PreferencesScreen
import de.syss.MifareClassicTool.ui.preferences.PreferencesUiState
import de.syss.MifareClassicTool.ui.theme.MctTheme
import java.util.Arrays

/**
 * Preferences Activity refactored to Compose Material 3 (UI 2.0 KernelSU style).
 * 100% backward compatible with MCT's [Preference] keys, SharedPreferences,
 * language configuration, and night mode switching.
 */
class Preferences : BasicActivity() {

    enum class Preference(private val text: String) {
        AutoReconnect("auto_reconnect"),
        AutoCopyUID("auto_copy_uid"),
        UIDFormat("uid_format"),
        SaveLastUsedKeyFiles("save_last_used_key_files"),
        UseCustomSectorCount("use_custom_sector_count"),
        CustomSectorCount("custom_sector_count"),
        UseRetryAuthentication("use_retry_authentication"),
        RetryAuthenticationCount("retry_authentication_count"),
        CustomAppLanguage("custom_app_language"),
        CustomAppTheme("custom_app_theme");

        override fun toString(): String = text
    }

    private lateinit var mPackageManager: PackageManager
    private lateinit var mComponentName: ComponentName

    private val mUiState = mutableStateOf(PreferencesUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        mPackageManager = applicationContext.packageManager
        mComponentName = ComponentName(packageName, "$packageName.MainMenuAlias")

        loadInitialPreferences()

        setContent {
            MctTheme {
                PreferencesScreen(
                    state = mUiState.value,
                    onStateChange = { mUiState.value = it },
                    onSave = { savePreferences() },
                    onCancel = { finish() },
                    onShowAutoReconnectInfo = { showAutoReconnectInfo() },
                    onShowCustomSectorCountInfo = { showCustomSectorCountInfo() },
                    onShowRetryAuthenticationInfo = { showRetryAuthenticationInfo() }
                )
            }
        }
    }

    private fun loadInitialPreferences() {
        val pref = Common.getPreferences()
        val autoReconnect = pref.getBoolean(Preference.AutoReconnect.toString(), false)
        val autoCopyUID = pref.getBoolean(Preference.AutoCopyUID.toString(), false)
        val uidFormat = pref.getInt(Preference.UIDFormat.toString(), 0)
        val saveLastUsedKeyFiles = pref.getBoolean(Preference.SaveLastUsedKeyFiles.toString(), true)
        val useCustomSectorCount = pref.getBoolean(Preference.UseCustomSectorCount.toString(), false)
        val customSectorCount = "" + pref.getInt(Preference.CustomSectorCount.toString(), 16)
        val useRetryAuthentication = pref.getBoolean(Preference.UseRetryAuthentication.toString(), false)
        val retryAuthenticationCount = "" + pref.getInt(Preference.RetryAuthenticationCount.toString(), 1)

        val enabledSetting = mPackageManager.getComponentEnabledSetting(mComponentName)
        val autostartIfCardDetected = when (enabledSetting) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            else -> true
        }

        val themeArray = resources.getStringArray(R.array.action_themes)
        val themeArrayList = ArrayList(Arrays.asList(*themeArray))
        val themeID: Int = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            themeArrayList.removeAt(2)
            pref.getInt(Preference.CustomAppTheme.toString(), 0)
        } else {
            pref.getInt(Preference.CustomAppTheme.toString(), 2)
        }

        // Language resolution:
        // Option 0: Follow System ("跟随系统" / "Follow System")
        // Option 1..N: supported_locales from resources
        val rawLocales = resources.getStringArray(R.array.supported_locales).toList()
        val followSystemLabel = getString(R.string.text_language_follow_system)
        val langList = listOf(followSystemLabel) + rawLocales

        val savedPref = pref.getInt(Preference.CustomAppLanguage.toString(), -1)
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val langID = LanguageHelper.resolveCurrentSelectionIndex(currentLocales, savedPref)

        mUiState.value = PreferencesUiState(
            autoReconnect = autoReconnect,
            autoCopyUid = autoCopyUID,
            uidFormat = uidFormat,
            saveLastUsedKeyFiles = saveLastUsedKeyFiles,
            useCustomSectorCount = useCustomSectorCount,
            customSectorCount = customSectorCount,
            useRetryAuthentication = useRetryAuthentication,
            retryAuthenticationCount = retryAuthenticationCount,
            autostartIfCardDetected = autostartIfCardDetected,
            customAppLanguage = langID,
            customAppTheme = themeID,
            languageOptions = langList,
            themeOptions = themeArrayList
        )
    }

    private fun savePreferences() {
        val state = mUiState.value

        var customSectorCount = 16
        if (state.useCustomSectorCount) {
            var error = false
            try {
                customSectorCount = state.customSectorCount.trim().toInt()
            } catch (ex: NumberFormatException) {
                error = true
            }
            if (!error && (customSectorCount > 40 || customSectorCount <= 0)) {
                error = true
            }
            if (error) {
                Toast.makeText(this, R.string.info_sector_count_error, Toast.LENGTH_LONG).show()
                return
            }
        }

        var retryAuthenticationCount = 1
        if (state.useRetryAuthentication) {
            var error = false
            try {
                retryAuthenticationCount = state.retryAuthenticationCount.trim().toInt()
            } catch (ex: NumberFormatException) {
                error = true
            }
            if (!error && (retryAuthenticationCount > 1000 || retryAuthenticationCount <= 0)) {
                error = true
            }
            if (error) {
                Toast.makeText(this, R.string.info_retry_authentication_count_error, Toast.LENGTH_LONG).show()
                return
            }
        }

        val edit = Common.getPreferences().edit()
        edit.putBoolean(Preference.AutoReconnect.toString(), state.autoReconnect)
        edit.putBoolean(Preference.AutoCopyUID.toString(), state.autoCopyUid)
        edit.putInt(Preference.UIDFormat.toString(), state.uidFormat)
        edit.putBoolean(Preference.SaveLastUsedKeyFiles.toString(), state.saveLastUsedKeyFiles)
        edit.putBoolean(Preference.UseCustomSectorCount.toString(), state.useCustomSectorCount)
        edit.putBoolean(Preference.UseRetryAuthentication.toString(), state.useRetryAuthentication)
        edit.putInt(Preference.CustomSectorCount.toString(), customSectorCount)
        edit.putInt(Preference.RetryAuthenticationCount.toString(), retryAuthenticationCount)

        // Save Language with backward compatibility
        val (legacyPrefInt, langCode) = LanguageHelper.calculateSaveState(
            state.customAppLanguage,
            state.languageOptions
        )
        edit.putInt(Preference.CustomAppLanguage.toString(), legacyPrefInt)
        if (langCode.isEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode))
        }

        edit.putInt(Preference.CustomAppTheme.toString(), state.customAppTheme)
        edit.apply()

        val newState = if (state.autostartIfCardDetected) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        mPackageManager.setComponentEnabledSetting(mComponentName, newState, PackageManager.DONT_KILL_APP)

        // Update theme
        when (state.customAppTheme) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        finish()
    }

    private fun showAutoReconnectInfo() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_auto_reconnect_title)
            .setMessage(R.string.dialog_auto_reconnect)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton(R.string.action_ok) { _, _ -> }
            .show()
    }

    private fun showCustomSectorCountInfo() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_custom_sector_count_title)
            .setMessage(R.string.dialog_custom_sector_count)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton(R.string.action_ok) { _, _ -> }
            .show()
    }

    private fun showRetryAuthenticationInfo() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_retry_authentication_title)
            .setMessage(R.string.dialog_retry_authentication)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton(R.string.action_ok) { _, _ -> }
            .show()
    }

    // Legacy XML reflection stubs
    @Suppress("UNUSED_PARAMETER")
    fun onShowAutoReconnectInfo(view: View) = showAutoReconnectInfo()

    @Suppress("UNUSED_PARAMETER")
    fun onShowCustomSectorCountInfo(view: View) = showCustomSectorCountInfo()

    @Suppress("UNUSED_PARAMETER")
    fun onShowRetryAuthenticationInfo(view: View) = showRetryAuthenticationInfo()

    @Suppress("UNUSED_PARAMETER")
    fun onSave(view: View) = savePreferences()

    @Suppress("UNUSED_PARAMETER")
    fun onCancel(view: View) = finish()
}
