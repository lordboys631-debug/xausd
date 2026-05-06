# 📚 EXPLICATION - Comment fonctionne le Viseur (Crosshair) existant

## 🎯 Vue d'ensemble

Le viseur (crosshair) existant fonctionne en 4 étapes principales:

1. **État** : Variable d'état pour stocker la position
2. **Activation** : Long press pour activer
3. **Suivi** : Glissement pour mettre à jour la position
4. **Dessin** : Rendu visuel du viseur

---

## 📊 Détail du fonctionnement

### 1️⃣ ÉTAT (Variable de mémoire)

```kotlin
var crosshairPosition by remember { mutableStateOf<Offset?>(null) }
var isLongPressing by remember { mutableStateOf(false) }
```

- `crosshairPosition` : Stocke la position du viseur (X, Y)
- `isLongPressing` : Indique si on appuie long
- `null` = viseur désactivé
- `Offset(x, y)` = viseur actif à cette position

---

### 2️⃣ ACTIVATION (onLongPress)

```kotlin
onLongPress = { offset ->
    if (offset.x <= chartWidthPx) {
        isLongPressing = true
        crosshairPosition = offset  // ← Activer le viseur à cette position
    }
}
```

**Quand l'utilisateur fait un long press:**
- `isLongPressing` devient `true`
- `crosshairPosition` reçoit la position du doigt
- Le viseur s'affiche!

---

### 3️⃣ SUIVI (onDrag)

```kotlin
when (dragArea) {
    0 -> {
        if (isLongPressing) {
            crosshairPosition = crosshairPosition?.let { 
                Offset(
                    (it.x + dragAmount.x).coerceIn(0f, chartWidthPx),
                    (it.y + dragAmount.y).coerceIn(0f, h)
                )
            }
        }
    }
}
```

**Pendant le glissement:**
- Le dragAmount = changement de position du doigt
- On ajoute dragAmount à la position actuelle
- `coerceIn()` = garder le viseur dans les limites du graphique
- Le viseur suit le doigt en temps réel!

---

### 4️⃣ DESSIN (Rendu visuel)

```kotlin
crosshairPosition?.let { p ->
    ChartDrawer.drawCrosshair(
        this, p,  // ← Position du viseur
        chartWidthPx, h, mainH, 
        priceWidthPx, timeHeightPx,
        settings, crosshairTextStyle, textMeasurer, 
        allCandles, scrollOffset, candleW, 
        crosshairTimeFormatter, denormY, 
        bottomIndicators, indicatorHeights, 
        indicatorRanges, defaultHeightPx, 
        crosshairLabelBgColor,
        timeframe, displayCount
    )
}
```

**Le dessin:**
- Uniquement si `crosshairPosition != null`
- Appelle `ChartDrawer.drawCrosshair()` (dans un fichier séparé)
- Passe la position et tous les paramètres nécessaires
- Affiche les lignes, cercles, et infos textuelles

---

### 5️⃣ DÉSACTIVATION (onTap)

```kotlin
onTap = {
    if (isLongPressing) {
        isLongPressing = false
        crosshairPosition = null  // ← Désactiver le viseur
    }
}
```

**Quand l'utilisateur tappe (relâche):**
- `isLongPressing` devient `false`
- `crosshairPosition` devient `null`
- Le viseur disparaît!

---

## 🔄 Flux complet

```
USER ACTION          STATE                   VISEUR
─────────────────────────────────────────────────────

Long press
   ↓
offset = (150, 300)  crosshairPosition = (150, 300)
isLongPressing = true
                     ✅ VISEUR APPARAÎT
                        à (150, 300)
   ↓
Glisse le doigt
dragAmount = (+10, +20)
                     crosshairPosition = (160, 320)
                     ✅ VISEUR BOUGE
                        à (160, 320)
   ↓
Glisse encore
dragAmount = (-5, +15)
                     crosshairPosition = (155, 335)
                     ✅ VISEUR BOUGE
                        à (155, 335)
   ↓
Tappe (relâche)
                     crosshairPosition = null
isLongPressing = false
                     ✅ VISEUR DISPARAÎT
```

---

## 🎨 Propriétés du Crosshair

D'après le code, le viseur affiche:

1. **Lignes croisées** : Verticale et horizontale
2. **Cercle central** : Au point de convergence
3. **Infos textuelles** : Prix et heure
4. **Couleur** : Selon le schéma de couleurs
5. **Position** : Suit exactement le doigt

---

## ✅ Points clés

1. **État mutable** : `mutableStateOf<Offset?>` permet la réaction
2. **Long press** : Activateur du viseur
3. **Glissement** : Suivi en temps réel
4. **coerceIn()** : Garde le viseur dans les limites
5. **Dessin externalisé** : `ChartDrawer.drawCrosshair()`
6. **Tap** : Désactivateur du viseur

---

## 🔌 Comparaison avec le Trend Line Mode

**Similitudes:**
- Les deux utilisent `Offset` pour la position
- Les deux se mettent à jour pendant le glissement
- Les deux s'affichent avec des lignes croisées

**Différences:**
- **Crosshair** : Activé par long press, suit avec `dragAmount`
- **Trend Line** : Activé par clic icône, suivi direct `change.position + offset`
- **Crosshair** : Une position (suivi relatif)
- **Trend Line** : Deux points + ligne entre eux

---

**Status** : 📚 Documentation complète  
**Date** : 2026-04-20

