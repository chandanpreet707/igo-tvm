package concordia.soen6611.igo_tvm.Services;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;

import java.util.*;
import java.util.prefs.Preferences;

/**
 * Service to manage dynamic text zoom across JavaFX nodes.
 * <p>
 * This singleton tracks a set of target text nodes ({@link Labeled} and {@link TextInputControl}),
 * remembers their baseline font sizes, and applies a scalable inline font-size style to each.
 * The current scale is persisted via {@link Preferences} so it can be shared across screens/runs.
 * </p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Register nodes (typically in a controller's initialize):
 * TextZoomService.get().register(titleLabel, subtitleLabel, inputField);
 *
 * // Or register all text nodes under a container:
 * TextZoomService.get().registerAll(root);
 *
 * // Adjust zoom:
 * TextZoomService.get().zoomIn();
 * TextZoomService.get().zoomOut();
 * TextZoomService.get().reset();
 * }</pre>
 *
 * <h3>Threading</h3>
 * All JavaFX node mutations should be performed on the JavaFX Application Thread.
 */
public class TextZoomService {
    // Singleton (or you can new it per screen if you prefer)
    private static final TextZoomService INSTANCE = new TextZoomService();

    /**
     * Returns the singleton instance of {@code TextZoomService}.
     *
     * @return global {@code TextZoomService} instance
     */
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

    /**
     * Registers one or more nodes for zoom control, caching their baseline font sizes
     * the first time they are seen. Immediately applies the current scale to new nodes.
     *
     * @param nodes nodes to register; {@code null} entries are ignored
     * @return this service (for chaining)
     */
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

    /**
     * Recursively registers all {@link Labeled} and {@link TextInputControl} descendants
     * of the provided root. Immediately applies the current scale to all found nodes.
     *
     * @param root container whose subtree will be scanned (no-op if {@code null})
     * @return this service (for chaining)
     */
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

    /**
     * Recursively registers all text nodes that contain the specified CSS style class,
     * then applies the current scale.
     *
     * @param root       container to search (no-op if {@code null})
     * @param styleClass style class to match; may be prefixed with '.' (e.g., ".prompt")
     * @return this service (for chaining)
     */
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

    /**
     * Unregisters the specified nodes from zoom control.
     *
     * @param nodes nodes to remove
     * @return this service (for chaining)
     */
    public TextZoomService unregister(Node... nodes) {
        registry.removeAll(Arrays.asList(nodes));
        return this;
    }

    /**
     * Clears all registered nodes and baseline caches.
     * Useful when tearing down or resetting the service.
     */
    public void clear() {
        registry.clear();
        baseline.clear();
    }

    // ---- Zoom controls ----

    /**
     * Returns the current zoom scale.
     *
     * @return scale in the range [{@value MIN}, {@value MAX}]
     */
    public double getScale() { return scale; }

    /**
     * Increases the zoom scale by {@value STEP}, clamps to the max, persists,
     * and applies to all registered nodes.
     */
    public void zoomIn()  { setScale(scale + STEP); }

    /**
     * Decreases the zoom scale by {@value STEP}, clamps to the min, persists,
     * and applies to all registered nodes.
     */
    public void zoomOut() { setScale(scale - STEP); }

    /**
     * Resets the zoom scale to 1.00 (baseline), persists, and applies to all registered nodes.
     */
    public void reset()   { setScale(1.00); }

    /**
     * Sets the zoom scale, clamped to the allowed range, persists the value,
     * and applies it to all registered nodes.
     *
     * @param s desired scale
     */
    public void setScale(double s) {
        scale = clamp(s);
        prefs.putDouble("scale", scale);
        apply();
    }

    // ---- Apply to all registered nodes ----

    /**
     * Applies the current scale to all registered nodes by setting an inline
     * {@code -fx-font-size} style based on each node's cached baseline font size.
     */
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

    /**
     * Determines whether a node is a supported text node type.
     *
     * @param n node to test
     * @return {@code true} if {@link Labeled} or {@link TextInputControl}
     */
    private static boolean isTextNode(Node n) {
        return (n instanceof Labeled) || (n instanceof TextInputControl);
    }

    /**
     * Caches the current font size of a node as its baseline (100%) size.
     *
     * @param n node whose baseline font size will be stored
     */
    private void cacheBaseline(Node n) {
        if (n instanceof Labeled lbl) {
            baseline.putIfAbsent(lbl, lbl.getFont().getSize());
        } else if (n instanceof TextInputControl tic) {
            baseline.putIfAbsent(tic, tic.getFont().getSize());
        }
    }

    /**
     * Clamps a value to the inclusive range [{@value MIN}, {@value MAX}].
     *
     * @param v raw scale value
     * @return clamped scale
     */
    private static double clamp(double v) { return Math.max(MIN, Math.min(MAX, v)); }

    /**
     * Applies an inline {@code -fx-font-size} style to a {@link Labeled} node.
     * Any existing {@code -fx-font-size} declarations in the inline style are removed first.
     *
     * @param node labeled node to style
     * @param px   target font size in pixels
     */
    private static void setInlineFontSize(Labeled node, double px) {
        String s = node.getStyle(); if (s == null) s = "";
        s = s.replaceAll("(?i)-fx-font-size\\s*:\\s*[^;]+;?", "");
        node.setStyle(s + String.format("-fx-font-size: %.1fpx;", px));
    }

    /**
     * Applies an inline {@code -fx-font-size} style to a {@link TextInputControl}.
     * Any existing {@code -fx-font-size} declarations in the inline style are removed first.
     *
     * @param node text input control to style
     * @param px   target font size in pixels
     */
    private static void setInlineFontSize(TextInputControl node, double px) {
        String s = node.getStyle(); if (s == null) s = "";
        s = s.replaceAll("(?i)-fx-font-size\\s*:\\s*[^;]+;?", "");
        node.setStyle(s + String.format("-fx-font-size: %.1fpx;", px));
    }
}
