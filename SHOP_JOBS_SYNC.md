# SYNCHRONISATION SHOP & JOBS - Guide de Configuration

## Vue d'Ensemble

Ce document explique comment les prix du shop sont synchronisés avec les récompenses des métiers pour maintenir une économie équilibrée.

## Principe de Synchronisation

### Ratio de Base
- **Temps de farming** : Un joueur devrait pouvoir acheter ce qu'il farm en environ 10-20 actions
- **Progression** : Les récompenses augmentent avec le niveau (multiplicateurs)
- **Balance économique** : Les prix du shop reflètent la difficulté d'obtention

### Exemples de Synchronisation

#### Mineur (Miner)
```
Minerai -> Reward Job -> Prix Shop -> Ratio
-----------------------------------------------
Coal Ore -> 3 ⛁/bloc -> Coal: 5 ⛁/16 -> ~21 blocs pour acheter 16
Iron Ore -> 5 ⛁/bloc -> Iron Ingot: 25 ⛁/8 -> ~40 blocs pour acheter 8
Gold Ore -> 8 ⛁/bloc -> Gold Ingot: 50 ⛁/8 -> ~50 blocs pour acheter 8
Diamond Ore -> 15 ⛁/bloc -> Diamond: 500 ⛁/1 -> ~33 blocs pour acheter 1
```

**Au niveau 1 (x1.0)** : ~33 diamants minés = 1 diamant acheté
**Au niveau 50 (x3.0)** : ~11 diamants minés = 1 diamant acheté
**Au niveau 100 (x5.0)** : ~7 diamants minés = 1 diamant acheté

#### Bûcheron (Woodcutter)
```
Bois -> Reward Job -> Prix Shop -> Ratio
-----------------------------------------------
Oak Log -> 2 ⛁/log -> Oak Planks: 15 ⛁/64 -> ~30 logs pour 64 planches
Dark Oak -> 3 ⛁/log -> Oak Planks: 15 ⛁/64 -> ~20 logs pour 64 planches
```

#### Fermier (Farmer)
```
Culture -> Reward Job -> Prix Shop -> Ratio
-----------------------------------------------
Wheat -> 1.5 ⛁ -> Bread: 10 ⛁/16 -> ~10-11 récoltes pour 16 pains
Cow Breeding -> 5 ⛁ -> Cooked Beef: 15 ⛁/16 -> ~48 élevages pour 16 steaks
```

#### Chasseur (Hunter)
```
Monstre -> Reward Job -> Équivalence
-----------------------------------------------
Zombie -> 3 ⛁ -> ~3 zombies = 1 flèche (2⛁/64)
Skeleton -> 3.5 ⛁ -> ~4 skeletons = 1 arc (80⛁)
Blaze -> 12 ⛁ -> ~21 blazes = 1 pomme d'or (250⛁)
```

## Formules de Calcul

### Pour déterminer un prix de shop équilibré :
```
Prix Shop = Reward Job × Multiplicateur Cible × Quantité

Exemple pour le diamant:
- Reward job niveau 1: 15 ⛁
- Multiplicateur cible: ~33 (nombre d'actions pour acheter)
- Quantité: 1
- Prix = 15 × 33 × 1 = 495 ⛁ ≈ 500 ⛁
```

### Pour déterminer une récompense de job équilibrée :
```
Reward Job = Prix Shop / (Multiplicateur Cible × Quantité)

Exemple pour le charbon:
- Prix shop: 5 ⛁ pour 16
- Multiplicateur cible: ~20 (actions)
- Quantité: 16
- Reward = 5 / (20 × 16) × 16 = 5/20 ≈ 0.25 ⛁
Ajusté à 3 ⛁ pour rendre le mining plus rentable
```

## Recommandations pour Équilibrage

### Ressources Communes (Pierre, Bois, Blé)
- Reward: 1-2 ⛁
- Prix shop: 10-15 ⛁ par stack
- Ratio: 20-30 actions pour acheter

### Ressources Rares (Fer, Or)
- Reward: 5-8 ⛁
- Prix shop: 25-50 ⛁ pour petite quantité
- Ratio: 30-50 actions pour acheter

### Ressources Très Rares (Diamant, Émeraude)
- Reward: 15-20 ⛁
- Prix shop: 300-500 ⛁ par unité
- Ratio: 25-35 actions pour acheter

### Activités Spéciales (Breeding, Fishing Treasure)
- Reward: 5-15 ⛁
- Prix shop: Variable selon rareté
- Ratio: Plus récompensant car plus difficile

## Impact des Niveaux

Le système de progression rend les métiers plus rentables :

### Niveau 1 → Niveau 100
- Multiplicateur: x1.0 → x5.0
- Temps pour acheter un item: Divisé par 5
- Encouragement à progresser dans un métier

### Exemple Concret
Un mineur niveau 100 gagne 5x plus qu'un niveau 1:
- Niveau 1: Mine 33 diamants → Achète 1 diamant (500⛁)
- Niveau 100: Mine 7 diamants → Achète 1 diamant (500⛁)

## Notes pour les Administrateurs

### Ajustements Possibles
1. **Modifier jobs.yml** : Changer les rewards et multiplicateurs
2. **Modifier shop.yml** : Ajuster les prix des items
3. **Surveiller l'économie** : Vérifier que les joueurs ne s'enrichissent pas trop vite

### Indicateurs d'Équilibre
- Les joueurs devraient progresser de 1-2 niveaux par heure de jeu actif
- Un nouveau joueur (niveau 1) devrait gagner 50-100⛁ par 30 minutes
- Un joueur niveau 100 devrait gagner 250-500⛁ par 30 minutes

### Outils de Debug
- `/ecoadmin` : Gérer l'économie des joueurs
- Logs des transactions : Surveiller les grosses sommes
- Stats des métiers : Voir la progression des joueurs

## Formule Excel pour Calculs Rapides

```
Reward Optimal = Prix_Shop / (Actions_Cibles × Quantité_Shop)
Prix Optimal = Reward_Job × Actions_Cibles × Quantité
Temps Farming = (Prix_Shop / Reward_Job) / Multiplicateur_Niveau
```
