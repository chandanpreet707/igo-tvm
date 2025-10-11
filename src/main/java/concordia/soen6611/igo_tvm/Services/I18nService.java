package concordia.soen6611.igo_tvm.Services;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class I18nService {
    public final MessageSource messages;
    private final ObjectProperty<Locale> locale = new SimpleObjectProperty<>(Locale.ENGLISH);

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
     * Get message for a code in the current locale with optional arguments
     * Signature: getMessage(code, args[], default, locale)
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

    public Locale getLocale() {
        return locale.get();
    }

    public void setLocale(Locale l) {
        locale.set(l);
    }

    public ReadOnlyObjectProperty<Locale> localeProperty() {
        return locale;
    }
}