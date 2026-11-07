package com.example.teste.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryNavy,
    onPrimary = SurfaceWhite,
    secondary = SecondaryBlue,
    onSecondary = SurfaceWhite,
    tertiary = TertiarySky,
    background = AppBackground,
    onBackground = TextPrimaryDark,
    surface = SurfaceWhite,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondarySlate
)

@Composable
fun TesteTheme(
    content: @Composable () -> Unit
) {
    // O aplicativo utiliza superfícies claras padronizadas (Color.White / #F2F5FA),
    // garantindo contraste adequado nos campos de texto mesmo com o modo escuro do sistema ativo.
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
