package com.bthr.backtest.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bthr.backtest.model.Indicator

@Composable
fun IndicatorLabelToolbar(
    indicator: Indicator,
    isExpanded: Boolean,
    values: List<Pair<String, Color>>,
    textColor: Color,
    surfaceColor: Color,
    onToggleExpand: () -> Unit,
    onToggleVisibility: () -> Unit,
    onSettings: () -> Unit,
    onRemove: () -> Unit
) {
    val titleFontSize = 12.sp
    val valueFontSize = 10.sp

    Surface(
        color = if (isExpanded) surfaceColor.copy(0.8f) else Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .height(22.dp)
            .then(
                if (isExpanded) Modifier.border(
                    0.5.dp,
                    textColor.copy(0.1f),
                    RoundedCornerShape(4.dp)
                ) else Modifier
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            // Titre de l'indicateur
            Text(
                indicator.name,
                color = if (indicator.isVisible) textColor else textColor.copy(0.4f),
                fontSize = titleFontSize,
                lineHeight = titleFontSize,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clickable { onToggleExpand() }
                    .padding(end = 4.dp) // Réduit pour rapprocher les icônes/valeurs
            )

            // Valeurs de données (Professionnel)
            if (indicator.isVisible) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    values.forEach { (valStr, color) ->
                        Text(
                            valStr,
                            color = color,
                            fontSize = valueFontSize,
                            lineHeight = valueFontSize,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.padding(end = 4.dp) // Réduit
                        )
                    }
                }
            }

            AnimatedVisibility(
                isExpanded,
                enter = expandHorizontally(),
                exit = shrinkHorizontally()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 2.dp) // Réduit l'espace entre texte/valeurs et icônes
                ) {
                    IconButton(onClick = onToggleVisibility, modifier = Modifier.size(18.dp)) {
                        Icon(
                            if (indicator.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            null,
                            tint = textColor.copy(0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(onClick = onSettings, modifier = Modifier.size(18.dp)) {
                        Icon(
                            Icons.Default.Settings,
                            null,
                            tint = textColor.copy(0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(18.dp)) {
                        Icon(
                            Icons.Default.Close,
                            null,
                            tint = textColor.copy(0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
