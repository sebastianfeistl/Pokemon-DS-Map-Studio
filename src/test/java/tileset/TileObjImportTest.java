package tileset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TileObjImportTest {

    @TempDir
    Path dir;

    private Tile load(String faces) throws Exception {
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB), "png", dir.resolve("t.png").toFile());
        Files.writeString(dir.resolve("t.mtl"), "newmtl m\nmap_Kd t.png\n");
        // Regular octagon in the XY plane, so any face of up to 8 vertices can use it
        StringBuilder obj = new StringBuilder("mtllib t.mtl\n");
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            obj.append(String.format(java.util.Locale.ROOT, "v %f %f 0\n", Math.cos(a), Math.sin(a)));
            obj.append(String.format(java.util.Locale.ROOT, "vt %f %f\n", 0.5 + Math.cos(a) / 2, 0.5 + Math.sin(a) / 2));
        }
        obj.append("vn 0 0 1\nusemtl m\n").append(faces);
        Files.writeString(dir.resolve("t.obj"), obj.toString());
        return new Tile(new Tileset(), new File(dir.toFile(), "t.obj").getPath());
    }

    @Test
    void triangleAndQuadAreUnchanged() throws Exception {
        Tile t = load("f 1/1/1 2/2/1 3/3/1\nf 1/1/1 3/3/1 4/4/1 5/5/1\n");
        assertEquals(9, t.getVCoordsTri().length);
        assertEquals(12, t.getVCoordsQuad().length);
    }

    @Test
    void pentagonBecomesThreeTriangles() throws Exception {
        Tile t = load("f 1/1/1 2/2/1 3/3/1 4/4/1 5/5/1\n");
        assertEquals(3 * 9, t.getVCoordsTri().length);
        assertEquals(0, t.getVCoordsQuad().length);
    }

    @Test
    void octagonKeepsEveryVertex() throws Exception {
        Tile t = load("f 1/1/1 2/2/1 3/3/1 4/4/1 5/5/1 6/6/1 7/7/1 8/8/1\n");
        float[] v = t.getVCoordsTri();
        assertEquals(6 * 9, v.length);
        // The fan covers all 8 corners: every one appears as a vertex of some triangle
        for (int corner = 0; corner < 8; corner++) {
            double x = Math.cos(Math.PI * 2 * corner / 8), y = Math.sin(Math.PI * 2 * corner / 8);
            boolean found = false;
            for (int i = 0; i < v.length; i += 3) {
                found |= Math.abs(v[i] - x) < 1e-4 && Math.abs(v[i + 1] - y) < 1e-4;
            }
            assertEquals(true, found, "corner " + corner + " missing");
        }
    }
}
