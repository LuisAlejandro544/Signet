package com.example.ui.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.theme.ColorPalette
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemeState

/**
 * Encapsulates persistent user preferences such as theming and onboarding completion.
 */
class AppPreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun loadThemeState(): ThemeState {
        val savedModeName = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val savedPaletteName = prefs.getString(KEY_COLOR_PALETTE, ColorPalette.DYNAMIC.name) ?: ColorPalette.DYNAMIC.name

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
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun saveColorPalette(palette: ColorPalette) {
        prefs.edit().putString(KEY_COLOR_PALETTE, palette.name).apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    companion object {
        private const val PREFS_NAME = "keystore_generator_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_COLOR_PALETTE = "color_palette"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

        const val URL_TERMS = "https://signet-web.luisalejandrososacamacho9.workers.dev/terms/"
        const val URL_PRIVACY = "https://signet-web.luisalejandrososacamacho9.workers.dev/privacy/"
    }
}
