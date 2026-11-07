# Changelog

## Unreleased

### Added
- Energy Drawer block (no item, model or recipe yet): stores 500,000 FE, exposed to cables on
  every side.
- Energy Drawers take Storage Drawers' own upgrades, 7 slots: storage upgrades multiply the
  capacity exactly as they do for items (from Storage Drawers' config; seven netherite upgrades
  reach about 7.2 billion FE), plus void, creative storage and creative vending. Right-click to
  install; sneak + right-click with an empty hand takes the last one out, unless the energy would
  no longer fit. Breaking the drawer drops its upgrades.
- Storage Drawers controllers expose the total energy of the Energy Drawers they link: cables and
  machines on a controller fill and drain the whole network. Totals are summed in 64-bit and
  clamped only for the standard 2.1B FE API.
- Energy Drawers join a Storage Drawers network like any drawer, without changing the
  controller's item slots.
