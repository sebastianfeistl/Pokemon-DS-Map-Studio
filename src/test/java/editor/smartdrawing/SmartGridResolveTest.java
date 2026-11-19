package editor.smartdrawing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SmartGridResolveTest {

    /** Slot i of the 5x3 grid holds tile id 100 + i, so a resolved id tells which slot was used. */
    private static SmartGrid numberedGrid() {
        int[][] data = new int[SmartGrid.width][SmartGrid.height];
        for (int i = 0; i < SmartGrid.width * SmartGrid.height; i++) {
            data[i % SmartGrid.width][i / SmartGrid.width] = 100 + i;
        }
        return new SmartGrid(data);
    }

    private static boolean[][] mask(String... rows) {
        boolean[][] m = new boolean[rows[0].length()][rows.length];
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                m[x][y] = rows[y].charAt(x) == '#';
            }
        }
        return m;
    }

    @Test
    void plusJunctionCentreGetsTheCentreTile() {
        int[][] r = numberedGrid().resolveMask(mask(
                ".#.",
                "###",
                ".#."), false);
        assertEquals(106, r[1][1], "centre of a + junction must use the centre slot (6), not an outer corner");
    }

    @Test
    void filledBlockStillUsesCentreAndOuterCorners() {
        int[][] r = numberedGrid().resolveMask(mask(
                "###",
                "###",
                "###"), false);
        assertEquals(106, r[1][1]);
        // Mask y grows upwards (map rows), so y = 0 is the bottom row: bottom-left is slot 10.
        assertEquals(100, r[0][2]);
        assertEquals(110, r[0][0]);
    }
}
