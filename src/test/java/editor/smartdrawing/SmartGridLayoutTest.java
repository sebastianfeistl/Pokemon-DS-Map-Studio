package editor.smartdrawing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class SmartGridLayoutTest {

    private static final int W = 80, H = 48, HEADER = 16;

    private SmartGridLayout layout(List<String> folders, int columns, String collapsed) {
        return new SmartGridLayout(folders, f -> f.equals(collapsed), columns, W, H, HEADER);
    }

    @Test
    void oneColumnStacksGridsUnderFolderHeaders() {
        SmartGridLayout l = layout(Arrays.asList("a", "a", "b"), 1, null);
        assertEquals(HEADER, l.y[0]);
        assertEquals(HEADER + H, l.y[1]);
        assertEquals(HEADER + 2 * H + HEADER, l.y[2]);
        assertEquals(2 * HEADER + 3 * H, l.height);
        assertEquals(0, l.x[1]);
        assertEquals(1, l.gridAt(5, HEADER + H + 3));
        assertEquals(-1, l.gridAt(W, HEADER + H + 3));
    }

    @Test
    void severalColumnsFillRowsLeftToRight() {
        SmartGridLayout l = layout(Arrays.asList("a", "a", "a", "a", "a"), 2, null);
        assertEquals(0, l.x[0]);
        assertEquals(W, l.x[1]);
        assertEquals(0, l.x[2]);
        assertEquals(HEADER + H, l.y[2]);
        assertEquals(HEADER + 2 * H, l.y[4]);
        assertEquals(HEADER + 3 * H, l.height);
        assertEquals(3, l.gridAt(W + 1, HEADER + H + 1));
        assertEquals(-1, l.gridAt(W + 1, HEADER + 2 * H + 1)); // empty slot after grid 4
    }

    @Test
    void newFolderStartsOnItsOwnRow() {
        SmartGridLayout l = layout(Arrays.asList("a", "b"), 3, null);
        assertEquals(0, l.headerAt(1));
        assertEquals(HEADER + H, l.headerY[1]);
        assertEquals(0, l.x[1]);
        assertEquals(1, l.headerAt(HEADER + H + 2));
    }

    @Test
    void collapsedFolderHidesGridsButKeepsHeader() {
        SmartGridLayout l = layout(Arrays.asList("a", "a", "b"), 2, "a");
        assertEquals(false, l.visible[0]);
        assertEquals(false, l.visible[1]);
        assertEquals(HEADER, l.headerY[2]);
        assertEquals(2, l.gridAt(3, 2 * HEADER + 1));
        assertEquals(2 * HEADER + H, l.height);
    }
}
