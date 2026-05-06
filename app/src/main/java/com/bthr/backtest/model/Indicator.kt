package com.bthr.backtest.model

import androidx.compose.ui.graphics.Color

sealed class Indicator(
    open val id: String = java.util.UUID.randomUUID().toString(),
    open var isVisible: Boolean = true
) {
    abstract val name: String

    data class Volume(
        val useMainSymbol: Boolean = true,
        val otherSymbol: String = "",
        val maLength: Int = 20,
        val maType: String = "SMA",
        val colorBasedOnPreviousClose: Boolean = false,
        val smoothingLine: String = "SMA",
        val smoothingLength: Int = 9,
        val showInputsInStatusLine: Boolean = true,
        val showVolume: Boolean = true,
        val upColor: Color = Color(0xFF26A69A),
        val downColor: Color = Color(0xFFEF5350),
        val upOpacity: Float = 0.5f,
        val downOpacity: Float = 0.5f,
        val showVolumeMa: Boolean = false,
        val volumeMaColor: Color = Color(0xFF2196F3),
        val volumeMaThickness: Float = 1f,
        val volumeMaStyle: Int = 0,
        val showSmoothedMa: Boolean = false,
        val smoothedMaColor: Color = Color(0xFF2196F3),
        val smoothedMaThickness: Float = 1f,
        val smoothedMaStyle: Int = 0,
        override var isVisible: Boolean = true,
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : Indicator(id, isVisible) {
        override val name: String get() = "Volume"
    }

    data class SMA(
        val period: Int = 14,
        val source: String = "Close",
        val color: Color = Color(0xFF2196F3),
        val thickness: Float = 1f,
        val style: Int = 0,
        override var isVisible: Boolean = true,
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : Indicator(id, isVisible) {
        override val name: String get() = "SMA ($period, $source)"
    }

    data class EMA(
        val period: Int = 9,
        val source: String = "Close",
        val color: Color = Color(0xFFE91E63),
        val thickness: Float = 1f,
        val style: Int = 0,
        override var isVisible: Boolean = true,
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : Indicator(id, isVisible) {
        override val name: String get() = "EMA ($period, $source)"
    }

    data class HMA(
        val period: Int = 9,
        val source: String = "Close",
        val color: Color = Color(0xFF00BCD4),
        val thickness: Float = 1f,
        val style: Int = 0,
        override var isVisible: Boolean = true,
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : Indicator(id, isVisible) {
        override val name: String get() = "HMA ($period, $source)"
    }

    data class BollingerBands(
        val period: Int = 20,
        val stdDev: Float = 2f,
        val middleColor: Color = Color(0xFFFF9800),
        val upperColor: Color = Color(0xFF4CAF50),
        val lowerColor: Color = Color(0xFFF44336),
        val middleThickness: Float = 1f,
        val upperThickness: Float = 1f,
        val lowerThickness: Float = 1f,
        val middleStyle: Int = 0,
        val upperStyle: Int = 0,
        val lowerStyle: Int = 0,
        val fillVisible: Boolean = true,
        val fillColor: Color = Color(0x222196F3),
        override var isVisible: Boolean = true,
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : Indicator(id, isVisible) {
        override val name: String get() = "Bollinger ($period, $stdDev)"
    }

    data class ATRBands(
        val period: Int = 3,
        val multiplier: Float = 2.5f,
        val source: String = "Close",
        val upperColor: Color = Color(0xFF4CAF50),
        val lowerColor: Color = Color(0xFFF44336),
        val upperThickness: Float = 2f,
        val lowerThickness: Float = 2f,
        val upperStyle: Int = 0,
        val lowerStyle: Int = 0,
        val showTPBands: Boolean = false,
        val tpScaleFactor: Float = 1.5f,
        val tpUpperColor: Color = Color(0xCCFFFFFF),
        val tpLowerColor: Color = Color(0xCCFFFF00),
        val tpThickness: Float = 1f,
        override var isVisible: Boolean = true,
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : Indicator(id, isVisible) {
        override val name: String get() = "ATR Bands ($period, $multiplier)"
    }

    data class RSI(
        val period: Int = 14,
        val source: String = "Close",
        val color: Color = Color(0xFF9C27B0),
        val thickness: Float = 1f,
        val style: Int = 0,
        val showMa: Boolean = false,
        val maType: String = "SMA",
        val maPeriod: Int = 14,
        val maColor: Color = Color(0xFF64B5F6),
        val maThickness: Float = 1f,
        val maStyle: Int = 0,
        val upperLevel: Int = 70,
        val middleLevel: Int = 50,
        val lowerLevel: Int = 30,
        val upperLevelColor: Color = Color(0xFF787B86),
        val middleLevelColor: Color = Color(0xFF787B86),
        val lowerLevelColor: Color = Color(0xFF787B86),
        val upperLevelVisible: Boolean = true,
        val middleLevelVisible: Boolean = true,
        val lowerLevelVisible: Boolean = true,
        val backgroundVisible: Boolean = true,
        val backgroundColor: Color = Color(0x1A9C27B0),
        val showRsi: Boolean = true,
        val showLabelsOnPriceScale: Boolean = true,
        val precision: Int = 2,
        override var isVisible: Boolean = true,
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : Indicator(id, isVisible) {
        override val name: String get() = "RSI ($period)"
    }

    data class MACD(
        val fastPeriod: Int = 12,
        val slowPeriod: Int = 26,
        val signalPeriod: Int = 9,
        val macdColor: Color = Color(0xFF2196F3),
        val macdThickness: Float = 1f,
        val macdStyle: Int = 0,
        val signalColor: Color = Color(0xFFFF5252),
        val signalThickness: Float = 1f,
        val signalStyle: Int = 0,
        val histColorUp: Color = Color(0xFF26A69A),
        val histColorDown: Color = Color(0xFFEF5350),
        val histVisible: Boolean = true,
        override var isVisible: Boolean = true,
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : Indicator(id, isVisible) {
        override val name: String get() = "MACD ($fastPeriod, $slowPeriod, $signalPeriod)"
    }

    data class Stochastic(
        val kPeriod: Int = 14,
        val kSmoothing: Int = 3,
        val dPeriod: Int = 3,
        val kColor: Color = Color(0xFF2196F3),
        val dColor: Color = Color(0xFFFF9800),
        val kThickness: Float = 1f,
        val dThickness: Float = 1f,
        val kStyle: Int = 0,
        val dStyle: Int = 0,
        val upperLevel: Int = 80,
        val lowerLevel: Int = 20,
        val upperLevelColor: Color = Color(0xFF787B86),
        val lowerLevelColor: Color = Color(0xFF787B86),
        val backgroundVisible: Boolean = true,
        val backgroundColor: Color = Color(0x1A2196F3),
        override var isVisible: Boolean = true,
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : Indicator(id, isVisible) {
        override val name: String get() = "Stoch ($kPeriod, $kSmoothing, $dPeriod)"
    }

    data class ATR(
        val period: Int = 14,
        val color: Color = Color(0xFFFF5252),
        val thickness: Float = 1f,
        val style: Int = 0,
        override var isVisible: Boolean = true,
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : Indicator(id, isVisible) {
        override val name: String get() = "ATR ($period)"
    }

    data class Supertrend(
        val period: Int = 10,
        val multiplier: Float = 3.0f,
        val upColor: Color = Color(0xFF00C853),
        val downColor: Color = Color(0xFFFF1744),
        val thickness: Float = 1f,
        val style: Int = 0,
        val fillVisible: Boolean = true,
        val upFillColor: Color = Color(0x2200C853),
        val downFillColor: Color = Color(0x22FF1744),
        override var isVisible: Boolean = true,
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : Indicator(id, isVisible) {
        override val name: String get() = "Supertrend ($period, $multiplier)"
    }

    data class Alligator(
        val jawPeriod: Int = 13,
        val jawOffset: Int = 8,
        val teethPeriod: Int = 8,
        val teethOffset: Int = 5,
        val lipsPeriod: Int = 5,
        val lipsOffset: Int = 3,
        val jawColor: Color = Color(0xFF2196F3), // Blue
        val teethColor: Color = Color(0xFFF44336), // Red
        val lipsColor: Color = Color(0xFF4CAF50), // Green
        val jawThickness: Float = 1f,
        val teethThickness: Float = 1f,
        val lipsThickness: Float = 1f,
        override var isVisible: Boolean = true,
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : Indicator(id, isVisible) {
        override val name: String get() = "Alligator ($jawPeriod, $teethPeriod, $lipsPeriod)"
    }

    data class Ichimoku(
        val tenkanPeriod: Int = 9,
        val kijunPeriod: Int = 26,
        val senkouBPeriod: Int = 52,
        val displacement: Int = 26,
        val tenkanColor: Color = Color(0xFF2196F3),
        val kijunColor: Color = Color(0xFFE91E63),
        val chikouColor: Color = Color(0xFF4CAF50),
        val senkouAColor: Color = Color(0xFFA5D6A7),
        val senkouBColor: Color = Color(0xFFEF9A9A),
        val kumoUpColor: Color = Color(0x33A5D6A7),
        val kumoDownColor: Color = Color(0x33EF9A9A),
        val tenkanThickness: Float = 1f,
        val kijunThickness: Float = 1f,
        val chikouThickness: Float = 1f,
        val senkouAThickness: Float = 1f,
        val senkouBThickness: Float = 1f,
        override var isVisible: Boolean = true,
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : Indicator(id, isVisible) {
        override val name: String get() = "Ichimoku ($tenkanPeriod, $kijunPeriod, $senkouBPeriod)"
    }

    data class Sessions(
        val showSydney: Boolean = true,
        val sydneyColor: Color = Color(0xFFFFF176), // Yellow
        val sydneyStart: String = "22:00",
        val sydneyEnd: String = "07:00",
        val showTokyo: Boolean = true,
        val tokyoColor: Color = Color(0xFFF06292), // Pink
        val tokyoStart: String = "00:00",
        val tokyoEnd: String = "09:00",
        val showLondon: Boolean = true,
        val londonColor: Color = Color(0xFF64B5F6), // Blue
        val londonStart: String = "08:00",
        val londonEnd: String = "17:00",
        val showNewYork: Boolean = true,
        val newYorkColor: Color = Color(0xFFFFB74D), // Orange
        val newYorkStart: String = "13:00",
        val newYorkEnd: String = "22:00",
        val showLabels: Boolean = true,
        val showBackground: Boolean = true,
        val opacity: Float = 0.1f,
        override var isVisible: Boolean = true,
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : Indicator(id, isVisible) {
        override val name: String get() = "Sessions"
    }

    data class VWAP(
        val anchor: String = "Session",
        val source: String = "HLC3",
        val color: Color = Color(0xFF2196F3),
        val thickness: Float = 1.5f,
        val style: Int = 0,
        val showBands: Boolean = false,
        val bandMult1: Float = 1.0f,
        val bandMult2: Float = 2.0f,
        val bandMult3: Float = 3.0f,
        val upperColor: Color = Color(0xFF4CAF50),
        val lowerColor: Color = Color(0xFFF44336),
        override var isVisible: Boolean = true,
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : Indicator(id, isVisible) {
        override val name: String get() = "VWAP ($anchor, $source)"
    }

    data class Ribbon(
        val isExponential: Boolean = true,
        val source: String = "Close",
        val refPeriod: Int = 100,
        val thickness: Float = 1f,
        val lastThickness: Float = 3f,
        override var isVisible: Boolean = true,
        override val id: String = java.util.UUID.randomUUID().toString()
    ) : Indicator(id, isVisible) {
        override val name: String get() = "MA Ribbon (${if (isExponential) "EMA" else "SMA"})"
    }
}
