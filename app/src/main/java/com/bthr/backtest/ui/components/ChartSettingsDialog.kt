package com.bthr.backtest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LegendToggle
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bthr.backtest.model.BackgroundType
import com.bthr.backtest.model.ChartSettings
import com.bthr.backtest.model.GridLines
import com.bthr.backtest.model.VisibilityMode

@Composable
fun ChartSettingsDialog(
    onDismiss: () -> Unit,
    onApply: (ChartSettings) -> Unit,
    initialSettings: ChartSettings,
    isDarkTheme: Boolean = true
) {
    var settings by remember { mutableStateOf(initialSettings) }
    var currentView by remember { mutableStateOf("Main") }

    val colorScheme = MaterialTheme.colorScheme
    val bgColor = colorScheme.surface
    val textColor = colorScheme.onSurface
    val borderColor = colorScheme.outline
    
    val primaryButtonBg = if (isDarkTheme) Color.White else Color.Black
    val primaryButtonText = if (isDarkTheme) Color.Black else Color.White

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = bgColor
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentView == "Main") {
                        Text(
                            text = "Configurations",
                            color = textColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { currentView = "Main" }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                            }
                            Text(
                                text = when(currentView) {
                                    "Symbol" -> "Symbole"
                                    "Scales" -> "Échelles et lignes"
                                    "Canvas" -> "Toile"
                                    else -> ""
                                },
                                color = textColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                    }
                }

                HorizontalDivider(color = borderColor)

                // Content
                Box(modifier = Modifier.weight(1f)) {
                    when (currentView) {
                        "Main" -> {
                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                MenuListItem("Symbole", Icons.Default.CandlestickChart, textColor, borderColor) { currentView = "Symbol" }
                                MenuListItem("Échelles et lignes", Icons.Default.LegendToggle, textColor, borderColor) { currentView = "Scales" }
                                MenuListItem("Toile", Icons.Default.Edit, textColor, borderColor) { currentView = "Canvas" }
                            }
                        }
                        "Symbol" -> SymbolSettingsView(settings, { settings = it }, textColor, borderColor, bgColor, isDarkTheme)
                        "Scales" -> ScalesSettingsView(settings, { settings = it }, textColor, borderColor, bgColor, isDarkTheme)
                        "Canvas" -> CanvasSettingsView(settings, { settings = it }, textColor, borderColor, bgColor, isDarkTheme)
                    }
                }

                HorizontalDivider(color = borderColor)

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        var showDefaultMenu by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { showDefaultMenu = true }, 
                            shape = RoundedCornerShape(6.dp), 
                            modifier = Modifier.size(44.dp, 36.dp),
                            contentPadding = PaddingValues(0.dp),
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = showDefaultMenu, 
                            onDismissRequest = { showDefaultMenu = false }, 
                            modifier = Modifier.background(bgColor).border(1.dp, borderColor)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Appliquer les paramètres par défaut", color = textColor, fontSize = 14.sp) }, 
                                onClick = { 
                                    // Use the current application theme to determine defaults
                                    settings = if (isDarkTheme) ChartSettings.dark() else ChartSettings.light()
                                    showDefaultMenu = false 
                                }
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(36.dp),
                            border = BorderStroke(1.dp, borderColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                        ) {
                            Text("Annuler", fontSize = 14.sp, fontWeight = FontWeight.Normal)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { onApply(settings) }, 
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryButtonBg,
                                contentColor = primaryButtonText
                            ), 
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("D'accord", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuListItem(label: String, icon: ImageVector, textColor: Color, borderColor: Color, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, color = textColor, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = textColor.copy(alpha = 0.5f))
        }
        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = borderColor.copy(alpha = 0.5f))
    }
}

@Composable
fun SymbolSettingsView(settings: ChartSettings, onSettingsChange: (ChartSettings) -> Unit, textColor: Color, borderColor: Color, bgColor: Color, isDarkTheme: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        SectionHeader("BOUGIES", textColor)
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(
                checked = settings.colorizeBarsBasedOnPrevClose, 
                onCheckedChange = { onSettingsChange(settings.copy(colorizeBarsBasedOnPrevClose = it)) },
                colors = CheckboxDefaults.colors(checkedColor = colorScheme.primary)
            )
            Text("Coloriser les Barres selon la Clôture Précédente", color = textColor, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        CandleSettingRow("Corps", settings.bodyEnabled, { onSettingsChange(settings.copy(bodyEnabled = it)) }, settings.upColor, settings.downColor, { onSettingsChange(settings.copy(upColor = it)) }, { onSettingsChange(settings.copy(downColor = it)) }, textColor, isDarkTheme)
        Spacer(modifier = Modifier.height(12.dp))
        CandleSettingRow("Bordures", settings.bordersEnabled, { onSettingsChange(settings.copy(bordersEnabled = it)) }, settings.upBorderColor, settings.downBorderColor, { onSettingsChange(settings.copy(upBorderColor = it)) }, { onSettingsChange(settings.copy(downBorderColor = it)) }, textColor, isDarkTheme)
        Spacer(modifier = Modifier.height(12.dp))
        CandleSettingRow("Mèche", settings.wickEnabled, { onSettingsChange(settings.copy(wickEnabled = it)) }, settings.upWickColor, settings.downWickColor, { onSettingsChange(settings.copy(upWickColor = it)) }, { onSettingsChange(settings.copy(downWickColor = it)) }, textColor, isDarkTheme)
        
        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader("MODIFICATION DES DONNÉES", textColor)
        
        SettingRow("Précision", textColor) {
            SimpleDropdown(selected = "${settings.precision} décimale${if(settings.precision > 1) "s" else ""}", options = listOf("0 décimale", "1 décimale", "2 décimales", "3 décimales", "4 décimales"), onSelect = { onSettingsChange(settings.copy(precision = it.split(" ")[0].toInt())) }, textColor = textColor, borderColor = borderColor, bgColor = bgColor, modifier = Modifier.width(180.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        SettingRow("Fuseau horaire", textColor) {
            SimpleDropdown(selected = "(UTC+1) Lagos", options = listOf("(UTC) UTC", "(UTC+1) Lagos", "(UTC+1) Paris"), onSelect = { onSettingsChange(settings.copy(timezone = it)) }, textColor = textColor, borderColor = borderColor, bgColor = bgColor, modifier = Modifier.width(180.dp))
        }
    }
}

@Composable
fun ScalesSettingsView(settings: ChartSettings, onSettingsChange: (ChartSettings) -> Unit, textColor: Color, borderColor: Color, bgColor: Color, isDarkTheme: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        SectionHeader("ECHELLE DE PRIX", textColor)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(
                checked = settings.lastPriceLineVisible, 
                onCheckedChange = { onSettingsChange(settings.copy(lastPriceLineVisible = it)) },
                colors = CheckboxDefaults.colors(checkedColor = colorScheme.primary)
            )
            Text("Ligne du dernier prix", color = textColor, fontSize = 14.sp, modifier = Modifier.width(140.dp))
            Spacer(modifier = Modifier.weight(1f))
            SimpleDropdown(selected = if(settings.lastPriceLineVisible) "Valeur et ligne" else "Masqué", options = listOf("Masqué", "Valeur et ligne"), onSelect = { onSettingsChange(settings.copy(lastPriceLineVisible = it == "Valeur et ligne")) }, textColor = textColor, borderColor = borderColor, bgColor = bgColor, modifier = Modifier.width(180.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(
                checked = settings.prevDayCloseVisible, 
                onCheckedChange = { onSettingsChange(settings.copy(prevDayCloseVisible = it)) },
                colors = CheckboxDefaults.colors(checkedColor = colorScheme.primary)
            )
            Text("Ligne du prix de clôture précédent", color = textColor, fontSize = 14.sp, modifier = Modifier.width(140.dp))
            Spacer(modifier = Modifier.weight(1f))
            SimpleDropdown(selected = if(settings.prevDayCloseVisible) "Valeur et ligne" else "Masqué", options = listOf("Masqué", "Valeur et ligne"), onSelect = { onSettingsChange(settings.copy(prevDayCloseVisible = it == "Valeur et ligne")) }, textColor = textColor, borderColor = borderColor, bgColor = bgColor, modifier = Modifier.width(180.dp))
        }
    }
}

@Composable
fun LabelCheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Checkbox(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = colorScheme.primary)
        )
        Text(label, color = colorScheme.onBackground, fontSize = 14.sp)
    }
}

@Composable
fun CanvasSettingsView(settings: ChartSettings, onSettingsChange: (ChartSettings) -> Unit, textColor: Color, borderColor: Color, bgColor: Color, isDarkTheme: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        SectionHeader("STYLES DE BASE DES GRAPHIQUES", textColor)
        SettingRow("Arrière-Plan", textColor) {
            SimpleDropdown(selected = if(settings.backgroundType == BackgroundType.SOLID) "Uni" else "Dégradé", options = listOf("Uni", "Dégradé"), onSelect = { onSettingsChange(settings.copy(backgroundType = if(it == "Uni") BackgroundType.SOLID else BackgroundType.GRADIENT)) }, textColor = textColor, borderColor = borderColor, bgColor = bgColor, modifier = Modifier.width(140.dp))
            Spacer(modifier = Modifier.width(8.dp))
            ColorBox(settings.backgroundColor, { onSettingsChange(settings.copy(backgroundColor = it)) }, isDarkTheme)
        }
        Spacer(modifier = Modifier.height(12.dp))
        SettingRow("Lignes de la grille", textColor) {
            SimpleDropdown(selected = when(settings.gridLines) { GridLines.BOTH -> "Vert and horz"; GridLines.VERTICAL -> "Vert uniquement"; GridLines.HORIZONTAL -> "Horz uniquement"; else -> "Aucun" }, options = listOf("Vert and horz", "Vert uniquement", "Horz uniquement", "Aucun"), onSelect = { onSettingsChange(settings.copy(gridLines = when(it) { "Vert and horz" -> GridLines.BOTH; "Vert uniquement" -> GridLines.VERTICAL; "Horz uniquement" -> GridLines.HORIZONTAL; else -> GridLines.NONE })) }, textColor = textColor, borderColor = borderColor, bgColor = bgColor, modifier = Modifier.width(140.dp))
            Spacer(modifier = Modifier.width(8.dp))
            ColorBox(
                color = settings.verticalGridColor,
                onColorChange = { onSettingsChange(settings.copy(verticalGridColor = it)) },
                isDarkTheme = isDarkTheme,
                thickness = settings.verticalGridThickness,
                style = settings.verticalGridStyle,
                onLineSettingsChange = { c, t, s -> onSettingsChange(settings.copy(verticalGridColor = c, verticalGridThickness = t, verticalGridStyle = s)) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            ColorBox(
                color = settings.horizontalGridColor,
                onColorChange = { onSettingsChange(settings.copy(horizontalGridColor = it)) },
                isDarkTheme = isDarkTheme,
                thickness = settings.horizontalGridThickness,
                style = settings.horizontalGridStyle,
                onLineSettingsChange = { c, t, s -> onSettingsChange(settings.copy(horizontalGridColor = c, horizontalGridThickness = t, horizontalGridStyle = s)) }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(
                checked = settings.sessionBreaksEnabled, 
                onCheckedChange = { onSettingsChange(settings.copy(sessionBreaksEnabled = it)) },
                colors = CheckboxDefaults.colors(checkedColor = colorScheme.primary)
            )
            Text("Arrêts de Session", color = textColor, fontSize = 14.sp, modifier = Modifier.width(108.dp))
            Spacer(modifier = Modifier.weight(1f))
            LineOptionBox(settings.sessionBreaksColor, settings.sessionBreaksThickness, settings.sessionBreaksStyle, { c, t, s -> onSettingsChange(settings.copy(sessionBreaksColor = c, sessionBreaksThickness = t, sessionBreaksStyle = s)) }, textColor, borderColor, isDarkTheme)
        }
        Spacer(modifier = Modifier.height(12.dp))
        SettingRow("Séparateurs de volets", textColor) { ColorBox(settings.paneSeparatorColor, { onSettingsChange(settings.copy(paneSeparatorColor = it)) }, isDarkTheme) }
        Spacer(modifier = Modifier.height(12.dp))
        SettingRow("Mire", textColor) {
            LineOptionBox(settings.crosshairColor, settings.crosshairThickness, settings.crosshairStyle, { c, t, s -> onSettingsChange(settings.copy(crosshairColor = c, crosshairThickness = t, crosshairStyle = s)) }, textColor, borderColor, isDarkTheme)
        }
        Spacer(modifier = Modifier.height(12.dp))
        SettingRow("Filigrane", textColor) {
            SimpleDropdown(selected = if(settings.watermarkVisible) "Visible" else "Masqué", options = listOf("Masqué", "Visible"), onSelect = { onSettingsChange(settings.copy(watermarkVisible = it == "Visible")) }, textColor = textColor, borderColor = borderColor, bgColor = bgColor, modifier = Modifier.width(140.dp))
            Spacer(modifier = Modifier.width(8.dp))
            ColorBox(settings.watermarkColor, { onSettingsChange(settings.copy(watermarkColor = it)) }, isDarkTheme)
        }
        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader("BOUTONS", textColor)
        SettingRow("Navigation", textColor) { VisibilityDropdown(settings.navigationButtonsMode, { onSettingsChange(settings.copy(navigationButtonsMode = it)) }, textColor, borderColor, bgColor) }
        Spacer(modifier = Modifier.height(12.dp))
        SettingRow("Volet", textColor) { VisibilityDropdown(settings.paneButtonsMode, { onSettingsChange(settings.copy(paneButtonsMode = it)) }, textColor, borderColor, bgColor) }
        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader("MARGES", textColor)
        MarginInputRow("Haut", settings.marginTopPercent, { onSettingsChange(settings.copy(marginTopPercent = it)) }, "%", textColor, borderColor)
        Spacer(modifier = Modifier.height(12.dp))
        MarginInputRow("Bas", settings.marginBottomPercent, { onSettingsChange(settings.copy(marginBottomPercent = it)) }, "%", textColor, borderColor)
        Spacer(modifier = Modifier.height(12.dp))
        MarginInputRow("Droite", settings.marginRightBars, { onSettingsChange(settings.copy(marginRightBars = it)) }, "barres", textColor, borderColor)
    }
}

@Composable
fun SectionHeader(text: String, color: Color) {
    Text(text, color = color.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun SettingRow(label: String, textColor: Color, content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = textColor, fontSize = 14.sp, modifier = Modifier.width(120.dp))
        Spacer(modifier = Modifier.weight(1f))
        content()
    }
}

@Composable
fun MarginInputRow(label: String, value: Int, onValueChange: (Int) -> Unit, unit: String, textColor: Color, borderColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = textColor, fontSize = 14.sp, modifier = Modifier.width(120.dp))
        Spacer(modifier = Modifier.weight(1f))
        var textValue by remember(value) { mutableStateOf(value.toString()) }
        BasicTextField(
            value = textValue,
            onValueChange = {
                textValue = it
                it.toIntOrNull()?.let { onValueChange(it) }
            },
            textStyle = TextStyle(color = textColor, fontSize = 14.sp),
            cursorBrush = SolidColor(textColor),
            modifier = Modifier
                .width(60.dp)
                .height(32.dp)
                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(unit, color = textColor, fontSize = 14.sp)
    }
}

@Composable
fun CandleSettingRow(label: String, enabled: Boolean, onCheckedChange: (Boolean) -> Unit, upColor: Color, downColor: Color, onUpColorChange: (Color) -> Unit, onDownColorChange: (Color) -> Unit, textColor: Color, isDarkTheme: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(
            checked = enabled, 
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = colorScheme.primary)
        )
        Text(label, color = textColor, fontSize = 14.sp, modifier = Modifier.width(80.dp))
        Spacer(modifier = Modifier.weight(1f))
        ColorBox(upColor, onUpColorChange, isDarkTheme)
        Spacer(modifier = Modifier.width(8.dp))
        ColorBox(downColor, onDownColorChange, isDarkTheme)
    }
}

@Composable
fun VisibilityDropdown(mode: VisibilityMode, onModeChange: (VisibilityMode) -> Unit, textColor: Color, borderColor: Color, bgColor: Color) {
    SimpleDropdown(
        selected = when(mode) {
            VisibilityMode.ALWAYS_VISIBLE -> "Toujours visible"
            VisibilityMode.VISIBLE_ON_MOUSE_OVER -> "Visible au survol de la souris"
            VisibilityMode.ALWAYS_INVISIBLE -> "Toujours invisible"
        },
        options = listOf("Toujours visible", "Visible au survol de la souris", "Toujours invisible"),
        onSelect = {
            onModeChange(when(it) {
                "Toujours visible" -> VisibilityMode.ALWAYS_VISIBLE
                "Visible au survol de la souris" -> VisibilityMode.VISIBLE_ON_MOUSE_OVER
                else -> VisibilityMode.ALWAYS_INVISIBLE
            })
        },
        textColor = textColor,
        borderColor = borderColor,
        bgColor = bgColor,
        modifier = Modifier.width(220.dp)
    )
}

@Composable
fun SimpleDropdown(
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    textColor: Color,
    borderColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedCard(
            onClick = { expanded = true },
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, borderColor),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selected, color = textColor, fontSize = 14.sp)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textColor)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(bgColor).border(1.dp, borderColor)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = textColor, fontSize = 14.sp) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
