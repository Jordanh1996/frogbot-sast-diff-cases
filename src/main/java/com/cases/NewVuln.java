package com.cases;

public class NewVuln {
    public Process purge() throws Exception {
        String target = System.getenv("PURGE_TARGET");
        return Runtime.getRuntime().exec("/usr/bin/purge-cache --target " + target);
    }
}
