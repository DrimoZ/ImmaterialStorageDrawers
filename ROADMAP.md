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
- **Look**: the front of Immaterial Drawers' drawers (its frame, tank and energy front, on Functional
  Storage's fluid drawer shape) in a casing of Storage Drawers' controller stone, the energy cube or the fluid in the tank, the amount on the frame,
  a distinct inventory icon each.
- **Drops keep everything**: a broken drawer keeps its contents and upgrades in the item.
- **Jade and The One Probe** show the full energy, past the 2.1 billion FE of the standard API.
- **Probes** for every content: Jade and The One Probe show energy, fluids, chemicals and Source, on
  drawers and controllers.
- **Source Drawer** (Ars Nouveau, optional), found by relays, the apparatus and sourcelinks, exposed
  by controllers.
- **Chemical Drawers** (Mekanism, optional) in 1x1, 1x2 and 2x2, exposed by controllers.
- **Fluid Drawers 1x2 and 2x2**, each compartment its own tank, on Storage Drawers' 2- and 4-drawer
  screens.
- **Framing** at Storage Drawers' framing table, through its own frame data, on the placed block and
  on its inventory icon; the frame is drawn by a model of ours, not by Storage Drawers' internal model
  code.
- **Seen in a world**: both drawers, framed or not, their fronts, the upgrade screen, the icons, and
  the Jade and The One Probe readings, by day and by night.
- **Configuration** of the base capacities; the upgrade multipliers come from Storage Drawers' own
  config.

## Next

1. **Store page and README**, in the same style as Immaterial Drawers.

## Considered, not planned

- **Fluid lock** (an emptied drawer keeping its fluid assignment, like Storage Drawers' item lock):
  only if players ask.
- **Ownership and security** (Storage Drawers' personal keys): only if players ask.
- **Faster linking.** A drawer placed next to an existing controller joins at the controller's next
  periodic rebuild, up to 5 seconds later. Storage Drawers' own drawers do exactly the same, so this
  stays as it is.
