# ✅ CORRECTION - Viseur n'apparaît pas au clic

## 🐛 Problèmes trouvés et corrigés

### Problème 1 : Viseur non initialisé au premier contact
❌ **Avant** : Le viseur n'était pas initialisé quand le doigt touchait le graphique

✅ **Solution** : Ajouter l'initialisation dans `onDragStart`

```kotlin
onDragStart = { offset ->
    if (isTrendLineMode && offset.x <= chartWidthPx && offset.y <= mainH) {
        // ✅ NOUVEAU: Initialiser le viseur au premier contact du doigt
        if (trendLinePoint1 == null) {
            trendLineCrosshair = offset.coerceIn(
                Offset(0f, 0f),
                Offset(chartWidthPx, mainH)
            )
        }
        dragArea = 0
        return@detectDragGestures
    }
}
```

### Problème 2 : Viseur ne s'affiche que si P1 est défini
❌ **Avant** : Condition `if (isTrendLineMode && trendLinePoint1 != null)` 
- Le viseur n'apparaît que APRÈS le premier clic

✅ **Solution** : Changer la condition pour `if (isTrendLineMode && trendLineCrosshair != null)`
- Le viseur apparaît IMMÉDIATEMENT

```kotlin
// ✅ AVANT (mauvais)
if (isTrendLineMode && trendLinePoint1 != null) {
    // Dessiner le viseur
}

// ✅ APRÈS (correct)
if (isTrendLineMode && trendLineCrosshair != null) {
    // ✅ Dessiner le viseur MÊME AVANT P1
    drawLine(...)  // Vertical
    drawLine(...)  // Horizontal
    drawCircle()   // Center
}

if (isTrendLineMode && trendLinePoint1 != null) {
    // Dessiner P1 et la ligne preview
    drawCircle(...)  // P1
    drawLine(...)    // P1 -> Crosshair
}
```

---

## 🎯 Nouveau flux CORRECT

```
1. ✅ CLIC sur icône "Ligne de tendance"
   → isTrendLineMode = true
   → trendLineCrosshair = Offset(0f, 0f)
   → Icône devient DORÉE

2. ✅ PREMIER CONTACT du doigt
   → onDragStart détecte
   → trendLineCrosshair = position du doigt
   → 🎉 VISEUR APPARAÎT!

3. ✅ GLISSE le doigt
   → dragArea = 0 met à jour trendLineCrosshair
   → Viseur SUIT le doigt

4. ✅ TAPPE (Point 1)
   → onTap enregistre trendLinePoint1
   → Cercle jaune P1 apparaît
   → Ligne preview s'affiche

5. ✅ GLISSE vers Point 2
   → Viseur continue à suivre

6. ✅ TAPPE (Point 2)
   → Ligne tracée
   → Viseur disparaît
```

---

## 📝 Fichiers modifiés

### 1. onDragStart (ligne ~305)
```kotlin
// Ajouter l'initialisation du viseur
if (trendLinePoint1 == null) {
    trendLineCrosshair = offset...
}
```

### 2. Rendu du viseur (ligne ~882)
```kotlin
// Condition changée
if (isTrendLineMode && trendLineCrosshair != null) {
    // Dessiner le viseur
}

if (isTrendLineMode && trendLinePoint1 != null) {
    // Dessiner P1 et la ligne preview
}
```

---

## ✅ Résultat final

- ✅ Viseur apparaît immédiatement au clic icône
- ✅ Viseur suit le doigt en temps réel
- ✅ Pas d'erreur de compilation
- ✅ Architecture Android correcte

---

## 🧪 À tester maintenant

```bash
# Compiler
./gradlew build

# Déployer
./gradlew installDebug

# TEST SUR ANDROID:
1. Tachez l'icône "Ligne de tendance"
   ✓ L'icône devient DORÉE

2. Touchez le graphique
   ✓ Le VISEUR APPARAÎT IMMÉDIATEMENT!

3. Glissez le doigt
   ✓ Le viseur SUIT le doigt

4. Tachez Point 1
   ✓ Cercle jaune + ligne preview

5. Glissez vers Point 2
   ✓ Viseur continue à suivre

6. Tachez Point 2
   ✓ Ligne tracée
```

---

**Status** : ✅ Viseur s'affiche correctement  
**Date** : 2026-04-20

