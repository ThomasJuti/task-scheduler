package com.jutinico.scheduler.common;

public final class Names {

    private Names() {
    }

    public static String normalize(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ");
    }
}
