package io.github.gmathi.novellibrary.model.preference

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the per-novel display-setting accessors added by the
 * per-novel-reader-display-settings spec: dark theme, font path, text size, the four
 * reader-mode colors, limit image width, keep text color, alternative text colors, and merge
 * (cluster) pages.
 *
 * Mirrors [DataCenterReaderModeTest]'s approach: [DataCenter] requires a real Android `Context`
 * at construction time, which isn't available on the plain JVM unit test classpath. Each
 * accessor pair is a thin `SharedPreferences`-backed get/set keyed by a prefix + novel id,
 * falling back to the corresponding app-wide default when unset - so this test exercises that
 * exact pattern against [FakeSharedPreferences] rather than a real `DataCenter` instance.
 */
class DataCenterReaderDisplaySettingsTest {

    private companion object {
        private const val IS_DARK_THEME = "isDarkTheme"
        private const val IS_DARK_THEME_PER_NOVEL_PREFIX = "is_dark_theme_"

        private const val FONT_PATH = "fontPath"
        private const val FONT_PATH_PER_NOVEL_PREFIX = "font_path_"
        private const val DEFAULT_FONT_PATH = "/android_asset/fonts/default.ttf"

        private const val TEXT_SIZE = "textSize"
        private const val TEXT_SIZE_PER_NOVEL_PREFIX = "text_size_"

        private const val DAY_MODE_BACKGROUND_COLOR = "dayModeBackgroundColor"
        private const val DAY_BG_COLOR_PER_NOVEL_PREFIX = "day_bg_color_"
        private const val NIGHT_MODE_BACKGROUND_COLOR = "nightModeBackgroundColor"
        private const val NIGHT_BG_COLOR_PER_NOVEL_PREFIX = "night_bg_color_"
        private const val DAY_MODE_TEXT_COLOR = "dayModeTextColor"
        private const val DAY_TEXT_COLOR_PER_NOVEL_PREFIX = "day_text_color_"
        private const val NIGHT_MODE_TEXT_COLOR = "nightModeTextColor"
        private const val NIGHT_TEXT_COLOR_PER_NOVEL_PREFIX = "night_text_color_"

        private const val LIMIT_IMAGE_WIDTH = "limitImageWidth"
        private const val LIMIT_IMAGE_WIDTH_PER_NOVEL_PREFIX = "limit_image_width_"
        private const val KEEP_TEXT_COLOR = "keepTextColor"
        private const val KEEP_TEXT_COLOR_PER_NOVEL_PREFIX = "keep_text_color_"
        private const val ALTERNATIVE_TEXT_COLORS = "alternativeTextColors"
        private const val ALT_TEXT_COLORS_PER_NOVEL_PREFIX = "alt_text_colors_"
        private const val ENABLE_CLUSTER_PAGES = "enableClusterPages"
        private const val ENABLE_CLUSTER_PAGES_PER_NOVEL_PREFIX = "enable_cluster_pages_"
    }

    /** Mirrors the boolean per-novel accessors added to `DataCenter`. */
    private class FakeBooleanSettingHolder(private val prefs: SharedPreferences, private val appWideKey: String, private val perNovelPrefix: String, private val defaultValue: Boolean) {
        var appWideValue: Boolean
            get() = prefs.getBoolean(appWideKey, defaultValue)
            set(value) = prefs.edit().putBoolean(appWideKey, value).apply()

        fun getForNovel(novelId: Long): Boolean = prefs.getBoolean(perNovelPrefix + novelId, appWideValue)

        fun setForNovel(novelId: Long, value: Boolean) {
            prefs.edit().putBoolean(perNovelPrefix + novelId, value).apply()
        }
    }

    /** Mirrors the int per-novel accessors added to `DataCenter` (text size, colors). */
    private class FakeIntSettingHolder(private val prefs: SharedPreferences, private val appWideKey: String, private val perNovelPrefix: String, private val defaultValue: Int) {
        var appWideValue: Int
            get() = prefs.getInt(appWideKey, defaultValue)
            set(value) = prefs.edit().putInt(appWideKey, value).apply()

        fun getForNovel(novelId: Long): Int = prefs.getInt(perNovelPrefix + novelId, appWideValue)

        fun setForNovel(novelId: Long, value: Int) {
            prefs.edit().putInt(perNovelPrefix + novelId, value).apply()
        }
    }

    /** Mirrors `DataCenter.fontPath`/`getFontPathForNovel`/`setFontPathForNovel` (String, no self-heal). */
    private class FakeFontPathHolder(private val prefs: SharedPreferences) {
        var fontPath: String
            get() = prefs.getString(FONT_PATH, DEFAULT_FONT_PATH)!!
            set(value) = prefs.edit().putString(FONT_PATH, value).apply()

        fun getFontPathForNovel(novelId: Long): String =
            prefs.getString(FONT_PATH_PER_NOVEL_PREFIX + novelId, null) ?: fontPath

        fun setFontPathForNovel(novelId: Long, value: String) {
            prefs.edit().putString(FONT_PATH_PER_NOVEL_PREFIX + novelId, value).apply()
        }
    }

    // --- Generic boolean-setting behavior, exercised once per setting below ---

    private fun verifyBooleanSettingContract(holder: FakeBooleanSettingHolder, defaultValue: Boolean) {
        // Falls back to the app-wide default when unset.
        assertEquals(defaultValue, holder.getForNovel(1L))
        holder.appWideValue = !defaultValue
        assertEquals(!defaultValue, holder.getForNovel(1L))

        // Round-trips independent of the app-wide value.
        holder.setForNovel(42L, defaultValue)
        assertEquals(defaultValue, holder.getForNovel(42L))

        // Writing for one novel does not affect another novel or the app-wide default.
        holder.appWideValue = defaultValue
        holder.setForNovel(1L, !defaultValue)
        assertEquals(!defaultValue, holder.getForNovel(1L))
        assertEquals(defaultValue, holder.getForNovel(2L))
        assertEquals(defaultValue, holder.appWideValue)

        // Changing the app-wide default after a per-novel value is set does not affect that novel.
        holder.appWideValue = !defaultValue
        assertEquals(!defaultValue, holder.getForNovel(1L)) // unchanged, still the per-novel override
    }

    @Test
    fun `isDarkTheme per-novel accessor satisfies the standard contract`() {
        verifyBooleanSettingContract(FakeBooleanSettingHolder(FakeSharedPreferences(), IS_DARK_THEME, IS_DARK_THEME_PER_NOVEL_PREFIX, true), defaultValue = true)
    }

    @Test
    fun `limitImageWidth per-novel accessor satisfies the standard contract`() {
        verifyBooleanSettingContract(FakeBooleanSettingHolder(FakeSharedPreferences(), LIMIT_IMAGE_WIDTH, LIMIT_IMAGE_WIDTH_PER_NOVEL_PREFIX, false), defaultValue = false)
    }

    @Test
    fun `keepTextColor per-novel accessor satisfies the standard contract`() {
        verifyBooleanSettingContract(FakeBooleanSettingHolder(FakeSharedPreferences(), KEEP_TEXT_COLOR, KEEP_TEXT_COLOR_PER_NOVEL_PREFIX, false), defaultValue = false)
    }

    @Test
    fun `alternativeTextColors per-novel accessor satisfies the standard contract`() {
        verifyBooleanSettingContract(FakeBooleanSettingHolder(FakeSharedPreferences(), ALTERNATIVE_TEXT_COLORS, ALT_TEXT_COLORS_PER_NOVEL_PREFIX, false), defaultValue = false)
    }

    @Test
    fun `enableClusterPages per-novel accessor satisfies the standard contract`() {
        verifyBooleanSettingContract(FakeBooleanSettingHolder(FakeSharedPreferences(), ENABLE_CLUSTER_PAGES, ENABLE_CLUSTER_PAGES_PER_NOVEL_PREFIX, false), defaultValue = false)
    }

    // --- Generic int-setting behavior, exercised once per setting below ---

    private fun verifyIntSettingContract(holder: FakeIntSettingHolder, defaultValue: Int, otherValue: Int) {
        assertEquals(defaultValue, holder.getForNovel(1L))
        holder.appWideValue = otherValue
        assertEquals(otherValue, holder.getForNovel(1L))

        holder.setForNovel(42L, defaultValue)
        assertEquals(defaultValue, holder.getForNovel(42L))

        holder.appWideValue = defaultValue
        holder.setForNovel(1L, otherValue)
        assertEquals(otherValue, holder.getForNovel(1L))
        assertEquals(defaultValue, holder.getForNovel(2L))
        assertEquals(defaultValue, holder.appWideValue)

        holder.appWideValue = otherValue
        assertEquals(otherValue, holder.getForNovel(1L)) // unchanged from the per-novel override coincidentally matching
    }

    @Test
    fun `textSize per-novel accessor satisfies the standard contract`() {
        verifyIntSettingContract(FakeIntSettingHolder(FakeSharedPreferences(), TEXT_SIZE, TEXT_SIZE_PER_NOVEL_PREFIX, 0), defaultValue = 0, otherValue = 5)
    }

    @Test
    fun `dayModeBackgroundColor per-novel accessor satisfies the standard contract`() {
        verifyIntSettingContract(FakeIntSettingHolder(FakeSharedPreferences(), DAY_MODE_BACKGROUND_COLOR, DAY_BG_COLOR_PER_NOVEL_PREFIX, -0x1), defaultValue = -0x1, otherValue = -0x111112)
    }

    @Test
    fun `nightModeBackgroundColor per-novel accessor satisfies the standard contract`() {
        verifyIntSettingContract(FakeIntSettingHolder(FakeSharedPreferences(), NIGHT_MODE_BACKGROUND_COLOR, NIGHT_BG_COLOR_PER_NOVEL_PREFIX, -0x1000000), defaultValue = -0x1000000, otherValue = -0x654322)
    }

    @Test
    fun `dayModeTextColor per-novel accessor satisfies the standard contract`() {
        verifyIntSettingContract(FakeIntSettingHolder(FakeSharedPreferences(), DAY_MODE_TEXT_COLOR, DAY_TEXT_COLOR_PER_NOVEL_PREFIX, -0x1000000), defaultValue = -0x1000000, otherValue = -0x123457)
    }

    @Test
    fun `nightModeTextColor per-novel accessor satisfies the standard contract`() {
        verifyIntSettingContract(FakeIntSettingHolder(FakeSharedPreferences(), NIGHT_MODE_TEXT_COLOR, NIGHT_TEXT_COLOR_PER_NOVEL_PREFIX, -0x1), defaultValue = -0x1, otherValue = -0x765434)
    }

    // --- Font path (String, no self-heal) ---

    @Test
    fun `getFontPathForNovel returns fontPath default when unset`() {
        val holder = FakeFontPathHolder(FakeSharedPreferences())

        assertEquals(DEFAULT_FONT_PATH, holder.getFontPathForNovel(1L))

        holder.fontPath = "/android_asset/fonts/serif.ttf"

        assertEquals("/android_asset/fonts/serif.ttf", holder.getFontPathForNovel(1L))
    }

    @Test
    fun `setFontPathForNovel then getFontPathForNovel round-trips independent of fontPath default`() {
        val holder = FakeFontPathHolder(FakeSharedPreferences())
        holder.fontPath = "/android_asset/fonts/serif.ttf"

        holder.setFontPathForNovel(42L, "/android_asset/fonts/mono.ttf")

        assertEquals("/android_asset/fonts/mono.ttf", holder.getFontPathForNovel(42L))
        assertEquals("/android_asset/fonts/serif.ttf", holder.fontPath)
    }

    @Test
    fun `writing font path for one novel does not affect a different novel or the app-wide default`() {
        val holder = FakeFontPathHolder(FakeSharedPreferences())
        holder.fontPath = DEFAULT_FONT_PATH

        holder.setFontPathForNovel(1L, "/android_asset/fonts/mono.ttf")

        assertEquals("/android_asset/fonts/mono.ttf", holder.getFontPathForNovel(1L))
        assertEquals(DEFAULT_FONT_PATH, holder.getFontPathForNovel(2L))
        assertEquals(DEFAULT_FONT_PATH, holder.fontPath)
    }

    @Test
    fun `changing fontPath default after a per-novel value is set does not affect that novel`() {
        val holder = FakeFontPathHolder(FakeSharedPreferences())
        holder.fontPath = DEFAULT_FONT_PATH
        holder.setFontPathForNovel(7L, "/android_asset/fonts/mono.ttf")

        holder.fontPath = "/android_asset/fonts/serif.ttf"

        assertEquals("/android_asset/fonts/mono.ttf", holder.getFontPathForNovel(7L))
    }

    // --- Spot-check: per-novel writes never touch the app-wide property (Property 3) ---

    @Test
    fun `setIsDarkThemeForNovel-equivalent write does not modify the app-wide isDarkTheme default`() {
        val holder = FakeBooleanSettingHolder(FakeSharedPreferences(), IS_DARK_THEME, IS_DARK_THEME_PER_NOVEL_PREFIX, true)
        holder.appWideValue = true

        holder.setForNovel(1L, false)

        assertTrue(holder.appWideValue)
        assertFalse(holder.getForNovel(1L))
    }
}
