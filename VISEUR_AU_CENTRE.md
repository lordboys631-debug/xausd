# ✅ MISE À JOUR - Viseur au centre

## 🎯 Modification appliquée

Le viseur apparaît maintenant **au centre de la grille** quand on clique sur l'icône de traçage de ligne de tendance.

### ❌ Avant
```kotlin
trendLineCrosshair = Offset(0f, 0f)  // ← Coin haut-gauche
```

### ✅ Après
```kotlin
trendLineCrosshair = Offset(
    chartWidthPx / 2f,  // Centre horizontal
    mainH / 2f          // Centre vertical
)
```

---

## 🎯 Flux d'utilisation

```
1. Clic icône "Ligne de tendance"
   ↓
   ✅ Viseur BLEU apparaît AU CENTRE du graphique
   ↓
2. Mouvement → Viseur suit le doigt
   ↓
3. Clic P1 → Cercle BLEU enregistre
   ↓
4. Clic P2 → Ligne ROSE tracée
```

---

## 🧪 À tester maintenant

```bash
./gradlew build
./gradlew installDebug

# Résultat attendu:
- Clic icône → Viseur BLEU apparaît AU CENTRE
- Le viseur doit être centré horizontalement et verticalement
```

---

**Status** : ✅ Viseur centré  
**Date** : 2026-04-20

