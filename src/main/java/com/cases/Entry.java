package com.cases;

public class Entry {
    public String readEnvTarget() {
        return System.getenv("EXPORT_TARGET");
    }

    public String readPropertyTarget() {
        return System.getProperty("export.target");
    }

    public Process runFromEnv() throws Exception {
        return new Middle().pass(readEnvTarget());
    }

    public Process runFromProperty() throws Exception {
        return new Middle().pass(readPropertyTarget());
    }

    public Process runWithRetry() throws Exception {
        return new Middle().passWithRetry(readEnvTarget());
    }
}
