package concordia.soen6611.igo_tvm.Services;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Internationalization (i18n) service that provides localized message lookup
 * and a bindable/current {@link Locale} for the application UI.
 * <p>
 * This service wraps a Spring {@link MessageSource} and exposes:
 * <ul>
 *   <li>A reactive {@link #localeProperty()} to observe or change the current locale.</li>
 *   <li>{@link #get(String, Object...)} for retrieving localized strings using message codes.</li>
 * </ul>
 * Controllers may listen for locale changes and re-apply translations to their UI.
 * <p>
 * Default locale is {@link Locale#ENGLISH}.
 */
@Service
public class I18nService {

    /** Backing Spring message source used for resolving message codes. */
    public final MessageSource messages;

    /** Observable/Settable current locale property (defaults to English). */
    private final ObjectProperty<Locale> locale = new SimpleObjectProperty<>(Locale.ENGLISH);

    /**
     * Constructs the service with the required {@link MessageSource}.
     * Performs a simple sanity check by attempting to load the {@code "welcome"} key
     * in English and French (logging results for diagnostics).
     *
     * @param messages configured Spring {@link MessageSource} (e.g., ReloadableResourceBundleMessageSource)
     */
    public I18nService(MessageSource messages) {
        this.messages = messages;
        System.out.println("[I18nService] Initialized with MessageSource: " + messages.getClass().getName());

        // Test loading a message
        try {
            String testEn = messages.getMessage("welcome", null, Locale.ENGLISH);
            String testFr = messages.getMessage("welcome", null, Locale.FRENCH);
            System.out.println("[I18nService] Test EN: " + testEn);
            System.out.println("[I18nService] Test FR: " + testFr);
        } catch (Exception e) {
            System.err.println("[I18nService] ERROR loading test messages!");
            e.printStackTrace();
        }
    }

    /**
     * Resolves a localized message for the given code in the current {@link Locale}.
     * <p>
     * This method delegates to {@link MessageSource#getMessage(String, Object[], Locale)}.
     * If the code is not found, the method logs the error and returns the code itself
     * as a fallback, allowing the UI to render a sensible placeholder.
     *
     * @param code message key (e.g., {@code "home.title"})
     * @param args optional message arguments for parameterized messages
     * @return the resolved localized string, or {@code code} if not found
     */
    public String get(String code, Object... args) {
        try {
            // Correct parameter order: code, args, defaultMessage, locale
            String result = messages.getMessage(code, args, getLocale());
            System.out.println("[I18n] Code: " + code + " | Locale: " + getLocale() + " | Result: " + result);
            return result;
        } catch (NoSuchMessageException e) {
            System.err.println("[ERROR] Message not found for code: " + code + " in locale: " + getLocale());
            System.err.println("[ERROR] MessageSource type: " + messages.getClass().getName());
            e.printStackTrace();
            return code; // Return code as fallback
        }
    }

    /**
     * Returns the current {@link Locale}.
     *
     * @return current locale value
     */
    public Locale getLocale() {
        return locale.get();
    }

    /**
     * Updates the current {@link Locale}. Listeners of {@link #localeProperty()}
     * will be notified, enabling controllers to re-localize their views.
     *
     * @param l new locale to apply (non-null recommended)
     */
    public void setLocale(Locale l) {
        locale.set(l);
    }

    /**
     * Exposes a read-only view of the current {@link Locale} property for binding.
     * <p>
     * Controllers can observe this property to react to language changes.
     *
     * @return read-only locale property
     */
    public ReadOnlyObjectProperty<Locale> localeProperty() {
        return locale;
    }
}
