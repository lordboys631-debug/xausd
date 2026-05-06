# Résumé des modifications - Fonctionnalité de Traçage de Ligne de Tendance

## Fichier modifié
- `C:\Users\is\Desktop\backtest\app\src\main\java\com\bthr\backtest\ui\components\CandlestickChart.kt`

## Modifications détaillées

### 1. Ajout des états de traçage (lignes 94-98)
```kotlin
// Trend line drawing state
var isTrendLineMode by remember { mutableStateOf(false) }
var trendLinePoint1 by remember { mutableStateOf<Offset?>(null) }
var trendLinePoint2 by remember { mutableStateOf<Offset?>(null) }
var trendLineCrosshair by remember { mutableStateOf<Offset?>(null) }
```

Ces états gèrent :
- `isTrendLineMode` : Si le mode de traçage est actif
- `trendLinePoint1` : Position du premier point cliqué
- `trendLinePoint2` : Position du deuxième point cliqué
- `trendLineCrosshair` : Position actuelle du viseur

### 2. Modification du gestionnaire pointerInput pour dragStart (lignes 302-309)
```kotlin
.pointerInput(bottomIndicators, isLongPressing, isTrendLineMode) {
    detectDragGestures(
        onDragStart = { offset ->
            if (isTrendLineMode && offset.x <= chartWidthPx && offset.y <= mainH) {
                dragArea = 0
                return@detectDragGestures
            }
            if (isLongPressing) {
                dragArea = 0
                return@detectDragGestures
            }
```

Permet de configurer dragArea = 0 pour le mode de traçage de ligne.

### 3. Modification du gestionnaire onDrag (lignes 356-363)
```kotlin
when (dragArea) {
    0 -> {
        if (isTrendLineMode && trendLinePoint1 != null && trendLinePoint2 == null) {
            // Update crosshair position while in trend line drawing mode
            trendLineCrosshair = trendLineCrosshair?.let { 
                Offset((it.x + dragAmount.x).coerceIn(0f, chartWidthPx), (it.y + dragAmount.y).coerceIn(0f, mainH)) 
            }
        } else if (isLongPressing) crosshairPosition = ...
    }
```

Met à jour la position du viseur pendant le glissement.

### 4. Modification du gestionnaire onTap (lignes 465-479)
```kotlin
if (isTrendLineMode && offset.x <= chartWidthPx && offset.y <= mainH) {
    // Trend line drawing mode
    if (trendLinePoint1 == null) {
        // First point selected
        trendLinePoint1 = offset
        trendLineCrosshair = offset
    } else if (trendLinePoint2 == null) {
        // Second point selected - line complete
        trendLinePoint2 = offset
        // Reset mode after a short delay so user can see the line
        isTrendLineMode = false
        trendLinePoint1 = null
        trendLinePoint2 = null
        trendLineCrosshair = null
    }
}
```

Gère la sélection des deux points de la ligne.

### 5. Modification du gestionnaire onLongPress (lignes 757-765)
```kotlin
onLongPress = { offset ->
    if (isTrendLineMode && trendLinePoint1 != null && trendLinePoint2 == null) {
        // Cancel trend line drawing on long press
        isTrendLineMode = false
        trendLinePoint1 = null
        trendLinePoint2 = null
        trendLineCrosshair = null
    } else if (offset.x <= chartWidthPx) { 
        isLongPressing = true; crosshairPosition = offset 
    }
}
```

Permet d'annuler le traçage avec un long press.

### 6. Ajout du rendu du viseur et de la ligne (lignes 860-902)
```kotlin
// Draw trend line if in drawing mode
if (isTrendLineMode && trendLinePoint1 != null) {
    // Draw first point circle
    drawCircle(color = Color.Yellow, radius = 5f, center = trendLinePoint1!!)
    
    // Draw crosshair
    trendLineCrosshair?.let { crosshair ->
        // Vertical line
        drawLine(color = Color.Yellow.copy(alpha = 0.5f), ...)
        // Horizontal line
        drawLine(color = Color.Yellow.copy(alpha = 0.5f), ...)
        // Center circle
        drawCircle(color = Color.Yellow, radius = 4f, center = crosshair)
    }
    
    // Draw line between point1 and crosshair (preview)
    trendLineCrosshair?.let { crosshair ->
        drawLine(color = Color.Yellow.copy(alpha = 0.7f), 
                 start = trendLinePoint1!!, end = crosshair, strokeWidth = 2f)
    }
}
```

Dessine le viseur et la ligne de prévisualisation.

### 7. Modification du gestionnaire des icônes de favoris (lignes 1241-1265)
```kotlin
favoritesToolsList.forEach { tool ->
    Icon(
        painter = painterResource(id = tool.iconRes),
        contentDescription = null,
        tint = if (isTrendLineMode && tool == DrawingTool.TREND_LINE) 
            Color(0xFFFFD700) 
        else 
            Color(0xFF555555).copy(alpha = 0.85f),
        modifier = Modifier
            .size(24.dp)
            .clickable {
                if (!isDraggingFavoritesBar) {
                    if (tool == DrawingTool.TREND_LINE) {
                        // Toggle trend line drawing mode
                        isTrendLineMode = !isTrendLineMode
                        if (!isTrendLineMode) {
                            trendLinePoint1 = null
                            trendLinePoint2 = null
                            trendLineCrosshair = null
                        }
                    } else {
                        onFavoriteToolsChange(favoriteTools - tool.name)
                    }
                }
            }
    )
}
```

Ajoute un bouton pour activer/désactiver le mode de traçage avec feedback visuel (couleur dorée).

## Flux utilisateur
1. Utilisateur clique sur l'icône "Ligne de tendance" → `isTrendLineMode = true`
2. Utilisateur clique sur le graphique (point 1) → `trendLinePoint1` et `trendLineCrosshair` sont définis
3. Utilisateur glisse son doigt/souris → `trendLineCrosshair` se met à jour en temps réel
4. Le viseur et la ligne de prévisualisation s'affichent
5. Utilisateur clique sur le point 2 → `trendLinePoint2` est défini et le mode se désactive
6. La ligne finale est dessinée

## Couleurs et styles
- **Viseur actif** : Jaune avec alpha 0.5 pour la transparence
- **Premier point** : Cercle jaune (radius = 5f)
- **Point du viseur** : Cercle jaune (radius = 4f)
- **Ligne de prévisualisation** : Jaune avec alpha 0.7f, strokeWidth = 2f

## Points d'amélioration futurs
1. Sauvegarder les lignes tracées dans une base de données
2. Permettre la modification des lignes existantes
3. Permettre la suppression des lignes
4. Ajouter des outils de dessin supplémentaires
5. Personnaliser les couleurs et l'épaisseur des lignes

