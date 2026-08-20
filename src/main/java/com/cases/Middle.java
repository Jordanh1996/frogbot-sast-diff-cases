package com.cases;

public class Middle {
    public Process pass(String value) throws Exception {
        return new Sink().exec(value);
    }

    public Process passWithRetry(String value) throws Exception {
        Process first = new Sink().exec(value);
        if (first == null) {
            return new Sink().exec(value);
        }
        return first;
    }
}
