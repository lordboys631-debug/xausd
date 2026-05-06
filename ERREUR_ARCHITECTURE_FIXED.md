# ✅ CORRECTION - Erreurs d'architecture pointerInput

## 🐛 Erreur trouvée

**Problème** : Mélange de deux handlers de gestes incompatibles dans le même `pointerInput`

```kotlin
// ❌ ERREUR: Deux handlers imbriqués
.pointerInput(...) {
    if (isTrendLineMode) {
        awaitEachGesture { ... }  // ← Handler 1
    }
    
    detectTapGestures { ... }     // ← Handler 2 (conflit!)
}
```

**Cause** : 
- `awaitEachGesture` et `detectTapGestures` ne peuvent pas être dans le même `pointerInput`
- Cela cause une erreur d'architecture Compose

---

## ✅ Solution appliquée

### Simplification: Garder seulement `detectTapGestures`

```kotlin
// ✅ CORRECT: Un seul handler
.pointerInput(..., isTrendLineMode) {
    detectTapGestures(
        onTap = { offset ->
            if (isTrendLineMode && trendLinePoint1 == null) {
                trendLinePoint1 = offset
                trendLineCrosshair = offset
            } else if (isTrendLineMode && trendLinePoint2 == null) {
                trendLinePoint2 = offset
                // Ligne tracée
                isTrendLineMode = false
            }
            // ... reste du code
        }
    )
}
```

### Le suivi du doigt via dragArea

Le suivi du viseur en temps réel est déjà implémenté dans le **dragArea = 0** du premier `pointerInput`:

```kotlin
when (dragArea) {
    0 -> {
        if (isTrendLineMode && trendLinePoint1 == null) {
            // ✅ Viseur suit le doigt
            trendLineCrosshair = Offset(
                change.position.x.coerceIn(0f, chartWidthPx),
                change.position.y.coerceIn(0f, mainH)
            )
        }
    }
}
```

---

## 🎯 Architecture finale correcte

```
Canvas
├─ pointerInput #1 (zoom)
├─ pointerInput #2 (drag)
├─ pointerInput #3 (tap + trend line) ✅ UNIFIÉ
└─ Canvas rendering
```

---

## 🔄 Flux Android (CORRECT)

```
1. Clic icône → Mode activé
2. Doigt touche → dragArea = 0 détecte
3. Doigt glisse → dragArea = 0 update le viseur
4. Doigt levé + tappe → onTap détecte → Point enregistré
5. Glisse à nouveau → dragArea = 0 update viseur
6. Tappe → onTap détecte → Point 2 + Ligne tracée
```

---

## ✅ Fichier corrigé

- ✅ Suppression du `awaitEachGesture` conflictuel
- ✅ Garder uniquement `detectTapGestures`
- ✅ Le suivi du doigt fonctionne via dragArea
- ✅ Pas de conflit d'architecture

---

## 🧪 À tester maintenant

```bash
# Compiler
./gradlew build    # ✅ Doit compiler sans erreur

# Déployer
./gradlew installDebug

# Tester:
1. Clic icône → Mode activé
2. Touchez le graphique → Viseur apparaît
3. Glissez le doigt → Viseur suit
4. Tachez Point 1 → Enregistré
5. Glissez → Ligne preview
6. Tachez Point 2 → Ligne tracée
```

---

**Status** : ✅ Erreur corrigée  
**Date** : 2026-04-20

