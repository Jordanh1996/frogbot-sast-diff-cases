package com.cases;

public class Entry {
    public String readEnvTarget() {
        return System.getenv("EXPORT_TARGET");
    }

    public Process runFromEnv() throws Exception {
        return new Middle().pass(readEnvTarget());
    }
}
