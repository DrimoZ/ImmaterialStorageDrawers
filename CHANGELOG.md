# Changelog

## Unreleased

### Added
- Storage Drawers' redstone upgrades (combined, max, min) on every drawer: a comparator reads how
  full it is, compartments counted as Storage Drawers counts its drawers, and its "analogOutput"
  setting is followed. The illumination upgrade lights the contents, at the level set in Storage
  Drawers' config.
- Storage Drawers' indicator icons on the front: claim (Personal Key), lock and claim together,
  void, priority, shroud and suspend, in Storage Drawers' places and order.

- Storage Drawers' keys work on every drawer: the Drawer Key locks fluid and chemical compartments to
  what they hold (a padlock on the front; an emptied compartment keeps it, shown as a ghost); Quantify and Shroud hide the
  amount or the contents; the Priority Key orders how controllers fill drawers and the Suspend Key
  holds a drawer back from them; the Personal Key keeps other players out.
- A framed drawer's inventory icon wears its frame, as the placed block does.
- Probes: Jade and The One Probe show a bar per chemical compartment and the Source of Source
  Drawers, on drawers and on controllers (their whole network). On these blocks Jade's chemical bars
  are ours, not Mekanism's, whose text was lost on pale chemicals.
- Source Drawer (with Ars Nouveau installed, optional): 32,000 Source base (configurable), Storage
  Drawers' upgrades included. Relays and turrets reach it through Ars's Source capability; the
  enchanting apparatus, imbuement and sourcelinks find it nearby like a Source Jar; controllers expose
  the Source of their whole network. Without Ars Nouveau, nothing of this is loaded.
- Chemical Drawers (with Mekanism installed, optional): 1x1, 1x2 and 2x2, one Mekanism chemical (gas,
  infuse type, pigment or slurry) per compartment, 32,000 mB base (configurable), Storage Drawers'
  upgrades included. Tubes and machines reach them on every side; a chemical tank in hand empties
  into, or fills from, the compartment it is used on; controllers expose every chemical compartment
  of their network. Gases glow in their tank. Without Mekanism, nothing of this is loaded.
- Fluid Drawers 1x2 and 2x2: two or four compartments, one fluid each, each holding half or a quarter
  of a 1x1 (as Storage Drawers divides its drawers). A bucket fills or empties the compartment it is
  used on; pipes and controllers fill compartments already holding that fluid first. Their upgrade
  screen is Storage Drawers' own screen for 2 or 4 drawers, with each compartment's fluid in its slot.
- The upgrade screen gives a compartment's capacity in its field, as Storage Drawers' screen does,
  and the exact amount when a slot is hovered.
- Framing: Storage Drawers' framing table frames Energy and Fluid Drawers like its own drawers
  (side, trim and front materials) and takes the frame off again. A framed drawer keeps its
  contents and upgrades through framing, breaking and placing.
- Configuration (`immaterialstorage_sd-common.toml`): the base capacity of each drawer, before
  storage upgrades. The upgrade multipliers stay in Storage Drawers' own config. Lowering a base
  never deletes anything: a drawer already holding more keeps it all and only refuses more.
- Fluid Drawer: holds one fluid, 32,000 mB (32 buckets) base, sized by the same Storage Drawers
  upgrades (void and creative included). Buckets and fluid containers fill and empty it by hand;
  pipes reach it on every side. Crafted from smooth stone, buckets and any Storage Drawers drawer.
  The fluid stands in the tank as high as the drawer is full, with the amount written below.
- Storage Drawers controllers and Controller Slaves expose the Fluid Drawers they link as one
  multi-tank handler, filling drawers that already hold a fluid before empty ones.
- Energy Drawer: stores 500,000 FE, exposed to cables on every side. Crafted from smooth stone,
  redstone blocks and any Storage Drawers drawer; listed in the Storage Drawers creative tab. Its front
  is Immaterial Drawers' Energy Drawer, in a casing of Storage Drawers' controller stone: a deep tank where an
  energy cube turns, its core glowing and spinning faster as the drawer fills. The amount (stored /
  capacity) is written on the frame under it, lit day and night.
- Each drawer has its own inventory icon: the energy cube in its tank for the Energy Drawer,
  water in its tank for the Fluid Drawer.
- A broken Energy Drawer keeps its energy and upgrades in the dropped item and gets them back when
  placed. An empty one drops a plain item that stacks. Its tooltip shows the energy and the number
  of upgrades it holds.
- Energy Drawers take Storage Drawers' own upgrades, 7 slots: storage upgrades multiply the
  capacity exactly as they do for items (from Storage Drawers' config; seven netherite upgrades
  reach about 7.2 billion FE), plus void, creative storage and creative vending. Right-click to
  install, or sneak + right-click with an empty hand to open the upgrade screen (Storage Drawers'
  own drawer screen: what the drawer holds sits in its slot as an item would, the fluid's texture
  or the energy cube turning, with the amount beside it). An upgrade the contents depend on cannot be
  taken out. Fluid Drawers take the same upgrades. Which upgrades fit together follows Storage
  Drawers' own rule, read from Storage Drawers itself: a drawer takes creative storage or creative
  vending, not both.
- Amounts on drawer fronts and in the upgrade screen are written by Storage Drawers' own count
  formatter (12.3K, 345M, 1.2G), so they read like the counts on its drawers.
- Storage Drawers controllers expose the total energy of the Energy Drawers they link: cables and
  machines on a controller fill and drain the whole network. Totals are summed in 64-bit and
  clamped only for the standard 2.1B FE API. Controller Slaves expose the same total.
- Jade and The One Probe show the real energy of Energy Drawers, controllers and Controller
  Slaves, past the 2.1 billion FE the standard energy API can express, in their own energy bar.
  Controllers without Energy Drawers show no empty bar. Both mods stay optional.
- Energy Drawers join a Storage Drawers network like any drawer, without changing the
  controller's item slots.
