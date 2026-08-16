package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private fun getBaseLightColorScheme(palette: ColorPalette): ColorScheme {
    return when (palette) {
        ColorPalette.EMERALD -> lightColorScheme(
            primary = EmeraldPrimary,
            onPrimary = EmeraldOnPrimary,
            primaryContainer = EmeraldPrimaryContainer,
            onPrimaryContainer = EmeraldOnPrimaryContainer,
            secondary = SlateSecondary,
            background = SlateBackground,
            surface = SlateSurface,
            surfaceVariant = SlateSurfaceVariant,
            outline = SlateOutline
        )
        ColorPalette.PURPLE -> lightColorScheme(
            primary = PurplePrimary,
            onPrimary = PurpleOnPrimary,
            primaryContainer = PurplePrimaryContainer,
            onPrimaryContainer = PurpleOnPrimaryContainer,
            secondary = SlateSecondary,
            background = SlateBackground,
            surface = SlateSurface,
            surfaceVariant = SlateSurfaceVariant,
            outline = SlateOutline
        )
        ColorPalette.AMBER -> lightColorScheme(
            primary = AmberPrimary,
            onPrimary = AmberOnPrimary,
            primaryContainer = AmberPrimaryContainer,
            onPrimaryContainer = AmberOnPrimaryContainer,
            secondary = SlateSecondary,
            background = SlateBackground,
            surface = SlateSurface,
            surfaceVariant = SlateSurfaceVariant,
            outline = SlateOutline
        )
        ColorPalette.TEAL -> lightColorScheme(
            primary = TealPrimary,
            onPrimary = TealOnPrimary,
            primaryContainer = TealPrimaryContainer,
            onPrimaryContainer = TealOnPrimaryContainer,
            secondary = SlateSecondary,
            background = SlateBackground,
            surface = SlateSurface,
            surfaceVariant = SlateSurfaceVariant,
            outline = SlateOutline
        )
        ColorPalette.CRIMSON -> lightColorScheme(
            primary = CrimsonPrimary,
            onPrimary = CrimsonOnPrimary,
            primaryContainer = CrimsonPrimaryContainer,
            onPrimaryContainer = CrimsonOnPrimaryContainer,
            secondary = SlateSecondary,
            background = SlateBackground,
            surface = SlateSurface,
            surfaceVariant = SlateSurfaceVariant,
            outline = SlateOutline
        )
        ColorPalette.MONOCHROME -> lightColorScheme(
            primary = MonoPrimary,
            onPrimary = MonoOnPrimary,
            primaryContainer = MonoPrimaryContainer,
            onPrimaryContainer = MonoOnPrimaryContainer,
            secondary = SlateSecondary,
            background = SlateBackground,
            surface = SlateSurface,
            surfaceVariant = SlateSurfaceVariant,
            outline = SlateOutline
        )
        else -> lightColorScheme(
            primary = SlateNavyPrimary,
            onPrimary = SlateNavyOnPrimary,
            primaryContainer = SlateNavyPrimaryContainer,
            onPrimaryContainer = SlateNavyOnPrimaryContainer,
            secondary = SlateSecondary,
            onSecondary = SlateOnSecondary,
            secondaryContainer = SlateSecondaryContainer,
            onSecondaryContainer = SlateOnSecondaryContainer,
            tertiary = WarmAmberTertiary,
            onTertiary = WarmAmberOnTertiary,
            tertiaryContainer = WarmAmberTertiaryContainer,
            onTertiaryContainer = WarmAmberOnTertiaryContainer,
            background = SlateBackground,
            onBackground = SlateOnBackground,
            surface = SlateSurface,
            onSurface = SlateOnSurface,
            surfaceVariant = SlateSurfaceVariant,
            onSurfaceVariant = SlateOnSurfaceVariant,
            outline = SlateOutline,
            outlineVariant = SlateOutlineVariant,
        )
    }
}

private fun getBaseDarkColorScheme(palette: ColorPalette): ColorScheme {
    return when (palette) {
        ColorPalette.EMERALD -> darkColorScheme(
            primary = EmeraldDarkPrimary,
            onPrimary = EmeraldDarkOnPrimary,
            primaryContainer = EmeraldDarkPrimaryContainer,
            onPrimaryContainer = EmeraldDarkOnPrimaryContainer,
            secondary = DarkSlateSecondary,
            background = DarkBackground,
            surface = DarkSurface,
            surfaceVariant = DarkSurfaceVariant,
            outline = DarkOutline
        )
        ColorPalette.PURPLE -> darkColorScheme(
            primary = PurpleDarkPrimary,
            onPrimary = PurpleDarkOnPrimary,
            primaryContainer = PurpleDarkPrimaryContainer,
            onPrimaryContainer = PurpleDarkOnPrimaryContainer,
            secondary = DarkSlateSecondary,
            background = DarkBackground,
            surface = DarkSurface,
            surfaceVariant = DarkSurfaceVariant,
            outline = DarkOutline
        )
        ColorPalette.AMBER -> darkColorScheme(
            primary = AmberDarkPrimary,
            onPrimary = AmberDarkOnPrimary,
            primaryContainer = AmberDarkPrimaryContainer,
            onPrimaryContainer = AmberDarkOnPrimaryContainer,
            secondary = DarkSlateSecondary,
            background = DarkBackground,
            surface = DarkSurface,
            surfaceVariant = DarkSurfaceVariant,
            outline = DarkOutline
        )
        ColorPalette.TEAL -> darkColorScheme(
            primary = TealDarkPrimary,
            onPrimary = TealDarkOnPrimary,
            primaryContainer = TealDarkPrimaryContainer,
            onPrimaryContainer = TealDarkOnPrimaryContainer,
            secondary = DarkSlateSecondary,
            background = DarkBackground,
            surface = DarkSurface,
            surfaceVariant = DarkSurfaceVariant,
            outline = DarkOutline
        )
        ColorPalette.CRIMSON -> darkColorScheme(
            primary = CrimsonDarkPrimary,
            onPrimary = CrimsonDarkOnPrimary,
            primaryContainer = CrimsonDarkPrimaryContainer,
            onPrimaryContainer = CrimsonDarkOnPrimaryContainer,
            secondary = DarkSlateSecondary,
            background = DarkBackground,
            surface = DarkSurface,
            surfaceVariant = DarkSurfaceVariant,
            outline = DarkOutline
        )
        ColorPalette.MONOCHROME -> darkColorScheme(
            primary = MonoDarkPrimary,
            onPrimary = MonoDarkOnPrimary,
            primaryContainer = MonoDarkPrimaryContainer,
            onPrimaryContainer = MonoDarkOnPrimaryContainer,
            secondary = DarkSlateSecondary,
            background = DarkBackground,
            surface = DarkSurface,
            surfaceVariant = DarkSurfaceVariant,
            outline = DarkOutline
        )
        else -> darkColorScheme(
            primary = DarkNavyPrimary,
            onPrimary = DarkNavyOnPrimary,
            primaryContainer = DarkNavyPrimaryContainer,
            onPrimaryContainer = DarkNavyOnPrimaryContainer,
            secondary = DarkSlateSecondary,
            onSecondary = DarkSlateOnSecondary,
            secondaryContainer = DarkSlateSecondaryContainer,
            onSecondaryContainer = DarkSlateOnSecondaryContainer,
            tertiary = DarkAmberTertiary,
            onTertiary = DarkAmberOnTertiary,
            tertiaryContainer = DarkAmberTertiaryContainer,
            onTertiaryContainer = DarkAmberOnTertiaryContainer,
            background = DarkBackground,
            onBackground = DarkOnBackground,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkOnSurfaceVariant,
            outline = DarkOutline,
            outlineVariant = DarkOutlineVariant,
        )
    }
}

private fun applyPureBlack(base: ColorScheme): ColorScheme {
    return base.copy(
        background = PureBlackBackground,
        surface = PureBlackSurface,
        surfaceVariant = PureBlackSurfaceVariant,
        outline = PureBlackOutline,
        outlineVariant = PureBlackOutlineVariant
    )
}

@Composable
fun MyApplicationTheme(
    themeState: ThemeState = ThemeState(),
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeState.themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.PURE_BLACK -> true
    }

    val context = LocalContext.current
    val isDynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useDynamic = themeState.colorPalette == ColorPalette.DYNAMIC && isDynamicAvailable

    var colorScheme: ColorScheme = when {
        useDynamic -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> getBaseDarkColorScheme(themeState.colorPalette)
        else -> getBaseLightColorScheme(themeState.colorPalette)
    }

    if (themeState.themeMode == ThemeMode.PURE_BLACK) {
        colorScheme = applyPureBlack(colorScheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
