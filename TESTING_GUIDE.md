# Guide de Test - Fonctionnalité de Traçage de Ligne de Tendance

## Prérequis
- Avoir le projet compilé et l'application en exécution
- Avoir des données de chandelles affichées sur le graphique
- L'icône "Ligne de tendance" doit être présente dans la barre de favoris

## Scénarios de test

### Scénario 1 : Activation basique du mode
**Objectif** : Vérifier que le mode de traçage s'active/désactive correctement

1. Observez la barre d'outils des favoris en haut à gauche
2. Cliquez sur l'icône "Ligne de tendance"
   - ✓ L'icône doit changer de couleur vers le doré/jaune
3. Cliquez à nouveau sur l'icône
   - ✓ L'icône doit revenir à sa couleur normale (gris)

### Scénario 2 : Viseur et premier point
**Objectif** : Vérifier l'affichage du viseur et la sélection du premier point

1. Cliquez sur l'icône "Ligne de tendance" pour l'activer
2. Cliquez sur n'importe quel point du graphique (zone des bougies)
   - ✓ Un cercle jaune doit apparaître à la position cliquée
   - ✓ C'est le premier point de la ligne

### Scénario 3 : Viseur interactif
**Objectif** : Vérifier que le viseur se met à jour en temps réel

1. Après avoir sélectionné le premier point (Scénario 2)
2. Déplacez votre doigt/souris sur le graphique
   - ✓ Un viseur avec deux lignes perpendiculaires (croix) doit apparaître
   - ✓ Le viseur doit suivre votre mouvement en temps réel
   - ✓ Une ligne jaune devrait connecter le premier point au viseur

### Scénario 4 : Traçage complet d'une ligne
**Objectif** : Vérifier le traçage d'une ligne de tendance complète

1. Activez le mode de traçage
2. Cliquez sur le graphique pour le premier point
3. Déplacez le viseur à une nouvelle position
4. Cliquez à nouveau pour valider le deuxième point
   - ✓ Une ligne jaune doit relier les deux points
   - ✓ Le mode doit se désactiver automatiquement (icône revient au gris)
   - ✓ Le viseur doit disparaître

### Scénario 5 : Annulation du traçage
**Objectif** : Vérifier que le traçage peut être annulé

1. Activez le mode de traçage
2. Cliquez pour sélectionner le premier point
3. Maintenez un long tap/click (appui long)
   - ✓ Le premier point doit disparaître
   - ✓ Le mode reste actif (icône toujours dorée)
   - ✓ Vous pouvez tracer une nouvelle ligne

### Scénario 6 : Désactivation sans traçage complet
**Objectif** : Vérifier que les états sont bien réinitialisés

1. Activez le mode de traçage
2. Cliquez pour le premier point
3. Cliquez sur l'icône pour désactiver le mode
   - ✓ L'icône revient au gris
   - ✓ Le premier point disparaît
   - ✓ Le viseur disparaît

### Scénario 7 : Lignes multiples (séquence)
**Objectif** : Vérifier qu'on peut tracer plusieurs lignes consécutives

1. Tracez une première ligne (Scénario 4)
2. Cliquez à nouveau sur l'icône pour réactiver le mode
3. Tracez une deuxième ligne
   - ✓ Les deux lignes doivent être visibles
   - ✓ Pas d'interférence entre elles

## Tests de régression

### Vérifier que les autres fonctionnalités restent intactes
- [ ] Le crosshair normal (long press) fonctionne toujours
- [ ] Les autres icônes de la barre de favoris fonctionnent
- [ ] Le zoom et le pan du graphique fonctionnent normalement
- [ ] Les indicateurs s'affichent correctement
- [ ] Les interactions avec les bougies (sélection, etc.) fonctionnent

## Cas limites à tester

### 1. Clic en dehors de la zone du graphique
- Activez le mode de traçage
- Cliquez en dehors de la zone des bougies (région des prix, région temporelle)
  - ✓ Rien ne doit se passer, le premier point ne doit pas être sélectionné

### 2. Clic très rapide
- Activez le mode de traçage
- Cliquez deux fois très rapidement
  - ✓ La ligne doit se tracer correctement
  - ✓ Pas de comportement imprévisible

### 3. Glissement sur les bords
- Sélectionnez le premier point près du bord du graphique
- Déplacez le viseur jusqu'aux extrêmes
  - ✓ Le viseur doit rester dans les limites du graphique
  - ✓ Les lignes doivent rester visibles

## Métriques de succès
- [ ] Le mode de traçage s'active/désactive sans erreur
- [ ] Le viseur s'affiche correctement avec les bonnes couleurs
- [ ] Les lignes de tendance se tracent sans artefacts
- [ ] Les performances ne se dégradent pas (pas de lag)
- [ ] L'annulation fonctionne sans laisser de traces
- [ ] Les autres fonctionnalités ne sont pas affectées

## Bugs connus à signaler (le cas échéant)
- Aucun connu actuellement

## Notes supplémentaires
- Les lignes tracées ne sont actuellement pas persistées (elles disparaissent au redémarrage)
- Les lignes ne peuvent pas être modifiées après traçage
- Un seul mode de traçage est actif à la fois

