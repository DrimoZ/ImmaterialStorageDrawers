# Changelog

## Unreleased

### Added
- Energy Drawer block (no item, model or recipe yet): stores 500,000 FE, exposed to cables on
  every side.
- Storage Drawers controllers expose the total energy of the Energy Drawers they link: cables and
  machines on a controller fill and drain the whole network. Totals are summed in 64-bit and
  clamped only for the standard 2.1B FE API.
- Energy Drawers join a Storage Drawers network like any drawer, without changing the
  controller's item slots.
