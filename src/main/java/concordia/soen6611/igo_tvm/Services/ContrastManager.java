package concordia.soen6611.igo_tvm.Services;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.ColorAdjust;

import java.util.prefs.Preferences;
public class ContrastManager {
    private static final ContrastManager INSTANCE = new ContrastManager();
    public static ContrastManager getInstance() { return INSTANCE; }

    // JavaFX ranges are [-1, 1]. We keep it conservative for kiosk readability.
    private static final double MIN_LEVEL = -0.40;  // darker / flatter
    private static final double MAX_LEVEL =  0.60;  // brighter / punchier
    private static final double STEP      =  0.10;  // per button press

    // Tune how much brightness changes relative to contrast:
    // brightness = level * BRIGHTNESS_FACTOR
    private static final double BRIGHTNESS_FACTOR = 0.50; // 50% of contrast change

    private final Preferences prefs = Preferences.userRoot().node("igo-tvm/contrast");
    private double level = clamp(prefs.getDouble("level", 0.00));

    private Parent attachedRoot;
    private final ColorAdjust effect = new ColorAdjust(0, 0, 0, 0);

    private ContrastManager() {}

    /** Call once per page (e.g., in controller initialize via Platform.runLater). */
    public void attach(Scene scene, Parent root) {
        this.attachedRoot = root;
        if (attachedRoot.getEffect() != effect) {
            attachedRoot.setEffect(effect);
        }
        apply();
    }

    // ---- API for buttons ----
    public double getLevel()      { return level; }
    public void increase()        { setLevel(level + STEP); }
    public void decrease()        { setLevel(level - STEP); }
    public void reset()           { setLevel(0.0); }

    public void setLevel(double newLevel) {
        level = clamp(newLevel);
        prefs.putDouble("level", level);
        apply();
    }

    // ---- internal apply ----
    private void apply() {
        // Contrast tracks level directly
        effect.setContrast(level);
        // Brightness follows level at a gentler slope
        effect.setBrightness(level * BRIGHTNESS_FACTOR);
        // We leave hue & saturation at 0 to avoid color shifts
    }

    private static double clamp(double v) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, v));
    }
}
