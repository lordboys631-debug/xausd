package com.bthr.backtest.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bthr.backtest.model.Indicator
import kotlin.math.roundToInt

data class IndicatorItem(
    val key: String,
    val displayName: String,
    val abbreviation: String,
    val factory: () -> Indicator
)

val allIndicatorItems = listOf(
    IndicatorItem("Volume",     "Volume",                              "Vol",         { Indicator.Volume() }),
    IndicatorItem("SMA",        "Moyenne Mobile Simple",               "SMA",         { Indicator.SMA() }),
    IndicatorItem("EMA",        "Moyenne Mobile Exponentielle",        "EMA",         { Indicator.EMA() }),
    IndicatorItem("HMA",        "Moyenne Mobile de Hull",              "HMA",         { Indicator.HMA() }),
    IndicatorItem("VWAP",       "Prix Moyen Pondéré / Volume",         "VWAP",        { Indicator.VWAP() }),
    IndicatorItem("Bollinger",  "Bandes de Bollinger",                 "BB",          { Indicator.BollingerBands() }),
    IndicatorItem("ATR Bands",  "Bandes ATR",                          "ATR Bands",   { Indicator.ATRBands() }),
    IndicatorItem("RSI",        "Indice de Force Relative",            "RSI",         { Indicator.RSI() }),
    IndicatorItem("MACD",       "Convergence/Divergence des MM",       "MACD",        { Indicator.MACD() }),
    IndicatorItem("Stochastic", "Oscillateur Stochastique",            "Stoch",       { Indicator.Stochastic() }),
    IndicatorItem("ATR",        "Plage Vraie Moyenne",                 "ATR",         { Indicator.ATR() }),
    IndicatorItem("Supertrend", "Supertendance",                       "ST",          { Indicator.Supertrend() }),
    IndicatorItem("Alligator",  "Alligator de Williams",               "Alligator",   { Indicator.Alligator() }),
    IndicatorItem("Ichimoku",   "Ichimoku Kinko Hyo",                  "Ichimoku",    { Indicator.Ichimoku() }),
    IndicatorItem("Sessions",   "Sessions de Marché",                  "Sessions",    { Indicator.Sessions() }),
    IndicatorItem("Ribbon",     "Ruban de Moyennes Mobiles",           "MA Ribbon",   { Indicator.Ribbon() })
)

@Composable
fun IndicatorsMenu(
    onIndicatorSelected: (Indicator) -> Unit,
    onDismissRequest: () -> Unit,
    isDarkTheme: Boolean = true,
    favoriteIndicators: Set<String> = emptySet(),
    onFavoritesChange: (Set<String>) -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("chart_prefs", Context.MODE_PRIVATE) }

    var favorites by remember { mutableStateOf(favoriteIndicators) }
    var searchQuery by remember { mutableStateOf("") }

    val colors = getDrawingToolsMenuColors(isDarkTheme)

    // Plein écran pour les indicateurs
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

                // Texte "Indicateurs" au centre
                Text(
                    text = "Indicateurs",
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
                    
                    // Bouton "X" à droite quand du texte est saisi
                    if (searchQuery.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Effacer",
                                tint = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Liste des indicateurs
            val allIndicators = allIndicatorItems
            
            val filteredIndicators = if (searchQuery.isEmpty()) {
                allIndicators
            } else {
                allIndicators.filter { indicator ->
                    indicator.displayName.contains(searchQuery, ignoreCase = true) ||
                    indicator.abbreviation.contains(searchQuery, ignoreCase = true)
                }
            }
            
            // Trier pour afficher les favoris en haut
            val sortedIndicators = filteredIndicators.sortedWith(compareBy<IndicatorItem> { !favorites.contains(it.key) }.thenBy { it.displayName })

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedIndicators) { indicator ->
                    IndicatorRowWithFavorite(
                        indicator = indicator,
                        isFavorite = favorites.contains(indicator.key),
                        textColor = colors.textColor,
                        cardBgColor = colors.cardBgColor,
                        borderColor = colors.borderColor,
                        onFavoriteToggle = {
                            favorites = if (favorites.contains(indicator.key)) {
                                favorites - indicator.key
                            } else {
                                favorites + indicator.key
                            }
                            onFavoritesChange(favorites)
                            sharedPrefs.edit().putString("favorite_indicators", favorites.joinToString(",")).apply()
                        },
                        onClick = {
                            onIndicatorSelected(indicator.factory())
                            onDismissRequest()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun IndicatorRowWithFavorite(
    indicator: IndicatorItem,
    isFavorite: Boolean,
    textColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBgColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor.copy(0.25f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Étoile de favoris à gauche
        IconButton(
            onClick = onFavoriteToggle,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = if (isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
                tint = if (isFavorite) Color(0xFFFFA500) else textColor.copy(0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Contenu de l'indicateur
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = indicator.displayName,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "(${indicator.abbreviation})",
                color = textColor.copy(0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun IndicatorRow(
    item: IndicatorItem,
    textColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBgColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor.copy(0.25f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "(${item.abbreviation})",
            color = textColor.copy(0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal
        )
    }
}








