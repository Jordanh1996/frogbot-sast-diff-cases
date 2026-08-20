package com.cases;

public class Middle {
    public Process pass(String value) throws Exception {
        String sanitized = value.trim();
        return new Sink().exec(sanitized);
    }
}
