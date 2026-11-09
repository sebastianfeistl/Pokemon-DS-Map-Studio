package formats.imd;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuadMergerTest {

    /** Two triangles over the unit square, optionally lifting one corner out of plane or shifting a UV. */
    private static PolygonData square(float liftZ, float uShift) {
        PolygonData p = new PolygonData();
        p.initQuads(0);
        p.initTris(2);
        // Triangle 0: (0,0) (1,0) (1,1); triangle 1: (0,0) (1,1) (0,1)
        float[][] pos = {{0, 0, 0}, {1, 0, 0}, {1, 1, liftZ}, {0, 0, 0}, {1, 1, liftZ}, {0, 1, 0}};
        float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 0}, {1 + uShift, 1}, {0, 1}};
        for (int v = 0; v < 6; v++) {
            System.arraycopy(pos[v], 0, p.vCoordsTri, v * 3, 3);
            System.arraycopy(uv[v], 0, p.tCoordsTri, v * 2, 2);
            p.nCoordsTri[v * 3 + 2] = 1;
            p.colorsTri[v * 3] = p.colorsTri[v * 3 + 1] = p.colorsTri[v * 3 + 2] = 1;
        }
        return p;
    }

    @Test
    void flatSquareBecomesOneQuad() {
        PolygonData p = square(0, 0);
        assertEquals(1, QuadMerger.merge(p, true, 32, 32));
        assertEquals(12, p.vCoordsQuad.length);
        assertEquals(0, p.vCoordsTri.length);
    }

    @Test
    void bentPairStaysTriangles() {
        PolygonData p = square(0.5f, 0);
        assertEquals(0, QuadMerger.merge(p, false, 32, 32));
        assertEquals(18, p.vCoordsTri.length);
        assertEquals(0, p.vCoordsQuad.length);
    }

    @Test
    void mismatchedTextureCoordsStayTriangles() {
        PolygonData p = square(0, 0.5f);
        assertEquals(0, QuadMerger.merge(p, false, 32, 32));
        assertEquals(18, p.vCoordsTri.length);
    }
}
