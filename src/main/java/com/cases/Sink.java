package com.cases;

public class Sink {
    public String describeExecutable() {
        return "/usr/bin/report-export";
    }

    public Process exec(String value) throws Exception {
        return Runtime.getRuntime().exec("/usr/bin/report-export --target " + value);
    }
}
