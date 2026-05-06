@file:Suppress("ASSIGNED_VALUE_IS_NEVER_READ", "UnusedAssignment", "UNUSED_VALUE")

package com.bthr.backtest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bthr.backtest.model.Candle
import com.bthr.backtest.model.ChartSettings
import com.bthr.backtest.model.DrawingTool
import com.bthr.backtest.model.Indicator
import com.bthr.backtest.util.ChartDrawer
import com.bthr.backtest.util.ChartUtils
import com.bthr.backtest.util.DrawingCoordinateMapper
import com.bthr.backtest.util.DrawingManager
import com.bthr.backtest.ui.components.DragHandle
import com.bthr.backtest.ui.components.getFavoritesBarColors
import com.bthr.backtest.util.DrawingState
import com.bthr.backtest.util.DrawingUseMode
import com.bthr.backtest.util.IndicatorCalculators
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// Simple drawing state enum for trend line drawing
private enum class SimpleDrawingState {
    NONE,
    PLACING_FIRST_POINT,
    PLACING_SECOND_POINT
}

@Suppress("UNUSED_VALUE", "UnusedAssignment")
@Composable
fun CandlestickChart(
    allCandles: List<Candle>,
    modifier: Modifier = Modifier,
    indicators: List<Indicator> = emptyList(),
    scrollOffset: Float = 0f,
    displayCount: Float = 60f,
    symbol: String = "",
    timeframe: String = "",
    onZoom: (Float) -> Unit = {},
    onPan: (Float) -> Unit = {},
    onSettingsRequest: () -> Unit = {},
    onOpenChartSettings: () -> Unit = {},
    onIndicatorSettingsRequest: (Indicator) -> Unit = {},
    onIndicatorToggleVisibility: (Indicator) -> Unit = {},
    onIndicatorRemove: (Indicator) -> Unit = {},
    settings: ChartSettings = ChartSettings(),
    timeZone: TimeZone = TimeZone.getDefault(),
    favoriteTools: Set<String> = emptySet(),
    @Suppress("UNUSED_PARAMETER")
    onFavoriteToolsChange: (Set<String>) -> Unit = {},
    activeDrawingTool: DrawingTool = DrawingTool.NONE,
    drawingUseMode: DrawingUseMode = DrawingUseMode.REPEAT,
    onDrawingUseModeChange: (DrawingUseMode) -> Unit = {},
    onDrawingToolUsed: () -> Unit = {},
    activateDrawingModeTrigger: Int = 0,
    onActivateSimpleDrawingMode: (() -> Unit)? = null
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    
    val colorScheme = MaterialTheme.colorScheme
    val textStyle = TextStyle(color = colorScheme.onBackground, fontSize = settings.scaleTextSize.sp)
    val crosshairTextStyle = TextStyle(color = Color.White, fontSize = 10.sp) 
    
    val currentOnZoom by rememberUpdatedState(onZoom)
    val currentOnPan by rememberUpdatedState(onPan)
    val currentDisplayCount by rememberUpdatedState(displayCount)
    val currentOnSettingsRequest by rememberUpdatedState(onSettingsRequest)

    var crosshairPosition by remember { mutableStateOf<Offset?>(null) }
    var crosshairDragOffset by remember { mutableStateOf(Offset.Zero) }
    var isLongPressing by remember { mutableStateOf(false) }
    var isSelected by remember { mutableStateOf(false) }
    var selectedIndicatorId by remember { mutableStateOf<String?>(null) }

    // Simple drawing state for trend line
    var simpleDrawingState by remember { mutableStateOf(SimpleDrawingState.NONE) }
    var firstPoint by remember { mutableStateOf<Pair<Int, Float>?>(null) }
    var firstPointScreen by remember { mutableStateOf<Offset?>(null) }
    var simpleTrendLines by remember { mutableStateOf<List<Pair<Pair<Int, Float>, Pair<Int, Float>>>>(emptyList()) }

    // ── Drawing Manager (replaces old isTrendLineMode / isFibMode scattered state) ──
    val drawingManager = remember { DrawingManager() }
    var effectiveDrawingUseMode by remember { mutableStateOf(drawingUseMode) }

    LaunchedEffect(drawingUseMode) {
        effectiveDrawingUseMode = drawingUseMode
        drawingManager.setUseMode(drawingUseMode)
    }

    // Activate tool from external menu
    LaunchedEffect(activeDrawingTool, effectiveDrawingUseMode) {
        drawingManager.setUseMode(effectiveDrawingUseMode)
        if (activeDrawingTool != DrawingTool.NONE) {
            drawingManager.setTool(activeDrawingTool)
        } else {
            drawingManager.cancelDrawing()
        }
    }

    // Trigger simple drawing mode when parent increments the trigger
    LaunchedEffect(activateDrawingModeTrigger) {
        if (activateDrawingModeTrigger > 0) {
            simpleDrawingState = SimpleDrawingState.PLACING_FIRST_POINT
            firstPoint = null
            firstPointScreen = null
        }
    }

    // Favorites bar draggable state (px, null = not yet initialized)
    var favoritesBarPosition by remember { mutableStateOf<Offset?>(null) }
    var favoritesBarSizePx by remember { mutableStateOf(Offset(200f, 40f)) }
    var isDraggingFavoritesBar by remember { mutableStateOf(false) }

    var isAutoHeight by remember { mutableStateOf(true) }
    var manualMinPrice by remember { mutableFloatStateOf(0f) }
    var manualMaxPrice by remember { mutableFloatStateOf(100f) }

    val indicatorHeights = remember { mutableStateMapOf<String, Float>() }
    val defaultHeightPx = with(density) { 60.dp.toPx() }
    val minIndicatorHeightPx = with(density) { 30.dp.toPx() }
    val minimizedHeightPx = with(density) { 20.dp.toPx() }

    val volumeIndicator = remember(indicators) { indicators.filterIsInstance<Indicator.Volume>().firstOrNull() }
    val bottomIndicators = indicators.filter { (it is Indicator.RSI || it is Indicator.MACD || it is Indicator.Stochastic || it is Indicator.ATR) }
    val overlayIndicators = indicators.filter { it is Indicator.SMA || it is Indicator.EMA || it is Indicator.HMA || it is Indicator.VWAP || it is Indicator.BollingerBands || it is Indicator.ATRBands || it is Indicator.Supertrend || it is Indicator.Alligator || it is Indicator.Ichimoku || it is Indicator.Sessions || it is Indicator.Ribbon }
    val sessionsIndicator = remember(indicators) { indicators.filterIsInstance<Indicator.Sessions>().firstOrNull() }

    val indicatorAutoHeight = remember { mutableStateMapOf<String, Boolean>() }
    val indicatorRanges = remember { mutableStateMapOf<String, Pair<Float, Float>>() }

    LaunchedEffect(bottomIndicators) {
        bottomIndicators.forEach { indicator ->
            if (!indicatorHeights.containsKey(indicator.id)) indicatorHeights[indicator.id] = defaultHeightPx
            if (!indicatorAutoHeight.containsKey(indicator.id)) indicatorAutoHeight[indicator.id] = true
        }
    }

    var draggingSeparatorIdx by remember { mutableStateOf(-1) }
    var dragArea by remember { mutableStateOf(0) } 
    var draggingIndicatorId by remember { mutableStateOf<String?>(null) }
    var expandedIndicatorId by remember { mutableStateOf<String?>(null) }
    val showIndicatorLabels by remember { mutableStateOf(true) }
    var overlayCollapsed by remember { mutableStateOf(false) }

    val timeFormatter = remember(timeZone) { SimpleDateFormat("HH:mm\ndd MMM", Locale.getDefault()).apply { this.timeZone = timeZone } }
    val crosshairTimeFormatter = remember(timeZone) { 
        SimpleDateFormat("EEE dd MMM ''yy  HH:mm", Locale.FRENCH).apply { this.timeZone = timeZone } 
    }

    val endIdx = (allCandles.size - scrollOffset.toInt()).coerceIn(0, allCandles.size)
    val startIdx = (endIdx - ceil(displayCount).toInt() - 10).coerceAtLeast(0)
    val visibleCandles = if (allCandles.isEmpty()) emptyList() else allCandles.subList(startIdx, endIdx)

    if (visibleCandles.isNotEmpty()) {
        val maxP = visibleCandles.maxOfOrNull { it.high } ?: 100f
        val minP = visibleCandles.minOfOrNull { it.low } ?: 0f
        if (isAutoHeight) {
            SideEffect { manualMinPrice = minP; manualMaxPrice = maxP }
        }
    }

    val smaValues = remember(allCandles, indicators) { 
        indicators.filterIsInstance<Indicator.SMA>().associate { 
            it.id to IndicatorCalculators.calculateSMA(
                allCandles, it.period, it.source)
        } 
    }
    val emaValues = remember(allCandles, indicators) { 
        indicators.filterIsInstance<Indicator.EMA>().associate { 
            it.id to IndicatorCalculators.calculateEMA(allCandles, it.period, it.source) 
        } 
    }
    val hmaValues = remember(allCandles, indicators) {
        indicators.filterIsInstance<Indicator.HMA>().associate {
            it.id to IndicatorCalculators.calculateHMA(allCandles, it.period, it.source)
        }
    }
    val vwapValues = remember(allCandles, indicators) {
        indicators.filterIsInstance<Indicator.VWAP>().associate {
            it.id to IndicatorCalculators.calculateVWAP(allCandles, it.anchor, it.source, it.bandMult1, it.bandMult2, it.bandMult3)
        }
    }
    val bbValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.BollingerBands>().associate { it.id to IndicatorCalculators.calculateBollingerBands(allCandles, it.period, it.stdDev) } }
    val atrBandsValues = remember(allCandles, indicators) { 
        indicators.filterIsInstance<Indicator.ATRBands>().associate { 
            it.id to IndicatorCalculators.calculateATRBands(allCandles, it.period, it.multiplier, it.source, it.showTPBands, it.tpScaleFactor) 
        } 
    }
    val stValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.Supertrend>().associate { it.id to IndicatorCalculators.calculateSupertrend(allCandles, it.period, it.multiplier) } }
    val alligatorValues = remember(allCandles, indicators) { 
        indicators.filterIsInstance<Indicator.Alligator>().associate { 
            it.id to IndicatorCalculators.calculateAlligator(allCandles, it.jawPeriod, it.jawOffset, it.teethPeriod, it.teethOffset, it.lipsPeriod, it.lipsOffset) 
        } 
    }
    val ichimokuValues = remember(allCandles, indicators) {
        indicators.filterIsInstance<Indicator.Ichimoku>().associate {
            it.id to IndicatorCalculators.calculateIchimoku(allCandles, it.tenkanPeriod, it.kijunPeriod, it.senkouBPeriod, it.displacement)
        }
    }
    val ribbonValues = remember(allCandles, indicators) {
        indicators.filterIsInstance<Indicator.Ribbon>().associate {
            it.id to IndicatorCalculators.calculateRibbon(allCandles, it.isExponential, it.source, it.refPeriod)
        }
    }
    
    val rsiValues = remember(allCandles, indicators) { 
        indicators.filterIsInstance<Indicator.RSI>().associate { 
            it.id to IndicatorCalculators.calculateRSI(allCandles, it.period, it.source) 
        } 
    }
    val rsiMaValues = remember(rsiValues, indicators) {
        indicators.filterIsInstance<Indicator.RSI>().associate { rsi ->
            val vals = rsiValues[rsi.id] ?: emptyList()
            rsi.id to if (rsi.showMa) {
                if (rsi.maType == "EMA") IndicatorCalculators.calculateEMAFromValues(vals, rsi.maPeriod)
                else IndicatorCalculators.calculateSMAFromValues(vals, rsi.maPeriod)
            } else emptyList()
        }
    }
    
    val macdValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.MACD>().associate { it.id to IndicatorCalculators.calculateMACD(allCandles, it.fastPeriod, it.slowPeriod, it.signalPeriod) } }
    val stochValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.Stochastic>().associate { it.id to IndicatorCalculators.calculateStochastic(allCandles, it.kPeriod, it.kSmoothing, it.dPeriod) } }
    val atrValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.ATR>().associate { it.id to IndicatorCalculators.calculateWilderATR(allCandles, it.period) } }

    val volumeMaValues = remember(allCandles, volumeIndicator) {
        volumeIndicator?.let { IndicatorCalculators.calculateVolumeMA(allCandles, it.maLength, it.maType) }
    }
    val smoothedMaValues = remember(allCandles, volumeIndicator) {
        volumeIndicator?.let { IndicatorCalculators.calculateVolumeMA(allCandles, it.smoothingLength, it.smoothingLine) }
    }

    LaunchedEffect(visibleCandles, bottomIndicators, rsiValues, rsiMaValues, macdValues, stochValues, atrValues) {
        bottomIndicators.forEach { ind ->
            if (indicatorAutoHeight[ind.id] != false) {
                val vals = when (ind) {
                    is Indicator.RSI -> {
                        val rsi = rsiValues[ind.id] ?: emptyList()
                        val rsiMa = rsiMaValues[ind.id] ?: emptyList()
                        val vRsi = if (rsi.isEmpty()) emptyList() else rsi.subList(startIdx.coerceIn(0, rsi.size), endIdx.coerceIn(0, rsi.size))
                        val vMa = if (rsiMa.isEmpty()) emptyList() else rsiMa.subList(startIdx.coerceIn(0, rsiMa.size), endIdx.coerceIn(0, rsiMa.size))
                        vRsi + vMa
                    }
                    is Indicator.MACD -> {
                        macdValues[ind.id]?.let {
                            val vMacd = if (it.macdLine.isEmpty()) emptyList() else it.macdLine.subList(startIdx.coerceIn(0, it.macdLine.size), endIdx.coerceIn(0, it.macdLine.size))
                            val vSignal = if (it.signalLine.isEmpty()) emptyList() else it.signalLine.subList(startIdx.coerceIn(0, it.signalLine.size), endIdx.coerceIn(0, it.signalLine.size))
                            val vHist = if (it.histogram.isEmpty()) emptyList() else it.histogram.let { h -> h.subList(startIdx.coerceIn(0, h.size), endIdx.coerceIn(0, h.size)) }
                            vMacd + vSignal + vHist
                        } ?: emptyList()
                    }
                    is Indicator.Stochastic -> {
                        stochValues[ind.id]?.let {
                            val vK = if (it.k.isEmpty()) emptyList() else it.k.subList(startIdx.coerceIn(0, it.k.size), endIdx.coerceIn(0, it.k.size))
                            val vD = if (it.d.isEmpty()) emptyList() else it.d.subList(startIdx.coerceIn(0, it.d.size), endIdx.coerceIn(0, it.d.size))
                            vK + vD
                        } ?: emptyList()
                    }
                    is Indicator.ATR -> {
                        val atr = atrValues[ind.id] ?: emptyList()
                        if (atr.isEmpty()) emptyList() else atr.subList(startIdx.coerceIn(0, atr.size), endIdx.coerceIn(0, atr.size))
                    }
                    else -> emptyList()
                }
                val visibleVals = vals.filterNotNull()
                if (visibleVals.isNotEmpty()) {
                    var minV = visibleVals.min(); var maxV = visibleVals.max()
                    val diff = (maxV - minV).coerceAtLeast(0.001f)
                    minV -= diff * 0.1f; maxV += diff * 0.1f
                    if (ind is Indicator.RSI || ind is Indicator.Stochastic) {
                        minV = minV.coerceAtMost(20f).coerceAtLeast(0f)
                        maxV = maxV.coerceAtLeast(80f).coerceAtMost(100f)
                    }
                    indicatorRanges[ind.id] = Pair(minV, maxV)
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val priceWidthPx = with(density) { 55.dp.toPx() }
        val timeHeightPx = with(density) { 35.dp.toPx() }

        // Helper to build a DrawingCoordinateMapper from current chart state
        fun buildMapper(
            chartWidthPx: Float, mainH: Float,
            totalCandles: Int, scrollOffset: Float, displayCount: Float, marginRightBars: Float,
            manualMinPrice: Float, manualMaxPrice: Float,
            topMarginPx: Float, bottomMarginPx: Float
        ): DrawingCoordinateMapper {
            val rawRange = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.001f)
            val range = rawRange * 1.1f
            val minP = (manualMaxPrice + manualMinPrice) / 2f - range / 2f
            val maxP = minP + range
            return DrawingCoordinateMapper(
                chartWidthPx = chartWidthPx,
                chartHeightPx = mainH,
                totalCandleCount = totalCandles,
                scrollOffset = scrollOffset,
                displayCount = displayCount,
                marginRightBars = marginRightBars,
                minPrice = minP,
                maxPrice = maxP,
                topMarginPx = topMarginPx,
                bottomMarginPx = bottomMarginPx
            )
        }
        val chartWidthPx = w - priceWidthPx
        val plotsHeightPx = h - timeHeightPx
        val labelBgColor = colorScheme.background

        // Snap crosshair X to the nearest candle center so horizontal drag moves bar-to-bar.
        fun snapXToCandleCenter(rawX: Float): Float {
            // Pas de clampage supérieur : on autorise rawX > chartWidthPx pour les barres virtuelles futures
            val clampedX = rawX.coerceAtLeast(0f)
            if (allCandles.isEmpty()) return clampedX.coerceAtMost(chartWidthPx)

            val localCandleW = if (currentDisplayCount > 0) {
                chartWidthPx / (currentDisplayCount + settings.marginRightBars)
            } else 0f
            if (localCandleW <= 0f) return clampedX.coerceAtMost(chartWidthPx)

            val rawIdx = round(
                allCandles.size - 1 - scrollOffset -
                    (chartWidthPx - clampedX - localCandleW / 2f) / localCandleW
            ).toInt()

            // Autoriser les indices virtuels futurs (au-delà de la dernière bougie)
            // mais pas avant la première bougie
            val maxVirtualIdx = allCandles.size - 1 + (currentDisplayCount + settings.marginRightBars).toInt()
            val snappedIdx = rawIdx.coerceIn(0, maxVirtualIdx)

            val snappedX = chartWidthPx - (localCandleW / 2f) -
                ((allCandles.size - 1 - snappedIdx - scrollOffset) * localCandleW)

            // Si le snap tombe au-delà du bord droit du chart, déclencher un pan
            // pour créer de la marge droite (le chart scrolle avec le viseur)
            if (snappedX > chartWidthPx && snappedIdx > allCandles.size - 1) {
                val overshoot = snappedX - chartWidthPx
                val candlesOver = overshoot / localCandleW
                currentOnPan(-candlesOver)
            }

            return snappedX.coerceIn(0f, chartWidthPx)
        }

        fun updateCrosshairFromRaw(rawX: Float, rawY: Float) {
            crosshairPosition = Offset(
                snapXToCandleCenter(rawX),
                rawY.coerceIn(0f, h)
            )
        }

        fun candleIndexAtX(x: Float): Int {
            if (currentDisplayCount <= 0f) return -1
            val candleW = chartWidthPx / (currentDisplayCount + settings.marginRightBars)
            if (candleW <= 0f) return -1
            return round(allCandles.size - 1 - scrollOffset - (chartWidthPx - x - candleW / 2f) / candleW).toInt()
        }

        fun currentBottomIndicatorsHeight(): Float = bottomIndicators.fold(0f) { acc, indicator ->
            acc + if (indicator.isVisible) {
                indicatorHeights[indicator.id] ?: defaultHeightPx
            } else {
                minimizedHeightPx
            }
        }

        fun currentMainHeight(): Float = plotsHeightPx - currentBottomIndicatorsHeight()

        fun routeDrawingDrag(pointerPos: Offset, dragAmount: Offset) {
            val mainHLocal = currentMainHeight()
            val mapper = buildMapper(
                chartWidthPx = chartWidthPx,
                mainH = mainHLocal,
                totalCandles = allCandles.size,
                scrollOffset = scrollOffset,
                displayCount = displayCount,
                marginRightBars = settings.marginRightBars.toFloat(),
                manualMinPrice = manualMinPrice,
                manualMaxPrice = manualMaxPrice,
                topMarginPx = mainHLocal * (settings.marginTopPercent / 100f),
                bottomMarginPx = mainHLocal * (settings.marginBottomPercent / 100f)
            )
            drawingManager.onCursorMove(pointerPos)
            drawingManager.onDragUpdate(pointerPos, dragAmount, mapper)
        }

        fun clearChartSelection() {
            isSelected = false
            selectedIndicatorId = null
        }

        // Simple drawing mode activation/deactivation functions
        fun activateSimpleDrawingMode() {
            simpleDrawingState = SimpleDrawingState.PLACING_FIRST_POINT
            firstPoint = null
            firstPointScreen = null
            // Show crosshair if not already visible
            if (crosshairPosition == null) {
                isLongPressing = true
                val mainHLocal = currentMainHeight()
                crosshairPosition = Offset(chartWidthPx / 2f, mainHLocal / 2f)
                crosshairDragOffset = Offset.Zero
            }
        }

        fun deactivateSimpleDrawingMode() {
            simpleDrawingState = SimpleDrawingState.NONE
            firstPoint = null
            firstPointScreen = null
            // Hide crosshair when exiting drawing mode
            isLongPressing = false
            crosshairPosition = null
        }

        fun isOverlayIndicatorHit(ind: Indicator, idx: Int, priceAtY: Float, searchThreshold: Float): Boolean =
            when (ind) {
                is Indicator.SMA -> smaValues[ind.id]?.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false
                is Indicator.EMA -> emaValues[ind.id]?.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false
                is Indicator.HMA -> hmaValues[ind.id]?.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false
                is Indicator.VWAP -> vwapValues[ind.id]?.let { v ->
                    (v.vwap.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (if (ind.showBands) {
                            (v.upper1.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                                (v.lower1.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false)
                        } else false)
                } ?: false
                is Indicator.BollingerBands -> bbValues[ind.id]?.let { bb ->
                    (bb.middle.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (bb.upper.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (bb.lower.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false)
                } ?: false
                is Indicator.ATRBands -> atrBandsValues[ind.id]?.let { ab ->
                    (ab.upper.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (ab.lower.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (ab.tpUpper.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (ab.tpLower.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false)
                } ?: false
                is Indicator.Alligator -> alligatorValues[ind.id]?.let { res ->
                    (res.jaw.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (res.teeth.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (res.lips.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false)
                } ?: false
                is Indicator.Ichimoku -> ichimokuValues[ind.id]?.let { res ->
                    (res.tenkanSen.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (res.kijunSen.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (res.senkouSpanA.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (res.senkouSpanB.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false) ||
                        (res.chikouSpan.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false)
                } ?: false
                is Indicator.Supertrend -> stValues[ind.id]?.values?.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false
                is Indicator.Ribbon -> ribbonValues[ind.id]?.let { res ->
                    res.mas.any { ma -> ma.getOrNull(idx)?.let { abs(it - priceAtY) < searchThreshold } ?: false }
                } ?: false
                is Indicator.Sessions -> false
                else -> false
            }

        fun isBottomIndicatorHit(ind: Indicator, idx: Int, valAtY: Float, searchThreshold: Float): Boolean =
            when (ind) {
                is Indicator.RSI ->
                    (rsiValues[ind.id]?.getOrNull(idx)?.let { abs(it - valAtY) < searchThreshold } ?: false) ||
                        (rsiMaValues[ind.id]?.getOrNull(idx)?.let { abs(it - valAtY) < searchThreshold } ?: false)
                is Indicator.MACD -> macdValues[ind.id]?.let { res ->
                    (res.macdLine.getOrNull(idx)?.let { abs(it - valAtY) < searchThreshold } ?: false) ||
                        (res.signalLine.getOrNull(idx)?.let { abs(it - valAtY) < searchThreshold } ?: false) ||
                        (res.histogram.getOrNull(idx)?.let { abs(it - valAtY) < searchThreshold } ?: false)
                } ?: false
                is Indicator.Stochastic -> stochValues[ind.id]?.let { res ->
                    (res.k.getOrNull(idx)?.let { abs(it - valAtY) < searchThreshold } ?: false) ||
                        (res.d.getOrNull(idx)?.let { abs(it - valAtY) < searchThreshold } ?: false)
                } ?: false
                is Indicator.ATR -> atrValues[ind.id]?.getOrNull(idx)?.let { abs(it - valAtY) < searchThreshold } ?: false
                else -> false
            }

        fun handleBottomIndicatorSettingsTap(ind: Indicator, idx: Int, valAtY: Float, rV: Float): Boolean {
            if (isBottomIndicatorHit(ind, idx, valAtY, rV * 0.05f)) {
                onIndicatorSettingsRequest(ind)
                return true
            }
            return false
        }

        fun handleMainCanvasDrag(change: PointerInputChange, dragAmount: Offset) {
            if (change.isConsumed) {
                return
            }

            // In simple drawing mode, only update crosshair position
            if (simpleDrawingState != SimpleDrawingState.NONE) {
                if (crosshairPosition != null) {
                    val rawX = change.position.x + crosshairDragOffset.x
                    updateCrosshairFromRaw(rawX, change.position.y + crosshairDragOffset.y)
                }
                change.consume()
                return
            }

            when (dragArea) {
                -10 -> {
                    routeDrawingDrag(change.position, dragAmount)
                }
                0 -> {
                    // Skip DrawingManager for trend line tool - use simple drawing mode instead
                    if (drawingManager.activeTool != DrawingTool.NONE && drawingManager.activeTool != DrawingTool.TREND_LINE) {
                        // Use snapped position for fluid movement like main crosshair
                        val rawX = change.position.x + crosshairDragOffset.x
                        val snappedX = snapXToCandleCenter(rawX)
                        val snappedPosition = Offset(snappedX, change.position.y + crosshairDragOffset.y)
                        drawingManager.onCursorMove(snappedPosition)
                    } else if (isLongPressing) {
                        val rawX = change.position.x + crosshairDragOffset.x
                        updateCrosshairFromRaw(rawX, change.position.y + crosshairDragOffset.y)
                    }
                }
                1 -> {
                    // Effacer la sélection lors du glissement sur la barre de prix
                    clearChartSelection()
                    
                    isAutoHeight = false
                    val zoom = (1f - (dragAmount.y / plotsHeightPx) * 2f).coerceIn(0.1f, 10f)
                    val range = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.1f)
                    val center = (manualMaxPrice + manualMinPrice) / 2f
                    manualMinPrice = center - (range / zoom) / 2f
                    manualMaxPrice = center + (range / zoom) / 2f
                }
                2 -> {
                    // Effacer la sélection lors du glissement sur la barre de temps
                    clearChartSelection()
                    
                    currentOnZoom((1f - (dragAmount.x / chartWidthPx) * 8f).coerceIn(0.15f, 100f))
                }
                3 -> {
                    // Effacer la sélection lors du glissement sur la grille
                    clearChartSelection()
                    
                    val candleW = if (currentDisplayCount > 0)
                        chartWidthPx / (currentDisplayCount + settings.marginRightBars)
                    else 0f

                    if (candleW > 0) {
                        currentOnPan(dragAmount.x / candleW)
                    }

                    val range = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.1f)
                    val priceDelta = (dragAmount.y / plotsHeightPx) * range
                    manualMinPrice += priceDelta
                    manualMaxPrice += priceDelta
                }
                4 -> {
                    val dy = dragAmount.y
                    val minChartH = with(density) { 60.dp.toPx() }

                    val idxBelow = (draggingSeparatorIdx until bottomIndicators.size).firstOrNull { bottomIndicators[it].isVisible }
                    val idxAbove = (draggingSeparatorIdx - 1 downTo 0).firstOrNull { bottomIndicators[it].isVisible }

                    if (idxBelow != null && idxAbove != null) {
                        val idAbove = bottomIndicators[idxAbove].id
                        val idBelow = bottomIndicators[idxBelow].id
                        val hAbove = indicatorHeights[idAbove] ?: defaultHeightPx
                        val hBelow = indicatorHeights[idBelow] ?: defaultHeightPx
                        val limitedDy = dy.coerceIn(minIndicatorHeightPx - hAbove, hBelow - minIndicatorHeightPx)
                        indicatorHeights[idAbove] = hAbove + limitedDy
                        indicatorHeights[idBelow] = hBelow - limitedDy
                    } else if (idxBelow != null) {
                        val idBelow = bottomIndicators[idxBelow].id
                        val hBelow = indicatorHeights[idBelow] ?: defaultHeightPx
                        val othersH = bottomIndicators.indices.filter { it != idxBelow }.sumOf { idx ->
                            val ind = bottomIndicators[idx]
                            if (ind.isVisible) (indicatorHeights[ind.id] ?: defaultHeightPx).toDouble() else minimizedHeightPx.toDouble()
                        }.toFloat()
                        val mHLimit = (plotsHeightPx - othersH - minChartH).coerceAtLeast(minIndicatorHeightPx)
                        indicatorHeights[idBelow] = (hBelow - dy).coerceIn(minIndicatorHeightPx, mHLimit)
                    } else if (idxAbove != null) {
                        val idAbove = bottomIndicators[idxAbove].id
                        val hAbove = indicatorHeights[idAbove] ?: defaultHeightPx
                        val othersH = bottomIndicators.indices.filter { it != idxAbove }.sumOf { idx ->
                            val ind = bottomIndicators[idx]
                            if (ind.isVisible) (indicatorHeights[ind.id] ?: defaultHeightPx).toDouble() else minimizedHeightPx.toDouble()
                        }.toFloat()
                        val mHLimit = (plotsHeightPx - othersH - minChartH).coerceAtLeast(minIndicatorHeightPx)
                        indicatorHeights[idAbove] = (hAbove + dy).coerceIn(minIndicatorHeightPx, mHLimit)
                    }
                }
                5 -> {
                    draggingIndicatorId?.let { id ->
                        indicatorAutoHeight[id] = false
                        val indH = indicatorHeights[id] ?: defaultHeightPx
                        val zoom = (1f - (dragAmount.y / indH) * 2f).coerceIn(0.1f, 10f)
                        val range = indicatorRanges[id] ?: Pair(0f, 100f)
                        val rV = (range.second - range.first).coerceAtLeast(0.001f)
                        val center = (range.second + range.first) / 2f
                        indicatorRanges[id] = Pair(center - (rV / zoom) / 2f, center + (rV / zoom) / 2f)
                    }
                }
                6 -> {
                    val candleW = if (currentDisplayCount > 0)
                        chartWidthPx / (currentDisplayCount + settings.marginRightBars)
                    else 0f

                    if (candleW > 0) {
                        currentOnPan(dragAmount.x / candleW)
                    }

                    draggingIndicatorId?.let { id ->
                        indicatorAutoHeight[id] = false
                        val indH = indicatorHeights[id] ?: defaultHeightPx
                        val range = indicatorRanges[id] ?: Pair(0f, 100f)
                        val rV = (range.second - range.first).coerceAtLeast(0.001f)
                        val vDelta = (dragAmount.y / indH) * rV
                        indicatorRanges[id] = Pair(range.first + vDelta, range.second + vDelta)
                    }
                }
            }
            change.consume()
        }

        fun handleMainCanvasTap(offset: Offset) {
            expandedIndicatorId = null
            val mainHLocal = currentMainHeight()

            // Priorité 1 : si le viseur est actif en mode normal, un tap dans la grille le ferme
            if (isLongPressing && simpleDrawingState == SimpleDrawingState.NONE) {
                val tIndH2 = bottomIndicators.sumOf {
                    if (it.isVisible) (indicatorHeights[it.id] ?: defaultHeightPx).toDouble() else minimizedHeightPx.toDouble()
                }.toFloat()
                val mainHForTap = plotsHeightPx - tIndH2
                val inGrid = offset.x <= chartWidthPx && offset.y <= mainHForTap
                if (inGrid) {
                    isLongPressing = false
                    crosshairPosition = null
                }
                return
            }

            // Priorité 1.2 : si le viseur est actif en mode dessin simple, un tap dans la grille le ferme
            if (isLongPressing && simpleDrawingState != SimpleDrawingState.NONE) {
                val tIndH2 = bottomIndicators.sumOf {
                    if (it.isVisible) (indicatorHeights[it.id] ?: defaultHeightPx).toDouble() else minimizedHeightPx.toDouble()
                }.toFloat()
                val mainHForTap = plotsHeightPx - tIndH2
                val inGrid = offset.x <= chartWidthPx && offset.y <= mainHForTap
                if (inGrid) {
                    deactivateSimpleDrawingMode()
                }
                return
            }

            // Priorité 1.5 : simple drawing mode taps
            if (simpleDrawingState != SimpleDrawingState.NONE) {
                val candleW = if (currentDisplayCount > 0) chartWidthPx / (currentDisplayCount + settings.marginRightBars) else 0f
                val topM = mainHLocal * (settings.marginTopPercent / 100f)
                val botM = mainHLocal * (settings.marginBottomPercent / 100f)
                val effectiveMainH = mainHLocal - topM - botM
                val rawRange = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.001f)
                val range = rawRange * 1.1f
                val minP = (manualMaxPrice + manualMinPrice) / 2f - (range / 2f)
                val denormY: (Float) -> Float = { minP + ((effectiveMainH - (it - topM)) / effectiveMainH * range) }

                when (simpleDrawingState) {
                    SimpleDrawingState.PLACING_FIRST_POINT -> {
                        // Place first point at crosshair center
                        crosshairPosition?.let { crosshair ->
                            val index = if (candleW > 0) {
                                round(allCandles.size - 1 - scrollOffset - (chartWidthPx - crosshair.x - candleW / 2f) / candleW).toInt()
                            } else -1
                            val price = denormY(crosshair.y)
                            firstPoint = Pair(index, price)
                            // Store screen coordinates to keep the point fixed during drawing
                            firstPointScreen = crosshair
                            simpleDrawingState = SimpleDrawingState.PLACING_SECOND_POINT
                        }
                    }
                    SimpleDrawingState.PLACING_SECOND_POINT -> {
                        // Place second point and finalize line
                        crosshairPosition?.let { crosshair ->
                            val index = if (candleW > 0) {
                                round(allCandles.size - 1 - scrollOffset - (chartWidthPx - crosshair.x - candleW / 2f) / candleW).toInt()
                            } else -1
                            val price = denormY(crosshair.y)
                            firstPoint?.let { (startIndex, startPrice) ->
                                // Add to simple trend lines list
                                simpleTrendLines = simpleTrendLines + Pair(Pair(startIndex, startPrice), Pair(index, price))
                            }
                        }
                        // Reset drawing mode
                        deactivateSimpleDrawingMode()
                    }
                    SimpleDrawingState.NONE -> {}
                }
                return
            }

            // Priorité 2 : outils de dessin
            if ((drawingManager.activeTool != DrawingTool.NONE || drawingManager.drawingState is DrawingState.Selected) && offset.x <= chartWidthPx && offset.y <= mainHLocal) {
                val topM = mainHLocal * (settings.marginTopPercent / 100f)
                val botM = mainHLocal * (settings.marginBottomPercent / 100f)
                val mapper = buildMapper(
                    chartWidthPx = chartWidthPx,
                    mainH = mainHLocal,
                    totalCandles = allCandles.size,
                    scrollOffset = scrollOffset,
                    displayCount = displayCount,
                    marginRightBars = settings.marginRightBars.toFloat(),
                    manualMinPrice = manualMinPrice,
                    manualMaxPrice = manualMaxPrice,
                    topMarginPx = topM,
                    bottomMarginPx = botM
                )
                if (drawingManager.onTap(offset, mapper, allCandles)) {
                    onDrawingToolUsed()
                }
            } else {
                val tIndH = bottomIndicators.sumOf {
                    if (it.isVisible) (indicatorHeights[it.id] ?: defaultHeightPx).toDouble() else minimizedHeightPx.toDouble()
                }.toFloat()
                val mH = plotsHeightPx - tIndH

                if (offset.x <= chartWidthPx) {
                    if (offset.y <= mH) {
                        val candleW = if (currentDisplayCount > 0) chartWidthPx / (currentDisplayCount + settings.marginRightBars) else 0f
                        if (candleW > 0) {
                            val rawRange = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.001f)
                            val range = rawRange * 1.1f
                            val minP = (manualMaxPrice + manualMinPrice) / 2f - (range / 2f)
                            val topMarginPx = (mH * (settings.marginTopPercent / 100f))
                            val bottomMarginPx = (mH * (settings.marginBottomPercent / 100f))
                            val effectiveMainH = mH - topMarginPx - bottomMarginPx
                            val denormY: (Float) -> Float = { minP + ((effectiveMainH - (it - topMarginPx)) / effectiveMainH * range) }

                            val idx = round(allCandles.size - 1 - scrollOffset - (chartWidthPx - offset.x - candleW / 2f) / candleW).toInt()

                            var foundInd = false
                            val searchThreshold = range * 0.03f
                            val priceAtY = denormY(offset.y)

                            for (ind in overlayIndicators) {
                                if (!ind.isVisible) continue
                                val found = isOverlayIndicatorHit(ind, idx, priceAtY, searchThreshold)
                                if (found) {
                                    selectedIndicatorId = ind.id
                                    isSelected = false
                                    foundInd = true
                                    break
                                }
                            }

                            if (!foundInd && idx in allCandles.indices) {
                                val candle = allCandles[idx]
                                if (priceAtY in candle.low..candle.high) {
                                    isSelected = true
                                    selectedIndicatorId = null
                                } else {
                                    clearChartSelection()
                                }
                            } else if (!foundInd) {
                                clearChartSelection()
                            }
                        }
                    } else {
                        var curYPos = mH
                        var found = false
                        for (ind in bottomIndicators) {
                            val indH = if (ind.isVisible) (indicatorHeights[ind.id] ?: defaultHeightPx) else minimizedHeightPx
                            if (offset.y in curYPos..curYPos + indH) {
                                if (ind.isVisible) {
                                    val rangeInd = indicatorRanges[ind.id] ?: Pair(0f, 100f)
                                    val rV = (rangeInd.second - rangeInd.first).coerceAtLeast(0.001f)
                                    val denormIndY: (Float) -> Float = { rangeInd.first + (curYPos + indH - it) / indH * rV }
                                    val valAtY = denormIndY(offset.y)
                                    val candleW = if (currentDisplayCount > 0) chartWidthPx / (currentDisplayCount + settings.marginRightBars) else 0f
                                    val idx = if (candleW > 0) round(allCandles.size - 1 - scrollOffset - (chartWidthPx - offset.x - candleW / 2f) / candleW).toInt() else -1
                                    val searchThreshold = rV * 0.05f

                                    if (isBottomIndicatorHit(ind, idx, valAtY, searchThreshold)) {
                                        selectedIndicatorId = ind.id
                                        isSelected = false
                                        found = true
                                        break
                                    }
                                }
                            }
                            curYPos += indH
                        }
                        if (!found) {
                            clearChartSelection()
                        }
                    }
                } else {
                    // Ne pas effacer la sélection sur la barre de prix pour permettre la sélection
                    // clearChartSelection()
                }
            }
        }

        val crosshairLabelBgColor = if (settings.backgroundColor.luminance() > 0.5f) Color(0xFF212121) else Color(0xFFEEEEEE)

        val totalIndH = bottomIndicators.sumOf {
            if (it.isVisible) (indicatorHeights[it.id] ?: defaultHeightPx).toDouble()
            else minimizedHeightPx.toDouble()
        }.toFloat()
        val mainH = plotsHeightPx - totalIndH

        // Initialize crosshair when simple drawing mode is activated
        LaunchedEffect(simpleDrawingState) {
            if (simpleDrawingState == SimpleDrawingState.PLACING_FIRST_POINT && crosshairPosition == null) {
                isLongPressing = true
                crosshairPosition = Offset(chartWidthPx / 2f, mainH / 2f)
                crosshairDragOffset = Offset.Zero
            }
        }

        Canvas(
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(false)
                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.size > 1 && !isLongPressing) {
                                val zoom = event.calculateZoom()
                                if (abs(zoom - 1f) > 0.001f) {
                                    currentOnZoom(zoom)
                                    currentOnPan(currentDisplayCount * (1f - 1f / zoom) / 2f)
                                }
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .pointerInput(bottomIndicators, isLongPressing, drawingManager.activeTool, drawingManager.drawingState) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // ── Drawing tool drag start ──────────────────────
                            if (drawingManager.activeTool != DrawingTool.NONE && offset.x <= chartWidthPx && offset.y <= mainH) {
                                val mapper = buildMapper(
                                    chartWidthPx = chartWidthPx,
                                    mainH = mainH,
                                    totalCandles = allCandles.size,
                                    scrollOffset = scrollOffset,
                                    displayCount = displayCount,
                                    marginRightBars = settings.marginRightBars.toFloat(),
                                    manualMinPrice = manualMinPrice,
                                    manualMaxPrice = manualMaxPrice,
                                    topMarginPx = mainH * (settings.marginTopPercent / 100f),
                                    bottomMarginPx = mainH * (settings.marginBottomPercent / 100f)
                                )
                                drawingManager.onDragStart(offset, mapper)
                                dragArea = -10  // consumed
                                return@detectDragGestures
                            }
                            if (drawingManager.drawingState is DrawingState.Selected && offset.x <= chartWidthPx && offset.y <= mainH) {
                                val mapper = buildMapper(
                                    chartWidthPx = chartWidthPx,
                                    mainH = mainH,
                                    totalCandles = allCandles.size,
                                    scrollOffset = scrollOffset,
                                    displayCount = displayCount,
                                    marginRightBars = settings.marginRightBars.toFloat(),
                                    manualMinPrice = manualMinPrice,
                                    manualMaxPrice = manualMaxPrice,
                                    topMarginPx = mainH * (settings.marginTopPercent / 100f),
                                    bottomMarginPx = mainH * (settings.marginBottomPercent / 100f)
                                )
                                drawingManager.onDragStart(offset, mapper)
                                dragArea = -10
                                return@detectDragGestures
                            }
                            if (isLongPressing) {
                                crosshairPosition?.let {
                                    crosshairDragOffset = Offset(it.x - offset.x, it.y - offset.y)
                                }
                                dragArea = 0
                                return@detectDragGestures
                            }

                            var found = false
                            val tIndH = bottomIndicators.sumOf {
                                if (it.isVisible) (indicatorHeights[it.id] ?: defaultHeightPx).toDouble()
                                else minimizedHeightPx.toDouble()
                            }.toFloat()
                            var currentY = plotsHeightPx - tIndH

                            val sepRadius = with(density) { 15.dp.toPx() }
                            val anyVisible = bottomIndicators.any { it.isVisible }

                            if (anyVisible) {
                                for (i in 0 until bottomIndicators.size) {
                                    if (abs(offset.y - currentY) < sepRadius) {
                                        dragArea = 4; draggingSeparatorIdx = i; found = true; break
                                    }
                                    val ind = bottomIndicators[i]
                                    currentY += if (ind.isVisible) (indicatorHeights[ind.id] ?: defaultHeightPx) else minimizedHeightPx
                                }
                            }

                            if (!found) {
                                currentY = plotsHeightPx - tIndH
                                for (ind in bottomIndicators) {
                                    val indH = if (ind.isVisible) (indicatorHeights[ind.id] ?: defaultHeightPx) else minimizedHeightPx
                                    if (offset.y in currentY..currentY + indH) {
                                        if (ind.isVisible) {
                                            if (offset.x > chartWidthPx) { dragArea = 5; draggingIndicatorId = ind.id }
                                            else { dragArea = 6; draggingIndicatorId = ind.id }
                                            found = true; break
                                        }
                                    }
                                    currentY += indH
                                }
                            }

                            if (!found) {
                                dragArea = when {
                                    offset.x > chartWidthPx -> 1
                                    offset.y > plotsHeightPx -> 2
                                    else -> 3
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            handleMainCanvasDrag(change, dragAmount)
                        },
                        onDragEnd = {
                            if (dragArea == -10) {
                                if (drawingManager.onDragEnd()) {
                                    onDrawingToolUsed()
                                }
                            }
                            draggingSeparatorIdx = -1; draggingIndicatorId = null
                        }
                    )
                }
                .pointerInput(visibleCandles, allCandles, scrollOffset, displayCount, manualMinPrice, manualMaxPrice, mainH, chartWidthPx, indicators, drawingManager.activeTool, drawingManager.drawingState) {
                    detectTapGestures(
                        onTap = { offset -> handleMainCanvasTap(offset) },
                        onDoubleTap = { offset ->
                            val tIndH = currentBottomIndicatorsHeight()
                            val mainHeight = plotsHeightPx - tIndH

                            if (offset.x > chartWidthPx) {
                                if (offset.y <= mainHeight) {
                                    // Double-tap on price axis resets to auto-scale
                                    isAutoHeight = true
                                } else {
                                    var curYPos = mainHeight
                                    for (ind in bottomIndicators) {
                                        val indH = if (ind.isVisible) (indicatorHeights[ind.id] ?: defaultHeightPx) else minimizedHeightPx
                                        if (offset.y in curYPos..curYPos + indH) { 
                                            if (ind.isVisible) indicatorAutoHeight[ind.id] = true
                                            break 
                                        }
                                        curYPos += indH
                                    }
                                }
                            } else if (offset.y <= mainHeight) {
                                val candleW = if (currentDisplayCount > 0) chartWidthPx / (currentDisplayCount + settings.marginRightBars) else 0f
                                if (candleW > 0) {
                                    val rawRange = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.001f)
                                    val range = rawRange * 1.1f
                                    val minP = (manualMaxPrice + manualMinPrice) / 2f - (range / 2f)
                                    val topMarginPx = (mainHeight * (settings.marginTopPercent / 100f))
                                    val bottomMarginPx = (mainHeight * (settings.marginBottomPercent / 100f))
                                    val effectiveMainH = mainHeight - topMarginPx - bottomMarginPx
                                    val denormY: (Float) -> Float = { minP + ((effectiveMainH - (it - topMarginPx)) / effectiveMainH * range) }
                                    
                                    val idx = round(allCandles.size - 1 - scrollOffset - (chartWidthPx - offset.x - candleW / 2f) / candleW).toInt()
                                    val searchThreshold = range * 0.03f
                                    val priceAtY = denormY(offset.y)

                                    var foundInd = false
                                    for (ind in overlayIndicators) {
                                        if (!ind.isVisible) continue
                                        val found = isOverlayIndicatorHit(ind, idx, priceAtY, searchThreshold)
                                        if (found) {
                                            onIndicatorSettingsRequest(ind)
                                            foundInd = true
                                            break
                                        }
                                    }
                                    
                                    if (!foundInd && idx in allCandles.indices) {
                                        val candle = allCandles[idx]
                                        val threshold = (candle.high - candle.low) * 0.2f + (range * 0.02f)
                                        if (priceAtY in (candle.low - threshold)..(candle.high + threshold)) {
                                            currentOnSettingsRequest()
                                        }
                                    }
                                }
                            } else {
                                var curYPos = mainHeight
                                var idxBottom = 0
                                var handled = false
                                while (idxBottom < bottomIndicators.size && !handled) {
                                    val ind = bottomIndicators[idxBottom]
                                    val indH = if (ind.isVisible) (indicatorHeights[ind.id] ?: defaultHeightPx) else minimizedHeightPx
                                    if (offset.y in curYPos..curYPos + indH) {
                                        if (ind.isVisible) {
                                            val rangeInd = indicatorRanges[ind.id] ?: Pair(0f, 100f)
                                            val rV = (rangeInd.second - rangeInd.first).coerceAtLeast(0.001f)
                                            val denormIndY: (Float) -> Float = { rangeInd.first + (curYPos + indH - it) / indH * rV }
                                            val valAtY = denormIndY(offset.y)
                                            val idx = candleIndexAtX(offset.x)

                                            if (handleBottomIndicatorSettingsTap(ind, idx, valAtY, rV)) {
                                                handled = true
                                            }
                                        }
                                    }
                                    curYPos += indH
                                    idxBottom++
                                }
                            }
                        },
                        onLongPress = { offset ->
                            if (offset.x <= chartWidthPx) {
                                isLongPressing = true
                                crosshairPosition = Offset(
                                    snapXToCandleCenter(offset.x),
                                    offset.y.coerceIn(0f, h)
                                )
                                crosshairDragOffset = Offset.Zero
                            }
                        }
                    )
                }
        ) {
            ChartDrawer.drawBackground(this, settings, w, h)
            
            val topMarginPx = (mainH * (settings.marginTopPercent / 100f))
            val bottomMarginPx = (mainH * (settings.marginBottomPercent / 100f))
            val effectiveMainH = mainH - topMarginPx - bottomMarginPx

            val rawRange = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.001f)
            val range = rawRange * 1.1f
            val minP = (manualMaxPrice + manualMinPrice) / 2f - (range / 2f)
            
            val normY: (Float) -> Float = { topMarginPx + effectiveMainH - ((it - minP) / range * effectiveMainH) }
            val denormY: (Float) -> Float = { minP + ((effectiveMainH - (it - topMarginPx)) / effectiveMainH * range) }

            val candleW = if (displayCount > 0) chartWidthPx / (displayCount + settings.marginRightBars) else 0f
            val bodyW = candleW * 0.7f

            val visibleMaxP = denormY(0f)
            val visibleMinP = denormY(mainH)
            
            ChartDrawer.drawGridAndLabels(this, textMeasurer, chartWidthPx, priceWidthPx, mainH, visibleMaxP - visibleMinP, visibleMinP, normY, settings, textStyle)
            
            ChartDrawer.drawTimeLabels(this, allCandles, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, plotsHeightPx, displayCount, timeFormatter, settings, textStyle, textMeasurer, labelBgColor, timeframe)

            ChartDrawer.drawWatermark(this, textMeasurer, chartWidthPx, mainH, symbol, timeframe, settings)

            clipRect(right = chartWidthPx, bottom = mainH) {
                sessionsIndicator?.let { ChartDrawer.drawSessions(this, allCandles, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, mainH, it, normY, textMeasurer) }
                ChartDrawer.drawOverlayIndicators(this, overlayIndicators, allCandles, smaValues, emaValues, hmaValues, vwapValues, bbValues, atrBandsValues, stValues, alligatorValues, ichimokuValues, ribbonValues, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY)

                ChartDrawer.drawCandles(this, visibleCandles, allCandles, startIdx, allCandles.size, scrollOffset, candleW, bodyW, chartWidthPx, mainH, volumeIndicator, volumeMaValues, smoothedMaValues, settings, normY)
                
                if (isSelected) {
                    ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> (allCandles[idx].high + allCandles[idx].low) / 2f }
                }
                
                selectedIndicatorId?.let { selId ->
                    overlayIndicators.find { it.id == selId }?.let { ind ->
                        when (ind) {
                            is Indicator.SMA -> smaValues[ind.id]?.let { vals ->
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> vals.getOrNull(idx) }
                            }
                            is Indicator.EMA -> emaValues[ind.id]?.let { vals ->
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> vals.getOrNull(idx) }
                            }
                            is Indicator.HMA -> hmaValues[ind.id]?.let { vals ->
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> vals.getOrNull(idx) }
                            }
                            is Indicator.VWAP -> vwapValues[ind.id]?.let { res ->
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.vwap.getOrNull(idx) }
                                if (ind.showBands) {
                                    ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.upper1.getOrNull(idx) }
                                    ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.lower1.getOrNull(idx) }
                                }
                            }
                            is Indicator.BollingerBands -> bbValues[ind.id]?.let { bb ->
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> bb.middle.getOrNull(idx) }
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> bb.upper.getOrNull(idx) }
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> bb.lower.getOrNull(idx) }
                            }
                            is Indicator.ATRBands -> atrBandsValues[ind.id]?.let { ab ->
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> ab.upper.getOrNull(idx) }
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> ab.lower.getOrNull(idx) }
                                if (ind.showTPBands) {
                                    ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> ab.tpUpper.getOrNull(idx) }
                                    ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> ab.tpLower.getOrNull(idx) }
                                }
                            }
                            is Indicator.Alligator -> alligatorValues[ind.id]?.let { res ->
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.jaw.getOrNull(idx) }
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.teeth.getOrNull(idx) }
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.lips.getOrNull(idx) }
                            }
                            is Indicator.Ichimoku -> ichimokuValues[ind.id]?.let { res ->
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.tenkanSen.getOrNull(idx) }
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.kijunSen.getOrNull(idx) }
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.senkouSpanA.getOrNull(idx) }
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.senkouSpanB.getOrNull(idx) }
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> res.chikouSpan.getOrNull(idx) }
                            }
                            is Indicator.Supertrend -> stValues[ind.id]?.let { st ->
                                ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> st.values.getOrNull(idx) }
                            }
                            is Indicator.Ribbon -> ribbonValues[ind.id]?.let { res ->
                                res.mas.forEach { ma ->
                                    ChartDrawer.drawSelectionHandles(this, allCandles.size, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY) { idx -> ma.getOrNull(idx) }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }

            ChartDrawer.drawBottomIndicators(this, bottomIndicators, indicatorHeights, defaultHeightPx, rsiValues, rsiMaValues, macdValues, stochValues, atrValues, indicatorRanges, visibleCandles, allCandles.size, startIdx, endIdx, scrollOffset, candleW, bodyW, chartWidthPx, priceWidthPx, mainH, settings, textMeasurer, textStyle, crosshairTextStyle, draggingSeparatorIdx, dragArea, minimizedHeightPx)

            ChartDrawer.drawOverlayIndicatorLabels(this, overlayIndicators, smaValues, emaValues, hmaValues, vwapValues, bbValues, atrBandsValues, stValues, alligatorValues, ichimokuValues, ribbonValues, chartWidthPx, priceWidthPx, mainH, textMeasurer, crosshairTextStyle, normY)

            if (allCandles.isNotEmpty()) {
                ChartDrawer.drawLastPriceLabel(this, allCandles.last(), chartWidthPx, mainH, priceWidthPx, settings, crosshairTextStyle, textMeasurer, normY)
            }

            // Draw simple trend lines
            clipRect(right = chartWidthPx, bottom = mainH) {
                val candleW = if (currentDisplayCount > 0) chartWidthPx / (currentDisplayCount + settings.marginRightBars) else 0f
                val topM = mainH * (settings.marginTopPercent / 100f)
                val botM = mainH * (settings.marginBottomPercent / 100f)
                val effectiveMainH = mainH - topM - botM
                val rawRange = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.001f)
                val range = rawRange * 1.1f
                val minP = (manualMaxPrice + manualMinPrice) / 2f - (range / 2f)
                val normY: (Float) -> Float = { topM + effectiveMainH - ((it - minP) / range * effectiveMainH) }

                // Draw completed simple trend lines
                simpleTrendLines.forEach { (p1, p2) ->
                    val (startIndex, startPrice) = p1
                    val (endIndex, endPrice) = p2
                    val startX = if (candleW > 0) chartWidthPx - (candleW / 2f) - ((allCandles.size - 1 - startIndex - scrollOffset) * candleW) else 0f
                    val startY = normY(startPrice)
                    val endX = if (candleW > 0) chartWidthPx - (candleW / 2f) - ((allCandles.size - 1 - endIndex - scrollOffset) * candleW) else 0f
                    val endY = normY(endPrice)

                    // Draw line with custom style (red, dashed)
                    drawLine(
                        color = Color.Red,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
                    )

                    // Draw bubbles at endpoints
                    drawCircle(Color.Red, 6f, Offset(startX, startY))
                    drawCircle(Color.Red, 6f, Offset(endX, endY))
                }

                // Draw preview line when placing second point
                if (simpleDrawingState == SimpleDrawingState.PLACING_SECOND_POINT && firstPoint != null && firstPointScreen != null && crosshairPosition != null) {
                    val (startIndex, startPrice) = firstPoint!!
                    val startX = if (candleW > 0) chartWidthPx - (candleW / 2f) - ((allCandles.size - 1 - startIndex - scrollOffset) * candleW) else 0f
                    val startY = normY(startPrice)

                    drawLine(
                        color = Color.Red,
                        start = Offset(startX, startY),
                        end = crosshairPosition!!,
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
                    )

                    // Draw bubble at first point
                    drawCircle(Color.Red, 6f, Offset(startX, startY))
                }
            }

            // Draw trend line if in drawing mode
            // (Now handled by DrawingManager via ChartDrawer.drawAllDrawings below)

              // ══ Draw all completed drawings + preview ══════════════════════
              clipRect(right = chartWidthPx, bottom = mainH) {
                  val mapper = buildMapper(
                      chartWidthPx = chartWidthPx,
                      mainH = mainH,
                      totalCandles = allCandles.size,
                      scrollOffset = scrollOffset,
                      displayCount = displayCount,
                      marginRightBars = settings.marginRightBars.toFloat(),
                      manualMinPrice = manualMinPrice,
                      manualMaxPrice = manualMaxPrice,
                      topMarginPx = mainH * (settings.marginTopPercent / 100f),
                      bottomMarginPx = mainH * (settings.marginBottomPercent / 100f)
                  )
                  ChartDrawer.drawAllDrawings(
                      this, drawingManager.completedDrawings, mapper,
                      drawingManager.selectedDrawingId,
                      drawingManager.drawingState,
                      textMeasurer, allCandles, density, activeDrawingTool
                  )
              }

              crosshairPosition?.let { p ->
                 ChartDrawer.drawCrosshair(
                     this, p, chartWidthPx, h, mainH, priceWidthPx, timeHeightPx,
                     settings, crosshairTextStyle, textMeasurer, allCandles,
                     scrollOffset, candleW, crosshairTimeFormatter,
                     denormalizeY = denormY, bottomIndicators, indicatorHeights,
                     indicatorRanges, defaultHeightPx, crosshairLabelBgColor,
                     timeframe = timeframe,
                     displayCount = displayCount
                 )
             }

            ChartDrawer.drawBorders(this, chartWidthPx, priceWidthPx, h, settings)
        }

        val candleW_hud = if (currentDisplayCount > 0) chartWidthPx / (currentDisplayCount + settings.marginRightBars) else 0f
        val hoverIdx = crosshairPosition?.let { p ->
            if (candleW_hud > 0) (allCandles.size - 1 - scrollOffset - (chartWidthPx - p.x - candleW_hud / 2f) / candleW_hud).roundToInt().coerceIn(allCandles.indices) else null
        }
        
        if (crosshairPosition != null) {
            val displayedCandle = hoverIdx?.let { allCandles[it] } ?: allCandles.lastOrNull()

            displayedCandle?.let { candle ->
                val hudTextColor = colorScheme.onSecondaryContainer
                val isUp = candle.close >= candle.open
                val trendColor = if (isUp) Color(0xFF26A69A) else Color(0xFFEF5350)
                
                Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp)) {
                    @Suppress("DEPRECATION")
                    Text("O", color = hudTextColor.copy(0.6f), fontSize = 11.sp)
                    @Suppress("DEPRECATION")
                    Text(ChartUtils.formatPrice(candle.open), color = trendColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                    @Suppress("DEPRECATION")
                    Text("H", color = hudTextColor.copy(0.6f), fontSize = 11.sp)
                    @Suppress("DEPRECATION")
                    Text(ChartUtils.formatPrice(candle.high), color = trendColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                    @Suppress("DEPRECATION")
                    Text("L", color = hudTextColor.copy(0.6f), fontSize = 11.sp)
                    @Suppress("DEPRECATION")
                    Text(ChartUtils.formatPrice(candle.low), color = trendColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                    @Suppress("DEPRECATION")
                    Text("C", color = hudTextColor.copy(0.6f), fontSize = 11.sp)
                    @Suppress("DEPRECATION")
                    Text(ChartUtils.formatPrice(candle.close), color = trendColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        }

        val surfCol = colorScheme.surface
        val txtCol = colorScheme.onSecondaryContainer

        val allOverlay = (if (volumeIndicator != null) listOf(volumeIndicator) else emptyList()) + overlayIndicators

        if (allOverlay.isNotEmpty()) {
            val overlayTopPadding = if (crosshairPosition != null) 18.dp else 2.dp
            Column(modifier = Modifier.padding(start = 8.dp, top = overlayTopPadding)) {
                if (showIndicatorLabels) {
                    if (!overlayCollapsed) {
                        allOverlay.forEach { ind ->
                            val values = if (crosshairPosition != null && ind.isVisible) {
                                when (ind) {
                                    is Indicator.Volume -> {
                                        val v = hoverIdx?.let { allCandles.getOrNull(it)?.volume } ?: allCandles.lastOrNull()?.volume
                                        val c = hoverIdx?.let { allCandles.getOrNull(it) } ?: allCandles.lastOrNull()
                                        val color = if (c?.let { it.close >= it.open } == true) settings.upColor else settings.downColor
                                        v?.let { listOf(ChartUtils.formatVolume(it) to color) } ?: emptyList()
                                    }
                                    is Indicator.SMA -> {
                                        val list = smaValues[ind.id] ?: emptyList()
                                        val v = hoverIdx?.let { list.getOrNull(it) } ?: list.lastOrNull { it != null }
                                        v?.let { listOf(String.format(Locale.getDefault(), "%.2f", it) to ind.color) } ?: emptyList()
                                    }
                                    is Indicator.EMA -> {
                                        val list = emaValues[ind.id] ?: emptyList()
                                        val v = hoverIdx?.let { list.getOrNull(it) } ?: list.lastOrNull { it != null }
                                        v?.let { listOf(String.format(Locale.getDefault(), "%.2f", it) to ind.color) } ?: emptyList()
                                    }
                                    is Indicator.HMA -> {
                                        val list = hmaValues[ind.id] ?: emptyList()
                                        val v = hoverIdx?.let { list.getOrNull(it) } ?: list.lastOrNull { it != null }
                                        v?.let { listOf(String.format(Locale.getDefault(), "%.2f", it) to ind.color) } ?: emptyList()
                                    }
                                    is Indicator.VWAP -> vwapValues[ind.id]?.let { res ->
                                        val idx = hoverIdx ?: res.vwap.indices.lastOrNull { res.vwap[it] != null }
                                        idx?.let { i ->
                                            val list = mutableListOf(
                                                String.format(Locale.getDefault(), "%.2f", res.vwap.getOrNull(i) ?: 0f) to ind.color
                                            )
                                            if (ind.showBands) {
                                                list.add(String.format(Locale.getDefault(), "%.2f", res.upper1.getOrNull(i) ?: 0f) to ind.upperColor)
                                                list.add(String.format(Locale.getDefault(), "%.2f", res.lower1.getOrNull(i) ?: 0f) to ind.lowerColor)
                                            }
                                            list
                                        }
                                    } ?: emptyList()
                                    is Indicator.BollingerBands -> bbValues[ind.id]?.let { bb ->
                                        val idx = hoverIdx ?: bb.middle.indices.lastOrNull { bb.middle[it] != null }
                                        idx?.let { i ->
                                            listOf(
                                                String.format(Locale.getDefault(), "%.2f", bb.upper.getOrNull(i) ?: 0f) to ind.upperColor,
                                                String.format(Locale.getDefault(), "%.2f", bb.middle.getOrNull(i) ?: 0f) to ind.middleColor,
                                                String.format(Locale.getDefault(), "%.2f", bb.lower.getOrNull(i) ?: 0f) to ind.lowerColor
                                            )
                                        }
                                    } ?: emptyList()
                                    is Indicator.ATRBands -> atrBandsValues[ind.id]?.let { ab ->
                                        val idx = hoverIdx ?: ab.upper.indices.lastOrNull { ab.upper[it] != null }
                                        idx?.let { i ->
                                            val res = mutableListOf(
                                                String.format(Locale.getDefault(), "%.2f", ab.upper.getOrNull(i) ?: 0f) to ind.upperColor,
                                                String.format(Locale.getDefault(), "%.2f", ab.lower.getOrNull(i) ?: 0f) to ind.lowerColor
                                            )
                                            if (ind.showTPBands) {
                                                res.add(String.format(Locale.getDefault(), "%.2f", ab.tpUpper.getOrNull(i) ?: 0f) to ind.tpUpperColor)
                                                res.add(String.format(Locale.getDefault(), "%.2f", ab.tpLower.getOrNull(i) ?: 0f) to ind.tpLowerColor)
                                            }
                                            res
                                        }
                                    } ?: emptyList()
                                    is Indicator.Alligator -> alligatorValues[ind.id]?.let { res ->
                                        val idx = hoverIdx ?: res.jaw.indices.lastOrNull { res.jaw[it] != null }
                                        idx?.let { i ->
                                            listOf(
                                                String.format(Locale.getDefault(), "%.2f", res.jaw.getOrNull(i) ?: 0f) to ind.jawColor,
                                                String.format(Locale.getDefault(), "%.2f", res.teeth.getOrNull(i) ?: 0f) to ind.teethColor,
                                                String.format(Locale.getDefault(), "%.2f", res.lips.getOrNull(i) ?: 0f) to ind.lipsColor
                                            )
                                        }
                                    } ?: emptyList()
                                    is Indicator.Ichimoku -> ichimokuValues[ind.id]?.let { res ->
                                        val idx = hoverIdx ?: res.tenkanSen.indices.lastOrNull { res.tenkanSen[it] != null }
                                        idx?.let { i ->
                                            listOf(
                                                String.format(Locale.getDefault(), "%.2f", res.tenkanSen.getOrNull(i) ?: 0f) to ind.tenkanColor,
                                                String.format(Locale.getDefault(), "%.2f", res.kijunSen.getOrNull(i) ?: 0f) to ind.kijunColor,
                                                String.format(Locale.getDefault(), "%.2f", res.senkouSpanA.getOrNull(i) ?: 0f) to ind.senkouAColor,
                                                String.format(Locale.getDefault(), "%.2f", res.senkouSpanB.getOrNull(i) ?: 0f) to ind.senkouBColor,
                                                String.format(Locale.getDefault(), "%.2f", res.chikouSpan.getOrNull(i) ?: 0f) to ind.chikouColor
                                            )
                                        }
                                    } ?: emptyList()
                                    is Indicator.Supertrend -> stValues[ind.id]?.let { st ->
                                        val idx = hoverIdx ?: st.values.indices.lastOrNull { st.values[it] != null }
                                        idx?.let { i ->
                                            val v = st.values.getOrNull(i)
                                            val up = st.isUp.getOrNull(i) ?: true
                                            v?.let { listOf(String.format(Locale.getDefault(), "%.2f", it) to (if (up) ind.upColor else ind.downColor)) } ?: emptyList()
                                        }
                                    } ?: emptyList()
                                    is Indicator.Ribbon -> ribbonValues[ind.id]?.let { res ->
                                        val idx = hoverIdx ?: res.mas.firstOrNull()?.indices?.lastOrNull { res.mas.firstOrNull()?.get(it) != null }
                                        idx?.let { i ->
                                            val lastMa = res.mas.last()
                                            val lastV = lastMa.getOrNull(i) ?: 0f
                                            val refV = res.refMa.getOrNull(i) ?: 0f
                                            val diff = lastV - (lastMa.getOrNull(i - 1) ?: lastV)
                                            val color = when {
                                                diff >= 0 && lastV > refV -> Color(0xFF00FF00)
                                                diff < 0 && lastV > refV -> Color(0xFF800000)
                                                diff <= 0 && lastV < refV -> Color(0xFFFF0000)
                                                diff >= 0 && lastV < refV -> Color(0xFF008000)
                                                else -> Color(0xFF808080)
                                            }
                                            listOf(String.format(Locale.getDefault(), "%.2f", lastV) to color)
                                        }
                                    } ?: emptyList()
                                    is Indicator.Sessions -> emptyList()
                                    else -> emptyList()
                                }
                            } else emptyList()
                            
                            IndicatorLabelToolbar(
                                indicator = ind,
                                isExpanded = expandedIndicatorId == ind.id,
                                values = values,
                                textColor = txtCol,
                                surfaceColor = surfCol,
                                onToggleExpand = { expandedIndicatorId = if (expandedIndicatorId == ind.id) null else ind.id },
                                onToggleVisibility = { onIndicatorToggleVisibility(ind) },
                                onSettings = { onIndicatorSettingsRequest(ind) },
                                onRemove = { onIndicatorRemove(ind) }
                            )
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .height(20.dp)
                            .border(0.5.dp, txtCol.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .clickable { overlayCollapsed = !overlayCollapsed }
                            .padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            if (overlayCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = txtCol.copy(alpha = 0.6f),
                            modifier = Modifier.size(25.dp)
                        )
                        if (overlayCollapsed) {
                            @Suppress("DEPRECATION")
                            Text(
                                text = allOverlay.size.toString(),
                                color = txtCol.copy(alpha = 0.6f),
                                fontSize = 15.sp,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        if (bottomIndicators.isNotEmpty()) {
            var currentY = mainH
            bottomIndicators.forEach { ind ->
                val hInd = if (ind.isVisible) (indicatorHeights[ind.id] ?: defaultHeightPx) else minimizedHeightPx
                val transY = currentY + with(density) { 2.dp.toPx() }
                Column(modifier = Modifier.padding(start = 8.dp).graphicsLayer { translationY = transY }) {
                    if (showIndicatorLabels) {
                        val values = if (crosshairPosition != null && ind.isVisible) {
                            when (ind) {
                                is Indicator.RSI -> {
                                    val list = rsiValues[ind.id] ?: emptyList()
                                    val v = hoverIdx?.let { list.getOrNull(it) } ?: list.lastOrNull { it != null }
                                    v?.let { listOf(String.format(Locale.getDefault(), "%.2f", it) to ind.color) } ?: emptyList()
                                }
                                is Indicator.MACD -> macdValues[ind.id]?.let { res ->
                                    val idx = hoverIdx ?: res.macdLine.indices.lastOrNull { res.macdLine[it] != null }
                                    idx?.let { i ->
                                        listOf(
                                            String.format(Locale.getDefault(), "%.2f", res.macdLine.getOrNull(i) ?: 0f) to ind.macdColor,
                                            String.format(Locale.getDefault(), "%.2f", res.signalLine.getOrNull(i) ?: 0f) to ind.signalColor,
                                            String.format(Locale.getDefault(), "%.2f", res.histogram.getOrNull(i) ?: 0f) to (if ((res.histogram.getOrNull(i) ?: 0f) >= 0) ind.histColorUp else ind.histColorDown)
                                        )
                                    }
                                } ?: emptyList()
                                is Indicator.Stochastic -> stochValues[ind.id]?.let { res ->
                                    val idx = hoverIdx ?: res.k.indices.lastOrNull { res.k[it] != null }
                                    idx?.let { i ->
                                        listOf(
                                            String.format(Locale.getDefault(), "%.2f", res.k.getOrNull(i) ?: 0f) to ind.kColor,
                                            String.format(Locale.getDefault(), "%.2f", res.d.getOrNull(i) ?: 0f) to ind.dColor
                                        )
                                    }
                                } ?: emptyList()
                                is Indicator.ATR -> {
                                    val list = atrValues[ind.id] ?: emptyList()
                                    val v = hoverIdx?.let { list.getOrNull(it) } ?: list.lastOrNull { it != null }
                                    v?.let { listOf(String.format(Locale.getDefault(), "%.2f", it) to ind.color) } ?: emptyList()
                                }
                                else -> emptyList()
                            }
                        } else emptyList()
                        
                        IndicatorLabelToolbar(
                            indicator = ind,
                            isExpanded = expandedIndicatorId == ind.id,
                            values = values,
                            textColor = txtCol,
                            surfaceColor = surfCol,
                            onToggleExpand = { expandedIndicatorId = if (expandedIndicatorId == ind.id) null else ind.id },
                            onToggleVisibility = { onIndicatorToggleVisibility(ind) },
                            onSettings = { onIndicatorSettingsRequest(ind) },
                            onRemove = { onIndicatorRemove(ind) }
                        )
                    }
                }
                currentY += hInd
            }
        }

        // Settings icon under price bar (bottom-right)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 8.dp)
        ) {
            IconButton(
                onClick = onOpenChartSettings,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Favorites Toolbar - Centered below top bar, clamped to screen bounds
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            val favoritesToolsList = DrawingTool.entries.filter { favoriteTools.contains(it.name) }
            if (favoritesToolsList.isNotEmpty()) {
                val isDarkChart = settings.backgroundColor.luminance() < 0.5f
                val favColors = getFavoritesBarColors(isDarkChart)

                // y=0 in chart coords = bottom of top app bar; small padding only
                val topBarMinY = with(density) { 8.dp.toPx() }

                val currentPos = favoritesBarPosition
                val barW = favoritesBarSizePx.x

                // Compute display position: initialize centered if not yet set
                val displayPos = currentPos ?: Offset((w - barW) / 2f, topBarMinY)

                Box(
                    modifier = Modifier
                        .offset { IntOffset(displayPos.x.roundToInt(), displayPos.y.roundToInt()) }
                        .onGloballyPositioned { coords ->
                            val newW = coords.size.width.toFloat()
                            val newH = coords.size.height.toFloat()
                            favoritesBarSizePx = Offset(newW, newH)
                            // Set centered initial position once size is known
                            if (favoritesBarPosition == null) {
                                favoritesBarPosition = Offset(
                                    ((w - newW) / 2f).coerceIn(0f, (w - newW).coerceAtLeast(0f)),
                                    topBarMinY
                                )
                            }
                        }
                        .pointerInput(w, h) {
                            detectDragGestures(
                                onDragStart = { isDraggingFavoritesBar = true },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val bW = favoritesBarSizePx.x
                                    val bH = favoritesBarSizePx.y
                                    val prev = favoritesBarPosition ?: Offset((w - bW) / 2f, topBarMinY)
                                    favoritesBarPosition = Offset(
                                        (prev.x + dragAmount.x).coerceIn(0f, (w - bW).coerceAtLeast(0f)),
                                        (prev.y + dragAmount.y).coerceIn(topBarMinY, (h - bH).coerceAtLeast(topBarMinY))
                                    )
                                },
                                onDragEnd = { isDraggingFavoritesBar = false },
                                onDragCancel = { isDraggingFavoritesBar = false }
                            )
                        }
                        .graphicsLayer {
                            shadowElevation = 8.dp.toPx()
                            shape = RoundedCornerShape(8.dp)
                            clip = true
                        }
                        .background(favColors.barBg, RoundedCornerShape(8.dp))
                        .border(0.5.dp, favColors.barBorder, RoundedCornerShape(8.dp))
                        .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Drag Handle - 6 dots (2x3 grid)
                        DragHandle(gripColor = favColors.grip, dotSize = 1.8f, spacing = 2.5f)

                        val context = LocalContext.current
                        favoritesToolsList.forEach { tool ->
                            // Nouvelle logique de dessin pour la ligne de tendance
                            if (tool == DrawingTool.TREND_LINE) {
                                Icon(
                                    painter = painterResource(id = tool.iconRes),
                                    contentDescription = null,
                                    tint = if (drawingManager.activeTool == tool) favColors.active else favColors.icon,
                                    modifier = Modifier.size(24.dp)
                                        .clickable(enabled = !isDraggingFavoritesBar) {
                                            // 1. Activation du mode de dessin
                                            simpleDrawingState = SimpleDrawingState.PLACING_FIRST_POINT
                                            firstPoint = null
                                            firstPointScreen = null
                                            // Viseur au centre
                                            crosshairPosition = Offset(chartWidthPx / 2f, mainH / 2f)
                                            crosshairDragOffset = Offset.Zero
                                            isLongPressing = true
                                            // 2. Les autres étapes sont gérées dans la logique de gestion des événements du chart
                                        }
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = tool.iconRes),
                                    contentDescription = null,
                                    tint = if (drawingManager.activeTool == tool) favColors.active else favColors.icon,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

