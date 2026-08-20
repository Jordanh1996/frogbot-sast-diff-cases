package com.cases;

public class Sink {
    public Process exec(String value) throws Exception {
        return Runtime.getRuntime().exec("/usr/bin/report-export --target " + value);
    }
}
