package com.bthr.backtest.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ThemeToggle(
    isDark: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbOffset by animateFloatAsState(
        targetValue = if (isDark) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "thumb_offset"
    )

    Box(
        modifier = modifier
            .width(56.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF2A2E39) else Color(0xFFE8E8E8))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggle(!isDark) },
        contentAlignment = Alignment.CenterStart
    ) {
        // Background icons (subtle)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.LightMode,
                contentDescription = null,
                tint = if (!isDark) Color(0xFFFFA000).copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.2f),
                modifier = Modifier
                    .size(14.dp)
                    .padding(start = 8.dp)
            )
            Icon(
                imageVector = Icons.Filled.DarkMode,
                contentDescription = null,
                tint = if (isDark) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.2f),
                modifier = Modifier
                    .size(14.dp)
                    .padding(end = 8.dp)
            )
        }

        // Thumb with icon
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(28.dp)
                .offset(x = (24.dp * thumbOffset))
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF1E222D) else Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                contentDescription = if (isDark) "Dark" else "Light",
                tint = if (isDark) Color(0xFF90CAF9) else Color(0xFFFFA000),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
