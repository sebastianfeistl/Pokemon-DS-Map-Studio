package utils.swing;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.HierarchyEvent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

/**
 * Gives every scroll pane a usable scroll step. A JScrollPane whose view is not Scrollable scrolls 1 pixel per
 * wheel notch (and per touchpad event), which feels dead. Installed once; every scroll pane that is shown
 * and still has the default step gets {@link #STEP}. Scroll panes that already set their own step, and
 * views that are Scrollable (lists, tables, text areas), are left alone.
 */
public final class ScrollPaneIncrements {

    public static final int STEP = 16;
    private static boolean installed = false;

    private ScrollPaneIncrements() {
    }

    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            HierarchyEvent e = (HierarchyEvent) event;
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && e.getComponent() instanceof JScrollPane) {
                fix((JScrollPane) e.getComponent());
            }
        }, AWTEvent.HIERARCHY_EVENT_MASK);
    }

    public static void fix(JScrollPane pane) {
        Component view = pane.getViewport().getView();
        if (view == null || view instanceof Scrollable) {
            return;
        }
        fix(pane.getVerticalScrollBar());
        fix(pane.getHorizontalScrollBar());
    }

    private static void fix(JScrollBar bar) {
        if (bar != null && bar.getUnitIncrement() <= 1) {
            bar.setUnitIncrement(STEP);
        }
    }
}
