package tileset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TilesetImportFolderTest {

    @TempDir
    Path dir;

    /** Writes name.png filled with a color derived from the name: tiles compare textures by pixels. */
    private void texture(String name) throws Exception {
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        int argb = 0xFF000000 | name.hashCode();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                img.setRGB(x, y, argb);
            }
        }
        ImageIO.write(img, "png", dir.resolve(name + ".png").toFile());
    }

    private Tile tile(Tileset tileset, String name) throws Exception {
        texture(name);
        Files.writeString(dir.resolve(name + ".mtl"), "newmtl m\nmap_Kd " + name + ".png\n");
        Files.writeString(dir.resolve(name + ".obj"), "mtllib " + name + ".mtl\n"
                + "v 0 0 0\nv 1 0 0\nv 1 1 0\nvt 0 0\nvt 1 0\nvt 1 1\nvn 0 0 1\nusemtl m\nf 1/1/1 2/2/1 3/3/1\n");
        return new Tile(tileset, new File(dir.toFile(), name + ".obj").getPath());
    }

    private ArrayList<Tile> source(String name, int count) throws Exception {
        Tileset src = new Tileset();
        ArrayList<Tile> tiles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tiles.add(tile(src, name));
        }
        return tiles;
    }

    @Test
    void importedTilesGoToTheirSourceFolderAndOldTilesAreUntouched() throws Exception {
        Tileset project = new Tileset();
        project.importTiles(source("a", 2)); // tiles already in the project, no folder
        project.getOrCreatePaletteFolder("Mine");
        project.get(1).setPaletteFolder("Mine");

        project.importTiles(source("b", 3), "Tileset_53_PT_Forest");
        project.importTiles(source("c", 1), "Tileset_14_-_Snow");
        project.importTiles(source("d", 2), "Tileset_53_PT_Forest"); // same source again reuses its folder

        assertEquals(8, project.size());
        assertEquals("", project.get(0).getPaletteFolder());
        assertEquals("Mine", project.get(1).getPaletteFolder());
        for (int i : new int[]{2, 3, 4, 6, 7}) {
            assertEquals("Imported from Tileset 53 PT Forest", project.get(i).getPaletteFolder(), "tile " + i);
        }
        assertEquals("Imported from Tileset 14 - Snow", project.get(5).getPaletteFolder());
        assertNotNull(project.getPaletteFolder("Imported from Tileset 53 PT Forest"));
        // Mine + two import folders; the forest folder was reused, not duplicated
        assertEquals(3, project.getPaletteFolders().stream().filter(f -> !f.getPath().isEmpty()).count());
    }

    @Test
    void foldersSurviveSaveAndReload() throws Exception {
        Tileset project = new Tileset();
        project.importTiles(source("a", 1));
        project.importTiles(source("b", 2), "Snow");
        String path = dir.resolve("p.pdsts").toString();
        TilesetIO.writeTilesetToFile(path, project);
        Tileset loaded = TilesetIO.readTilesetFromFile(path);
        assertEquals(3, loaded.size());
        assertEquals("", loaded.get(0).getPaletteFolder());
        assertEquals("Imported from Snow", loaded.get(1).getPaletteFolder());
        assertEquals("Imported from Snow", loaded.get(2).getPaletteFolder());
    }

    @Test
    void plainImportStillLeavesTilesUnsorted() throws Exception {
        Tileset project = new Tileset();
        project.importTiles(source("a", 2));
        assertEquals("", project.get(0).getPaletteFolder());
        assertNull(project.getPaletteFolder("Anything"));
    }

    @Test
    void collisionDefaultsTravelWithImportedTiles() throws Exception {
        Tileset library = new Tileset();
        library.importTiles(source("a", 1));
        Tile t = library.get(0);
        t.getOrCreateCollisionDefaultGrid(0)[0][0] = 0x80; // layer 0 = Type
        t.getOrCreateCollisionDefaultGrid(1)[0][0] = 0x01; // layer 1 = Collision
        String path = dir.resolve("library.pdsts").toString();
        TilesetIO.writeTilesetToFile(path, library);

        Tileset project = new Tileset();
        project.importTiles(TilesetIO.readTilesetFromFile(path).getTiles(), "library");

        Tile imported = project.get(0);
        assertEquals(0x80, imported.getCollisionDefaults().get(0)[0][0]);
        assertEquals(0x01, imported.getCollisionDefaults().get(1)[0][0]);
        assertEquals("Imported from library", imported.getPaletteFolder());
    }

    @Test
    void importSkipsTilesThatAlreadyExist() throws Exception {
        Tile fromA = tile(new Tileset(), "same");
        Tileset second = new Tileset();
        Tile sameInB = tile(second, "same");
        Tile onlyInB = tile(second, "other");
        Tile repeatInB = tile(second, "other"); // repeated inside one import

        Tileset project = new Tileset();
        assertEquals(0, project.importTilesSkippingDuplicates(new ArrayList<>(List.of(fromA)), "Set A"));
        assertEquals(2, project.importTilesSkippingDuplicates(
                new ArrayList<>(List.of(sameInB, onlyInB, repeatInB)), "Set B"));

        assertEquals(2, project.size());
        assertEquals("Imported from Set A", project.get(0).getPaletteFolder()); // the shared tile stays where it was first filed
        assertEquals("Imported from Set B", project.get(1).getPaletteFolder());
    }

    /** A tile from an OBJ named objName using a texture named texName. */
    private Tile named(Tileset tileset, String objName, String texName) throws Exception {
        texture(texName);
        Files.writeString(dir.resolve(objName + ".mtl"), "newmtl m\nmap_Kd " + texName + ".png\n");
        Files.writeString(dir.resolve(objName + ".obj"), "mtllib " + objName + ".mtl\n"
                + "v 0 0 0\nv 1 0 0\nv 1 1 0\nvt 0 0\nvt 1 0\nvt 1 1\nvn 0 0 1\nusemtl m\nf 1/1/1 2/2/1 3/3/1\n");
        return new Tile(tileset, new File(dir.toFile(), objName + ".obj").getPath());
    }

    @Test
    void tilesAreSortedByTypeFromModelAndTextureNames() throws Exception {
        Tileset src = new Tileset();
        ArrayList<Tile> tiles = new ArrayList<>(List.of(
                named(src, "Tree_snow", "tshadow_lm32"),        // model name says tree
                named(src, "SeaPlateCorner", "asasea_lm25"),     // water
                named(src, "GrassPathBorder", "lgreenp_lm2"),    // path, not grass
                named(src, "Sand_road_l", "nsandp_lm96"),        // sand
                named(src, "RockClimb", "hanger_lm4"),           // cliffs before rocks
                named(src, "7", "allpeak_lm38"),                 // numbered: texture says cliff
                named(src, "Fence", "imped_lm51"),
                named(src, "Mystery", "unknown")));
        Tileset project = new Tileset();
        project.importTilesSkippingDuplicates(tiles, "Source A");
        assertEquals(8, project.sortIntoCategoryFolders());

        assertEquals(TileCategories.TREES, project.get(0).getPaletteFolder());
        assertEquals(TileCategories.WATER, project.get(1).getPaletteFolder());
        assertEquals(TileCategories.PATHS, project.get(2).getPaletteFolder());
        assertEquals(TileCategories.SAND, project.get(3).getPaletteFolder());
        assertEquals(TileCategories.CLIFFS, project.get(4).getPaletteFolder());
        assertEquals(TileCategories.CLIFFS, project.get(5).getPaletteFolder());
        assertEquals(TileCategories.OBJECTS, project.get(6).getPaletteFolder());
        assertEquals(TileCategories.OTHER, project.get(7).getPaletteFolder());
        assertNull(project.getPaletteFolder("Imported from Source A")); // emptied source folder is gone
        // folders only for categories that have tiles, in display order
        assertEquals(List.of(TileCategories.PATHS, TileCategories.TREES, TileCategories.CLIFFS, TileCategories.WATER,
                        TileCategories.SAND, TileCategories.OBJECTS, TileCategories.OTHER),
                project.getPaletteFolders().stream().map(PaletteFolder::getPath).filter(p -> !p.isEmpty()).toList());
    }

    @Test
    void sortingKeepsHandMadeFoldersAndEarlierMoves() throws Exception {
        Tileset src = new Tileset();
        ArrayList<Tile> tiles = new ArrayList<>(List.of(named(src, "Tree", "t1"), named(src, "Tree_b", "t2")));
        Tileset project = new Tileset();
        project.importTilesSkippingDuplicates(tiles, "Source A");
        project.getOrCreatePaletteFolder("My favourites");
        project.sortIntoCategoryFolders();
        project.get(1).setPaletteFolder("My favourites"); // moved by hand
        project.getOrCreatePaletteFolder("Empty on purpose");

        Tileset more = new Tileset();
        project.importTilesSkippingDuplicates(new ArrayList<>(List.of(named(more, "Lake_surface", "lake"))), "Source B");
        assertEquals(1, project.sortIntoCategoryFolders()); // only the new tile

        assertEquals(TileCategories.TREES, project.get(0).getPaletteFolder());
        assertEquals("My favourites", project.get(1).getPaletteFolder());
        assertEquals(TileCategories.WATER, project.get(2).getPaletteFolder());
        assertNotNull(project.getPaletteFolder("Empty on purpose"));
        assertNull(project.getPaletteFolder("Imported from Source B"));
    }
}
