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

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import de.syss.MifareClassicTool.Common
import de.syss.MifareClassicTool.R
import java.util.ArrayList
import java.util.Arrays

/**
 * This view will let the user edit global preferences.
 * @author Gerhard Klostermeier
 */
class Preferences : BasicActivity() {

    /**
     * Enumeration with all preferences. This enumeration implements
     * "toString()" so it can be used to access the shared preferences.
     */
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

        override fun toString(): String {
            return text
        }
    }

    private lateinit var mPrefAutoReconnect: CheckBox
    private lateinit var mPrefAutoCopyUID: CheckBox
    private lateinit var mPrefSaveLastUsedKeyFiles: CheckBox
    private lateinit var mUseCustomSectorCount: CheckBox
    private lateinit var mUseRetryAuthentication: CheckBox
    private lateinit var mPrefAutostartIfCardDetected: CheckBox
    private lateinit var mCustomSectorCount: EditText
    private lateinit var mRetryAuthenticationCount: EditText
    private lateinit var mUIDFormatRadioGroup: RadioGroup
    private lateinit var mLangauge: Spinner
    private lateinit var mTheme: Spinner

    private lateinit var mPackageManager: PackageManager
    private lateinit var mComponentName: ComponentName

    /**
     * Initialize the preferences with the last stored ones.
     */
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)

        mPackageManager = applicationContext.packageManager
        mComponentName = ComponentName(packageName, "$packageName.MainMenuAlias")

        // Get preferences (init member variables).
        mPrefAutoReconnect = findViewById(R.id.checkBoxPreferencesAutoReconnect)
        mPrefAutoCopyUID = findViewById(R.id.checkBoxPreferencesCopyUID)
        mPrefSaveLastUsedKeyFiles = findViewById(R.id.checkBoxPreferencesSaveLastUsedKeyFiles)
        mUseCustomSectorCount = findViewById(R.id.checkBoxPreferencesUseCustomSectorCount)
        mCustomSectorCount = findViewById(R.id.editTextPreferencesCustomSectorCount)
        mPrefAutostartIfCardDetected = findViewById(R.id.checkBoxPreferencesAutostartIfCardDetected)
        mUseRetryAuthentication = findViewById(R.id.checkBoxPreferencesUseRetryAuthentication)
        mRetryAuthenticationCount = findViewById(R.id.editTextPreferencesRetryAuthenticationCount)
        mLangauge = findViewById(R.id.spinnerPreferencesLanguage)
        mTheme = findViewById(R.id.spinnerPreferencesTheme)

        // Assign the last stored values.
        val pref = Common.getPreferences()
        mPrefAutoReconnect.isChecked = pref.getBoolean(Preference.AutoReconnect.toString(), false)
        mPrefAutoCopyUID.isChecked = pref.getBoolean(Preference.AutoCopyUID.toString(), false)
        mUIDFormatRadioGroup = findViewById(R.id.radioGroupUIDFormat)
        setUIDFormatBySequence(pref.getInt(Preference.UIDFormat.toString(), 0))
        mPrefSaveLastUsedKeyFiles.isChecked = pref.getBoolean(Preference.SaveLastUsedKeyFiles.toString(), true)
        mUseCustomSectorCount.isChecked = pref.getBoolean(Preference.UseCustomSectorCount.toString(), false)
        mCustomSectorCount.isEnabled = mUseCustomSectorCount.isChecked
        mCustomSectorCount.setText("" + pref.getInt(Preference.CustomSectorCount.toString(), 16))
        mUseRetryAuthentication.isChecked = pref.getBoolean(Preference.UseRetryAuthentication.toString(), false)
        mRetryAuthenticationCount.isEnabled = mUseRetryAuthentication.isChecked
        mRetryAuthenticationCount.setText("" + pref.getInt(Preference.RetryAuthenticationCount.toString(), 1))
        detectAutostartIfCardDetectedState()
        getLanguageAndUpdateChooser()
        getThemeAndUpdateChooser()

        toggleUIDFormat(null)
    }

    /**
     * Get used language from MCTs own settings and set it in the custom app language chooser UI.
     */
    private fun getLanguageAndUpdateChooser() {
        val adapter = ArrayAdapter.createFromResource(
            this, R.array.supported_locales, android.R.layout.simple_spinner_dropdown_item
        )
        mLangauge.adapter = adapter
        val pref = Common.getPreferences()
        val langID = pref.getInt(Preference.CustomAppLanguage.toString(), 0)
        mLangauge.setSelection(langID)
    }

    /**
     * Get the used theme from MCTs own settings and set it in the custom app theme chooser UI.
     */
    private fun getThemeAndUpdateChooser() {
        val themeArray = resources.getStringArray(R.array.action_themes)
        val themeArrayList = ArrayList(Arrays.asList(*themeArray))
        val pref = Common.getPreferences()
        val themeID: Int = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            themeArrayList.removeAt(2)
            pref.getInt(Preference.CustomAppTheme.toString(), 0)
        } else {
            pref.getInt(Preference.CustomAppTheme.toString(), 2)
        }
        val adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, themeArrayList
        )
        mTheme.adapter = adapter
        mTheme.setSelection(themeID)
    }

    /**
     * Detect the current "Autostart if card is detected" state and set the checkbox accordingly.
     */
    @SuppressLint("SwitchIntDef")
    private fun detectAutostartIfCardDetectedState() {
        val enabledSetting = mPackageManager.getComponentEnabledSetting(mComponentName)
        when (enabledSetting) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> mPrefAutostartIfCardDetected.isChecked = true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> mPrefAutostartIfCardDetected.isChecked = false
        }
    }

    /**
     * Show information on the "auto reconnect" preference.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onShowAutoReconnectInfo(view: View) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_auto_reconnect_title)
            .setMessage(R.string.dialog_auto_reconnect)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton(R.string.action_ok) { _, _ -> }
            .show()
    }

    /**
     * Toggle the radio group for the copy UID format options.
     */
    @Suppress("UNUSED_PARAMETER")
    fun toggleUIDFormat(view: View?) {
        for (i in 0 until mUIDFormatRadioGroup.childCount) {
            mUIDFormatRadioGroup.getChildAt(i).isEnabled = mPrefAutoCopyUID.isChecked
        }
    }

    /**
     * Convenience method for converting selected radio item to an int.
     */
    private fun getUIDFormatSequence(): Int {
        return when (mUIDFormatRadioGroup.checkedRadioButtonId) {
            R.id.radioButtonHex -> 0
            R.id.radioButtonDecBE -> 1
            R.id.radioButtonDecLE -> 2
            else -> 0
        }
    }

    /**
     * Sets the correct radio, reverse of getUIDFormatSequence.
     */
    private fun setUIDFormatBySequence(seq: Int) {
        val rBID = when (seq) {
            2 -> R.id.radioButtonDecLE
            1 -> R.id.radioButtonDecBE
            else -> R.id.radioButtonHex
        }
        val selectRadioButton = findViewById<RadioButton>(rBID)
        selectRadioButton?.isChecked = true
    }

    /**
     * Enable or disable the custom sector count text box according to the checkbox state.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onUseCustomSectorCountChanged(view: View) {
        mCustomSectorCount.isEnabled = mUseCustomSectorCount.isChecked
    }

    /**
     * Enable or disable the retry authentication count text box according to the checkbox state.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onUseRetryAuthenticationChanged(view: View) {
        mRetryAuthenticationCount.isEnabled = mUseRetryAuthentication.isChecked
    }

    /**
     * Show information on the "use custom sector count" preference.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onShowCustomSectorCountInfo(view: View) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_custom_sector_count_title)
            .setMessage(R.string.dialog_custom_sector_count)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton(R.string.action_ok) { _, _ -> }
            .show()
    }

    /**
     * Show information on the "retry authentication" preference.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onShowRetryAuthenticationInfo(view: View) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_retry_authentication_title)
            .setMessage(R.string.dialog_retry_authentication)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton(R.string.action_ok) { _, _ -> }
            .show()
    }

    /**
     * Save the preferences.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onSave(view: View) {
        // Check if settings are valid.
        var error = false
        var customSectorCount = 16
        if (mUseCustomSectorCount.isChecked) {
            try {
                customSectorCount = mCustomSectorCount.text.toString().toInt()
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
        error = false
        var retryAuthenticationCount = 1
        if (mUseRetryAuthentication.isChecked) {
            try {
                retryAuthenticationCount = mRetryAuthenticationCount.text.toString().toInt()
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

        // Save preferences.
        val edit = Common.getPreferences().edit()
        edit.putBoolean(Preference.AutoReconnect.toString(), mPrefAutoReconnect.isChecked)
        edit.putBoolean(Preference.AutoCopyUID.toString(), mPrefAutoCopyUID.isChecked)
        edit.putInt(Preference.UIDFormat.toString(), getUIDFormatSequence())
        edit.putBoolean(Preference.SaveLastUsedKeyFiles.toString(), mPrefSaveLastUsedKeyFiles.isChecked)
        edit.putBoolean(Preference.UseCustomSectorCount.toString(), mUseCustomSectorCount.isChecked)
        edit.putBoolean(Preference.UseRetryAuthentication.toString(), mUseRetryAuthentication.isChecked)
        edit.putInt(Preference.CustomSectorCount.toString(), customSectorCount)
        edit.putInt(Preference.RetryAuthenticationCount.toString(), retryAuthenticationCount)
        edit.putInt(Preference.CustomAppLanguage.toString(), mLangauge.selectedItemId.toInt())
        edit.putInt(Preference.CustomAppTheme.toString(), mTheme.selectedItemId.toInt())
        edit.apply()

        // Update card detection on autostart.
        val newState = if (mPrefAutostartIfCardDetected.isChecked) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        mPackageManager.setComponentEnabledSetting(mComponentName, newState, PackageManager.DONT_KILL_APP)

        // Update language.
        val lang = mLangauge.selectedItem as String
        val langCode = lang.substring(lang.indexOf("(") + 1, lang.indexOf(")"))
        val appLocale = LocaleListCompat.forLanguageTags(langCode)
        AppCompatDelegate.setApplicationLocales(appLocale)

        // Update theme.
        when (mTheme.selectedItemId.toInt()) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        // Exit the preferences view.
        finish()
    }

    /**
     * Exit the preferences view without saving anything.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onCancel(view: View) {
        finish()
    }
}
