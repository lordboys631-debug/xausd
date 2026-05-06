# ✅ CORRECTION - Décalage du viseur vs Position du point

## 🎯 Distinction importante

Il faut comprendre la différence entre:

1. **Viseur** : Ce que vous VOYEZ (avec décalage +30px)
2. **Point** : Ce qui est ENREGISTRÉ (position réelle du doigt)

---

## ✅ Correction appliquée

### ❌ Avant (MAUVAIS)
```kotlin
if (trendLinePoint1 == null) {
    trendLinePoint1 = trendLineCrosshair  // ← Enregistrer le viseur (MAUVAIS!)
}
```

**Problème** : Le point était décalé de +30px, pas à la vraie position!

### ✅ Après (CORRECT)
```kotlin
if (trendLinePoint1 == null) {
    trendLinePoint1 = Offset(
        offset.x.coerceIn(0f, chartWidthPx),  // ← Position RÉELLE du doigt
        offset.y.coerceIn(0f, mainH)
    )
}
```

**Résultat** : Le point est enregistré à la vraie position!

---

## 🎯 Comprendre la différence

```
Votre doigt:           Viseur:               Point enregistré:
  (100, 200)      (130, 230)                    (100, 200)
  
Le viseur est          Mais le point est
décalé de +30px       à la vraie position!
```

---

## 🔄 Flux correct maintenant

```
1. Touche le graphique à (100, 200)
   ↓
   trendLineCrosshair = (130, 230)  ← Viseur avec décalage
   trendLinePoint1 = (100, 200)     ← Point à vraie position
   ✅ Viseur visible, point correct

2. Glisse à (150, 250)
   ↓
   trendLineCrosshair = (180, 280)  ← Viseur suit avec décalage
   (Point 1 reste à (100, 200))

3. Clic Point 2 à (150, 250)
   ↓
   trendLinePoint2 = (150, 250)     ← Point à vraie position
   ✅ Ligne tracée entre (100, 200) et (150, 250)
```

---

## ✨ Résumé

| Élément | Position | Raison |
|---------|----------|--------|
| Viseur | doigt + 30px | Pour être VISIBLE |
| Point | doigt (réel) | Pour être CORRECT |

---

## 🧪 À tester maintenant

```bash
./gradlew build
./gradlew installDebug

# RÉSULTAT ATTENDU:
1. Clic icône → Mode activé
2. Touche et glisse → Viseur suit avec décalage
3. Clic P1 → Point enregistré à vraie position
4. Clic P2 → Point enregistré à vraie position
5. Ligne tracée = PARFAITE!
```

---

**Status** : ✅ Viseur et points correctement positionnés  
**Date** : 2026-04-20

