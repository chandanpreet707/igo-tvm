package concordia.soen6611.igo_tvm.Services;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.ColorAdjust;

import java.util.prefs.Preferences;

/**
 * Manages global contrast/brightness adjustments for the kiosk UI.
 * <p>
 * This singleton applies a {@link ColorAdjust} effect to the current scene's root
 * and persists the current contrast "level" using {@link Preferences}, so the
 * setting survives application restarts. Contrast is mapped directly from the
 * selected level, while brightness is derived at a gentler slope
 * (see {@link #BRIGHTNESS_FACTOR}).
 * </p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // In a controller's initialize (typically via Platform.runLater):
 * ContrastManager.getInstance().attach(root.getScene(), root);
 *
 * // Hook up buttons:
 * ContrastManager.getInstance().increase();
 * ContrastManager.getInstance().decrease();
 * ContrastManager.getInstance().reset();
 * }</pre>
 *
 * <h3>Threading</h3>
 * All calls that mutate JavaFX nodes (e.g., {@link #attach(Scene, Parent)}) should
 * be executed on the JavaFX Application Thread.
 */
public class ContrastManager {

    /** Singleton instance. */
    private static final ContrastManager INSTANCE = new ContrastManager();

    /**
     * Returns the singleton {@code ContrastManager}.
     *
     * @return global instance
     */
    public static ContrastManager getInstance() { return INSTANCE; }

    /** Minimum allowed contrast level (darker/flatter). */
    private static final double MIN_LEVEL = -0.40;  // darker / flatter
    /** Maximum allowed contrast level (brighter/punchier). */
    private static final double MAX_LEVEL =  0.60;  // brighter / punchier
    /** Increment/decrement step applied per button press. */
    private static final double STEP      =  0.10;  // per button press

    /**
     * Brightness scaling relative to contrast:
     * {@code brightness = level * BRIGHTNESS_FACTOR}.
     */
    private static final double BRIGHTNESS_FACTOR = 0.50; // 50% of contrast change

    /** Preferences node used to persist the current level between runs. */
    private final Preferences prefs = Preferences.userRoot().node("igo-tvm/contrast");

    /** Current contrast level, clamped to [{@link #MIN_LEVEL}, {@link #MAX_LEVEL}]. */
    private double level = clamp(prefs.getDouble("level", 0.00));

    /** The root currently having the effect attached. */
    private Parent attachedRoot;
    /** Shared color adjustment effect applied to {@link #attachedRoot}. */
    private final ColorAdjust effect = new ColorAdjust(0, 0, 0, 0);

    /** Hidden constructor for singleton. */
    private ContrastManager() {}

    /**
     * Attaches the contrast/brightness effect to the given scene root and applies
     * the current level. Call once per page (e.g., in controller {@code initialize}
     * via {@code Platform.runLater}).
     *
     * @param scene the active JavaFX {@link Scene}
     * @param root  the root {@link Parent} to which the effect will be applied
     */
    /** Call once per page (e.g., in controller initialize via Platform.runLater). */
    public void attach(Scene scene, Parent root) {
        this.attachedRoot = root;
        if (attachedRoot.getEffect() != effect) {
            attachedRoot.setEffect(effect);
        }
        apply();
    }

    // ---- API for buttons ----

    /**
     * Returns the current contrast level.
     *
     * @return current level in [{@link #MIN_LEVEL}, {@link #MAX_LEVEL}]
     */
    public double getLevel()      { return level; }

    /**
     * Increases the contrast level by {@link #STEP} (clamped to max) and applies it.
     */
    public void increase()        { setLevel(level + STEP); }

    /**
     * Decreases the contrast level by {@link #STEP} (clamped to min) and applies it.
     */
    public void decrease()        { setLevel(level - STEP); }

    /**
     * Resets the contrast level to {@code 0.0} and applies it.
     */
    public void reset()           { setLevel(0.0); }

    /**
     * Sets the contrast level (clamped to allowed range), persists it to
     * {@link Preferences}, and applies the effect.
     *
     * @param newLevel desired level; will be clamped to [{@link #MIN_LEVEL}, {@link #MAX_LEVEL}]
     */
    public void setLevel(double newLevel) {
        level = clamp(newLevel);
        prefs.putDouble("level", level);
        apply();
    }

    // ---- internal apply ----

    /**
     * Applies the current {@link #level} to the {@link #effect}, mapping contrast
     * directly and brightness via {@link #BRIGHTNESS_FACTOR}. Hue and saturation
     * remain at zero to avoid color shifts.
     */
    private void apply() {
        // Contrast tracks level directly
        effect.setContrast(level);
        // Brightness follows level at a gentler slope
        effect.setBrightness(level * BRIGHTNESS_FACTOR);
        // We leave hue & saturation at 0 to avoid color shifts
    }

    /**
     * Clamps a value to the allowed contrast range.
     *
     * @param v raw level
     * @return clamped level within [{@link #MIN_LEVEL}, {@link #MAX_LEVEL}]
     */
    private static double clamp(double v) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, v));
    }
}
