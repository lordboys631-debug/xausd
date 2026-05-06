# ✅ ANNULATION - Retour au code précédent

## 🔄 Modifications annulées

Toutes les modifications pour réutiliser `crosshairPosition` ont été annulées.

**On revient à:**
- ✅ Viseur personnalisé **JAUNE** (pas le viseur existant)
- ✅ Utilisation de `trendLineCrosshair` (pas `crosshairPosition`)
- ✅ Dessin du viseur personnalisé dans le Canvas

---

## ✅ Code restauré

### 1. dragArea = 0 (remis à l'original)
```kotlin
if (isTrendLineMode && trendLinePoint1 == null) {
    trendLineCrosshair = Offset(...)  // ← Utilise trendLineCrosshair
}
```

### 2. Dessin du viseur (restauré)
```kotlin
if (isTrendLineMode && trendLineCrosshair != null) {
    drawLine(...) // Croix verticale + horizontale
    drawCircle(...) // Centre du viseur
}
```

### 3. onDragStart (remis à l'original)
```kotlin
if (trendLinePoint1 == null) {
    trendLineCrosshair = Offset(...)  // ← Pas crosshairPosition
}
```

### 4. onTap (remis à l'original)
```kotlin
trendLinePoint2 = offset
// ← Pas de: crosshairPosition = null
```

### 5. onLongPress (remis à l'original)
```kotlin
// ← Pas de: crosshairPosition = null
```

### 6. Barre de favoris (remise à l'original)
```kotlin
// ← Pas de: crosshairPosition = null
```

---

## 🎯 Retour au flux original

```
1. Clic icône → Mode activé
2. Mouvement du doigt
   ↓
   trendLineCrosshair se met à jour
   ↓
   ✅ Viseur JAUNE personnalisé s'affiche
3. Clic P1 → Enregistré
4. Clic P2 → Ligne tracée
```

---

## 🧪 À tester maintenant

```bash
./gradlew build
./gradlew installDebug

# Le viseur JAUNE doit apparaître et suivre le doigt
```

---

**Status** : ✅ Annulation complète  
**Date** : 2026-04-20

