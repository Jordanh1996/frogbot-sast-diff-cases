package com.cases;

public class Middle {
    public String describe() {
        return "forwards export targets to the executor";
    }

    public Process pass(String value) throws Exception {
        return new Sink().exec(value);
    }
}
