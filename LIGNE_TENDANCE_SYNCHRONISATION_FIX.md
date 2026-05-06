# 🔧 FIX: Synchronisation des lignes de tendance avec le chart

## 📋 Problème identifié

**Avant** : Les lignes de tendance ne suivaient pas le défilement du chart
- Les lignes restaient figées à leurs coordonnées absolues
- Lors du pan/scroll du chart, les lignes ne se déplaçaient pas
- Les lignes disparaissaient ou se positionnaient incorrectement après un scroll

**Raison** : Les coordonnées de pixel étaient sauvegardées directement sans tenir compte du scroll offset

---

## ✅ Solution implémentée

### 1. Nouvelle structure de données (Ligne 56-62)

```kotlin
data class TrendLineData(
    val candle1Idx: Int,      // Index de la première bougie
    val price1: Float,        // Prix à ce point
    val candle2Idx: Int,      // Index de la deuxième bougie
    val price2: Float         // Prix à ce point
)
```

**Avantage** : Les données sont maintenant **relatives aux bougies**, pas aux pixels absolus

### 2. Conversion lors de la sauvegarde (Lignes 497-521)

Quand l'utilisateur clique sur le deuxième point:

```kotlin
// Convertir les coordonnées pixels en indices de bougie + prix
val idx1 = round(allCandles.size - 1 - scrollOffset - (chartWidthPx - trendLinePoint1!!.x - candleW / 2f) / candleW).toInt()
val price1 = denormY(trendLinePoint1!!.y)

val idx2 = round(allCandles.size - 1 - scrollOffset - (chartWidthPx - trendLinePoint2!!.x - candleW / 2f) / candleW).toInt()
val price2 = denormY(trendLinePoint2!!.y)

completedTrendLines = completedTrendLines + TrendLineData(idx1, price1, idx2, price2)
```

### 3. Recalcul lors du rendu (Lignes 964-1000)

À chaque redessinage du canvas:

```kotlin
completedTrendLines.forEach { trendLine ->
    // Convertir les indices en positions X actuelles
    val x1 = chartWidthPx - (allCandles.size - 1 - trendLine.candle1Idx - scrollOffset) * candleW - candleW / 2f
    val x2 = chartWidthPx - (allCandles.size - 1 - trendLine.candle2Idx - scrollOffset) * candleW - candleW / 2f
    
    // Convertir les prix en positions Y
    val y1 = normY(trendLine.price1)
    val y2 = normY(trendLine.price2)
    
    // Dessiner avec les nouvelles coordonnées
    drawLine(..., start = Offset(x1, y1), end = Offset(x2, y2), ...)
}
```

---

## 🎯 Résultats

| Comportement | Avant | Après |
|-------------|-------|-------|
| Pan du chart | ❌ Lignes figées | ✅ Lignes suivent |
| Zoom | ❌ Positionnement incorrect | ✅ Lignes repositionnées correctement |
| Scroll horizontal | ❌ Lignes disparaissent | ✅ Lignes persistent |
| Persistance visuelle | ❌ Perte après 2 points | ✅ Toutes les lignes conservées |

---

## 🔄 Flux de données

```
┌─────────────────────────────────────────────┐
│    Utilisateur clique sur point 1 & 2       │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
    ┌──────────────────────────────────┐
    │ Convertir pixels → indices/prix   │
    │ (avec scrollOffset actuel)        │
    └──────────────────┬───────────────┘
                       │
                       ▼
        ┌─────────────────────────────┐
        │  Stocker TrendLineData       │
        │  (relative au chart, pas px) │
        └──────────────────┬──────────┘
                           │
                           ▼
    ┌──────────────────────────────────────┐
    │  À chaque redraw du canvas:          │
    │  Reconvertir indices/prix → pixels   │
    │  (avec scrollOffset actuel)          │
    └──────────────────┬───────────────────┘
                       │
                       ▼
    ┌──────────────────────────────────────┐
    │  Dessiner la ligne à sa position     │
    │  correcte même après un pan/scroll   │
    └──────────────────────────────────────┘
```

---

## 🧪 Cas de test

### Test 1: Pan horizontal
- ✅ Tracer une ligne
- ✅ Panner le chart vers la gauche
- ✅ Ligne doit se déplacer avec le chart

### Test 2: Zoom
- ✅ Tracer une ligne
- ✅ Zoomer in/out
- ✅ Ligne doit rester positionnée correctement

### Test 3: Multiples lignes
- ✅ Tracer 3-4 lignes à différents endroits
- ✅ Panner et zoomer
- ✅ Toutes les lignes doivent rester visibles et correctes

### Test 4: Limite d'écran
- ✅ Tracer une ligne
- ✅ Scroller jusqu'à ce que la ligne sorte de l'écran
- ✅ Scroller en arrière
- ✅ Ligne réapparaît correctement

---

## 📝 Notes técniques

### Calcul de la position X
```
x = chartWidthPx - (allCandles.size - 1 - candleIdx - scrollOffset) * candleW - candleW / 2f
```
- `chartWidthPx` : largeur disponible pour le chart
- `allCandles.size - 1 - candleIdx` : position relative depuis la fin
- `scrollOffset` : décalage actuel du scroll
- `candleW` : largeur d'une bougie
- `-candleW / 2f` : centrer sur la bougie

### Calcul de la position Y
```
y = normY(price) = topMarginPx + effectiveMainH - ((price - minP) / range * effectiveMainH)
```
- Utilise la même fonction de normalisation que le reste du chart
- Reste cohérente même avec les marges top/bottom

### Avantages de cette approche
1. **Invariant au scroll** : Les indices de bougies ne changent jamais
2. **Invariant au zoom** : Les prix restent les mêmes
3. **Déterministe** : Même résultat à chaque render
4. **Extensible** : Facile d'ajouter d'autres formes (rectangles, etc.)

---

## 🚀 Impact sur les autres fonctionnalités

| Fonction | Impact | Status |
|----------|--------|--------|
| Crosshair | Aucun impact | ✅ Fonctionnel |
| Indicateurs | Aucun impact | ✅ Fonctionnel |
| Sélection de bougie | Aucun impact | ✅ Fonctionnel |
| Pan/Zoom | **Amélioré** | ✅ Mieux |

---

## 📚 Fichiers modifiés

- `CandlestickChart.kt` (1375 lignes)
  - Ligne 56-62 : Ajout classe `TrendLineData`
  - Ligne 107 : Modification du state
  - Lignes 497-521 : Conversion pixels → indices/prix
  - Lignes 964-1000 : Recalcul indices/prix → pixels

---

**Date du fix** : 2026-04-20  
**Sévérité du bug** : 🔴 HAUTE  
**Urgence** : 🔴 CRITIQUE  
**Status** : ✅ RÉSOLU

