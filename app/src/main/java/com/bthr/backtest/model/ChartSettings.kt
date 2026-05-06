package com.bthr.backtest.model

import androidx.compose.ui.graphics.Color

enum class GridLines {
    BOTH, VERTICAL, HORIZONTAL, NONE
}

enum class BackgroundType {
    SOLID, GRADIENT
}

enum class VisibilityMode {
    ALWAYS_VISIBLE, ALWAYS_INVISIBLE, VISIBLE_ON_MOUSE_OVER
}

data class ChartSettings(
    // Symbol
    val upColor: Color = Color(0xFF26A69A),
    val downColor: Color = Color(0xFFEF5350),
    val volumeUpColor: Color = Color(0xFF26A69A).copy(alpha = 0.5f),
    val volumeDownColor: Color = Color(0xFFEF5350).copy(alpha = 0.5f),
    val bodyEnabled: Boolean = true,
    val bordersEnabled: Boolean = true,
    val upBorderColor: Color = Color(0xFF26A69A),
    val downBorderColor: Color = Color(0xFFEF5350),
    val wickEnabled: Boolean = true,
    val upWickColor: Color = Color(0xFF26A69A),
    val downWickColor: Color = Color(0xFFEF5350),
    val colorizeBarsBasedOnPrevClose: Boolean = false,
    val precision: Int = 2,
    val timezone: String = "UTC",

    // Scales and Lines
    val lockPriceBarRatio: Boolean = false,
    val priceBarRatioValue: String = "1.7035978",
    val scalePlacement: String = "Auto",
    val noOverlappingLabels: Boolean = true,
    val plusButtonEnabled: Boolean = false,
    val countdownToBarClose: Boolean = true,
    val lastPriceLineVisible: Boolean = true,
    val indicatorNameLabel: Boolean = true,
    val indicatorValueLabel: Boolean = true,
    val symbolLabelMode: String = "Valeur, droite",
    val prevDayCloseVisible: Boolean = false,
    val indicatorsLabelsMode: String = "Valeur",
    val highLowLabelsVisible: Boolean = false,
    val bidAskMode: String = "Droite",
    val dayOfWeekOnLabels: Boolean = true,
    val dateFormat: String = "lun. 29 Sept '97",
    val timeFormat: String = "24 heures",
    
    // Canvas settings
    val backgroundType: BackgroundType = BackgroundType.SOLID,
    val backgroundColor: Color = Color.White, // Default to light background
    val backgroundGradientColor: Color = Color.White,
    
    val showGrid: Boolean = true,
    val gridLines: GridLines = GridLines.BOTH,
    val verticalGridColor: Color = Color(0xFFF0F3FA),
    val horizontalGridColor: Color = Color(0xFFF0F3FA),
    val verticalGridThickness: Int = 1,
    val horizontalGridThickness: Int = 1,
    val verticalGridStyle: Int = 0, 
    val horizontalGridStyle: Int = 0,
    
    val sessionBreaksEnabled: Boolean = false,
    val sessionBreaksColor: Color = Color(0xFF2962FF).copy(alpha = 0.4f),
    val sessionBreaksThickness: Int = 1,
    val sessionBreaksStyle: Int = 1,
    
    val paneSeparatorColor: Color = Color(0xFFF0F3FA),
    
    // Mire par defaut proche du style TradingView de la capture: gris + pointille fin
    val crosshairColor: Color = Color(0xFF9094A1),
    val crosshairThickness: Int = 1,
    val crosshairStyle: Int = 1,
    
    val watermarkVisible: Boolean = true,
    val watermarkColor: Color = Color.Black.copy(alpha = 0.05f),
    
    // Scales appearance
    val scaleTextColor: Color = Color(0xFF131722),
    val scaleTextSize: Int = 10,
    val scaleLinesColor: Color = Color(0xFFF0F3FA),
    
    // Buttons settings
    val navigationButtonsMode: VisibilityMode = VisibilityMode.VISIBLE_ON_MOUSE_OVER,
    val paneButtonsMode: VisibilityMode = VisibilityMode.VISIBLE_ON_MOUSE_OVER,
    
    // Margins settings
    val marginTopPercent: Int = 10,
    val marginBottomPercent: Int = 8,
    val marginRightBars: Int = 10
) {
    companion object {
        fun dark() = ChartSettings(
            upColor = Color(0xFF26A69A),
            downColor = Color(0xFFEF5350),
            volumeUpColor = Color(0xFF26A69A).copy(alpha = 0.5f),
            volumeDownColor = Color(0xFFEF5350).copy(alpha = 0.5f),
            upBorderColor = Color(0xFF26A69A),
            downBorderColor = Color(0xFFEF5350),
            upWickColor = Color(0xFF26A69A),
            downWickColor = Color(0xFFEF5350),
            backgroundColor = Color(0xFF131722),
            verticalGridColor = Color(0xFF2A2E39),
            horizontalGridColor = Color(0xFF2A2E39),
            paneSeparatorColor = Color(0xFF2A2E39),
            crosshairColor = Color(0xFF9094A1),
            watermarkColor = Color.White.copy(alpha = 0.05f),
            scaleTextColor = Color(0xFFD1D4DC),
            scaleLinesColor = Color(0xFF2A2E39)
        )

        fun light() = ChartSettings(
            upColor = Color(0xFF26A69A),
            downColor = Color(0xFFEF5350),
            volumeUpColor = Color(0xFF26A69A).copy(alpha = 0.5f),
            volumeDownColor = Color(0xFFEF5350).copy(alpha = 0.5f),
            upBorderColor = Color(0xFF26A69A),
            downBorderColor = Color(0xFFEF5350),
            upWickColor = Color(0xFF26A69A),
            downWickColor = Color(0xFFEF5350),
            backgroundColor = Color.White,
            verticalGridColor = Color(0xFFF0F3FA),
            horizontalGridColor = Color(0xFFF0F3FA),
            paneSeparatorColor = Color(0xFFF0F3FA),
            crosshairColor = Color(0xFF9094A1),
            watermarkColor = Color.Black.copy(alpha = 0.05f),
            scaleTextColor = Color(0xFF131722),
            scaleLinesColor = Color(0xFFF0F3FA)
        )
    }
}
