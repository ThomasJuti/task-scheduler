package com.jutinico.scheduler.common;

public final class Names {

    private Names() {
    }

    /** Trims and collapses internal whitespace, so "  Big   task " becomes "Big task". */
    public static String normalize(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ");
    }
}
