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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
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
import com.bthr.backtest.util.IndicatorCalculators
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

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
    onIndicatorSettingsRequest: (Indicator) -> Unit = {},
    onIndicatorToggleVisibility: (Indicator) -> Unit = {},
    onIndicatorRemove: (Indicator) -> Unit = {},
    settings: ChartSettings = ChartSettings(),
    timeZone: TimeZone = TimeZone.getDefault(),
    favoriteTools: Set<String> = emptySet(),
    onFavoriteToolsChange: (Set<String>) -> Unit = {},
    activeDrawingTool: DrawingTool = DrawingTool.NONE,
    onDrawingToolUsed: () -> Unit = {}
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    
    val colorScheme = MaterialTheme.colorScheme
    val textStyle = TextStyle(color = colorScheme.onBackground, fontSize = settings.scaleTextSize.sp)
    val crosshairTextStyle = TextStyle(
        color = if (settings.backgroundColor.luminance() > 0.5f) Color.Black else Color.White,
        fontSize = 10.sp
    )
    
    val currentOnZoom by rememberUpdatedState(onZoom)
    val currentOnPan by rememberUpdatedState(onPan)
    val currentDisplayCount by rememberUpdatedState(displayCount)
    val currentOnSettingsRequest by rememberUpdatedState(onSettingsRequest)

    var crosshairPosition by remember { mutableStateOf<Offset?>(null) }
    var isLongPressing by remember { mutableStateOf(false) }

    // Favorites bar position state
    var favoritesBarPosition by remember { mutableStateOf(Offset(0f, 0f)) }
    var isDraggingFavoritesBar by remember { mutableStateOf(false) }
    var favoritesBarInitialized by remember { mutableStateOf(false) }

    var isAutoHeight by remember { mutableStateOf(true) }
    var manualMinPrice by remember { mutableFloatStateOf(0f) }
    var manualMaxPrice by remember { mutableFloatStateOf(100f) }

    val indicatorHeights = remember { mutableStateMapOf<String, Float>() }
    val defaultHeightPx = with(density) { 100.dp.toPx() }
    val minIndicatorHeightPx = with(density) { 40.dp.toPx() }
    val minimizedHeightPx = with(density) { 24.dp.toPx() }

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
    var showIndicatorLabels by remember { mutableStateOf(true) }
    var overlayCollapsed by remember { mutableStateOf(false) }

    val timeFormatter = remember(timeZone) { SimpleDateFormat("HH:mm\ndd MMM", Locale.getDefault()).apply { this.timeZone = timeZone } }
    val crosshairTimeFormatter = remember(timeZone) { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).apply { this.timeZone = timeZone } }

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

    val smaValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.SMA>().associate { it.id to IndicatorCalculators.calculateSMA(allCandles, it.period, it.source) } }
    val emaValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.EMA>().associate { it.id to IndicatorCalculators.calculateEMA(allCandles, it.period, it.source) } }
    val hmaValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.HMA>().associate { it.id to IndicatorCalculators.calculateHMA(allCandles, it.period, it.source) } }
    val vwapValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.VWAP>().associate { it.id to IndicatorCalculators.calculateVWAP(allCandles, it.anchor, it.source, it.bandMult1, it.bandMult2, it.bandMult3) } }
    val bbValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.BollingerBands>().associate { it.id to IndicatorCalculators.calculateBollingerBands(allCandles, it.period, it.stdDev) } }
    val atrBandsValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.ATRBands>().associate { it.id to IndicatorCalculators.calculateATRBands(allCandles, it.period, it.multiplier, it.source, it.showTPBands, it.tpScaleFactor) } }
    val stValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.Supertrend>().associate { it.id to IndicatorCalculators.calculateSupertrend(allCandles, it.period, it.multiplier) } }
    val alligatorValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.Alligator>().associate { it.id to IndicatorCalculators.calculateAlligator(allCandles, it.jawPeriod, it.jawOffset, it.teethPeriod, it.teethOffset, it.lipsPeriod, it.lipsOffset) } }
    val ichimokuValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.Ichimoku>().associate { it.id to IndicatorCalculators.calculateIchimoku(allCandles, it.tenkanPeriod, it.kijunPeriod, it.senkouBPeriod, it.displacement) } }
    val ribbonValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.Ribbon>().associate { it.id to IndicatorCalculators.calculateRibbon(allCandles, it.isExponential, it.source, it.refPeriod) } }
    
    val rsiValues = remember(allCandles, indicators) { indicators.filterIsInstance<Indicator.RSI>().associate { it.id to IndicatorCalculators.calculateRSI(allCandles, it.period, it.source) } }
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

    val volumeMaValues = remember(allCandles, volumeIndicator) { volumeIndicator?.let { IndicatorCalculators.calculateVolumeMA(allCandles, it.maLength, it.maType) } }
    val smoothedMaValues = remember(allCandles, volumeIndicator) { volumeIndicator?.let { IndicatorCalculators.calculateVolumeMA(allCandles, it.smoothingLength, it.smoothingLine) } }

    LaunchedEffect(visibleCandles, bottomIndicators, rsiValues, rsiMaValues, macdValues, stochValues, atrValues) {
        bottomIndicators.forEach { ind ->
            if (indicatorAutoHeight[ind.id] != false) {
                val vals = when (ind) {
                    is Indicator.RSI -> (rsiValues[ind.id] ?: emptyList()) + (rsiMaValues[ind.id] ?: emptyList())
                    is Indicator.MACD -> macdValues[ind.id]?.let { it.macdLine + it.signalLine + it.histogram } ?: emptyList()
                    is Indicator.Stochastic -> stochValues[ind.id]?.let { it.k + it.d } ?: emptyList()
                    is Indicator.ATR -> atrValues[ind.id] ?: emptyList()
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
        val chartWidthPx = w - priceWidthPx
        val plotsHeightPx = h - timeHeightPx
        val labelBgColor = colorScheme.background
        
        val totalBottomH = bottomIndicators.sumOf { if (it.isVisible) (indicatorHeights[it.id] ?: defaultHeightPx).toDouble() else minimizedHeightPx.toDouble() }.toFloat()
        val mainH = plotsHeightPx - totalBottomH

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
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
                .pointerInput(bottomIndicators) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            var found = false
                            var currentY = mainH
                            val sepRadius = with(density) { 15.dp.toPx() }
                            for (i in 0 until bottomIndicators.size) {
                                if (abs(offset.y - currentY) < sepRadius) {
                                    dragArea = 4; draggingSeparatorIdx = i; found = true; break
                                }
                                val ind = bottomIndicators[i]
                                currentY += if (ind.isVisible) (indicatorHeights[ind.id] ?: defaultHeightPx) else minimizedHeightPx
                            }

                            if (!found) {
                                currentY = mainH
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
                                    isLongPressing -> 0
                                    offset.x > chartWidthPx -> 1
                                    offset.y > plotsHeightPx -> 2
                                    else -> 3
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (change.isConsumed) return@detectDragGestures
                            when (dragArea) {
                                0 -> if (isLongPressing) {
                                    val newX = (crosshairPosition?.x ?: 0f) + dragAmount.x
                                    // Si le viseur dépasse le bord droit du graphique, scroller pour créer de la marge droite
                                    if (newX > chartWidthPx) {
                                        val cw = if (currentDisplayCount > 0) chartWidthPx / currentDisplayCount else 0f
                                        if (cw > 0) currentOnPan(-(dragAmount.x / cw))
                                    }
                                    crosshairPosition = crosshairPosition?.let {
                                        Offset(
                                            (it.x + dragAmount.x).coerceIn(0f, chartWidthPx - 1f),
                                            (it.y + dragAmount.y).coerceIn(0f, h)
                                        )
                                    }
                                }
                                1 -> {
                                    isAutoHeight = false
                                    val zoom = (1f - (dragAmount.y / plotsHeightPx) * 2f).coerceIn(0.1f, 10f)
                                    val range = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.1f)
                                    val center = (manualMaxPrice + manualMinPrice) / 2f
                                    manualMinPrice = center - (range / zoom) / 2f
                                    manualMaxPrice = center + (range / zoom) / 2f
                                }
                                2 -> currentOnZoom((1f - (dragAmount.x / chartWidthPx) * 8f).coerceIn(0.15f, 100f))
                                3 -> {
                                    val candleW = if (currentDisplayCount > 0) chartWidthPx / currentDisplayCount else 0f
                                    if (candleW > 0) currentOnPan((dragAmount.x / candleW) * 3f)
                                    isAutoHeight = false
                                    val range = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.1f)
                                    val priceDelta = (dragAmount.y / plotsHeightPx) * range
                                    manualMinPrice += priceDelta; manualMaxPrice += priceDelta
                                }
                                4 -> {
                                    val dy = dragAmount.y
                                    if (draggingSeparatorIdx in 0 until bottomIndicators.size) {
                                        val ind = bottomIndicators[draggingSeparatorIdx]
                                        val curH = indicatorHeights[ind.id] ?: defaultHeightPx
                                        indicatorHeights[ind.id] = (curH - dy).coerceAtLeast(minIndicatorHeightPx)
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
                                    val candleW = if (currentDisplayCount > 0) chartWidthPx / currentDisplayCount else 0f
                                    if (candleW > 0) currentOnPan((dragAmount.x / candleW) * 3f)
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
                        },
                        onDragEnd = { draggingSeparatorIdx = -1; draggingIndicatorId = null }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { expandedIndicatorId = null; if (isLongPressing) { isLongPressing = false; crosshairPosition = null } },
                        onDoubleTap = { offset ->
                            if (offset.x > chartWidthPx) {
                                if (offset.y <= mainH) isAutoHeight = true
                                else {
                                    var curYPos = mainH
                                    for (ind in bottomIndicators) {
                                        val indH = if (ind.isVisible) (indicatorHeights[ind.id] ?: defaultHeightPx) else minimizedHeightPx
                                        if (offset.y in curYPos..curYPos + indH) { if (ind.isVisible) indicatorAutoHeight[ind.id] = true; break }
                                        curYPos += indH
                                    }
                                }
                            }
                        },
                        onLongPress = { offset ->
                            if (offset.x <= chartWidthPx) { isLongPressing = true; crosshairPosition = offset }
                        }
                    )
                }
        ) {
            val range = (manualMaxPrice - manualMinPrice).coerceAtLeast(0.1f) * 1.1f
            val minP = (manualMaxPrice + manualMinPrice) / 2f - (range / 2f)
            val normY: (Float) -> Float = { mainH - ((it - minP) / range * mainH) }
            val denormY: (Float) -> Float = { minP + ((mainH - it) / mainH * range) }

            ChartDrawer.drawGridAndLabels(this, textMeasurer, chartWidthPx, priceWidthPx, mainH, range, minP, normY, settings, textStyle)

            val candleW = if (displayCount > 0) chartWidthPx / displayCount else 0f
            val bodyW = candleW * 0.7f

            clipRect(right = chartWidthPx, bottom = mainH) {
                ChartDrawer.drawCandles(this, visibleCandles, allCandles, startIdx, allCandles.size, scrollOffset, candleW, bodyW, chartWidthPx, mainH, volumeIndicator, volumeMaValues, smoothedMaValues, settings, normY)
                ChartDrawer.drawOverlayIndicators(this, overlayIndicators, allCandles, smaValues, emaValues, hmaValues, vwapValues, bbValues, atrBandsValues, stValues, alligatorValues, ichimokuValues, ribbonValues, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, normY)
            }

            ChartDrawer.drawBottomIndicators(this, bottomIndicators, indicatorHeights, defaultHeightPx, rsiValues, rsiMaValues, macdValues, stochValues, atrValues, indicatorRanges, visibleCandles, allCandles.size, startIdx, endIdx, scrollOffset, candleW, bodyW, chartWidthPx, priceWidthPx, mainH, settings, textMeasurer, textStyle, crosshairTextStyle, draggingSeparatorIdx, dragArea, minimizedHeightPx)

            if (allCandles.isNotEmpty()) {
                ChartDrawer.drawLastPriceLabel(this, allCandles.last(), chartWidthPx, mainH, priceWidthPx, settings, crosshairTextStyle, textMeasurer, normY)
            }

            ChartDrawer.drawTimeLabels(this, allCandles, startIdx, allCandles.size, scrollOffset, candleW, chartWidthPx, plotsHeightPx, displayCount, timeFormatter, settings, textStyle, textMeasurer, labelBgColor, timeframe)
            
            crosshairPosition?.let { p ->
                ChartDrawer.drawCrosshair(this, p, chartWidthPx, h, mainH, priceWidthPx, timeHeightPx, settings, crosshairTextStyle, textMeasurer, allCandles, scrollOffset, candleW, crosshairTimeFormatter, denormalizeY = denormY, bottomIndicators, indicatorHeights, indicatorRanges, defaultHeightPx, colorScheme.background, timeframe, displayCount)
            }

            ChartDrawer.drawBorders(this, chartWidthPx, priceWidthPx, h, settings)
        }

        // --- HUD Overlay (OHLC) ---
        val candleW_hud = if (currentDisplayCount > 0) chartWidthPx / currentDisplayCount else 0f
        val hoverIdx = crosshairPosition?.let { p ->
            if (candleW_hud > 0) {
                val raw = (allCandles.size - 1 - scrollOffset - (chartWidthPx - p.x - candleW_hud / 2f) / candleW_hud).roundToInt()
                // Clamp to valid range but allow up to last candle (no virtual beyond for OHLC display)
                raw.coerceIn(allCandles.indices)
            } else null
        }
        val displayedCandle = hoverIdx?.let { allCandles[it] } ?: allCandles.lastOrNull()

        displayedCandle?.let { candle ->
            val hudTextColor = if (settings.backgroundColor.luminance() < 0.5f) Color(0xFFd1d4dc) else Color(0xFF131722)
            val isUp = candle.close >= candle.open
            val trendColor = if (isUp) settings.upColor else settings.downColor
            
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

        val surfCol = colorScheme.surface
        val txtCol = colorScheme.onSurface
        val allOverlay = (if (volumeIndicator != null) listOf(volumeIndicator) else emptyList()) + overlayIndicators

        if (allOverlay.isNotEmpty()) {
            val overlayTopPadding = if (crosshairPosition != null) 18.dp else 2.dp
            Column(modifier = Modifier.padding(start = 8.dp, top = overlayTopPadding)) {
                if (showIndicatorLabels && !overlayCollapsed) {
                    allOverlay.forEach { ind ->
                        val values = if (crosshairPosition != null && ind.isVisible) {
                            when (ind) {
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
                                is Indicator.BollingerBands -> bbValues[ind.id]?.let { bb ->
                                    val i = hoverIdx ?: bb.middle.indices.lastOrNull { bb.middle[it] != null }
                                    i?.let { idx ->
                                        listOf(
                                            String.format(Locale.getDefault(), "%.2f", bb.upper.getOrNull(idx) ?: 0f) to ind.upperColor,
                                            String.format(Locale.getDefault(), "%.2f", bb.middle.getOrNull(idx) ?: 0f) to ind.middleColor,
                                            String.format(Locale.getDefault(), "%.2f", bb.lower.getOrNull(idx) ?: 0f) to ind.lowerColor
                                        )
                                    }
                                } ?: emptyList()
                                else -> emptyList()
                            }
                        } else emptyList()
                        IndicatorLabelToolbar(ind, expandedIndicatorId == ind.id, values, txtCol, surfCol, { expandedIndicatorId = if (expandedIndicatorId == ind.id) null else ind.id }, { onIndicatorToggleVisibility(ind) }, { onIndicatorSettingsRequest(ind) }, { onIndicatorRemove(ind) })
                    }
                }
                IconButton(onClick = { overlayCollapsed = !overlayCollapsed }, modifier = Modifier.size(36.dp)) { 
                    Icon(imageVector = if (overlayCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp, contentDescription = null, tint = txtCol.copy(0.6f), modifier = Modifier.size(24.dp))
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
                                    val rsiList = rsiValues[ind.id] ?: emptyList()
                                    val maList = rsiMaValues[ind.id] ?: emptyList()
                                    val rsiVal = hoverIdx?.let { rsiList.getOrNull(it) } ?: rsiList.lastOrNull { it != null }
                                    val maVal = hoverIdx?.let { maList.getOrNull(it) } ?: maList.lastOrNull { it != null }
                                    val list = mutableListOf<Pair<String, Color>>()
                                    if (rsiVal != null) list.add(String.format(Locale.getDefault(), "%.${ind.precision}f", rsiVal) to ind.color)
                                    if (ind.showMa && maVal != null) list.add(String.format(Locale.getDefault(), "%.${ind.precision}f", maVal) to ind.maColor)
                                    list
                                }
                                is Indicator.MACD -> macdValues[ind.id]?.let { res ->
                                    val i = hoverIdx ?: res.macdLine.indices.lastOrNull { res.macdLine[it] != null }
                                    i?.let { idx ->
                                        val mV = res.macdLine.getOrNull(idx); val sV = res.signalLine.getOrNull(idx); val hV = res.histogram.getOrNull(idx)
                                        val list = mutableListOf<Pair<String, Color>>()
                                        if (mV != null) list.add(String.format(Locale.getDefault(), "%.2f", mV) to ind.macdColor)
                                        if (sV != null) list.add(String.format(Locale.getDefault(), "%.2f", sV) to ind.signalColor)
                                        if (hV != null) list.add(String.format(Locale.getDefault(), "%.2f", hV) to (if (hV >= 0) ind.histColorUp else ind.histColorDown))
                                        list
                                    }
                                } ?: emptyList()
                                is Indicator.Stochastic -> stochValues[ind.id]?.let { res ->
                                    val i = hoverIdx ?: res.k.indices.lastOrNull { res.k[it] != null }
                                    i?.let { idx ->
                                        listOf(
                                            String.format(Locale.getDefault(), "%.2f", res.k.getOrNull(idx) ?: 0f) to ind.kColor,
                                            String.format(Locale.getDefault(), "%.2f", res.d.getOrNull(idx) ?: 0f) to ind.dColor
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
                        IndicatorLabelToolbar(ind, expandedIndicatorId == ind.id, values, txtCol, surfCol, { expandedIndicatorId = if (expandedIndicatorId == ind.id) null else ind.id }, { onIndicatorToggleVisibility(ind) }, { onIndicatorSettingsRequest(ind) }, { onIndicatorRemove(ind) })
                    }
                }
                currentY += hInd
            }
        }
            }
        }
    }
}
