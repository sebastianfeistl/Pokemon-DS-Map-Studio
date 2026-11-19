package editor.smartdrawing;

import java.util.List;
import java.util.function.Predicate;

/**
 * Places the smart grids of the Smart Drawing panel: a header row per folder, then the folder's grids
 * left to right, {@code columns} per row. Collapsed folders keep their header and hide their grids.
 * With one column this is the original single-column list.
 */
public class SmartGridLayout {

    public final int columns;
    public final int cellWidth;
    public final int cellHeight;
    public final int headerHeight;
    /** Top-left corner of each grid; meaningless where {@link #visible} is false. */
    public final int[] x;
    public final int[] y;
    public final boolean[] visible;
    /** Top of the folder header drawn above grid i, or -1 when grid i does not start a folder group. */
    public final int[] headerY;
    public final int height;

    public SmartGridLayout(List<String> folders, Predicate<String> collapsed, int columns,
                           int cellWidth, int cellHeight, int headerHeight) {
        this.columns = Math.max(1, columns);
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.headerHeight = headerHeight;
        int n = folders.size();
        x = new int[n];
        y = new int[n];
        visible = new boolean[n];
        headerY = new int[n];
        int top = 0;
        int column = 0;
        String previous = null;
        for (int i = 0; i < n; i++) {
            String folder = folders.get(i);
            headerY[i] = -1;
            if (previous == null || !previous.equals(folder)) {
                if (column > 0) {
                    top += cellHeight;
                    column = 0;
                }
                headerY[i] = top;
                top += headerHeight;
            }
            previous = folder;
            if (collapsed.test(folder)) {
                continue;
            }
            visible[i] = true;
            x[i] = column * cellWidth;
            y[i] = top;
            column++;
            if (column == this.columns) {
                column = 0;
                top += cellHeight;
            }
        }
        if (column > 0) {
            top += cellHeight;
        }
        height = Math.max(1, top);
    }

    public int width() {
        return columns * cellWidth;
    }

    /** Index of the visible grid at the point, or -1. */
    public int gridAt(int px, int py) {
        for (int i = 0; i < x.length; i++) {
            if (visible[i] && px >= x[i] && px < x[i] + cellWidth && py >= y[i] && py < y[i] + cellHeight) {
                return i;
            }
        }
        return -1;
    }

    /** Index of the first grid of the folder whose header is at the point, or -1. */
    public int headerAt(int py) {
        for (int i = 0; i < headerY.length; i++) {
            if (headerY[i] >= 0 && py >= headerY[i] && py < headerY[i] + headerHeight) {
                return i;
            }
        }
        return -1;
    }
}
