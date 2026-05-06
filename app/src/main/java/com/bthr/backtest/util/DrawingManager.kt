package com.bthr.backtest.util

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import com.bthr.backtest.model.Candle
import com.bthr.backtest.model.*
import kotlin.math.abs
import kotlin.math.sqrt

// ────────────────────────────────────────────────────────────────────────────
// Drawing state machine — inspired by TradingView's drawing interaction model
// ────────────────────────────────────────────────────────────────────────────

sealed class DrawingState {
    /** No tool active */
    object Idle : DrawingState()

    /** Waiting for the first tap to place point 1 */
    data class PlacingFirstPoint(val cursorPx: Offset) : DrawingState()

    /** Point 1 placed, preview line shown to cursor */
    data class PlacingSecondPoint(val p1: AnchorPoint, val cursorPx: Offset) : DrawingState()

    /** For 3-point tools (ParallelChannel): points 1 and 2 placed */
    data class PlacingThirdPoint(val p1: AnchorPoint, val p2: AnchorPoint, val cursorPx: Offset) : DrawingState()

    /** A completed drawing is selected */
    data class Selected(val drawingId: String) : DrawingState()

    /** User is dragging an entire drawing */
    data class DraggingDrawing(
        val drawingId: String,
        val dragStartPx: Offset,
        val originalDrawing: Drawing
    ) : DrawingState()

    /** User is dragging a single handle (endpoint) */
    data class DraggingHandle(
        val drawingId: String,
        val handleIndex: Int,   // 0 = p1, 1 = p2, 2 = p3
        val originalDrawing: Drawing
    ) : DrawingState()

    /** Freehand brush: collecting points */
    data class BrushDrawing(val points: MutableList<AnchorPoint> = mutableListOf()) : DrawingState()
}

enum class DrawingUseMode {
    SINGLE,
    REPEAT
}

class DrawingManager {

    // ── Public observable state ────────────────────────────────────────────

    var completedDrawings by mutableStateOf<List<Drawing>>(emptyList())
        private set

    var drawingState by mutableStateOf<DrawingState>(DrawingState.Idle)
        private set

    var activeTool by mutableStateOf(DrawingTool.NONE)
        private set

    private var _useMode by mutableStateOf(DrawingUseMode.REPEAT)
    val useMode: DrawingUseMode get() = _useMode

    /** Currently selected drawing (mirrors Selected state) */
    val selectedDrawingId: String?
        get() = (drawingState as? DrawingState.Selected)?.drawingId

    // ── Tool activation ────────────────────────────────────────────────────

    fun setTool(tool: DrawingTool) {
        activeTool = tool
        drawingState = if (tool == DrawingTool.NONE) DrawingState.Idle
                       else DrawingState.PlacingFirstPoint(Offset.Zero)
    }

    fun setUseMode(mode: DrawingUseMode) {
        _useMode = mode
    }

    fun cancelDrawing() {
        drawingState = DrawingState.Idle
        activeTool = DrawingTool.NONE
    }

    @Suppress("unused")
    fun deselect() {
        if (drawingState is DrawingState.Selected) drawingState = DrawingState.Idle
    }

    // ── Cursor tracking (call during drag/move for preview) ────────────────

    fun onCursorMove(pixelOffset: Offset) {
        drawingState = when (val s = drawingState) {
            is DrawingState.PlacingFirstPoint  -> s.copy(cursorPx = pixelOffset)
            is DrawingState.PlacingSecondPoint -> s.copy(cursorPx = pixelOffset)
            is DrawingState.PlacingThirdPoint  -> s.copy(cursorPx = pixelOffset)
            else -> drawingState
        }
    }

    // ── Tap handling ───────────────────────────────────────────────────────

    fun onTap(pixelOffset: Offset, mapper: DrawingCoordinateMapper, candles: List<Candle> = emptyList()): Boolean {
        var completedAction = false
        when (val s = drawingState) {
            // ── ERASER ──────────────────────────────────────────────────────
            is DrawingState.PlacingFirstPoint if activeTool == DrawingTool.ERASER -> {
                val hit = hitTest(pixelOffset, mapper)
                if (hit != null) {
                    completedDrawings = completedDrawings.filter { it.id != hit }
                    if (useMode == DrawingUseMode.SINGLE) {
                        afterDrawingCompleted(pixelOffset)
                    }
                    completedAction = true
                }
            }

            // ── FIRST POINT for line-type tools ────────────────────────────
            is DrawingState.PlacingFirstPoint -> {
                val anchor = snappedAnchor(pixelOffset, mapper, candles)
                if (isSinglePointTool()) {
                    if (completeSinglePointDrawing(anchor, pixelOffset)) {
                        completedAction = true
                    }
                } else {
                    drawingState = DrawingState.PlacingSecondPoint(anchor, pixelOffset)
                }
            }

            // ── SECOND POINT ────────────────────────────────────────────────
            is DrawingState.PlacingSecondPoint -> {
                val anchor = snappedAnchor(pixelOffset, mapper, candles)
                if (needs3Points()) {
                    drawingState = DrawingState.PlacingThirdPoint(s.p1, anchor, pixelOffset)
                } else {
                    val drawing = buildDrawing(s.p1, anchor)
                    if (drawing != null) {
                        completedDrawings = completedDrawings + drawing
                        afterDrawingCompleted(pixelOffset)
                        completedAction = true
                    }
                }
            }

            // ── THIRD POINT ─────────────────────────────────────────────────
            is DrawingState.PlacingThirdPoint -> {
                val anchor = snappedAnchor(pixelOffset, mapper, candles)
                val drawing = buildDrawing3(s.p1, s.p2, anchor)
                if (drawing != null) {
                    completedDrawings = completedDrawings + drawing
                    afterDrawingCompleted(pixelOffset)
                    completedAction = true
                }
            }

            // ── IDLE — try to select a nearby drawing ────────────────────
            is DrawingState.Idle, is DrawingState.Selected -> {
                val hit = hitTest(pixelOffset, mapper)
                drawingState = if (hit != null) DrawingState.Selected(hit) else DrawingState.Idle
            }

            else -> {}
        }
        return completedAction
    }

    // ── Drag handling (for moving drawings after selection) ────────────────

    fun onDragStart(pixelOffset: Offset, mapper: DrawingCoordinateMapper) {
        val s = drawingState
        if (s is DrawingState.BrushDrawing) return
        if (activeTool == DrawingTool.BRUSH || activeTool == DrawingTool.PATH) {
            val anchor = mapper.pixelToAnchor(pixelOffset)
            drawingState = DrawingState.BrushDrawing(mutableListOf(anchor))
            return
        }
        if (s is DrawingState.Selected) {
            val drawing = completedDrawings.find { it.id == s.drawingId } ?: return
            // Check if touching a handle
            val handleIdx = getHandleIndex(pixelOffset, drawing, mapper)
            drawingState = if (handleIdx >= 0) {
                DrawingState.DraggingHandle(s.drawingId, handleIdx, drawing)
            } else {
                DrawingState.DraggingDrawing(s.drawingId, pixelOffset, drawing)
            }
        }
    }

    fun onDragUpdate(pixelOffset: Offset, dragDelta: Offset, mapper: DrawingCoordinateMapper) {
        when (val s = drawingState) {
            is DrawingState.BrushDrawing -> {
                s.points.add(mapper.pixelToAnchor(pixelOffset))
            }
            is DrawingState.DraggingHandle -> {
                val newAnchor = mapper.pixelToAnchor(pixelOffset)
                val updated = applyHandleDrag(s.originalDrawing, s.handleIndex, newAnchor)
                if (updated != null) updateDrawing(s.drawingId, updated)
            }
            is DrawingState.DraggingDrawing -> {
                val dIdx = (dragDelta.x / mapper.candleW).toInt()
                val dPrice = mapper.yToPrice(mapper.priceToY(0f) + dragDelta.y) -
                             mapper.yToPrice(mapper.priceToY(0f))
                val updated = applyTranslate(s.originalDrawing, dIdx, -dPrice)
                if (updated != null) updateDrawing(s.drawingId, updated)
            }
            else -> {}
        }
    }

    fun onDragEnd(): Boolean {
        var completedAction = false
        when (val s = drawingState) {
            is DrawingState.BrushDrawing -> {
                if (s.points.size >= 2) {
                    val drawing = Drawing.Polyline(s.points.toList())
                    completedDrawings = completedDrawings + drawing
                    completedAction = true
                }
                afterDrawingCompleted(Offset.Zero)
            }
            is DrawingState.DraggingHandle, is DrawingState.DraggingDrawing -> {
                val id = when (s) {
                    is DrawingState.DraggingHandle -> s.drawingId
                    is DrawingState.DraggingDrawing -> s.drawingId
                    else -> null
                }
                drawingState = if (id != null) DrawingState.Selected(id) else DrawingState.Idle
            }
            else -> {}
        }
        return completedAction
    }

    // ── Delete / modify selected drawing ──────────────────────────────────

    @Suppress("unused")
    fun deleteSelected() {
        val id = selectedDrawingId ?: return
        completedDrawings = completedDrawings.filter { it.id != id }
        drawingState = DrawingState.Idle
    }

    @Suppress("unused")
    fun updateSelected(newDrawing: Drawing) {
        completedDrawings = completedDrawings.map { if (it.id == newDrawing.id) newDrawing else it }
    }

    @Suppress("unused")
    fun clearAll() {
        completedDrawings = emptyList()
        drawingState = DrawingState.Idle
    }

    fun addDrawing(drawing: Drawing) {
        completedDrawings = completedDrawings + drawing
    }

    fun removeMeasures() {
        completedDrawings = completedDrawings.filterNot { it is Drawing.Measure }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ────────────────────────────────────────────────────────────────────────

    private fun isSinglePointTool() = activeTool in listOf(
        DrawingTool.HORIZONTAL_LINE, DrawingTool.VERTICAL_LINE,
        DrawingTool.CROSS_LINE, DrawingTool.CROSSHAIR
    )

    private fun needs3Points() = activeTool == DrawingTool.PARALLEL_CHANNEL

    /** Snap pixel to OHLC if close enough, then convert to AnchorPoint */
    private fun snappedAnchor(
        px: Offset,
        mapper: DrawingCoordinateMapper,
        candles: List<Candle>,
        snapThreshPx: Float = 18f
    ): AnchorPoint {
        val idx = mapper.xToCandleIdx(px.x)
        val candle = candles.getOrNull(idx)
        if (candle != null) {
            val x = mapper.candleIdxToX(idx)
            for (price in listOf(candle.high, candle.low, candle.open, candle.close)) {
                val y = mapper.priceToY(price)
                val d = sqrt((px.x - x) * (px.x - x) + (px.y - y) * (px.y - y))
                if (d <= snapThreshPx) return AnchorPoint(idx, price)
            }
        }
        return mapper.pixelToAnchor(px)
    }

    private fun completeSinglePointDrawing(anchor: AnchorPoint, cursorPx: Offset): Boolean {
        val drawing: Drawing? = when (activeTool) {
            DrawingTool.HORIZONTAL_LINE, DrawingTool.CROSSHAIR ->
                Drawing.HorizontalLine(anchor.price)
            DrawingTool.VERTICAL_LINE ->
                Drawing.VerticalLine(anchor.candleIdx)
            DrawingTool.CROSS_LINE ->
                Drawing.CrossLine(anchor)
            else -> null
        }
        if (drawing != null) {
            completedDrawings = completedDrawings + drawing
            afterDrawingCompleted(cursorPx)
            return true
        }
        return false
    }

    private fun afterDrawingCompleted(cursorPx: Offset) {
        drawingState = if (useMode == DrawingUseMode.REPEAT && activeTool != DrawingTool.NONE) {
            DrawingState.PlacingFirstPoint(cursorPx)
        } else {
            DrawingState.Idle
        }
        if (useMode == DrawingUseMode.SINGLE) {
            activeTool = DrawingTool.NONE
        }
    }

    private fun buildDrawing(p1: AnchorPoint, p2: AnchorPoint): Drawing? = when (activeTool) {
        DrawingTool.TREND_LINE   -> Drawing.TrendLine(p1, p2, ExtendMode.SEGMENT)
        DrawingTool.EXTENDED_LINE -> Drawing.TrendLine(p1, p2, ExtendMode.BOTH)
        DrawingTool.RAY          -> Drawing.TrendLine(p1, p2, ExtendMode.RAY)
        DrawingTool.INFO_LINE    -> Drawing.TrendLine(p1, p2, ExtendMode.SEGMENT)
        DrawingTool.HORIZONTAL_RAY -> Drawing.HorizontalRay(p1)
        DrawingTool.ARROW        -> Drawing.Arrow(p1, p2)
        DrawingTool.FIB_RETRACEMENT -> Drawing.FibRetracement(p1, p2)
        DrawingTool.FIB_EXTENSION   -> Drawing.FibRetracement(p1, p2, DEFAULT_FIB_LEVELS)
        DrawingTool.RECTANGLE    -> Drawing.Rectangle(p1, p2)
        DrawingTool.CIRCLE       -> Drawing.Circle(p1, p2)
        DrawingTool.MEASURE, DrawingTool.PRICE_RANGE,
        DrawingTool.DATE_RANGE, DrawingTool.DATE_PRICE_RANGE -> Drawing.Measure(p1, p2)
        DrawingTool.LONG_POSITION -> Drawing.LongPosition(
            p1,
            stopPrice  = p1.price - abs(p2.price - p1.price),
            targetPrice = p1.price + abs(p2.price - p1.price) * 2f
        )
        DrawingTool.SHORT_POSITION -> Drawing.ShortPosition(
            p1,
            stopPrice  = p1.price + abs(p2.price - p1.price),
            targetPrice = p1.price - abs(p2.price - p1.price) * 2f
        )
        DrawingTool.POLYLINE, DrawingTool.PATH, DrawingTool.CURVE,
        DrawingTool.PROJECTION ->
            Drawing.Polyline(listOf(p1, p2))
        DrawingTool.TEXT, DrawingTool.ANCHORED_TEXT, DrawingTool.NOTE ->
            Drawing.TextLabel(p1, "Text")
        else -> null
    }

    private fun buildDrawing3(p1: AnchorPoint, p2: AnchorPoint, p3: AnchorPoint): Drawing? =
        when (activeTool) {
            DrawingTool.PARALLEL_CHANNEL -> Drawing.ParallelChannel(p1, p2, p3)
            else -> null
        }

    // Hit-test: returns id of the topmost drawing under tap, or null
    private fun hitTest(tap: Offset, mapper: DrawingCoordinateMapper): String? {
        val thresh = 14f
        return completedDrawings.lastOrNull { drawing ->
            hitTestDrawing(tap, drawing, mapper, thresh)
        }?.id
    }

    private fun hitTestDrawing(tap: Offset, d: Drawing, mapper: DrawingCoordinateMapper, thresh: Float): Boolean =
        when (d) {
            is Drawing.TrendLine -> {
                val (s, e) = mapper.extendLine(mapper.anchorToPixel(d.p1), mapper.anchorToPixel(d.p2), d.extend)
                mapper.isNearSegment(tap, s, e, thresh) ||
                mapper.isNearAnchor(tap, mapper.anchorToPixel(d.p1)) ||
                mapper.isNearAnchor(tap, mapper.anchorToPixel(d.p2))
            }
            is Drawing.HorizontalLine -> {
                val y = mapper.priceToY(d.price)
                abs(tap.y - y) <= thresh
            }
            is Drawing.HorizontalRay -> {
                val ax = mapper.candleIdxToX(d.anchor.candleIdx)
                val ay = mapper.priceToY(d.anchor.price)
                tap.x >= ax && abs(tap.y - ay) <= thresh
            }
            is Drawing.VerticalLine -> {
                val x = mapper.candleIdxToX(d.candleIdx)
                abs(tap.x - x) <= thresh
            }
            is Drawing.CrossLine -> {
                val px = mapper.anchorToPixel(d.anchor)
                abs(tap.x - px.x) <= thresh || abs(tap.y - px.y) <= thresh
            }
            is Drawing.FibRetracement -> {
                val px1 = mapper.anchorToPixel(d.p1); val px2 = mapper.anchorToPixel(d.p2)
                val highY = minOf(px1.y, px2.y); val lowY = maxOf(px1.y, px2.y)
                d.levels.any { level ->
                    val levelY = highY + (lowY - highY) * level.ratio
                    abs(tap.y - levelY) <= thresh
                }
            }
            is Drawing.Rectangle -> {
                val px1 = mapper.anchorToPixel(d.p1); val px2 = mapper.anchorToPixel(d.p2)
                val left = minOf(px1.x, px2.x); val right = maxOf(px1.x, px2.x)
                val top = minOf(px1.y, px2.y); val bottom = maxOf(px1.y, px2.y)
                (tap.x in left..right && (abs(tap.y - top) <= thresh || abs(tap.y - bottom) <= thresh)) ||
                (tap.y in top..bottom && (abs(tap.x - left) <= thresh || abs(tap.x - right) <= thresh))
            }
            is Drawing.Arrow -> {
                val px1 = mapper.anchorToPixel(d.p1); val px2 = mapper.anchorToPixel(d.p2)
                mapper.isNearSegment(tap, px1, px2, thresh)
            }
            is Drawing.Measure -> {
                val px1 = mapper.anchorToPixel(d.p1); val px2 = mapper.anchorToPixel(d.p2)
                val left = minOf(px1.x, px2.x); val right = maxOf(px1.x, px2.x)
                val top = minOf(px1.y, px2.y); val bottom = maxOf(px1.y, px2.y)
                tap.x in (left - thresh)..(right + thresh) && tap.y in (top - thresh)..(bottom + thresh)
            }
            is Drawing.Polyline -> {
                val pxPoints = d.points.map { mapper.anchorToPixel(it) }
                pxPoints.zipWithNext().any { (a, b) -> mapper.isNearSegment(tap, a, b, thresh) }
            }
            is Drawing.TextLabel -> {
                val px = mapper.anchorToPixel(d.anchor)
                mapper.isNearAnchor(tap, px, 30f)
            }
            is Drawing.Circle -> {
                val center = mapper.anchorToPixel(d.center)
                val radiusPt = mapper.anchorToPixel(d.radiusPoint)
                val r = sqrt((radiusPt.x - center.x) * (radiusPt.x - center.x) + (radiusPt.y - center.y) * (radiusPt.y - center.y))
                val dist = sqrt((tap.x - center.x) * (tap.x - center.x) + (tap.y - center.y) * (tap.y - center.y))
                abs(dist - r) <= thresh
            }
            is Drawing.LongPosition, is Drawing.ShortPosition -> {
                val entry = when (d) {
                    is Drawing.LongPosition -> mapper.anchorToPixel(d.entryPoint)
                    is Drawing.ShortPosition -> mapper.anchorToPixel(d.entryPoint)
                    else -> return false
                }
                abs(tap.y - entry.y) <= thresh * 2
            }
            is Drawing.ParallelChannel -> {
                val px1 = mapper.anchorToPixel(d.p1); val px2 = mapper.anchorToPixel(d.p2)
                val px3 = mapper.anchorToPixel(d.p3)
                val dy = px3.y - px2.y
                val px2b = Offset(px1.x, px1.y + dy)
                val px3b = Offset(px2.x, px2.y + dy)
                mapper.isNearSegment(tap, px1, px2, thresh) ||
                mapper.isNearSegment(tap, px2b, px3b, thresh)
            }
        }

    // Returns handle index that is near tap, or -1
    private fun getHandleIndex(tap: Offset, drawing: Drawing, mapper: DrawingCoordinateMapper): Int {
        val handleThresh = 20f
        val anchors = getAnchors(drawing)
        anchors.forEachIndexed { i, a ->
            if (mapper.isNearAnchor(tap, mapper.anchorToPixel(a), handleThresh)) return i
        }
        return -1
    }

    private fun getAnchors(d: Drawing): List<AnchorPoint> = when (d) {
        is Drawing.TrendLine -> listOf(d.p1, d.p2)
        is Drawing.FibRetracement -> listOf(d.p1, d.p2)
        is Drawing.Rectangle -> listOf(d.p1, d.p2)
        is Drawing.Arrow -> listOf(d.p1, d.p2)
        is Drawing.Measure -> listOf(d.p1, d.p2)
        is Drawing.Circle -> listOf(d.center, d.radiusPoint)
        is Drawing.ParallelChannel -> listOf(d.p1, d.p2, d.p3)
        is Drawing.HorizontalRay -> listOf(d.anchor)
        is Drawing.CrossLine -> listOf(d.anchor)
        is Drawing.TextLabel -> listOf(d.anchor)
        is Drawing.HorizontalLine -> listOf(AnchorPoint(0, d.price))
        is Drawing.VerticalLine -> listOf(AnchorPoint(d.candleIdx, 0f))
        is Drawing.LongPosition -> listOf(d.entryPoint)
        is Drawing.ShortPosition -> listOf(d.entryPoint)
        is Drawing.Polyline -> d.points
    }

    private fun applyHandleDrag(d: Drawing, handleIdx: Int, newAnchor: AnchorPoint): Drawing? =
        when (d) {
            is Drawing.TrendLine -> if (handleIdx == 0) d.copy(p1 = newAnchor) else d.copy(p2 = newAnchor)
            is Drawing.FibRetracement -> if (handleIdx == 0) d.copy(p1 = newAnchor) else d.copy(p2 = newAnchor)
            is Drawing.Rectangle -> if (handleIdx == 0) d.copy(p1 = newAnchor) else d.copy(p2 = newAnchor)
            is Drawing.Arrow -> if (handleIdx == 0) d.copy(p1 = newAnchor) else d.copy(p2 = newAnchor)
            is Drawing.Measure -> if (handleIdx == 0) d.copy(p1 = newAnchor) else d.copy(p2 = newAnchor)
            is Drawing.Circle -> if (handleIdx == 0) d.copy(center = newAnchor) else d.copy(radiusPoint = newAnchor)
            is Drawing.HorizontalLine -> d.copy(price = newAnchor.price)
            is Drawing.HorizontalRay -> d.copy(anchor = newAnchor)
            is Drawing.VerticalLine -> d.copy(candleIdx = newAnchor.candleIdx)
            is Drawing.CrossLine -> d.copy(anchor = newAnchor)
            is Drawing.TextLabel -> d.copy(anchor = newAnchor)
            is Drawing.LongPosition -> d.copy(entryPoint = newAnchor)
            is Drawing.ShortPosition -> d.copy(entryPoint = newAnchor)
            is Drawing.ParallelChannel -> when (handleIdx) {
                0 -> d.copy(p1 = newAnchor)
                1 -> d.copy(p2 = newAnchor)
                else -> d.copy(p3 = newAnchor)
            }
            is Drawing.Polyline -> {
                if (handleIdx in d.points.indices) {
                    val updated = d.points.toMutableList()
                    updated[handleIdx] = newAnchor
                    d.copy(points = updated)
                } else null
            }
        }

    private fun applyTranslate(d: Drawing, dIdx: Int, dPrice: Float): Drawing? {
        fun AnchorPoint.shift() = AnchorPoint((candleIdx + dIdx).coerceAtLeast(0), price + dPrice)
        return when (d) {
            is Drawing.TrendLine    -> d.copy(p1 = d.p1.shift(), p2 = d.p2.shift())
            is Drawing.FibRetracement -> d.copy(p1 = d.p1.shift(), p2 = d.p2.shift())
            is Drawing.Rectangle    -> d.copy(p1 = d.p1.shift(), p2 = d.p2.shift())
            is Drawing.Arrow        -> d.copy(p1 = d.p1.shift(), p2 = d.p2.shift())
            is Drawing.Measure      -> d.copy(p1 = d.p1.shift(), p2 = d.p2.shift())
            is Drawing.Circle       -> d.copy(center = d.center.shift(), radiusPoint = d.radiusPoint.shift())
            is Drawing.HorizontalLine -> d.copy(price = d.price + dPrice)
            is Drawing.HorizontalRay -> d.copy(anchor = d.anchor.shift())
            is Drawing.VerticalLine -> d.copy(candleIdx = (d.candleIdx + dIdx).coerceAtLeast(0))
            is Drawing.CrossLine    -> d.copy(anchor = d.anchor.shift())
            is Drawing.TextLabel    -> d.copy(anchor = d.anchor.shift())
            is Drawing.LongPosition -> d.copy(entryPoint = d.entryPoint.shift(),
                stopPrice = d.stopPrice + dPrice, targetPrice = d.targetPrice + dPrice)
            is Drawing.ShortPosition -> d.copy(entryPoint = d.entryPoint.shift(),
                stopPrice = d.stopPrice + dPrice, targetPrice = d.targetPrice + dPrice)
            is Drawing.ParallelChannel -> d.copy(p1 = d.p1.shift(), p2 = d.p2.shift(), p3 = d.p3.shift())
            is Drawing.Polyline     -> d.copy(points = d.points.map { it.shift() })
        }
    }

    private fun updateDrawing(id: String, updated: Drawing) {
        completedDrawings = completedDrawings.map { if (it.id == id) updated else it }
    }
}
