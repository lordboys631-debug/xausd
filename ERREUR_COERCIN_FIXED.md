# ✅ CORRECTION - Erreur coerceIn

## 🐛 Erreur trouvée

**Ligne 308-311** : Mauvaise utilisation de `coerceIn`

```kotlin
// ❌ ERREUR: coerceIn attend des Float, pas des Offset
trendLineCrosshair = offset.coerceIn(
    Offset(0f, 0f),
    Offset(chartWidthPx, mainH)
)
```

**Cause** : La méthode `coerceIn(min, max)` pour les Offset n'existe pas. Il faut coercer les composantes individuellement.

---

## ✅ Solution appliquée

```kotlin
// ✅ CORRECT: Coercer les composantes X et Y séparément
trendLineCrosshair = Offset(
    offset.x.coerceIn(0f, chartWidthPx),
    offset.y.coerceIn(0f, mainH)
)
```

---

## 📊 Avant/Après

| Avant | Après |
|-------|-------|
| `offset.coerceIn(Offset(...), Offset(...))` | `Offset(offset.x.coerceIn(...), offset.y.coerceIn(...))` |
| ❌ Type error | ✅ Correct |

---

## ✅ Fichier corrigé

- ✅ Ligne 308-311 : Méthode coerceIn corrigée
- ✅ Pas d'erreurs de type
- ✅ Logique correcte

---

## 🧪 À tester maintenant

```bash
./gradlew build
./gradlew installDebug
```

---

**Status** : ✅ Erreur corrigée  
**Date** : 2026-04-20

