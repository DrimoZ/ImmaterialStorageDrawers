# Spike — le contrôleur Storage Drawers lie-t-il un bloc tiers, sans mixin ?

**Ce que fait ce fichier :** le compte rendu de la première tâche du plan, bloquante pour la suite. Ce qui
était estimé, ce qui a tourné, où l'estimation était fausse. Ce n'est pas la doc d'architecture.

Daté du 7 novembre 2026. Storage Drawers `1.21.1-13.11.4` (NeoForge), NeoForge `21.1.248`.

## Verdict : PASSÉ

`./gradlew runGameTestServer` → **5/5 game tests** (`gametest/SpikeGameTests.java`) :

| Test | Ce qu'il prouve |
|---|---|
| `ourCapabilityIsTheOneStorageDrawersQueries` | `BlockCapability.create("storagedrawers:drawer_group", …)` chez nous **est** l'objet que SD interroge : on ne dépend pas de leur classe interne `Capabilities` |
| `aNodeNextToAControllerIsLinked` | le contrôleur appelle `softBindControlGroup` sur notre groupe ; notre bloc ajoute **0** slot d'item |
| `aNodeOnlyReachableThroughAnotherNodeIsLinked` | la recherche du contrôleur **traverse** nos blocs (un mur mixte marche) |
| `breakingTheControllerUnlinksTheNode` | contrôleur cassé → `isSoftBindingValid` le rejette, le nœud ne connaît plus de contrôleur |
| `aWallOfNodesIsLinkedAndDoesNotRebuildTheControllerEveryTick` | 50 nœuds liés **par le chemin normal du jeu** (pas d'appel à `updateCache()`), et aucune reconstruction pendant 60 ticks |

Aucun mixin, aucune réflexion dans le mod (la réflexion n'est que dans le test, pour lire
`lastUpdateTime`).

## Ce qui a été construit

- `network/NetworkNodeBlock` : `Block` + `INetworked` (c'est le **bloc** que teste `populateRoot`).
- `network/NetworkNodeBlockEntity` : expose un `IDrawerGroup` à 0 tiroir qui est aussi `INetworked` ;
  retient les contrôleurs qui le soft-bindent, purge à chaque lecture via `isSoftBindingValid`.
- `StorageDrawersContract` : le seul fichier du mod qui nomme quelque chose de SD hors de son package `api`.
- Bloc de test `immaterialstorage_sd:network_node`, sans item ni modèle.

## Où l'estimation était fausse

1. **La coordonnée Maven par numéro de version sert le jar Fabric.**
   `maven.modrinth:storagedrawers:1.21.1-13.11.4` a résolu vers le jar Fabric (erreurs
   `class_1937 not found` = noms intermediary). Les jars NeoForge, Forge et Fabric partagent le même
   numéro. **Corrigé** : épinglé par id de version Modrinth `px0CCB06`. Même piège que GeckoLib chez
   le frère — à retenir pour toute dépendance multi-loader.
2. **Le contexte de la capability est `Void.TYPE`, pas `Void.class`.** SD passe le `void` primitif ;
   NeoForge refuse un même nom avec un contexte différent. Vu à la lecture, avant la compilation.
3. **« Ne reconstruit pas à chaque tick » : premier essai en échec, à tort.** Le contrôleur programme
   sa propre reconstruction **1 tick après sa pose** (`onEntityLoad`), puis toutes les 100 ticks
   (`BlockController.tick`). Mon test appelait `updateCache()` puis voyait celle de SD 2 ticks plus
   tard. Ce n'était pas nous. Le test attend désormais la reconstruction naturelle.
4. **`GameTestHelper.assertionException` n'existe pas en 1.21.1** → `new GameTestAssertException`.

## Ce que le spike ne prouve PAS encore

- ~~L’agrégation FE sur le contrôleur~~ : **faite après le spike** (tâche 2, 7 nov.). Le sens
  contrôleur → nœuds est un index tenu par nos nœuds (`network/ControllerNodes`), rempli au
  soft-bind, vidé au `setRemoved`, revalidé à chaque lecture ; `EnergyStorage.BLOCK` est enregistré
  sur le type `storagedrawers:controller`. 5 game tests de plus (`EnergyGameTests`).
- **Délai de liaison en jeu** : un nœud posé à côté d'un contrôleur existant n'est lié qu'à la
  prochaine reconstruction, **jusqu'à 100 ticks (5 s)**. Acceptable ? À voir en jeu ; sinon,
  programmer un tick sur le contrôleur à la pose du nœud.
- **Rechargement de chunk / de monde** : la liaison est en mémoire seulement ; elle se refait au
  premier rebuild après chargement. Non testé.
- **Contrôleurs qui se chevauchent**, upgrade Remote, Controller Slave (`BlockControllerIO`).
- Client : rien n'a été lancé hors du serveur de test.
