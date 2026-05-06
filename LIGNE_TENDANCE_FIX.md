# ✅ FIX - La ligne de tendance disparaît après le deuxième point

## 🎯 Problème identifié

La ligne de tendance **disparaissait complètement** après avoir cliqué le deuxième point car:
1. Elle était dessinée **uniquement en mode édition** (`isTrendLineMode = true`)
2. Après le deuxième clic, le mode était réinitialisé à `false`
3. La ligne n'était **jamais sauvegardée** dans une liste persistante

---

## 🔧 Solution implémentée

### 1. Ajout d'une liste persistante pour les lignes complétées
```kotlin
var completedTrendLines by remember { mutableStateOf<List<Pair<Offset, Offset>>>(emptyList()) }
```

### 2. Sauvegarde de la ligne au 2e clic
Avant la réinitialisation des variables (ligne ~489):
```kotlin
// ✅ Sauvegarder la ligne avant de réinitialiser
if (trendLinePoint1 != null) {
    completedTrendLines = completedTrendLines + (trendLinePoint1!! to trendLinePoint2!!)
}
```

### 3. Dessiner les lignes sauvegardées
Après le dessin de la ligne active (ligne ~942):
```kotlin
// ✅ Dessiner les lignes complétées
completedTrendLines.forEach { (point1, point2) ->
    drawLine(
        color = Color(0xFFE91E63),  // ✅ ROSE/MAGENTA
        start = point1,
        end = point2,
        strokeWidth = 2f
    )
    // Dessiner les points d'extrémité
    drawCircle(
        color = Color(0xFF1E88E5),  // ✅ BLEU
        radius = 6f,
        center = point1
    )
    drawCircle(
        color = Color(0xFF1E88E5),  // ✅ BLEU
        radius = 6f,
        center = point2
    )
}
```

---

## 📊 Résultat attendu

✅ **La ligne de tendance PERSISTE** après le deuxième clic
✅ Les points restent **BLEUS** 
✅ La ligne est **ROSE/MAGENTA**
✅ Vous pouvez dessiner **PLUSIEURS lignes** successivement

---

## 🧪 À tester

```bash
./gradlew build
./gradlew installDebug

# RÉSULTAT ATTENDU:
1. Clic icône → Viseur au centre
2. Touche P1 → Écart calculé automatiquement
3. Glisse → Viseur garde cet écart
4. Clic P1 → Point 1 enregistré (BLEU)
5. Glisse → Aperçu de la ligne (ROSE)
6. Clic P2 → ✅ LIGNE PERSISTE! (ROSE + points BLEU)
7. Clic P3 → Nouvelle ligne possible
```

---

## 📝 Fichiers modifiés

- `app/src/main/java/com/bthr/backtest/ui/components/CandlestickChart.kt`
  - Ligne 99: Ajout variable `completedTrendLines`
  - Ligne 489-493: Sauvegarde au 2e clic
  - Ligne 942-967: Dessin des lignes persistantes

---

**Status** : ✅ Ligne de tendance persistante  
**Date** : 2026-04-20  
**Impact** : Majeur - Feature core de dessin maintenant fonctionnelle

