package concordia.soen6611.igo_tvm.Services;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;
import javafx.scene.text.Font;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.WeakHashMap;
import java.util.prefs.Preferences;

/** Manages High Contrast + Text-only zoom for the current screen. */
public final class AccessibilityService {
    private static final AccessibilityService INSTANCE = new AccessibilityService();
    public static AccessibilityService get() { return INSTANCE; }

    private final Preferences prefs = Preferences.userRoot().node("igo-tvm/a11y");

    private Scene scene;
    private Parent root;

    // ---- High contrast state
    private boolean highContrast = prefs.getBoolean("highContrast", false);

    // ---- Text-only zoom state
    private static final double MIN_SCALE = 1.00;   // 100% = default
    private static final double MAX_SCALE = 1.40;   // cap at +40%
    private static final double STEP      = 0.08;   // 8% per click
    private double textScale = clamp(prefs.getDouble("textScale", 1.00), MIN_SCALE, MAX_SCALE);

    // store each node's baseline font size so scaling is relative and reversible
    private final WeakHashMap<Object, Double> baselineFont = new WeakHashMap<>();

    private AccessibilityService() {}

    /** Attach once per screen after Scene is available. */
    public void attach(Scene scene, Parent root) {
        this.scene = scene;
        this.root  = root;
        // apply current preferences
        applyHighContrast(highContrast);
        applyTextScale(textScale, /*rebuildBaseline*/ true);
    }

    // ---------------- High Contrast ----------------
    public boolean isHighContrast() { return highContrast; }
    public void toggleHighContrast() { applyHighContrast(!highContrast); }

    public void applyHighContrast(boolean on) {
        highContrast = on; prefs.putBoolean("highContrast", on);
        if (scene == null) return;
        final String uri = resource("/a11y/a11y-contrast.css");
        scene.getStylesheets().remove(uri);
        if (on) scene.getStylesheets().add(uri);
        if (root != null) {
            root.getStyleClass().remove("a11y-contrast-on");
            if (on) root.getStyleClass().add("a11y-contrast-on");
        }
    }

    // ---------------- Text-only Zoom ----------------
    public double getTextScale() { return textScale; }
    public void zoomTextIn()  { setTextScale(textScale + STEP); }
    public void zoomTextOut() { setTextScale(textScale - STEP); }
    public void resetTextScale() { setTextScale(1.00); }

    private void setTextScale(double s) {
        s = clamp(s, MIN_SCALE, MAX_SCALE);
        applyTextScale(s, /*rebuildBaseline*/ false);
    }

    private void applyTextScale(double scale, boolean rebuildBaseline) {
        textScale = scale;
        prefs.putDouble("textScale", textScale);
        if (root == null) return;

        // BFS traversal; collect text-bearing controls and apply scale
        Deque<javafx.scene.Node> q = new ArrayDeque<>();
        q.add(root);
        while (!q.isEmpty()) {
            var n = q.removeFirst();

            if (n instanceof Labeled labeled) {
                // baseline lookup (store once)
                if (rebuildBaseline || !baselineFont.containsKey(labeled)) {
                    baselineFont.put(labeled, labeled.getFont().getSize());
                }
                double base = baselineFont.get(labeled);
                labeled.setFont(Font.font(labeled.getFont().getFamily(), base * textScale));
            } else if (n instanceof TextInputControl input) {
                if (rebuildBaseline || !baselineFont.containsKey(input)) {
                    baselineFont.put(input, input.getFont().getSize());
                }
                double base = baselineFont.get(input);
                input.setFont(Font.font(input.getFont().getFamily(), base * textScale));
            }

            if (n instanceof Parent p) q.addAll(p.getChildrenUnmodifiable());
        }
    }

    // ---------------- Helpers ----------------
    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    private String resource(String cp) {
        var url = getClass().getResource(cp);
        if (url == null) throw new IllegalStateException("Missing resource " + cp);
        return url.toExternalForm();
    }
}
