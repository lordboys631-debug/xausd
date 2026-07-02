package com.bthr.backtest.ui.components

import android.util.Log
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.DialogProperties
import com.bthr.backtest.R
import com.bthr.backtest.model.Indicator
import com.bthr.backtest.model.TimeframeVisibility
import android.widget.EditText
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.graphics.drawable.GradientDrawable
import android.content.res.ColorStateList
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView

val LocalScrollRequest = compositionLocalOf<(Float) -> Unit> { { } }

@Composable
fun IndicatorSettingsDialog(
    indicator: Indicator,
    onDismiss: () -> Unit,
    onSave: (Indicator) -> Unit,
    isDarkTheme: Boolean = true
) {
    var currentIndicator by remember { mutableStateOf(indicator) }
    var selectedTab by remember { mutableStateOf("Inputs") }
    var recomposeCount by remember { mutableIntStateOf(0) }
    recomposeCount++
    Log.d("DIALOG_DEBUG", "Dialog recompose #$recomposeCount, tab=$selectedTab")

    val colorScheme = MaterialTheme.colorScheme
    val bgColor = colorScheme.surface
    val textColor = colorScheme.onSurface
    val borderColor = colorScheme.outline

    val accentColor = if (isDarkTheme) Color.White else Color.Black
    val onAccentColor = if (isDarkTheme) Color.Black else Color.White

    val density = LocalDensity.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val dialogRoot = LocalView.current.rootView
        LaunchedEffect(Unit) {
            val lp = dialogRoot.layoutParams
            if (lp is android.view.WindowManager.LayoutParams) {
                lp.softInputMode = android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f),
                shape = RoundedCornerShape(6.dp),
                color = bgColor
            ) {
            Column(modifier = Modifier.fillMaxSize()) {
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    val tabs = listOf("Inputs", "Style", "Visibility")
                    tabs.forEach { tab ->
                        val isSelected = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .padding(end = 24.dp)
                                .clickable { selectedTab = tab }
                        ) {
                            Column(
                                modifier = Modifier.padding(bottom = 8.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = when(tab) {
                                        "Inputs" -> "Paramètres en Entrée"
                                        "Style" -> "Style"
                                        else -> "Visibilité"
                                    },
                                    color = if (isSelected) textColor else textColor.copy(alpha = 0.6f),
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .height(3.dp)
                                        .matchParentSize()
                                        .background(accentColor)
                                ) {}
                            }
                        }
                    }
                }

                HorizontalDivider(color = borderColor, thickness = 1.dp)

                // Zone de contenu scrollable
                val scrollState = rememberScrollState()
                var targetRowRootY by remember { mutableFloatStateOf(-1f) }
                var boxTopY by remember { mutableFloatStateOf(0f) }
                val scope = rememberCoroutineScope()
                LaunchedEffect(targetRowRootY) {
                    if (targetRowRootY >= 0f) {
                        val desiredScroll = (targetRowRootY - boxTopY + scrollState.value - with(density) { 8.dp.toPx() }).toInt().coerceAtLeast(0)
                        Log.d("SCROLL_DEBUG", "scrolling to scrollOffset=$desiredScroll (targetRowRootY=$targetRowRootY boxTopY=$boxTopY currentScroll=${scrollState.value})")
                        scrollState.animateScrollTo(desiredScroll)
                        targetRowRootY = -1f
                    }
                }
                CompositionLocalProvider(LocalScrollRequest provides { y -> targetRowRootY = y }) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .verticalScroll(scrollState)
                            .onGloballyPositioned { boxTopY = it.positionInRoot().y }
                    ) {
                        when (selectedTab) {
                            "Inputs" -> InputsContent(currentIndicator, onUpdate = { currentIndicator = it }, textColor, borderColor, bgColor, accentColor)
                            "Style" -> StyleContent(currentIndicator, onUpdate = { currentIndicator = it }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
                            "Visibility" -> VisibilityContent(currentIndicator, onUpdate = { currentIndicator = it }, textColor, borderColor, accentColor)
                        }
                    }
                }

                HorizontalDivider(color = borderColor, thickness = 1.dp)

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
                            Icon(painterResource(id = R.drawable.ic_more_horiz), null, tint = textColor)
                        }
                        DropdownMenu(
                            expanded = showDefaultMenu,
                            onDismissRequest = { showDefaultMenu = false },
                            offset = androidx.compose.ui.unit.DpOffset(x = 0.dp, y = 4.dp),
                            modifier = Modifier.shadow(8.dp, RoundedCornerShape(4.dp)).background(bgColor)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Appliquer les paramètres par défaut", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Normal) },
                                onClick = { 
                                    // Réinitialiser aux valeurs par défaut en conservant l'ID original
                                    val originalId = currentIndicator.id
                                    currentIndicator = when(currentIndicator) {
                                        is Indicator.Sessions -> Indicator.Sessions().copy(id = originalId)
                                        is Indicator.Volume -> Indicator.Volume().copy(id = originalId)
                                        is Indicator.RSI -> Indicator.RSI().copy(id = originalId)
                                        is Indicator.MACD -> Indicator.MACD().copy(id = originalId)
                                        is Indicator.Stochastic -> Indicator.Stochastic().copy(id = originalId)
                                        is Indicator.ATR -> Indicator.ATR().copy(id = originalId)
                                        is Indicator.ATRBands -> Indicator.ATRBands().copy(id = originalId)
                                        is Indicator.Supertrend -> Indicator.Supertrend().copy(id = originalId)
                                        is Indicator.SMA -> Indicator.SMA().copy(id = originalId)
                                        is Indicator.EMA -> Indicator.EMA().copy(id = originalId)
                                        is Indicator.HMA -> Indicator.HMA().copy(id = originalId)
                                        is Indicator.WMA -> Indicator.WMA().copy(id = originalId)
                                        is Indicator.BollingerBands -> Indicator.BollingerBands().copy(id = originalId)
                                        is Indicator.Alligator -> Indicator.Alligator().copy(id = originalId)
                                        is Indicator.Ichimoku -> Indicator.Ichimoku().copy(id = originalId)
                                        is Indicator.VWAP -> Indicator.VWAP().copy(id = originalId)
                                        is Indicator.Ribbon -> Indicator.Ribbon().copy(id = originalId)
                                        else -> currentIndicator
                                    }
                                    showDefaultMenu = false 
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(34.dp)
                            )
                            DropdownMenuItem(
                                text = { Text("Sauvegarder Sous...", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Normal) },
                                onClick = { showDefaultMenu = false },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(34.dp)
    )
}
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD600),
                                contentColor = Color.Black
                            ),
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
            SettingsDropdownRow("Type de MA", ind.maType, listOf("SMA", "EMA", "WMA"), { onUpdate(ind.copy(maType = it)) }, textColor, borderColor, bgColor)
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
            Text("ATR bands standard setting", color = textColor.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
            InputRow("ATR Period", ind.period.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.period) onUpdate(ind.copy(period = v)) } }, textColor, borderColor)
            InputRow("ATR Band Scale Factor", ind.multiplier.toString(), { it.toFloatOrNull()?.let { v -> if(v != ind.multiplier) onUpdate(ind.copy(multiplier = v)) } }, textColor, borderColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Take profit setting", color = textColor.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
            CheckboxRow("Show opposite bands for take-profit zones", ind.showTPBands, { onUpdate(ind.copy(showTPBands = it)) }, textColor, accentColor)
            InputRow("Take-Profit Scale Factor", ind.tpScaleFactor.toString(), { it.toFloatOrNull()?.let { v -> if(v != ind.tpScaleFactor) onUpdate(ind.copy(tpScaleFactor = v)) } }, textColor, borderColor)
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
        is Indicator.WMA -> {
            InputRow("Longueur", ind.period.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.period) onUpdate(ind.copy(period = v)) } }, textColor, borderColor)
            SettingsDropdownRow("Source", ind.source, listOf("Close", "Open", "High", "Low"), { onUpdate(ind.copy(source = it)) }, textColor, borderColor, bgColor)
        }
        is Indicator.BollingerBands -> {
            InputRow("Longueur", ind.period.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.period) onUpdate(ind.copy(period = v)) } }, textColor, borderColor)
            InputRow("Mult", ind.stdDev.toString(), { it.toFloatOrNull()?.let { v -> if(v != ind.stdDev) onUpdate(ind.copy(stdDev = v)) } }, textColor, borderColor)
            SettingsDropdownRow("MA Type", ind.maType ?: "SMA", listOf("SMA", "EMA", "WMA"), { onUpdate(ind.copy(maType = it)) }, textColor, borderColor, bgColor)
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
            InputRow("Périodes de lignes de conversion (tenkanPeriod)", ind.tenkanPeriod.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.tenkanPeriod) onUpdate(ind.copy(tenkanPeriod = v)) } }, textColor, borderColor)
            InputRow("Périodes de ligne de base (kijunPeriod)", ind.kijunPeriod.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.kijunPeriod) onUpdate(ind.copy(kijunPeriod = v)) } }, textColor, borderColor)
            InputRow("Périodes de couverture principales (senkouBPeriod)", ind.senkouBPeriod.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.senkouBPeriod) onUpdate(ind.copy(senkouBPeriod = v)) } }, textColor, borderColor)
            InputRow("Périodes de latence (displacement)", ind.displacement.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.displacement) onUpdate(ind.copy(displacement = v)) } }, textColor, borderColor)
            InputRow("Principales périodes de roulement (senkouAPeriod)", ind.senkouAPeriod.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.senkouAPeriod) onUpdate(ind.copy(senkouAPeriod = v)) } }, textColor, borderColor)
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
        is Indicator.Sessions -> {
            val ind = indicator as Indicator.Sessions
            val utcOptions = (-12..14).map { if (it > 0) "+$it" else it.toString() }
            val currentUtc = if (ind.utcOffset > 0) "+${ind.utcOffset}" else ind.utcOffset.toString()
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "TIMEZONE",
                color = textColor.copy(0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
            )
            SettingsDropdownRow(
                "UTC (+/-)",
                currentUtc,
                utcOptions,
                { onUpdate(ind.copy(utcOffset = it.replace("+", "").toIntOrNull() ?: 0)) },
                textColor, borderColor, bgColor
            )
            CheckboxRow(
                "Use Exchange Timezone",
                ind.useExchangeTimezone,
                { onUpdate(ind.copy(useExchangeTimezone = it)) },
                textColor,
                accentColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            CheckboxRow(
                "Show Labels",
                ind.showLabels,
                { onUpdate(ind.copy(showLabels = it)) },
                textColor,
                accentColor
            )
            SettingsDropdownRow(
                "Text Size",
                ind.labelTextSize.toString(),
                (6..16).map { it.toString() },
                { onUpdate(ind.copy(labelTextSize = it.toIntOrNull() ?: 8)) },
                textColor, borderColor, bgColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            CheckboxRow(
                "Show Background",
                ind.showBackground,
                { onUpdate(ind.copy(showBackground = it)) },
                textColor,
                accentColor
            )
            SettingsDropdownRow(
                "Opacité",
                "${(ind.opacity * 100).toInt()}%",
                listOf(0.1f,0.2f,0.3f,0.4f,0.5f,0.6f,0.7f,0.8f,0.9f,1.0f).map { "${(it * 100).toInt()}%" },
                { opStr ->
                    val op = opStr.replace("%","").toFloat() / 100f
                    if(op != ind.opacity) onUpdate(ind.copy(opacity = op))
                },
                textColor, borderColor, bgColor
            )
        }
    }
}

@Composable
fun SessionInputRow(
    sessionLabel: String,
    name: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onNameChange: (String) -> Unit,
    startTime: String,
    onStartTimeChange: (String) -> Unit,
    endTime: String,
    onEndTimeChange: (String) -> Unit,
    sessionColor: Color,
    onSessionColorChange: (Color) -> Unit,
    textColor: Color,
    borderColor: Color,
    accentColor: Color,
    onFocus: (() -> Unit)? = null  // Callback optionnel appelé quand un champ reçoit le focus
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Label de session (SESSION A, SESSION B, etc.)
        Text(sessionLabel, color = textColor.copy(0.7f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
        
        // Première ligne : Checkbox + Nom de session éditable
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            CustomCheckbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.padding(end = 8.dp, top = 10.dp, bottom = 10.dp)
            )
            
            // Nom de session éditable dans un rectangle arrondi
            var sessionName by remember { mutableStateOf(name) }
            // Sync quand le nom change de l'extérieur (pas quand l'utilisateur tape)
            LaunchedEffect(name) { sessionName = name }
            val focusCallback: () -> Unit = {
                android.util.Log.e("FOCUS_DEBUG", "SessionInputRow onFocus appelé pour $sessionLabel")
                onFocus?.invoke() ?: Unit
            }
            AndroidEditTextInput(
                value = sessionName,
                onValueChange = { sessionName = it },
                onCommit = { if (sessionName != name) onNameChange(sessionName) },
                textColor = textColor,
                fontSizeSp = 14f,
                onFocus = focusCallback,
                tag = "${sessionLabel}_name",
                modifier = Modifier
                    .width(120.dp)
                    .height(36.dp)
                    .border(1.dp, Color(0xFFBDBDBD), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Deuxième ligne : Heures + Carré de picker de couleur avec picker
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.width(40.dp)) // Alignement avec la checkbox
            
            // Heure de début avec icône horloge
            TimeInputBox(
                value = startTime,
                onValueChange = onStartTimeChange,
                modifier = Modifier.width(90.dp),
                textColor = textColor,
                borderColor = borderColor,
                onFocus = onFocus,
                tag = "${sessionLabel}_start"
            )

            Text(" – ", color = textColor, modifier = Modifier.padding(horizontal = 4.dp))

            // Heure de fin avec icône horloge
            TimeInputBox(
                value = endTime,
                onValueChange = onEndTimeChange,
                modifier = Modifier.width(90.dp),
                textColor = textColor,
                borderColor = borderColor,
                onFocus = onFocus,
                tag = "${sessionLabel}_end"
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Carré de couleur cliquable avec picker
            ColorBox(
                color = sessionColor,
                onColorChange = onSessionColorChange,
                isDarkTheme = true,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color,
    borderColor: Color,
    onFocus: (() -> Unit)? = null,
    tag: Any? = null
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(34.dp)
            .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .clickable { showDialog = true }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(value, color = textColor, fontSize = 13.sp, maxLines = 1)
    }

    if (showDialog) {
        val parts = value.split(":")
        val initialHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
        val initialMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val state = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )
        val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = if (isDarkTheme) Color(0xFF2D2D2D) else Color.White,
            title = { Text("Choisir une heure", color = textColor) },
            text = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TimePicker(state = state)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val h = state.hour.toString().padStart(2, '0')
                    val m = state.minute.toString().padStart(2, '0')
                    onValueChange("$h:$m")
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
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
            CheckboxRow("Etiquettes sur l'echelle de prix", ind.showLabelsOnPriceScale, { onUpdate(ind.copy(showLabelsOnPriceScale = it)) }, textColor, accentColor)
        }
        is Indicator.RSI -> {
            StyleRowIndicator("Tracé", ind.showRsi, { onUpdate(ind.copy(showRsi = it)) }, ind.color, ind.thickness.toInt(), ind.style, { c, t, s -> onUpdate(ind.copy(color = c, thickness = t.toFloat(), style = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Smoothed MA", ind.showMa, { onUpdate(ind.copy(showMa = it)) }, ind.maColor, ind.maThickness.toInt(), ind.maStyle, { c, t, s -> onUpdate(ind.copy(maColor = c, maThickness = t.toFloat(), maStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            Spacer(modifier = Modifier.height(8.dp))
            StyleRowIndicator("Limite supérieure", ind.upperLevelVisible, { onUpdate(ind.copy(upperLevelVisible = it)) }, ind.upperLevelColor, ind.upperLevelThickness.toInt(), ind.upperLevelStyle, { c, t, s -> onUpdate(ind.copy(upperLevelColor = c, upperLevelThickness = t.toFloat(), upperLevelStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            InputRow("Valeur", ind.upperLevel.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.upperLevel) onUpdate(ind.copy(upperLevel = v)) } }, textColor, borderColor)
            StyleRowIndicator("MiddleLimit", ind.middleLevelVisible, { onUpdate(ind.copy(middleLevelVisible = it)) }, ind.middleLevelColor, ind.middleLevelThickness.toInt(), ind.middleLevelStyle, { c, t, s -> onUpdate(ind.copy(middleLevelColor = c, middleLevelThickness = t.toFloat(), middleLevelStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            InputRow("Valeur", ind.middleLevel.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.middleLevel) onUpdate(ind.copy(middleLevel = v)) } }, textColor, borderColor)
            StyleRowIndicator("Limite inférieure", ind.lowerLevelVisible, { onUpdate(ind.copy(lowerLevelVisible = it)) }, ind.lowerLevelColor, ind.lowerLevelThickness.toInt(), ind.lowerLevelStyle, { c, t, s -> onUpdate(ind.copy(lowerLevelColor = c, lowerLevelThickness = t.toFloat(), lowerLevelStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            InputRow("Valeur", ind.lowerLevel.toString(), { it.toIntOrNull()?.let { v -> if(v != ind.lowerLevel) onUpdate(ind.copy(lowerLevel = v)) } }, textColor, borderColor)
            StyleRow("Hlines Background", ind.backgroundVisible, { onUpdate(ind.copy(backgroundVisible = it)) }, ind.backgroundColor, { onUpdate(ind.copy(backgroundColor = it)) }, textColor, borderColor, isDarkTheme, bgColor, isBackground = true, accentColor = accentColor)
            CheckboxRow("Etiquettes sur l'echelle de prix", ind.showLabelsOnPriceScale, { onUpdate(ind.copy(showLabelsOnPriceScale = it)) }, textColor, accentColor)
        }
        is Indicator.Stochastic -> {
            StyleRowIndicator("%K", true, {}, ind.kColor, ind.kThickness.toInt(), ind.kStyle, { c, t, s -> onUpdate(ind.copy(kColor = c, kThickness = t.toFloat(), kStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("%D", true, {}, ind.dColor, ind.dThickness.toInt(), ind.dStyle, { c, t, s -> onUpdate(ind.copy(dColor = c, dThickness = t.toFloat(), dStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            Spacer(modifier = Modifier.height(8.dp))
            LevelStyleRow("Upper Level", true, {}, ind.upperLevelColor, { onUpdate(ind.copy(upperLevelColor = it)) }, ind.upperLevel, { onUpdate(ind.copy(upperLevel = it)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            LevelStyleRow("Lower Level", true, {}, ind.lowerLevelColor, { onUpdate(ind.copy(lowerLevelColor = it)) }, ind.lowerLevel, { onUpdate(ind.copy(lowerLevel = it)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRow("Background", ind.backgroundVisible, { onUpdate(ind.copy(backgroundVisible = it)) }, ind.backgroundColor, { onUpdate(ind.copy(backgroundColor = it)) }, textColor, borderColor, isDarkTheme, bgColor, isBackground = true, accentColor = accentColor)
            CheckboxRow("Etiquettes sur l'echelle de prix", ind.showLabelsOnPriceScale, { onUpdate(ind.copy(showLabelsOnPriceScale = it)) }, textColor, accentColor)
        }
        is Indicator.ATR -> {
            StyleRowIndicator("Tracé", ind.showLine, { onUpdate(ind.copy(showLine = it)) }, ind.color, ind.thickness.toInt(), ind.style, { c, t, s -> onUpdate(ind.copy(color = c, thickness = t.toFloat(), style = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            CheckboxRow("Etiquettes sur l'echelle de prix", ind.showLabelsOnPriceScale, { onUpdate(ind.copy(showLabelsOnPriceScale = it)) }, textColor, accentColor)
        }
        is Indicator.ATRBands -> {
            StyleRowIndicator("Upper ATR Band", ind.showUpper, { onUpdate(ind.copy(showUpper = it)) }, ind.upperColor, ind.upperThickness.toInt(), ind.upperStyle, { c, t, s -> onUpdate(ind.copy(upperColor = c, upperThickness = t.toFloat(), upperStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Lower ATR Band", ind.showLower, { onUpdate(ind.copy(showLower = it)) }, ind.lowerColor, ind.lowerThickness.toInt(), ind.lowerStyle, { c, t, s -> onUpdate(ind.copy(lowerColor = c, lowerThickness = t.toFloat(), lowerStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Upper Take profit band", ind.showTPUpper, { onUpdate(ind.copy(showTPUpper = it)) }, ind.tpUpperColor, ind.tpUpperThickness.toInt(), ind.tpUpperStyle, { c, t, s -> onUpdate(ind.copy(tpUpperColor = c, tpUpperThickness = t.toFloat(), tpUpperStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Lower Take profit band", ind.showTPLower, { onUpdate(ind.copy(showTPLower = it)) }, ind.tpLowerColor, ind.tpLowerThickness.toInt(), ind.tpLowerStyle, { c, t, s -> onUpdate(ind.copy(tpLowerColor = c, tpLowerThickness = t.toFloat(), tpLowerStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            CheckboxRow("Etiquettes sur l'echelle de prix", ind.showLabelsOnPriceScale, { onUpdate(ind.copy(showLabelsOnPriceScale = it)) }, textColor, accentColor)
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
            CheckboxRow("Etiquettes sur l'echelle de prix", ind.showLabelsOnPriceScale, { onUpdate(ind.copy(showLabelsOnPriceScale = it)) }, textColor, accentColor)
        }
        is Indicator.SMA -> {
            StyleRowIndicator("Tracé", true, {}, ind.color, ind.thickness.toInt(), ind.style, { c, t, s -> onUpdate(ind.copy(color = c, thickness = t.toFloat(), style = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            CheckboxRow("Etiquettes sur l'echelle de prix", ind.showLabelsOnPriceScale, { onUpdate(ind.copy(showLabelsOnPriceScale = it)) }, textColor, accentColor)
        }
        is Indicator.EMA -> {
            StyleRowIndicator("Tracé", true, {}, ind.color, ind.thickness.toInt(), ind.style, { c, t, s -> onUpdate(ind.copy(color = c, thickness = t.toFloat(), style = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            CheckboxRow("Etiquettes sur l'echelle de prix", ind.showLabelsOnPriceScale, { onUpdate(ind.copy(showLabelsOnPriceScale = it)) }, textColor, accentColor)
        }
        is Indicator.HMA -> {
            StyleRowIndicator("Tracé", true, {}, ind.color, ind.thickness.toInt(), ind.style, { c, t, s -> onUpdate(ind.copy(color = c, thickness = t.toFloat(), style = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            CheckboxRow("Etiquettes sur l'echelle de prix", ind.showLabelsOnPriceScale, { onUpdate(ind.copy(showLabelsOnPriceScale = it)) }, textColor, accentColor)
        }
        is Indicator.WMA -> {
            StyleRowIndicator("Tracé", true, {}, ind.color, ind.thickness.toInt(), ind.style, { c, t, s -> onUpdate(ind.copy(color = c, thickness = t.toFloat(), style = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            CheckboxRow("Etiquettes sur l'echelle de prix", ind.showLabelsOnPriceScale, { onUpdate(ind.copy(showLabelsOnPriceScale = it)) }, textColor, accentColor)
        }
        is Indicator.BollingerBands -> {
            StyleRowIndicator("Middle", ind.showMiddle, { onUpdate(ind.copy(showMiddle = it)) }, ind.middleColor, ind.middleThickness.toInt(), ind.middleStyle, { c, t, s -> onUpdate(ind.copy(middleColor = c, middleThickness = t.toFloat(), middleStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Upper", ind.showUpper, { onUpdate(ind.copy(showUpper = it)) }, ind.upperColor, ind.upperThickness.toInt(), ind.upperStyle, { c, t, s -> onUpdate(ind.copy(upperColor = c, upperThickness = t.toFloat(), upperStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Lower", ind.showLower, { onUpdate(ind.copy(showLower = it)) }, ind.lowerColor, ind.lowerThickness.toInt(), ind.lowerStyle, { c, t, s -> onUpdate(ind.copy(lowerColor = c, lowerThickness = t.toFloat(), lowerStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            CheckboxRow("Background Fill", ind.fillVisible, { onUpdate(ind.copy(fillVisible = it)) }, textColor, accentColor)
            if (ind.fillVisible) {
                var showFillPicker by remember { mutableStateOf(false) }
                val density = LocalDensity.current
                val fillPopY = with(density) { 33.dp.roundToPx() }
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 36.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(modifier = Modifier.size(32.dp))
                        Box(modifier = Modifier.size(32.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                                        .padding(4.dp)
                                        .background(ind.fillColor, RoundedCornerShape(2.dp))
                                        .border(0.5.dp, borderColor, RoundedCornerShape(2.dp))
                                        .clickable { showFillPicker = true }
                                )
                                if (showFillPicker) {
                                    ColorPickerPopup(
                                        selectedColor = ind.fillColor,
                                        onColorSelected = { c ->
                                            onUpdate(ind.copy(fillColor = c))
                                            showFillPicker = false
                                        },
                                        onDismiss = { showFillPicker = false },
                                        isDarkTheme = isDarkTheme,
                                        positionX = 0,
                                        positionY = fillPopY,
                                        onLineSettingsChange = null
                                    )
                                }
                            }
                        }
                    }
                }
            }
            CheckboxRow("Etiquettes sur l'echelle de prix", ind.showLabelsOnPriceScale, { onUpdate(ind.copy(showLabelsOnPriceScale = it)) }, textColor, accentColor)
        }
        is Indicator.MACD -> {
            StyleRowIndicator("MACD", true, {}, ind.macdColor, ind.macdThickness.toInt(), ind.macdStyle, { c, t, s -> onUpdate(ind.copy(macdColor = c, macdThickness = t.toFloat(), macdStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Signal", true, {}, ind.signalColor, ind.signalThickness.toInt(), ind.signalStyle, { c, t, s -> onUpdate(ind.copy(signalColor = c, signalThickness = t.toFloat(), signalStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            CheckboxRow("Histogram", ind.histVisible, { onUpdate(ind.copy(histVisible = it)) }, textColor, accentColor)
            if (ind.histVisible) {
                StyleRowSimple("Hist Up", ind.histColorUp, { onUpdate(ind.copy(histColorUp = it)) }, textColor, borderColor, bgColor)
                StyleRowSimple("Hist Down", ind.histColorDown, { onUpdate(ind.copy(histColorDown = it)) }, textColor, borderColor, bgColor)
            }
            CheckboxRow("Etiquettes sur l'echelle de prix", ind.showLabelsOnPriceScale, { onUpdate(ind.copy(showLabelsOnPriceScale = it)) }, textColor, accentColor)
        }
        is Indicator.Alligator -> {
            StyleRowIndicator("Jaw", true, {}, ind.jawColor, ind.jawThickness.toInt(), ind.jawStyle, { c, t, s -> onUpdate(ind.copy(jawColor = c, jawThickness = t.toFloat(), jawStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Teeth", true, {}, ind.teethColor, ind.teethThickness.toInt(), ind.teethStyle, { c, t, s -> onUpdate(ind.copy(teethColor = c, teethThickness = t.toFloat(), teethStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Lips", true, {}, ind.lipsColor, ind.lipsThickness.toInt(), ind.lipsStyle, { c, t, s -> onUpdate(ind.copy(lipsColor = c, lipsThickness = t.toFloat(), lipsStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            CheckboxRow("Etiquettes sur l'echelle de prix", ind.showLabelsOnPriceScale, { onUpdate(ind.copy(showLabelsOnPriceScale = it)) }, textColor, accentColor)
        }
        is Indicator.Ichimoku -> {
            StyleRowIndicator("Tenkan-sen", ind.showTenkan ?: true, { onUpdate(ind.copy(showTenkan = it)) }, ind.tenkanColor, ind.tenkanThickness.toInt(), ind.tenkanStyle, { c, t, s -> onUpdate(ind.copy(tenkanColor = c, tenkanThickness = t.toFloat(), tenkanStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Kijun-sen", ind.showKijun ?: true, { onUpdate(ind.copy(showKijun = it)) }, ind.kijunColor, ind.kijunThickness.toInt(), ind.kijunStyle, { c, t, s -> onUpdate(ind.copy(kijunColor = c, kijunThickness = t.toFloat(), kijunStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Chikou Span", ind.showChikou ?: true, { onUpdate(ind.copy(showChikou = it)) }, ind.chikouColor, ind.chikouThickness.toInt(), ind.chikouStyle, { c, t, s -> onUpdate(ind.copy(chikouColor = c, chikouThickness = t.toFloat(), chikouStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Senkou Span A", ind.showSenkouA ?: true, { onUpdate(ind.copy(showSenkouA = it)) }, ind.senkouAColor, ind.senkouAThickness.toInt(), ind.senkouAStyle, { c, t, s -> onUpdate(ind.copy(senkouAColor = c, senkouAThickness = t.toFloat(), senkouAStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Senkou Span B", ind.showSenkouB ?: true, { onUpdate(ind.copy(showSenkouB = it)) }, ind.senkouBColor, ind.senkouBThickness.toInt(), ind.senkouBStyle, { c, t, s -> onUpdate(ind.copy(senkouBColor = c, senkouBThickness = t.toFloat(), senkouBStyle = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            CheckboxRow("Kumo Up", ind.showKumoUp ?: true, { onUpdate(ind.copy(showKumoUp = it)) }, textColor, accentColor)
            if (ind.showKumoUp ?: true) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 36.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.size(32.dp))
                    Box(modifier = Modifier.size(32.dp))
                    ColorBox(ind.kumoUpColor, { onUpdate(ind.copy(kumoUpColor = it)) }, isDarkTheme)
                }
            }
            CheckboxRow("Kumo Down", ind.showKumoDown ?: true, { onUpdate(ind.copy(showKumoDown = it)) }, textColor, accentColor)
            if (ind.showKumoDown ?: true) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 36.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.size(32.dp))
                    Box(modifier = Modifier.size(32.dp))
                    ColorBox(ind.kumoDownColor, { onUpdate(ind.copy(kumoDownColor = it)) }, isDarkTheme)
                }
            }
            CheckboxRow("Etiquettes sur l'echelle de prix", ind.showLabelsOnPriceScale, { onUpdate(ind.copy(showLabelsOnPriceScale = it)) }, textColor, accentColor)
        }
        is Indicator.Sessions -> {
            // Sessions style uses hardcoded defaults (dotted, 0.75dp)
        }
        is Indicator.VWAP -> {
            StyleRowIndicator("VWAP", true, {}, ind.color, ind.thickness.toInt(), ind.style, { c, t, s -> onUpdate(ind.copy(color = c, thickness = t.toFloat(), style = s)) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            if (ind.showBands) {
                StyleRowSimple("Upper Bands Color", ind.upperColor, { onUpdate(ind.copy(upperColor = it)) }, textColor, borderColor, bgColor)
                StyleRowSimple("Lower Bands Color", ind.lowerColor, { onUpdate(ind.copy(lowerColor = it)) }, textColor, borderColor, bgColor)
            }
            CheckboxRow("Etiquettes sur l'echelle de prix", ind.showLabelsOnPriceScale, { onUpdate(ind.copy(showLabelsOnPriceScale = it)) }, textColor, accentColor)
        }
        is Indicator.Ribbon -> {
            StyleRowIndicator("Thickness", true, {}, Color.White, ind.thickness.toInt(), 0, { c, t, s -> onUpdate(ind.copy(thickness = t.toFloat())) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            StyleRowIndicator("Last MA Thickness", true, {}, Color.White, ind.lastThickness.toInt(), 0, { c, t, s -> onUpdate(ind.copy(lastThickness = t.toFloat())) }, textColor, borderColor, isDarkTheme, bgColor, accentColor)
            CheckboxRow("Etiquettes sur l'echelle de prix", ind.showLabelsOnPriceScale, { onUpdate(ind.copy(showLabelsOnPriceScale = it)) }, textColor, accentColor)
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
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(label, color = textColor, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(34.dp)
                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(currentValue, color = textColor, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, null, tint = textColor, modifier = Modifier.size(20.dp))
            }
            if (expanded) {
                val density = LocalDensity.current
                Popup(
                    onDismissRequest = { expanded = false },
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, with(density) { (34.dp + 4.dp).roundToPx() }),
                    properties = PopupProperties(focusable = true)
                ) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .shadow(8.dp, RoundedCornerShape(4.dp))
                            .background(bgColor)
                    ) {
                        Column {
                            options.forEach { option ->
                                Text(
                                    option,
                                    color = textColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOptionSelected(option); expanded = false }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VisibilityContent(indicator: Indicator, onUpdate: (Indicator) -> Unit, textColor: Color, borderColor: Color, accentColor: Color) {
    var minutesExpanded by remember { mutableStateOf(true) }
    var hoursExpanded by remember { mutableStateOf(true) }
    val vis = indicator.timeframeVisibility

    fun updateTimeframeVisibility(newVis: TimeframeVisibility) {
        onUpdate(when (indicator) {
            is Indicator.Volume -> indicator.copy(timeframeVisibility = newVis)
            is Indicator.SMA -> indicator.copy(timeframeVisibility = newVis)
            is Indicator.EMA -> indicator.copy(timeframeVisibility = newVis)
            is Indicator.HMA -> indicator.copy(timeframeVisibility = newVis)
            is Indicator.WMA -> indicator.copy(timeframeVisibility = newVis)
            is Indicator.BollingerBands -> indicator.copy(timeframeVisibility = newVis)
            is Indicator.ATRBands -> indicator.copy(timeframeVisibility = newVis)
            is Indicator.RSI -> indicator.copy(timeframeVisibility = newVis)
            is Indicator.MACD -> indicator.copy(timeframeVisibility = newVis)
            is Indicator.Stochastic -> indicator.copy(timeframeVisibility = newVis)
            is Indicator.ATR -> indicator.copy(timeframeVisibility = newVis)
            is Indicator.Supertrend -> indicator.copy(timeframeVisibility = newVis)
            is Indicator.Alligator -> indicator.copy(timeframeVisibility = newVis)
            is Indicator.Ichimoku -> indicator.copy(timeframeVisibility = newVis)
            is Indicator.Sessions -> indicator.copy(timeframeVisibility = newVis)
            is Indicator.VWAP -> indicator.copy(timeframeVisibility = newVis)
            is Indicator.Ribbon -> indicator.copy(timeframeVisibility = newVis)
        })
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { minutesExpanded = !minutesExpanded }
        ) {
            Icon(
                imageVector = if (minutesExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = "Expand Minutes",
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            CustomCheckbox(
                checked = vis.showMinutes,
                onCheckedChange = { isChecked ->
                    updateTimeframeVisibility(vis.copy(
                        showMinutes = isChecked,
                        showMinute1 = isChecked,
                        showMinute5 = isChecked,
                        showMinute15 = isChecked,
                        showMinute30 = isChecked,
                        showMinute45 = isChecked
                    ))
                },
                modifier = Modifier.padding(end = 12.dp, top = 10.dp, bottom = 10.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Minutes", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
        }

        if (minutesExpanded) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val startX = 10.dp.toPx()
                    val startY = 0f
                    val endY = size.height
                    drawLine(
                        color = borderColor.copy(alpha = 0.5f),
                        start = Offset(startX, startY),
                        end = Offset(startX, endY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                }
                Column(modifier = Modifier.padding(start = 56.dp)) {
                    HourSubOptionsRow("1m", vis.showMinute1, { updateTimeframeVisibility(vis.copy(showMinute1 = it)) }, textColor)
                    HourSubOptionsRow("5m", vis.showMinute5, { updateTimeframeVisibility(vis.copy(showMinute5 = it)) }, textColor)
                    HourSubOptionsRow("15m", vis.showMinute15, { updateTimeframeVisibility(vis.copy(showMinute15 = it)) }, textColor)
                    HourSubOptionsRow("30m", vis.showMinute30, { updateTimeframeVisibility(vis.copy(showMinute30 = it)) }, textColor)
                    HourSubOptionsRow("45m", vis.showMinute45, { updateTimeframeVisibility(vis.copy(showMinute45 = it)) }, textColor)
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = borderColor)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { hoursExpanded = !hoursExpanded }
        ) {
            Icon(
                imageVector = if (hoursExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = "Expand Hours",
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            CustomCheckbox(
                checked = vis.showHours,
                onCheckedChange = { isChecked ->
                    updateTimeframeVisibility(vis.copy(
                        showHours = isChecked,
                        showHour1 = isChecked,
                        showHour2 = isChecked,
                        showHour3 = isChecked,
                        showHour4 = isChecked
                    ))
                },
                modifier = Modifier.padding(end = 12.dp, top = 10.dp, bottom = 10.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Heures", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
        }

        if (hoursExpanded) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val startX = 10.dp.toPx()
                    val startY = 0f
                    val endY = size.height
                    drawLine(
                        color = borderColor.copy(alpha = 0.5f),
                        start = Offset(startX, startY),
                        end = Offset(startX, endY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                }
                Column(modifier = Modifier.padding(start = 56.dp)) {
                    HourSubOptionsRow("1h", vis.showHour1, { updateTimeframeVisibility(vis.copy(showHour1 = it)) }, textColor)
                    HourSubOptionsRow("2h", vis.showHour2, { updateTimeframeVisibility(vis.copy(showHour2 = it)) }, textColor)
                    HourSubOptionsRow("3h", vis.showHour3, { updateTimeframeVisibility(vis.copy(showHour3 = it)) }, textColor)
                    HourSubOptionsRow("4h", vis.showHour4, { updateTimeframeVisibility(vis.copy(showHour4 = it)) }, textColor)
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = borderColor)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        ) {
            Spacer(modifier = Modifier.width(28.dp))
            CustomCheckbox(
                checked = vis.showDaily,
                onCheckedChange = { updateTimeframeVisibility(vis.copy(showDaily = it)) },
                modifier = Modifier.padding(end = 12.dp, top = 10.dp, bottom = 10.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Quotidien", color = textColor, fontSize = 14.sp)
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        ) {
            Spacer(modifier = Modifier.width(28.dp))
            CustomCheckbox(
                checked = vis.showWeekly,
                onCheckedChange = { updateTimeframeVisibility(vis.copy(showWeekly = it)) },
                modifier = Modifier.padding(end = 12.dp, top = 10.dp, bottom = 10.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Hebdomadaire", color = textColor, fontSize = 14.sp)
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        ) {
            Spacer(modifier = Modifier.width(28.dp))
            CustomCheckbox(
                checked = vis.showMonthly,
                onCheckedChange = { updateTimeframeVisibility(vis.copy(showMonthly = it)) },
                modifier = Modifier.padding(end = 12.dp, top = 10.dp, bottom = 10.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Mensuel", color = textColor, fontSize = 14.sp)
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun HourSubOptionsRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, textColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        CustomCheckbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = textColor, fontSize = 14.sp)
    }
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
    val scrollRequest = LocalScrollRequest.current
    val scope = rememberCoroutineScope()
    var rowPosition by remember { mutableFloatStateOf(-1f) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .onGloballyPositioned { rowPosition = it.positionInRoot().y }
    ) {
        Text(label, color = textColor, modifier = Modifier.weight(1f), fontSize = 14.sp, lineHeight = 20.sp)
        SettingsInputBox(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(120.dp),
            textColor = textColor,
            borderColor = borderColor,
            onFocus = {
                scope.launch {
                    android.util.Log.e("SCROLL_DEBUG", "InputRow onFocus rowPosition=$rowPosition")
                    scrollRequest(rowPosition)
                }
            }
        )
    }
}

@Composable
fun CheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, textColor: Color, accentColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp).clickable { onCheckedChange(!checked) }
    ) {
        CustomCheckbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(vertical = 10.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = textColor, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
fun StyleRowIndicator(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, color: Color, thickness: Int, style: Int, onLineSettingsChange: (Color, Int, Int) -> Unit, textColor: Color, borderColor: Color, isDarkTheme: Boolean, bgColor: Color, accentColor: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CustomCheckbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.padding(end = 12.dp, top = 10.dp, bottom = 10.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = textColor, modifier = Modifier.width(100.dp), fontSize = 14.sp, lineHeight = 20.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ColorPickerCell(color, thickness, style, onLineSettingsChange, textColor, borderColor, isDarkTheme)
            StylePickerCell(style, { onLineSettingsChange(color, thickness, it) }, textColor, borderColor, isDarkTheme)
            ThicknessPickerCell(thickness, { onLineSettingsChange(color, it, style) }, textColor, borderColor, isDarkTheme, accentColor)
        }
    }
}

@Composable
private fun ColorPickerCell(color: Color, thickness: Int, style: Int, onLineSettingsChange: (Color, Int, Int) -> Unit, textColor: Color, borderColor: Color, isDarkTheme: Boolean) {
    var showPicker by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val popY = with(density) { 33.dp.roundToPx() }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                            .padding(4.dp)
                            .background(color, RoundedCornerShape(2.dp))
                            .border(0.5.dp, borderColor, RoundedCornerShape(2.dp))
                            .clickable { showPicker = true }
                    )
                    if (showPicker) {
                        ColorPickerPopup(
                            selectedColor = color,
                            onColorSelected = { c -> onLineSettingsChange(c, thickness, style) },
                            onDismiss = { showPicker = false },
                            isDarkTheme = isDarkTheme,
                            positionX = 0,
                            positionY = popY,
                            onLineSettingsChange = null
                        )
                    }
                }
        Spacer(modifier = Modifier.height(2.dp))
        Text("Couleur", color = textColor.copy(0.5f), fontSize = 10.sp)
    }
}

@Composable
private fun StylePickerCell(style: Int, onStyleChange: (Int) -> Unit, textColor: Color, borderColor: Color, isDarkTheme: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                    .clickable { expanded = true },
                contentAlignment = Alignment.Center
            ) {
                val previewColor = if (isDarkTheme) Color.White.copy(0.7f) else Color.Black.copy(0.5f)
                Canvas(modifier = Modifier.size(26.dp, 14.dp)) {
                    val pathEffect = when(style) {
                        1 -> PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                        2 -> PathEffect.dashPathEffect(floatArrayOf(0.1f, 4f), 0f)
                        else -> null
                    }
                    val cap = if (style == 2) StrokeCap.Round else StrokeCap.Butt
                    drawLine(color = previewColor, start = Offset(0f, size.height/2), end = Offset(size.width, size.height/2), strokeWidth = 3f, pathEffect = pathEffect, cap = cap)
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(if (isDarkTheme) Color(0xFF1e222d) else Color.White)
            ) {
                listOf(0 to "Solide", 1 to "Tirets", 2 to "Pointillés").forEach { (s, label) ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Canvas(modifier = Modifier.size(36.dp, 12.dp)) {
                                    val eff = when (s) {
                                        1 -> PathEffect.dashPathEffect(floatArrayOf(8f, 5f), 0f)
                                        2 -> PathEffect.dashPathEffect(floatArrayOf(0.1f, 5f), 0f)
                                        else -> null
                                    }
                                    drawLine(color = textColor, start = Offset(0f, size.height/2), end = Offset(size.width, size.height/2), strokeWidth = 3f, pathEffect = eff, cap = if (s == 2) StrokeCap.Round else StrokeCap.Butt)
                                }
                                Text(label, color = textColor, fontSize = 13.sp)
                            }
                        },
                        onClick = { onStyleChange(s); expanded = false },
                        modifier = Modifier.height(34.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text("Style", color = textColor.copy(0.5f), fontSize = 10.sp)
    }
}

@Composable
private fun ThicknessPickerCell(thickness: Int, onThicknessChange: (Int) -> Unit, textColor: Color, borderColor: Color, isDarkTheme: Boolean, accentColor: Color) {
    var expanded by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                    .padding(6.dp)
                    .clickable { expanded = true },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((thickness.coerceIn(1, 4)).dp)
                        .background(if (isDarkTheme) Color.White else Color.Black, RoundedCornerShape(1.dp))
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(if (isDarkTheme) Color(0xFF1e222d) else Color.White)
            ) {
                (1..4).forEach { t ->
                    val isSelected = thickness == t
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(30.dp)
                                        .height(t.dp)
                                        .background(if (isSelected) accentColor else textColor, RoundedCornerShape(1.dp))
                                )
                                Text("$t", color = if (isSelected) accentColor else textColor, fontSize = 13.sp)
                            }
                        },
                        onClick = { onThicknessChange(t); expanded = false },
                        modifier = Modifier.height(34.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text("Épaisseur", color = textColor.copy(0.5f), fontSize = 10.sp)
    }
}

@Composable
fun StyleRowSimple(label: String, color: Color, onColorChange: (Color) -> Unit, textColor: Color, borderColor: Color, bgColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
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
        CustomCheckbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(end = 12.dp, top = 10.dp, bottom = 10.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = textColor, modifier = Modifier.width(100.dp), fontSize = 14.sp)
        ColorBox(color, onColorChange, isDarkTheme)
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun LevelStyleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, color: Color, onColorChange: (Color) -> Unit, value: Int, onValueChange: (Int) -> Unit, textColor: Color, borderColor: Color, isDarkTheme: Boolean, bgColor: Color, accentColor: Color) {
    val scrollRequest = LocalScrollRequest.current
    val scope = rememberCoroutineScope()
    var rowPosition by remember { mutableFloatStateOf(-1f) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .onGloballyPositioned { rowPosition = it.positionInRoot().y }
    ) {
        CustomCheckbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(end = 12.dp, top = 10.dp, bottom = 10.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
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
                borderColor = borderColor,
                onFocus = {
                    scope.launch {
                        android.util.Log.e("SCROLL_DEBUG", "LevelStyleRow onFocus")
                        scrollRequest(rowPosition)
                    }
                }
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Champ de texte natif Android (EditText) via AndroidView.
 * Utilise le système de popup Android au lieu de Compose PopupLayout,
 * ce qui évite la boucle de recomposition dans les Dialog.
 */
@Composable
fun AndroidEditTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    textColor: Color,
    fontSizeSp: Float,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    onFocus: (() -> Unit)? = null,  // Callback optionnel appelé quand le champ reçoit le focus
    tag: Any? = null  // Tag stable pour retrouver le champ après recyclage
) {
    val editTextHolder = LocalEditTextHolder.current
    val rootView = LocalView.current.rootView
    var isUpdatingFromCompose by remember { mutableStateOf(false) }
    android.util.Log.e("FOCUS_DEBUG", "AndroidEditTextInput composé")
    // Stocker le tag pour restauration du focus après scroll
    LaunchedEffect(tag) {
        if (tag != null && editTextHolder.pendingTag == tag) {
            val found = rootView.findViewWithTag<android.widget.EditText>(tag)
            if (found != null && found.hasWindowFocus()) {
                found.requestFocus()
                editTextHolder.pendingTag = null
            }
        }
    }
    AndroidView(
        factory = { context ->
            EditText(context).apply {
                this.isSingleLine = true
                this.isEnabled = enabled
                if (tag != null) this.tag = tag
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
                setTextColor(android.graphics.Color.argb(
                    (textColor.alpha * 255).toInt(),
                    (textColor.red * 255).toInt(),
                    (textColor.green * 255).toInt(),
                    (textColor.blue * 255).toInt()
                ))
                setHintTextColor(android.graphics.Color.argb(
                    (textColor.alpha * 128).toInt(),
                    (textColor.red * 255).toInt(),
                    (textColor.green * 255).toInt(),
                    (textColor.blue * 255).toInt()
                ))
                background = null
                setPadding(0, 0, 0, 0)
                gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
                val inputType = when (keyboardType) {
                    KeyboardType.Number -> EditorInfo.TYPE_CLASS_NUMBER
                    KeyboardType.NumberPassword -> EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD
                    else -> EditorInfo.TYPE_CLASS_TEXT
                }
                this.inputType = inputType
                setSelection(0, text.length)
                android.util.Log.e("FOCUS_DEBUG", "EditText factory créé")
                // Listener: propage les changements de texte vers Compose
                addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        if (!isUpdatingFromCompose) {
                            onValueChange(s?.toString() ?: "")
                        }
                    }
                })
                // Listener: commit au blur (perte de focus) ET notify au focus
                onFocusChangeListener = android.view.View.OnFocusChangeListener { v, hasFocus ->
                    android.util.Log.e("FOCUS_DEBUG", "onFocusChange hasFocus=$hasFocus")
                    if (hasFocus) {
                        // Stocker la référence et le tag pour restauration après scroll
                        val et = v as EditText
                        editTextHolder.editText = et
                        editTextHolder.pendingTag = et.tag
                        // Appeler le callback onFocus s'il est fourni
                        onFocus?.invoke()
                    } else {
                        // Commit au blur
                        onCommit()
                    }
                }
                // Forcer la prise de focus au premier toucher
                setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        v.requestFocus()
                    }
                    false
                }
                // Supprimer la barre flottante (Coller/Sélectionner/Tout)
                customInsertionActionModeCallback = object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
                    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
                    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean = false
                    override fun onDestroyActionMode(mode: ActionMode?) {}
                }
                customSelectionActionModeCallback = object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
                    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
                    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean = false
                    override fun onDestroyActionMode(mode: ActionMode?) {}
                }
            }
        },
        update = { editText ->
            android.util.Log.e("FOCUS_DEBUG", "update block appelé isFocused=${editText.isFocused}")
            // Sync texte depuis Compose vers Android
            isUpdatingFromCompose = true
            if (editText.text.toString() != value) {
                editText.setText(value)
                editText.setSelection(value.length)
            }
            isUpdatingFromCompose = false
            editText.isEnabled = enabled
        },
        modifier = modifier
    )
}

@Composable
fun SettingsInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color,
    borderColor: Color,
    enabled: Boolean = true,
    onFocus: (() -> Unit)? = null
) {
    var textFieldValue by remember { mutableStateOf(value) }
    LaunchedEffect(value) { textFieldValue = value }

    AndroidEditTextInput(
        value = textFieldValue,
        onValueChange = { textFieldValue = it },
        onCommit = { if (textFieldValue != value) onValueChange(textFieldValue) },
        textColor = if (enabled) textColor else textColor.copy(0.5f),
        fontSizeSp = 13f,
        enabled = enabled,
        keyboardType = KeyboardType.Number,
        modifier = modifier
            .height(34.dp)
            .border(1.dp, if (enabled) borderColor else borderColor.copy(0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp),
        onFocus = onFocus
    )
}
