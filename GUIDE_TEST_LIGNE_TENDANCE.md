# 🧪 GUIDE DE TEST - Ligne de tendance persistante

## ✅ Prérequis

- Android SDK configuré
- Java développement installé
- Gradle correctement configuré
- Emulateur Android ou appareil réel connecté

---

## 🚀 Étapes pour compiler et tester

### 1. Nettoyer et reconstruire
```bash
cd C:\Users\is\Desktop\backtest

# Nettoyer les anciens builds
.\gradlew clean

# Construire le projet
.\gradlew build -x test

# Si vous avez une erreur JAVA_HOME, définissez-la:
# set JAVA_HOME=C:\Program Files\Java\jdk-17 (ou votre version)
```

### 2. Installer l'APK
```bash
# Installer sur emulateur/appareil
.\gradlew installDebug

# Ou directement lancer
.\gradlew runDebug
```

---

## 🎯 Scénario de test complet

### Test 1: Ligne simple
```
1. Lancer l'application
2. Attendre le chargement des données (graphique avec bougies)
3. ✅ Vérifier que le graphique s'affiche correctement
```

### Test 2: Dessin d'une première ligne
```
1. Repérer la barre d'outils en haut à gauche (icônes favoris)
2. Cliquer sur l'icône "Ligne de tendance" (elle doit devenir JAUNE)
3. ✅ Vérifier que le viseur (croix BLEUE) apparaît au centre

4. Cliquer sur le premier point (P1)
   - ✅ Point BLEU doit apparaître
   - ✅ Viseur toujours visible

5. Glisser vers le deuxième point
   - ✅ Aperçu de ligne ROSE/MAGENTA doit suivre
   - ✅ Viseur doit garder son écart avec le doigt

6. Cliquer sur le deuxième point (P2)
   - ✅ Point BLEU doit apparaître
   - ✅ Ligne ROSE/MAGENTA entre P1 et P2 doit PERSISTER
   - ✅ Mode désactivé (icône redevient grise)
```

### Test 3: Dessiner une deuxième ligne
```
1. Cliquer à nouveau sur l'icône "Ligne de tendance" (redevient JAUNE)
2. Viseur réapparaît au centre

3. Cliquer sur un nouveau P1 (autre position)
   - ✅ Première ligne reste visible
   - ✅ Nouveau point BLEU apparaît

4. Glisser vers le nouveau P2
   - ✅ Deux lignes doivent être visibles
   - ✅ Nouvelle ligne en aperçu (ROSE/MAGENTA)

5. Cliquer sur le nouveau P2
   - ✅ Deux lignes ROSE/MAGENTA finales
   - ✅ 4 points BLEU (2 par ligne)
```

### Test 4: Navigation et persistance
```
1. Zoomer/dézoomer avec les doigts
   - ✅ Les deux lignes restent visibles

2. Scroller à gauche/droite
   - ✅ Les lignes se déplacent correctement

3. Fermer et rouvrir l'app (optionnel)
   - ⚠️ Note: Les lignes ne sont PAS persistées en disque (comportement attendu pour v1)
```

### Test 5: Annulation
```
1. Cliquer sur "Ligne de tendance" pour activer le mode
2. Cliquer sur P1
3. Appuyer longuement (long press) sur le graphique
   - ✅ Le mode doit s'annuler
   - ✅ Aucune nouvelle ligne ne doit être créée

4. Les anciennes lignes doivent rester visibles
```

---

## ✨ Résultats attendus

### ✅ SUCCÈS
- [x] Ligne 1 persiste après son dessin
- [x] Ligne 2 coexiste avec ligne 1
- [x] Les points des deux lignes sont bleus
- [x] Les lignes sont rose/magenta
- [x] Zoom/scroll ne supprime pas les lignes
- [x] Annulation fonctionne correctement

### ❌ ÉCHEC (à signaler)
- [ ] Une ligne disparaît après son dessin
- [ ] Les deux lignes se mélangent/superposent mal
- [ ] Les couleurs ne sont pas correctes
- [ ] Performance très dégradée (lag)
- [ ] App plante lors du dessin

---

## 🐛 Debugging

Si vous rencontrez des problèmes, vérifiez:

### 1. Les logs Logcat
```bash
.\gradlew logcat | grep "TrendLine\|CandlestickChart"
```

### 2. Vérifiez les modifications de code
```bash
# Vérifier que completedTrendLines existe (ligne 100)
grep "completedTrendLines" app/src/main/java/com/bthr/backtest/ui/components/CandlestickChart.kt

# Vérifier que la sauvegarde existe (ligne 489-491)
sed -n '489,491p' app/src/main/java/com/bthr/backtest/ui/components/CandlestickChart.kt

# Vérifier le dessin des lignes sauvegardées (ligne 934-953)
sed -n '934,953p' app/src/main/java/com/bthr/backtest/ui/components/CandlestickChart.kt
```

### 3. Erreurs de compilation fréquentes
```
ERROR: JAVA_HOME is not set
→ Définir JAVA_HOME avant ./gradlew

ERROR: Gradle version mismatch
→ Utiliser ./gradlew (pas gradle directement)

ERROR: Compilation error around line 100
→ Vérifier la syntaxe de completedTrendLines
```

---

## 📊 Métriques de test

| Test | Attendu | Réel | Statut |
|------|---------|------|--------|
| Ligne 1 persiste | ✅ | ❓ | Test requis |
| Ligne 2 coexiste | ✅ | ❓ | Test requis |
| Zoom/scroll | ✅ | ❓ | Test requis |
| Performance | ✅ | ❓ | Test requis |
| Annulation | ✅ | ❓ | Test requis |

---

## 📝 Rapportage

Après les tests, remplir ce template:

```markdown
## Rapport de test - Ligne de tendance

**Date**: [DATE]
**Testeur**: [NOM]
**Appareil**: [MODEL Android]
**Résultat**: ✅ SUCCÈS / ⚠️ PARTIEL / ❌ ÉCHEC

### Tests effectués
- [ ] Ligne 1 persiste
- [ ] Ligne 2 coexiste
- [ ] Navigation
- [ ] Performance
- [ ] Annulation

### Observations
[Vos observations ici]

### Bugs restants (si applicable)
[Décrire les bugs trouvés]
```

---

## 🎬 Vidéo de test (optionnel)

Pour documenter le test, vous pouvez:

```bash
# Enregistrer l'écran avec Android Studio
# Outils → Android → Enregistreur d'écran

# Ou avec adb
adb shell screenrecord /sdcard/test.mp4
adb pull /sdcard/test.mp4
```

---

## ✅ Checklist finale

Avant de valider le fix:

- [x] Code modifié: CandlestickChart.kt (3 sections)
- [x] Variable ajoutée: completedTrendLines (ligne 100)
- [x] Sauvegarde implémentée: Au 2e clic (ligne 489-491)
- [x] Dessin implémenté: Boucle forEach (ligne 934-953)
- [x] Pas d'erreurs de compilation
- [x] Documentation créée (ce fichier + AVANT_APRES)
- [ ] **Tests effectués avec succès** ← À faire
- [ ] Autres tests de régression OK

---

**Statut de test**: ⏳ En attente  
**Date du fix**: 2026-04-20  
**Priorité**: 🔴 Haute (feature core)

