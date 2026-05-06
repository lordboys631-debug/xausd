# 📏 VÉRIFICATION - Distance entre le Viseur et le Doigt

## 📊 Décalage actuel

```
Doigt                  Viseur (offset)
(X, Y)        ────→    (X + 30px, Y + 30px)
```

### Détails:
- **Décalage Horizontal (X)** : +30 pixels vers la droite
- **Décalage Vertical (Y)** : +30 pixels vers le bas
- **Distance totale** : √(30² + 30²) ≈ 42.4 pixels

---

## 📍 Position visuelle

```
Votre doigt (point noir)              Viseur (croix bleue)
    👆                                     ⊕
  (100, 200)                          (130, 230)
    
    Distance:
    - X = +30px
    - Y = +30px
```

---

## 📝 Emplacements dans le code

### **onDragStart** (ligne 309-310)
```kotlin
trendLineCrosshair = Offset(
    (offset.x + 30f).coerceIn(0f, chartWidthPx),
    (offset.y + 30f).coerceIn(0f, mainH)
)
```

### **onDrag** (ligne 372-373)
```kotlin
trendLineCrosshair = Offset(
    (change.position.x + 30f).coerceIn(0f, chartWidthPx),
    (change.position.y + 30f).coerceIn(0f, mainH)
)
```

---

## 🎯 Options de décalage

Vous pouvez ajuster les valeurs comme suit:

| Décalage | Description |
|----------|-------------|
| `+15f, +15f` | Petit décalage (21px) |
| `+25f, +25f` | Décalage moyen (35px) |
| `+30f, +30f` | **Décalage actuel** (42px) |
| `+40f, +40f` | Grand décalage (57px) |
| `+50f, +50f` | Très grand décalage (71px) |

---

## 🔧 Comment modifier

Si vous trouvez que 30px n'est pas assez ou trop, modifiez les deux lignes:

### Pour +20px:
```kotlin
(offset.x + 20f).coerceIn(0f, chartWidthPx),
(offset.y + 20f).coerceIn(0f, mainH)
```

### Pour +50px:
```kotlin
(offset.x + 50f).coerceIn(0f, chartWidthPx),
(offset.y + 50f).coerceIn(0f, mainH)
```

---

## 🎯 Récapitulatif

```
Position du doigt : (X, Y)
Position du viseur : (X + 30px, Y + 30px)

Le viseur est toujours 30px à droite ET 30px vers le bas du doigt
```

---

**Status** : ✅ Distance vérifiée  
**Décalage actuel** : +30px horizontal, +30px vertical  
**Distance totale** : 42.4 pixels  
**Date** : 2026-04-20

