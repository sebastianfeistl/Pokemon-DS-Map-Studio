package tileset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TilesetIORoundTripTest {

    @TempDir
    Path dir;

    @Test
    void nonAsciiNamesSurviveSaveAndLoad() throws Exception {
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB), "png", dir.resolve("grass.png").toFile());
        Files.writeString(dir.resolve("grass.mtl"), "newmtl m\nmap_Kd grass.png\n");
        Files.writeString(dir.resolve("grass.obj"), "mtllib grass.mtl\n"
                + "v 0 0 0\nv 1 0 0\nv 1 1 0\nvt 0 0\nvt 1 0\nvt 1 1\nvn 0 0 1\nusemtl m\nf 1/1/1 2/2/1 3/3/1\n");

        Tileset tileset = new Tileset();
        tileset.importTiles(new java.util.ArrayList<>(java.util.List.of(
                new Tile(tileset, new File(dir.toFile(), "grass.obj").getPath()))));
        tileset.getMaterials().get(0).setMaterialName("Pokémon_grass");
        tileset.getMaterials().get(0).setPaletteNameImd("Café_pl");
        tileset.get(0).setPaletteName("Größe");

        String path = dir.resolve("roundtrip.pdsts").toString();
        TilesetIO.writeTilesetToFile(path, tileset);
        assertFalse(new File(path + ".tmp").exists());

        Tileset loaded = TilesetIO.readTilesetFromFile(path);
        assertEquals(1, loaded.size());
        assertEquals("Pokémon_grass", loaded.getMaterials().get(0).getMaterialName());
        assertEquals("Café_pl", loaded.getMaterials().get(0).getPaletteNameImd());
        assertEquals(tileset.getMaterials().get(0).getTextureNameImd(),
                loaded.getMaterials().get(0).getTextureNameImd());
        assertEquals("Größe", loaded.get(0).getPaletteName());

        //Loading and saving again gives the same bytes (loading adds a default smart grid, so start from loaded)
        String again = dir.resolve("again.pdsts").toString();
        TilesetIO.writeTilesetToFile(again, loaded);
        String twice = dir.resolve("twice.pdsts").toString();
        TilesetIO.writeTilesetToFile(twice, TilesetIO.readTilesetFromFile(again));
        assertArrayEquals(Files.readAllBytes(Path.of(again)), Files.readAllBytes(Path.of(twice)));
    }

    @Test
    void failedMoveLeavesNoTemporaryFile() throws Exception {
        //A non-empty folder where the tileset should go makes the move fail
        Path target = dir.resolve("blocked.pdsts");
        Files.createDirectories(target.resolve("inside"));

        assertThrows(java.io.IOException.class, () -> TilesetIO.writeTilesetToFile(target.toString(), new Tileset()));
        assertFalse(new File(target + ".tmp").exists());
        assertTrue(Files.isDirectory(target.resolve("inside")));
    }
}
