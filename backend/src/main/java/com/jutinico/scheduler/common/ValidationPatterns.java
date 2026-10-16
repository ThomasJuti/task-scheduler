package com.jutinico.scheduler.common;

public final class ValidationPatterns {

    /** Optional surrounding spaces (trimmed later), then a letter or digit; then letters, digits, spaces and + # . - / (e.g. "C#", "Node.js", "CI/CD"). */
    public static final String SKILL = "^\\s*[\\p{L}\\p{N}][\\p{L}\\p{N} +#.\\-/]*$";

    public static final String SKILL_MESSAGE = "may only contain letters, digits, spaces and + # . - /";

    private ValidationPatterns() {
    }
}
