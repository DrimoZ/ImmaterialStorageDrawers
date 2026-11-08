# Roadmap

Ordered, not scheduled. Nothing here is promised until it has run in a game test.

## Done

- **Storage Drawers networks.** Energy and Fluid Drawers join a controller's network like any
  drawer, with no mixin, and controllers and Controller Slaves expose the network's energy and
  fluids to cables and pipes. See `SPIKE.md`.
- **Energy Drawer** (500,000 FE base, stored in 64-bit) and **Fluid Drawer** (one fluid,
  32,000 mB base).
- **Storage Drawers' own upgrades**: storage tiers, void, creative storage, creative vending, in 7
  slots, through Storage Drawers' own drawer screen.
- **Look**: Storage Drawers' frame and controller stone around a deep niche, the energy cube or the
  fluid inside, the amount on the front, a distinct inventory icon each.
- **Drops keep everything**: a broken drawer keeps its contents and upgrades in the item.
- **Jade and The One Probe** show the full energy, past the 2.1 billion FE of the standard API.

## Next

1. **Looked at in a world.** Every render (both drawers, the screen, the icons, the probe bars) has
   only been checked to load without errors, never seen. Orientation, light and readability come
   first, before anything is added on top.
2. **Framed variants.** Storage Drawers' framing table accepts any block that offers itself through
   its public `IFramedSourceBlock`, but rendering a framed block goes through Storage Drawers'
   internal model code, a much heavier dependency than anything else here. To be weighed first.
3. **Configuration.** Base capacities (500,000 FE, 32,000 mB) are constants; the upgrade
   multipliers already come from Storage Drawers' own config.
4. **Store page and README**, in the same style as Immaterial Drawers.

## Considered, not planned

- **Fluid lock** (an emptied drawer keeping its fluid assignment, like Storage Drawers' item lock):
  only if players ask.
- **Ownership and security** (Storage Drawers' personal keys): only if players ask.
- **Faster linking.** A drawer placed next to an existing controller joins at the controller's next
  periodic rebuild, up to 5 seconds later. Storage Drawers' own drawers do exactly the same, so this
  stays as it is.
- **Other contents** (Mekanism chemicals, Ars Nouveau Source): not planned for this mod.
