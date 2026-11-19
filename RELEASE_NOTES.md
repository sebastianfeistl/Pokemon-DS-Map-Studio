Builds on PDSMS v2.4 [Unofficial] by AdAstra-LD, based on Trifindo's Pokémon DS Map Studio. Please credit
Trifindo and AdAstra-LD if this tool helped your project.

## Downloads

- **macOS:** `Pokemon.DS.Map.Studio-2.5-macOS.zip`. Unzip it and double-click `Pokemon DS Map Studio.command`
  (the first time, right-click it and choose Open, since the script isn't signed). Needs Java 11 or newer,
  e.g. from adoptium.net. It includes JOGL 2.6.0, which fixes the 3D view crash on recent macOS versions.
- **Windows:** `Pokemon.DS.Map.Studio-2.5-Windows.zip`, with Java included. Start `Pokemon DS Map Studio.exe`.
- **Any system:** `Pokemon.DS.Map.Studio-2.5-Portable.zip`, needs Java 11 or newer.

For `.nsbmd` export, put your own legally obtained `g3dcvtr.exe` into the `converter` folder. On macOS and
Linux it runs through Wine.

## What's new in 2.5

### macOS
- Starts and runs on recent macOS versions (JOGL 2.6.0), with a double-clickable start script.
- Clean Smart Drawing previews and drag previews: no more stray pixels or pink blocks, which the macOS
  graphics pipeline produced when drawing many small images.
- Editor background screenshots are captured at full resolution on Retina screens.
- `.nsbmd` and `.nsbtx` export through Wine works from any folder.
- If Wine is missing, the export now says so (instead of claiming that g3dcvtr.exe is missing).

### Tiles and tilesets
- Tiles can be sorted into folders by type.
- Tileset import skips tiles you already have, keeps their collision defaults, files new tiles under a folder
  named after the source tileset, and keeps same-named textures with different pixels apart.
- OBJ tiles with faces of more than 4 vertices import correctly (they are split into triangles).

### Layout
- The tile list can be resized: drag strips on both sides change the tiles per row.
- The Smart Drawing panel shows several drawings per row and shares one splitter with the tile list.
  Touchpad scrolling works there.

### Export
- IMD export merges triangle pairs into quads, so models have fewer polygons.

### Saving and performance
- Saving is much faster: a large tileset (1,280 tiles) now saves in a few milliseconds instead of about
  2 seconds, and loads about twice as fast.
- Tilesets are written to a temporary file first, so a crash during saving can't destroy the old one.
- Save errors (full disk, missing permissions, unexpected errors) are now shown instead of failing silently.
- Saving only cleans up old per-map files of the map being saved, never those of another map in the same
  folder.
- Tile and material names with non-ASCII characters (é, ü, ...) are stored correctly.
- The top-down map view only redraws when the mouse moves to another tile.

## Before you use
- Maps resaved with this version are not compatible with vanilla PDSMS or older PDSMS versions (the extra
  3D layer and export groups add data to `.pdsmap` files). Tile folders live in the `.meta` file next to
  your tileset. **Back up your maps** before opening them in a new version.

REQUIREMENTS: Java 11 or newer (included in the Windows download).

*Pokemon is a trademark of Nintendo, Creatures Inc., and GAME FREAK inc. This is an unofficial community project, not affiliated with or endorsed by those companies.*
