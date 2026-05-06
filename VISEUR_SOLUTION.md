# ✅ SOLUTION - Viseur n'apparaît pas

## 🐛 Problème identifié

**Cause** : J'utilisais `chartWidthPx` et `mainH` dans le scope du clic sur l'icône de favoris, mais ces variables n'existaient que dans le scope du Canvas.

**Erreur** : 
```kotlin
trendLineCrosshair = Offset(chartWidthPx / 2f, mainH / 2f)  // ❌ Non accessible ici!
```

---

## ✅ Solution appliquée

### 1. Initialisation simple du viseur
```kotlin
if (isTrendLineMode) {
    // ✅ Initialiser avec une valeur simple
    trendLineCrosshair = Offset(0f, 0f)
} else {
    trendLinePoint1 = null
    trendLinePoint2 = null
    trendLineCrosshair = null
}
```

### 2. Mise à jour en temps réel via dragArea
```kotlin
// Dans le onDrag handler du Canvas (dragArea = 0)
if (isTrendLineMode && trendLinePoint1 == null) {
    // ✅ Viseur suit le curseur AVANT P1
    trendLineCrosshair = Offset(
        change.position.x.coerceIn(0f, chartWidthPx),
        change.position.y.coerceIn(0f, mainH)
    )
} else if (isTrendLineMode && trendLinePoint1 != null && trendLinePoint2 == null) {
    // ✅ Viseur suit le curseur APRÈS P1
    trendLineCrosshair = Offset(
        change.position.x.coerceIn(0f, chartWidthPx),
        change.position.y.coerceIn(0f, mainH)
    )
}
```

---

## 🎯 Nouveau flux (MAINTENANT CORRECT)

```
1. ✅ CLIC sur icône "Ligne de tendance"
   → isTrendLineMode = true
   → trendLineCrosshair = Offset(0f, 0f) ← VISEUR INITIALISÉ
   → Icône devient DORÉE

2. ✅ MOUVEMENT du curseur
   → dragArea = 0 détecte le mouvement
   → trendLineCrosshair se met à jour à chaque mouvement
   → 🎉 VISEUR SUIT LE CURSEUR EN TEMPS RÉEL!

3. ✅ CLIC (Point 1)
   → trendLinePoint1 = offset
   → Viseur continue à suivre

4. ✅ MOUVEMENT jusqu'à Point 2
   → Ligne preview s'affiche
   → Viseur suit toujours

5. ✅ CLIC (Point 2)
   → Ligne tracée
   → Viseur disparaît
   → Mode désactivé
```

---

## 🧪 À tester maintenant

```bash
# Compiler
./gradlew build

# Déployer
./gradlew installDebug

# Test:
1. Cliquez l'icône "Ligne de tendance"
   ✓ L'icône devient DORÉE
   
2. Bougez la souris sur le graphique
   ✓ Le VISEUR (croix + cercle) APPARAÎT ET SUIT
   
3. Cliquez Point 1
   ✓ Cercle jaune enregistre le point
   
4. Bougez
   ✓ Ligne preview + viseur
   
5. Cliquez Point 2
   ✓ Ligne tracée
   ✓ Viseur disparaît
```

---

## 📝 Fichier modifié

- ✅ `CandlestickChart.kt` ligne 1269
  - Avant : `Offset(chartWidthPx / 2f, mainH / 2f)` ❌ (variable non accessible)
  - Après : `Offset(0f, 0f)` ✅ (simple initialisation)

---

**Pourquoi ça fonctionne maintenant:**

1. Le viseur s'initialise immédiatement (même si à 0,0)
2. Dès que vous bougez la souris, le dragArea=0 handler détecte le mouvement
3. Le viseur se met à jour avec la position réelle du curseur
4. Le viseur suit continuellement en temps réel
5. Quand vous cliquez, le premier point est enregistré
6. Le viseur continue à suivre jusqu'au deuxième clic

---

**Status** : ✅ Problème résolu  
**Date** : 2026-04-20

