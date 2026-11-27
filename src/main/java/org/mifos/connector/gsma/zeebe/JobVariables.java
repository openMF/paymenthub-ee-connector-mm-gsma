package org.mifos.connector.gsma.zeebe;

import java.util.Map;

/** Reads the Zeebe variables a job cannot run without. */
public final class JobVariables {

    private JobVariables() {}

    /**
     * The text value of a required variable. A missing or non-text variable fails the job with a message that names it,
     * rather than a NullPointerException or ClassCastException that doesn't. The Zeebe client puts the message on the
     * failed job and, when the retries run out, on the incident.
     */
    public static String requiredText(Map<String, Object> variables, String name) {
        Object value = variables.get(name);
        if (value == null) {
            throw new IllegalStateException("Zeebe variable '" + name + "' is missing");
        }
        if (!(value instanceof String text)) {
            throw new IllegalStateException("Zeebe variable '" + name + "' is a " + value.getClass().getSimpleName() + ", expected text");
        }
        return text;
    }

    /** The value of a required variable as text, whatever its type. */
    public static String requiredAsText(Map<String, Object> variables, String name) {
        Object value = variables.get(name);
        if (value == null) {
            throw new IllegalStateException("Zeebe variable '" + name + "' is missing");
        }
        return value.toString();
    }
}
