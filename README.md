# Pokémon DS Map Studio

A map editor for Pokémon DS games. You paint maps with tiles, like a 2D tilemap, and the editor turns
them into 3D models the games can use. No 3D modelling needed. It edits your own maps; it can't open
the maps of the official games.

Supported games: Diamond/Pearl, Platinum, HeartGold/SoulSilver, Black/White, Black 2/White 2.

## What this version adds

- Tiles sorted into folders by type, and a tile list you can resize (more or fewer tiles per row).
- Tileset import that skips tiles you already have, keeps their collision settings, and keeps
  same-named textures with different pixels apart.
- A Smart Drawing panel that shows several drawings per row, sharing one resizable splitter with the
  tile list.
- macOS support: a launcher script, sharp screenshots on Retina screens, and clean Smart Drawing
  previews.

## Running

Build from source with JDK 21 or older (Gradle doesn't run on newer JDKs):

```shell
./gradlew jar
java -jar "build/libs/Pokemon DS Map Studio-2.4.jar"
```

On macOS, use `./start-macos.sh` instead. It rebuilds the jar when the sources have changed, then
starts the editor. It needs `libs-macos/jogamp-fat-2.6.0.jar` (jogamp-fat.jar 2.6.0 from jogamp.org),
which fixes a crash of the 3D view on recent macOS versions.

## Notes

- To export `.nsbmd` files, put your own legally obtained `g3dcvtr.exe` in the `converter` folder. It
  can't be distributed here. On macOS and Linux it runs through Wine.
- Map files saved with this version may not open in older versions of the editor (the extra 3D layer
  and export groups add new data to `.pdsmap` files).

## Credits

Based on Pokémon DS Map Studio by Trifindo and the fork by AdAstra-LD.
