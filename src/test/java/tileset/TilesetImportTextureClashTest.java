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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Built-in tilesets reuse texture file names for different images
 * (file1Material.png is dark rock in one set and light rock in another).
 * Importing must keep both images and must not treat the tiles as duplicates.
 */
class TilesetImportTextureClashTest {

    private static final int RED = 0xFFFF0000;
    private static final int BLUE = 0xFF0000FF;

    @TempDir
    Path dir;

    private int folders;

    /** A one-triangle tile in its own folder (its own source tileset), textured with a single-color image. */
    private Tile tile(String image, int argb, double height) throws Exception {
        Path d = Files.createDirectories(dir.resolve("src" + folders++));
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                img.setRGB(x, y, argb);
            }
        }
        ImageIO.write(img, "png", d.resolve(image).toFile());
        Files.writeString(d.resolve("t.mtl"), "newmtl m\nmap_Kd " + image + "\n");
        Files.writeString(d.resolve("t.obj"), "mtllib t.mtl\n"
                + "v 0 0 0\nv 1 0 0\nv 1 1 " + height + "\nvt 0 0\nvt 1 0\nvt 1 1\nvn 0 0 1\nusemtl m\nf 1/1/1 2/2/1 3/3/1\n");
        return new Tile(new Tileset(), new File(d.toFile(), "t.obj").getPath());
    }

    private static ArrayList<Tile> list(Tile... tiles) {
        return new ArrayList<>(List.of(tiles));
    }

    private static TilesetMaterial material(Tileset tileset, int tile) {
        return tileset.getMaterial(tileset.get(tile).getTextureIDs().get(0));
    }

    private static int color(TilesetMaterial m) {
        return m.getTextureImg().getRGB(0, 0);
    }

    @Test
    void sameNameDifferentPixelsIsImportedUnderANewName() throws Exception {
        Tileset project = new Tileset();
        project.importTilesSkippingDuplicates(list(tile("file1Material.png", RED, 0)), "Dark");
        int skipped = project.importTilesSkippingDuplicates(list(tile("file1Material.png", BLUE, 0)), "Light");

        assertEquals(0, skipped, "same geometry and name, other pixels: not a duplicate");
        assertEquals(2, project.size());
        assertEquals(2, project.getMaterials().size());

        TilesetMaterial dark = material(project, 0);
        TilesetMaterial light = material(project, 1);
        assertEquals("file1Material.png", dark.getImageName());
        assertEquals(RED, color(dark));
        assertEquals("file1Material_2.png", light.getImageName());
        assertEquals(BLUE, color(light));
        assertNotEquals(dark.getTextureNameImd(), light.getTextureNameImd());
        assertNotEquals(dark.getPaletteNameImd(), light.getPaletteNameImd());
        assertTrue(light.getPaletteNameImd().endsWith("_pl"), light.getPaletteNameImd());
    }

    @Test
    void sameNameSamePixelsReusesTheMaterial() throws Exception {
        Tileset project = new Tileset();
        project.importTilesSkippingDuplicates(list(tile("rock.png", RED, 0)), "A");
        int skipped = project.importTilesSkippingDuplicates(
                list(tile("rock.png", RED, 0), tile("rock.png", RED, 1)), "B");

        assertEquals(1, skipped, "identical tile is skipped");
        assertEquals(2, project.size());
        assertEquals(1, project.getMaterials().size(), "no renamed copy for identical pixels");
    }

    @Test
    void laterImportsOfTheRenamedImageReuseTheRenamedCopy() throws Exception {
        Tileset project = new Tileset();
        project.importTiles(list(tile("f.png", RED, 0)), "Dark");
        project.importTiles(list(tile("f.png", BLUE, 0)), "Light");
        project.importTiles(list(tile("f.png", BLUE, 1)), "Light again"); // other geometry, same light image
        project.importTiles(list(tile("f.png", 0xFF00FF00, 2)), "Green"); // a third image with the same name

        assertEquals(List.of("f.png", "f_2.png", "f_2.png", "f_3.png"),
                List.of(material(project, 0).getImageName(), material(project, 1).getImageName(),
                        material(project, 2).getImageName(), material(project, 3).getImageName()));
        assertEquals(3, project.getMaterials().size());
    }

    @Test
    void renamedImageSurvivesSaveAndReload() throws Exception {
        Tileset project = new Tileset();
        project.importTiles(list(tile("file2Material.png", RED, 0)), "Dark");
        project.importTiles(list(tile("file2Material.png", BLUE, 0)), "Light");

        Path out = Files.createDirectories(dir.resolve("out"));
        String path = out.resolve("p.pdsts").toString();
        TilesetIO.writeTilesetToFile(path, project);
        project.saveImagesToFile(out.toString());
        assertTrue(Files.exists(out.resolve("file2Material_2.png")));

        Tileset loaded = TilesetIO.readTilesetFromFile(path);
        assertEquals(RED, color(material(loaded, 0)));
        assertEquals(BLUE, color(material(loaded, 1)));
        assertEquals("file2Material_2.png", material(loaded, 1).getImageName());
    }

    @Test
    void modelKeyComparesTexturesByPixelsNotNames() throws Exception {
        Tile a = tile("criff.png", RED, 0);
        Tile sameImageOtherName = tile("criff_lm44.png", RED, 0);
        Tile sameNameOtherImage = tile("criff.png", BLUE, 0);

        assertEquals(a.modelKey(), sameImageOtherName.modelKey());
        assertNotEquals(a.modelKey(), sameNameOtherImage.modelKey());
    }

    @Test
    void renamedImdNamesFitTheNsbtxLimit() throws Exception {
        Tileset project = new Tileset();
        project.importTiles(list(tile("a_very_long_texture_name.png", RED, 0)), "A");
        project.importTiles(list(tile("a_very_long_texture_name.png", BLUE, 0)), "B");

        TilesetMaterial renamed = material(project, 1);
        assertTrue(renamed.getTextureNameImd().length() <= 16, renamed.getTextureNameImd());
        assertTrue(renamed.getPaletteNameImd().length() <= 16, renamed.getPaletteNameImd());
        assertTrue(renamed.getPaletteNameImd().endsWith("_pl"), renamed.getPaletteNameImd());
        assertNotEquals(material(project, 0).getTextureNameImd(), renamed.getTextureNameImd());
    }
}
