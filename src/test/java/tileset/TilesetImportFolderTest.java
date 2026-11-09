package tileset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TilesetImportFolderTest {

    @TempDir
    Path dir;

    private Tile tile(Tileset tileset, String name) throws Exception {
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB), "png", dir.resolve(name + ".png").toFile());
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
            assertEquals("Tileset 53 PT Forest", project.get(i).getPaletteFolder(), "tile " + i);
        }
        assertEquals("Tileset 14 - Snow", project.get(5).getPaletteFolder());
        assertNotNull(project.getPaletteFolder("Tileset 53 PT Forest"));
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
        assertEquals("Snow", loaded.get(1).getPaletteFolder());
        assertEquals("Snow", loaded.get(2).getPaletteFolder());
    }

    @Test
    void plainImportStillLeavesTilesUnsorted() throws Exception {
        Tileset project = new Tileset();
        project.importTiles(source("a", 2));
        assertEquals("", project.get(0).getPaletteFolder());
        assertNull(project.getPaletteFolder("Anything"));
    }
}
