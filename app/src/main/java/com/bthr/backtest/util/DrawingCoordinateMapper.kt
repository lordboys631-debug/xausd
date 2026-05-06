package com.bthr.backtest.util

import androidx.compose.ui.geometry.Offset
import kotlin.math.*

/**
 * Converts between logical chart coordinates (candleIdx, price) and screen pixels.
 * Recompute whenever zoom, scroll, or chart dimensions change.
 *
 * Coordinate system mirrors TradingView:
 *  - X axis: candle index, 0 = oldest candle (left)
 *  - Y axis: price, increases upward
 */
data class DrawingCoordinateMapper(
    val chartWidthPx: Float,
    val chartHeightPx: Float,
    val totalCandleCount: Int,
    val scrollOffset: Float,       // how many candles are scrolled from the right
    val displayCount: Float,       // visible candle count
    val marginRightBars: Float,
    val minPrice: Float,           // effective bottom price (after padding)
    val maxPrice: Float,           // effective top price (after padding)
    val topMarginPx: Float,
    val bottomMarginPx: Float
) {
    private val effectiveH = chartHeightPx - topMarginPx - bottomMarginPx
    val candleW: Float = if (displayCount > 0) chartWidthPx / (displayCount + marginRightBars) else 1f
    private val priceRange: Float = (maxPrice - minPrice).coerceAtLeast(0.001f)

    // ── Price ↔ Y ──────────────────────────────────────────────────────────────

    fun priceToY(price: Float): Float =
        topMarginPx + effectiveH - ((price - minPrice) / priceRange * effectiveH)

    fun yToPrice(y: Float): Float =
        minPrice + ((effectiveH - (y - topMarginPx)) / effectiveH * priceRange)

    // ── CandleIdx ↔ X ─────────────────────────────────────────────────────────

    fun candleIdxToX(idx: Int): Float =
        chartWidthPx - (totalCandleCount - 1 - idx - scrollOffset) * candleW - candleW / 2f

    fun xToCandleIdx(x: Float): Int =
        round(totalCandleCount - 1 - scrollOffset - (chartWidthPx - x - candleW / 2f) / candleW)
            .toInt()
            .coerceIn(0, (totalCandleCount - 1).coerceAtLeast(0))

    // ── AnchorPoint ↔ Offset ──────────────────────────────────────────────────

    fun anchorToPixel(a: com.bthr.backtest.model.AnchorPoint): Offset =
        Offset(candleIdxToX(a.candleIdx), priceToY(a.price))

    fun pixelToAnchor(px: Offset): com.bthr.backtest.model.AnchorPoint =
        com.bthr.backtest.model.AnchorPoint(xToCandleIdx(px.x), yToPrice(px.y))

    /** Snap a pixel X to the nearest candle center X */
    fun snapXToCandle(x: Float): Float {
        val idx = xToCandleIdx(x)
        return candleIdxToX(idx)
    }

    // ── Line geometry helpers ──────────────────────────────────────────────────

    /**
     * Extend (or clip) a segment [p1→p2] according to [extend], clipped to [bounds].
     * Returns a (start, end) pair ready to drawLine.
     */
    fun extendLine(
        p1: Offset, p2: Offset,
        extend: com.bthr.backtest.model.ExtendMode,
        left: Float = 0f,
        right: Float = chartWidthPx,
        top: Float = 0f,
        bottom: Float = chartHeightPx
    ): Pair<Offset, Offset> {
        if (extend == com.bthr.backtest.model.ExtendMode.SEGMENT) return Pair(p1, p2)
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        if (abs(dx) < 0.001f && abs(dy) < 0.001f) return Pair(p1, p2)

        fun xAtY(y: Float) = if (abs(dy) < 0.001f) p1.x else p1.x + (y - p1.y) * dx / dy
        fun yAtX(x: Float) = if (abs(dx) < 0.001f) p1.y else p1.y + (x - p1.x) * dy / dx

        // Find where the line exits the bounding box in the forward direction
        fun edgeOffset(from: Offset, dirDx: Float, dirDy: Float): Offset {
            val candidates = mutableListOf<Offset>()
            if (abs(dirDx) > 0.001f) {
                val xEdge = if (dirDx > 0) right else left
                val yAtXEdge = from.y + (xEdge - from.x) * dirDy / dirDx
                if (yAtXEdge in top..bottom) candidates.add(Offset(xEdge, yAtXEdge))
            }
            if (abs(dirDy) > 0.001f) {
                val yEdge = if (dirDy > 0) bottom else top
                val xAtYEdge = from.x + (yEdge - from.y) * dirDx / dirDy
                if (xAtYEdge in left..right) candidates.add(Offset(xAtYEdge, yEdge))
            }
            return candidates.minByOrNull {
                val ex = it.x - from.x; val ey = it.y - from.y; ex * ex + ey * ey
            } ?: from
        }

        val forward = edgeOffset(p1, dx, dy)
        return if (extend == com.bthr.backtest.model.ExtendMode.RAY) {
            Pair(p1, forward)
        } else { // BOTH
            val backward = edgeOffset(p1, -dx, -dy)
            Pair(backward, forward)
        }
    }

    // ── Hit-testing ────────────────────────────────────────────────────────────

    /** Distance from point [tap] to the infinite line through [a] and [b] */
    fun distanceToLine(tap: Offset, a: Offset, b: Offset): Float {
        val dx = b.x - a.x; val dy = b.y - a.y
        val len = sqrt(dx * dx + dy * dy)
        if (len < 0.001f) return sqrt((tap.x - a.x).pow(2) + (tap.y - a.y).pow(2))
        return abs(dy * tap.x - dx * tap.y + b.x * a.y - b.y * a.x) / len
    }

    /** Distance from point [tap] to segment [a]→[b] */
    fun distanceToSegment(tap: Offset, a: Offset, b: Offset): Float {
        val dx = b.x - a.x; val dy = b.y - a.y
        val lenSq = dx * dx + dy * dy
        if (lenSq < 0.001f) return sqrt((tap.x - a.x).pow(2) + (tap.y - a.y).pow(2))
        val t = ((tap.x - a.x) * dx + (tap.y - a.y) * dy) / lenSq
        val clampedT = t.coerceIn(0f, 1f)
        val projX = a.x + clampedT * dx; val projY = a.y + clampedT * dy
        return sqrt((tap.x - projX).pow(2) + (tap.y - projY).pow(2))
    }

    fun isNearAnchor(tap: Offset, anchor: Offset, threshPx: Float = 20f): Boolean {
        val dx = tap.x - anchor.x; val dy = tap.y - anchor.y
        return sqrt(dx * dx + dy * dy) <= threshPx
    }

    fun isNearSegment(tap: Offset, a: Offset, b: Offset, threshPx: Float = 12f): Boolean =
        distanceToSegment(tap, a, b) <= threshPx

    fun isNearLine(tap: Offset, a: Offset, b: Offset, threshPx: Float = 12f): Boolean =
        distanceToLine(tap, a, b) <= threshPx
}


