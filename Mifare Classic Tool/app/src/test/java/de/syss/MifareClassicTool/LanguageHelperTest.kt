package de.syss.MifareClassicTool

import androidx.core.os.LocaleListCompat
import de.syss.MifareClassicTool.ui.preferences.LanguageHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class LanguageHelperTest {

    private val sampleLanguageOptions = listOf(
        "跟随系统",
        "English (en)",
        "Français (fr)",
        "Italiano (it)",
        "Portuguese (pt)",
        "Русский (ru)",
        "简体中文 (zh)",
        "正體中文 (zh-TW)"
    )

    @Test
    fun testExtractLanguageTag() {
        assertEquals("en", LanguageHelper.extractLanguageTag("English (en)"))
        assertEquals("fr", LanguageHelper.extractLanguageTag("Français (fr)"))
        assertEquals("it", LanguageHelper.extractLanguageTag("Italiano (it)"))
        assertEquals("pt", LanguageHelper.extractLanguageTag("Portuguese (pt)"))
        assertEquals("ru", LanguageHelper.extractLanguageTag("Русский (ru)"))
        assertEquals("zh", LanguageHelper.extractLanguageTag("简体中文 (zh)"))
        assertEquals("zh-TW", LanguageHelper.extractLanguageTag("正體中文 (zh-TW)"))
        assertNull(LanguageHelper.extractLanguageTag("跟随系统"))
        assertNull(LanguageHelper.extractLanguageTag("Follow System"))
        assertNull(LanguageHelper.extractLanguageTag(""))
    }

    @Test
    fun testMapLocaleTagToLegacyIndex() {
        // English -> 0
        assertEquals(0, LanguageHelper.mapLocaleTagToLegacyIndex("en"))
        assertEquals(0, LanguageHelper.mapLocaleTagToLegacyIndex("en-US"))
        assertEquals(0, LanguageHelper.mapLocaleTagToLegacyIndex("en-GB"))

        // French -> 1
        assertEquals(1, LanguageHelper.mapLocaleTagToLegacyIndex("fr"))
        assertEquals(1, LanguageHelper.mapLocaleTagToLegacyIndex("fr-FR"))

        // Italian -> 2
        assertEquals(2, LanguageHelper.mapLocaleTagToLegacyIndex("it"))

        // Portuguese -> 3
        assertEquals(3, LanguageHelper.mapLocaleTagToLegacyIndex("pt"))
        assertEquals(3, LanguageHelper.mapLocaleTagToLegacyIndex("pt-BR"))

        // Russian -> 4
        assertEquals(4, LanguageHelper.mapLocaleTagToLegacyIndex("ru"))

        // Simplified Chinese -> 5
        assertEquals(5, LanguageHelper.mapLocaleTagToLegacyIndex("zh"))
        assertEquals(5, LanguageHelper.mapLocaleTagToLegacyIndex("zh-CN"))
        assertEquals(5, LanguageHelper.mapLocaleTagToLegacyIndex("zh-Hans"))
        assertEquals(5, LanguageHelper.mapLocaleTagToLegacyIndex("zh-Hans-CN"))

        // Traditional Chinese -> 6
        assertEquals(6, LanguageHelper.mapLocaleTagToLegacyIndex("zh-TW"))
        assertEquals(6, LanguageHelper.mapLocaleTagToLegacyIndex("zh-Hant"))
        assertEquals(6, LanguageHelper.mapLocaleTagToLegacyIndex("zh-Hant-TW"))
        assertEquals(6, LanguageHelper.mapLocaleTagToLegacyIndex("zh-HK"))

        // Unknown / null
        assertEquals(-1, LanguageHelper.mapLocaleTagToLegacyIndex("de"))
        assertEquals(-1, LanguageHelper.mapLocaleTagToLegacyIndex("es"))
        assertEquals(-1, LanguageHelper.mapLocaleTagToLegacyIndex(null))
        assertEquals(-1, LanguageHelper.mapLocaleTagToLegacyIndex(""))
    }

    @Test
    fun testMapLegacyIndexToTag() {
        assertEquals("en", LanguageHelper.mapLegacyIndexToTag(0))
        assertEquals("fr", LanguageHelper.mapLegacyIndexToTag(1))
        assertEquals("it", LanguageHelper.mapLegacyIndexToTag(2))
        assertEquals("pt", LanguageHelper.mapLegacyIndexToTag(3))
        assertEquals("ru", LanguageHelper.mapLegacyIndexToTag(4))
        assertEquals("zh", LanguageHelper.mapLegacyIndexToTag(5))
        assertEquals("zh-TW", LanguageHelper.mapLegacyIndexToTag(6))
        assertNull(LanguageHelper.mapLegacyIndexToTag(-1))
        assertNull(LanguageHelper.mapLegacyIndexToTag(99))
    }

    @Test
    fun testResolveCurrentSelectionIndexFromAppCompatLocales() {
        // When AppCompatDelegate has explicit locales set
        val zhLocales = LocaleListCompat.forLanguageTags("zh-CN")
        assertEquals(6, LanguageHelper.resolveCurrentSelectionIndex(zhLocales, -1))

        val enLocales = LocaleListCompat.forLanguageTags("en-US")
        assertEquals(1, LanguageHelper.resolveCurrentSelectionIndex(enLocales, -1))

        val twLocales = LocaleListCompat.forLanguageTags("zh-TW")
        assertEquals(7, LanguageHelper.resolveCurrentSelectionIndex(twLocales, -1))

        val frLocales = LocaleListCompat.forLanguageTags("fr")
        assertEquals(2, LanguageHelper.resolveCurrentSelectionIndex(frLocales, -1))
    }

    @Test
    fun testResolveCurrentSelectionIndexWhenLocalesEmpty() {
        val emptyLocales = LocaleListCompat.getEmptyLocaleList()

        // Fresh install / no previous preference -> Follow System (0)
        assertEquals(0, LanguageHelper.resolveCurrentSelectionIndex(emptyLocales, -1))

        // Legacy pref was 0 (default English in old MCT) but no appLocales -> Follow System (0)
        // (Prevents accidental English lock-in on fresh installs on Chinese phones!)
        assertEquals(0, LanguageHelper.resolveCurrentSelectionIndex(emptyLocales, 0))

        // Legacy pref was 5 (explicit Chinese in older MCT) -> 6 (Simplified Chinese)
        assertEquals(6, LanguageHelper.resolveCurrentSelectionIndex(emptyLocales, 5))

        // Legacy pref was 1 (explicit French in older MCT) -> 2 (French)
        assertEquals(2, LanguageHelper.resolveCurrentSelectionIndex(emptyLocales, 1))

        // Null locales check
        assertEquals(0, LanguageHelper.resolveCurrentSelectionIndex(null, -1))
    }

    @Test
    fun testCalculateSaveState() {
        // Follow System (index 0)
        val (prefFollow, tagFollow) = LanguageHelper.calculateSaveState(0, sampleLanguageOptions)
        assertEquals(-1, prefFollow)
        assertEquals("", tagFollow)

        // English (index 1)
        val (prefEn, tagEn) = LanguageHelper.calculateSaveState(1, sampleLanguageOptions)
        assertEquals(0, prefEn)
        assertEquals("en", tagEn)

        // Simplified Chinese (index 6)
        val (prefZh, tagZh) = LanguageHelper.calculateSaveState(6, sampleLanguageOptions)
        assertEquals(5, prefZh)
        assertEquals("zh", tagZh)

        // Traditional Chinese (index 7)
        val (prefTw, tagTw) = LanguageHelper.calculateSaveState(7, sampleLanguageOptions)
        assertEquals(6, prefTw)
        assertEquals("zh-TW", tagTw)

        // Invalid index
        val (prefInvalid, tagInvalid) = LanguageHelper.calculateSaveState(99, sampleLanguageOptions)
        assertEquals(-1, prefInvalid)
        assertEquals("", tagInvalid)
    }
}
