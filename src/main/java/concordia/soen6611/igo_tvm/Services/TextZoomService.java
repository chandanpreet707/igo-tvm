package concordia.soen6611.igo_tvm.Services;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;

import java.util.*;
import java.util.prefs.Preferences;

public class TextZoomService {
    // Singleton (or you can new it per screen if you prefer)
    private static final TextZoomService INSTANCE = new TextZoomService();
    public static TextZoomService get() { return INSTANCE; }

    // Clamp & step
    private static final double MIN = 1.00;
    private static final double MAX = 1.50;
    private static final double STEP = 0.10;

    // Persist scale if you want it remembered across screens/runs
    private final Preferences prefs = Preferences.userRoot().node("igo-tvm/text-zoom");
    private double scale = clamp(prefs.getDouble("scale", 1.00));

    // Baselines for each registered node (font size at 100%)
    private final WeakHashMap<Object, Double> baseline = new WeakHashMap<>();
    // The nodes we want to control
    private final Set<Node> registry = Collections.newSetFromMap(new WeakHashMap<>());

    private TextZoomService() {}

    // ---- Register targets ----
    public TextZoomService register(Node... nodes) {
        for (Node n : nodes) {
            if (n != null) {
                registry.add(n);
                cacheBaseline(n);
            }
        }
        apply(); // apply current scale to newly-registered nodes
        return this;
    }

    /** Register all Labeled/TextInputControl under a container (e.g., your root) */
    public TextZoomService registerAll(Parent root) {
        if (root == null) return this;
        Deque<Node> q = new ArrayDeque<>();
        q.add(root);
        while (!q.isEmpty()) {
            Node n = q.removeFirst();
            if (isTextNode(n)) {
                registry.add(n);
                cacheBaseline(n);
            }
            if (n instanceof Parent p) q.addAll(p.getChildrenUnmodifiable());
        }
        apply();
        return this;
    }

    /** Register nodes by style class (e.g., ".tile-title", ".prompt") */
    public TextZoomService registerByStyleClass(Parent root, String styleClass) {
        if (root == null || styleClass == null || styleClass.isEmpty()) return this;
        Deque<Node> q = new ArrayDeque<>();
        q.add(root);
        while (!q.isEmpty()) {
            Node n = q.removeFirst();
            if (n.getStyleClass().contains(styleClass.replaceFirst("^\\.", "")) && isTextNode(n)) {
                registry.add(n);
                cacheBaseline(n);
            }
            if (n instanceof Parent p) q.addAll(p.getChildrenUnmodifiable());
        }
        apply();
        return this;
    }

    public TextZoomService unregister(Node... nodes) {
        registry.removeAll(Arrays.asList(nodes));
        return this;
    }

    public void clear() {
        registry.clear();
        baseline.clear();
    }

    // ---- Zoom controls ----
    public double getScale() { return scale; }
    public void zoomIn()  { setScale(scale + STEP); }
    public void zoomOut() { setScale(scale - STEP); }
    public void reset()   { setScale(1.00); }

    public void setScale(double s) {
        scale = clamp(s);
        prefs.putDouble("scale", scale);
        apply();
    }

    // ---- Apply to all registered nodes ----
    public void apply() {
        for (Node n : registry) {
            if (n == null) continue;
            if (n instanceof Labeled lbl) {
                double base = baseline.computeIfAbsent(lbl, k -> lbl.getFont().getSize());
                setInlineFontSize(lbl, base * scale);
            } else if (n instanceof TextInputControl tic) {
                double base = baseline.computeIfAbsent(tic, k -> tic.getFont().getSize());
                setInlineFontSize(tic, base * scale);
            }
        }
    }

    // ---- helpers ----
    private static boolean isTextNode(Node n) {
        return (n instanceof Labeled) || (n instanceof TextInputControl);
    }

    private void cacheBaseline(Node n) {
        if (n instanceof Labeled lbl) {
            baseline.putIfAbsent(lbl, lbl.getFont().getSize());
        } else if (n instanceof TextInputControl tic) {
            baseline.putIfAbsent(tic, tic.getFont().getSize());
        }
    }

    private static double clamp(double v) { return Math.max(MIN, Math.min(MAX, v)); }

    private static void setInlineFontSize(Labeled node, double px) {
        String s = node.getStyle(); if (s == null) s = "";
        s = s.replaceAll("(?i)-fx-font-size\\s*:\\s*[^;]+;?", "");
        node.setStyle(s + String.format("-fx-font-size: %.1fpx;", px));
    }
    private static void setInlineFontSize(TextInputControl node, double px) {
        String s = node.getStyle(); if (s == null) s = "";
        s = s.replaceAll("(?i)-fx-font-size\\s*:\\s*[^;]+;?", "");
        node.setStyle(s + String.format("-fx-font-size: %.1fpx;", px));
    }
}
