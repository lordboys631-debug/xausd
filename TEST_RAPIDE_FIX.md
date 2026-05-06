# 🧪 QUICK START - Test Rapide du Fix

## ⚡ Test en 2 minutes

### Étape 1: Démarrer l'app

1. Connecter un appareil Android ou émulateur
2. Compiler et déployer : `./gradlew installDebug`
3. Ouvrir l'app Backtest

### Étape 2: Accéder au chart

1. Aller à l'écran du chart (BTCUSDT, M5)
2. Attendre que les données se chargent
3. Afficher ~100 bougies

### Étape 3: Tester la ligne

**Test 1: Tracer une ligne (30s)**
```
1. Appuyer sur l'outil Trend Line (bouton or en haut à gauche)
2. Cliquer sur le chart: Point 1 (bas de l'écran)
3. Cliquer sur le chart: Point 2 (haut de l'écran)
4. ✅ Vérifier: Ligne rose entre les deux points
5. ✅ Vérifier: Points bleus aux extrémités
```

**Test 2: Pan horizontal (30s)**
```
1. Avec la ligne affichée
2. Faire un swipe horizontal vers la gauche
3. ✅ CRITÈRE: La ligne doit se déplacer AVEC le chart
4. ✅ CRITÈRE: Les points bleus bougent aussi
5. ✅ CRITÈRE: La ligne ne reste PAS figée
```

**Test 3: Zoom (1m)**
```
1. Zoomer avant avec deux doigts (pinch in)
2. ✅ CRITÈRE: La ligne s'agrandit
3. ✅ CRITÈRE: Pas de distorsion
4. Zoomer arrière (pinch out)
5. ✅ CRITÈRE: La ligne rétrécit
6. ✅ CRITÈRE: Position reste correcte
```

### Résultat final
- ✅ 3/3 tests passés = **FIX RÉUSSI** 🎉
- ⚠️  1-2 tests échoués = Voir section débogage
- ❌ 3/3 tests échoués = Voir section "Ça ne fonctionne pas"

---

## 🐛 Débogage rapide

### Symptôme: Ligne reste figée lors du pan

**Diagnostic**:
```
Avant pan: Ligne à (100, 250)
Après pan gauche: Ligne à (100, 250)  ← MÊME POSITION
                  ❌ MAUVAIS - Devrait être à (~50, 250)
```

**Solution**:
1. Vérifier que le code est bien compilé
2. Vérifier que les formules de conversion sont correctes
3. Checker la ligne 971-972 pour la conversion X

### Symptôme: Ligne a disparu après le zoom

**Diagnostic**:
```
Avant zoom: Ligne visible
Après zoom: Ligne invisible ❌
```

**Cause possible**: `candleW` était trop petit
- Vérifier la validation de `candleW > 0` (ligne 969)

**Solution**:
1. Vérifier que `normY()` fonctionne correctement
2. Vérifier que le visibility check n'est pas trop restrictif

### Symptôme: Ligne au mauvais endroit

**Diagnostic**:
```
Expected: (150, 200)
Actual:   (250, 300)
Offset:   (+100, +100)
```

**Cause possible**: 
- Erreur dans la conversion indices → prix
- Erreur dans la conversion pixels → indices

**Solution**:
1. Vérifier la formule de conversion au ligne 513-518
2. Vérifier les marges (topMarginPx, bottomMarginPx)

---

## 📊 Checklist de vérification rapide

```
✅ Ligne s'affiche immédiatement après 2 taps
✅ Ligne est rose/magenta (#E91E63)
✅ Points aux extrémités sont bleus (#1E88E5)
✅ Pan → Ligne suit le chart
✅ Zoom avant → Ligne s'agrandit
✅ Zoom arrière → Ligne se rétrécit
✅ Multiples lignes → Toutes synchronisées
✅ Aucun crash
✅ Aucun lag visuel
✅ Pas de super-positionnement
```

**Tous cochés = ✅ FIX OK**

---

## 🚀 Déployer après test

### Si les tests passent ✅
```bash
# Finaliser
git add -A
git commit -m "Fix: Synchronisation des lignes de tendance avec le chart"

# Créer l'APK final
./gradlew bundleRelease

# Tester sur device réel si possible
adb install -r app/build/outputs/apk/release/app-release.apk

# Après vérification: push
git push origin feature/trend-line-sync
```

### Si les tests échouent ❌
```bash
# Revenir à la version précédente
git revert HEAD --no-edit

# Recompiler
./gradlew clean build

# Déboguer
# (voir section "Ça ne fonctionne pas")
```

---

## ❌ Ça ne fonctionne pas?

### Erreur: "Cannot resolve symbol 'TrendLineData'"

**Solution**:
```
La classe n'a pas été trouvée. Vérifier:
1. Que la classe est définie ligne 58-63
2. Que la classe est AVANT le @Composable
3. Que la classe n'est pas inside le Composable
4. Recompiler: ./gradlew clean build
```

### Erreur: "Type mismatch: expected Pair<Offset, Offset>, got TrendLineData"

**Solution**:
```
Une partie du code n'a pas été mise à jour. Chercher:
1. Les appels à completedTrendLines
2. Remplacer les Pair par TrendLineData
3. Vérifier les lignes 934-953 (rendu ancien)
```

### Erreur: Ligne apparaît à la mauvaise position

**Solution**:
```
La formule de conversion est incorrecte. Vérifier:
1. Ligne 513: Conversion pixels → idx
2. Ligne 517: Conversion pixels → idx
3. Ligne 971: Conversion idx → pixels
4. Ligne 972: Conversion idx → pixels

Les formules doivent être symétriques:
idx = f(x) et x = f_inverse(idx)
```

### Erreur: Crash lors du rendu

**Solution**:
```
Une exception a été levée. Vérifier:
1. Pas d'index out of bounds
   - coerceIn(0, allCandles.size - 1) ligne 513
   - coerceIn(0, allCandles.size - 1) ligne 517
2. Pas de division par zéro
   - if (candleW > 0) ligne 969
3. Pas de null pointer
   - Tous les !! sont protégés
```

---

## 📱 Configuration recommandée pour le test

| Paramètre | Valeur |
|-----------|--------|
| Device | Émulateur ou téléphone réel |
| OS Android | 10+ (API 29+) |
| Display | 1080x1920 ou plus |
| Bougies | 60-100 visibles |
| Timeframe | M5 ou M15 |
| Paires | BTC, ETH, altcoins stables |

---

## 🎓 Comprendre le test

### Pourquoi pan?
- Pan teste le calcul avec `scrollOffset`
- C'est où le bug était au départ

### Pourquoi zoom?
- Zoom teste le recalcul de `candleW`
- Important pour s'assurer que la position se recalcule

### Pourquoi multiples lignes?
- Teste que chaque ligne reste cohérente
- Évite les bugs d'état global

---

## ⏱️ Durée totale: ~5-10 minutes

```
Setup:        2 min
Test 1 (Tap): 1 min
Test 2 (Pan): 2 min
Test 3 (Zoom): 2 min
Documentation: 2 min
─────────────────
TOTAL:        9 min
```

---

## ✨ Si tout fonctionne

Bravo! 🎉

Le fix est prêt pour:
- ✅ Merge dans master
- ✅ Déploiement en production
- ✅ Release aux utilisateurs

Les lignes de tendance vont maintenant:
- ✅ Suivre le chart
- ✅ Se redimensionner correctement
- ✅ Persister à travers les gestes
- ✅ Fonctionner comme attendu

---

**Guide créé**: 2026-04-20
**Temps de test**: ~5-10 minutes
**Complexité**: ⭐ (Très simple)
**Fiabilité**: ⭐⭐⭐⭐⭐ (5/5)

