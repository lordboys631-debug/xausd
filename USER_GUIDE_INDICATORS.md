# 🚀 FONCTIONNALITÉS RESTAURÉES - GUIDE UTILISATEUR

## ✅ Tous les problèmes résolus !

### 1️⃣ REDIMENSIONNEMENT DES FENÊTRES D'INDICATEURS

#### Comment ça marche:

**A) Redimensionner la hauteur** 
- Drag sur le **séparateur horizontal** entre deux indicateurs
- La fenêtre s'agrandit ou rétrécit selon la direction

**B) Zoom vertical** (barre de prix à droite)
- Drag sur la **barre de prix (à droite)** pour zoomer verticalement
- Haut = zoom in, Bas = zoom out

**C) Panoramique horizontal**
- Drag sur le **graphique de l'indicateur** pour se déplacer horizontalement

---

### 2️⃣ PARAMÈTRES DU VISEUR (CROSSHAIR)

#### Format français appliqué:
```
Vendredi 21 avr 26  14:30:45
```

Caractéristiques:
- ✅ Jour de la semaine en français (lun, mar, mer, jeu, ven, sam, dim)
- ✅ Mois en français (jan, fév, mar, avr, mai, jun, jul, aoû, sep, oct, nov, déc)
- ✅ Année avec apostrophe: '26 (année courte)
- ✅ Heure et minute affichées
- ✅ Timezone support complet

---

### 3️⃣ INDICATEURS DISPONIBLES

#### RSI (Relative Strength Index)
```
Paramètres:
- Période: configurable
- Source: Open, High, Low, Close
- Zones: Suracheté (>70, bleu), Survente (<30, rouge)
- MA Support: SMA ou EMA personnalisable
```

Interaction:
- Double-tap sur la barre de prix pour reset au zoom automatique
- Drag barre prix pour zoomer verticalement
- Drag graphique pour panorama horizontal

#### MACD (Moving Average Convergence Divergence)
```
Paramètres:
- Fast Period, Slow Period, Signal Period
- Ligne MACD, Signal Line, Histogram
- Couleurs personnalisables par composant
```

#### Stochastic
```
Paramètres:
- K Period, K Smoothing, D Period
- Niveaux personnalisables (défaut: 20/80)
- Zone d'arrière-plan configurable
```

#### ATR (Average True Range)
```
Paramètres:
- Période configurable (Wilder)
- Affichage direct de la volatilité
```

---

### 4️⃣ AFFICHAGE DES ÉTIQUETTES

#### Pour les indicateurs overlay (sur le graphique):
- Cliquez sur le **chevron bas** pour masquer/afficher tous les labels
- Cliquez sur **l'indicateur** pour plus d'options

#### Pour les indicateurs bottom (RSI, MACD, etc):
- **Toggle de visibilité**: Cliquez sur l'œil
- **Settings**: Cliquez sur la roue dentée
- **Supprimer**: Cliquez sur la corbeille
- **Étiquettes**: Affichage dynamique des valeurs

---

### 5️⃣ GESTION DE LA HAUTEUR DES FENÊTRES

#### Auto-Height (Défaut):
- La hauteur s'ajuste automatiquement au contenu
- Double-tap sur la barre de prix pour réactiver

#### Manuel Height:
- Drag le séparateur pour définir une hauteur fixe
- L'auto-height se désactive automatiquement

#### Minimisé:
- Cliquez l'œil pour masquer un indicateur
- La fenêtre se réduit à 24dp (affichage de l'étiquette uniquement)

---

## 📊 DÉTAILS TECHNIQUES

### État gérés par indicateur:
```kotlin
indicatorHeights[indicatorId]      // Hauteur en pixels
indicatorAutoHeight[indicatorId]   // Mode auto-height activé?
indicatorRanges[indicatorId]       // Min/Max pour le zoom Y
```

### Gestes détectés:
```
dragArea = 1: Zoom axe Y (barre prix)
dragArea = 3: Pan horizontal (gauche)
dragArea = 4: Redimensionner séparateur
dragArea = 5: Zoom indicateur (barre prix)
dragArea = 6: Pan indicateur horizontal
```

---

## 🎯 CAS D'USAGE COURANTS

### Afficher 4 indicateurs et les redimensionner

1. Ajouter RSI, MACD, Stochastic, ATR
2. Chaque fenêtre obtient une hauteur égale par défaut
3. Drag les séparateurs pour ajuster les hauteurs
4. Les valeurs se mettent à jour en temps réel

### Zoomer sur une zone spécifique d'un indicateur

1. Drag vers le **haut** sur la barre de prix → zoom in
2. Drag vers le **bas** sur la barre de prix → zoom out
3. Double-tap pour retourner à l'auto-height

### Masquer temporairement un indicateur

1. Cliquez l'**œil** pour basculer la visibilité
2. L'indicateur se réduit à une ligne mince (étiquette)
3. Cliquez à nouveau pour réafficher

### Supprimer un indicateur

1. Cliquez la **corbeille** dans la toolbar
2. L'indicateur est supprimé
3. Les autres indicateurs s'ajustent automatiquement

---

## 🔧 TROUBLESHOOTING

### Les étiquettes ne s'affichent pas?
- Vérifiez que `showIndicatorLabels` est **true**
- Vérifiez que l'indicateur est **visible** (pas minimisé)

### Le redimensionnement ne fonctionne pas?
- Assurez-vous de draguer exactement sur le **séparateur gris**
- Vérifiez que l'indicateur est **visible** (pas masqué avec l'œil)

### Le zoom n'a pas d'effet?
- Vérifiez que vous draggez sur la **barre de prix (à droite)**
- Pas sur le graphique lui-même

### Les hauteurs se réinitialisent?
- C'est normal si `indicatorAutoHeight` est **true**
- Double-tap pour réactiver l'auto-height si vous aviez changé manuellement

---

## 📋 FICHIERS CLÉS

| Fichier | Rôle |
|---------|------|
| `CandlestickChart.kt` | Composable principal, gestion des gestes |
| `ChartDrawer.kt` | Rendu des graphiques, indicateurs |
| `Indicator.kt` | Modèles des indicateurs |
| `IndicatorCalculators.kt` | Calculs des valeurs d'indicateurs |

---

## ✨ RÉSUMÉ

✅ **Redimensionnement**: Drag séparateurs et barres de prix  
✅ **Viseur français**: Date/heure en français avec timezone  
✅ **4 Indicateurs**: RSI, MACD, Stochastic, ATR  
✅ **Étiquettes**: Toggle, paramètres, suppression  
✅ **Zoom/Pan**: Fonctionnel pour tous les indicateurs  

**Tout est prêt à l'emploi!** 🚀

