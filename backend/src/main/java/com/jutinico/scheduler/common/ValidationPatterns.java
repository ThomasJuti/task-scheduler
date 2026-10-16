package com.jutinico.scheduler.common;

public final class ValidationPatterns {

    /** Espacios opcionales al inicio (se recortan después), luego letra o dígito; después letras, dígitos, espacios y + # . - / (ej. "C#", "Node.js", "CI/CD"). */
    public static final String SKILL = "^\\s*[\\p{L}\\p{N}][\\p{L}\\p{N} +#.\\-/]*$";

    public static final String SKILL_MESSAGE = "may only contain letters, digits, spaces and + # . - /";

    /** Sin caracteres de control (NUL, saltos de línea, tabuladores…): PostgreSQL rechaza NUL en texto y daba un 409 engañoso. */
    public static final String NO_CONTROL_CHARS = "^\\P{Cntrl}*$";

    public static final String NO_CONTROL_CHARS_MESSAGE = "must not contain control characters";

    private ValidationPatterns() {
    }
}
