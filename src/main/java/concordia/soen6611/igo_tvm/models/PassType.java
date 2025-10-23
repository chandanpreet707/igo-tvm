package concordia.soen6611.igo_tvm.models;

/**
 * Enumerates available pass types for card reloads/purchases.
 * <p>
 * Each enum constant carries an i18n message key used to localize
 * its display label in the UI (e.g., "Single Pass", "Weekly Pass").
 * Retrieve the key via {@link #key()} and resolve it with the
 * application's {@code I18nService}.
 * </p>
 */
public enum PassType {
    /** Single-trip product. Message key: {@code cardReloadAmount.pass.single}. */
    SINGLE("cardReloadAmount.pass.single"),   // e.g. "Single Pass"
    /** Weekly pass product. Message key: {@code cardReloadAmount.pass.weekly}. */
    WEEKLY("cardReloadAmount.pass.weekly"),   // "Weekly Pass"
    /** Monthly pass product. Message key: {@code cardReloadAmount.pass.monthly}. */
    MONTHLY("cardReloadAmount.pass.monthly"), // "Monthly Pass"
    /** Day pass product. Message key: {@code cardReloadAmount.pass.day}. */
    DAY("cardReloadAmount.pass.day");         // "Day Pass"

    /** Internationalization message key for the pass label. */
    private final String msgKey;

    /**
     * Associates an i18n message key with the enum constant.
     *
     * @param k message key to be used with the i18n service
     */
    PassType(String k) { this.msgKey = k; }

    /**
     * Returns the i18n message key for this pass type.
     *
     * @return non-null message key (e.g., {@code "cardReloadAmount.pass.weekly"})
     */
    public String key() { return msgKey; }
}
