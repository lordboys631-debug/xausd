# Documentation Technique Complète - Traçage de Lignes de Tendance

## Architecture

### Composant Principal : CandlestickChart

Le composant `CandlestickChart` est une composable Jetpack Compose qui affiche un graphique en chandelier avec support complet des interactions tactiles et souris.

### États de traçage

Trois états supplémentaires ont été ajoutés au composant :

```kotlin
var isTrendLineMode by remember { mutableStateOf(false) }     // Flag d'activation du mode
var trendLinePoint1 by remember { mutableStateOf<Offset?>(null) }      // Premier point (px)
var trendLinePoint2 by remember { mutableStateOf<Offset?>(null) }      // Deuxième point (px)
var trendLineCrosshair by remember { mutableStateOf<Offset?>(null) }   // Pos. du viseur (px)
```

### Flux de données

```
Utilisateur
    ↓
Clic sur icône → isTrendLineMode = true
    ↓
Clic sur graphique → trendLinePoint1 = offset, trendLineCrosshair = offset
    ↓
Glissement souris → trendLineCrosshair.x/y mis à jour
    ↓
Clic final → trendLinePoint2 = offset, mode désactivé
    ↓
Rendu → Viseur et ligne affichés sur le Canvas
```

## Implémentation détaillée

### 1. Gestion des entrées tactiles

#### Détection du drag (onDragStart)
```kotlin
.pointerInput(bottomIndicators, isLongPressing, isTrendLineMode) {
    detectDragGestures(
        onDragStart = { offset ->
            if (isTrendLineMode && offset.x <= chartWidthPx && offset.y <= mainH) {
                dragArea = 0  // Zone 0 = graphique principal
                return@detectDragGestures
            }
            // ... autres cas ...
        }
    )
}
```

**Explication** :
- Vérifie que l'utilisateur est en mode traçage
- Vérifie que le clic est dans la zone valide (graphique)
- Force `dragArea = 0` pour le gestionnaire de drag

#### Mise à jour en temps réel (onDrag)
```kotlin
when (dragArea) {
    0 -> {
        if (isTrendLineMode && trendLinePoint1 != null && trendLinePoint2 == null) {
            trendLineCrosshair = trendLineCrosshair?.let { 
                Offset(
                    (it.x + dragAmount.x).coerceIn(0f, chartWidthPx),
                    (it.y + dragAmount.y).coerceIn(0f, mainH)
                ) 
            }
        }
    }
}
```

**Explication** :
- Ajoute le delta de mouvement (`dragAmount`) à la position actuelle
- Utilise `coerceIn()` pour rester dans les limites du graphique
- Mise à jour en temps réel pendant le glissement

#### Sélection des points (onTap)
```kotlin
if (isTrendLineMode && offset.x <= chartWidthPx && offset.y <= mainH) {
    if (trendLinePoint1 == null) {
        trendLinePoint1 = offset
        trendLineCrosshair = offset
    } else if (trendLinePoint2 == null) {
        trendLinePoint2 = offset
        // Réinitialiser
        isTrendLineMode = false
        trendLinePoint1 = null
        trendLinePoint2 = null
        trendLineCrosshair = null
    }
}
```

**Explication** :
- Premier clic : défini le point de départ et initialise le viseur
- Deuxième clic : finalise la ligne et réinitialise tous les états

#### Annulation (onLongPress)
```kotlin
onLongPress = { offset ->
    if (isTrendLineMode && trendLinePoint1 != null && trendLinePoint2 == null) {
        isTrendLineMode = false
        trendLinePoint1 = null
        trendLinePoint2 = null
        trendLineCrosshair = null
    }
}
```

### 2. Rendu visuel (Canvas)

#### Dessin du premier point
```kotlin
if (isTrendLineMode && trendLinePoint1 != null) {
    drawCircle(
        color = Color.Yellow,
        radius = 5f,
        center = trendLinePoint1!!
    )
}
```

#### Dessin du viseur (croix + cercle)
```kotlin
trendLineCrosshair?.let { crosshair ->
    // Ligne verticale
    drawLine(
        color = Color.Yellow.copy(alpha = 0.5f),
        start = Offset(crosshair.x, 0f),
        end = Offset(crosshair.x, mainH),
        strokeWidth = 1f
    )
    // Ligne horizontale
    drawLine(
        color = Color.Yellow.copy(alpha = 0.5f),
        start = Offset(0f, crosshair.y),
        end = Offset(chartWidthPx, crosshair.y),
        strokeWidth = 1f
    )
    // Cercle central
    drawCircle(
        color = Color.Yellow,
        radius = 4f,
        center = crosshair
    )
}
```

#### Dessin de la ligne de prévisualisation
```kotlin
trendLineCrosshair?.let { crosshair ->
    drawLine(
        color = Color.Yellow.copy(alpha = 0.7f),
        start = trendLinePoint1!!,
        end = crosshair,
        strokeWidth = 2f
    )
}
```

#### Dessin de l'icône d'activation
```kotlin
Icon(
    painter = painterResource(id = tool.iconRes),
    contentDescription = null,
    tint = if (isTrendLineMode && tool == DrawingTool.TREND_LINE) 
        Color(0xFFFFD700)  // Doré quand actif
    else 
        Color(0xFF555555).copy(alpha = 0.85f),  // Gris normal
    // ...
)
```

## Performances

### Optimisations actuelles
1. **Vérifications précoces** : Les conditions sont testées en premier pour éviter les calculs inutiles
2. **Utilisation de `.let`** : Évite les null checks répétés
3. **Canvas natif** : Utilise le système de rendu natif Compose pour les dessin
4. **États locaux** : Les états ne sont pas propagés vers le haut

### Potentiel d'amélioration
- Implémenter la sérialisation des lignes pour la persistance
- Ajouter un système de cache pour les lignes fréquemment utilisées
- Utiliser un `Path` réutilisable pour le viseur

## Compatibilité

### Versions Android
- Minimum : SDK 24 (Android 7.0)
- Target : SDK 34 (Android 14)

### Dépendances Compose
- androidx.compose.foundation
- androidx.compose.material3
- androidx.compose.ui

## Limitations actuelles

1. **Pas de persistance** : Les lignes ne sont pas sauvegardées
2. **Pas de modification** : On ne peut pas éditer une ligne après création
3. **Pas de suppression** : On ne peut pas effacer les lignes tracées
4. **Un outil à la fois** : Un seul mode de dessin peut être actif
5. **Pas de stockage** : Les lignes sont réinitialisées au redémarrage de l'app

## Améliorations recommandées

### Court terme (1-2 sprints)
1. Implémenter la persistance des lignes dans une base de données
   ```kotlin
   data class TrendLineDrawing(
       val id: String,
       val startPoint: Pair<Long, Float>,  // timestamp, price
       val endPoint: Pair<Long, Float>,
       val color: Color = Color.Yellow,
       val strokeWidth: Float = 2f,
       val timestamp: Long = System.currentTimeMillis()
   )
   ```

2. Ajouter un ViewModel pour gérer les lignes
   ```kotlin
   class TrendLineViewModel : ViewModel() {
       val trendLines = mutableStateListOf<TrendLineDrawing>()
       
       fun saveTrendLine(drawing: TrendLineDrawing) { /* ... */ }
       fun deleteTrendLine(id: String) { /* ... */ }
       fun loadTrendLines() { /* ... */ }
   }
   ```

3. Implémenter la modification des lignes existantes

### Moyen terme (3-4 sprints)
1. Ajouter d'autres outils de dessin (rectangles, cercles, texte)
2. Système de couleurs personnalisables
3. Système de couches pour organiser les dessins
4. Export des dessins (PNG, PDF)

### Long terme (5+ sprints)
1. Synchronisation cloud des dessins
2. Partage des dessins avec d'autres utilisateurs
3. Bibliothèque de modèles de dessins
4. IA pour les dessins suggérés

## Debugging

### Logs recommandés
```kotlin
Log.d("TrendLine", "Mode activated: $isTrendLineMode")
Log.d("TrendLine", "Point1: $trendLinePoint1")
Log.d("TrendLine", "Point2: $trendLinePoint2")
Log.d("TrendLine", "Crosshair: $trendLineCrosshair")
```

### Cas problématiques à surveiller
1. Clic rapide : Peut sélectionner deux points trop proches
2. Glissement rapide : Peut sortir des limites
3. Changement d'orientation : À tester sur device rotatif

## Conclusion

L'implémentation actuelle fournit une base solide pour le traçage de lignes de tendance. Elle est facile à étendre avec d'autres outils de dessin et peut servir de fondation pour un système de dessin complet dans l'application.

