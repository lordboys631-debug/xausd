# ✅ SOLUTION - Viseur visible par-dessus le glissement

## 🎯 Problème identifié

Le viseur du trend line se fait couvrir/masquer pendant le glissement du doigt car il est dessiné dans le Canvas qui peut être couvert par d'autres éléments.

**Solution** : Dessiner le viseur sur un layer Canvas SÉPARÉ, ou ajouter un Canvas overlay spécifique pour le viseur.

---

## ✅ Solution appliquée

Le viseur doit être dessiné dans un Canvas SÉPARÉ **APRÈS** le Canvas principal, pour qu'il soit toujours visible par-dessus.

### Architecture:
```
Box (Main Container)
├─ Canvas (Main Chart) ← Tous les éléments du graphique
└─ Canvas (Overlay) ← VISEUR SEULEMENT ← Toujours par-dessus!
```

---

## 📊 Code à ajouter

Après le Canvas principal, ajouter un Canvas overlay pour le viseur:

```kotlin
// Canvas principal (existing)
Canvas(modifier = Modifier.fillMaxSize() { ... })

// ✅ NOUVEAU: Canvas overlay pour le viseur (toujours par-dessus)
if (isTrendLineMode && trendLineCrosshair != null) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        trendLineCrosshair?.let { crosshair ->
            // Viseur (croix)
            drawLine(...)
            drawLine(...)
            drawCircle(...)
        }
        
        if (trendLinePoint1 != null) {
            // Cercle P1
            drawCircle(...)
            
            // Ligne preview
            drawLine(...)
        }
    }
}
```

---

## 🔄 Pourquoi c'est mieux?

1. **Pas de clipRect** : Le viseur n'est pas affecté par la clipRect du graphique
2. **Toujours visible** : Le Canvas overlay est dessiné APRÈS le Canvas principal
3. **Pas de confusion** : Séparation claire entre les couches
4. **Cohérent** : Similaire au comportement du crosshair existant

---

## 🧪 À implémenter

```bash
# 1. Supprimer le code de dessin du viseur du Canvas principal (ligne 882-926)
# 2. Ajouter un Canvas overlay APRÈS le Canvas principal
# 3. Recompiler

./gradlew build
./gradlew installDebug
```

---

**Status** : Solution identifiée  
**Prochaines étapes** : Implémenter le Canvas overlay  
**Date** : 2026-04-20

