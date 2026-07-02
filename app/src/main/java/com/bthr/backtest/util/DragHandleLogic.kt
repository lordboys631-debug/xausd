package com.bthr.backtest.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bthr.backtest.model.AnchorPoint
import kotlin.math.sqrt

/**
 * Extracted and optimized drag handle logic for SimpleTrendLine bubbles and Path movement.
 * This module contains the proven-working logic for:
 * - Big bubble handles on trend lines (TradingView-style drawing interactions)
 * - Path point dragging with snapping
 * - Drawing translation (moving entire drawings)
 */

object DragHandleLogic {

    /**
     * Constants for handle detection and rendering
     */
    object HandleConstants {
        const val BIG_BUBBLE_RADIUS = 25f        // Radius of the big bubble handle
        const val BIG_BUBBLE_STROKE_WIDTH = 4f   // Stroke width for bubble outline
        const val HANDLE_THRESHOLD = 100f        // Distance threshold for hit detection (pixel)
        const val PATH_POINT_THRESHOLD = 100f    // Distance threshold for path point detection
        const val SNAP_THRESHOLD = 18f           // Distance to snap to OHLC candle values (pixel)
    }

    /**
     * Check if a tap/click is near a handle (circular detection)
     *
     * @param tapPoint The position where user tapped
     * @param handleCenter The center of the handle
     * @param threshold The detection threshold in pixels
     * @return true if tap is within threshold distance
     */
    fun isNearHandle(
        tapPoint: Offset,
        handleCenter: Offset,
        threshold: Float = HandleConstants.HANDLE_THRESHOLD
    ): Boolean {
        val dx = tapPoint.x - handleCenter.x
        val dy = tapPoint.y - handleCenter.y
        val distance = sqrt(dx * dx + dy * dy)
        return distance <= threshold
    }

    /**
     * Find the closest handle in a list of handle positions
     * Useful for multi-point drawings like paths or polylines
     *
     * @param tapPoint The position where user tapped
     * @param handleCenters List of handle positions
     * @param threshold The detection threshold in pixels
     * @return Index of the closest handle, or -1 if none within threshold
     */
    fun findClosestHandle(
        tapPoint: Offset,
        handleCenters: List<Offset>,
        threshold: Float = HandleConstants.HANDLE_THRESHOLD
    ): Int {
        var closest = -1
        var minDistance = threshold

        handleCenters.forEachIndexed { index, center ->
            val dx = tapPoint.x - center.x
            val dy = tapPoint.y - center.y
            val distance = sqrt(dx * dx + dy * dy)

            if (distance < minDistance) {
                minDistance = distance
                closest = index
            }
        }

        return closest
    }

    /**
     * Calculate the movement delta from a drag gesture
     * Used for translating entire drawings
     *
     * @param dragStartPx The starting position of the drag
     * @param currentPx The current position of the drag
     * @return Offset representing the delta movement
     */
    fun calculateDragDelta(dragStartPx: Offset, currentPx: Offset): Offset {
        return Offset(
            x = currentPx.x - dragStartPx.x,
            y = currentPx.y - dragStartPx.y
        )
    }

    /**
     * Convert drag delta (pixels) to candle index and price delta
     * Essential for cross-timeframe persistence
     *
     * @param totalDeltaX Accumulated X pixel delta
     * @param totalDeltaY Accumulated Y pixel delta
     * @param candleWidthPx Width of one candle in pixels
     * @param yToPrice Function to convert Y pixel to price
     * @param priceToY Function to convert price to Y pixel
     * @return Pair of (candleIndexDelta, priceDelta)
     */
    fun calculateCandleAndPriceDelta(
        totalDeltaX: Float,
        totalDeltaY: Float,
        candleWidthPx: Float,
        yToPrice: (Float) -> Float,
        priceToY: (Float) -> Float
    ): Pair<Int, Float> {
        val dIdx = (totalDeltaX / candleWidthPx).toInt()

        // y increases downward, so negative totalDeltaY means price up
        val dPrice = yToPrice(priceToY(0f) + totalDeltaY) - yToPrice(priceToY(0f))

        return Pair(dIdx, dPrice)
    }

    /**
     * Validate if a new position is within drawing area bounds
     * Prevents handles from being dragged completely off-screen
     *
     * @param position The proposed new position
     * @param chartWidthPx Width of the chart area
     * @param chartHeightPx Height of the chart area
     * @param margin Optional margin to allow some overflow (default: 0)
     * @return The position, clamped to bounds if necessary
     */
    fun clampPositionToBounds(
        position: Offset,
        chartWidthPx: Float,
        chartHeightPx: Float,
        margin: Float = 50f
    ): Offset {
        return Offset(
            x = position.x.coerceIn(-margin, chartWidthPx + margin),
            y = position.y.coerceIn(-margin, chartHeightPx + margin)
        )
    }

    /**
     * Calculate bubble render properties based on theme
     *
     * @param isDarkTheme Whether the theme is dark
     * @return BubbleRenderProps with colors and dimensions
     */
    fun getBubbleRenderProperties(isDarkTheme: Boolean): BubbleRenderProps {
        return BubbleRenderProps(
            fillColor = if (isDarkTheme) androidx.compose.ui.graphics.Color.Black
                       else androidx.compose.ui.graphics.Color.White,
            strokeColor = androidx.compose.ui.graphics.Color(0xFF2196F3),
            radius = HandleConstants.BIG_BUBBLE_RADIUS,
            strokeWidth = HandleConstants.BIG_BUBBLE_STROKE_WIDTH
        )
    }

    /**
     * Check if a point (especially for paths) is within segment distance
     * Useful for hit-testing polylines and paths
     *
     * @param tapPoint The tap location
     * @param segmentStart The start of the segment
     * @param segmentEnd The end of the segment
     * @param threshold Detection threshold
     * @return true if tap is near the segment
     */
    fun isNearSegment(
        tapPoint: Offset,
        segmentStart: Offset,
        segmentEnd: Offset,
        threshold: Float = 50f
    ): Boolean {
        // Project point onto line segment
        val dx = segmentEnd.x - segmentStart.x
        val dy = segmentEnd.y - segmentStart.y

        if (dx == 0f && dy == 0f) {
            // Segment is a point
            return isNearHandle(tapPoint, segmentStart, threshold)
        }

        val t = ((tapPoint.x - segmentStart.x) * dx + (tapPoint.y - segmentStart.y) * dy) /
                (dx * dx + dy * dy)
        val t_clamped = t.coerceIn(0f, 1f)

        val projectionX = segmentStart.x + t_clamped * dx
        val projectionY = segmentStart.y + t_clamped * dy

        val pdx = tapPoint.x - projectionX
        val pdy = tapPoint.y - projectionY
        val distance = sqrt(pdx * pdx + pdy * pdy)

        return distance <= threshold
    }

    /**
     * Calculate snapped anchor position for OHLC alignment
     * Ensures consistency when snapping to candle values
     *
     * @param pixelPosition The pixel position to snap
     * @param candleData Optional OHLC data for snapping
     * @param mapper The coordinate mapper
     * @param snapThreshold Pixel distance threshold for snapping
     * @return The snapped AnchorPoint
     */
    fun getSnappedAnchor(
        pixelPosition: Offset,
        mapper: DrawingCoordinateMapper,
        snapThreshold: Float = HandleConstants.SNAP_THRESHOLD
    ): AnchorPoint {
        val idx = mapper.xToCandleIdx(pixelPosition.x)

        // Try to snap to candle OHLC values
        val candle = mapper.candles.getOrNull(idx)
        if (candle != null) {
            val x = mapper.candleIdxToX(idx)
            for (price in listOf(candle.high, candle.low, candle.open, candle.close)) {
                val y = mapper.priceToY(price)
                val distance = sqrt(
                    (pixelPosition.x - x) * (pixelPosition.x - x) +
                    (pixelPosition.y - y) * (pixelPosition.y - y)
                )
                if (distance <= snapThreshold) {
                    return AnchorPoint(idx, price)
                }
            }
        }

        // No snap applicable, return regular anchor
        return mapper.pixelToAnchor(pixelPosition)
    }

    /**
     * Data class for bubble rendering properties
     */
    data class BubbleRenderProps(
        val fillColor: androidx.compose.ui.graphics.Color,
        val strokeColor: androidx.compose.ui.graphics.Color,
        val radius: Float,
        val strokeWidth: Float
    )
}

/**
 * Extension function for easier handle detection on lists of offsets
 */
fun List<Offset>.findNearestHandle(
    tapPoint: Offset,
    threshold: Float = DragHandleLogic.HandleConstants.HANDLE_THRESHOLD
): Int = DragHandleLogic.findClosestHandle(tapPoint, this, threshold)

/**
 * Extension function for clamping offsets
 */
fun Offset.clampToBounds(
    chartWidthPx: Float,
    chartHeightPx: Float,
    margin: Float = 50f
): Offset = DragHandleLogic.clampPositionToBounds(this, chartWidthPx, chartHeightPx, margin)

