package editor.mapdisplay;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects many small images and draws them as one image. With the macOS Metal pipeline, drawing
 * many small images one by one garbles some of them (black or stale pixels, magenta blocks);
 * drawing one composed image doesn't.
 */
class ImageBatch {

    private static class Entry {
        final Image image;
        final int x, y;

        Entry(Image image, int x, int y) {
            this.image = image;
            this.x = x;
            this.y = y;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private Rectangle bounds;

    void add(Image image, int x, int y) {
        if (image == null) {
            return;
        }
        int w = image.getWidth(null), h = image.getHeight(null);
        if (w <= 0 || h <= 0) {
            return;
        }
        entries.add(new Entry(image, x, y));
        Rectangle r = new Rectangle(x, y, w, h);
        bounds = bounds == null ? r : bounds.union(r);
    }

    /** Draws the collected images, in the order added, as one image on g (with g's transform and composite). */
    void draw(Graphics g) {
        if (bounds == null) {
            return;
        }
        BufferedImage buf = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D bg = buf.createGraphics();
        for (Entry e : entries) {
            bg.drawImage(e.image, e.x - bounds.x, e.y - bounds.y, null);
        }
        bg.dispose();
        g.drawImage(buf, bounds.x, bounds.y, null);
    }
}
