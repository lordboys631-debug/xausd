package com.bthr.backtest.ui.components

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bthr.backtest.R
import com.bthr.backtest.model.DrawingTool
import com.bthr.backtest.model.SimpleTrendLine
import kotlin.math.roundToInt


// ============================================================
// 2. MENU PRINCIPAL DES OUTILS (GRILLE 3 COLONNES)
// ============================================================

@Composable
fun DrawingToolsMenu(
    onToolSelected: (DrawingTool) -> Unit,
    onDismissRequest: () -> Unit,
    isDarkTheme: Boolean = true,
    favoriteTools: Set<String> = emptySet(),
    onFavoritesChange: (Set<String>) -> Unit = {},
    onActivateSimpleDrawingMode: (DrawingTool) -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("chart_prefs", Context.MODE_PRIVATE) }

    var favorites by remember { mutableStateOf(favoriteTools) }
    var dragOffset by remember { mutableStateOf(Offset(16f, 16f)) }
    var searchQuery by remember { mutableStateOf("") }

    val colors = getDrawingToolsMenuColors(isDarkTheme)

    // Plein écran pour les outils de dessin
    val gridColumns = 3
    val gridHorizontalPadding = 20.dp
    val horizontalSpacing = 12.dp
    val verticalSpacing = 12.dp
    val cardAspectRatio = 0.8f
    val titleBarHeight = 45.dp
    val searchBarHeight = 50.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Barre de titre avec X bouton à gauche
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(titleBarHeight)
                    .background(colors.titleBgColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // X bouton à gauche pour fermer
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = colors.textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Texte "Dessins" au centre
                Text(
                    text = "Dessins",
                    color = colors.textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // Espacement vide à droite pour équilibrer
                Spacer(modifier = Modifier.size(24.dp))
            }

            // Barre de recherche
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(searchBarHeight)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.cardBgColor, RoundedCornerShape(8.dp))
                        .border(1.dp, colors.borderColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Rechercher",
                        tint = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { newValue -> searchQuery = newValue },
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = if (isDarkTheme) Color.White else Color.Black,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(if (isDarkTheme) Color.White else Color.Black),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Chercher",
                                    color = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f),
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            } else {
                                innerTextField()
                            }
                        }
                    )
                    
                    if (searchQuery.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Effacer",
                                tint = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            val allTools = listOf(
                DrawingTool.MEASURE,
                DrawingTool.ERASER,
                DrawingTool.EXTENDED_LINE,
                DrawingTool.TREND_LINE,
                DrawingTool.RAY,
                DrawingTool.HORIZONTAL_LINE,
                DrawingTool.HORIZONTAL_RAY,
                DrawingTool.VERTICAL_LINE,
                DrawingTool.CROSS_LINE,
                DrawingTool.PARALLEL_CHANNEL,
                DrawingTool.FIB_RETRACEMENT,
                DrawingTool.LONG_POSITION,
                DrawingTool.SHORT_POSITION,
                DrawingTool.RECTANGLE,
                DrawingTool.BRUSH,
                DrawingTool.CIRCLE,
                DrawingTool.PATH,
                DrawingTool.ARROW,
                DrawingTool.TEXT,
                DrawingTool.ANCHORED_TEXT
            )
            
            val toolsToDisplay = if (searchQuery.isEmpty()) {
                allTools
            } else {
                allTools.filter { tool ->
                    tool.displayName.contains(searchQuery, ignoreCase = true) ||
                    tool.name.contains(searchQuery, ignoreCase = true)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = gridHorizontalPadding, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing)
            ) {
                items(toolsToDisplay) { tool ->
                    DrawingToolCard(
                        tool = tool,
                        isFavorite = favorites.contains(tool.name),
                        textColor = colors.textColor,
                        iconColor = if (isDarkTheme) Color.White else Color.Black,
                        cardBgColor = colors.cardBgColor,
                        borderColor = colors.borderColor,
                        onFavoriteToggle = {
                            favorites = if (favorites.contains(tool.name)) favorites - tool.name else favorites + tool.name
                            onFavoritesChange(favorites)
                            sharedPrefs.edit().putString("favorite_tools", favorites.joinToString(",")).apply()
                        },
                        onToolSelected = {
                            android.util.Log.d("DrawingToolsMenu", "Tool selected: ${tool.name}")
                            // Activate simple drawing mode for trend line, extended line, ray, arrow, and measure tools instead of DrawingManager
                            if (tool == DrawingTool.TREND_LINE || tool == DrawingTool.EXTENDED_LINE || tool == DrawingTool.RAY || tool == DrawingTool.ARROW || tool == DrawingTool.MEASURE) {
                                android.util.Log.d("DrawingToolsMenu", "Activating simple drawing mode for: ${tool.name}")
                                onActivateSimpleDrawingMode(tool)
                                onDismissRequest()
                            } else {
                                android.util.Log.d("DrawingToolsMenu", "Using normal tool selection for: ${tool.name}")
                                onToolSelected(tool)
                                onDismissRequest()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawingToolCard(
    tool: DrawingTool,
    isFavorite: Boolean,
    textColor: Color,
    iconColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    onFavoriteToggle: () -> Unit,
    onToolSelected: () -> Unit
) {
    val context = LocalContext.current
    val titleFontSize = 12.sp
    val titleLineHeight = 14.sp
    val toolIconSize = 32.dp

    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1.0f).background(cardBgColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor.copy(0.25f), RoundedCornerShape(12.dp))
            .then(
                if (tool == DrawingTool.TREND_LINE || tool == DrawingTool.EXTENDED_LINE || tool == DrawingTool.RAY || tool == DrawingTool.ARROW || tool == DrawingTool.MEASURE) {
                    Modifier.clickable {
                        android.util.Log.d("DrawingToolCard", "Card clicked: ${tool.name}")
                        // Activate trend line, extended line, ray, or arrow tool
                        onToolSelected()
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = tool.iconRes),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(toolIconSize)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tool.displayName,
                color = textColor.copy(0.9f),
                fontSize = titleFontSize,
                lineHeight = titleLineHeight,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
        Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.TopEnd) {
            IconButton(onClick = onFavoriteToggle, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (isFavorite) Color(0xFFFFA500) else textColor.copy(0.25f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ============================================================
// 5. BARRE DE RÉGLAGES LIGNE DE TENDANCE SIMPLE (AVEC POPUPS)
// ============================================================

@Composable
fun SimpleTrendLineSettingsBar(
    selectedLine: SimpleTrendLine,
    chartWidthPx: Float,
    screenWidth: Float,
    screenHeight: Float,
    isDarkTheme: Boolean,
    onUpdate: (SimpleTrendLine) -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("chart_prefs", Context.MODE_PRIVATE) }
    val colors = getDrawingToolsMenuColors(isDarkTheme)

    var showColorPicker by remember { mutableStateOf(false) }
    var showThicknessMenu by remember { mutableStateOf(false) }
    var showStyleMenu by remember { mutableStateOf(false) }

    // Position draggable de la barre avec persistance
    var barOffset by remember {
        mutableStateOf(
            Offset(
                sharedPrefs.getFloat("trendline_bar_x", 100f),
                sharedPrefs.getFloat("trendline_bar_y", 100f)
            )
        )
    }

    // Constrain bar position to screen limits
    val barHeight = with(density) { 40.dp.toPx() }
    val maxY = screenHeight - barHeight // Bottom screen limit (time bar)

    barOffset = Offset(
        barOffset.x.coerceIn(0f, screenWidth - with(density) { 220.dp.toPx() }.coerceAtLeast(0f)),
        barOffset.y.coerceIn(0f, maxY.coerceAtLeast(0f))
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // --- BARRE PRINCIPALE ---
        Box(
            modifier = Modifier
                .offset { IntOffset(barOffset.x.roundToInt(), barOffset.y.roundToInt()) }
                .wrapContentWidth()
                .height(40.dp)
                .background(colors.bgColor, RoundedCornerShape(8.dp))
                .border(0.5.dp, colors.borderColor, RoundedCornerShape(8.dp))
         ) {
              Row(
                  horizontalArrangement = Arrangement.spacedBy(0.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                      .wrapContentWidth()
                      .fillMaxHeight()
                      .pointerInput(Unit) {
                          detectDragGestures(
                              onDragStart = { offset ->
                                  // Only start drag if clicking on the drag handle area (first ~60px)
                                  offset.x < 60f
                              },
                              onDrag = { change, dragAmount ->
                                  barOffset = Offset(barOffset.x + dragAmount.x, barOffset.y + dragAmount.y)
                                  sharedPrefs.edit()
                                      .putFloat("trendline_bar_x", barOffset.x)
                                      .putFloat("trendline_bar_y", barOffset.y)
                                      .apply()
                                  change.consume()
                              }
                          )
                      }
              ) {
                  // Drag Handle - padding avant
                  Column(
                      verticalArrangement = Arrangement.spacedBy(3.dp),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 0.dp, bottom = 0.dp)
                  ) {
                      DragHandle(gripColor = colors.textColor.copy(0.5f), dotSize = 3f, spacing = 3f)
                  }

                // Couleur
                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(40.dp)
                        .background(colors.bgColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .clickable { showColorPicker = !showColorPicker; showThicknessMenu = false; showStyleMenu = false }
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(selectedLine.color, RoundedCornerShape(3.dp))
                            .border(1.dp, colors.borderColor.copy(0.35f), RoundedCornerShape(3.dp))
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Épaisseur
                val displayThickness = when (selectedLine.strokeWidth) {
                    2f -> 1
                    5f -> 2
                    8f -> 3
                    11f -> 4
                    else -> selectedLine.strokeWidth.toInt()
                }
                Box(
                    modifier = Modifier
                        .width(35.dp)
                        .height(40.dp)
                        .background(colors.bgColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .clickable { showThicknessMenu = !showThicknessMenu; showColorPicker = false; showStyleMenu = false }
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${displayThickness} px", color = colors.textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Style (Canvas pour l'aperçu du trait)
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(40.dp)
                        .background(colors.bgColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .clickable { showStyleMenu = !showStyleMenu; showColorPicker = false; showThicknessMenu = false }
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(width = 28.dp, height = 8.dp)) {
                        val lineY = size.height / 2f
                        val pathEffect = when (selectedLine.lineStyle) {
                            1 -> PathEffect.dashPathEffect(floatArrayOf(6f, 6f), phase = 0f) // 3 dashes "- - -"
                            2 -> PathEffect.dashPathEffect(floatArrayOf(0f, 5f), phase = 0f) // 5 dots "....."
                            else -> null
                        }
                        drawLine(color = colors.textColor, start = Offset(0f, lineY), end = Offset(size.width, lineY), strokeWidth = 3f, pathEffect = pathEffect, cap = StrokeCap.Round)
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Verrouiller
                Box(
                    modifier = Modifier
                        .width(35.dp)
                        .height(40.dp)
                        .background(colors.bgColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .clickable { onUpdate(selectedLine.copy(isLocked = !selectedLine.isLocked)) }
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (selectedLine.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock",
                        tint = if (selectedLine.isLocked) Color(0xFF2962FF) else colors.textColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Supprimer
                Box(
                    modifier = Modifier
                        .width(35.dp)
                        .height(40.dp)
                        .background(colors.bgColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .clickable { 
                    android.util.Log.d("TrendLineDelete", "Delete icon clicked")
                    onDelete()
                }
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = "Delete",
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

         // --- POPUP COULEUR ---
         if (showColorPicker) {
             val barHeight = with(density) { 40.dp.toPx() }
             val popupHeight = with(density) { 130.dp.toPx() } // Reduced to match actual content height

             // Aligner avec le bord gauche de l'encadrement couleur
             // Position: 8dp start + 10dp handle + 4dp end + 4dp spacer = 26dp
             val colorBoxX = with(density) { 26.dp.toPx() }
             val popX = (barOffset.x + colorBoxX).roundToInt()

              // Vérifier l'espace disponible en bas et en haut
              val spaceBelow = screenHeight - (barOffset.y + barHeight)
              val spaceAbove = barOffset.y

              // Décider de la position: afficher en haut si pas assez d'espace en bas
              val canShowBelow = spaceBelow >= popupHeight
              val canShowAbove = spaceAbove >= popupHeight
              
              // Priorité: si assez d'espace en bas, afficher en bas, sinon en haut si possible
              val showAbove = !canShowBelow && canShowAbove
              
              val popY = if (showAbove) {
                  // Ajouter la hauteur de la barre pour faire monter le popup plus haut
                  val desiredY = barOffset.y - popupHeight - barHeight
                  val minY = 0f // Pas négatif, reste dans l'écran
                  desiredY.coerceAtLeast(minY).roundToInt()
              } else {
                  (barOffset.y + barHeight).roundToInt()
              }
                android.util.Log.d("PopupPos", "Color Picker - barOffset.y=${barOffset.y}, barHeight=$barHeight, popupHeight=$popupHeight, showAbove=$showAbove, popY=$popY")

             ColorPickerPopup(
                selectedColor = selectedLine.color,
                onColorSelected = { color ->
                    onUpdate(selectedLine.copy(color = color))
                },
                onDismiss = { showColorPicker = false },
                isDarkTheme = isDarkTheme,
                positionX = popX,
                positionY = popY,
                screenHeight = screenHeight,
                popupHeight = popupHeight,
                barHeight = barHeight
            )
        }

        // --- POPUP ÉPAISSEUR ---
        if (showThicknessMenu) {
            val popupWidth = with(density) { 140.dp.toPx() }
            // Height calculation: Title (16dp) + 4 items * (24dp each + padding) + Box padding (8dp) + borders
            val popupHeight = with(density) { 150.dp.toPx() }  // Augmented from 120dp to accommodate content
            val barHeight = with(density) { 40.dp.toPx() }

             // Aligner avec le bord gauche de l'encadrement épaisseur
             // Position: 26dp (drag) + 30dp (color) = 56dp
             val thicknessBoxX = with(density) { 56.dp.toPx() }
             val popX = (barOffset.x + thicknessBoxX).roundToInt()

             // Vérifier l'espace disponible en bas et en haut
             val spaceBelow = screenHeight - (barOffset.y + barHeight)
             val spaceAbove = barOffset.y

             // Décider de la position: afficher en haut si pas assez d'espace en bas
             val canShowBelow = spaceBelow >= popupHeight
             val canShowAbove = spaceAbove >= popupHeight
             
             // Priorité: si assez d'espace en bas, afficher en bas, sinon en haut si possible
             val showAbove = !canShowBelow && canShowAbove
            val popY = if (showAbove) {
                // Ajouter 5dp pour plus d'espacement
                val density = LocalDensity.current
                val additionalReduction = with(density) { 20.dp.toPx() }
                val extraSpacing = with(density) { 5.dp.toPx() }
                val reducedBarHeight = (barHeight * 0.5f) - additionalReduction + extraSpacing // Moitié - 20dp + 5dp
                val desiredY = barOffset.y - popupHeight - reducedBarHeight
                val minY = 0f // Pas négatif, reste dans l'écran
                desiredY.coerceAtLeast(minY).roundToInt()
            } else {
                (barOffset.y + barHeight).roundToInt()
            }
            android.util.Log.d("PopupPos", "Thickness - barOffset.y=${barOffset.y}, barHeight=$barHeight, popupHeight=$popupHeight, showAbove=$showAbove, popY=$popY")

             Box(
                 modifier = Modifier.offset { IntOffset(popX, popY) }.background(colors.bgColor, RoundedCornerShape(8.dp)).border(0.5.dp, colors.borderColor, RoundedCornerShape(8.dp))
             ) {
                 Column(verticalArrangement = Arrangement.spacedBy(0.dp), modifier = Modifier.padding(4.dp)) {
                     Text("Thickness", color = colors.textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    listOf(2f, 5f, 8f, 11f).forEach { thick ->
                        val displayValue = when (thick) {
                            2f -> 1
                            5f -> 2
                            8f -> 3
                            11f -> 4
                            else -> thick.toInt()
                        }
                        val visualThickness = when (thick) {
                            2f -> 1.dp
                            5f -> 2.dp
                            8f -> 3.dp
                            11f -> 4.dp
                            else -> thick.dp
                        }
                        Box(modifier = Modifier.width(120.dp).background(if (selectedLine.strokeWidth == thick) Color(0xFF2962FF).copy(0.35f) else Color.Transparent, RoundedCornerShape(5.dp)).clickable { onUpdate(selectedLine.copy(strokeWidth = thick)); showThicknessMenu = false }.padding(horizontal = 12.dp, vertical = 3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.width(30.dp).height(visualThickness).background(colors.textColor, RoundedCornerShape(visualThickness / 2)))
                                Text("${displayValue}px", color = colors.textColor, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- POPUP STYLE ---
        if (showStyleMenu) {
            val popupWidth = with(density) { 160.dp.toPx() }
             val popupHeight = with(density) { 120.dp.toPx() }
             val barHeight = with(density) { 40.dp.toPx() }

             // Aligner avec le bord gauche de l'encadrement style
             // Position: 26dp (drag) + 30dp (color) + 35dp (thickness) = 91dp
             val styleBoxX = with(density) { 91.dp.toPx() }
             val popX = (barOffset.x + styleBoxX).roundToInt()

              // Vérifier l'espace disponible en bas et en haut
              val spaceBelow = screenHeight - (barOffset.y + barHeight)
              val spaceAbove = barOffset.y

              // Décider de la position: afficher en haut si pas assez d'espace en bas
              val canShowBelow = spaceBelow >= popupHeight
              val canShowAbove = spaceAbove >= popupHeight
              
              // Priorité: si assez d'espace en bas, afficher en bas, sinon en haut si possible
              val showAbove = !canShowBelow && canShowAbove
               val popY = if (showAbove) {
                   // Ajouter 5dp pour plus d'espacement
                   val density = LocalDensity.current
                   val additionalReduction = with(density) { 20.dp.toPx() }
                   val extraSpacing = with(density) { 5.dp.toPx() }
                   val reducedBarHeight = (barHeight * 0.5f) - additionalReduction + extraSpacing // Moitié - 20dp + 5dp
                   val desiredY = barOffset.y - popupHeight - reducedBarHeight
                   val minY = 0f // Pas négatif, reste dans l'écran
                   desiredY.coerceAtLeast(minY).roundToInt()
               } else {
                   (barOffset.y + barHeight).roundToInt()
               }
               android.util.Log.d("PopupPos", "Style - barOffset.y=${barOffset.y}, barHeight=$barHeight, popupHeight=$popupHeight, showAbove=$showAbove, popY=$popY")

              Box(
                  modifier = Modifier.offset { IntOffset(popX, popY) }.background(colors.bgColor, RoundedCornerShape(8.dp)).border(0.5.dp, colors.borderColor, RoundedCornerShape(8.dp))
              ) {
                  Column(verticalArrangement = Arrangement.spacedBy(0.dp), modifier = Modifier.padding(4.dp)) {
                      Text("Line style", color = colors.textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    listOf(0 to "Solid", 1 to "Dashed", 2 to "Dotted").forEach { (style, label) ->
                        Box(modifier = Modifier.width(150.dp).background(if (selectedLine.lineStyle == style) Color(0xFF2962FF).copy(0.15f) else Color.Transparent, RoundedCornerShape(5.dp)).clickable { onUpdate(selectedLine.copy(lineStyle = style)); showStyleMenu = false }.padding(horizontal = 12.dp, vertical = 3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Canvas(modifier = Modifier.size(width = 30.dp, height = 10.dp)) {
                                    val eff = when (style) {
                                        1 -> PathEffect.dashPathEffect(floatArrayOf(6f, 6f), phase = 0f) // 3 dashes "- - -"
                                        2 -> PathEffect.dashPathEffect(floatArrayOf(0f, 5f), phase = 0f) // 5 dots "....."
                                        else -> null
                                    }
                                    drawLine(color = colors.textColor, start = Offset(0f, size.height / 2f), end = Offset(size.width, size.height / 2f), strokeWidth = 3f, pathEffect = eff, cap = StrokeCap.Round)
                                }
                                Text(label, color = colors.textColor.copy(0.85f), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

