package com.bthr.backtest.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*

object ChartUtils {
    fun formatPrice(price: Float, precision: Int = 2): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ' '
            decimalSeparator = '.'
        }
        val pattern = if (precision > 0) "#,##0." + "0".repeat(precision) else "#,##0"
        return DecimalFormat(pattern, symbols).format(price)
    }

    fun formatVolume(volume: Float): String {
        return when {
            volume >= 1_000_000_000 -> String.format(Locale.US, "%.2f B", volume / 1_000_000_000f)
            volume >= 1_000_000 -> String.format(Locale.US, "%.2f M", volume / 1_000_000f)
            volume >= 1_000 -> String.format(Locale.US, "%.2f K", volume / 1_000f)
            else -> String.format(Locale.US, "%.0f", volume)
        }
    }

    fun timeframeToMillis(timeframe: String): Long {
        val value = timeframe.filter { it.isDigit() }.toLongOrNull() ?: 1L
        val unit = timeframe.filter { it.isLetter() }.lowercase()
        return when (unit) {
            "m" -> value * 60 * 1000
            "h" -> value * 60 * 60 * 1000
            "d" -> value * 24 * 60 * 60 * 1000
            "w" -> value * 7 * 24 * 60 * 60 * 1000
            "mn" -> value * 30 * 24 * 60 * 60 * 1000
            else -> value * 60 * 1000
        }
    }

    fun getNextTradingTimestamp(currentTs: Long, tfMillis: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        var nextTs = currentTs + tfMillis
        cal.timeInMillis = nextTs
        while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            nextTs += tfMillis
            cal.timeInMillis = nextTs
        }
        return nextTs
    }

    fun getPrevTradingTimestamp(currentTs: Long, tfMillis: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        var prevTs = currentTs - tfMillis
        cal.timeInMillis = prevTs
        while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            prevTs -= tfMillis
            cal.timeInMillis = prevTs
        }
        return prevTs
    }

    fun calculatePriceStep(range: Float, targetSteps: Int = 8): Float {
        if (range <= 0) return 10f
        val rawStep = range / targetSteps
        val exp = floor(log10(rawStep.toDouble())).toInt()
        val frac = rawStep / 10.0.pow(exp.toDouble())
        
        val prettyFrac = when {
            frac < 1.5 -> 1.0
            frac < 3.0 -> 2.0
            frac < 4.5 -> 4.0
            frac < 7.5 -> 5.0
            else -> 10.0
        }
        return (prettyFrac * 10.0.pow(exp.toDouble())).toFloat()
    }

    fun drawIndicatorPath(
        drawScope: DrawScope,
        values: List<Float?>,
        startIdx: Int,
        endIdx: Int,
        scrollOffset: Float,
        candleW: Float,
        chartW: Float,
        color: Color,
        thickness: Float = 1f,
        style: Int = 0,
        normY: (Float) -> Float,
        totalCount: Int = -1,
        marginRightBars: Float = 0f
    ) {
        if (values.isEmpty()) return
        val path = Path()
        var started = false
        val refCount = if (totalCount <= 0) values.size else totalCount
        
        var lastX = -1000f
        val minXGap = 1.0f // Ignorer les points qui font moins d'un pixel de large

        for (i in startIdx until endIdx.coerceAtMost(values.size)) {
            val v = values[i] ?: continue
            val x = chartW - (candleW / 2f) - (marginRightBars * candleW) - ((refCount - 1 - i - scrollOffset) * candleW)
            
            // Performance: Sauter les points invisibles ou trop proches horizontalement
            if (x < -10f) continue
            if (x > chartW + 10f) break
            if (started && abs(x - lastX) < minXGap) continue 

            if (!started) {
                path.moveTo(x, normY(v))
                started = true
            } else {
                path.lineTo(x, normY(v))
            }
            lastX = x
        }
        
        val strokeWidth = drawScope.run { thickness.dp.toPx() }
        val pathEffect = when(style) {
            1 -> PathEffect.dashPathEffect(floatArrayOf(strokeWidth * 5f, strokeWidth * 3f))
            2 -> PathEffect.dashPathEffect(floatArrayOf(0f, strokeWidth * 3f))
            else -> null
        }
        val cap = if (style == 2) StrokeCap.Round else StrokeCap.Butt

        drawScope.drawPath(path, color, style = Stroke(width = strokeWidth, pathEffect = pathEffect, cap = cap))
    }
}
