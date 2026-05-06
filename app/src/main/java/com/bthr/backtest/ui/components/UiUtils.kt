package com.bthr.backtest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bthr.backtest.R

// ============================================================
// 1. DRAG HANDLE (POIGNÉE DE GLISSEMENT)
// ============================================================

@Composable
fun DragHandle(
    gripColor: Color,
    dotSize: Float = 1.8f,
    spacing: Float = 2.5f
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_drag_handle),
        contentDescription = "Drag Handle",
        tint = gripColor,
        modifier = Modifier.size(10.dp, 16.dp)
    )
}

// ============================================================
// 2. COULEURS DE THÈME
// ============================================================

data class ThemeColors(
    val bgColor: Color,
    val textColor: Color,
    val borderColor: Color,
    val cardBgColor: Color,
    val titleBgColor: Color,
    val closeBgColor: Color,
    val gripColor: Color
)

fun getDrawingToolsMenuColors(isDarkTheme: Boolean): ThemeColors {
    return if (isDarkTheme) {
        ThemeColors(
            bgColor = Color(0xFF1E222D),
            textColor = Color(0xFFE0E3EB),
            borderColor = Color(0xFF434651),
            cardBgColor = Color(0xFF272D3B),
            titleBgColor = Color(0xFF2A2E39),
            closeBgColor = Color(0xFF363A45),
            gripColor = Color.White.copy(alpha = 0.45f)
        )
    } else {
        ThemeColors(
            bgColor = Color.White,
            textColor = Color(0xFF131722),
            borderColor = Color(0xFFD1D4DC),
            cardBgColor = Color(0xFFF5F5F5),
            titleBgColor = Color(0xFFF5F5F5),
            closeBgColor = Color(0xFFEEEEEE),
            gripColor = Color.Black.copy(alpha = 0.4f)
        )
    }
}

data class PopupColors(
    val bgColor: Color,
    val borderColor: Color,
    val textColor: Color
)

fun getPopupColors(isDarkTheme: Boolean): PopupColors {
    return if (isDarkTheme) {
        PopupColors(
            bgColor = Color(0xFF2A2E39),
            borderColor = Color(0xFF363A45),
            textColor = Color.White
        )
    } else {
        PopupColors(
            bgColor = Color(0xFFFAFAFA),
            borderColor = Color(0xFFCCCCCC),
            textColor = Color.Black.copy(alpha = 0.85f)
        )
    }
}

data class FavoritesBarColors(
    val barBg: Color,
    val barBorder: Color,
    val grip: Color,
    val icon: Color,
    val active: Color
)

fun getFavoritesBarColors(isDarkChart: Boolean): FavoritesBarColors {
    return if (isDarkChart) {
        FavoritesBarColors(
            barBg = Color(0xFF1E222D).copy(alpha = 0.96f),
            barBorder = Color(0xFF434651).copy(alpha = 0.75f),
            grip = Color(0xFFAEB4C0).copy(alpha = 0.7f),
            icon = Color(0xFFE0E3EB).copy(alpha = 0.9f),
            active = Color(0xFF2962FF)
        )
    } else {
        FavoritesBarColors(
            barBg = Color(0xFFF5F5F5).copy(alpha = 0.96f),
            barBorder = Color(0xFFD0D0D0).copy(alpha = 0.7f),
            grip = Color(0xFF999999).copy(alpha = 0.7f),
            icon = Color(0xFF555555).copy(alpha = 0.85f),
            active = Color(0xFF2962FF)
        )
    }
}
