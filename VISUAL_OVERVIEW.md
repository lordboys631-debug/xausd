# Aperçu visuel - Traçage de ligne de tendance

## Diagramme du flux d'interaction

```
┌──────────────────────────────────────────────────────────────┐
│         APPLICATION GRAPHIQUE DE BACKTESTING                 │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  Barre de favoris (TOP LEFT - DRAGGABLE)             │  │
│  │  ┌─────────┬─────────┬──────────┬──────────┐         │  │
│  │  │  ≡≡≡    │ 🔼      │ 📏       │ ❌       │         │  │
│  │  │ Handle  │ Autres  │ TENDLINE │ Autres   │         │  │
│  │  │         │ Tools   │ (ACTIF)  │ Tools    │         │  │
│  │  └─────────┴─────────┴──────────┴──────────┘         │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                               │
│  ┌────────────────────────────────────────────────────────┐  │
│  │              ZONE DU GRAPHIQUE CANDLESTICK            │  │
│  │                                                        │  │
│  │    ╔════════════════════════════════════════════╗    │  │
│  │    ║  ÉTAPE 1 : Clic sur TREND_LINE             ║    │  │
│  │    ║  → Mode activé (icône = DORÉ)              ║    │  │
│  │    ║  ┌──────────────────────────────────────┐  ║    │  │
│  │    ║  │  ┃                                   │  ║    │  │
│  │    ║  │  ┃  ▲  ▲  ▲                          │  ║    │  │
│  │    ║  │  ┃ █  █ █ █ █                        │  ║    │  │
│  │    ║  │  ┃ █ █ █ █ █ █                       │  ║    │  │
│  │    ║  │  ┃ █ █ █ █ █ █ █                     │  ║    │  │
│  │    ║  │  ┃ █ █ █ █ █ █ █ █                   │  ║    │  │
│  │    ║  │  ┃ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼                  │  ║    │  │
│  │    ║  │  │                                   │  ║    │  │
│  │    ║  └──────────────────────────────────────┘  ║    │  │
│  │    ╚════════════════════════════════════════════╝    │  │
│  │                                                        │  │
│  │    ╔════════════════════════════════════════════╗    │  │
│  │    ║  ÉTAPE 2 : Clic sur Point 1                ║    │  │
│  │    ║  → P1 sélectionné (cercle jaune)           ║    │  │
│  │    ║  ┌──────────────────────────────────────┐  ║    │  │
│  │    ║  │  ┃                                   │  ║    │  │
│  │    ║  │  ┃  ▲  ▲  ▲                          │  ║    │  │
│  │    ║  │  ┃ █ 🟡█ █ █                        │  ║    │  │
│  │    ║  │  ┃ █ █ █ █ █ █                       │  ║    │  │
│  │    ║  │  ┃ █ █ █ █ █ █ █                     │  ║    │  │
│  │    ║  │  ┃ █ █ █ █ █ █ █ █                   │  ║    │  │
│  │    ║  │  ┃ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼                  │  ║    │  │
│  │    ║  │  │                                   │  ║    │  │
│  │    ║  └──────────────────────────────────────┘  ║    │  │
│  │    ║                                              ║    │  │
│  │    ║  Note: Viseur (croix) sera visible au       ║    │  │
│  │    ║        mouvement du doigt                   ║    │  │
│  │    ╚════════════════════════════════════════════╝    │  │
│  │                                                        │  │
│  │    ╔════════════════════════════════════════════╗    │  │
│  │    ║  ÉTAPE 3 : Mouvement du doigt/souris       ║    │  │
│  │    ║  → Viseur suit le mouvement (croix + cercle)║   │  │
│  │    ║  → Ligne preview entre P1 et viseur        ║    │  │
│  │    ║  ┌──────────────────────────────────────┐  ║    │  │
│  │    ║  │  ┃                  ━━━━━━━━━┃      │  ║    │  │
│  │    ║  │  ┃  ▲  ▲  ▲          ┃      ┃      │  ║    │  │
│  │    ║  │  ┃ █ 🟡█ █ █         ✦      ┃      │  ║    │  │
│  │    ║  │  ┃ █ █ █ █ █ █      ━━━━━━━━━      │  ║    │  │
│  │    ║  │  ┃ █ █ █ █ █ █ █                   │  ║    │  │
│  │    ║  │  ┃ █ █ █ █ █ █ █ █                 │  ║    │  │
│  │    ║  │  ┃ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼                │  ║    │  │
│  │    ║  │  │                                   │  ║    │  │
│  │    ║  └──────────────────────────────────────┘  ║    │  │
│  │    ║  Légende:                                   ║    │  │
│  │    ║  🟡 = Cercle point (5px, jaune)            ║    │  │
│  │    ║  ✦  = Cercle viseur (4px, jaune)           ║    │  │
│  │    ║  ━━━ = Ligne preview (jaune, semi-transp)  ║    │  │
│  │    ║  ┃  = Ligne viseur (jaune, alpha=0.5)      ║    │  │
│  │    ╚════════════════════════════════════════════╝    │  │
│  │                                                        │  │
│  │    ╔════════════════════════════════════════════╗    │  │
│  │    ║  ÉTAPE 4 : Clic sur Point 2                ║    │  │
│  │    ║  → Ligne tracée (P1 ─── P2)               ║    │  │
│  │    ║  → Mode désactivé (icône = gris)           ║    │  │
│  │    ║  → Viseur disparaît                         ║    │  │
│  │    ║  ┌──────────────────────────────────────┐  ║    │  │
│  │    ║  │  ┃                                   │  ║    │  │
│  │    ║  │  ┃  ▲  ▲  ▲                          │  ║    │  │
│  │    ║  │  ┃ █ 🟡 ─ ─ ─ 🟡                    │  ║    │  │
│  │    ║  │  ┃ █ █ █ █ █ █                       │  ║    │  │
│  │    ║  │  ┃ █ █ █ █ █ █ █                     │  ║    │  │
│  │    ║  │  ┃ █ █ █ █ █ █ █ █                   │  ║    │  │
│  │    ║  │  ┃ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼                  │  ║    │  │
│  │    ║  │  │                                   │  ║    │  │
│  │    ║  └──────────────────────────────────────┘  ║    │  │
│  │    ║                                              ║    │  │
│  │    ║  ✓ Ligne tracée avec succès!                ║    │  │
│  │    ║  ✓ Vous pouvez tracer une autre ligne       ║    │  │
│  │    ║    en cliquant à nouveau sur l'icône        ║    │  │
│  │    ╚════════════════════════════════════════════╝    │  │
│  │                                                        │  │
│  │    ╔════════════════════════════════════════════╗    │  │
│  │    ║  INTERACTION: Long Press = ANNULATION      ║    │  │
│  │    ║  (À tout moment pendant le traçage)        ║    │  │
│  │    ║                                              ║    │  │
│  │    ║  Étape 2 ou 3 + Long Press                  ║    │  │
│  │    ║  → Réinitialise P1                          ║    │  │
│  │    ║  → Mode reste actif (icône = DORÉ)          ║    │  │
│  │    ║  → Vous pouvez recommencer                  ║    │  │
│  │    ╚════════════════════════════════════════════╝    │  │
│  │                                                        │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                               │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  Zone des prix (droite)    |                           │  │
│  │  Zone de temps (bas)       |                           │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

## Palette de couleurs

| Élément | Hex | RGB | Utilisation |
|---------|-----|-----|-------------|
| 🟡 Jaune Principal | #FFFF00 | (255, 255, 0) | Points, viseur, lignes |
| 🟡 Jaune Doré (Actif) | #FFFFD700 | (255, 215, 0) | Icône quand mode actif |
| ⚪ Blanc (Normal) | #FFFFFF | (255, 255, 255) | Icône normal |
| ⚫ Gris | #FF555555 | (85, 85, 85, alpha=0.85) | Icône normal |
| 🔲 Semi-transparent | Alpha 0.5 | - | Lignes du viseur |
| 🔲 Semi-transparent | Alpha 0.7 | - | Ligne de prévisualisation |

## Dimensions et espacements

```
┌─ Écran complet
│
├─ Barre de favoris
│  ├─ Handle drag : 10dp × 24dp
│  ├─ Espacement : 8dp (horizontal)
│  ├─ Padding : 10dp (start), 12dp (end), 8dp (top/bottom)
│  └─ Icônes : 24dp × 24dp
│
├─ Graphique principal
│  ├─ Largeur zone bougies : width - 55dp (zone des prix)
│  ├─ Hauteur zone bougies : height - 35dp (zone de temps)
│  ├─ Cercle point P1 : radius = 5px
│  ├─ Cercle viseur : radius = 4px
│  └─ Ligne : strokeWidth = 2px (ou 1px pour viseur)
│
└─ Marges internes
   ├─ Largeur : settings.marginRightBars
   ├─ Haut : settings.marginTopPercent
   └─ Bas : settings.marginBottomPercent
```

## États de transition

```
STATE DIAGRAM
═════════════

          ┌─────────────────┐
          │   INACTIVE      │
          │ (isTrendLineMode │
          │    = false)     │
          └────────┬────────┘
                   │
                   │ Clic sur icône
                   ↓
          ┌─────────────────┐
          │  MODE ACTIF     │
          │ (isTrendLineMode │
          │    = true)      │
          └────────┬────────┘
                   │
      ┌────────────┼────────────┐
      │            │            │
   Clic icon   Clic graph   Long press
      │         (P1 null)        │
      │            │             │
      ↓            ↓             ↓
   INACTIVE   P1 SELECTED   INACTIVE
      (3)        (4)            (3)
                   │
                   │ Clic graph (P1 set)
                   ↓
          ┌─────────────────┐
          │  P2 SELECTED    │
          │  (Ligne tracée) │
          └────────┬────────┘
                   │
                   │ (Auto-reset)
                   ↓
          ┌─────────────────┐
          │   INACTIVE      │
          └─────────────────┘

Legend:
(3) = Retour à l'état INACTIVE
(4) = Passage à P1_SELECTED
```

## Animation/Timing

```
Timeline d'une interaction utilisateur
═════════════════════════════════════

T = 0ms   : Clic sur icône → Mode On
            • isTrendLineMode = true
            • Icône change de couleur (instant)

T = 100ms : Clic sur P1
            • trendLinePoint1 = offset
            • trendLineCrosshair = offset
            • Cercle jaune apparaît

T = 100-500ms : Mouvement souris/doigt
            • trendLineCrosshair mis à jour (chaque 16ms ~ 60fps)
            • Viseur suit le mouvement
            • Ligne preview mise à jour
            • Rendu smooth sans lag

T = 500ms : Clic sur P2
            • trendLinePoint2 = offset
            • Ligne finale dessinée
            • Tous les états réinitialisés
            • Mode désactivé (instant)
            • Icône redevient gris

T = 500+  : Prêt pour nouvelle ligne
            • Utilisateur peut cliquer sur icône
            • Ou tracer une nouvelle ligne

Long Press Timeline (annulation)
────────────────────────────────
T = 0ms   : Long press détecté
T = 500ms : Seuil de long press atteint
            • trendLinePoint1 = null
            • trendLineCrosshair = null
            • Mode reste On
            • Viseur disparaît
            • Prêt pour nouveau P1
```

## Zones interactives

```
Zones de clic/touch
═══════════════════

┌──────────────────────────────────────────┐
│  Barre de favoris (20.dp, 20.dp offset)  │
│  ┌────────────────────────────────────┐  │
│  │  Zone de drag (10dp width) │ Icônes │  │
│  │  [≡≡≡] [🔼] [📏 ACTIF] [❌]          │  │
│  │   ^                                │  │
│  │   └─ Draggable zone                │  │
│  └────────────────────────────────────┘  │
│                                          │
│  Zones de graphique                      │
│  ┌────────────────────────────────────┐  │
│  │                                    │  │
│  │  Zone de traçage (tapable/draggable)  │
│  │  • Tap = sélection point           │  │
│  │  • Drag = viseur                   │  │
│  │                                    │  │
│  │  Zone bougies (tapable)            │  │
│  │  • Tap = interaction normal        │  │
│  │                                    │  │
│  │ Zone prix    Zone de temps │       │  │
│  │ (read-only)  (read-only)   │       │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

---

**Version** : 1.0  
**Dernière mise à jour** : 2026-04-20  
**Format** : ASCII Art + Diagrammes

