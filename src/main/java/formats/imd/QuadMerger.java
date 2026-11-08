package formats.imd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Joins pairs of triangles that form a flat quad into that quad.
 *
 * Many tilesets store their tiles as triangles. On the DS, the two triangles
 * of a quad map the texture slightly differently, so the diagonal between
 * them shimmers while the camera moves. The original maps use quads.
 *
 * Two triangles are joined only if the quad draws the same thing: they share
 * an edge (same position, texture coordinate, color and, for lit materials,
 * normal at both ends), lie in one plane, form a strictly convex quad and map
 * the texture with one affine mapping.
 */
public class QuadMerger {

    private static final float MIN_NORMAL_DOT = 0.99999f;
    private static final float UV_TOLERANCE_TEXELS = 1 / 32f;

    private final PolygonData polygon;
    private final boolean compareNormals;
    private final float uvToleranceU, uvToleranceV;

    /**
     * @param compareNormals false if the material has all lights off, so the
     *                       normals are not used
     * @param texWidth       texture size, for the texture coordinate tolerance
     */
    private QuadMerger(PolygonData polygon, boolean compareNormals, int texWidth, int texHeight) {
        this.polygon = polygon;
        this.compareNormals = compareNormals;
        this.uvToleranceU = UV_TOLERANCE_TEXELS / texWidth;
        this.uvToleranceV = UV_TOLERANCE_TEXELS / texHeight;
    }

    /**
     * Moves the triangles of the polygon that can be joined into quads to its
     * quads. Texture coordinates must still be in OBJ units.
     *
     * @return the number of quads created
     */
    public static int merge(PolygonData polygon, boolean compareNormals, int texWidth, int texHeight) {
        return new QuadMerger(polygon, compareNormals, texWidth, texHeight).merge();
    }

    private int merge() {
        int numTris = polygon.vCoordsTri.length / 9;
        HashMap<List<Float>, ArrayList<Integer>> edges = new HashMap<>();
        for (int i = 0; i < numTris; i++) {
            for (int k = 0; k < 3; k++) {
                edges.computeIfAbsent(edgeKey(i, k, (k + 1) % 3), key -> new ArrayList<>()).add(i);
            }
        }

        boolean[] used = new boolean[numTris];
        ArrayList<int[]> quads = new ArrayList<>(); // {triangle, vertex} pairs
        ArrayList<Integer> left = new ArrayList<>();
        for (int i = 0; i < numTris; i++) {
            if (used[i]) {
                continue;
            }
            int[] quad = null;
            for (int k = 0; k < 3 && quad == null; k++) {
                List<Integer> partners = edges.get(edgeKey(i, (k + 1) % 3, k));
                if (partners == null) {
                    continue;
                }
                for (int j : partners) {
                    if (j != i && !used[j]) {
                        quad = joinTriangles(i, k, j);
                        if (quad != null) {
                            used[i] = true;
                            used[j] = true;
                            break;
                        }
                    }
                }
            }
            if (quad != null) {
                quads.add(quad);
            } else {
                left.add(i);
            }
        }
        if (quads.isEmpty()) {
            return 0;
        }

        int oldQuads = polygon.vCoordsQuad.length / 12;
        float[][] quadData = {polygon.vCoordsQuad, polygon.tCoordsQuad, polygon.nCoordsQuad, polygon.colorsQuad};
        float[][] triData = {polygon.vCoordsTri, polygon.tCoordsTri, polygon.nCoordsTri, polygon.colorsTri};
        int[] sizes = {3, 2, 3, 3};

        polygon.initQuads(oldQuads + quads.size());
        polygon.initTris(left.size());
        float[][] newQuadData = {polygon.vCoordsQuad, polygon.tCoordsQuad, polygon.nCoordsQuad, polygon.colorsQuad};
        float[][] newTriData = {polygon.vCoordsTri, polygon.tCoordsTri, polygon.nCoordsTri, polygon.colorsTri};
        for (int a = 0; a < 4; a++) {
            int n = sizes[a];
            System.arraycopy(quadData[a], 0, newQuadData[a], 0, oldQuads * 4 * n);
            for (int q = 0; q < quads.size(); q++) {
                int[] quad = quads.get(q);
                for (int v = 0; v < 4; v++) {
                    System.arraycopy(triData[a], (quad[2 * v] * 3 + quad[2 * v + 1]) * n,
                            newQuadData[a], ((oldQuads + q) * 4 + v) * n, n);
                }
            }
            for (int t = 0; t < left.size(); t++) {
                System.arraycopy(triData[a], left.get(t) * 3 * n, newTriData[a], t * 3 * n, 3 * n);
            }
        }
        return quads.size();
    }

    /**
     * The quad of triangle a and triangle b, which shares a's edge from vertex
     * k to k + 1, as four {triangle, vertex} pairs; null if they can't be joined.
     */
    private int[] joinTriangles(int a, int k, int b) {
        int p = k, q = (k + 1) % 3, r = (k + 2) % 3;
        int s = -1;
        for (int j = 0; j < 3; j++) {
            if (sameVertex(b, j, a, q) && sameVertex(b, (j + 1) % 3, a, p)) {
                s = (j + 2) % 3;
            }
        }
        if (s == -1) {
            return null;
        }

        float[] na = normal(a), nb = normal(b);
        if (na == null || nb == null || dot(na, nb) < MIN_NORMAL_DOT) {
            return null;
        }

        // Same winding as a: p, s, q, r
        float[][] quad = {pos(a, p), pos(b, s), pos(a, q), pos(a, r)};
        for (int i = 0; i < 4; i++) {
            float[] e1 = sub(quad[(i + 1) % 4], quad[i]);
            float[] e2 = sub(quad[(i + 2) % 4], quad[(i + 1) % 4]);
            if (dot(cross(e1, e2), na) <= 1e-9f) {
                return null;
            }
        }

        // The texture coordinate a's affine mapping gives b's far vertex
        float[] e1 = sub(pos(a, 1), pos(a, 0));
        float[] e2 = sub(pos(a, 2), pos(a, 0));
        float[] d = sub(pos(b, s), pos(a, 0));
        float m11 = dot(e1, e1), m12 = dot(e1, e2), m22 = dot(e2, e2);
        float det = m11 * m22 - m12 * m12;
        if (Math.abs(det) < 1e-12f) {
            return null;
        }
        float r1 = dot(d, e1), r2 = dot(d, e2);
        float u = (r1 * m22 - r2 * m12) / det;
        float v = (r2 * m11 - r1 * m12) / det;
        float[] tolerance = {uvToleranceU, uvToleranceV};
        for (int m = 0; m < 2; m++) {
            float t0 = tex(a, 0)[m];
            float expected = t0 + u * (tex(a, 1)[m] - t0) + v * (tex(a, 2)[m] - t0);
            if (Math.abs(expected - tex(b, s)[m]) > tolerance[m]) {
                return null;
            }
        }
        return new int[]{a, p, b, s, a, q, a, r};
    }

    private List<Float> edgeKey(int tri, int from, int to) {
        ArrayList<Float> key = new ArrayList<>(22);
        for (int i : new int[]{from, to}) {
            for (float[] attribute : vertex(tri, i)) {
                for (float x : attribute) {
                    key.add(x);
                }
            }
        }
        return key;
    }

    private boolean sameVertex(int t1, int i, int t2, int j) {
        return Arrays.deepEquals(vertex(t1, i), vertex(t2, j));
    }

    private float[][] vertex(int tri, int i) {
        float[] color = get(polygon.colorsTri, tri, i, 3);
        if (!compareNormals) {
            return new float[][]{pos(tri, i), tex(tri, i), color};
        }
        return new float[][]{pos(tri, i), tex(tri, i), get(polygon.nCoordsTri, tri, i, 3), color};
    }

    private float[] pos(int tri, int i) {
        return get(polygon.vCoordsTri, tri, i, 3);
    }

    private float[] tex(int tri, int i) {
        return get(polygon.tCoordsTri, tri, i, 2);
    }

    private static float[] get(float[] data, int tri, int i, int n) {
        return Arrays.copyOfRange(data, (tri * 3 + i) * n, (tri * 3 + i + 1) * n);
    }

    private float[] normal(int tri) {
        float[] n = cross(sub(pos(tri, 1), pos(tri, 0)), sub(pos(tri, 2), pos(tri, 0)));
        float length = (float) Math.sqrt(dot(n, n));
        if (length < 1e-9f) {
            return null;
        }
        return new float[]{n[0] / length, n[1] / length, n[2] / length};
    }

    private static float[] sub(float[] a, float[] b) {
        return new float[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }
}
