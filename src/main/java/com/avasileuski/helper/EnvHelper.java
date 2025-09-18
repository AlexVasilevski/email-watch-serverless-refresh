package com.avasileuski.helper;

public class EnvHelper {

    public static String getEnvRequired(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required env var: " + key);
        }
        return v;
    }
}
