package com.bthr.backtest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bthr.backtest.model.Indicator

@Composable
fun IndicatorSettingsDialog(
    indicator: Indicator,
    onDismiss: () -> Unit,
    onSave: (Indicator) -> Unit,
    isDarkTheme: Boolean = true
) {
    var currentIndicator by remember { mutableStateOf(indicator) }
    var selectedTab by remember { mutableStateOf("Inputs") }

    val colorScheme = MaterialTheme.colorScheme
    val bgColor = colorScheme.surface
    val textColor = colorScheme.onSurface
    val borderColor = colorScheme.outline

    val accentColor = if (isDarkTheme) Color.White else Color.Black
    val onAccentColor = if (isDarkTheme) Color.Black else Color.White

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(6.dp),
            color = bgColor
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val title = if (indicator is Indicator.Sessions) "Sessions" else indicator.name.split(" ")[0]
                    Text(
                        text = title,
                        color = textColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                    }
                }

                // Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    val tabs = listOf("Inputs", "Style", "Visibility")
                    tabs.forEach { tab ->
                        val isSelected = selectedTab == tab
                        Column(
                            modifier = Modifier
                                .padding(end = 24.dp)
                                .clickable { selectedTab = tab },
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = when(tab) {
                                    "Inputs" -> "Paramètres en Entrée"
                                    "Style" -> "Style"
                                    else -> "Visibilité"
                                },
                                color = if (isSelected) textColor else textColor.copy(alpha = 0.6f),
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            if (isSelected) {
                                Box(modifier = Modifier.height(3.dp).width(40.dp).background(accentColor))
                            }
                        }
                    }
                }

                HorizontalDivider(color = borderColor, thickness = 1.dp)

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    @Suppress("DEPRECATION")
                    when (selectedTab) {
                        "Inputs" -> InputsContent(currentIndicator, onUpdate = { currentIndicator = it }, textColor, borderColor, bgColor, accentColor)
                        "Style" -> StyleContent(currentIndicator, onUpdate = { currentIndicator = it }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
                        "Visibility" -> VisibilityContent(currentIndicator, onUpdate = { currentIndicator = it }, textColor, accentColor)
                    }
                }

                HorizontalDivider(color = borderColor, thickness = 1.dp)

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        var showDefaultMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showDefaultMenu = true }, modifier = Modifier.border(1.dp, borderColor, RoundedCornerShape(4.dp)).size(36.dp)) {
                            Icon(Icons.Default.MoreHoriz, null, tint = textColor)
                        }
                        DropdownMenu(expanded = showDefaultMenu, onDismissRequest = { showDefaultMenu = false }, modifier = Modifier.background(bgColor).border(1.dp, borderColor)) {
                            DropdownMenuItem(
                                text = { Text("Appliquer les paramètres par défaut", color = textColor, fontSize = 14.sp) },
                                onClick = { showDefaultMenu = false }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, borderColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
                            modifier = Modifier.height(36.dp)
                        ) {
                            @Suppress("DEPRECATION")
                            Text("Annuler", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { onSave(currentIndicator) },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            modifier = Modifier.height(36.dp)
                        ) {
                            @Suppress("DEPRECATION")
                            Text("D'accord", color = onAccentColor, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InputsContent(indicator: Indicator, onUpdate: (Indicator) -> Unit, textColor: Color, borderColor: Color, bgColor: Color, accentColor: Color) {
    when (val ind = indicator) {
        is Indicator.Volume -> {
            RadioButtonRow("Symbole principal du graphique", ind.useMainSymbol, { onUpdate(ind.copy(useMainSymbol = true)) }, textColor, accentColor)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                RadioButton(
                    selected = !ind.useMainSymbol,
                    onClick = { onUpdate(ind.copy(useMainSymbol = false)) },
                    colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = textColor.copy(0.4f))
                )
                Text("Un autre symbole", color = textColor, fontSize = 14.sp, modifier = Modifier.weight(1f))
                SettingsInputBox(
                    value = ind.otherSymbol,
                    onValueChange = { onUpdate(ind.copy(otherSymbol = it)) },
                    modifier = Modifier.width(120.dp),
                    textColor = textColor,
                    borderColor = borderColor,
                    enabled = !ind.useMainSymbol
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            InputRow("Longueur MA", ind.maLength.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.maLength) onUpdate(ind.copy(maLength = v)) } }, textColor, borderColor)
            SettingsDropdownRow("Volume MA", ind.maType, listOf("SMA", "EMA", "WMA"), { onUpdate(ind.copy(maType = it)) }, textColor, borderColor, bgColor)
            
            CheckboxRow("Couleur basée sur la clôture précédente", ind.colorBasedOnPreviousClose, { onUpdate(ind.copy(colorBasedOnPreviousClose = it)) }, textColor, accentColor)
            
            SettingsDropdownRow("Ligne de lissage", ind.smoothingLine, listOf("SMA", "EMA"), { onUpdate(ind.copy(smoothingLine = it)) }, textColor, borderColor, bgColor)
            InputRow("Longueur de lissage", ind.smoothingLength.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.smoothingLength) onUpdate(ind.copy(smoothingLength = v)) } }, textColor, borderColor)
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("VALEURS D'ENTRÉE", color = textColor.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            CheckboxRow("Entrées dans la ligne d'état", ind.showInputsInStatusLine, { onUpdate(ind.copy(showInputsInStatusLine = it)) }, textColor, accentColor)
        }
        is Indicator.RSI -> {
            InputRow("Longueur", ind.period.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.period) onUpdate(ind.copy(period = v)) } }, textColor, borderColor)
            SettingsDropdownRow("Source", ind.source, listOf("Close", "Open", "High", "Low"), { onUpdate(ind.copy(source = it)) }, textColor, borderColor, bgColor)
            SettingsDropdownRow("Type de MA", ind.maType, listOf("SMA", "EMA"), { onUpdate(ind.copy(maType = it)) }, textColor, borderColor, bgColor)
            InputRow("Longueur de la MA", ind.maPeriod.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.maPeriod) onUpdate(ind.copy(maPeriod = v)) } }, textColor, borderColor)
        }
        is Indicator.MACD -> {
            InputRow("Fast Period", ind.fastPeriod.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.fastPeriod) onUpdate(ind.copy(fastPeriod = v)) } }, textColor, borderColor)
            InputRow("Slow Period", ind.slowPeriod.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.slowPeriod) onUpdate(ind.copy(slowPeriod = v)) } }, textColor, borderColor)
            InputRow("Signal Period", ind.signalPeriod.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.signalPeriod) onUpdate(ind.copy(signalPeriod = v)) } }, textColor, borderColor)
        }
        is Indicator.Stochastic -> {
            InputRow("%K Period", ind.kPeriod.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.kPeriod) onUpdate(ind.copy(kPeriod = v)) } }, textColor, borderColor)
            InputRow("%K Smoothing", ind.kSmoothing.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.kSmoothing) onUpdate(ind.copy(kSmoothing = v)) } }, textColor, borderColor)
            InputRow("%D Period", ind.dPeriod.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.dPeriod) onUpdate(ind.copy(dPeriod = v)) } }, textColor, borderColor)
        }
        is Indicator.ATR -> {
            InputRow("Longueur", ind.period.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.period) onUpdate(ind.copy(period = v)) } }, textColor, borderColor)
        }
        is Indicator.ATRBands -> {
            InputRow("ATR Period", ind.period.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.period) onUpdate(ind.copy(period = v)) } }, textColor, borderColor)
            InputRow("ATR Band Scale Factor", ind.multiplier.toString(), { it.toFloatOrNull()?.let { v -> if(v != ind.multiplier) onUpdate(ind.copy(multiplier = v)) } }, textColor, borderColor)
            CheckboxRow("Show opposite bands for take-profit zones", ind.showTPBands, { onUpdate(ind.copy(showTPBands = it)) }, textColor, accentColor)
            if (ind.showTPBands) {
                InputRow("Take-Profit Scale Factor", ind.tpScaleFactor.toString(), { it.toFloatOrNull()?.let { v -> if(v != ind.tpScaleFactor) onUpdate(ind.copy(tpScaleFactor = v)) } }, textColor, borderColor)
            }
        }
        is Indicator.Supertrend -> {
            InputRow("ATR Period", ind.period.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.period) onUpdate(ind.copy(period = v)) } }, textColor, borderColor)
            InputRow("Multiplier", ind.multiplier.toString(), { it.toFloatOrNull()?.let { v -> if(v != ind.multiplier) onUpdate(ind.copy(multiplier = v)) } }, textColor, borderColor)
        }
        is Indicator.SMA -> {
            InputRow("Longueur", ind.period.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.period) onUpdate(ind.copy(period = v)) } }, textColor, borderColor)
            SettingsDropdownRow("Source", ind.source, listOf("Close", "Open", "High", "Low"), { onUpdate(ind.copy(source = it)) }, textColor, borderColor, bgColor)
        }
        is Indicator.EMA -> {
            InputRow("Longueur", ind.period.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.period) onUpdate(ind.copy(period = v)) } }, textColor, borderColor)
            SettingsDropdownRow("Source", ind.source, listOf("Close", "Open", "High", "Low"), { onUpdate(ind.copy(source = it)) }, textColor, borderColor, bgColor)
        }
        is Indicator.HMA -> {
            InputRow("Longueur", ind.period.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.period) onUpdate(ind.copy(period = v)) } }, textColor, borderColor)
            SettingsDropdownRow("Source", ind.source, listOf("Close", "Open", "High", "Low"), { onUpdate(ind.copy(source = it)) }, textColor, borderColor, bgColor)
        }
        is Indicator.BollingerBands -> {
            InputRow("Période", ind.period.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.period) onUpdate(ind.copy(period = v)) } }, textColor, borderColor)
            InputRow("StdDev", ind.stdDev.toString(), { it.toFloatOrNull()?.let { v -> if(v != ind.stdDev) onUpdate(ind.copy(stdDev = v)) } }, textColor, borderColor)
        }
        is Indicator.Alligator -> {
            InputRow("Jaw Period", ind.jawPeriod.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.jawPeriod) onUpdate(ind.copy(jawPeriod = v)) } }, textColor, borderColor)
            InputRow("Jaw Offset", ind.jawOffset.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.jawOffset) onUpdate(ind.copy(jawOffset = v)) } }, textColor, borderColor)
            InputRow("Teeth Period", ind.teethPeriod.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.teethPeriod) onUpdate(ind.copy(teethPeriod = v)) } }, textColor, borderColor)
            InputRow("Teeth Offset", ind.teethOffset.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.teethOffset) onUpdate(ind.copy(teethOffset = v)) } }, textColor, borderColor)
            InputRow("Lips Period", ind.lipsPeriod.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.lipsPeriod) onUpdate(ind.copy(lipsPeriod = v)) } }, textColor, borderColor)
            InputRow("Lips Offset", ind.lipsOffset.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.lipsOffset) onUpdate(ind.copy(lipsOffset = v)) } }, textColor, borderColor)
        }
        is Indicator.Ichimoku -> {
            InputRow("Tenkan Period", ind.tenkanPeriod.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.tenkanPeriod) onUpdate(ind.copy(tenkanPeriod = v)) } }, textColor, borderColor)
            InputRow("Kijun Period", ind.kijunPeriod.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.kijunPeriod) onUpdate(ind.copy(kijunPeriod = v)) } }, textColor, borderColor)
            InputRow("Senkou B Period", ind.senkouBPeriod.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.senkouBPeriod) onUpdate(ind.copy(senkouBPeriod = v)) } }, textColor, borderColor)
            InputRow("Displacement", ind.displacement.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.displacement) onUpdate(ind.copy(displacement = v)) } }, textColor, borderColor)
        }
        is Indicator.Sessions -> {
            SessionInputRow("Sydney", ind.showSydney, { onUpdate(ind.copy(showSydney = it)) }, ind.sydneyStart, { onUpdate(ind.copy(sydneyStart = it)) }, ind.sydneyEnd, { onUpdate(ind.copy(sydneyEnd = it)) }, textColor, borderColor, accentColor)
            SessionInputRow("Tokyo", ind.showTokyo, { onUpdate(ind.copy(showTokyo = it)) }, ind.tokyoStart, { onUpdate(ind.copy(tokyoStart = it)) }, ind.tokyoEnd, { onUpdate(ind.copy(tokyoEnd = it)) }, textColor, borderColor, accentColor)
            SessionInputRow("London", ind.showLondon, { onUpdate(ind.copy(showLondon = it)) }, ind.londonStart, { onUpdate(ind.copy(londonStart = it)) }, ind.londonEnd, { onUpdate(ind.copy(londonEnd = it)) }, textColor, borderColor, accentColor)
            SessionInputRow("New York", ind.showNewYork, { onUpdate(ind.copy(showNewYork = it)) }, ind.newYorkStart, { onUpdate(ind.copy(newYorkStart = it)) }, ind.newYorkEnd, { onUpdate(ind.copy(newYorkEnd = it)) }, textColor, borderColor, accentColor)
            
            Spacer(modifier = Modifier.height(16.dp))
            CheckboxRow("Show Labels", ind.showLabels, { onUpdate(ind.copy(showLabels = it)) }, textColor, accentColor)
            CheckboxRow("Show Background", ind.showBackground, { onUpdate(ind.copy(showBackground = it)) }, textColor, accentColor)
            InputRow("Opacity", ind.opacity.toString(), { it.toFloatOrNull()?.let { v -> if(v != ind.opacity) onUpdate(ind.copy(opacity = v)) } }, textColor, borderColor)
        }
        is Indicator.VWAP -> {
            SettingsDropdownRow("Anchor Period", ind.anchor, listOf("Session", "Week", "Month", "Quarter", "Year", "Decade", "Century"), { onUpdate(ind.copy(anchor = it)) }, textColor, borderColor, bgColor)
            SettingsDropdownRow("Source", ind.source, listOf("Close", "Open", "High", "Low", "HL2", "HLC3", "OHLC4"), { onUpdate(ind.copy(source = it)) }, textColor, borderColor, bgColor)
            CheckboxRow("Show Bands", ind.showBands, { onUpdate(ind.copy(showBands = it)) }, textColor, accentColor)
            if (ind.showBands) {
                InputRow("Band Multiplier #1", ind.bandMult1.toString(), { it.toFloatOrNull()?.let { v -> if(v != ind.bandMult1) onUpdate(ind.copy(bandMult1 = v)) } }, textColor, borderColor)
                InputRow("Band Multiplier #2", ind.bandMult2.toString(), { it.toFloatOrNull()?.let { v -> if(v != ind.bandMult2) onUpdate(ind.copy(bandMult2 = v)) } }, textColor, borderColor)
                InputRow("Band Multiplier #3", ind.bandMult3.toString(), { it.toFloatOrNull()?.let { v -> if(v != ind.bandMult3) onUpdate(ind.copy(bandMult3 = v)) } }, textColor, borderColor)
            }
        }
        is Indicator.Ribbon -> {
            CheckboxRow("Exponential MA", ind.isExponential, { onUpdate(ind.copy(isExponential = it)) }, textColor, accentColor)
            SettingsDropdownRow("Source", ind.source, listOf("Close", "Open", "High", "Low", "HL2", "HLC3", "OHLC4"), { onUpdate(ind.copy(source = it)) }, textColor, borderColor, bgColor)
            InputRow("Reference Period", ind.refPeriod.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.refPeriod) onUpdate(ind.copy(refPeriod = v)) } }, textColor, borderColor)
        }
    }
}

@Composable
fun SessionInputRow(
    name: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    startTime: String,
    onStartTimeChange: (String) -> Unit,
    endTime: String,
    onEndTimeChange: (String) -> Unit,
    textColor: Color,
    borderColor: Color,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = accentColor, uncheckedColor = textColor.copy(0.4f)),
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        
        // Session Name Box
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(34.dp)
                .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(name, color = textColor, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Start Time Box
        TimeInputBox(
            value = startTime,
            onValueChange = onStartTimeChange,
            modifier = Modifier.width(85.dp),
            textColor = textColor,
            borderColor = borderColor
        )

        Text(" — ", color = textColor, modifier = Modifier.padding(horizontal = 4.dp))

        // End Time Box
        TimeInputBox(
            value = endTime,
            onValueChange = onEndTimeChange,
            modifier = Modifier.width(85.dp),
            textColor = textColor,
            borderColor = borderColor
        )
    }
}

@Composable
fun TimeInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color,
    borderColor: Color
) {
    var textFieldValue by remember(value) { mutableStateOf(value) }

    BasicTextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValue = it
            onValueChange(it)
        },
        modifier = modifier
            .height(34.dp)
            .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
        textStyle = TextStyle(fontSize = 13.sp, color = textColor, textAlign = TextAlign.Start),
        singleLine = true,
        cursorBrush = SolidColor(textColor),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    innerTextField()
                }
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    )
}

@Composable
fun StyleContent(indicator: Indicator, onUpdate: (Indicator) -> Unit, textColor: Color, borderColor: Color, isDarkTheme: Boolean, bgColor: Color, accentColor: Color) {
    when (val ind = indicator) {
        is Indicator.Volume -> {
            CheckboxRow("Volume", ind.showVolume, { onUpdate(ind.copy(showVolume = it)) }, textColor, accentColor)
            Column(modifier = Modifier.padding(start = 32.dp)) {
                StyleRowSimple("En chute", ind.downColor, { onUpdate(ind.copy(downColor = it)) }, textColor, borderColor, bgColor)
                StyleRowSimple("En croissance", ind.upColor, { onUpdate(ind.copy(upColor = it)) }, textColor, borderColor, bgColor)
            }
            Spacer(modifier = Modifier.height(8.dp))
            StyleRowIndicator("Volume MA", ind.showVolumeMa, { onUpdate(ind.copy(showVolumeMa = it)) }, ind.volumeMaColor, ind.volumeMaThickness.toInt(), ind.volumeMaStyle, { c, t, s -> onUpdate(ind.copy(volumeMaColor = c, volumeMaThickness = t.toFloat(), volumeMaStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Smoothed MA", ind.showSmoothedMa, { onUpdate(ind.copy(showSmoothedMa = it)) }, ind.smoothedMaColor, ind.smoothedMaThickness.toInt(), ind.smoothedMaStyle, { c, t, s -> onUpdate(ind.copy(smoothedMaColor = c, smoothedMaThickness = t.toFloat(), smoothedMaStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
        }
        is Indicator.RSI -> {
            StyleRowIndicator("Tracé", ind.showRsi, { onUpdate(ind.copy(showRsi = it)) }, ind.color, ind.thickness.toInt(), ind.style, { c, t, s -> onUpdate(ind.copy(color = c, thickness = t.toFloat(), style = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Smoothed MA", ind.showMa, { onUpdate(ind.copy(showMa = it)) }, ind.maColor, ind.maThickness.toInt(), ind.maStyle, { c, t, s -> onUpdate(ind.copy(maColor = c, maThickness = t.toFloat(), maStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            Spacer(modifier = Modifier.height(8.dp))
            LevelStyleRow("Limite supérieure", ind.upperLevelVisible, { onUpdate(ind.copy(upperLevelVisible = it)) }, ind.upperLevelColor, { onUpdate(ind.copy(upperLevelColor = it)) }, ind.upperLevel, { onUpdate(ind.copy(upperLevel = it)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            LevelStyleRow("MiddleLimit", ind.middleLevelVisible, { onUpdate(ind.copy(middleLevelVisible = it)) }, ind.middleLevelColor, { onUpdate(ind.copy(middleLevelColor = it)) }, ind.middleLevel, { onUpdate(ind.copy(middleLevel = it)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            LevelStyleRow("Limite inférieure", ind.lowerLevelVisible, { onUpdate(ind.copy(lowerLevelVisible = it)) }, ind.lowerLevelColor, { onUpdate(ind.copy(lowerLevelColor = it)) }, ind.lowerLevel, { onUpdate(ind.copy(lowerLevel = it)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRow("Hlines Background", ind.backgroundVisible, { onUpdate(ind.copy(backgroundVisible = it)) }, ind.backgroundColor, { onUpdate(ind.copy(backgroundColor = it)) }, textColor, borderColor, isDarkTheme, bgColor, isBackground = true, accentColor = accentColor)
        }
        is Indicator.Stochastic -> {
            StyleRowIndicator("%K", true, {}, ind.kColor, ind.kThickness.toInt(), ind.kStyle, { c, t, s -> onUpdate(ind.copy(kColor = c, kThickness = t.toFloat(), kStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("%D", true, {}, ind.dColor, ind.dThickness.toInt(), ind.dStyle, { c, t, s -> onUpdate(ind.copy(dColor = c, dThickness = t.toFloat(), dStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            Spacer(modifier = Modifier.height(8.dp))
            LevelStyleRow("Upper Level", true, {}, ind.upperLevelColor, { onUpdate(ind.copy(upperLevelColor = it)) }, ind.upperLevel, { onUpdate(ind.copy(upperLevel = it)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            LevelStyleRow("Lower Level", true, {}, ind.lowerLevelColor, { onUpdate(ind.copy(lowerLevelColor = it)) }, ind.lowerLevel, { onUpdate(ind.copy(lowerLevel = it)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRow("Background", ind.backgroundVisible, { onUpdate(ind.copy(backgroundVisible = it)) }, ind.backgroundColor, { onUpdate(ind.copy(backgroundColor = it)) }, textColor, borderColor, isDarkTheme, bgColor, isBackground = true, accentColor = accentColor)
        }
        is Indicator.ATR -> {
            StyleRowIndicator("Couleur", true, {}, ind.color, ind.thickness.toInt(), ind.style, { c, t, s -> onUpdate(ind.copy(color = c, thickness = t.toFloat(), style = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
        }
        is Indicator.ATRBands -> {
            StyleRowIndicator("Upper ATR Band", true, {}, ind.upperColor, ind.upperThickness.toInt(), ind.upperStyle, { c, t, s -> onUpdate(ind.copy(upperColor = c, upperThickness = t.toFloat(), upperStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Lower ATR Band", true, {}, ind.lowerColor, ind.lowerThickness.toInt(), ind.lowerStyle, { c, t, s -> onUpdate(ind.copy(lowerColor = c, lowerThickness = t.toFloat(), lowerStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            if (ind.showTPBands) {
                StyleRowIndicator("Upper Take-Profit Band", true, {}, ind.tpUpperColor, ind.tpThickness.toInt(), 0, { c, t, s -> onUpdate(ind.copy(tpUpperColor = c, tpThickness = t.toFloat())) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
                StyleRowIndicator("Lower Take-Profit Band", true, {}, ind.tpLowerColor, ind.tpThickness.toInt(), 0, { c, t, s -> onUpdate(ind.copy(tpLowerColor = c, tpThickness = t.toFloat())) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            }
        }
        is Indicator.Supertrend -> {
            StyleRowSimple("Up Color", ind.upColor, { onUpdate(ind.copy(upColor = it)) }, textColor, borderColor, bgColor)
            StyleRowSimple("Down Color", ind.downColor, { onUpdate(ind.copy(downColor = it)) }, textColor, borderColor, bgColor)
            CheckboxRow("Fill Visible", ind.fillVisible, { onUpdate(ind.copy(fillVisible = it)) }, textColor, accentColor)
            if (ind.fillVisible) {
                StyleRowSimple("Up Fill", ind.upFillColor, { onUpdate(ind.copy(upFillColor = it)) }, textColor, borderColor, bgColor)
                StyleRowSimple("Down Fill", ind.downFillColor, { onUpdate(ind.copy(downFillColor = it)) }, textColor, borderColor, bgColor)
            }
            StyleRowIndicator("Ligne", true, {}, ind.upColor, ind.thickness.toInt(), ind.style, { c, t, s -> onUpdate(ind.copy(thickness = t.toFloat(), style = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
        }
        is Indicator.SMA -> {
            StyleRowIndicator("Couleur", true, {}, ind.color, ind.thickness.toInt(), ind.style, { c, t, s -> onUpdate(ind.copy(color = c, thickness = t.toFloat(), style = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
        }
        is Indicator.EMA -> {
            StyleRowIndicator("Couleur", true, {}, ind.color, ind.thickness.toInt(), ind.style, { c, t, s -> onUpdate(ind.copy(color = c, thickness = t.toFloat(), style = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
        }
        is Indicator.HMA -> {
            StyleRowIndicator("Couleur", true, {}, ind.color, ind.thickness.toInt(), ind.style, { c, t, s -> onUpdate(ind.copy(color = c, thickness = t.toFloat(), style = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
        }
        is Indicator.BollingerBands -> {
            StyleRowIndicator("Middle", true, {}, ind.middleColor, ind.middleThickness.toInt(), ind.middleStyle, { c, t, s -> onUpdate(ind.copy(middleColor = c, middleThickness = t.toFloat(), middleStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Upper", true, {}, ind.upperColor, ind.upperThickness.toInt(), ind.upperStyle, { c, t, s -> onUpdate(ind.copy(upperColor = c, upperThickness = t.toFloat(), upperStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Lower", true, {}, ind.lowerColor, ind.lowerThickness.toInt(), ind.lowerStyle, { c, t, s -> onUpdate(ind.copy(lowerColor = c, lowerThickness = t.toFloat(), lowerStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            CheckboxRow("Background Fill", ind.fillVisible, { onUpdate(ind.copy(fillVisible = it)) }, textColor, accentColor)
            if (ind.fillVisible) {
                StyleRowSimple("Fill Color", ind.fillColor, { onUpdate(ind.copy(fillColor = it)) }, textColor, borderColor, bgColor)
            }
        }
        is Indicator.MACD -> {
            StyleRowIndicator("MACD", true, {}, ind.macdColor, ind.macdThickness.toInt(), ind.macdStyle, { c, t, s -> onUpdate(ind.copy(macdColor = c, macdThickness = t.toFloat(), macdStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Signal", true, {}, ind.signalColor, ind.signalThickness.toInt(), ind.signalStyle, { c, t, s -> onUpdate(ind.copy(signalColor = c, signalThickness = t.toFloat(), signalStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            CheckboxRow("Histogram", ind.histVisible, { onUpdate(ind.copy(histVisible = it)) }, textColor, accentColor)
            if (ind.histVisible) {
                StyleRowSimple("Hist Up", ind.histColorUp, { onUpdate(ind.copy(histColorUp = it)) }, textColor, borderColor, bgColor)
                StyleRowSimple("Hist Down", ind.histColorDown, { onUpdate(ind.copy(histColorDown = it)) }, textColor, borderColor, bgColor)
            }
        }
        is Indicator.Alligator -> {
            StyleRowIndicator("Jaw", true, {}, ind.jawColor, ind.jawThickness.toInt(), 0, { c, t, s -> onUpdate(ind.copy(jawColor = c, jawThickness = t.toFloat())) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Teeth", true, {}, ind.teethColor, ind.teethThickness.toInt(), 0, { c, t, s -> onUpdate(ind.copy(teethColor = c, teethThickness = t.toFloat())) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Lips", true, {}, ind.lipsColor, ind.lipsThickness.toInt(), 0, { c, t, s -> onUpdate(ind.copy(lipsColor = c, lipsThickness = t.toFloat())) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
        }
        is Indicator.Ichimoku -> {
            StyleRowIndicator("Tenkan-sen", true, {}, ind.tenkanColor, ind.tenkanThickness.toInt(), 0, { c, t, s -> onUpdate(ind.copy(tenkanColor = c, tenkanThickness = t.toFloat())) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Kijun-sen", true, {}, ind.kijunColor, ind.kijunThickness.toInt(), 0, { c, t, s -> onUpdate(ind.copy(kijunColor = c, kijunThickness = t.toFloat())) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Chikou Span", true, {}, ind.chikouColor, ind.chikouThickness.toInt(), 0, { c, t, s -> onUpdate(ind.copy(chikouColor = c, chikouThickness = t.toFloat())) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Senkou Span A", true, {}, ind.senkouAColor, ind.senkouAThickness.toInt(), 0, { c, t, s -> onUpdate(ind.copy(senkouAColor = c, senkouAThickness = t.toFloat())) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Senkou Span B", true, {}, ind.senkouBColor, ind.senkouBThickness.toInt(), 0, { c, t, s -> onUpdate(ind.copy(senkouBColor = c, senkouBThickness = t.toFloat())) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowSimple("Kumo Up", ind.kumoUpColor, { onUpdate(ind.copy(kumoUpColor = it)) }, textColor, borderColor, bgColor)
            StyleRowSimple("Kumo Down", ind.kumoDownColor, { onUpdate(ind.copy(kumoDownColor = it)) }, textColor, borderColor, bgColor)
        }
        is Indicator.Sessions -> {
            StyleRowSimple("Sydney Color", ind.sydneyColor, { onUpdate(ind.copy(sydneyColor = it)) }, textColor, borderColor, bgColor)
            StyleRowSimple("Tokyo Color", ind.tokyoColor, { onUpdate(ind.copy(tokyoColor = it)) }, textColor, borderColor, bgColor)
            StyleRowSimple("London Color", ind.londonColor, { onUpdate(ind.copy(londonColor = it)) }, textColor, borderColor, bgColor)
            StyleRowSimple("New York Color", ind.newYorkColor, { onUpdate(ind.copy(newYorkColor = it)) }, textColor, borderColor, bgColor)
        }
        is Indicator.VWAP -> {
            StyleRowIndicator("VWAP", true, {}, ind.color, ind.thickness.toInt(), ind.style, { c, t, s -> onUpdate(ind.copy(color = c, thickness = t.toFloat(), style = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            if (ind.showBands) {
                StyleRowSimple("Upper Bands Color", ind.upperColor, { onUpdate(ind.copy(upperColor = it)) }, textColor, borderColor, bgColor)
                StyleRowSimple("Lower Bands Color", ind.lowerColor, { onUpdate(ind.copy(lowerColor = it)) }, textColor, borderColor, bgColor)
            }
        }
        is Indicator.Ribbon -> {
            StyleRowIndicator("Thickness", true, {}, Color.White, ind.thickness.toInt(), 0, { c, t, s -> onUpdate(ind.copy(thickness = t.toFloat())) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Last MA Thickness", true, {}, Color.White, ind.lastThickness.toInt(), 0, { c, t, s -> onUpdate(ind.copy(lastThickness = t.toFloat())) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
        }
    }
}

@Composable
fun SettingsDropdownRow(
    label: String,
    currentValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    textColor: Color,
    borderColor: Color,
    bgColor: Color
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = textColor, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(34.dp)
                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(currentValue, color = textColor, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, null, tint = textColor, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(bgColor).border(1.dp, borderColor)) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = textColor, fontSize = 14.sp) },
                        onClick = { onOptionSelected(option); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
fun VisibilityContent(indicator: Indicator, onUpdate: (Indicator) -> Unit, textColor: Color, accentColor: Color) {
    CheckboxRow("Visible", indicator.isVisible, { onUpdate(when(indicator) {
        is Indicator.Volume -> indicator.copy(isVisible = it)
        is Indicator.SMA -> indicator.copy(isVisible = it)
        is Indicator.EMA -> indicator.copy(isVisible = it)
        is Indicator.HMA -> indicator.copy(isVisible = it)
        is Indicator.BollingerBands -> indicator.copy(isVisible = it)
        is Indicator.ATRBands -> indicator.copy(isVisible = it)
        is Indicator.RSI -> indicator.copy(isVisible = it)
        is Indicator.MACD -> indicator.copy(isVisible = it)
        is Indicator.Stochastic -> indicator.copy(isVisible = it)
        is Indicator.ATR -> indicator.copy(isVisible = it)
        is Indicator.Supertrend -> indicator.copy(isVisible = it)
        is Indicator.Alligator -> indicator.copy(isVisible = it)
        is Indicator.Ichimoku -> indicator.copy(isVisible = it)
        is Indicator.Sessions -> indicator.copy(isVisible = it)
        is Indicator.VWAP -> indicator.copy(isVisible = it)
        is Indicator.Ribbon -> indicator.copy(isVisible = it)
    }) }, textColor, accentColor)
}

@Composable
fun RadioButtonRow(label: String, selected: Boolean, onClick: () -> Unit, textColor: Color, accentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = textColor.copy(0.4f))
        )
        Text(label, color = textColor, fontSize = 14.sp)
    }
}

@Composable
fun InputRow(label: String, value: String, onValueChange: (String) -> Unit, textColor: Color, borderColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = textColor, modifier = Modifier.weight(1f), fontSize = 14.sp)
        SettingsInputBox(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(120.dp),
            textColor = textColor,
            borderColor = borderColor
        )
    }
}

@Composable
fun CheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, textColor: Color, accentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = accentColor, uncheckedColor = textColor.copy(0.4f)),
                modifier = Modifier.padding(end = 12.dp)
            )
        }
        Text(label, color = textColor, fontSize = 14.sp)
    }
}

@Composable
fun StyleRowIndicator(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, color: Color, thickness: Int, style: Int, onLineSettingsChange: (Color, Int, Int) -> Unit, textColor: Color, borderColor: Color, isDarkTheme: Boolean, bgColor: Color, accentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = accentColor),
                modifier = Modifier.padding(end = 12.dp)
            )
        }
        Text(label, color = textColor, modifier = Modifier.width(100.dp), fontSize = 14.sp)
        LineOptionBox(color, thickness, style, onLineSettingsChange, textColor, borderColor, isDarkTheme)
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun StyleRowSimple(label: String, color: Color, onColorChange: (Color) -> Unit, textColor: Color, borderColor: Color, bgColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = textColor, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorBox(color, onColorChange, isDarkTheme = true)
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier.size(32.dp, 28.dp).border(1.dp, borderColor, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.height(1.dp).width(16.dp).background(textColor.copy(0.5f)))
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier.size(32.dp, 28.dp).border(1.dp, borderColor, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ArrowDropDown, null, tint = textColor, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun StyleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    color: Color,
    onColorChange: (Color) -> Unit,
    textColor: Color,
    borderColor: Color,
    isDarkTheme: Boolean,
    bgColor: Color,
    isBackground: Boolean = false,
    accentColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = accentColor),
                modifier = Modifier.padding(end = 12.dp)
            )
        }
        Text(label, color = textColor, modifier = Modifier.width(100.dp), fontSize = 14.sp)
        ColorBox(color, onColorChange, isDarkTheme)
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun LevelStyleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, color: Color, onColorChange: (Color) -> Unit, value: Int, onValueChange: (Int) -> Unit, textColor: Color, borderColor: Color, isDarkTheme: Boolean, bgColor: Color, accentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = accentColor),
                modifier = Modifier.padding(end = 12.dp)
            )
        }
        Text(label, color = textColor, modifier = Modifier.width(100.dp), fontSize = 14.sp)
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorBox(color, onColorChange, isDarkTheme)
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier.size(32.dp, 28.dp).border(1.dp, borderColor, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
                    drawLine(textColor.copy(0.5f), Offset(0f, size.height/2), Offset(size.width, size.height/2), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f)))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            SettingsInputBox(
                value = value.toString(),
                onValueChange = { it.toIntOrNull()?.let { v -> if(v != value) onValueChange(v) } },
                modifier = Modifier.width(80.dp),
                textColor = textColor,
                borderColor = borderColor
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun SettingsInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color,
    borderColor: Color,
    enabled: Boolean = true
) {
    var textFieldValue by remember(value) { mutableStateOf(value) }

    BasicTextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValue = it
            onValueChange(it)
        },
        enabled = enabled,
        modifier = modifier
            .height(34.dp)
            .border(1.dp, if (enabled) borderColor else borderColor.copy(0.3f), RoundedCornerShape(4.dp)),
        textStyle = TextStyle(fontSize = 13.sp, color = if (enabled) textColor else textColor.copy(0.5f), textAlign = TextAlign.Start),
        singleLine = true,
        cursorBrush = SolidColor(textColor),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                innerTextField()
            }
        }
    )
}
