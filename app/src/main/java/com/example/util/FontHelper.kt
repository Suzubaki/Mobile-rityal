package com.example.util

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

object FontHelper {
    fun getFontFamily(styleKey: String): FontFamily {
        return when (styleKey.uppercase()) {
            "SERIF", "SERIF_BOLD", "OLD_SLAVIC", "ANTIQUE" -> FontFamily.Serif
            "SANS_SERIF", "SANS_SERIF_BOLD", "BRUSOK" -> FontFamily.SansSerif
            "CURSIVE", "ITALIC", "CALLIGRAPHY" -> FontFamily.Cursive
            "MONOSPACE", "TECHNICAL", "GOTHIC" -> FontFamily.Monospace
            else -> FontFamily.Default
        }
    }

    fun getFontWeight(styleKey: String): FontWeight {
        return when (styleKey.uppercase()) {
            "SANS_SERIF_BOLD", "BRUSOK" -> FontWeight.ExtraBold
            "SERIF_BOLD", "OLD_SLAVIC" -> FontWeight.Bold
            "ANTIQUE" -> FontWeight.SemiBold
            "MONOSPACE", "TECHNICAL", "GOTHIC" -> FontWeight.Medium
            else -> FontWeight.Normal
        }
    }

    fun getFontStyle(styleKey: String): FontStyle {
        return when (styleKey.uppercase()) {
            "CURSIVE", "ITALIC", "CALLIGRAPHY" -> FontStyle.Italic
            else -> FontStyle.Normal
        }
    }

    fun getStyleDisplayName(styleKey: String): String {
        return when (styleKey.uppercase()) {
            "SERIF" -> "Классика с засечками (Serif)"
            "SANS_SERIF" -> "Прямой гротеск (Sans-Serif)"
            "SANS_SERIF_BOLD", "BRUSOK" -> "Брусковый жирный (Bold)"
            "OLD_SLAVIC" -> "Старославянский (Serif Bold)"
            "CURSIVE", "ITALIC", "CALLIGRAPHY" -> "Рукописный курсив (Cursive)"
            "ANTIQUE" -> "Антиква / Модерн"
            "MONOSPACE", "TECHNICAL", "GOTHIC" -> "Моноширинный (Monospace)"
            else -> "Стандартный"
        }
    }

    val availableStyles = listOf(
        "SERIF" to "Классика с засечками (Serif)",
        "SANS_SERIF" to "Прямой гротеск (Sans-Serif)",
        "SANS_SERIF_BOLD" to "Брусковый жирный (Bold)",
        "OLD_SLAVIC" to "Старославянский (Serif Bold)",
        "CURSIVE" to "Рукописный курсив (Cursive)",
        "ANTIQUE" to "Антиква / Модерн",
        "MONOSPACE" to "Моноширинный (Monospace)"
    )
}
