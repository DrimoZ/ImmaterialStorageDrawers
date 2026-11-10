# Immaterial Storage

Storage Drawers for what isn't an item.

An addon for [Storage Drawers](https://github.com/jaquadro/StorageDrawers). Storage Drawers stores
items; this one adds drawers for everything else: **energy**, **fluids**, **Mekanism chemicals** and
**Ars Nouveau Source**. They sit in a wall of Storage Drawers drawers, take its upgrades, join its
controllers and its framing table, and look like the drawers of its sibling mod,
[Immaterial Drawers](https://github.com/DrimoZ/ImmaterialDrawers) (the same family, for Functional
Storage).

> **Status: pre-release.** Everything below runs in game tests and has been looked at in game.
> Nothing is published yet. See [ROADMAP.md](ROADMAP.md).

## Drawers

| Drawer | Layouts | Base capacity | Needs |
|---|---|---|---|
| Energy Drawer | 1x1 | 500,000 FE | - |
| Fluid Drawer | 1x1, 1x2, 2x2 | 32,000 mB, shared by the compartments | - |
| Chemical Drawer | 1x1, 1x2, 2x2 | 32,000 mB, shared by the compartments | Mekanism |
| Source Drawer | 1x1 | 32,000 Source | Ars Nouveau |

- **Storage Drawers' own upgrades**, 7 slots: the storage upgrades multiply the capacity exactly as
  they do for items (from Storage Drawers' config), plus void, creative storage, creative vending,
  redstone (a comparator reads how full the drawer is) and illumination.
- **Storage Drawers' keys**: the Drawer Key locks a compartment to its fluid or chemical, Quantify
  and Shroud hide the amount or the contents, Priority and Suspend steer controllers, the Personal
  Key keeps other players out. Each shows Storage Drawers' own icon on the front.
  Energy is counted in 64 bits: seven Netherite upgrades hold about 7.2 billion FE.
- **Controllers and Controller Slaves** expose the energy, fluids, chemicals and Source of every
  drawer they link, as one store, to cables, pipes, tubes and relays.
- **Framing table**: every drawer can be framed like a Storage Drawers drawer, and keeps its contents.
- **Breaking a drawer keeps everything** (contents, upgrades, frame) in the dropped item.
- **By hand**: buckets fill and empty the compartment they are used on, and so do Mekanism chemical
  tanks. Ars Nouveau's relays, apparatus and sourcelinks find a Source Drawer like a Source Jar.
- **Jade and The One Probe** show every content, on drawers and on controllers.
- **Configuration** (`immaterialstorage_sd-common.toml`): each drawer's base capacity.

## Requires

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1 |
| Storage Drawers | 13.11.4+ |
| Mekanism *(optional)* | 10.7+, for the Chemical Drawers |
| Ars Nouveau *(optional)* | 5.13+, for the Source Drawer |
| Jade, The One Probe *(optional)* | for the probe readouts |

Without Mekanism or Ars Nouveau, their drawers simply do not exist; nothing else changes.

## Building

```
./gradlew build                                   # jar in build/libs/
./gradlew runClient
./gradlew runGameTestServer                       # every game test
./gradlew runGameTestServer -PnoMekanism -PnoArs  # the same, without the optional mods
```

Java 21. Gradle provisions its own toolchain if the system JDK differs.

## Documentation

- [ROADMAP.md](ROADMAP.md): what is done and what comes next
- [CHANGELOG.md](CHANGELOG.md)
- [SPIKE.md](SPIKE.md): how the drawers join a Storage Drawers network without a mixin
- [STORE.md](STORE.md): the CurseForge and Modrinth page text

## Licence

Code is [MIT](LICENSE). Assets are [All Rights Reserved](LICENSE-ASSETS), except those adapted from
Functional Storage, which stay MIT. Modpacks may include the published jar unmodified.

Storage Drawers and Functional Storage are MIT; parts of this mod are adapted from them, and
[NOTICE](NOTICE) says which.
