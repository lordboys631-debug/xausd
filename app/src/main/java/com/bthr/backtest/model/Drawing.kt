package com.bthr.backtest.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.bthr.backtest.R
import java.util.UUID

data class SimpleTrendLine(
    val timestamp1: Long,
    val price1: Float,
    val timestamp2: Long,
    val price2: Float,
    val color: Color = Color.Red,
    val strokeWidth: Float = 2f,
    val lineStyle: Int = 0, // 0 = solid, 1 = dashed, 2 = dotted
    val isLocked: Boolean = false
)

data class AnchorPoint(
    val candleIdx: Int,
    val price: Float
)

enum class ExtendMode { SEGMENT, RAY, BOTH }

enum class LineStyle { SOLID, DASHED, DOTTED }

data class FibLevel(
    val ratio: Float,
    val label: String,
    val color: Color,
    val visible: Boolean = true
)

val DEFAULT_FIB_LEVELS = listOf(
    FibLevel(0.000f, "0.0%", Color(0xFF9E9E9E)),
    FibLevel(0.236f, "23.6%", Color(0xFF42A5F5)),
    FibLevel(0.382f, "38.2%", Color(0xFF26A69A)),
    FibLevel(0.500f, "50.0%", Color(0xFFFFA726)),
    FibLevel(0.618f, "61.8%", Color(0xFFEF5350)),
    FibLevel(0.786f, "78.6%", Color(0xFFAB47BC)),
    FibLevel(1.000f, "100%", Color(0xFFBDBDBD))
)

sealed class Drawing {
    abstract val id: String
    abstract val color: Color
    abstract val strokeWidth: Float
    abstract val lineStyle: LineStyle
    abstract val isVisible: Boolean
    abstract val isLocked: Boolean

    data class TrendLine(
        val p1: AnchorPoint,
        val p2: AnchorPoint,
        val extend: ExtendMode = ExtendMode.SEGMENT,
        override val id: String = UUID.randomUUID().toString(),
        override val color: Color = Color(0xFFE91E63),
        override val strokeWidth: Float = 2f,
        override val lineStyle: LineStyle = LineStyle.SOLID,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false
    ) : Drawing()

    data class HorizontalLine(
        val price: Float,
        override val id: String = UUID.randomUUID().toString(),
        override val color: Color = Color.White,
        override val strokeWidth: Float = 2f,
        override val lineStyle: LineStyle = LineStyle.SOLID,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false
    ) : Drawing()

    data class HorizontalRay(
        val anchor: AnchorPoint,
        override val id: String = UUID.randomUUID().toString(),
        override val color: Color = Color.White,
        override val strokeWidth: Float = 2f,
        override val lineStyle: LineStyle = LineStyle.SOLID,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false
    ) : Drawing()

    data class VerticalLine(
        val candleIdx: Int,
        override val id: String = UUID.randomUUID().toString(),
        override val color: Color = Color.White,
        override val strokeWidth: Float = 2f,
        override val lineStyle: LineStyle = LineStyle.SOLID,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false
    ) : Drawing()

    data class CrossLine(
        val anchor: AnchorPoint,
        override val id: String = UUID.randomUUID().toString(),
        override val color: Color = Color.White,
        override val strokeWidth: Float = 1.5f,
        override val lineStyle: LineStyle = LineStyle.DOTTED,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false
    ) : Drawing()

    data class ParallelChannel(
        val p1: AnchorPoint,
        val p2: AnchorPoint,
        val p3: AnchorPoint,
        val fillAlpha: Float = 0.14f,
        override val id: String = UUID.randomUUID().toString(),
        override val color: Color = Color(0xFF42A5F5),
        override val strokeWidth: Float = 2f,
        override val lineStyle: LineStyle = LineStyle.SOLID,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false
    ) : Drawing()

    data class FibRetracement(
        val p1: AnchorPoint,
        val p2: AnchorPoint,
        val levels: List<FibLevel> = DEFAULT_FIB_LEVELS,
        override val id: String = UUID.randomUUID().toString(),
        override val color: Color = Color(0xFF42A5F5),
        override val strokeWidth: Float = 1.5f,
        override val lineStyle: LineStyle = LineStyle.SOLID,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false
    ) : Drawing()

    data class Rectangle(
        val p1: AnchorPoint,
        val p2: AnchorPoint,
        val fillAlpha: Float = 0.12f,
        override val id: String = UUID.randomUUID().toString(),
        override val color: Color = Color(0xFF26A69A),
        override val strokeWidth: Float = 1.5f,
        override val lineStyle: LineStyle = LineStyle.SOLID,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false
    ) : Drawing()

    data class Circle(
        val center: AnchorPoint,
        val radiusPoint: AnchorPoint,
        val fillAlpha: Float = 0.10f,
        override val id: String = UUID.randomUUID().toString(),
        override val color: Color = Color(0xFFFFA726),
        override val strokeWidth: Float = 1.5f,
        override val lineStyle: LineStyle = LineStyle.SOLID,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false
    ) : Drawing()

    data class Arrow(
        val p1: AnchorPoint,
        val p2: AnchorPoint,
        override val id: String = UUID.randomUUID().toString(),
        override val color: Color = Color.White,
        override val strokeWidth: Float = 2f,
        override val lineStyle: LineStyle = LineStyle.SOLID,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false
    ) : Drawing()

    data class Polyline(
        val points: List<AnchorPoint>,
        override val id: String = UUID.randomUUID().toString(),
        override val color: Color = Color.White,
        override val strokeWidth: Float = 2f,
        override val lineStyle: LineStyle = LineStyle.SOLID,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false
    ) : Drawing()

    data class TextLabel(
        val anchor: AnchorPoint,
        val text: String,
        val fontSize: Float = 12f,
        override val id: String = UUID.randomUUID().toString(),
        override val color: Color = Color.White,
        override val strokeWidth: Float = 1f,
        override val lineStyle: LineStyle = LineStyle.SOLID,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false
    ) : Drawing()

    data class Measure(
        val p1: AnchorPoint,
        val p2: AnchorPoint,
        val barCount: Int = 0,
        val duration: String = "",
        val priceChange: Float = 0f,
        val percentageChange: Float = 0f,
        override val id: String = UUID.randomUUID().toString(),
        override val color: Color = Color(0xFF4CAF50), // Vert
        override val strokeWidth: Float = 2f,
        override val lineStyle: LineStyle = LineStyle.SOLID,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false
    ) : Drawing()

    data class LongPosition(
        val entryPoint: AnchorPoint,
        val stopPrice: Float,
        val targetPrice: Float,
        override val id: String = UUID.randomUUID().toString(),
        override val color: Color = Color(0xFF4CAF50),
        override val strokeWidth: Float = 1.5f,
        override val lineStyle: LineStyle = LineStyle.SOLID,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false
    ) : Drawing()

    data class ShortPosition(
        val entryPoint: AnchorPoint,
        val stopPrice: Float,
        val targetPrice: Float,
        override val id: String = UUID.randomUUID().toString(),
        override val color: Color = Color(0xFFF44336),
        override val strokeWidth: Float = 1.5f,
        override val lineStyle: LineStyle = LineStyle.SOLID,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false
    ) : Drawing()
}

enum class DrawingTool(val displayName: String, @DrawableRes val iconRes: Int) {
    NONE("Aucun", android.R.drawable.ic_menu_close_clear_cancel),
    MEASURE("Mesure", R.drawable.ic_measure_drawings),
    ERASER("Gomme", R.drawable.ic_eraser_drawings),

    // Lignes et Mesures
    TREND_LINE("Ligne de tendance", R.drawable.ic_tool_trend_line),
    HORIZONTAL_LINE("Ligne horizontale", R.drawable.ic_tool_horizontal_line),
    EXTENDED_LINE("Ligne étendue", R.drawable.ic_tool_extended_line),
    VERTICAL_LINE("Ligne verticale", R.drawable.ic_tool_vertical_line),
    HORIZONTAL_RAY("Rayon horizontal", R.drawable.ic_tool_horizontal_ray),
    CROSSHAIR("Ligne de croisement", R.drawable.ic_tool_cross_line),
    CROSS_LINE("Ligne transversale", R.drawable.ic_tool_cross_line),
    RAY("Rayon", R.drawable.ic_tool_ray),
    ARROW("Flèche", R.drawable.ic_tool_arrow),
    INFO_LINE("Ligne d'information", R.drawable.ic_info_line),
    PARALLEL_CHANNEL("Canal parallèle", R.drawable.ic_tool_parallel_channel),


    // Patterns
    FIB_RETRACEMENT("Retracement de Fibonacci", R.drawable.ic_fib_retracement_drawings),
    FIB_EXTENSION("Extension de Fibonacci", R.drawable.ic_price_range),
    FIB_CHANNEL("Canal de Fibonacci", R.drawable.ic_price_range),
    FIB_TIME_ZONES("Fuseaux horaires de Fibonacci", R.drawable.ic_date_range),

    // Formes et Texte
    POLYLINE("Polyline", R.drawable.ic_polyline),
    PATH("Chemin", R.drawable.ic_path),
    CURVE("Courbe", R.drawable.ic_curve),
    PROJECTION("Projection", R.drawable.ic_projection),
    RECTANGLE("Rectangle", R.drawable.ic_tool_rectangle),
    LONG_POSITION("Position Longue", R.drawable.ic_long_position),
    SHORT_POSITION("Position courte", R.drawable.ic_short_position_drawings),
    BRUSH("Pinceau", R.drawable.ic_brush_drawing),
    CIRCLE("Cercle", R.drawable.ic_circle),
    DATE_RANGE("Plage de dates", R.drawable.ic_date_range),
    PRICE_RANGE("Plage de prix", R.drawable.ic_price_range),
    DATE_PRICE_RANGE("Plage de date et de prix", R.drawable.ic_date_price_range),

    // Sacred Geometry
    GANN_BOX("Boîte de Gann", R.drawable.ic_date_price_range),
    GANN_SQUARE("Carré de Gann", R.drawable.ic_rectangle),
    GANN_FAN("Ventail de Gann", R.drawable.ic_trend_angle),

    TEXT("Texte", R.drawable.ic_text_drawings),
    ANCHORED_TEXT("Texte ancré", R.drawable.ic_anchor_text_drawings),
    NOTE("Note", android.R.drawable.ic_menu_edit)
}

data class DrawingCategory(
    val name: String,
    val tools: List<DrawingTool>
) {
    val count: Int get() = tools.size
}

object DrawingToolsData {
    val categories = listOf(
        DrawingCategory(
            "Lignes et Mesures",
            listOf(
                DrawingTool.TREND_LINE,DrawingTool.HORIZONTAL_LINE , DrawingTool.CROSSHAIR, DrawingTool.EXTENDED_LINE, DrawingTool.VERTICAL_LINE, DrawingTool.HORIZONTAL_RAY, DrawingTool.RAY, DrawingTool.ARROW,
                DrawingTool.INFO_LINE,
                 DrawingTool.CROSS_LINE
            )
        ),
        DrawingCategory(
            "Patterns",
            listOf(
                DrawingTool.FIB_RETRACEMENT, DrawingTool.FIB_EXTENSION,
                DrawingTool.FIB_CHANNEL, DrawingTool.FIB_TIME_ZONES
            )
        ),
        DrawingCategory(
            "Formes et Texte",
            listOf(
                DrawingTool.POLYLINE, DrawingTool.PATH, DrawingTool.CURVE,
                DrawingTool.PROJECTION, DrawingTool.RECTANGLE,
                DrawingTool.DATE_RANGE, DrawingTool.PRICE_RANGE, DrawingTool.DATE_PRICE_RANGE,
                DrawingTool.TEXT, DrawingTool.NOTE
            )
        )
    )
}
