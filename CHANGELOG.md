# Changelog

## Unreleased

### Added
- Energy Drawer: stores 500,000 FE, exposed to cables on every side. Crafted from smooth stone,
  redstone blocks and any Storage Drawers drawer; listed in the Storage Drawers creative tab. Built
  in Storage Drawers' style (controller stone, 1 px trim rim) around a deep niche where an energy
  cube turns: its core glows and spins faster as the drawer fills. The amount (stored / capacity)
  is written under it.
- A broken Energy Drawer keeps its energy and upgrades in the dropped item and gets them back when
  placed. An empty one drops a plain item that stacks. Its tooltip shows the energy and the number
  of upgrades it holds.
- Energy Drawers take Storage Drawers' own upgrades, 7 slots: storage upgrades multiply the
  capacity exactly as they do for items (from Storage Drawers' config; seven netherite upgrades
  reach about 7.2 billion FE), plus void, creative storage and creative vending. Right-click to
  install; sneak + right-click with an empty hand takes the last one out, unless the energy would
  no longer fit.
- Storage Drawers controllers expose the total energy of the Energy Drawers they link: cables and
  machines on a controller fill and drain the whole network. Totals are summed in 64-bit and
  clamped only for the standard 2.1B FE API. Controller Slaves expose the same total.
- Energy Drawers join a Storage Drawers network like any drawer, without changing the
  controller's item slots.
