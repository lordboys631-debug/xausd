package com.bthr.backtest

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bthr.backtest.model.Candle
import com.bthr.backtest.model.Indicator
import com.bthr.backtest.model.ChartSettings
import com.bthr.backtest.model.Timeframe
import com.bthr.backtest.model.DrawingTool
import com.bthr.backtest.model.SimpleTrendLine
import com.bthr.backtest.ui.components.CandlestickChart
import com.bthr.backtest.ui.components.ChartSettingsDialog
import com.bthr.backtest.ui.components.IndicatorSettingsDialog
import com.bthr.backtest.ui.components.DrawingToolsMenu
import com.bthr.backtest.ui.components.IndicatorsMenu
import com.bthr.backtest.ui.components.ThemeToggle
import com.bthr.backtest.ui.theme.BacktestTheme
import com.bthr.backtest.util.CandleUtil
import com.bthr.backtest.util.CsvParser
import com.bthr.backtest.util.DrawingUseMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Migration one-shot: applique le nouveau default de mire si l'utilisateur
        // est encore sur l'ancien default (noir/blanc, pointille, epaisseur 1).
        migrateCrosshairDefaultsIfNeeded(getSharedPreferences("chart_prefs", Context.MODE_PRIVATE))

        setContent {
            val context = LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences("chart_prefs", Context.MODE_PRIVATE) }
            val systemInDarkTheme = isSystemInDarkTheme()
            val density = LocalDensity.current
            
            var isDarkTheme by rememberSaveable { 
                mutableStateOf(sharedPrefs.getBoolean("is_dark_theme", systemInDarkTheme)) 
            }
            
            var chartSettings by remember { 
                mutableStateOf(loadSettings(sharedPrefs, isDarkTheme)) 
            }

            var activeIndicators by remember { 
                mutableStateOf<List<Indicator>>(
                    listOf(Indicator.Volume())
                )
            }

            var editingIndicator by remember { mutableStateOf<Indicator?>(null) }

            BacktestTheme(darkTheme = isDarkTheme) {
                val colorScheme = MaterialTheme.colorScheme
                var displayedCandles by remember { mutableStateOf<List<Candle>>(emptyList()) }
                var zoomLevel by rememberSaveable { mutableFloatStateOf(1f) }
                var scrollOffset by rememberSaveable { mutableFloatStateOf(0f) }
                var isLoading by remember { mutableStateOf(true) }
                var showIndicatorsMenu by remember { mutableStateOf(false) }
                var showDrawingToolsMenu by remember { mutableStateOf(false) }
                var currentDrawingTool by remember { mutableStateOf(DrawingTool.NONE) }
                var drawingUseMode by rememberSaveable {
                    mutableStateOf(
                        if (sharedPrefs.getString("drawing_use_mode", DrawingUseMode.REPEAT.name) == DrawingUseMode.SINGLE.name) {
                            DrawingUseMode.SINGLE
                        } else {
                            DrawingUseMode.REPEAT
                        }
                    )
                }
                var selectedTimeframe by rememberSaveable { mutableStateOf(Timeframe.H1) }
                var showTimeframeMenu by remember { mutableStateOf(false) }
                val selectedSymbol = "XAUUSD"
                var showSettingsMenu by remember { mutableStateOf(false) }
                var showChartSettingsDialog by remember { mutableStateOf(false) }

            var favoriteTools by remember {
                mutableStateOf(
                    sharedPrefs.getString("favorite_tools", "")?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
                )
            }
            var favoriteIndicators by remember {
                mutableStateOf(
                    sharedPrefs.getString("favorite_indicators", "")?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
                )
            }
            var activateDrawingModeTrigger by remember { mutableIntStateOf(0) }
    var selectedSimpleTool by remember { mutableStateOf<DrawingTool>(DrawingTool.NONE) }
    var isIntentionalDrawingActivation by remember { mutableStateOf(false) }

            // Persistent drawing lines storage
            var simpleTrendLines by remember { mutableStateOf<List<SimpleTrendLine>>(emptyList()) }
            var arrows by remember { mutableStateOf<List<SimpleTrendLine>>(emptyList()) }
            var extendedLines by remember { mutableStateOf<List<SimpleTrendLine>>(emptyList()) }
            var rays by remember { mutableStateOf<List<SimpleTrendLine>>(emptyList()) }

                // Reset intentional activation flag after it's used
                LaunchedEffect(activateDrawingModeTrigger) {
                    if (activateDrawingModeTrigger > 0) {
                        // Reset the flag after a short delay to allow the LaunchedEffect in CandlestickChart to process it
                        kotlinx.coroutines.delay(100)
                        isIntentionalDrawingActivation = false
                    }
                }

                LaunchedEffect(selectedSymbol, selectedTimeframe) {
                    isLoading = true
                    try {
                        val candles = withContext(Dispatchers.IO) {
                            val specificFileName = "${selectedSymbol.lowercase()}_${selectedTimeframe.displayName.lowercase()}.csv"
                            var result = CsvParser.loadCandlesFromAssets(context, specificFileName)
                            if (result.isEmpty()) {
                                val baseFileName = "${selectedSymbol.lowercase()}_h1.csv"
                                var baseData = CsvParser.loadCandlesFromAssets(context, baseFileName)
                                if (baseData.isEmpty()) {
                                    val fallbackName = "${selectedSymbol.lowercase()}.csv"
                                    baseData = CsvParser.loadCandlesFromAssets(context, fallbackName)
                                }
                                if (baseData.isEmpty()) baseData = CsvParser.loadCandlesFromAssets(context, "data.csv")
                                if (baseData.isNotEmpty()) result = CandleUtil.aggregate(baseData, selectedTimeframe)
                            }
                            if (result.isEmpty()) {
                                val generatedCandles = mutableListOf<Candle>()
                                var currentPrice = 2350f
                                val intervalMs = selectedTimeframe.minutes * 60000L
                                for (i in 0 until 1000) {
                                    val open = currentPrice
                                    val close = open + (Math.random().toFloat() - 0.5f) * 10f
                                    generatedCandles.add(Candle(System.currentTimeMillis() - (1000 - i) * intervalMs, open, maxOf(open, close) + 2f, minOf(open, close) - 2f, close, (Math.random().toFloat() * 1000f)))
                                    currentPrice = close
                                }
                                result = generatedCandles
                            }
                            result
                        }
                        displayedCandles = candles
                    } catch (e: Exception) { e.printStackTrace() } finally { isLoading = false }
                }

                if (showChartSettingsDialog) {
                    ChartSettingsDialog(
                        onDismiss = { showChartSettingsDialog = false },
                        onApply = { newSettings ->
                            chartSettings = newSettings
                            saveSettings(sharedPrefs, newSettings, isDarkTheme)
                            showChartSettingsDialog = false
                        },
                        initialSettings = chartSettings,
                        isDarkTheme = isDarkTheme
                    )
                }

                if (editingIndicator != null) {
                    IndicatorSettingsDialog(
                        indicator = editingIndicator!!,
                        onDismiss = { editingIndicator = null },
                        onSave = { updated ->
                            activeIndicators = activeIndicators.map { if (it.id == updated.id) updated else it }
                            editingIndicator = null
                        },
                        isDarkTheme = isDarkTheme
                    )
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = colorScheme.background
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Surface(
                            color = colorScheme.surface,
                            modifier = Modifier.fillMaxWidth().height(48.dp).border(0.5.dp, colorScheme.outline)
                        ) {
                            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        @Suppress("DEPRECATION")
                                        Text(selectedSymbol, color = colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                                VerticalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp), color = colorScheme.outline)
                                Box {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showTimeframeMenu = true }) {
                                        Text(selectedTimeframe.displayName, color = colorScheme.onSurface, fontSize = 13.sp)
                                        Icon(Icons.Default.KeyboardArrowDown, null, tint = colorScheme.onSurface, modifier = Modifier.size(25.dp))
                                    }
                                    DropdownMenu(
                                        expanded = showTimeframeMenu,
                                        onDismissRequest = { showTimeframeMenu = false },
                                        modifier = Modifier.background(colorScheme.surface).border(0.5.dp, colorScheme.outline)
                                    ) {
                                        Timeframe.entries.forEach { tf -> 
                                            DropdownMenuItem(
                                                text = { Text(tf.displayName, color = colorScheme.onSurface) },
                                                onClick = { 
                                                    selectedTimeframe = tf
                                                    scrollOffset = 0f 
                                                    showTimeframeMenu = false 
                                                }
                                            )
                                        }
                                    }
                                }
                                VerticalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp), color = colorScheme.outline)
                                Box {
                                    IconButton(onClick = { 
                                        showIndicatorsMenu = true
                                        showDrawingToolsMenu = false
                                    }) { 
                                        Icon(painterResource(id = R.drawable.ic_indicators), "Indicators", tint = colorScheme.onSurface, modifier = Modifier.size(25.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Box {
                                    IconButton(onClick = { 
                                        showDrawingToolsMenu = true
                                        showIndicatorsMenu = false
                                    }) {
                                        Icon(
                                            painterResource(id = R.drawable.ic_drawing_tools),
                                            "Outils de dessin",
                                            tint = colorScheme.onSurface,
                                            modifier = Modifier.size(25.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                ThemeToggle(
                                    isDark = isDarkTheme,
                                    onToggle = { newIsDarkTheme: Boolean ->
                                        val oldDefaults = if (isDarkTheme) ChartSettings.dark() else ChartSettings.light()
                                        val newDefaults = if (newIsDarkTheme) ChartSettings.dark() else ChartSettings.light()
                                        val updatedSettings = rethemeChartSettings(chartSettings, oldDefaults, newDefaults)
                                        isDarkTheme = newIsDarkTheme
                                        chartSettings = updatedSettings
                                        saveSettings(context.getSharedPreferences("chart_prefs", Context.MODE_PRIVATE), updatedSettings, newIsDarkTheme)
                                    }
                                )
                            }
                        }
                        var chartAreaWidthPx by remember { mutableFloatStateOf(0f) }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .onSizeChanged { chartAreaWidthPx = it.width.toFloat() }
                        ) {
                            val widthPx = chartAreaWidthPx
                            val priceWidthPx = with(density) { 55.dp.toPx() }
                            val chartWidthPx = (widthPx - priceWidthPx).coerceAtLeast(1f)
                            
                            // Limite dézoom style TradingView: min 2px par bougie
                            val minCandleWidthPx = 2f
                            val maxVisibleFromPx = (chartWidthPx / minCandleWidthPx).coerceAtLeast(10f)
                            val dynamicMaxVisibleCount = minOf(maxVisibleFromPx, displayedCandles.size.toFloat()).coerceAtLeast(10f)
                            val baseVisibleCount = 60f
                            val displayCount = (baseVisibleCount / zoomLevel).coerceIn(2f, dynamicMaxVisibleCount)

                            val maxScroll = (displayedCandles.size.toFloat() - 1f).coerceAtLeast(0f)
                            val minScroll = -(displayCount * 0.9f + chartSettings.marginRightBars)

                            if (isLoading) { 
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                                    CircularProgressIndicator(color = colorScheme.primary) 
                                } 
                            } else {
                                Box(Modifier.fillMaxSize()) {
                                CandlestickChart(
                                    allCandles = displayedCandles,
                                    modifier = Modifier.fillMaxSize(),
                                    indicators = activeIndicators,
                                    scrollOffset = scrollOffset,
                                    displayCount = displayCount,
                                    symbol = selectedSymbol,
                                    timeframe = selectedTimeframe.displayName,
                                    onZoom = { factor -> 
                                        zoomLevel = (zoomLevel * factor).coerceIn(baseVisibleCount / dynamicMaxVisibleCount, 30f)
                                    },
                                    onPan = { delta -> scrollOffset = (scrollOffset + delta).coerceIn(minScroll, maxScroll) },
                                    onSettingsRequest = { showChartSettingsDialog = true },
                                    onIndicatorSettingsRequest = { ind -> editingIndicator = ind },
                                    onIndicatorToggleVisibility = { ind -> 
                                        activeIndicators = activeIndicators.map { 
                                            if (it.id == ind.id) { 
                                                when(it) { 
                                                    is Indicator.SMA -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.EMA -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.HMA -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.VWAP -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.BollingerBands -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.ATRBands -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.Alligator -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.Ichimoku -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.RSI -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.MACD -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.Stochastic -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.Volume -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.ATR -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.Supertrend -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.Sessions -> it.copy(isVisible = !it.isVisible)
                                                    is Indicator.Ribbon -> it.copy(isVisible = !it.isVisible)
                                                } 
                                            } else it 
                                        } 
                                    },
                                    onIndicatorRemove = { ind -> activeIndicators = activeIndicators.filter { it.id != ind.id } },
                                    onResetHorizontalZoom = { 
                                        zoomLevel = 1f
                                        scrollOffset = 0f 
                                    },
                                    settings = chartSettings,
                                    favoriteTools = favoriteTools,
                                    onFavoriteToolsChange = { newFavorites ->
                                        favoriteTools = newFavorites
                                        sharedPrefs.edit().putString("favorite_tools", newFavorites.joinToString(",")).apply()
                                    },
                                    activeDrawingTool = currentDrawingTool,
                                    drawingUseMode = drawingUseMode,
                                    onDrawingUseModeChange = { mode ->
                                        drawingUseMode = mode
                                        sharedPrefs.edit().putString("drawing_use_mode", mode.name).apply()
                                    },
                                    onDrawingToolUsed = { currentDrawingTool = DrawingTool.NONE },
                                    onOpenChartSettings = { showChartSettingsDialog = true },
                                    activateDrawingModeTrigger = activateDrawingModeTrigger,
                                        selectedSimpleTool = selectedSimpleTool,
                                        isIntentionalDrawingActivation = isIntentionalDrawingActivation,
                                        simpleTrendLines = simpleTrendLines,
                                        arrows = arrows,
                                        extendedLines = extendedLines,
                                        rays = rays,
                                        onLinesChanged = { simple, arrow, extended, ray ->
                                            simpleTrendLines = simple
                                            arrows = arrow
                                            extendedLines = extended
                                            rays = ray
                                        },
                                        onFavoritesBarToolSelected = { tool ->
                                            selectedSimpleTool = tool
                                            activateDrawingModeTrigger++
                                        }
                                )
                                // ── Drawing Tools floating overlay on chart grid ──
                                if (showDrawingToolsMenu) {
                                    DrawingToolsMenu(
                                        onToolSelected = { tool ->
                                            currentDrawingTool = tool
                                            showDrawingToolsMenu = false
                                        },
                                        onDismissRequest = { showDrawingToolsMenu = false },
                                        isDarkTheme = isDarkTheme,
                                        favoriteTools = favoriteTools,
                                        onFavoritesChange = { newFavorites -> favoriteTools = newFavorites },
                                        onActivateSimpleDrawingMode = { tool ->
                                            android.util.Log.d("DrawingToolsMenu", "Tool selected from menu: ${tool.name}")
                                            android.util.Log.d("DrawingToolsMenu", "Marking as intentional activation")
                                            isIntentionalDrawingActivation = true
                                            selectedSimpleTool = tool
                                            activateDrawingModeTrigger++
                                        }
                                    )
                                }
                                // ── Indicators floating overlay on chart grid ──
                                if (showIndicatorsMenu) {
                                    IndicatorsMenu(
                                        onIndicatorSelected = { newInd ->
                                            activeIndicators = activeIndicators + newInd
                                        },
                                        onDismissRequest = { showIndicatorsMenu = false },
                                        isDarkTheme = isDarkTheme,
                                        favoriteIndicators = favoriteIndicators,
                                        onFavoritesChange = { newFavorites -> favoriteIndicators = newFavorites }
                                    )
                                }
                                } // end Box overlay
                            }
                        }
                    }
                }
            }
        }
    }

    private fun migrateCrosshairDefaultsIfNeeded(prefs: android.content.SharedPreferences) {
        val migrationVersion = prefs.getInt("settings_migration_version", 0)
        if (migrationVersion >= 1) return

        val hasColor = prefs.contains("crosshair_color")
        val hasStyle = prefs.contains("crosshair_style")
        val hasThickness = prefs.contains("crosshair_thickness")

        val savedColor = if (hasColor) prefs.getInt("crosshair_color", Color.Black.toArgb()) else null
        val savedStyle = if (hasStyle) prefs.getInt("crosshair_style", 1) else null
        val savedThickness = if (hasThickness) prefs.getInt("crosshair_thickness", 1) else null

        val looksLikeLegacyDefault =
            (!hasColor && !hasStyle && !hasThickness) ||
            (savedStyle == 1 && savedThickness == 1 && (savedColor == Color.Black.toArgb() || savedColor == Color.White.toArgb()))

        prefs.edit().apply {
            if (looksLikeLegacyDefault) {
                val defaults = ChartSettings.light()
                putInt("crosshair_color", defaults.crosshairColor.toArgb())
                putInt("crosshair_style", defaults.crosshairStyle)
                putInt("crosshair_thickness", defaults.crosshairThickness)
            }
            putInt("settings_migration_version", 1)
            apply()
        }
    }

    private fun rethemeChartSettings(
        current: ChartSettings,
        oldDefaults: ChartSettings,
        newDefaults: ChartSettings
    ): ChartSettings {
        fun pick(currentColor: Color, oldColor: Color, newColor: Color): Color {
            // Si la couleur suit encore le default de l'ancien theme, on la bascule.
            return if (currentColor == oldColor) newColor else currentColor
        }

        return current.copy(
            backgroundColor = pick(current.backgroundColor, oldDefaults.backgroundColor, newDefaults.backgroundColor),
            backgroundGradientColor = pick(current.backgroundGradientColor, oldDefaults.backgroundGradientColor, newDefaults.backgroundGradientColor),
            verticalGridColor = pick(current.verticalGridColor, oldDefaults.verticalGridColor, newDefaults.verticalGridColor),
            horizontalGridColor = pick(current.horizontalGridColor, oldDefaults.horizontalGridColor, newDefaults.horizontalGridColor),
            paneSeparatorColor = pick(current.paneSeparatorColor, oldDefaults.paneSeparatorColor, newDefaults.paneSeparatorColor),
            scaleTextColor = pick(current.scaleTextColor, oldDefaults.scaleTextColor, newDefaults.scaleTextColor),
            scaleLinesColor = pick(current.scaleLinesColor, oldDefaults.scaleLinesColor, newDefaults.scaleLinesColor),
            watermarkColor = pick(current.watermarkColor, oldDefaults.watermarkColor, newDefaults.watermarkColor),
            crosshairColor = pick(current.crosshairColor, oldDefaults.crosshairColor, newDefaults.crosshairColor)
        )
    }

    private fun loadSettings(prefs: android.content.SharedPreferences, isDark: Boolean): ChartSettings {
        val defaultSettings = if (isDark) ChartSettings.dark() else ChartSettings.light()
        
        fun getSavedColor(key: String, default: Color): Color {
            if (!prefs.contains(key)) return default
            val colorVal = prefs.getInt(key, default.toArgb())
            return Color(colorVal)
        }

        return ChartSettings(
            backgroundColor = getSavedColor("bg_color", defaultSettings.backgroundColor),
            backgroundGradientColor = getSavedColor("bg_grad_color", defaultSettings.backgroundGradientColor),
            backgroundType = com.bthr.backtest.model.BackgroundType.entries[prefs.getInt("bg_type", defaultSettings.backgroundType.ordinal)],
            upColor = getSavedColor("up_color", defaultSettings.upColor),
            downColor = getSavedColor("down_color", defaultSettings.downColor),
            upBorderColor = getSavedColor("up_border_color", defaultSettings.upBorderColor),
            downBorderColor = getSavedColor("down_border_color", defaultSettings.downBorderColor),
            upWickColor = getSavedColor("up_wick_color", defaultSettings.upWickColor),
            downWickColor = getSavedColor("down_wick_color", defaultSettings.downWickColor),
            wickEnabled = prefs.getBoolean("wick_enabled", defaultSettings.wickEnabled),
            bodyEnabled = prefs.getBoolean("body_enabled", defaultSettings.bodyEnabled),
            bordersEnabled = prefs.getBoolean("borders_enabled", defaultSettings.bordersEnabled),
            gridLines = com.bthr.backtest.model.GridLines.entries[prefs.getInt("grid_lines", defaultSettings.gridLines.ordinal)],
            showGrid = prefs.getBoolean("show_grid", defaultSettings.showGrid),
            horizontalGridColor = getSavedColor("h_grid_color", defaultSettings.horizontalGridColor),
            verticalGridColor = getSavedColor("v_grid_color", defaultSettings.verticalGridColor),
            horizontalGridStyle = prefs.getInt("h_grid_style", defaultSettings.horizontalGridStyle),
            verticalGridStyle = prefs.getInt("v_grid_style", defaultSettings.verticalGridStyle),
            horizontalGridThickness = prefs.getInt("h_grid_thickness", defaultSettings.horizontalGridThickness),
            verticalGridThickness = prefs.getInt("v_grid_thickness", defaultSettings.verticalGridThickness),
            scaleLinesColor = getSavedColor("scale_lines_color", defaultSettings.scaleLinesColor),
            scaleTextColor = getSavedColor("scale_text_color", defaultSettings.scaleTextColor),
            scaleTextSize = prefs.getInt("scale_text_size", defaultSettings.scaleTextSize),
            crosshairColor = getSavedColor("crosshair_color", defaultSettings.crosshairColor),
            crosshairStyle = prefs.getInt("crosshair_style", defaultSettings.crosshairStyle),
            crosshairThickness = prefs.getInt("crosshair_thickness", defaultSettings.crosshairThickness),
            watermarkVisible = prefs.getBoolean("watermark_visible", defaultSettings.watermarkVisible),
            watermarkColor = getSavedColor("watermark_color", defaultSettings.watermarkColor),
            marginRightBars = prefs.getInt("margin_right", defaultSettings.marginRightBars),
            marginTopPercent = prefs.getInt("margin_top", defaultSettings.marginTopPercent),
            marginBottomPercent = prefs.getInt("margin_bottom", defaultSettings.marginBottomPercent)
        )
    }

    private fun saveSettings(prefs: android.content.SharedPreferences, s: ChartSettings, isDark: Boolean) {
        prefs.edit().apply {
            putInt("bg_color", s.backgroundColor.toArgb())
            putInt("bg_grad_color", s.backgroundGradientColor.toArgb())
            putInt("bg_type", s.backgroundType.ordinal)
            putInt("up_color", s.upColor.toArgb())
            putInt("down_color", s.downColor.toArgb())
            putInt("up_border_color", s.upBorderColor.toArgb())
            putInt("down_border_color", s.downBorderColor.toArgb())
            putInt("up_wick_color", s.upWickColor.toArgb())
            putInt("down_wick_color", s.downWickColor.toArgb())
            putBoolean("wick_enabled", s.wickEnabled)
            putBoolean("body_enabled", s.bodyEnabled)
            putBoolean("borders_enabled", s.bordersEnabled)
            putInt("grid_lines", s.gridLines.ordinal)
            putBoolean("show_grid", s.showGrid)
            putInt("h_grid_color", s.horizontalGridColor.toArgb())
            putInt("v_grid_color", s.verticalGridColor.toArgb())
            putInt("h_grid_style", s.horizontalGridStyle)
            putInt("v_grid_style", s.verticalGridStyle)
            putInt("h_grid_thickness", s.horizontalGridThickness)
            putInt("v_grid_thickness", s.verticalGridThickness)
            putInt("scale_lines_color", s.scaleLinesColor.toArgb())
            putInt("scale_text_color", s.scaleTextColor.toArgb())
            putInt("scale_text_size", s.scaleTextSize)
            putInt("crosshair_color", s.crosshairColor.toArgb())
            putInt("crosshair_style", s.crosshairStyle)
            putInt("crosshair_thickness", s.crosshairThickness)
            putBoolean("watermark_visible", s.watermarkVisible)
            putInt("watermark_color", s.watermarkColor.toArgb())
            putInt("margin_right", s.marginRightBars)
            putInt("margin_top", s.marginTopPercent)
            putInt("margin_bottom", s.marginBottomPercent)
            putBoolean("is_dark_theme", isDark)
            apply()
        }
    }
}
