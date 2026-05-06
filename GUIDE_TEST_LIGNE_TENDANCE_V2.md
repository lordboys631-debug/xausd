# 🧪 Guide de Test - Synchronisation des Lignes de Tendance

## 📌 Objectif

Valider que les lignes de tendance se déplacent correctement avec le chart lors du pan, zoom et scroll.

---

## 🛠️ Setup

1. Démarrer l'application
2. Ouvrir le chart d'une crypto (Ex: BTCUSDT)
3. Afficher au moins 60-100 bougies
4. Mettre en favoris l'outil "Trend Line" pour accès rapide

---

## 🧪 Scénario 1 : Pan horizontal

**Étapes** :
1. Cliquer sur l'outil Trend Line (le bouton est en OR)
2. Cliquer sur le chart à un point de départ (Pt1)
3. Déplacer le curseur et cliquer pour placer Pt2
   - Une ligne rose/magenta doit s'afficher
   - Deux points bleus aux extrémités
4. Faire un pan horizontal du chart (drag horizontal)
   - **ATTENDU** : La ligne doit se déplacer avec le chart
   - **VÉRIFIER** : Les points restent attachés aux mêmes bougies

**Validation** : ✅ si la ligne suit le chart, ❌ si elle reste figée

---

## 🧪 Scénario 2 : Zoom avant (In)

**Étapes** :
1. Tracer une ligne (voir Scénario 1)
2. Zoomer avant avec deux doigts (pinch in)
   - **ATTENDU** : La ligne s'agrandit correctement
   - **VÉRIFIER** : Les points bleus sont plus grands
   - **VÉRIFIER** : L'épaisseur de la ligne semble la même (pas de distorsion)
3. Continuer le zoom avant
   - **ATTENDU** : Les bougies sous la ligne deviennent visibles

**Validation** : ✅ si tout s'agrandit proportionnellement, ❌ si la ligne se casse

---

## 🧪 Scénario 3 : Zoom arrière (Out)

**Étapes** :
1. Tracer une ligne sur une portée visible
2. Zoomer arrière (pinch out pour voir plus de bougies)
   - **ATTENDU** : La ligne devient plus petite
   - **VÉRIFIER** : La position reste correcte
   - **VÉRIFIER** : Les extrémités restent sur les mêmes bougies

**Validation** : ✅ si la ligne reste correcte, ❌ si elle disparaît ou se décale

---

## 🧪 Scénario 4 : Ligne qui sort de l'écran

**Étapes** :
1. Tracer une ligne vers la droite du chart (près du bord)
2. Scroller vers la gauche (vieilles bougies)
   - **ATTENDU** : La ligne sort progressivement de l'écran
   - **VÉRIFIER** : Les points disparaissent graduellement
3. Scroller en arrière vers les nouvelles bougies
   - **ATTENDU** : La ligne réapparaît correctement

**Validation** : ✅ si la ligne réapparaît, ❌ si elle ne revient pas ou mal positionnée

---

## 🧪 Scénario 5 : Multiples lignes

**Étapes** :
1. Tracer 3 lignes à différents endroits du chart
   - Ligne 1 : De bas à haut (support à résistance)
   - Ligne 2 : Presque horizontale (level)
   - Ligne 3 : Diagonale inverse (résistance à support)
2. Panner le chart
   - **ATTENDU** : Les 3 lignes se déplacent avec le chart
   - **VÉRIFIER** : Aucune ligne n'est plus "fixe" que l'autre
3. Zoomer
   - **ATTENDU** : Les 3 lignes se redimensionnent proportionnellement

**Validation** : ✅ si toutes les lignes se comportent identiquement, ❌ si une ligne agit différemment

---

## 🧪 Scénario 6 : Ligne diagonale montante

**Étapes** :
1. Tracer une ligne de bas à haut (trend haussier)
2. Panner lentement vers la droite (futures)
   - **ATTENDU** : La ligne descend progressivement (puisqu'on voit les bougies futures plus basses)
   - **VÉRIFIER** : L'angle de la ligne reste constant

3. Revenir arrière (passé)
   - **ATTENDU** : La ligne remonte

**Validation** : ✅ si le positionnement est correct, ❌ si l'angle change

---

## 🧪 Scénario 7 : Ligne horizontale

**Étapes** :
1. Tracer une ligne horizontale (niveaux d'appui/résistance)
   - Pt1 : x=100px, y=500px
   - Pt2 : x=400px, y=500px (même Y)
2. Panner verticalement (up/down)
   - **ATTENDU** : La ligne reste horizontale
   - **VÉRIFIER** : Les deux points restent au même prix
3. Panner horizontalement
   - **ATTENDU** : La ligne reste horizontale

**Validation** : ✅ si la ligne reste horizontale, ❌ si elle se penche

---

## 🧪 Scénario 8 : Changement de timeframe

**Étapes** :
1. Tracer une ligne en timeframe M5
2. Changer le timeframe (M15, H1, D1)
   - **COMPORTEMENT ATTENDU** : Les lignes peuvent disparaître (données différentes)
   - **VÉRIFIER** : Aucune erreur de crash
3. Revenir au M5
   - **ATTENDU** : Les lignes réapparaissent

**Validation** : ✅ pas de crash, ❌ si crash ou comportement imprévisible

---

## ✅ Checklist de validation

| Critère | Status | Notes |
|---------|--------|-------|
| Ligne suit le pan horizontal | ✅/❌ | |
| Ligne suit le zoom avant | ✅/❌ | |
| Ligne suit le zoom arrière | ✅/❌ | |
| Ligne réapparaît après scroll | ✅/❌ | |
| Multiples lignes synchronisées | ✅/❌ | |
| Angle préservé en pan | ✅/❌ | |
| Horizontale reste horizontale | ✅/❌ | |
| Pas de crash | ✅/❌ | |

---

## 🐛 Si vous trouvez un bug

### 1. Position incorrecte après un geste
**Diagnostic** :
```
Position attendue: X=250px, Y=400px
Position actuelle: X=300px, Y=350px
Décalage: ΔX=50px, ΔY=50px
```

**Cause probable** : Erreur dans le calcul de conversion pixels ↔ indices

### 2. Ligne disparaît lors du scroll
**Diagnostic** :
- Elle disparaît quand elle est hors écran ?
- Ou elle disparaît toujours ?

**Cause probable** :
- Hors écran = bug de visibility check (ligne 979)
- Toujours = bug de calcul de position

### 3. Multiples lignes avec positions incohérentes
**Diagnostic** :
- Comparer les positions avant/après pan
- Vérifier si les décalages sont proportionnels

**Cause probable** : State not updated correctly

---

## 📊 Résultats attendus

### Avant la correction (❌)
```
Pan → Ligne reste figée aux coordonnées originales
     → Utilisateur voit la ligne à la mauvaise position
     → Après 2 lignes, confusion totale
```

### Après la correction (✅)
```
Pan → Ligne se déplace avec le chart
    → Utilisateur voit toujours la ligne aux mêmes bougies
    → Même après 10 lignes, tout est cohérent
```

---

## 🎬 Vidéo de test (si vous enregistrez)

Filmer les séquences suivantes :
1. Tracer une ligne
2. Pan lent du chart (10 secondes)
3. Vérifier la position
4. Zoom avant
5. Pan supplémentaire
6. Zoom arrière
7. Tracer une seconde ligne
8. Pan final

**Durée attendue** : ~1 minute par ligne

---

## 📝 Rapport de test

Template :
```
Date: [date]
Testeur: [nom]
Version: [app version]

Lignes testées: [nombre]
Scénarios: [1-8]
Résultats: ✅ ALL PASS / ⚠️ SOME ISSUES / ❌ CRITICAL

Issues trouvées:
- [Issue 1]
- [Issue 2]

Observations:
- [Note]
```

---

**Test terminé quand** : Tous les ✅ sont cochés  
**Escalade si** : 2+ ❌ ou un crash

