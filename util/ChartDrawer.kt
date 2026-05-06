package com.bthr.backtest.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bthr.backtest.model.Candle
import com.bthr.backtest.model.Indicator
import com.bthr.backtest.model.ChartSettings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.*

object ChartDrawer {

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

    fun drawGridAndLabels(
        drawScope: DrawScope,
        textMeasurer: TextMeasurer,
        chartW: Float,
        priceW: Float,
        mainH: Float,
        range: Float,
        minP: Float,
        normalizeY: (Float) -> Float,
        showGrid: Boolean,
        gridColor: Color,
        textStyle: TextStyle
    ) = with(drawScope) {
        if (range <= 0 || chartW <= 0) return@with
        val step = ChartUtils.calculatePriceStep(range)
        var pLabel = ceil(minP / step) * step
        var safety = 0
        
        val labelStyle = textStyle.copy(fontWeight = FontWeight.Normal, fontSize = 10.sp)
        
        while (pLabel <= minP + range && safety < 100) {
            val y = normalizeY(pLabel)
            if (y in 0f..mainH) {
                if (showGrid) {
                    // Complete horizontal grid line spanning full width
                    drawLine(gridColor.copy(alpha = 0.2f), Offset(0f, y), Offset(chartW + priceW, y), 1f)
                }
                // Price axis tick
                drawLine(gridColor.copy(alpha = 0.4f), Offset(chartW, y), Offset(chartW + 4.dp.toPx(), y), 1f)
                
                safeDrawText(
                    textMeasurer,
                    ChartUtils.formatPrice(pLabel),
                    Offset(chartW + 8.dp.toPx(), y - 7.dp.toPx()),
                    labelStyle
                )
            }
            pLabel += step; safety++
        }
    }

    fun drawCandles(
        drawScope: DrawScope,
        visibleCandles: List<Candle>,
        startIdx: Int,
        totalCandles: Int,
        scrollOffset: Float,
        candleW: Float,
        bodyW: Float,
        chartW: Float,
        mainH: Float,
        volumeIndicator: Indicator.Volume?,
        settings: ChartSettings,
        normalizeY: (Float) -> Float
    ) = with(drawScope) {
        if (visibleCandles.isEmpty() || chartW <= 0) return@with
        val maxVol = visibleCandles.maxOf { it.volume }.coerceAtLeast(1f)
        visibleCandles.forEachIndexed { index, candle ->
            val x = chartW - (candleW / 2f) - ((totalCandles - 1 - (startIdx + index) - scrollOffset) * candleW)
            if (x in -candleW..chartW + candleW) {
                val isUp = candle.close >= candle.open
                val color = if (isUp) settings.upColor else settings.downColor
                val highY = normalizeY(candle.high)
                val lowY = normalizeY(candle.low)
                val bTop = min(normalizeY(candle.open), normalizeY(candle.close))
                val bBottom = max(normalizeY(candle.open), normalizeY(candle.close))

                if (volumeIndicator?.isVisible == true) {
                    val volH = (candle.volume / maxVol) * (mainH * 0.12f)
                    drawRect(
                        if (isUp) settings.upColor.copy(0.15f) else settings.downColor.copy(0.15f),
                        Offset(x - bodyW / 2, mainH - volH),
                        Size(bodyW, volH)
                    )
                }
                
                if (settings.wickEnabled) {
                    drawLine(color, Offset(x, highY), Offset(x, bTop), 1f)
                    drawLine(color, Offset(x, bBottom), Offset(x, lowY), 1f)
                }
                if (settings.bodyEnabled) {
                    drawRect(color, Offset(x - bodyW / 2, bTop), Size(bodyW, max(bBottom - bTop, 0.5f)))
                }
                if (settings.bordersEnabled) {
                    drawRect(color, Offset(x - bodyW / 2, bTop), Size(bodyW, max(bBottom - bTop, 0.5f)), style = Stroke(1f))
                }
            }
        }
    }

    fun drawOverlayIndicators(
        drawScope: DrawScope,
        overlayIndicators: List<Indicator>,
        smaValues: Map<String, List<Float?>>,
        emaValues: Map<String, List<Float?>>,
        bbValues: Map<String, IndicatorCalculators.BollingerResult>,
        startIdx: Int,
        endIdx: Int,
        scrollOffset: Float,
        candleW: Float,
        chartW: Float,
        normalizeY: (Float) -> Float
    ) {
        overlayIndicators.forEach { ind ->
            if (!ind.isVisible) return@forEach
            when (ind) {
                is Indicator.SMA -> smaValues[ind.id]?.let { ChartUtils.drawIndicatorPath(drawScope, it, startIdx, endIdx, scrollOffset, candleW, chartW, ind.color, normalizeY) }
                is Indicator.EMA -> emaValues[ind.id]?.let { ChartUtils.drawIndicatorPath(drawScope, it, startIdx, endIdx, scrollOffset, candleW, chartW, ind.color, normalizeY) }
                is Indicator.BollingerBands -> bbValues[ind.id]?.let { bb ->
                    ChartUtils.drawIndicatorPath(drawScope, bb.upper, startIdx, endIdx, scrollOffset, candleW, chartW, ind.upperColor, normalizeY)
                    ChartUtils.drawIndicatorPath(drawScope, bb.middle, startIdx, endIdx, scrollOffset, candleW, chartW, ind.middleColor, normalizeY)
                    ChartUtils.drawIndicatorPath(drawScope, bb.lower, startIdx, endIdx, scrollOffset, candleW, chartW, ind.lowerColor, normalizeY)
                }
                else -> {}
            }
        }
    }

    fun drawBottomIndicators(
        drawScope: DrawScope,
        visibleBottom: List<Indicator>,
        indicatorHeights: Map<String, Float>,
        defaultHeightPx: Float,
        rsiValues: Map<String, List<Float?>>,
        rsiMaValues: Map<String, List<Float?>>,
        macdValues: Map<String, IndicatorCalculators.MACDResult>,
        indicatorRanges: Map<String, Pair<Float, Float>>,
        visibleCandles: List<Candle>,
        totalCandles: Int,
        startIdx: Int,
        endIdx: Int,
        scrollOffset: Float,
        candleW: Float,
        bodyW: Float,
        chartW: Float,
        priceW: Float,
        mainH: Float,
        gridColor: Color,
        textMeasurer: TextMeasurer,
        textStyle: TextStyle,
        crosshairTextStyle: TextStyle,
        draggingSeparatorIdx: Int,
        dragArea: Int
    ) = with(drawScope) {
        var currentY = mainH
        visibleBottom.forEachIndexed { idx, ind ->
            val h = (indicatorHeights[ind.id] ?: defaultHeightPx).coerceAtLeast(1f)
            val range = indicatorRanges[ind.id] ?: Pair(0f, 100f)
            val minV = range.first; val maxV = range.second
            val rV = (maxV - minV).coerceAtLeast(0.001f)
            val normIndY: (Float) -> Float = { currentY + h - ((it - minV) / rV * h) }

            when (ind) {
                is Indicator.RSI -> {
                    if (ind.showLabelsOnPriceScale) {
                        val levels = mutableListOf<Int>()
                        if (ind.upperLevelVisible) levels.add(ind.upperLevel)
                        if (ind.middleLevelVisible) levels.add(ind.middleLevel)
                        if (ind.lowerLevelVisible) levels.add(ind.lowerLevel)
                        
                        levels.forEach { level ->
                            val y = normIndY(level.toFloat())
                            if (y in currentY..currentY + h) {
                                drawLine(gridColor.copy(alpha = 0.3f), Offset(chartW, y), Offset(chartW + 4.dp.toPx(), y), 1f)
                                safeDrawText(textMeasurer, level.toString(), Offset(chartW + 8.dp.toPx(), y - 6.dp.toPx()), textStyle)
                            }
                        }
                        rsiValues[ind.id]?.lastOrNull { it != null }?.let { lastV ->
                            val y = normIndY(lastV)
                            if (y in currentY..currentY + h) {
                                drawRect(ind.color, Offset(chartW, y - 8.dp.toPx()), Size(priceW, 16.dp.toPx()))
                                safeDrawText(textMeasurer, String.format(Locale.getDefault(), "%.${ind.precision}f", lastV), Offset(chartW + 6.dp.toPx(), y - 7.dp.toPx()), crosshairTextStyle)
                            }
                        }
                    }
                }
                is Indicator.MACD -> macdValues[ind.id]?.let { res ->
                    val step = ChartUtils.calculatePriceStep(rV)
                    var pL = ceil(minV / step) * step
                    var safety = 0
                    while (pL <= maxV && safety < 50) {
                        val y = normIndY(pL)
                        if (y in currentY..currentY + h) {
                            drawLine(gridColor.copy(alpha = 0.08f), Offset(0f, y), Offset(chartW, y), 1f)
                            drawLine(gridColor.copy(alpha = 0.3f), Offset(chartW, y), Offset(chartW + 4.dp.toPx(), y), 1f)
                            safeDrawText(textMeasurer, String.format(Locale.getDefault(), "%.2f", pL), Offset(chartW + 8.dp.toPx(), y - 6.dp.toPx()), textStyle)
                        }
                        pL += step; safety++
                    }
                }
                else -> {}
            }

            clipRect(top = currentY, right = chartW, bottom = currentY + h) {
                when (ind) {
                    is Indicator.RSI -> rsiValues[ind.id]?.let { vals ->
                        if (ind.backgroundVisible) {
                            val yTop = normIndY(ind.upperLevel.toFloat())
                            val yBottom = normIndY(ind.lowerLevel.toFloat())
                            drawRect(
                                color = ind.backgroundColor,
                                topLeft = Offset(0f, min(yTop, yBottom)),
                                size = Size(chartW, abs(yTop - yBottom))
                            )
                        }
                        val levels = mutableListOf<Pair<Int, Color>>()
                        if (ind.upperLevelVisible) levels.add(ind.upperLevel to ind.upperLevelColor)
                        if (ind.middleLevelVisible) levels.add(ind.middleLevel to ind.middleLevelColor)
                        if (ind.lowerLevelVisible) levels.add(ind.lowerLevel to ind.lowerLevelColor)
                        levels.forEach { (level, color) ->
                            val y = normIndY(level.toFloat())
                            drawLine(
                                color = color.copy(alpha = 0.3f),
                                start = Offset(0f, y),
                                end = Offset(chartW, y),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                            )
                        }
                        if (ind.showRsi) ChartUtils.drawIndicatorPath(this, vals, startIdx, endIdx, scrollOffset, candleW, chartW, ind.color, normIndY)
                        if (ind.showMa) rsiMaValues[ind.id]?.let { ChartUtils.drawIndicatorPath(this, it, startIdx, endIdx, scrollOffset, candleW, chartW, ind.maColor, normIndY) }
                    }
                    is Indicator.MACD -> macdValues[ind.id]?.let { res ->
                        val zeroY = normIndY(0f)
                        visibleCandles.forEachIndexed { i, _ ->
                            val cIdx = startIdx + i
                            if (cIdx < totalCandles) {
                                val x = chartW - (candleW / 2f) - ((totalCandles - 1 - cIdx - scrollOffset) * candleW)
                                val hv = res.histogram.getOrNull(cIdx) ?: return@forEachIndexed
                                drawRect(
                                    if (hv >= 0) ind.histColorUp.copy(0.5f) else ind.histColorDown.copy(0.5f),
                                    Offset(x - bodyW / 2, min(zeroY, normIndY(hv))),
                                    Size(bodyW, abs(zeroY - normIndY(hv)))
                                )
                            }
                        }
                        ChartUtils.drawIndicatorPath(this, res.macdLine, startIdx, endIdx, scrollOffset, candleW, chartW, ind.macdColor, normIndY)
                        ChartUtils.drawIndicatorPath(this, res.signalLine, startIdx, endIdx, scrollOffset, candleW, chartW, ind.signalColor, normIndY)
                    }
                    else -> {}
                }
            }
            val isDrag = draggingSeparatorIdx == idx && dragArea == 4
            if (isDrag) drawRect(Color(0xFF2962FF).copy(0.2f), Offset(0f, currentY - 10.dp.toPx()), Size(chartW, 20.dp.toPx()))
            drawLine(if (isDrag) Color(0xFF2962FF) else gridColor.copy(0.15f), Offset(0f, currentY), Offset(chartW, currentY), if (isDrag) 2.dp.toPx() else 1f)
            currentY += h
        }
    }

    fun drawCrosshair(
        drawScope: DrawScope,
        p: Offset,
        chartW: Float,
        fullH: Float,
        mainH: Float,
        priceW: Float,
        timeH: Float,
        crosshairColor: Color,
        crosshairTextStyle: TextStyle,
        textMeasurer: TextMeasurer,
        allCandles: List<Candle>,
        scrollOffset: Float,
        candleW: Float,
        crosshairTimeFormatter: SimpleDateFormat,
        denormalizeY: (Float) -> Float,
        bottomIndicators: List<Indicator>,
        indicatorHeights: Map<String, Float>,
        indicatorRanges: Map<String, Pair<Float, Float>>,
        defaultHeightPx: Float,
        labelBgColor: Color
    ) = with(drawScope) {
        val crosshairAlpha = 0.8f
        val crosshairStroke = 0.5f
        val plotsH = fullH - timeH

        clipRect(right = chartW + priceW, bottom = fullH) {
            // Vertical line drawn later (snapped to candle center)
            drawLine(
                crosshairColor.copy(alpha = crosshairAlpha),
                Offset(0f, p.y),
                Offset(chartW + priceW, p.y),
                crosshairStroke,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            )
        }

        // Time label on the time bar
        // p.x brut (sans clamping supérieur) → la ligne suit librement le doigt vers la droite
        val lineX = p.x.coerceAtLeast(0f)
        val rawIdxF = allCandles.size - 1 - scrollOffset - (chartW - lineX - candleW / 2f) / candleW
        val idx = round(rawIdxF).toInt()

        // Determine timestamp: real candle or extrapolated virtual candle
        val tfMs: Long = if (allCandles.size >= 2) allCandles.last().timestamp - allCandles[allCandles.size - 2].timestamp else 60_000L
        val timestamp: Long? = when {
            idx in allCandles.indices -> allCandles[idx].timestamp
            idx >= allCandles.size && allCandles.isNotEmpty() -> {
                val extraSteps = idx - (allCandles.size - 1)
                allCandles.last().timestamp + extraSteps * tfMs
            }
            idx < 0 && allCandles.isNotEmpty() -> allCandles[0].timestamp + idx * tfMs
            else -> null
        }

        // Ligne verticale dans la zone chart
        clipRect(right = chartW) {
            drawLine(
                crosshairColor.copy(alpha = crosshairAlpha),
                Offset(lineX, 0f),
                Offset(lineX, plotsH + timeH),
                crosshairStroke,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            )
        }

        if (timestamp != null) {
            val dStr = crosshairTimeFormatter.format(Date(timestamp))
            val layout = textMeasurer.measure(dStr, crosshairTextStyle.copy(fontWeight = FontWeight.Bold))
            val rectW = layout.size.width + 16.dp.toPx()
            val rectH = timeH - 4.dp.toPx()
            val lX = (lineX - rectW / 2f).coerceIn(0f, chartW - rectW)

            // Prominent Opaque background
            drawRect(labelBgColor.copy(alpha = 1f), Offset(lX, plotsH + 2.dp.toPx()), Size(rectW, rectH))
            drawRect(crosshairColor.copy(alpha = 0.6f), Offset(lX, plotsH + 2.dp.toPx()), Size(rectW, rectH), style = Stroke(1.5.dp.toPx()))
            
            safeDrawText(textMeasurer, dStr, Offset(lX + 8.dp.toPx(), plotsH + (timeH - layout.size.height) / 2f), crosshairTextStyle.copy(fontWeight = FontWeight.Bold))
        }

        if (p.y <= mainH) {
            val pTxt = ChartUtils.formatPrice(denormalizeY(p.y))
            val layout = textMeasurer.measure(pTxt, crosshairTextStyle.copy(fontWeight = FontWeight.Bold))
            val rectW = priceW + 20.dp.toPx()
            val rectH = layout.size.height.toFloat() + 10.dp.toPx()
            val lY = (p.y - rectH / 2f).coerceIn(0f, mainH - rectH)
            
            drawRect(labelBgColor.copy(alpha = 1f), Offset(chartW, lY), Size(rectW, rectH))
            drawRect(crosshairColor.copy(alpha = 0.6f), Offset(chartW, lY), Size(rectW, rectH), style = Stroke(1.5.dp.toPx()))
            
            safeDrawText(textMeasurer, pTxt, Offset(chartW + 8.dp.toPx(), lY + 5.dp.toPx()), crosshairTextStyle.copy(fontWeight = FontWeight.Bold))
        } else {
            var currentY = mainH
            val visibleBottom = bottomIndicators.filter { it.isVisible }
            for (ind in visibleBottom) {
                val h = (indicatorHeights[ind.id] ?: defaultHeightPx).coerceAtLeast(1f)
                if (p.y in currentY..currentY + h) {
                    val range = indicatorRanges[ind.id] ?: Pair(0f, 100f)
                    val minV = range.first; val maxV = range.second
                    val valAtY = maxV - ((p.y - currentY) / h * (maxV - minV))
                    
                    val pTxt = String.format(Locale.getDefault(), "%.2f", valAtY)
                    val layout = textMeasurer.measure(pTxt, crosshairTextStyle.copy(fontWeight = FontWeight.Bold))
                    val rectW = priceW + 20.dp.toPx()
                    val rectH = layout.size.height.toFloat() + 10.dp.toPx()
                    val lY = (p.y - rectH / 2f).coerceIn(currentY, currentY + h - rectH)
                    
                    drawRect(labelBgColor.copy(alpha = 1f), Offset(chartW, lY), Size(rectW, rectH))
                    drawRect(crosshairColor.copy(alpha = 0.6f), Offset(chartW, lY), Size(rectW, rectH), style = Stroke(1.5.dp.toPx()))
                    
                    safeDrawText(textMeasurer, pTxt, Offset(chartW + 8.dp.toPx(), lY + 5.dp.toPx()), crosshairTextStyle.copy(fontWeight = FontWeight.Bold))
                    break
                }
                currentY += h
            }
        }
    }

    fun drawLastPriceLabel(
        drawScope: DrawScope,
        lastCandle: Candle,
        chartW: Float,
        mainH: Float,
        priceW: Float,
        crosshairTextStyle: TextStyle,
        textMeasurer: TextMeasurer,
        normalizeY: (Float) -> Float
    ) = with(drawScope) {
        val lastY = normalizeY(lastCandle.close)
        if (lastY in 0f..mainH) {
            val isUp = lastCandle.close >= lastCandle.open
            val color = if (isUp) Color(0xFF26A69A) else Color(0xFFEF5350)
            val pTxt = ChartUtils.formatPrice(lastCandle.close)
            val layout = textMeasurer.measure(pTxt, crosshairTextStyle.copy(fontWeight = FontWeight.Bold))
            
            drawRect(color, Offset(chartW, lastY - layout.size.height / 2f - 4.dp.toPx()), Size(priceW + 20.dp.toPx(), layout.size.height.toFloat() + 8.dp.toPx()))
            safeDrawText(textMeasurer, pTxt, Offset(chartW + 8.dp.toPx(), lastY - layout.size.height / 2f), crosshairTextStyle)
        }
    }

    fun drawTimeLabels(
        drawScope: DrawScope,
        visibleCandles: List<Candle>,
        startIdx: Int,
        totalCandles: Int,
        scrollOffset: Float,
        candleW: Float,
        chartW: Float,
        plotsH: Float,
        displayCount: Float,
        timeFormatter: SimpleDateFormat,
        textStyle: TextStyle,
        textMeasurer: TextMeasurer,
        labelBgColor: Color
    ) = with(drawScope) {
        if (visibleCandles.isEmpty() || chartW <= 0) return@with

        val firstT = visibleCandles.first().timestamp
        val lastT = visibleCandles.last().timestamp
        val timeRangeMs = (lastT - firstT).coerceAtLeast(1L)
        val pixelsPerMs = chartW / timeRangeMs
        val targetSpacingPx = 100f 
        val targetStepMs = targetSpacingPx / pixelsPerMs

        val steps = listOf(
            60 * 1000L, 5 * 60 * 1000L, 15 * 60 * 1000L, 30 * 60 * 1000L,
            3600000L, 4 * 3600000L, 12 * 3600000L,
            86400000L, 2 * 86400000L, 7 * 86400000L,
            30 * 86400000L, 365 * 86400000L
        )

        val stepMs = steps.find { it >= targetStepMs } ?: steps.last()
        val labelStyle = textStyle.copy(fontWeight = FontWeight.Normal, fontSize = 10.sp)

        val cal = Calendar.getInstance()
        var lastX = -1000f

        // Fixed grid: always align to multiples of indexStep from index 0
        val tfMs = if (totalCandles > 1) (visibleCandles.last().timestamp - visibleCandles.first().timestamp) / (visibleCandles.size - 1).coerceAtLeast(1) else 3600000L
        val indexStep = max(1, (stepMs / tfMs).toInt())

        // Calculate the full range of candle indices that could be visible on screen
        val rightmostIdx = (totalCandles - 1 - scrollOffset).toInt()
        val leftmostIdx = (rightmostIdx - (chartW / candleW).toInt() - 2).coerceAtLeast(0)

        // Snap to fixed multiples of indexStep from 0
        val startDrawIdx = (leftmostIdx / indexStep) * indexStep
        val endDrawIdx = ((rightmostIdx / indexStep) + 1) * indexStep

        for (candleIdx in startDrawIdx..endDrawIdx.coerceAtMost(totalCandles - 1) step indexStep) {
            if (candleIdx < 0 || candleIdx >= totalCandles) continue

            val candle = visibleCandles.getOrNull(candleIdx - startIdx) ?: continue
            val ts = candle.timestamp
            cal.timeInMillis = ts
            
            val curYear = cal.get(Calendar.YEAR)
            val curMonth = cal.get(Calendar.MONTH)
            val curDay = cal.get(Calendar.DAY_OF_MONTH)
            val curHour = cal.get(Calendar.HOUR_OF_DAY)
            val curMin = cal.get(Calendar.MINUTE)

            val is1Jan = curMonth == Calendar.JANUARY && curDay == 1
            val is1stOfMonth = curDay == 1
            
            // Rules: YEAR only on Jan 1st. Month name on 1st. Day number otherwise.
            val label = when {
                is1Jan -> curYear.toString()
                is1stOfMonth -> SimpleDateFormat("MMM", Locale.getDefault()).format(Date(ts))
                stepMs >= 86400000L -> curDay.toString()
                else -> String.format(Locale.getDefault(), "%02d:%02d", curHour, curMin)
            }
            
            val isProminent = is1Jan || is1stOfMonth
            val finalStyle = if (isProminent) labelStyle.copy(fontWeight = FontWeight.Bold, color = if (isProminent) Color.White else textStyle.color) else labelStyle
            
            val x = chartW - (candleW / 2f) - ((totalCandles - 1 - candleIdx - scrollOffset) * candleW)
            
            // Avoid drawing near the right app border
            if (x in 0f..(chartW - 40.dp.toPx())) {
                val layout = textMeasurer.measure(label, finalStyle)
                val safeX = x.coerceAtMost(chartW - layout.size.width / 2f - 4.dp.toPx())
                
                if (safeX - layout.size.width / 2f > lastX + 15f) {
                    val gridAlpha = if (isProminent) 0.4f else 0.2f
                    drawLine(textStyle.color.copy(gridAlpha), Offset(x, 0f), Offset(x, plotsH), 1f)
                    
                    if (isProminent) {
                        val rectW = layout.size.width + 12.dp.toPx()
                        val rectH = 18.dp.toPx()
                        drawRect(labelBgColor.copy(alpha = 1f), Offset(safeX - rectW / 2f, plotsH + 4.dp.toPx()), Size(rectW, rectH))
                        drawRect(Color.Gray.copy(alpha = 0.5f), Offset(safeX - rectW / 2f, plotsH + 4.dp.toPx()), Size(rectW, rectH), style = Stroke(1.dp.toPx()))
                    }

                    safeDrawText(textMeasurer, label, Offset(safeX - layout.size.width / 2f, plotsH + 6.dp.toPx()), finalStyle)
                    lastX = safeX + layout.size.width / 2f
                }
            }
        }
    }

    fun drawBorders(drawScope: DrawScope, chartW: Float, priceW: Float, h: Float, labelColor: Color) = with(drawScope) {
        // Main vertical line chart/axis
        drawLine(labelColor.copy(0.5f), Offset(chartW, 0f), Offset(chartW, h), 1.5.dp.toPx())
        // Far right vertical border
        drawLine(labelColor.copy(0.5f), Offset(chartW + priceW, 0f), Offset(chartW + priceW, h), 1f)
        // Bottom line spanning both
        drawLine(labelColor.copy(0.5f), Offset(0f, h), Offset(chartW + priceW, h), 1f)
    }
}
