package com.example.ui.preferences

import android.content.Context
import com.example.ui.theme.ColorPalette
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemeState

/**
 * Encapsulates persistent user preferences such as theming and onboarding completion.
 * Supports both Android (SharedPreferences) and Windows/Desktop (.properties in AppData).
 */
class AppPreferencesManager(private val dataSource: PreferencesDataSource) {

    constructor(context: Context) : this(AndroidPreferencesDataSource(context, PREFS_NAME))

    constructor() : this(DesktopPreferencesDataSource())

    fun loadThemeState(): ThemeState {
        val savedModeName = dataSource.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        val savedPaletteName = dataSource.getString(KEY_COLOR_PALETTE, ColorPalette.DYNAMIC.name)

        val mode = try {
            ThemeMode.valueOf(savedModeName)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }

        val palette = try {
            ColorPalette.valueOf(savedPaletteName)
        } catch (_: Exception) {
            ColorPalette.DYNAMIC
        }

        return ThemeState(themeMode = mode, colorPalette = palette)
    }

    fun saveThemeMode(mode: ThemeMode) {
        dataSource.putString(KEY_THEME_MODE, mode.name)
    }

    fun saveColorPalette(palette: ColorPalette) {
        dataSource.putString(KEY_COLOR_PALETTE, palette.name)
    }

    fun isOnboardingCompleted(): Boolean {
        return dataSource.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        dataSource.putBoolean(KEY_ONBOARDING_COMPLETED, completed)
    }

    companion object {
        const val PREFS_NAME = "keystore_generator_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_COLOR_PALETTE = "color_palette"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

        const val URL_TERMS = "https://signet-web.luisalejandrososacamacho9.workers.dev/terms/"
        const val URL_PRIVACY = "https://signet-web.luisalejandrososacamacho9.workers.dev/privacy/"
    }
}
