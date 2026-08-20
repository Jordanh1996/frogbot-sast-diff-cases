package com.cases;

public class Forwarder {
    public Process pass(String value) throws Exception {
        return new Sink().exec(value);
    }
}
