package de.syss.MifareClassicTool.ui.preferences

import androidx.core.os.LocaleListCompat

/**
 * Pure utility functions for Language & Locale resolution, bridging legacy
 * MCT SharedPreferences values, UI selection indices, and AndroidX
 * [AppCompatDelegate] / [LocaleListCompat].
 */
object LanguageHelper {

    /**
     * Extracts the language tag enclosed in parentheses, e.g.:
     * "English (en)" -> "en"
     * "正體中文 (zh-TW)" -> "zh-TW"
     * "跟随系统" -> null
     */
    fun extractLanguageTag(option: String): String? {
        val start = option.indexOf('(')
        val end = option.indexOf(')')
        return if (start != -1 && end != -1 && end > start + 1) {
            option.substring(start + 1, end).trim()
        } else {
            null
        }
    }

    /**
     * Maps a BCP-47 language tag (from [LocaleListCompat]) to MCT's legacy
     * custom_app_language index (0..6), or -1 if unknown / follow system.
     *
     * 0: English (en)
     * 1: Français (fr)
     * 2: Italiano (it)
     * 3: Portuguese (pt)
     * 4: Русский (ru)
     * 5: 简体中文 (zh)
     * 6: 正體中文 (zh-TW)
     */
    fun mapLocaleTagToLegacyIndex(tag: String?): Int {
        if (tag.isNullOrBlank()) return -1
        val cleanTag = tag.trim().lowercase()

        return when {
            cleanTag.startsWith("zh-tw") || cleanTag.startsWith("zh-hant") || cleanTag.startsWith("zh-hk") -> 6
            cleanTag.startsWith("zh") -> 5
            cleanTag.startsWith("ru") -> 4
            cleanTag.startsWith("pt") -> 3
            cleanTag.startsWith("it") -> 2
            cleanTag.startsWith("fr") -> 1
            cleanTag.startsWith("en") -> 0
            else -> -1
        }
    }

    /**
     * Maps MCT's legacy custom_app_language index (0..6) to a language code,
     * or null if follow system / invalid.
     */
    fun mapLegacyIndexToTag(index: Int): String? {
        return when (index) {
            0 -> "en"
            1 -> "fr"
            2 -> "it"
            3 -> "pt"
            4 -> "ru"
            5 -> "zh"
            6 -> "zh-TW"
            else -> null
        }
    }

    /**
     * Determines the active UI selection index (where 0 = Follow System, 1 = English, 2 = French,
     * ... 6 = Simplified Chinese, 7 = Traditional Chinese).
     *
     * @param currentLocales The currently applied per-app locales from AppCompatDelegate.
     * @param savedLegacyPref The persisted integer from SharedPreferences ("custom_app_language", default -1).
     * @return 0 for Follow System, or 1..7 for explicit languages.
     */
    fun resolveCurrentSelectionIndex(currentLocales: LocaleListCompat?, savedLegacyPref: Int): Int {
        if (currentLocales != null && !currentLocales.isEmpty) {
            val primaryLocale = currentLocales[0]
            val tag = primaryLocale?.toLanguageTag() ?: ""
            val legacyIndex = mapLocaleTagToLegacyIndex(tag)
            if (legacyIndex in 0..6) {
                return legacyIndex + 1
            }
        }

        // If AppCompat locales is empty: check if there was an explicit non-default legacy choice (1..6)
        // (Index 0 in legacy MCT was English, but was also returned on fresh installs when unset,
        // so index 0 without appLocales is treated as Follow System to prevent unwanted English lock-in).
        if (savedLegacyPref in 1..6) {
            return savedLegacyPref + 1
        }

        return 0 // Follow System
    }

    /**
     * Calculates the legacy preference integer and BCP-47 language tag to be saved.
     *
     * @param selectionIndex 0 for Follow System, 1..N for specific language.
     * @param languageOptions Full list of language display names (index 0 is Follow System).
     * @return Pair of (legacyPrefInt, languageTag). If Follow System, returns (-1, "").
     */
    fun calculateSaveState(selectionIndex: Int, languageOptions: List<String>): Pair<Int, String> {
        if (selectionIndex <= 0 || selectionIndex >= languageOptions.size) {
            return Pair(-1, "")
        }

        val legacyIndex = selectionIndex - 1
        val option = languageOptions[selectionIndex]
        val tag = extractLanguageTag(option) ?: (mapLegacyIndexToTag(legacyIndex) ?: "")
        return Pair(legacyIndex, tag)
    }
}
