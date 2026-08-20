package com.cases;

public class Middle {
    public Process pass(String value) throws Exception {
        return new Sink().exec(value);
    }
}
