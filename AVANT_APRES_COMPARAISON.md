# 🔄 COMPARAISON AVANT/APRÈS - Ligne de tendance

## 🔴 AVANT (Bugué)

### Code original (CandlestickChart.kt, ligne 94-99)
```kotlin
// Trend line drawing state
var isTrendLineMode by remember { mutableStateOf(false) }
var trendLinePoint1 by remember { mutableStateOf<Offset?>(null) }
var trendLinePoint2 by remember { mutableStateOf<Offset?>(null) }
var trendLineCrosshair by remember { mutableStateOf<Offset?>(null) }
var trendLineCrosshairOffset by remember { mutableStateOf(Offset(0f, 0f)) }

// ❌ MANQUE: Variable pour stocker les lignes complétées!
```

### Dessin du 2e point (ligne 482-498)
```kotlin
} else if (trendLinePoint2 == null) {
    trendLinePoint2 = Offset(
        offset.x.coerceIn(0f, chartWidthPx),
        offset.y.coerceIn(0f, mainH)
    )
    // ❌ AUCUNE SAUVEGARDE!
    
    // Reset mode
    isTrendLineMode = false    // ❌ Mode désactivé = pas de dessin
    trendLinePoint1 = null     // ❌ Les données sont perdues!
    trendLinePoint2 = null
    trendLineCrosshair = null
}
```

### Dessin à l'écran (ligne 910-932)
```kotlin
if (isTrendLineMode && trendLineCrosshair != null) {  // ❌ Condition trop restrictive
    // Dessiner le viseur
    // Dessiner la ligne d'aperçu
}

// ❌ RIEN pour dessiner les lignes sauvegardées!
```

### Résultat
```
État: isTrendLineMode = false → aucun dessin
      Ligne perdue jamais sauvegardée
```

---

## 🟢 APRÈS (Corrigé)

### Code corrigé (CandlestickChart.kt, ligne 94-100)
```kotlin
// Trend line drawing state
var isTrendLineMode by remember { mutableStateOf(false) }
var trendLinePoint1 by remember { mutableStateOf<Offset?>(null) }
var trendLinePoint2 by remember { mutableStateOf<Offset?>(null) }
var trendLineCrosshair by remember { mutableStateOf<Offset?>(null) }
var trendLineCrosshairOffset by remember { mutableStateOf(Offset(0f, 0f)) }
var completedTrendLines by remember { mutableStateOf<List<Pair<Offset, Offset>>>(emptyList()) }

// ✅ Variable persistante pour les lignes complétées
```

### Dessin du 2e point (ligne 483-497)
```kotlin
} else if (trendLinePoint2 == null) {
    trendLinePoint2 = Offset(
        offset.x.coerceIn(0f, chartWidthPx),
        offset.y.coerceIn(0f, mainH)
    )
    // ✅ SAUVEGARDER LA LIGNE AVANT RESET
    if (trendLinePoint1 != null) {
        completedTrendLines = completedTrendLines + (trendLinePoint1!! to trendLinePoint2!!)
    }
    
    // Reset mode
    isTrendLineMode = false
    trendLinePoint1 = null
    trendLinePoint2 = null
    trendLineCrosshair = null
}
```

### Dessin à l'écran (ligne 910-953)
```kotlin
if (isTrendLineMode && trendLineCrosshair != null) {
    // Dessiner le viseur
    // Dessiner la ligne d'aperçu
}

// ✅ DESSINER TOUTES LES LIGNES SAUVEGARDÉES (nouveau!)
completedTrendLines.forEach { (point1, point2) ->
    drawLine(
        color = Color(0xFFE91E63),  // ROSE/MAGENTA
        start = point1,
        end = point2,
        strokeWidth = 2f
    )
    // Dessiner les points d'extrémité
    drawCircle(color = Color(0xFF1E88E5), radius = 6f, center = point1)
    drawCircle(color = Color(0xFF1E88E5), radius = 6f, center = point2)
}
```

### Résultat
```
État: isTrendLineMode = false → toujours pas de viseur
      ✅ Mais ligne visible car elle est dans completedTrendLines
      ✅ Peut dessiner plusieurs lignes
```

---

## 📊 Tableau comparatif

| Aspect | AVANT | APRÈS |
|--------|-------|-------|
| **État persistant** | ❌ Aucun | ✅ `completedTrendLines` |
| **Dessin après P2** | ❌ Ligne disparaît | ✅ Ligne persiste |
| **Multiplicité** | ❌ Une seule ligne | ✅ Plusieurs lignes |
| **Code maintenable** | ⚠️ État incohérent | ✅ Clair et logique |
| **Performance** | ✅ Légère | ✅ Légère (boucle simple) |

---

## 🎬 Scénario utilisateur

### AVANT
```
Utilisateur                          App
1. Clic icône ligne         →  Mode activé ✓
2. Clic P1                  →  Point 1 enregistré ✓
3. Glisse vers P2           →  Aperçu visible ✓
4. Clic P2                  →  Mode désactivé... ❌ LIGNE DISPARAÎT!
5. Clic icône à nouveau     →  Mode re-activé... vide
```

### APRÈS
```
Utilisateur                          App
1. Clic icône ligne         →  Mode activé ✓
2. Clic P1                  →  Point 1 enregistré ✓
3. Glisse vers P2           →  Aperçu visible ✓
4. Clic P2                  →  Mode désactivé... ✅ LIGNE PERSISTE!
5. Clic icône à nouveau     →  Mode re-activé... prêt pour ligne 2
6. Dessinez ligne 2         →  Deux lignes coexistent ✅
```

---

## 🔎 Points clés du fix

| Concept | Explication | Ligne |
|---------|-------------|-------|
| **Persistance** | Stocker les données indépendamment du mode | 100 |
| **Capture** | Enregistrer avant de réinitialiser | 489-491 |
| **Rendu** | Toujours afficher les données sauvegardées | 934-953 |

---

## 💡 Leçon apprise

**Principe**: Les données doivent être **découplées de l'état UI temporaire**

```
❌ Mauvais: État = Données temporaires
✅ Bon:    Données persistantes + État UI temporaire
```

---

**Correction appliquée le**: 2026-04-20  
**Fichier principal**: CandlestickChart.kt  
**Lignes modifiées**: 4 (100, 489-491, 934-953)  
**Complexité**: O(n) où n = nombre de lignes dessinées (négligeable)

