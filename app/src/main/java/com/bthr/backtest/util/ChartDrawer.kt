package com.bthr.backtest.util

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bthr.backtest.model.BackgroundType
import com.bthr.backtest.model.Candle
import com.bthr.backtest.model.Indicator
import com.bthr.backtest.model.ChartSettings
import com.bthr.backtest.model.GridLines
import com.bthr.backtest.model.LineStyle
import com.bthr.backtest.util.DrawingCoordinateMapper
import com.bthr.backtest.util.DrawingState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*

object ChartDrawer {

    // Fonction pour détecter les jours fériés
    private fun isHoliday(cal: Calendar): Boolean {
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        
        // Jours fériés fixes (France/Europe)
        when (month) {
            Calendar.JANUARY -> {
                if (day == 1) return true // Jour de l'An
            }
            Calendar.MAY -> {
                if (day == 1) return true // Fête du travail
                if (day == 8) return true // Victoire 1945
            }
            Calendar.JULY -> {
                if (day == 14) return true // Fête nationale
            }
            Calendar.AUGUST -> {
                if (day == 15) return true // Assomption
            }
            Calendar.NOVEMBER -> {
                if (day == 1) return true // Toussaint
                if (day == 11) return true // Armistice 1918
            }
            Calendar.DECEMBER -> {
                if (day == 25) return true // Noël
            }
        }
        
        // Dimanche et samedi sont considérés comme non-tradés pour les timeframes intraday
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return true
        }
        
        return false
    }

    // Fonction pour déterminer si une date est importante (mérite d'être mise en évidence)
    private fun isImportantDate(cal: Calendar): Boolean {
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        
        // 1er janvier, 1er du mois, jours fériés principaux
        if (month == Calendar.JANUARY && day == 1) return true
        if (day == 1) return true
        if (month == Calendar.DECEMBER && day == 25) return true
        if (month == Calendar.MAY && day == 1) return true
        if (month == Calendar.JULY && day == 14) return true
        
        return false
    }

    private fun DrawScope.safeDrawText(
        textMeasurer: TextMeasurer,
        text: String,
        offset: Offset,
        style: TextStyle
    ) {
        if (text.isBlank()) return
        try {
            val result = textMeasurer.measure(text, style)
            drawText(result, topLeft = offset)
        } catch (e: Exception) {}
    }

    private fun getContrastColor(backgroundColor: Color): Color {
        val opaqueColor = backgroundColor.copy(alpha = 1f)
        return if (opaqueColor.luminance() > 0.5f) Color.Black else Color.White
    }

    private fun getPathEffect(style: Int, thicknessPx: Float): PathEffect? {
        return when (style) {
            1 -> PathEffect.dashPathEffect(floatArrayOf(thicknessPx * 4, thicknessPx * 2))
            2 -> PathEffect.dashPathEffect(floatArrayOf(0.1f, thicknessPx * 3f))
            else -> null
        }
    }

    private fun drawIndicatorLabel(
        drawScope: DrawScope,
        chartW: Float,
        priceW: Float,
        y: Float,
        value: Float,
        color: Color,
        textMeasurer: TextMeasurer,
        textStyle: TextStyle,
        precision: Int = 2,
        minY: Float = 0f,
        maxY: Float = Float.MAX_VALUE
    ) = with(drawScope) {
        if (y !in minY..maxY) return@with
        val pTxt = String.format(Locale.getDefault(), "%.${precision}f", value)
        
        val opaqueBg = color.copy(alpha = 1f)
        val labelTextColor = getContrastColor(opaqueBg)
        val style = textStyle.copy(color = labelTextColor, fontWeight = FontWeight.Bold)
        val layout = textMeasurer.measure(pTxt, style)
        
        val rectH = layout.size.height.toFloat() + 4.dp.toPx()
        val lY = (y - rectH / 2f).coerceIn(minY, maxY - rectH)
        
        drawRect(opaqueBg, Offset(chartW, lY), Size(priceW + 20.dp.toPx(), rectH))
        drawText(layout, topLeft = Offset(chartW + 6.dp.toPx(), lY + (rectH - layout.size.height) / 2f))
    }

    fun drawBackground(drawScope: DrawScope, settings: ChartSettings, fullW: Float, fullH: Float) = with(drawScope) {
        if (settings.backgroundType == BackgroundType.SOLID) {
            drawRect(settings.backgroundColor, size = Size(fullW, fullH))
        } else {
            drawRect(brush = Brush.verticalGradient(colors = listOf(settings.backgroundColor, settings.backgroundGradientColor)), size = Size(fullW, fullH))
        }
    }

    fun drawWatermark(drawScope: DrawScope, textMeasurer: TextMeasurer, chartW: Float, chartH: Float, symbol: String, timeframe: String, settings: ChartSettings) = with(drawScope) {
        if (!settings.watermarkVisible || chartW <= 0) return@with
        val targetWidth = chartW * 0.7f
        var symbolFontSize = chartW * 0.15f
        var symbolStyle = TextStyle(color = settings.watermarkColor, fontSize = symbolFontSize.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        var symbolLayout = textMeasurer.measure(symbol, symbolStyle)
        if (symbolLayout.size.width > targetWidth) {
            symbolFontSize *= (targetWidth / symbolLayout.size.width)
            symbolStyle = symbolStyle.copy(fontSize = symbolFontSize.sp)
            symbolLayout = textMeasurer.measure(symbol, symbolStyle)
        }
        
        val tfFormatted = timeframe.replace("m", " MIN").replace("h", " H").replace("d", " DAY").uppercase()
        
        var tfFontSize = symbolFontSize * 0.5f
        var tfStyle = TextStyle(color = settings.watermarkColor, fontSize = tfFontSize.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        var tfLayout = textMeasurer.measure(tfFormatted, tfStyle)
        if (tfLayout.size.width > targetWidth) {
            tfFontSize *= (targetWidth / tfLayout.size.width)
            tfStyle = tfStyle.copy(fontSize = tfFontSize.sp)
            tfLayout = textMeasurer.measure(tfFormatted, tfStyle)
        }
        val spacing = 8.dp.toPx()
        val totalH = symbolLayout.size.height + tfLayout.size.height + spacing
        val startY = (chartH - totalH) / 2f
        drawText(symbolLayout, topLeft = Offset((chartW - symbolLayout.size.width) / 2f, startY))
        drawText(tfLayout, topLeft = Offset((chartW - tfLayout.size.width) / 2f, startY + symbolLayout.size.height + spacing))
    }

    fun drawGridAndLabels(drawScope: DrawScope, textMeasurer: TextMeasurer, chartW: Float, priceW: Float, mainH: Float, range: Float, minP: Float, normalizeY: (Float) -> Float, settings: ChartSettings, textStyle: TextStyle) = with(drawScope) {
        if (range <= 0 || chartW <= 0) return@with
        val step = ChartUtils.calculatePriceStep(range)
        var pLabel = ceil(minP / step) * step
        var safety = 0
        val labelStyle = textStyle.copy(color = settings.scaleTextColor, fontSize = settings.scaleTextSize.sp, fontWeight = FontWeight.Normal)
        val showHorizontal = settings.showGrid && (settings.gridLines == GridLines.BOTH || settings.gridLines == GridLines.HORIZONTAL)
        val horizontalThicknessPx = settings.horizontalGridThickness.toFloat().dp.toPx()
        val horizontalEffect = getPathEffect(settings.horizontalGridStyle, horizontalThicknessPx)
        val cap = if (settings.horizontalGridStyle == 2) StrokeCap.Round else StrokeCap.Butt
        
        val isDark = settings.backgroundColor.luminance() < 0.5f
        val adaptiveGridColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
        val horizontalGridColor = settings.horizontalGridColor.copy(alpha = 0.2f).compositeOver(adaptiveGridColor)

        while (pLabel <= minP + range && safety < 100) {
            val y = normalizeY(pLabel)
            if (y in 0f..mainH) {
                if (showHorizontal) drawLine(color = horizontalGridColor, start = Offset(0f, y), end = Offset(chartW, y), strokeWidth = horizontalThicknessPx, pathEffect = horizontalEffect, cap = cap)
                drawLine(settings.scaleLinesColor.copy(alpha = 0.4f), Offset(chartW, y), Offset(chartW + 4.dp.toPx(), y), 1f)
                safeDrawText(textMeasurer, ChartUtils.formatPrice(pLabel), Offset(chartW + 8.dp.toPx(), y - (settings.scaleTextSize * 0.7).dp.toPx()), labelStyle)
            }
            pLabel += step; safety++
        }
    }

    fun drawSessions(
        drawScope: DrawScope,
        allCandles: List<Candle>,
        startIdx: Int,
        totalCandles: Int,
        scrollOffset: Float,
        candleW: Float,
        chartW: Float,
        mainH: Float,
        indicator: Indicator.Sessions,
        normalizeY: (Float) -> Float,
        textMeasurer: TextMeasurer
    ) = with(drawScope) {
        if (!indicator.isVisible || allCandles.isEmpty() || chartW <= 0) return@with

        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        
        fun parseTimeToMinutes(timeStr: String): Int {
            return try {
                val parts = timeStr.split(":")
                val h = parts[0].toInt()
                val m = if (parts.size > 1) parts[1].toInt() else 0
                h * 60 + m
            } catch (e: Exception) { 0 }
        }

        data class SessionInfo(val name: String, val startMin: Int, val endMin: Int, val color: Color)
        val sessions = listOfNotNull(
            if (indicator.showSydney) SessionInfo("Sydney", parseTimeToMinutes(indicator.sydneyStart), parseTimeToMinutes(indicator.sydneyEnd), indicator.sydneyColor) else null,
            if (indicator.showTokyo) SessionInfo("Tokyo", parseTimeToMinutes(indicator.tokyoStart), parseTimeToMinutes(indicator.tokyoEnd), indicator.tokyoColor) else null,
            if (indicator.showLondon) SessionInfo("London", parseTimeToMinutes(indicator.londonStart), parseTimeToMinutes(indicator.londonEnd), indicator.londonColor) else null,
            if (indicator.showNewYork) SessionInfo("New York", parseTimeToMinutes(indicator.newYorkStart), parseTimeToMinutes(indicator.newYorkEnd), indicator.newYorkColor) else null
        )

        val visibleEndIdx = (startIdx + (chartW / candleW).toInt() + 1).coerceAtMost(allCandles.size)
        
        sessions.forEach { session ->
            val processedIndices = mutableSetOf<Int>()
            
            for (i in startIdx until visibleEndIdx) {
                if (i in processedIndices) continue
                
                val candle = allCandles[i]
                cal.timeInMillis = candle.timestamp
                val currentMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                
                val isInSession = if (session.startMin < session.endMin) {
                    currentMin in session.startMin until session.endMin
                } else {
                    currentMin >= session.startMin || currentMin < session.endMin
                }
                
                if (isInSession) {
                    var blockStart = i
                    while (blockStart > 0) {
                        val prevCandle = allCandles[blockStart - 1]
                        cal.timeInMillis = prevCandle.timestamp
                        val m = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                        val ok = if (session.startMin < session.endMin) m in session.startMin until session.endMin else m >= session.startMin || m < session.endMin
                        if (!ok) break
                        blockStart--
                    }
                    
                    var blockEnd = i
                    while (blockEnd < allCandles.size - 1) {
                        val nextCandle = allCandles[blockEnd + 1]
                        cal.timeInMillis = nextCandle.timestamp
                        val m = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                        val ok = if (session.startMin < session.endMin) m in session.startMin until session.endMin else m >= session.startMin || m < session.endMin
                        if (!ok) break
                        blockEnd++
                    }
                    
                    var sessionMin = Float.MAX_VALUE
                    var sessionMax = Float.MIN_VALUE
                    for (k in blockStart..blockEnd) {
                        sessionMin = min(sessionMin, allCandles[k].low)
                        sessionMax = max(sessionMax, allCandles[k].high)
                        processedIndices.add(k)
                    }
                    
                    drawSessionBox(this, blockStart, blockEnd, sessionMin, sessionMax, session.name, session.color, indicator, totalCandles, scrollOffset, candleW, chartW, mainH, normalizeY, textMeasurer)
                }
            }
        }
    }

    private fun drawSessionBox(
        drawScope: DrawScope,
        startIdx: Int,
        endIdx: Int,
        minP: Float,
        maxP: Float,
        name: String,
        color: Color,
        indicator: Indicator.Sessions,
        totalCandles: Int,
        scrollOffset: Float,
        candleW: Float,
        chartW: Float,
        mainH: Float,
        normalizeY: (Float) -> Float,
        textMeasurer: TextMeasurer
    ) = with(drawScope) {
        val xStart = chartW - (candleW / 2f) - ((totalCandles - 1 - startIdx - scrollOffset) * candleW) - candleW/2f
        val xEnd = chartW - (candleW / 2f) - ((totalCandles - 1 - endIdx - scrollOffset) * candleW) + candleW/2f
        
        if (xEnd < 0 || xStart > chartW) return@with

        val yTop = normalizeY(maxP)
        val yBottom = normalizeY(minP)

        if (indicator.showBackground) {
            drawRect(
                color = color.copy(alpha = indicator.opacity),
                topLeft = Offset(xStart, yTop),
                size = Size(xEnd - xStart, yBottom - yTop)
            )
        }

        drawRect(
            color = color.copy(alpha = 0.4f),
            topLeft = Offset(xStart, yTop),
            size = Size(xEnd - xStart, yBottom - yTop),
            style = Stroke(width = 0.8.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
        )

        if (indicator.showLabels) {
            val style = TextStyle(color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            val layout = textMeasurer.measure(name, style)
            val labelX = xStart + (xEnd - xStart - layout.size.width) / 2f
            if (labelX + layout.size.width > 0 && labelX < chartW) {
                drawText(layout, topLeft = Offset(labelX, (yTop - layout.size.height - 2.dp.toPx()).coerceAtLeast(2.dp.toPx())))
            }
        }
    }

    fun drawCandles(
        drawScope: DrawScope,
        visibleCandles: List<Candle>,
        allCandles: List<Candle>,
        startIdx: Int,
        totalCandles: Int,
        scrollOffset: Float,
        candleW: Float,
        bodyW: Float,
        chartW: Float,
        mainH: Float,
        volumeIndicator: Indicator.Volume?,
        volumeMaValues: List<Float?>?,
        smoothedMaValues: List<Float?>?,
        settings: ChartSettings,
        normalizeY: (Float) -> Float
    ) = with(drawScope) {
        if (visibleCandles.isEmpty() || chartW <= 0) return@with
        val maxVol = visibleCandles.maxOf { it.volume }.coerceAtLeast(1f)
        
        if (volumeIndicator?.isVisible == true) {
            val endIdx = startIdx + visibleCandles.size
            val normVolY: (Float) -> Float = { mainH - (it / maxVol) * (mainH * 0.15f) }
            if (volumeIndicator.showVolumeMa && volumeMaValues != null) ChartUtils.drawIndicatorPath(this, volumeMaValues, startIdx, endIdx, scrollOffset, candleW, chartW, volumeIndicator.volumeMaColor, thickness = volumeIndicator.volumeMaThickness, style = volumeIndicator.volumeMaStyle, normY = normVolY)
            if (volumeIndicator.showSmoothedMa && smoothedMaValues != null) ChartUtils.drawIndicatorPath(this, smoothedMaValues, startIdx, endIdx, scrollOffset, candleW, chartW, volumeIndicator.smoothedMaColor, thickness = volumeIndicator.smoothedMaThickness, style = volumeIndicator.smoothedMaStyle, normY = normVolY)
        }

        val showVolume = volumeIndicator?.isVisible == true && volumeIndicator.showVolume
        val isVerySmall = candleW < 1.5.dp.toPx() 
        
        val pathUp = Path()
        val pathDown = Path()
        var lastX = -1000f
        val minXGap = 1.0f 

        visibleCandles.forEachIndexed { index, candle ->
            val absIdx = startIdx + index
            val x = chartW - (candleW / 2f) - ((totalCandles - 1 - absIdx - scrollOffset) * candleW)
            if (x < -candleW) return@forEachIndexed
            if (x > chartW + candleW) return@forEachIndexed

            if (showVolume) {
                val volH = (candle.volume / maxVol) * (mainH * 0.15f)
                val isGrowing = if (volumeIndicator!!.colorBasedOnPreviousClose && absIdx > 0) candle.close >= allCandles[absIdx - 1].close else candle.close >= candle.open
                drawRect(if (isGrowing) volumeIndicator.upColor.copy(alpha = volumeIndicator.upOpacity) else volumeIndicator.downColor.copy(alpha = volumeIndicator.downOpacity), Offset(x - bodyW / 2, mainH - volH), Size(bodyW, volH))
            }

            val isUp = candle.close >= candle.open
            val color = if (isUp) settings.upColor else settings.downColor

            if (isVerySmall) {
                if (abs(x - lastX) < minXGap && index != 0 && index != visibleCandles.size - 1) return@forEachIndexed
                lastX = x
                
                val highY = normalizeY(candle.high)
                val lowY = normalizeY(candle.low)
                val targetPath = if (isUp) pathUp else pathDown
                targetPath.moveTo(x, highY)
                targetPath.lineTo(x, lowY)
            } else {
                val highY = normalizeY(candle.high); val lowY = normalizeY(candle.low)
                val bTop = min(normalizeY(candle.open), normalizeY(candle.close))
                val bBottom = max(normalizeY(candle.open), normalizeY(candle.close))
                
                if (settings.wickEnabled) {
                    val wickColor = if (isUp) settings.upWickColor else settings.downWickColor
                    drawLine(wickColor, Offset(x, highY), Offset(x, bTop), 1f)
                    drawLine(wickColor, Offset(x, bBottom), Offset(x, lowY), 1f)
                }
                if (settings.bodyEnabled) drawRect(color, Offset(x - bodyW / 2, bTop), Size(bodyW, max(bBottom - bTop, 0.5f)))
                if (settings.bordersEnabled) {
                    val borderColor = if (isUp) settings.upBorderColor else settings.downBorderColor
                    drawRect(borderColor, Offset(x - bodyW / 2, bTop), Size(bodyW, max(bBottom - bTop, 0.5f)), style = Stroke(1f))
                }
            }
        }
        
        if (isVerySmall) {
            val strokeW = max(bodyW, 1f)
            drawPath(pathUp, settings.upColor, style = Stroke(width = strokeW))
            drawPath(pathDown, settings.downColor, style = Stroke(width = strokeW))
        }
    }

    fun drawSelectionHandles(
        drawScope: DrawScope,
        totalCount: Int,
        startIdx: Int,
        endIdx: Int,
        scrollOffset: Float,
        candleW: Float,
        chartW: Float,
        normalizeY: (Float) -> Float,
        getValue: (Int) -> Float?
    ) = with(drawScope) {
        if (totalCount < 2 || chartW <= 0 || candleW <= 0) return@with
        val handleColor = Color(0xFF2962FF); val radius = 3.2.dp.toPx(); val step = 10
        var i = totalCount - 2
        while (i >= 0) {
            if (i in startIdx until endIdx) {
                val v = getValue(i); if (v != null) {
                    val x = chartW - (candleW / 2f) - ((totalCount - 1 - i - scrollOffset) * candleW)
                    if (x in 0f..chartW) {
                        val y = normalizeY(v)
                        drawCircle(color = Color.White, radius = radius, center = Offset(x, y))
                        drawCircle(color = handleColor, radius = radius, center = Offset(x, y), style = Stroke(width = 1.1.dp.toPx()))
                    }
                }
            }
            if (i < startIdx) break
            i -= step
        }
    }

    fun drawOverlayIndicators(
        drawScope: DrawScope, 
        overlayIndicators: List<Indicator>, 
        allCandles: List<Candle>,
        smaValues: Map<String, List<Float?>>, 
        emaValues: Map<String, List<Float?>>,
        hmaValues: Map<String, List<Float?>>,
        vwapValues: Map<String, IndicatorCalculators.VWAPResult>,
        bbValues: Map<String, IndicatorCalculators.BollingerBandsResult>, 
        atrBandsValues: Map<String, IndicatorCalculators.ATRBandsResult>,
        stValues: Map<String, IndicatorCalculators.SupertrendResult>,
        alligatorValues: Map<String, IndicatorCalculators.AlligatorResult>,
        ichimokuValues: Map<String, IndicatorCalculators.IchimokuResult>,
        ribbonValues: Map<String, IndicatorCalculators.RibbonResult>,
        startIdx: Int, 
        totalCandles: Int, 
        scrollOffset: Float, 
        candleW: Float, 
        chartW: Float, 
        normalizeY: (Float) -> Float
    ) {
        overlayIndicators.forEach { ind ->
            if (!ind.isVisible) return@forEach
            when (ind) {
                is Indicator.SMA -> smaValues[ind.id]?.let { ChartUtils.drawIndicatorPath(drawScope, it, startIdx, startIdx + it.size, scrollOffset, candleW, chartW, ind.color, ind.thickness, ind.style, normalizeY) }
                is Indicator.EMA -> emaValues[ind.id]?.let { ChartUtils.drawIndicatorPath(drawScope, it, startIdx, startIdx + it.size, scrollOffset, candleW, chartW, ind.color, ind.thickness, ind.style, normalizeY) }
                is Indicator.HMA -> hmaValues[ind.id]?.let { ChartUtils.drawIndicatorPath(drawScope, it, startIdx, startIdx + it.size, scrollOffset, candleW, chartW, ind.color, ind.thickness, ind.style, normalizeY) }
                is Indicator.VWAP -> vwapValues[ind.id]?.let { res ->
                    ChartUtils.drawIndicatorPath(drawScope, res.vwap, startIdx, res.vwap.size, scrollOffset, candleW, chartW, ind.color, ind.thickness, ind.style, normalizeY, totalCandles)
                    if (ind.showBands) {
                        ChartUtils.drawIndicatorPath(drawScope, res.upper1, startIdx, res.upper1.size, scrollOffset, candleW, chartW, ind.upperColor, 1f, 1, normalizeY, totalCandles)
                        ChartUtils.drawIndicatorPath(drawScope, res.lower1, startIdx, res.lower1.size, scrollOffset, candleW, chartW, ind.lowerColor, 1f, 1, normalizeY, totalCandles)
                    }
                }
                is Indicator.BollingerBands -> bbValues[ind.id]?.let { bb ->
                    ChartUtils.drawIndicatorPath(drawScope, bb.upper, startIdx, startIdx + bb.upper.size, scrollOffset, candleW, chartW, ind.upperColor, ind.upperThickness, ind.upperStyle, normalizeY)
                    ChartUtils.drawIndicatorPath(drawScope, bb.middle, startIdx, startIdx + bb.middle.size, scrollOffset, candleW, chartW, ind.middleColor, ind.middleThickness, ind.middleStyle, normalizeY)
                    ChartUtils.drawIndicatorPath(drawScope, bb.lower, startIdx, startIdx + bb.lower.size, scrollOffset, candleW, chartW, ind.lowerColor, ind.lowerThickness, ind.lowerStyle, normalizeY)
                }
                is Indicator.ATRBands -> atrBandsValues[ind.id]?.let { ab ->
                    ChartUtils.drawIndicatorPath(drawScope, ab.upper, startIdx, startIdx + ab.upper.size, scrollOffset, candleW, chartW, ind.upperColor, ind.upperThickness, ind.upperStyle, normalizeY)
                    ChartUtils.drawIndicatorPath(drawScope, ab.lower, startIdx, startIdx + ab.lower.size, scrollOffset, candleW, chartW, ind.lowerColor, ind.lowerThickness, ind.lowerStyle, normalizeY)
                    if (ind.showTPBands) {
                        ChartUtils.drawIndicatorPath(drawScope, ab.tpUpper, startIdx, startIdx + ab.tpUpper.size, scrollOffset, candleW, chartW, ind.tpUpperColor, ind.tpThickness, 0, normalizeY)
                        ChartUtils.drawIndicatorPath(drawScope, ab.tpLower, startIdx, startIdx + ab.tpLower.size, scrollOffset, candleW, chartW, ind.tpLowerColor, ind.tpThickness, 0, normalizeY)
                    }
                }
                is Indicator.Alligator -> alligatorValues[ind.id]?.let { res ->
                    ChartUtils.drawIndicatorPath(drawScope, res.jaw, startIdx, res.jaw.size, scrollOffset, candleW, chartW, ind.jawColor, ind.jawThickness, 0, normalizeY, totalCandles)
                    ChartUtils.drawIndicatorPath(drawScope, res.teeth, startIdx, res.teeth.size, scrollOffset, candleW, chartW, ind.teethColor, ind.teethThickness, 0, normalizeY, totalCandles)
                    ChartUtils.drawIndicatorPath(drawScope, res.lips, startIdx, res.lips.size, scrollOffset, candleW, chartW, ind.lipsColor, ind.lipsThickness, 0, normalizeY, totalCandles)
                }
                is Indicator.Ichimoku -> ichimokuValues[ind.id]?.let { res ->
                    val pathUp = Path(); val pathDown = Path(); val cloudLimit = res.senkouSpanA.size - 1
                    var lastX = -1000f; val minXGap = 1f
                    for (i in startIdx until cloudLimit) {
                        val sa1 = res.senkouSpanA[i]; val sa2 = res.senkouSpanA.getOrNull(i+1)
                        val sb1 = res.senkouSpanB[i]; val sb2 = res.senkouSpanB.getOrNull(i+1)
                        if (sa1 == null || sa2 == null || sb1 == null || sb2 == null) continue
                        val x1 = chartW - (candleW / 2f) - ((totalCandles - 1 - i - scrollOffset) * candleW); val x2 = x1 + candleW
                        if (x2 < 0) continue; if (x1 > chartW) break
                        if (abs(x1 - lastX) < minXGap && i != startIdx && i != cloudLimit - 1) continue
                        lastX = x1
                        val targetPath = if (sa1 > sb1) pathUp else pathDown
                        targetPath.moveTo(x1, normalizeY(sa1)); targetPath.lineTo(x2, normalizeY(sa2)); targetPath.lineTo(x2, normalizeY(sb2)); targetPath.lineTo(x1, normalizeY(sb1)); targetPath.close()
                    }
                    drawScope.drawPath(pathUp, ind.kumoUpColor); drawScope.drawPath(pathDown, ind.kumoDownColor)
                    ChartUtils.drawIndicatorPath(drawScope, res.tenkanSen, startIdx, totalCandles, scrollOffset, candleW, chartW, ind.tenkanColor, ind.tenkanThickness, 0, normalizeY, totalCandles)
                    ChartUtils.drawIndicatorPath(drawScope, res.kijunSen, startIdx, totalCandles, scrollOffset, candleW, chartW, ind.kijunColor, ind.kijunThickness, 0, normalizeY, totalCandles)
                    ChartUtils.drawIndicatorPath(drawScope, res.senkouSpanA, startIdx, res.senkouSpanA.size, scrollOffset, candleW, chartW, ind.senkouAColor, ind.senkouAThickness, 0, normalizeY, totalCandles)
                    ChartUtils.drawIndicatorPath(drawScope, res.senkouSpanB, startIdx, res.senkouSpanB.size, scrollOffset, candleW, chartW, ind.senkouBColor, ind.senkouBThickness, 0, normalizeY, totalCandles)
                    ChartUtils.drawIndicatorPath(drawScope, res.chikouSpan, startIdx, totalCandles, scrollOffset, candleW, chartW, ind.chikouColor, ind.chikouThickness, 0, normalizeY, totalCandles)
                }
                is Indicator.Supertrend -> stValues[ind.id]?.let { st ->
                    val vals = st.values; val isUp = st.isUp
                    if (ind.fillVisible) {
                        val pathUp = Path(); val pathDown = Path(); var lastX = -1000f; val minXGap = 1f; val limit = (startIdx + vals.size - 1).coerceAtMost(totalCandles - 1)
                        for (i in startIdx until limit) {
                            val v1 = vals[i]; val v2 = vals[i+1]; if (v1 == null || v2 == null || isUp[i] != isUp[i+1]) continue
                            val x1 = chartW - (candleW / 2f) - ((totalCandles - 1 - i - scrollOffset) * candleW); val x2 = x1 + candleW
                            if (x2 < 0) continue; if (x1 > chartW) break
                            if (abs(x1 - lastX) < minXGap && i != startIdx && i != limit - 1) continue
                            lastX = x1
                            val c1 = allCandles[i].close; val c2 = allCandles[i+1].close; val targetPath = if (isUp[i+1]) pathUp else pathDown
                            targetPath.moveTo(x1, normalizeY(v1)); targetPath.lineTo(x2, normalizeY(v2)); targetPath.lineTo(x2, normalizeY(c2)); targetPath.lineTo(x1, normalizeY(c1)); targetPath.close()
                        }
                        drawScope.drawPath(pathUp, ind.upFillColor); drawScope.drawPath(pathDown, ind.downFillColor)
                    }
                    val thicknessPx = drawScope.run { ind.thickness.dp.toPx() }; val effect = getPathEffect(ind.style, thicknessPx); val cap = if (ind.style == 2) StrokeCap.Round else StrokeCap.Butt
                    var lastX = -1000f; val minXGap = 1f; val limit = (startIdx + vals.size - 1).coerceAtMost(totalCandles - 1)
                    for (i in startIdx until limit) {
                        val v1 = vals[i]; val v2 = vals[i+1]; if (v1 == null || v2 == null) continue
                        val x1 = chartW - (candleW / 2f) - ((totalCandles - 1 - i - scrollOffset) * candleW); val x2 = x1 + candleW
                        if (x2 < 0) continue; if (x1 > chartW) break
                        if (abs(x1 - lastX) < minXGap && i != startIdx && i != limit - 1) continue
                        lastX = x1
                        if (isUp[i] == isUp[i+1]) drawScope.drawLine(if (isUp[i+1]) ind.upColor else ind.downColor, Offset(x1, normalizeY(v1)), Offset(x2, normalizeY(v2)), thicknessPx, pathEffect = effect, cap = cap)
                    }
                }
                is Indicator.Ribbon -> ribbonValues[ind.id]?.let { res ->
                    drawRibbon(drawScope, res, ind, startIdx, totalCandles, scrollOffset, candleW, chartW, normalizeY)
                }
                else -> {}
            }
        }
    }

    private fun drawRibbon(
        drawScope: DrawScope,
        res: IndicatorCalculators.RibbonResult,
        ind: Indicator.Ribbon,
        startIdx: Int,
        totalCandles: Int,
        scrollOffset: Float,
        candleW: Float,
        chartW: Float,
        normalizeY: (Float) -> Float
    ) = with(drawScope) {
        val thicknessPx = ind.thickness.dp.toPx()
        val lastThicknessPx = ind.lastThickness.dp.toPx()
        
        res.mas.forEachIndexed { maIdx, ma ->
            val isLast = maIdx == res.mas.size - 1
            val t = if (isLast) lastThicknessPx else thicknessPx
            
            var lastX = -1000f
            val minXGap = 1f
            val limit = (startIdx + ma.size - 1).coerceAtMost(totalCandles - 1)
            
            for (i in startIdx until limit) {
                val v1 = ma[i]; val v2 = ma[i+1]; if (v1 == null || v2 == null) continue
                val x1 = chartW - (candleW / 2f) - ((totalCandles - 1 - i - scrollOffset) * candleW); val x2 = x1 + candleW
                if (x2 < 0) continue; if (x1 > chartW) break
                if (abs(x1 - lastX) < minXGap && i != startIdx && i != limit - 1) continue
                lastX = x1
                
                val refV = res.refMa.getOrNull(i+1) ?: 0f
                val diff = v2 - (ma.getOrNull(i) ?: v2)
                
                val color = when {
                    diff >= 0 && v2 > refV -> Color(0xFF00FF00) // LIME
                    diff < 0 && v2 > refV -> Color(0xFF800000)  // MAROON
                    diff <= 0 && v2 < refV -> Color(0xFFFF0000) // RUBI
                    diff >= 0 && v2 < refV -> Color(0xFF008000) // GREEN
                    else -> Color(0xFF808080)                   // GRAY
                }
                
                drawLine(color, Offset(x1, normalizeY(v1)), Offset(x2, normalizeY(v2)), t)
            }
        }
    }

    fun drawOverlayIndicatorLabels(
        drawScope: DrawScope, 
        overlayIndicators: List<Indicator>, 
        smaValues: Map<String, List<Float?>>, 
        emaValues: Map<String, List<Float?>>, 
        hmaValues: Map<String, List<Float?>>,
        vwapValues: Map<String, IndicatorCalculators.VWAPResult>,
        bbValues: Map<String, IndicatorCalculators.BollingerBandsResult>, 
        atrBandsValues: Map<String, IndicatorCalculators.ATRBandsResult>,
        stValues: Map<String, IndicatorCalculators.SupertrendResult>, 
        alligatorValues: Map<String, IndicatorCalculators.AlligatorResult>, 
        ichimokuValues: Map<String, IndicatorCalculators.IchimokuResult>, 
        ribbonValues: Map<String, IndicatorCalculators.RibbonResult>,
        chartW: Float, 
        priceW: Float, 
        mainH: Float, 
        textMeasurer: TextMeasurer, 
        crosshairTextStyle: TextStyle, 
        normalizeY: (Float) -> Float
    ) {
        overlayIndicators.forEach { ind ->
            if (!ind.isVisible) return@forEach
            when (ind) {
                is Indicator.SMA -> smaValues[ind.id]?.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.color, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                is Indicator.EMA -> emaValues[ind.id]?.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.color, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                is Indicator.HMA -> hmaValues[ind.id]?.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.color, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                is Indicator.VWAP -> vwapValues[ind.id]?.let { res ->
                    res.vwap.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.color, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                    if (ind.showBands) {
                        res.upper1.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.upperColor, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                        res.lower1.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.lowerColor, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                    }
                }
                is Indicator.BollingerBands -> bbValues[ind.id]?.let { bb ->
                    bb.upper.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.upperColor, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                    bb.middle.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.middleColor, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                    bb.lower.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.lowerColor, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                }
                is Indicator.ATRBands -> atrBandsValues[ind.id]?.let { ab ->
                    ab.upper.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.upperColor, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                    ab.lower.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.lowerColor, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                    if (ind.showTPBands) {
                        ab.tpUpper.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.tpUpperColor, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                        ab.tpLower.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.tpLowerColor, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                    }
                }
                is Indicator.Alligator -> alligatorValues[ind.id]?.let { res ->
                    res.jaw.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.jawColor, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                    res.teeth.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.teethColor, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                    res.lips.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.lipsColor, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                }
                is Indicator.Ichimoku -> ichimokuValues[ind.id]?.let { res ->
                    res.tenkanSen.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.tenkanColor, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                    res.kijunSen.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.kijunColor, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                    res.senkouSpanA.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.senkouAColor, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                    res.senkouSpanB.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.senkouBColor, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                    res.chikouSpan.lastOrNull { it != null }?.let { drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(it), it, ind.chikouColor, textMeasurer, crosshairTextStyle, 2, 0f, mainH) }
                }
                is Indicator.Supertrend -> stValues[ind.id]?.let { st ->
                    st.values.lastOrNull { it != null }?.let { lastV ->
                        val isUp = st.isUp.lastOrNull() ?: true
                        drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(lastV), lastV, if (isUp) ind.upColor else ind.downColor, textMeasurer, crosshairTextStyle, 2, 0f, mainH)
                    }
                }
                is Indicator.Ribbon -> ribbonValues[ind.id]?.let { res ->
                    res.mas.lastOrNull()?.lastOrNull { it != null }?.let { lastV ->
                        val refV = res.refMa.lastOrNull() ?: 0f
                        val ma = res.mas.last()
                        val diff = lastV - (ma.getOrNull(ma.size - 2) ?: lastV)
                        val color = when {
                            diff >= 0 && lastV > refV -> Color(0xFF00FF00)
                            diff < 0 && lastV > refV -> Color(0xFF800000)
                            diff <= 0 && lastV < refV -> Color(0xFFFF0000)
                            diff >= 0 && lastV < refV -> Color(0xFF008000)
                            else -> Color(0xFF808080)
                        }
                        drawIndicatorLabel(drawScope, chartW, priceW, normalizeY(lastV), lastV, color, textMeasurer, crosshairTextStyle, 2, 0f, mainH)
                    }
                }
                else -> {}
            }
        }
    }

    fun drawVolumeLabel(drawScope: DrawScope, lastCandle: Candle, chartW: Float, priceW: Float, mainH: Float, maxVol: Float, settings: ChartSettings, textMeasurer: TextMeasurer, crosshairTextStyle: TextStyle) = with(drawScope) {
        if (maxVol <= 0) return@with
        val y = mainH - (lastCandle.volume / maxVol) * (mainH * 0.15f); val opaqueBg = (if (lastCandle.close >= lastCandle.open) settings.upColor else settings.downColor).copy(alpha = 1f)
        val style = crosshairTextStyle.copy(color = getContrastColor(opaqueBg), fontWeight = FontWeight.Bold); val layout = textMeasurer.measure(ChartUtils.formatVolume(lastCandle.volume), style)
        val rectH = layout.size.height.toFloat() + 4.dp.toPx(); val lY = (y - rectH / 2f).coerceIn(mainH - (mainH * 0.15f) - rectH, mainH - rectH)
        drawRect(opaqueBg, Offset(chartW, lY), Size(priceW + 20.dp.toPx(), rectH)); drawText(layout, topLeft = Offset(chartW + 6.dp.toPx(), lY + (rectH - layout.size.height) / 2f))
    }

    fun drawBottomIndicators(drawScope: DrawScope, bottomIndicators: List<Indicator>, indicatorHeights: Map<String, Float>, defaultHeightPx: Float, rsiValues: Map<String, List<Float?>>, rsiMaValues: Map<String, List<Float?>>, macdValues: Map<String, IndicatorCalculators.MACDResult>, stochValues: Map<String, IndicatorCalculators.StochResult>, atrValues: Map<String, List<Float?>>, indicatorRanges: Map<String, Pair<Float, Float>>, visibleCandles: List<Candle>, totalCandles: Int, startIdx: Int, endIdx: Int, scrollOffset: Float, candleW: Float, bodyW: Float, chartW: Float, priceW: Float, mainH: Float, settings: ChartSettings, textMeasurer: TextMeasurer, textStyle: TextStyle, crosshairTextStyle: TextStyle, draggingSeparatorIdx: Int, dragArea: Int, minimizedHeightPx: Float) = with(drawScope) {
        var currentY = mainH; val showHorizontal = settings.showGrid && (settings.gridLines == GridLines.BOTH || settings.gridLines == GridLines.HORIZONTAL); val horizontalThicknessPx = settings.horizontalGridThickness.toFloat().dp.toPx(); val horizontalEffect = getPathEffect(settings.horizontalGridStyle, horizontalThicknessPx); val hCap = if (settings.horizontalGridStyle == 2) StrokeCap.Round else StrokeCap.Butt
        val isDark = settings.backgroundColor.luminance() < 0.5f; val adaptiveGridColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f); val horizontalGridColor = settings.horizontalGridColor.copy(alpha = 0.2f).compositeOver(adaptiveGridColor)
        bottomIndicators.forEachIndexed { idx, ind ->
            val h = if (ind.isVisible) (indicatorHeights[ind.id] ?: defaultHeightPx).coerceAtLeast(1f) else minimizedHeightPx
            val range = indicatorRanges[ind.id] ?: Pair(0f, 100f); val minV = range.first; val maxV = range.second; val rV = (maxV - minV).coerceAtLeast(0.001f); val normIndY: (Float) -> Float = { currentY + h - ((it - minV) / rV * h) }
            if (ind.isVisible) {
                val adaptiveSteps = (h / 40.dp.toPx()).toInt().coerceIn(3, 10)
                when (ind) {
                    is Indicator.RSI -> {
                        val step = ChartUtils.calculatePriceStep(rV, targetSteps = adaptiveSteps); var level = ceil(minV / step) * step; var safety = 0
                        while (level <= maxV && safety < 50) {
                            val y = normIndY(level); if (y in currentY..currentY + h) {
                                drawLine(settings.scaleTextColor.copy(alpha = 0.1f), Offset(0f, y), Offset(chartW, y), 1f)
                                drawLine(settings.scaleTextColor.copy(alpha = 0.2f), Offset(chartW, y), Offset(chartW + 4.dp.toPx(), y), 1f)
                                safeDrawText(textMeasurer, level.toInt().toString(), Offset(chartW + 8.dp.toPx(), y - 6.dp.toPx()), textStyle.copy(color = settings.scaleTextColor, fontSize = settings.scaleTextSize.sp))
                            }
                            level += step; safety++
                        }
                        rsiValues[ind.id]?.lastOrNull { it != null }?.let { drawIndicatorLabel(this, chartW, priceW, normIndY(it), it, ind.color, textMeasurer, crosshairTextStyle, ind.precision, currentY, currentY + h) }
                    }
                    is Indicator.MACD -> macdValues[ind.id]?.let { res ->
                        val step = ChartUtils.calculatePriceStep(rV, targetSteps = adaptiveSteps); var pL = ceil(minV / step) * step; var safety = 0
                        while (pL <= maxV && safety < 50) {
                            val y = normIndY(pL); if (y in currentY..currentY + h) {
                                if (showHorizontal) drawLine(color = horizontalGridColor, start = Offset(0f, y), end = Offset(chartW, y), strokeWidth = horizontalThicknessPx, pathEffect = horizontalEffect, cap = hCap)
                                drawLine(settings.scaleTextColor.copy(alpha = 0.2f), Offset(chartW, y), Offset(chartW + 4.dp.toPx(), y), 1f)
                                safeDrawText(textMeasurer, String.format(Locale.getDefault(), "%.2f", pL), Offset(chartW + 8.dp.toPx(), y - 6.dp.toPx()), textStyle.copy(color = settings.scaleTextColor, fontSize = settings.scaleTextSize.sp))
                            }
                            pL += step; safety++
                        }
                        res.macdLine.lastOrNull { it != null }?.let { drawIndicatorLabel(this, chartW, priceW, normIndY(it), it, ind.macdColor, textMeasurer, crosshairTextStyle, 2, currentY, currentY + h) }
                        res.signalLine.lastOrNull { it != null }?.let { drawIndicatorLabel(this, chartW, priceW, normIndY(it), it, ind.signalColor, textMeasurer, crosshairTextStyle, 2, currentY, currentY + h) }
                        res.histogram.lastOrNull { it != null }?.let { drawIndicatorLabel(this, chartW, priceW, normIndY(it), it, if (it >= 0) ind.histColorUp else ind.histColorDown, textMeasurer, crosshairTextStyle, 2, currentY, currentY + h) }
                    }
                    is Indicator.Stochastic -> stochValues[ind.id]?.let { res ->
                        val step = ChartUtils.calculatePriceStep(rV, targetSteps = adaptiveSteps); var level = ceil(minV / step) * step; var safety = 0
                        while (level <= maxV && safety < 50) {
                            val y = normIndY(level); if (y in currentY..currentY + h) {
                                drawLine(settings.scaleTextColor.copy(alpha = 0.1f), Offset(0f, y), Offset(chartW, y), 1f)
                                drawLine(settings.scaleTextColor.copy(alpha = 0.2f), Offset(chartW, y), Offset(chartW + 4.dp.toPx(), y), 1f)
                                safeDrawText(textMeasurer, level.toInt().toString(), Offset(chartW + 8.dp.toPx(), y - 6.dp.toPx()), textStyle.copy(color = settings.scaleTextColor, fontSize = settings.scaleTextSize.sp))
                            }
                            level += step; safety++
                        }
                        res.k.lastOrNull { it != null }?.let { drawIndicatorLabel(this, chartW, priceW, normIndY(it), it, ind.kColor, textMeasurer, crosshairTextStyle, 2, currentY, currentY + h) }
                        res.d.lastOrNull { it != null }?.let { drawIndicatorLabel(this, chartW, priceW, normIndY(it), it, ind.dColor, textMeasurer, crosshairTextStyle, 2, currentY, currentY + h) }
                    }
                    is Indicator.ATR -> {
                        val step = ChartUtils.calculatePriceStep(rV, targetSteps = adaptiveSteps); var level = ceil(minV / step) * step; var safety = 0
                        while (level <= maxV && safety < 50) {
                            val y = normIndY(level); if (y in currentY..currentY + h) {
                                drawLine(settings.scaleTextColor.copy(alpha = 0.1f), Offset(0f, y), Offset(chartW, y), 1f)
                                drawLine(settings.scaleTextColor.copy(alpha = 0.2f), Offset(chartW, y), Offset(chartW + 4.dp.toPx(), y), 1f)
                                safeDrawText(textMeasurer, String.format(Locale.getDefault(), "%.2f", level), Offset(chartW + 8.dp.toPx(), y - 6.dp.toPx()), textStyle.copy(color = settings.scaleTextColor, fontSize = settings.scaleTextSize.sp))
                            }
                            level += step; safety++
                        }
                        atrValues[ind.id]?.lastOrNull { it != null }?.let { drawIndicatorLabel(this, chartW, priceW, normIndY(it), it, ind.color, textMeasurer, crosshairTextStyle, 2, currentY, currentY + h) }
                    }
                    else -> {}
                }
                clipRect(top = currentY, right = chartW, bottom = currentY + h) {
                    when (ind) {
                        is Indicator.RSI -> rsiValues[ind.id]?.let { vals ->
                            val y70 = normIndY(70f); val y50 = normIndY(50f); val y30 = normIndY(30f)
                            drawRect(color = Color(0xFF9C27B0).copy(alpha = 0.05f), topLeft = Offset(0f, min(y70, y30)), size = Size(chartW, abs(y70 - y30)))
                            val obPath = Path(); val osPath = Path(); var obStarted = false; var osStarted = false; var lastX = -1000f; val minXGap = 1f
                            vals.forEachIndexed { i, v ->
                                if (v == null || i < startIdx || i >= endIdx) return@forEachIndexed
                                val x = chartW - (candleW / 2f) - ((vals.size - 1 - i - scrollOffset) * candleW)
                                if (x < -10 || x > chartW + 10) return@forEachIndexed
                                if (abs(x - lastX) < minXGap && i != startIdx && i != endIdx - 1) return@forEachIndexed
                                lastX = x; val y = normIndY(v)
                                if (v > 70f) { if (!obStarted) { obPath.moveTo(x, y70); obStarted = true }; obPath.lineTo(x, y) } else if (obStarted) { obPath.lineTo(x, y70); obPath.close(); obStarted = false }
                                if (v < 30f) { if (!osStarted) { osPath.moveTo(x, y30); osStarted = true }; osPath.lineTo(x, y) } else if (osStarted) { osPath.lineTo(x, y30); osPath.close(); osStarted = false }
                            }
                            if (obStarted) { obPath.lineTo(chartW, y70); obPath.close() }; if (osStarted) { osPath.lineTo(chartW, y30); osPath.close() }
                            drawPath(obPath, brush = Brush.verticalGradient(listOf(Color(0xFF26A69A).copy(0.3f), Color(0xFF26A69A).copy(0.01f)), startY = normIndY(100f), endY = y70))
                            drawPath(osPath, brush = Brush.verticalGradient(listOf(Color(0xFFEF5350).copy(0.01f), Color(0xFFEF5350).copy(0.3f)), startY = y30, endY = normIndY(0f)))
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                            drawLine(Color.Gray.copy(0.3f), Offset(0f, y70), Offset(chartW, y70), 1f, pathEffect = dashEffect)
                            drawLine(Color.Gray.copy(0.15f), Offset(0f, y50), Offset(chartW, y50), 1f, pathEffect = dashEffect)
                            drawLine(Color.Gray.copy(0.3f), Offset(0f, y30), Offset(chartW, y30), 1f, pathEffect = dashEffect)
                            if (ind.showRsi) ChartUtils.drawIndicatorPath(this, vals, startIdx, endIdx, scrollOffset, candleW, chartW, ind.color, thickness = ind.thickness, style = ind.style, normY = normIndY)
                            if (ind.showMa) rsiMaValues[ind.id]?.let { ChartUtils.drawIndicatorPath(this, it, startIdx, endIdx, scrollOffset, candleW, chartW, ind.maColor, thickness = ind.maThickness, style = ind.maStyle, normY = normIndY) }
                        }
                        is Indicator.MACD -> macdValues[ind.id]?.let { res ->
                            val zeroY = normIndY(0f); var lastX = -1000f; val minXGap = 1f
                            visibleCandles.forEachIndexed { i, _ ->
                                val iIdx = startIdx + i; if (iIdx < totalCandles) {
                                    val x = chartW - (candleW / 2f) - ((totalCandles - 1 - iIdx - scrollOffset) * candleW)
                                    if (x < -candleW || x > chartW + candleW || (abs(x - lastX) < minXGap && i != 0 && i != visibleCandles.size - 1)) return@forEachIndexed
                                    lastX = x; val hv = res.histogram.getOrNull(iIdx) ?: return@forEachIndexed
                                    drawRect(if (hv >= 0) ind.histColorUp.copy(0.4f) else ind.histColorDown.copy(0.4f), Offset(x - bodyW / 2, min(zeroY, normIndY(hv))), Size(bodyW, abs(zeroY - normIndY(hv))))
                                }
                            }
                            ChartUtils.drawIndicatorPath(this, res.macdLine, startIdx, endIdx, scrollOffset, candleW, chartW, ind.macdColor, thickness = ind.macdThickness, style = ind.macdStyle, normY = normIndY)
                            ChartUtils.drawIndicatorPath(this, res.signalLine, startIdx, endIdx, scrollOffset, candleW, chartW, ind.signalColor, thickness = ind.signalThickness, style = ind.signalStyle, normY = normIndY)
                        }
                        is Indicator.Stochastic -> stochValues[ind.id]?.let { res ->
                            val yUpper = normIndY(ind.upperLevel.toFloat()); val yLower = normIndY(ind.lowerLevel.toFloat())
                            if (ind.backgroundVisible) drawRect(color = ind.backgroundColor, topLeft = Offset(0f, min(yUpper, yLower)), size = Size(chartW, abs(yUpper - yLower)))
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                            drawLine(ind.upperLevelColor.copy(0.3f), Offset(0f, yUpper), Offset(chartW, yUpper), 1f, pathEffect = dashEffect)
                            drawLine(ind.lowerLevelColor.copy(0.3f), Offset(0f, yLower), Offset(chartW, yLower), 1f, pathEffect = dashEffect)
                            ChartUtils.drawIndicatorPath(this, res.k, startIdx, endIdx, scrollOffset, candleW, chartW, ind.kColor, thickness = ind.kThickness, style = ind.kStyle, normY = normIndY)
                            ChartUtils.drawIndicatorPath(this, res.d, startIdx, endIdx, scrollOffset, candleW, chartW, ind.dColor, thickness = ind.dThickness, style = ind.dStyle, normY = normIndY)
                        }
                        is Indicator.ATR -> atrValues[ind.id]?.let { vals ->
                            ChartUtils.drawIndicatorPath(this, vals, startIdx, endIdx, scrollOffset, candleW, chartW, ind.color, thickness = ind.thickness, style = ind.style, normY = normIndY)
                        }
                        else -> {}
                    }
                }
            }
            if (draggingSeparatorIdx == idx && dragArea == 4) drawRect(Color(0xFF2962FF).copy(0.2f), Offset(0f, currentY - 10.dp.toPx()), Size(chartW, 20.dp.toPx()))
            drawLine(if (draggingSeparatorIdx == idx && dragArea == 4) Color(0xFF2962FF) else settings.paneSeparatorColor, Offset(0f, currentY), Offset(chartW, currentY), 2.dp.toPx()); currentY += h
        }
    }

    fun drawCrosshair(drawScope: DrawScope, p: Offset, chartW: Float, fullH: Float, mainH: Float, priceW: Float, timeH: Float, settings: ChartSettings, crosshairTextStyle: TextStyle, textMeasurer: TextMeasurer, allCandles: List<Candle>, scrollOffset: Float, candleW: Float, crosshairTimeFormatter: SimpleDateFormat, denormalizeY: (Float) -> Float, bottomIndicators: List<Indicator>, indicatorHeights: Map<String, Float>, indicatorRanges: Map<String, Pair<Float, Float>>, defaultHeightPx: Float, labelBgColor: Color, timeframe: String = "1h", displayCount: Float = 100f, secondaryCrosshairTimeFormatter: SimpleDateFormat? = null, showDualTimezone: Boolean = false, isDrawingMode: Boolean = false) = with(drawScope) {
        val plotsH = fullH - timeH
        // Niveau 1 volontairement plus fin que 1dp pour une mire plus legere.
        val crosshairThicknessDp = if (settings.crosshairThickness <= 1) 0.75f else settings.crosshairThickness.toFloat()
        val crosshairThicknessPx = crosshairThicknessDp.dp.toPx()
        val effect = getPathEffect(settings.crosshairStyle, crosshairThicknessPx)
        val cap = if (settings.crosshairStyle == 2) StrokeCap.Round else StrokeCap.Butt
        clipRect(right = chartW + priceW, bottom = fullH) {
            // Keep the main vertical crosshair inside the plotting area (above the time label band).
            drawLine(settings.crosshairColor.copy(alpha = 0.8f), Offset(p.x, 0f), Offset(p.x, plotsH), crosshairThicknessPx, pathEffect = effect, cap = cap)
            drawLine(settings.crosshairColor.copy(alpha = 0.8f), Offset(0f, p.y), Offset(chartW + priceW, p.y), crosshairThicknessPx, pathEffect = effect, cap = cap)
        }
        val opaqueLabelBg = labelBgColor.copy(alpha = 1f); val style = crosshairTextStyle.copy(color = getContrastColor(opaqueLabelBg), fontWeight = FontWeight.Bold)
        val refSize = if (allCandles.isEmpty()) (displayCount + 20).toInt() else allCandles.size; val idx = round(refSize - 1 - scrollOffset - (chartW - p.x - candleW / 2f) / candleW).toInt()
        val timestamp = if (allCandles.isNotEmpty() && idx in allCandles.indices) allCandles[idx].timestamp else {
            val lastDataIdx = refSize - 1; val lastDataTs = if (allCandles.isNotEmpty()) allCandles.last().timestamp else System.currentTimeMillis(); val tfM = ChartUtils.timeframeToMillis(timeframe)
            if (idx > lastDataIdx) lastDataTs + (idx - lastDataIdx) * tfM else (if (allCandles.isNotEmpty()) allCandles.first().timestamp else System.currentTimeMillis()) + idx * tfM
        }
        // Calcul de la hauteur du rectangle en fonction du mode double fuseau horaire
        val rectH = if (showDualTimezone && secondaryCrosshairTimeFormatter != null) 32.dp.toPx() else 22.dp.toPx()
        val mainTimeText = crosshairTimeFormatter.format(Date(timestamp))
        val layout = textMeasurer.measure(mainTimeText, style)
        
        // Calcul de la largeur du rectangle en tenant compte du texte secondaire
        var rectW = layout.size.width + 16.dp.toPx()
        if (showDualTimezone && secondaryCrosshairTimeFormatter != null) {
            val secondaryTimeText = secondaryCrosshairTimeFormatter.format(Date(timestamp))
            val secondaryLayout = textMeasurer.measure(secondaryTimeText, style.copy(fontSize = style.fontSize * 0.8f))
            rectW = maxOf(rectW, secondaryLayout.size.width + 16.dp.toPx())
        }
        
        val lX = (p.x - rectW / 2f).coerceIn(0f, chartW - rectW)
        drawRect(opaqueLabelBg, Offset(lX, plotsH), Size(rectW, rectH))
        
        // Affichage du temps principal
        drawText(layout, topLeft = Offset(lX + 8.dp.toPx(), plotsH + 4.dp.toPx()))
        
        // Affichage du temps secondaire si activé
        if (showDualTimezone && secondaryCrosshairTimeFormatter != null) {
            val secondaryTimeText = secondaryCrosshairTimeFormatter.format(Date(timestamp))
            val secondaryStyle = style.copy(
                fontSize = style.fontSize * 0.8f,
                color = style.color.copy(alpha = 0.7f)
            )
            val secondaryLayout = textMeasurer.measure(secondaryTimeText, secondaryStyle)
            drawText(secondaryLayout, topLeft = Offset(lX + 8.dp.toPx(), plotsH + 18.dp.toPx()))
        }
        if (p.y <= mainH) {
            val pTxt = ChartUtils.formatPrice(denormalizeY(p.y)); val layoutP = textMeasurer.measure(pTxt, style); val rectWP = priceW + 12.dp.toPx(); val rectHP = layoutP.size.height.toFloat() + 8.dp.toPx(); val lYP = (p.y - rectHP / 2f).coerceIn(0f, mainH - rectHP)
            drawRect(opaqueLabelBg, Offset(chartW, lYP), Size(rectWP, rectHP)); drawText(layoutP, topLeft = Offset(chartW + 6.dp.toPx(), lYP + (rectHP - layoutP.size.height) / 2f))
        } else {
            var currentY = mainH; for (ind in bottomIndicators) {
                val h = if (ind.isVisible) (indicatorHeights[ind.id] ?: defaultHeightPx).coerceAtLeast(1f) else 0f
                if (h > 0f && p.y in currentY..currentY + h) {
                    val range = indicatorRanges[ind.id] ?: Pair(0f, 100f); val valAtY = range.second - ((p.y - currentY) / h * (range.second - range.first)); val layoutP = textMeasurer.measure(String.format(Locale.getDefault(), "%.2f", valAtY), style); val rectWP = priceW + 12.dp.toPx(); val rectHP = layoutP.size.height.toFloat() + 8.dp.toPx(); val lYP = (p.y - rectHP / 2f).coerceIn(currentY, currentY + h - rectHP)
                    drawRect(opaqueLabelBg, Offset(chartW, lYP), Size(rectWP, rectHP)); drawText(layoutP, topLeft = Offset(chartW + 6.dp.toPx(), lYP + (rectHP - layoutP.size.height) / 2f)); break
                }
                currentY += if (ind.isVisible) h else 24.dp.toPx()
            }
        }
        
        // Ajout d'un point rouge pour différencier le viseur de traçage
        if (isDrawingMode && p.x in 0f..chartW && p.y in 0f..mainH) {
            drawCircle(Color.Red, radius = 4.dp.toPx(), center = p, alpha = 0.8f)
            drawCircle(Color.White, radius = 2.dp.toPx(), center = p, alpha = 0.9f)
        }
    }

    fun drawLastPriceLabel(drawScope: DrawScope, lastCandle: Candle, chartW: Float, mainH: Float, priceW: Float, settings: ChartSettings, crosshairTextStyle: TextStyle, textMeasurer: TextMeasurer, normalizeY: (Float) -> Float) = with(drawScope) {
        val lastY = normalizeY(lastCandle.close); if (lastY in 0f..mainH) {
            val opaqueBg = (if (lastCandle.close >= lastCandle.open) settings.upColor else settings.downColor).copy(alpha = 1f); val style = crosshairTextStyle.copy(color = getContrastColor(opaqueBg), fontWeight = FontWeight.Bold); val layout = textMeasurer.measure(ChartUtils.formatPrice(lastCandle.close), style); val rectH = layout.size.height.toFloat() + 8.dp.toPx()
            drawRect(opaqueBg, Offset(chartW, lastY - rectH / 2f), Size(priceW + 20.dp.toPx(), rectH)); drawText(layout, topLeft = Offset(chartW + 8.dp.toPx(), lastY - layout.size.height / 2f))
        }
    }

    fun drawTimeLabels(
        drawScope: DrawScope,
        allCandles: List<Candle>,
        startIdx: Int,
        totalCandles: Int,
        scrollOffset: Float,
        candleW: Float,
        chartW: Float,
        plotsH: Float,
        displayCount: Float,
        timeFormatter: SimpleDateFormat,
        settings: ChartSettings,
        textStyle: TextStyle,
        textMeasurer: TextMeasurer,
        labelBgColor: Color,
        timeframe: String = "1h",
        secondaryTimeFormatter: SimpleDateFormat? = null,
        showDualTimezone: Boolean = false
    ) = with(drawScope) {
        if (chartW <= 0) return@with
        
        // Extension de la barre de temps pour une meilleure continuité visuelle
        val extendedChartWidth = chartW + 150.dp.toPx()
        drawRect(color = labelBgColor, topLeft = Offset(0f, plotsH), size = Size(extendedChartWidth, 40.dp.toPx()))
        drawLine(color = settings.scaleLinesColor.copy(alpha = 0.15f), start = Offset(0f, plotsH), end = Offset(extendedChartWidth, plotsH), strokeWidth = 1f)

        val labelStyle = textStyle.copy(color = settings.scaleTextColor, fontSize = settings.scaleTextSize.sp)
        val boldStyle = labelStyle.copy(fontWeight = FontWeight.Bold, fontSize = (settings.scaleTextSize + 1).sp)
        val labelsTimeZone = timeFormatter.timeZone ?: TimeZone.getDefault()
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault()).apply { timeZone = labelsTimeZone }
        val cal = Calendar.getInstance(labelsTimeZone)
        val calPrev = Calendar.getInstance(labelsTimeZone)
        val minSpace = 25.dp.toPx()

        data class TimeMark(val index: Int, val weight: Int, val text: String, val isBold: Boolean, val isImportant: Boolean = false, val isWeekend: Boolean = false, var x: Float = 0f, var width: Float = 0f)
        val marks = mutableListOf<TimeMark>()
        
        val tfUnit = timeframe.filter { it.isLetter() }.lowercase()
        val isLongTermTf = tfUnit == "d" || tfUnit == "w" || tfUnit == "mn"
        
        val tfMillis = ChartUtils.timeframeToMillis(timeframe)
        val refCount = if (allCandles.isEmpty()) (displayCount + 20).toInt() else totalCandles
        val lastDataIdx = refCount - 1
        val lastDataTimestamp = if (allCandles.isNotEmpty()) allCandles.last().timestamp else System.currentTimeMillis()
        
        val rightVisibleIdx = (lastDataIdx - scrollOffset + settings.marginRightBars + 50).toInt()
        val leftVisibleIdx = (rightVisibleIdx - displayCount - 100).toInt().coerceAtLeast(-2000)

        var lastTradingTs = -1L
        
        // Espacement adaptatif selon la taille de l'écran
        val adaptiveSpacing = when {
            chartW < 600 -> 75.dp.toPx() // Petits écrans
            chartW < 1200 -> 85.dp.toPx() // Écrans moyens
            else -> 95.dp.toPx() // Grands écrans
        }
        val targetLabelCount = (chartW / adaptiveSpacing).toInt().coerceAtLeast(1)
        val minutesInView = displayCount * (tfMillis / 60000f)
        val idealMinutesPerLabel = minutesInView / targetLabelCount
        
        val intervalMinutes = when {
            idealMinutesPerLabel <= 1 -> 1
            idealMinutesPerLabel <= 2 -> 2
            idealMinutesPerLabel <= 5 -> 5
            idealMinutesPerLabel <= 10 -> 10
            idealMinutesPerLabel <= 15 -> 15
            idealMinutesPerLabel <= 30 -> 30
            idealMinutesPerLabel <= 60 -> 60
            idealMinutesPerLabel <= 120 -> 120
            idealMinutesPerLabel <= 180 -> 180
            idealMinutesPerLabel <= 240 -> 240
            idealMinutesPerLabel <= 360 -> 360
            idealMinutesPerLabel <= 720 -> 720
            else -> 1440
        }

        for (i in leftVisibleIdx until rightVisibleIdx) {
            val timestamp = if (allCandles.isNotEmpty() && i in allCandles.indices) {
                allCandles[i].timestamp
            } else if (i > lastDataIdx) {
                lastDataTimestamp + (i - lastDataIdx) * tfMillis
            } else {
                (if (allCandles.isNotEmpty()) allCandles.first().timestamp else System.currentTimeMillis()) + i * tfMillis
            }
            
            cal.timeInMillis = timestamp
            
            // Détecter si c'est un week-end pour appliquer un style différent
            val isWeekendDay = !isLongTermTf && isHoliday(cal)
            
            // Ne pas sauter les week-ends, mais les marquer pour un style différent
            // if (!isLongTermTf && isHoliday(cal)) {
            //     continue
            // }

            if (lastTradingTs != -1L) {
                calPrev.timeInMillis = lastTradingTs
                val newYear = cal.get(Calendar.YEAR) != calPrev.get(Calendar.YEAR)
                val newMonth = cal.get(Calendar.MONTH) != calPrev.get(Calendar.MONTH)
                val newDay = cal.get(Calendar.DAY_OF_YEAR) != calPrev.get(Calendar.DAY_OF_YEAR)
                
                val totalMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                val prevTotalMinutes = calPrev.get(Calendar.HOUR_OF_DAY) * 60 + calPrev.get(Calendar.MINUTE)
                
                val isIntervalMark = (totalMinutes / intervalMinutes) != (prevTotalMinutes / intervalMinutes) || newDay
                
                // Calculate day step to avoid showing too many days when zoomed out
                val daysInView = (displayCount * tfMillis / 86400000f).toInt().coerceAtLeast(1)
                val dayStep = when {
                    daysInView > 120 -> 10
                    daysInView > 60 -> 7
                    daysInView > 30 -> 5
                    daysInView > 14 -> 3
                    daysInView > 7 -> 2
                    else -> 1
                }
                val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                val showThisDay = (dayOfMonth % dayStep == 1) || dayStep == 1

                when {
                    newYear -> marks.add(TimeMark(i, 100, cal.get(Calendar.YEAR).toString(), true))
                    newMonth -> marks.add(TimeMark(i, 90, monthFormat.format(cal.time).replaceFirstChar { it.uppercase() }, true))
                    newDay && showThisDay -> {
                        val isImportant = isImportantDate(cal)
                        marks.add(TimeMark(i, if (isImportant) 85 else 80, dayOfMonth.toString(), isImportant, isImportant, isWeekendDay))
                    }
                    isIntervalMark && !isLongTermTf -> {
                        // When zoomed out (sparser day labels), hide intraday time labels to avoid isolated hours.
                        if (dayStep > 1) continue

                        val h = cal.get(Calendar.HOUR_OF_DAY)
                        val m = cal.get(Calendar.MINUTE)
                        val timeLine = timeFormatter.format(Date(timestamp)).lineSequence().firstOrNull()?.trim()
                        val timeStr = if (!timeLine.isNullOrEmpty()) timeLine else String.format(Locale.getDefault(), "%02d:%02d", h, m)

                        // Midnight time labels are visually redundant with day/month markers when zoomed out.
                        val isMidnight = h == 0 && m == 0
                        if (isMidnight) continue

                        val weight = if (m == 0) (if (h % 6 == 0) 70 else 60) else 50
                        marks.add(TimeMark(i, weight, timeStr, false))
                    }
                }
            }
            lastTradingTs = timestamp
        }

        marks.forEach { mark ->
            mark.x = chartW - (candleW / 2f) - ((refCount - 1 - mark.index - scrollOffset) * candleW)
            val style = if (mark.isBold) boldStyle else labelStyle
            mark.width = textMeasurer.measure(mark.text, style).size.width.toFloat()
        }

        val sortedMarks = marks.sortedWith(compareByDescending<TimeMark> { it.weight }.thenBy { it.index })
        val finalMarks = mutableListOf<TimeMark>()
        for (mark in sortedMarks) {
            val hasOverlap = finalMarks.any { existing -> 
                abs(mark.x - existing.x) < (mark.width + existing.width) / 2f + minSpace 
            }
            if (!hasOverlap) finalMarks.add(mark)
        }

        val isDark = settings.backgroundColor.luminance() < 0.5f
        val verticalGridColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
        val overdrawMargin = 1000f

        finalMarks.forEach { mark ->
            if (mark.x < -overdrawMargin || mark.x > chartW + overdrawMargin) return@forEach
            val style = if (mark.isBold) boldStyle else labelStyle

            if (settings.showGrid && (settings.gridLines == GridLines.BOTH || settings.gridLines == GridLines.VERTICAL)) {
                if (mark.x in 0f..chartW) {
                    val vPx = settings.verticalGridThickness.toFloat().dp.toPx()
                    // Grille plus visible pour les dates importantes
                    val gridAlpha = if (mark.isBold) 0.15f else 0.08f
                    val gridColor = if (isDark) Color.White.copy(alpha = gridAlpha) else Color.Black.copy(alpha = gridAlpha * 0.75f)
                    drawLine(color = gridColor, start = Offset(mark.x, 0f), end = Offset(mark.x, plotsH), strokeWidth = vPx, pathEffect = getPathEffect(settings.verticalGridStyle, vPx))
                }
            }

            if (mark.x > -mark.width / 2f && mark.x < chartW + mark.width / 2f) {
                if (mark.x in 0f..chartW) {
                    // Style différent pour les week-ends
                    val isWeekend = mark.isWeekend
                    
                    // Ligne verticale avec style adapté
                    val tickAlpha = when {
                        mark.isBold -> 0.6f
                        isWeekend -> 0.2f // Très peu visible pour les week-ends
                        else -> 0.4f
                    }
                    val tickHeight = when {
                        mark.isBold -> 8.dp.toPx()
                        isWeekend -> 4.dp.toPx() // Plus petit pour les week-ends
                        else -> 6.dp.toPx()
                    }
                    val tickStroke = if (mark.isBold) 1.5f else if (isWeekend) 0.5f else 1f
                    
                    drawLine(color = settings.scaleLinesColor.copy(alpha = tickAlpha), start = Offset(mark.x, plotsH), end = Offset(mark.x, plotsH + tickHeight), strokeWidth = tickStroke)
                    
                    // Style de texte adapté pour les week-ends et fuseaux horaires multiples
                    if (mark.isBold) {
                        val rectW = mark.width + 16.dp.toPx()
                        val rectH = if (showDualTimezone && secondaryTimeFormatter != null) 26.dp.toPx() else 20.dp.toPx()
                        val rectX = mark.x - rectW / 2f
                        val rectY = plotsH + 4.dp.toPx()
                        
                        // Fond avec bordure pour les dates importantes
                        drawRect(labelBgColor.copy(alpha = 0.9f), Offset(rectX, rectY), Size(rectW, rectH))
                        drawRect(settings.scaleLinesColor.copy(alpha = 0.3f), Offset(rectX, rectY), Size(rectW, rectH), style = Stroke(1.dp.toPx()))
                        
                        safeDrawText(textMeasurer, mark.text, Offset(mark.x - mark.width / 2f, plotsH + 6.dp.toPx()), style)
                        
                        // Ajout du fuseau horaire secondaire pour les dates importantes
                        if (showDualTimezone && secondaryTimeFormatter != null) {
                            // Récupérer le timestamp pour cette marque
                            val markTimestamp = when {
                                mark.index in allCandles.indices -> allCandles[mark.index].timestamp
                                mark.index >= allCandles.size -> {
                                    val lastTimestamp = allCandles.lastOrNull()?.timestamp ?: System.currentTimeMillis()
                                    lastTimestamp + (mark.index - (allCandles.size - 1)) * ChartUtils.timeframeToMillis(timeframe)
                                }
                                else -> {
                                    val firstTimestamp = allCandles.firstOrNull()?.timestamp ?: System.currentTimeMillis()
                                    firstTimestamp + mark.index * ChartUtils.timeframeToMillis(timeframe)
                                }
                            }
                            
                            val secondaryTime = secondaryTimeFormatter.format(Date(markTimestamp))
                            val secondaryStyle = labelStyle.copy(
                                color = settings.scaleTextColor.copy(alpha = 0.6f),
                                fontSize = (settings.scaleTextSize - 2).sp
                            )
                            safeDrawText(textMeasurer, secondaryTime, Offset(mark.x - mark.width / 2f, plotsH + 18.dp.toPx()), secondaryStyle)
                        }
                    } else if (isWeekend) {
                        // Style week-end : grisé, plus petit, italique
                        val weekendStyle = labelStyle.copy(
                            color = settings.scaleTextColor.copy(alpha = 0.4f),
                            fontSize = (settings.scaleTextSize - 1).sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                        safeDrawText(textMeasurer, mark.text, Offset(mark.x - mark.width / 2f, plotsH + 10.dp.toPx()), weekendStyle)
                        
                        // Ajout du fuseau horaire secondaire pour les week-ends
                        if (showDualTimezone && secondaryTimeFormatter != null) {
                            val markTimestamp = when {
                                mark.index in allCandles.indices -> allCandles[mark.index].timestamp
                                mark.index >= allCandles.size -> {
                                    val lastTimestamp = allCandles.lastOrNull()?.timestamp ?: System.currentTimeMillis()
                                    lastTimestamp + (mark.index - (allCandles.size - 1)) * ChartUtils.timeframeToMillis(timeframe)
                                }
                                else -> {
                                    val firstTimestamp = allCandles.firstOrNull()?.timestamp ?: System.currentTimeMillis()
                                    firstTimestamp + mark.index * ChartUtils.timeframeToMillis(timeframe)
                                }
                            }
                            
                            val secondaryTime = secondaryTimeFormatter.format(Date(markTimestamp))
                            val secondaryWeekendStyle = weekendStyle.copy(
                                fontSize = (settings.scaleTextSize - 3).sp
                            ).copy(color = weekendStyle.color.copy(alpha = 0.3f))
                            safeDrawText(textMeasurer, secondaryTime, Offset(mark.x - mark.width / 2f, plotsH + 22.dp.toPx()), secondaryWeekendStyle)
                        }
                    } else {
                        safeDrawText(textMeasurer, mark.text, Offset(mark.x - mark.width / 2f, plotsH + 10.dp.toPx()), style)
                        
                        // Ajout du fuseau horaire secondaire pour les dates normales
                        if (showDualTimezone && secondaryTimeFormatter != null) {
                            val markTimestamp = when {
                                mark.index in allCandles.indices -> allCandles[mark.index].timestamp
                                mark.index >= allCandles.size -> {
                                    val lastTimestamp = allCandles.lastOrNull()?.timestamp ?: System.currentTimeMillis()
                                    lastTimestamp + (mark.index - (allCandles.size - 1)) * ChartUtils.timeframeToMillis(timeframe)
                                }
                                else -> {
                                    val firstTimestamp = allCandles.firstOrNull()?.timestamp ?: System.currentTimeMillis()
                                    firstTimestamp + mark.index * ChartUtils.timeframeToMillis(timeframe)
                                }
                            }
                            
                            val secondaryTime = secondaryTimeFormatter.format(Date(markTimestamp))
                            val secondaryStyle = labelStyle.copy(
                                color = settings.scaleTextColor.copy(alpha = 0.5f),
                                fontSize = (settings.scaleTextSize - 2).sp
                            )
                            safeDrawText(textMeasurer, secondaryTime, Offset(mark.x - mark.width / 2f, plotsH + 22.dp.toPx()), secondaryStyle)
                        }
                    }
                }
            }
        }
    }

    fun drawBorders(drawScope: DrawScope, chartW: Float, priceW: Float, h: Float, settings: ChartSettings) = with(drawScope) { drawLine(settings.scaleLinesColor.copy(0.15f), Offset(chartW, 0f), Offset(chartW, h), 1f); drawLine(settings.scaleLinesColor.copy(0.15f), Offset(0f, h), Offset(chartW + priceW, h), 1f) }

    // ══════════════════════════════════════════════════════════════════════════
    // DRAWING TOOLS RENDERER — professional TradingView-style rendering
    // ══════════════════════════════════════════════════════════════════════════

    fun drawAllDrawings(
        drawScope: DrawScope,
        drawings: List<com.bthr.backtest.model.Drawing>,
        mapper: DrawingCoordinateMapper,
        selectedId: String?,
        drawingState: DrawingState,
        textMeasurer: TextMeasurer,
        candles: List<com.bthr.backtest.model.Candle>,
        density: androidx.compose.ui.unit.Density,
        activeTool: com.bthr.backtest.model.DrawingTool,
        timeframe: com.bthr.backtest.model.Timeframe
    ) = with(drawScope) {
        drawings.forEach { drawing ->
            if (!drawing.isVisible) return@forEach
            val isSelected = drawing.id == selectedId
            drawSingleDrawing(this, drawing, mapper, isSelected, textMeasurer, candles, drawingState, density, activeTool, timeframe)
        }

        // Preview ghost while placing points
        drawPlacementPreview(this, drawingState, mapper, density, activeTool)
    }

    private fun DrawScope.getPathEffect(style: com.bthr.backtest.model.LineStyle, sw: Float): PathEffect? =
        when (style) {
            com.bthr.backtest.model.LineStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(sw * 4, sw * 2))
            com.bthr.backtest.model.LineStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(0.1f, sw * 3))
            else -> null
        }

    private fun drawSingleDrawing(
        drawScope: DrawScope,
        d: com.bthr.backtest.model.Drawing,
        mapper: DrawingCoordinateMapper,
        isSelected: Boolean,
        textMeasurer: TextMeasurer,
        candles: List<com.bthr.backtest.model.Candle>,
        drawingState: com.bthr.backtest.util.DrawingState,
        density: androidx.compose.ui.unit.Density,
        activeTool: com.bthr.backtest.model.DrawingTool,
        timeframe: com.bthr.backtest.model.Timeframe
    ) = with(drawScope) {
        val pe = getPathEffect(d.lineStyle, d.strokeWidth)

        when (d) {
            // ── TrendLine / Ray / ExtendedLine ───────────────────────────────
            is com.bthr.backtest.model.Drawing.TrendLine -> {
                val px1 = mapper.anchorToPixel(d.p1); val px2 = mapper.anchorToPixel(d.p2)
                val (s, e) = mapper.extendLine(px1, px2, d.extend,
                    right = mapper.chartWidthPx, bottom = mapper.chartHeightPx)
                // Use custom trend line drawing with red color and bubbles ONLY when TREND_LINE tool is active
                if (activeTool == com.bthr.backtest.model.DrawingTool.TREND_LINE) {
                    val isDragging = drawingState is com.bthr.backtest.util.DrawingState.DraggingHandle ||
                                     drawingState is com.bthr.backtest.util.DrawingState.DraggingDrawing
                    drawCustomTrendLine(this, s, e, isSelected, isDragging, density)
                } else {
                    // Use default drawing style for other tools
                    drawLine(d.color, s, e, d.strokeWidth, cap = StrokeCap.Round, pathEffect = pe)
                    if (isSelected) {
                        drawSelectionHandle(px1); drawSelectionHandle(px2)
                        drawLine(d.color.copy(alpha = 0.3f), s, e, d.strokeWidth + 6f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f)))
                    }
                }
            }

            // ── HorizontalLine ───────────────────────────────────────────────
            is com.bthr.backtest.model.Drawing.HorizontalLine -> {
                val y = mapper.priceToY(d.price)
                if (y < 0f || y > mapper.chartHeightPx) return@with
                drawLine(d.color, Offset(0f, y), Offset(mapper.chartWidthPx, y), d.strokeWidth, pathEffect = pe)
                // Price label on right
                val label = "%.5g".format(d.price)
                val style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                val result = textMeasurer.measure(label, style)
                val bgW = result.size.width + 8f; val bgH = result.size.height + 4f
                drawRect(d.color, Offset(mapper.chartWidthPx - bgW - 2f, y - bgH / 2f),
                    Size(bgW, bgH))
                drawText(result, topLeft = Offset(mapper.chartWidthPx - bgW + 2f, y - bgH / 2f + 2f))
                if (isSelected) drawSelectionHandle(Offset(mapper.chartWidthPx / 2f, y))
            }

            // ── HorizontalRay ────────────────────────────────────────────────
            is com.bthr.backtest.model.Drawing.HorizontalRay -> {
                val ax = mapper.candleIdxToX(d.anchor.candleIdx)
                val ay = mapper.priceToY(d.anchor.price)
                drawLine(d.color, Offset(ax, ay), Offset(mapper.chartWidthPx, ay), d.strokeWidth, pathEffect = pe)
                if (isSelected) drawSelectionHandle(Offset(ax, ay))
            }

            // ── VerticalLine ─────────────────────────────────────────────────
            is com.bthr.backtest.model.Drawing.VerticalLine -> {
                val x = mapper.candleIdxToX(d.candleIdx)
                drawLine(d.color, Offset(x, 0f), Offset(x, mapper.chartHeightPx), d.strokeWidth, pathEffect = pe)
                if (isSelected) drawSelectionHandle(Offset(x, mapper.chartHeightPx / 2f))
            }

            // ── CrossLine ────────────────────────────────────────────────────
            is com.bthr.backtest.model.Drawing.CrossLine -> {
                val px = mapper.anchorToPixel(d.anchor)
                drawLine(d.color, Offset(0f, px.y), Offset(mapper.chartWidthPx, px.y), d.strokeWidth, pathEffect = pe)
                drawLine(d.color, Offset(px.x, 0f), Offset(px.x, mapper.chartHeightPx), d.strokeWidth, pathEffect = pe)
                if (isSelected) drawSelectionHandle(px)
            }

            // ── ParallelChannel ──────────────────────────────────────────────
            is com.bthr.backtest.model.Drawing.ParallelChannel -> {
                val px1 = mapper.anchorToPixel(d.p1); val px2 = mapper.anchorToPixel(d.p2)
                val px3 = mapper.anchorToPixel(d.p3)
                val dy = px3.y - px2.y
                val px1b = Offset(px1.x, px1.y + dy); val px2b = Offset(px2.x, px2.y + dy)
                // Fill
                val path = Path().apply {
                    moveTo(px1.x, px1.y); lineTo(px2.x, px2.y)
                    lineTo(px2b.x, px2b.y); lineTo(px1b.x, px1b.y); close()
                }
                drawPath(path, d.color.copy(alpha = d.fillAlpha))
                // Lines
                drawLine(d.color, px1, px2, d.strokeWidth, pathEffect = pe)
                drawLine(d.color, px1b, px2b, d.strokeWidth, pathEffect = pe)
                // Middle dashed line
                val midPx1 = Offset(px1.x, (px1.y + px1b.y) / 2f)
                val midPx2 = Offset(px2.x, (px2.y + px2b.y) / 2f)
                drawLine(d.color.copy(alpha = 0.4f), midPx1, midPx2, d.strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
                if (isSelected) { drawSelectionHandle(px1); drawSelectionHandle(px2); drawSelectionHandle(px3) }
            }

            // ── FibRetracement ───────────────────────────────────────────────
            is com.bthr.backtest.model.Drawing.FibRetracement -> {
                val px1 = mapper.anchorToPixel(d.p1); val px2 = mapper.anchorToPixel(d.p2)
                val highPrice = maxOf(d.p1.price, d.p2.price)
                val lowPrice  = minOf(d.p1.price, d.p2.price)
                val priceRange = highPrice - lowPrice
                val xLeft  = minOf(px1.x, px2.x)
                val xRight = maxOf(px1.x, px2.x)
                d.levels.forEach { level ->
                    if (!level.visible) return@forEach
                    val levelPrice = lowPrice + priceRange * (1f - level.ratio)
                    val yLevel = mapper.priceToY(levelPrice)
                    if (yLevel !in 0f..mapper.chartHeightPx) return@forEach
                    drawLine(level.color.copy(alpha = 0.8f), Offset(xLeft, yLevel),
                        Offset(xRight, yLevel), d.strokeWidth)
                    // Label
                    val label = "${level.label}  (${String.format("%.2f", levelPrice)})"
                    val style = TextStyle(color = level.color, fontSize = 9.sp)
                    val result = textMeasurer.measure(label, style)
                    drawText(result, topLeft = Offset(xRight + 4f, yLevel - result.size.height / 2f))
                }
                drawLine(d.color.copy(alpha = 0.3f), px1, px2, 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
                if (isSelected) { drawSelectionHandle(px1); drawSelectionHandle(px2) }
            }

            // ── Rectangle ────────────────────────────────────────────────────
            is com.bthr.backtest.model.Drawing.Rectangle -> {
                val px1 = mapper.anchorToPixel(d.p1); val px2 = mapper.anchorToPixel(d.p2)
                val left   = minOf(px1.x, px2.x); val right  = maxOf(px1.x, px2.x)
                val top    = minOf(px1.y, px2.y); val bottom = maxOf(px1.y, px2.y)
                drawRect(d.color.copy(alpha = d.fillAlpha), Offset(left, top),
                    Size(right - left, bottom - top))
                drawRect(d.color, Offset(left, top), Size(right - left, bottom - top),
                    style = Stroke(width = d.strokeWidth, pathEffect = pe))
                if (isSelected) {
                    drawSelectionHandle(px1); drawSelectionHandle(px2)
                    drawSelectionHandle(Offset(left, bottom)); drawSelectionHandle(Offset(right, top))
                }
            }

            // ── Circle ───────────────────────────────────────────────────────
            is com.bthr.backtest.model.Drawing.Circle -> {
                val center = mapper.anchorToPixel(d.center)
                val radiusPt = mapper.anchorToPixel(d.radiusPoint)
                val r = sqrt((radiusPt.x - center.x).pow(2) + (radiusPt.y - center.y).pow(2))
                drawCircle(d.color.copy(alpha = d.fillAlpha), r, center)
                drawCircle(d.color, r, center, style = Stroke(width = d.strokeWidth, pathEffect = pe))
                if (isSelected) { drawSelectionHandle(center); drawSelectionHandle(radiusPt) }
            }

            // ── Arrow ─────────────────────────────────────────────────────────
            is com.bthr.backtest.model.Drawing.Arrow -> {
                val px1 = mapper.anchorToPixel(d.p1); val px2 = mapper.anchorToPixel(d.p2)
                
                // Determine line style based on lineStyle property (comme les lignes de tendance normales)
                val pathEffect = when (d.lineStyle) {
                    LineStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(4f * d.strokeWidth, 4f * d.strokeWidth))
                    LineStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(0f, 4f * d.strokeWidth))
                    else -> null
                }
                
                drawLine(d.color, px1, px2, d.strokeWidth, cap = StrokeCap.Round, pathEffect = pathEffect)
                
                // Arrowhead - adapté au thickness
                val dx = px2.x - px1.x; val dy = px2.y - px1.y
                val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
                val ux = dx / len; val uy = dy / len
                val headLen = (d.strokeWidth * 6f).coerceAtLeast(8f)  // Taille proportionnelle au thickness
                val headWidth = headLen * 0.6f  // Ratio standard pour une pointe bien visible
                val bx = px2.x - ux * headLen; val by = px2.y - uy * headLen
                
                // Draw only the two sides of the triangle arrowhead (no base line)
                drawLine(d.color, Offset(px2.x, px2.y), Offset(bx - uy * headWidth, by + ux * headWidth), d.strokeWidth, cap = StrokeCap.Round)
                drawLine(d.color, Offset(px2.x, px2.y), Offset(bx + uy * headWidth, by - ux * headWidth), d.strokeWidth, cap = StrokeCap.Round)
                if (isSelected) { drawSelectionHandle(px1); drawSelectionHandle(px2) }
            }

            // ── Polyline / Brush / Path ──────────────────────────────────────
            is com.bthr.backtest.model.Drawing.Polyline -> {
                if (d.points.size < 2) return@with
                val path = Path()
                d.points.mapIndexed { i, a ->
                    val px = mapper.anchorToPixel(a)
                    if (i == 0) path.moveTo(px.x, px.y) else path.lineTo(px.x, px.y)
                }
                drawPath(path, d.color, style = Stroke(width = d.strokeWidth,
                    cap = StrokeCap.Round, pathEffect = pe))
                if (isSelected) {
                    val f = mapper.anchorToPixel(d.points.first())
                    val l = mapper.anchorToPixel(d.points.last())
                    drawSelectionHandle(f); drawSelectionHandle(l)
                }
            }

            // ── TextLabel ────────────────────────────────────────────────────
            is com.bthr.backtest.model.Drawing.TextLabel -> {
                val px = mapper.anchorToPixel(d.anchor)
                val style = TextStyle(color = d.color, fontSize = d.fontSize.sp, fontWeight = FontWeight.Medium)
                val result = textMeasurer.measure(d.text, style)
                val bgPad = 6f
                val bgW = result.size.width + bgPad * 2; val bgH = result.size.height + bgPad
                drawRoundRect(Color.Black.copy(alpha = 0.55f), Offset(px.x - bgPad, px.y - bgH + bgPad),
                    Size(bgW, bgH), androidx.compose.ui.geometry.CornerRadius(4f))
                drawText(result, topLeft = Offset(px.x, px.y - result.size.height))
                if (isSelected) drawSelectionHandle(px)
            }

            // ── Measure ──────────────────────────────────────────────────────
            is com.bthr.backtest.model.Drawing.Measure -> {
                val px1 = mapper.anchorToPixel(d.p1); val px2 = mapper.anchorToPixel(d.p2)
                val left   = minOf(px1.x, px2.x); val right  = maxOf(px1.x, px2.x)
                val top    = minOf(px1.y, px2.y); val bottom = maxOf(px1.y, px2.y)
                val dPrice = d.p2.price - d.p1.price
                val pct    = if (d.p1.price != 0f) dPrice / d.p1.price * 100f else 0f
                val bars   = kotlin.math.abs(d.p2.candleIdx - d.p1.candleIdx)
                val fillColor = if (dPrice >= 0f) Color(0xFF4CAF50) else Color(0xFFF44336)
                
                // Calculer la durée en format "Xj Yh" en utilisant le timeframe
                val totalMinutes = bars * timeframe.minutes
                val days = totalMinutes / 1440
                val hours = (totalMinutes % 1440) / 60
                val remainingMinutes = totalMinutes % 60
                
                val duration = when {
                    days > 0 && hours > 0 && remainingMinutes > 0 -> "${days}j ${hours}h ${remainingMinutes}m"
                    days > 0 && hours > 0 -> "${days}j ${hours}h"
                    days > 0 && remainingMinutes > 0 -> "${days}j ${remainingMinutes}m"
                    days > 0 -> "${days}j"
                    hours > 0 && remainingMinutes > 0 -> "${hours}h ${remainingMinutes}m"
                    hours > 0 -> "${hours}h"
                    remainingMinutes > 0 -> "${remainingMinutes}m"
                    else -> "0m"
                }
                
                drawRect(fillColor.copy(alpha = 0.12f), Offset(left, top), Size(right - left, bottom - top))
                drawRect(fillColor.copy(alpha = 0.7f), Offset(left, top), Size(right - left, bottom - top),
                    style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 3f))))
                
                val sign   = if (dPrice >= 0f) "+" else ""
                
                // Format d'affichage sur deux lignes comme demandé
                val priceLabel = "${String.format("%.2f", dPrice)} ($sign${String.format("%.2f", pct)}%)"
                val infoLabel = "$bars barres, $duration"
                
                
                val priceStyle = TextStyle(color = fillColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                val infoStyle = TextStyle(color = fillColor, fontSize = 11.sp, fontWeight = FontWeight.Normal)
                
                val priceResult = textMeasurer.measure(priceLabel, priceStyle)
                val infoResult = textMeasurer.measure(infoLabel, infoStyle)
                
                val cx = (left + right) / 2f; val cy = (top + bottom) / 2f
                val totalHeight = priceResult.size.height + infoResult.size.height + 8f
                val totalWidth = maxOf(priceResult.size.width, infoResult.size.width)
                
                // Dessiner le fond pour les deux lignes
                drawRoundRect(Color.Black.copy(alpha = 0.6f),
                    Offset(cx - totalWidth / 2f - 6f, cy - totalHeight / 2f - 3f),
                    Size(totalWidth + 12f, totalHeight + 6f),
                    androidx.compose.ui.geometry.CornerRadius(4f))
                
                // Dessiner la première ligne (prix et pourcentage)
                drawText(priceResult, topLeft = Offset(cx - priceResult.size.width / 2f, cy - totalHeight / 2f))
                // Dessiner la deuxième ligne (barres et durée)
                drawText(infoResult, topLeft = Offset(cx - infoResult.size.width / 2f, cy - totalHeight / 2f + priceResult.size.height + 4f))
                if (isSelected) { drawSelectionHandle(px1); drawSelectionHandle(px2) }
            }

            // ── LongPosition ─────────────────────────────────────────────────
            is com.bthr.backtest.model.Drawing.LongPosition -> {
                val px = mapper.anchorToPixel(d.entryPoint)
                val stopY   = mapper.priceToY(d.stopPrice)
                val targetY = mapper.priceToY(d.targetPrice)
                val left    = px.x; val right = mapper.chartWidthPx
                val rr = if (d.stopPrice != d.entryPoint.price)
                    kotlin.math.abs((d.targetPrice - d.entryPoint.price) / (d.entryPoint.price - d.stopPrice)) else 0f
                // Profit zone (green)
                drawRect(Color(0xFF4CAF50).copy(alpha = 0.18f), Offset(left, targetY), Size(right - left, px.y - targetY))
                // Loss zone (red)
                drawRect(Color(0xFFF44336).copy(alpha = 0.18f), Offset(left, px.y), Size(right - left, stopY - px.y))
                // Lines
                drawLine(Color.White, Offset(left, px.y), Offset(right, px.y), 1.5f)
                drawLine(Color(0xFF4CAF50), Offset(left, targetY), Offset(right, targetY), 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 3f)))
                drawLine(Color(0xFFF44336), Offset(left, stopY), Offset(right, stopY), 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 3f)))
                // Labels
                val entryLabel  = "E: ${String.format("%.2f", d.entryPoint.price)}"
                val targetLabel = "T: ${String.format("%.2f", d.targetPrice)}  R:R ${String.format("%.1f", rr)}"
                val stopLabel   = "S: ${String.format("%.2f", d.stopPrice)}"
                listOf(
                    entryLabel  to Pair(px.y, Color.White),
                    targetLabel to Pair(targetY, Color(0xFF4CAF50)),
                    stopLabel   to Pair(stopY, Color(0xFFF44336))
                ).forEach { (lbl, pair) ->
                    val style = TextStyle(color = pair.second, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    val r2 = textMeasurer.measure(lbl, style)
                    drawText(r2, topLeft = Offset(right - r2.size.width - 4f, pair.first - r2.size.height - 2f))
                }
                if (isSelected) drawSelectionHandle(px)
            }

            // ── ShortPosition ────────────────────────────────────────────────
            is com.bthr.backtest.model.Drawing.ShortPosition -> {
                val px = mapper.anchorToPixel(d.entryPoint)
                val stopY   = mapper.priceToY(d.stopPrice)
                val targetY = mapper.priceToY(d.targetPrice)
                val left    = px.x; val right = mapper.chartWidthPx
                val rr = if (d.stopPrice != d.entryPoint.price)
                    kotlin.math.abs((d.entryPoint.price - d.targetPrice) / (d.stopPrice - d.entryPoint.price)) else 0f
                drawRect(Color(0xFFF44336).copy(alpha = 0.18f), Offset(left, px.y), Size(right - left, stopY - px.y))
                drawRect(Color(0xFF4CAF50).copy(alpha = 0.18f), Offset(left, targetY), Size(right - left, px.y - targetY))
                drawLine(Color.White, Offset(left, px.y), Offset(right, px.y), 1.5f)
                drawLine(Color(0xFFF44336), Offset(left, stopY), Offset(right, stopY), 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 3f)))
                drawLine(Color(0xFF4CAF50), Offset(left, targetY), Offset(right, targetY), 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 3f)))
                val entryLabel  = "E: ${String.format("%.2f", d.entryPoint.price)}"
                val targetLabel = "T: ${String.format("%.2f", d.targetPrice)}  R:R ${String.format("%.1f", rr)}"
                val stopLabel   = "S: ${String.format("%.2f", d.stopPrice)}"
                listOf(
                    entryLabel  to Pair(px.y, Color.White),
                    targetLabel to Pair(targetY, Color(0xFF4CAF50)),
                    stopLabel   to Pair(stopY, Color(0xFFF44336))
                ).forEach { (lbl, pair) ->
                    val style = TextStyle(color = pair.second, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    val r2 = textMeasurer.measure(lbl, style)
                    drawText(r2, topLeft = Offset(right - r2.size.width - 4f, pair.first - r2.size.height - 2f))
                }
                if (isSelected) drawSelectionHandle(px)
            }

            else -> {}
        }
    }

    // ── Selection handle dot ───────────────────────────────────────────────

    private fun DrawScope.drawSelectionHandle(center: Offset) {
        drawCircle(Color.White, 6f, center)
        drawCircle(Color(0xFF2962FF), 4f, center)
    }

    // ── Custom trend line drawing with red color and bubbles ───────────────

    fun drawCustomTrendLine(
        drawScope: DrawScope,
        p1: Offset,
        p2: Offset,
        isSelected: Boolean,
        isDragging: Boolean = false,
        density: androidx.compose.ui.unit.Density
    ) = with(drawScope) {
        val lineColor = Color(0xFFFF0000)
        val strokeWidth = if (isSelected) 2.5f else 1.5f

        // Calculate line endpoints (stop before bubbles if selected)
        val bubbleRadius = with(density) { 10.dp.toPx() }
        val (lineStart, lineEnd) = if (isSelected) {
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val length = sqrt(dx * dx + dy * dy)

            if (length > bubbleRadius * 2) {
                val scale = (length - bubbleRadius * 2) / length
                val adjustedStart = Offset(
                    p1.x + dx * (1 - scale) / 2,
                    p1.y + dy * (1 - scale) / 2
                )
                val adjustedEnd = Offset(
                    p2.x - dx * (1 - scale) / 2,
                    p2.y - dy * (1 - scale) / 2
                )
                adjustedStart to adjustedEnd
            } else {
                p1 to p2
            }
        } else {
            p1 to p2
        }

        // Draw line with anti-aliasing
        drawLine(
            color = lineColor,
            start = lineStart,
            end = lineEnd,
            strokeWidth = strokeWidth.dp.toPx(),
            pathEffect = if (!isSelected) PathEffect.dashPathEffect(floatArrayOf(4f, 2f)) else null
        )

        // Draw selection endpoints if selected (bubbles/circles)
        if (isSelected && !isDragging) {
            val borderColor = Color(0xFF000000)
            val fillColor = Color(0xFFFFFFFF)

            // Start endpoint
            drawCircle(color = fillColor, radius = bubbleRadius, center = p1)
            drawCircle(
                color = borderColor,
                radius = bubbleRadius,
                center = p1,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f.dp.toPx())
            )

            // End endpoint
            drawCircle(color = fillColor, radius = bubbleRadius, center = p2)
            drawCircle(
                color = borderColor,
                radius = bubbleRadius,
                center = p2,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f.dp.toPx())
            )
        }
    }

    fun drawTrendLinePreview(
        drawScope: DrawScope,
        startScreen: Offset,
        endScreen: Offset,
        density: androidx.compose.ui.unit.Density
    ) = with(drawScope) {
        // Preview line: dashed red line
        drawLine(
            color = Color(0xFFFF0000).copy(alpha = 0.7f),
            start = startScreen,
            end = endScreen,
            strokeWidth = 2f.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
        )

        // Draw bubble at first point (already placed)
        val bubbleRadius = with(density) { 10.dp.toPx() }
        val borderColor = Color(0xFF000000)
        val fillColor = Color(0xFFFFFFFF)
        drawCircle(color = fillColor, radius = bubbleRadius, center = startScreen)
        drawCircle(
            color = borderColor,
            radius = bubbleRadius,
            center = startScreen,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f.dp.toPx())
        )

        // Draw blue dot at crosshair center (second point not yet created)
        val dotRadius = with(density) { 3.dp.toPx() }
        drawCircle(color = Color(0xFF2196F3), radius = dotRadius, center = endScreen)
    }

    fun drawPlacementDot(
        drawScope: DrawScope,
        position: Offset,
        density: androidx.compose.ui.unit.Density
    ) = with(drawScope) {
        val dotRadius = with(density) { 3.dp.toPx() }
        drawCircle(color = Color(0xFF2196F3), radius = dotRadius, center = position)
    }

    // ── Placement preview (ghost while drawing) ────────────────────────────

    private fun drawPlacementPreview(
        drawScope: DrawScope,
        state: DrawingState,
        mapper: DrawingCoordinateMapper,
        density: androidx.compose.ui.unit.Density,
        activeTool: com.bthr.backtest.model.DrawingTool
    ) = with(drawScope) {
        val previewColor = Color(0xFF1E88E5)
        val ghostColor   = Color(0xFFE91E63).copy(alpha = 0.7f)

        fun drawCrosshair(px: Offset) {
            drawLine(previewColor, Offset(px.x, 0f), Offset(px.x, mapper.chartHeightPx), 1f)
            drawLine(previewColor, Offset(0f, px.y), Offset(mapper.chartWidthPx, px.y), 1f)
            drawCircle(previewColor, 5f, px)
        }

        when (state) {
            is DrawingState.PlacingFirstPoint -> {
                drawCrosshair(state.cursorPx)
                // Draw blue dot at crosshair center when placing first point
                drawPlacementDot(this, state.cursorPx, density)
            }
            is DrawingState.PlacingSecondPoint -> {
                val p1px = mapper.anchorToPixel(state.p1)
                drawCrosshair(state.cursorPx)
                // Use custom trend line preview ONLY when TREND_LINE tool is active
                if (activeTool == com.bthr.backtest.model.DrawingTool.TREND_LINE) {
                    drawTrendLinePreview(this, p1px, state.cursorPx, density)
                } else {
                    drawCircle(previewColor, 6f, p1px)
                    drawLine(ghostColor, p1px, state.cursorPx, 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f)))
                }
            }
            is DrawingState.PlacingThirdPoint -> {
                val p1px = mapper.anchorToPixel(state.p1)
                val p2px = mapper.anchorToPixel(state.p2)
                drawCrosshair(state.cursorPx)
                drawCircle(previewColor, 6f, p1px); drawCircle(previewColor, 6f, p2px)
                drawLine(ghostColor, p1px, p2px, 2f)
                // Third point preview line parallel
                val dy = state.cursorPx.y - p2px.y
                drawLine(ghostColor.copy(alpha = 0.5f),
                    Offset(p1px.x, p1px.y + dy), Offset(p2px.x, p2px.y + dy), 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
            }
            else -> {}
        }
    }
}
