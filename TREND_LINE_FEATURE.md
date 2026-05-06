# Fonctionnalité de Traçage de Ligne de Tendance

## Description
Cette fonctionnalité permet aux utilisateurs de tracer des lignes de tendance sur le graphique en temps réel avec une interface intuitive incluant un viseur (crosshair).

## Comment utiliser

### Activation du mode de traçage
1. Cliquez sur l'icône **"Ligne de tendance"** dans la barre d'outils des favoris (en haut à gauche du graphique)
2. L'icône se teinte en **doré/jaune** indiquant que le mode est activé
3. Un **viseur** (crosshair) apparaît sur le graphique

### Traçage d'une ligne
1. **Premier point** : Cliquez sur le graphique pour sélectionner le premier point de la ligne
   - Un cercle jaune apparaît pour marquer le premier point
   - Le viseur devient actif pour le deuxième point

2. **Deuxième point** : Déplacez le doigt/la souris pour positionner le viseur
   - Une ligne de prévisualisation en **jaune** relie les deux points
   - Le viseur se compose de :
     - Deux lignes perpendiculaires (horizontale et verticale)
     - Un petit cercle central au centre du viseur

3. **Finalisation** : Cliquez sur le deuxième point pour finaliser la ligne de tendance
   - La ligne est dessinée entre les deux points
   - Le mode de traçage se désactive automatiquement
   - Le viseur disparaît

### Annulation
- **Long tap/Long click** : Maintenez le doigt/bouton appuyé pendant le traçage pour annuler
  - Les points sont réinitialisés
  - Le mode reste actif pour tracer une nouvelle ligne

### Désactivation du mode
- Cliquez à nouveau sur l'icône "Ligne de tendance" pour désactiver le mode
- Ou effectuez une action sur un autre élément du graphique

## Caractéristiques visuelles

### Couleurs utilisées
- **Jaune** : Points de ligne et viseur (indication d'action active)
- **Semi-transparent** : Lignes du viseur et ligne de prévisualisation

### Interaction pendant le traçage
- Le viseur se **met à jour en temps réel** pendant que vous bougez
- La ligne de prévisualisation montre **précisément** où la ligne sera tracée

## Intégration technique

### États gérés dans CandlestickChart
```kotlin
var isTrendLineMode: Boolean              // Mode de traçage actif/inactif
var trendLinePoint1: Offset?              // Premier point de la ligne
var trendLinePoint2: Offset?              // Deuxième point de la ligne
var trendLineCrosshair: Offset?           // Position actuelle du viseur
```

### Interactions supportées
- **Tap/Click** : Sélection des points
- **Drag/Glissement** : Mise à jour de la position du viseur
- **Long Press** : Annulation du traçage en cours
- **Clic sur l'icône** : Activation/Désactivation du mode

## Améliorations futures possibles
- Sauvegarde des lignes tracées dans une base de données
- Modification des lignes existantes
- Couleurs personnalisables pour les lignes
- Épaisseur de trait personnalisable
- Support pour d'autres outils de dessin (rectangles, cercles, etc.)

