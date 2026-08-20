package com.cases.legacy;

public class Legacy {
    public Process archive() throws Exception {
        String target = System.getenv("LEGACY_TARGET");
        return Runtime.getRuntime().exec("/usr/bin/legacy-archive --target " + target);
    }
}
