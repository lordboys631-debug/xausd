package com.bthr.backtest.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.platform.LocalContext

private const val COLOR_SWATCHES_PREF_KEY = "custom_color_swatches"
private const val PREFS_NAME = "chart_prefs"

/** Charge les couleurs personnalisées depuis SharedPreferences */
private fun loadCustomSwatches(context: Context): List<Color> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val colorsStr = prefs.getString(COLOR_SWATCHES_PREF_KEY, "") ?: ""
    return if (colorsStr.isEmpty()) emptyList()
    else colorsStr.split(",").mapNotNull { it.toIntOrNull()?.let { argb -> Color(argb) } }
}

/** Sauvegarde les couleurs personnalisées dans SharedPreferences */
private fun saveCustomSwatches(context: Context, swatches: List<Color>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val colorsStr = swatches.joinToString(",") { it.toArgb().toString() }
    prefs.edit().putString(COLOR_SWATCHES_PREF_KEY, colorsStr).apply()
}

private fun lineThicknessDp(thickness: Int): Float {
    // Niveau 1 plus fin que 1dp pour une mire plus discrète.
    return if (thickness <= 1) 0.75f else thickness.toFloat()
}

@Composable
fun ColorBox(
    color: Color,
    onColorChange: (Color) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier.size(24.dp),
    thickness: Int = 1,
    style: Int = 0,
    onLineSettingsChange: ((Color, Int, Int) -> Unit)? = null
) {
    var showPicker by remember { mutableStateOf(false) }
    val borderColor = if (isDarkTheme) Color(0xFF434651) else Color(0xFFD1D4DC)
    
    Box {
        Box(
            modifier = modifier
                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                .padding(2.dp)
                .background(color, shape = RoundedCornerShape(2.dp))
                .clickable { showPicker = true }
        )
        if (showPicker) {
            ColorPickerPopup(
                selectedColor = color,
                onColorSelected = { selected ->
                    onColorChange(selected)
                    onLineSettingsChange?.invoke(selected, thickness, style)
                },
                onDismiss = { showPicker = false },
                isDarkTheme = isDarkTheme,
                thickness = thickness,
                style = style,
                onLineSettingsChange = onLineSettingsChange
            )
        }
    }
}

@Composable
fun ColorBoxIndicator(
    color: Color,
    onColorChange: (Color) -> Unit,
    isDarkTheme: Boolean,
    thickness: Int = 1,
    style: Int = 0,
    onLineSettingsChange: ((Color, Int, Int) -> Unit)? = null
) {
    ColorBox(
        color = color,
        onColorChange = onColorChange,
        isDarkTheme = isDarkTheme,
        modifier = Modifier
            .size(32.dp, 28.dp),
        thickness = thickness,
        style = style,
        onLineSettingsChange = onLineSettingsChange
    )
}

@Composable
fun LineOptionBox(
    color: Color,
    thickness: Int,
    style: Int,
    onLineSettingsChange: (Color, Int, Int) -> Unit,
    textColor: Color,
    borderColor: Color,
    isDarkTheme: Boolean
) {
    var showPicker by remember { mutableStateOf(false) }
    
    Box {
        Surface(
            modifier = Modifier
                .width(100.dp)
                .height(34.dp)
                .clickable { showPicker = true },
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, borderColor),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Carré de couleur
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(color, RoundedCornerShape(4.dp))
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Ligne de preview
                val thicknessDp = lineThicknessDp(thickness)
                Canvas(modifier = Modifier.weight(1f).height(thicknessDp.dp)) {
                    val strokeWidth = thicknessDp.dp.toPx()
                    val pathEffect = when(style) {
                        1 -> PathEffect.dashPathEffect(floatArrayOf(strokeWidth * 4, strokeWidth * 2), 0f)
                        2 -> PathEffect.dashPathEffect(floatArrayOf(0.1f, strokeWidth * 2.5f), 0f)
                        else -> null
                    }
                    val cap = if (style == 2) StrokeCap.Round else StrokeCap.Butt
                    
                    drawLine(
                        color = color,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = strokeWidth,
                        pathEffect = pathEffect,
                        cap = cap
                    )
                }
            }
        }
        
        if (showPicker) {
            ColorPickerPopup(
                selectedColor = color,
                onColorSelected = { selected ->
                    onLineSettingsChange(selected, thickness, style)
                },
                onDismiss = { showPicker = false },
                isDarkTheme = isDarkTheme,
                thickness = thickness,
                style = style,
                onLineSettingsChange = onLineSettingsChange
            )
        }
    }
}

@Composable
fun LineOptionBoxIndicator(
    color: Color,
    thickness: Int,
    style: Int,
    onLineSettingsChange: (Color, Int, Int) -> Unit,
    textColor: Color,
    borderColor: Color,
    isDarkTheme: Boolean
) {
    LineOptionBox(color, thickness, style, onLineSettingsChange, textColor, borderColor, isDarkTheme)
}

@Composable
fun OpacitySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(10.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val thumbRadius = size.height / 2f
                    val effectiveWidth = size.width - 2 * thumbRadius
                    val newValue = ((offset.x - thumbRadius) / effectiveWidth).coerceIn(0f, 1f)
                    onValueChange(newValue)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val thumbRadius = size.height / 2f
                    val effectiveWidth = size.width - 2 * thumbRadius
                    val newValue = ((change.position.x - thumbRadius) / effectiveWidth).coerceIn(0f, 1f)
                    onValueChange(newValue)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            val w = size.width
            val radius = h / 2f
            
            val trackPath = Path().apply {
                addRoundRect(RoundRect(left = 0f, top = 0f, right = w, bottom = h, cornerRadius = CornerRadius(radius, radius)))
            }

            clipPath(trackPath) {
                val cellSize = 4.dp.toPx()
                for (x in 0..(w / cellSize).toInt()) {
                    for (y in 0..(h / cellSize).toInt()) {
                        if ((x + y) % 2 == 0) drawRect(color = Color.LightGray.copy(alpha = 0.5f), topLeft = Offset(x * cellSize, y * cellSize), size = Size(cellSize, cellSize))
                    }
                }
                drawRect(brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0f), color.copy(alpha = 1f))), size = Size(w, h))
            }
            drawRoundRect(color = Color.Black.copy(alpha = 0.3f), cornerRadius = CornerRadius(radius, radius), style = Stroke(width = 1.dp.toPx()))
            
            val thumbRadius = h / 2f
            val thumbX = thumbRadius + value * (w - 2 * thumbRadius)
            
            drawCircle(color = Color.Black, radius = thumbRadius, center = Offset(thumbX, h / 2f))
            drawCircle(color = Color.White, radius = thumbRadius - 1.dp.toPx(), center = Offset(thumbX, h / 2f))
        }
    }
}

@Composable
fun ColorPickerPopup(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean,
    thickness: Int = 1,
    style: Int = 0,
    onLineSettingsChange: ((Color, Int, Int) -> Unit)? = null,
    positionX: Int = -260,
    positionY: Int = 40,
    screenHeight: Float = 0f,
    popupHeight: Float = 0f,
    barHeight: Float = 0f
) {
    val context = LocalContext.current
    var pickerMode by remember { mutableStateOf("Palette") }
    // Charger les carreaux personnalisés depuis SharedPreferences
    val customSwatches = remember {
        mutableStateListOf<Color>().apply {
            addAll(loadCustomSwatches(context))
        }
    }
    var hsv by remember {
        val hsvArr = FloatArray(3)
        android.graphics.Color.colorToHSV(selectedColor.toArgb(), hsvArr)
        mutableStateOf(Triple(hsvArr[0], hsvArr[1], hsvArr[2]))
    }

    val bgColor = if (isDarkTheme) Color(0xFF1e222d) else Color.White
    val borderColor = if (isDarkTheme) Color(0xFF2a2e39) else Color(0xFFE0E3EB)
    val textColor = if (isDarkTheme) Color(0xFFd1d4dc) else Color(0xFF131722)
    val accentColor = Color(0xFF2962ff)
    val density = LocalDensity.current
    val sectionGap = 10.dp
    val smallGap = 6.dp

    Popup(onDismissRequest = onDismiss, alignment = Alignment.TopStart, offset = IntOffset(positionX, positionY)) {
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(0.5.dp, borderColor),
            modifier = Modifier.width(270.dp)
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                if (pickerMode == "Palette") {
                    // Palette simplifiee: 10 couleurs par ligne
                    val colorPalette = listOf(
                        listOf(Color.White, Color(0xFFD1D4DC), Color(0xFFB2B5BE), Color(0xFF9094A1), Color(0xFF787B86), Color(0xFF5D606B), Color(0xFF434651), Color(0xFF2A2E39), Color(0xFF131722), Color.Black),
                        listOf(Color(0xFFff4a68), Color(0xFFff9800), Color(0xFFffeb3b), Color(0xFF4caf50), Color(0xFF00bcd4), Color(0xFF2196f3), Color(0xFF3f51b5), Color(0xFF9c27b0), Color(0xFFe91e63), Color(0xFFf06292))
                    )
                    colorPalette.forEach { row ->
                        Row(modifier = Modifier.wrapContentWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { color ->
                                val isSelected = selectedColor.copy(alpha = 1f) == color
                                val borderThickness = if (isSelected) 2.dp else 1.dp
                                val borderCol = if (isSelected) accentColor else borderColor.copy(0.35f)
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(color, shape = RectangleShape)
                                        .border(width = borderThickness, color = borderCol, shape = RectangleShape)
                                        .clickable { onColorSelected(color.copy(alpha = selectedColor.alpha)) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = borderColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        customSwatches.forEach { swatch ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(24.dp)
                                    .background(swatch, RoundedCornerShape(4.dp))
                                    .clickable {
                                        onColorSelected(swatch.copy(alpha = selectedColor.alpha))
                                    }
                            )
                        }
                        IconButton(
                            onClick = { pickerMode = "Custom" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Ajouter une couleur", tint = textColor)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Opacity", color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OpacitySlider(value = selectedColor.alpha, onValueChange = { onColorSelected(selectedColor.copy(alpha = it)) }, color = selectedColor.copy(alpha = 1f), modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .width(54.dp)
                                .height(32.dp)
                                .border(1.dp, borderColor, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "${(selectedColor.alpha * 100).toInt()}%", color = textColor, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }

                    if (onLineSettingsChange != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Thickness", color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(modifier = Modifier.fillMaxWidth().height(36.dp).border(1.dp, borderColor, RoundedCornerShape(4.dp))) {
                            (1..4).forEach { t ->
                                val isSelected = thickness == t
                                val tDp = lineThicknessDp(t)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(if (isSelected) (if(isDarkTheme) Color.White.copy(0.1f) else Color.Black.copy(0.1f)) else Color.Transparent)
                                        .clickable { onLineSettingsChange(selectedColor, t, style) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .height(tDp.dp)
                                            .background(if (isSelected) (if(isDarkTheme) Color.White else Color.Black) else textColor.copy(0.7f))
                                    )
                                }
                                if (t < 4) VerticalDivider(color = borderColor)
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            (1..4).forEach { t ->
                                val px = with(density) { lineThicknessDp(t).dp.toPx() }
                                val pxRounded = kotlin.math.round(px * 10f) / 10f
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${pxRounded} px",
                                        color = textColor.copy(alpha = 0.75f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Line style", color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(modifier = Modifier.fillMaxWidth().height(36.dp).border(1.dp, borderColor, RoundedCornerShape(4.dp))) {
                            listOf(0, 1, 2).forEach { s ->
                                val isSelected = style == s
                                val thicknessDp = lineThicknessDp(thickness)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(if (isSelected) (if(isDarkTheme) Color.White.copy(0.1f) else Color.Black.copy(0.1f)) else Color.Transparent)
                                        .clickable { onLineSettingsChange(selectedColor, thickness, s) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxWidth(0.6f).height(thicknessDp.dp)) {
                                        val strokeWidth = thicknessDp.dp.toPx()
                                        val pathEffect = when(s) {
                                            1 -> PathEffect.dashPathEffect(floatArrayOf(strokeWidth * 4, strokeWidth * 2), 0f)
                                            2 -> PathEffect.dashPathEffect(floatArrayOf(0.1f, strokeWidth * 2.5f), 0f)
                                            else -> null
                                        }
                                        val cap = if (s == 2) StrokeCap.Round else StrokeCap.Butt
                                        
                                        drawLine(
                                            color = if (isSelected) (if(isDarkTheme) Color.White else Color.Black) else textColor.copy(0.7f),
                                            start = Offset(0f, size.height / 2),
                                            end = Offset(size.width, size.height / 2),
                                            strokeWidth = strokeWidth,
                                            pathEffect = pathEffect,
                                            cap = cap
                                        )
                                    }
                                }
                                if (s < 2) VerticalDivider(color = borderColor)
                            }
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(32.dp).background(Color(android.graphics.Color.HSVToColor((selectedColor.alpha * 255).toInt(), floatArrayOf(hsv.first, hsv.second, hsv.third))), RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.width(8.dp))
                        var hexText by remember { mutableStateOf("#" + Integer.toHexString(selectedColor.toArgb()).uppercase().takeLast(6)) }
                        OutlinedTextField(value = hexText, onValueChange = { hexText = it; try { if(it.length == 7) onColorSelected(Color(android.graphics.Color.parseColor(it)).copy(alpha = selectedColor.alpha)) } catch(e: Exception) {} }, textStyle = TextStyle(color = textColor, fontSize = 14.sp), modifier = Modifier.width(100.dp).height(48.dp), singleLine = true)
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                val newColor = selectedColor.copy(alpha = 1f)
                                if (!customSwatches.contains(newColor)) {
                                    if (customSwatches.size >= 7) customSwatches.removeAt(0)
                                    customSwatches.add(newColor)
                                }
                                saveCustomSwatches(context, customSwatches)
                                pickerMode = "Palette"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Add", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.height(180.dp)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().pointerInput(Unit) { detectDragGestures { change, dragAmount -> val newS = (hsv.second + dragAmount.x / size.width).coerceIn(0f, 1f); val newV = (hsv.third - dragAmount.y / size.height).coerceIn(0f, 1f); hsv = Triple(hsv.first, newS, newV); onColorSelected(Color(android.graphics.Color.HSVToColor((selectedColor.alpha * 255).toInt(), floatArrayOf(hsv.first, newS, newV)))) } }.pointerInput(Unit) { detectTapGestures { offset -> val newS = (offset.x / size.width).coerceIn(0f, 1f); val newV = (1f - offset.y / size.height).coerceIn(0f, 1f); hsv = Triple(hsv.first, newS, newV); onColorSelected(Color(android.graphics.Color.HSVToColor((selectedColor.alpha * 255).toInt(), floatArrayOf(hsv.first, newS, newV)))) } }) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRect(color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, 1f, 1f))))
                                drawRect(brush = Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
                                drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                                val x = hsv.second * size.width; val y = (1f - hsv.third) * size.height
                                drawCircle(Color.Black, radius = 10.dp.toPx(), center = Offset(x, y), style = Stroke(width = 2.dp.toPx()))
                                drawCircle(Color.White, radius = 8.dp.toPx(), center = Offset(x, y), style = Stroke(width = 2.dp.toPx()))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.width(24.dp).fillMaxHeight().pointerInput(Unit) { detectDragGestures { change, dragAmount -> val newH = (hsv.first + dragAmount.y / size.height * 360f).coerceIn(0f, 360f); hsv = Triple(newH, hsv.second, hsv.third); onColorSelected(Color(android.graphics.Color.HSVToColor((selectedColor.alpha * 255).toInt(), floatArrayOf(newH, hsv.second, hsv.third)))) } }.pointerInput(Unit) { detectTapGestures { offset -> val newH = (offset.y / size.height * 360f).coerceIn(0f, 360f); hsv = Triple(newH, hsv.second, hsv.third); onColorSelected(Color(android.graphics.Color.HSVToColor((selectedColor.alpha * 255).toInt(), floatArrayOf(newH, hsv.second, hsv.third)))) } }) {
                            val hues = (0..360).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it.toFloat(), 1f, 1f))) }
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRect(Brush.verticalGradient(hues))
                                val y = (hsv.first / 360f) * size.height
                                drawRect(Color.Black, topLeft = Offset(-2.dp.toPx(), y - 2.dp.toPx()), size = Size(size.width + 4.dp.toPx(), 4.dp.toPx()), style = Stroke(2.dp.toPx()))
                            }
                        }
                    }
                }
            }
        }
    }
}
