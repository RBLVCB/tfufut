package com.htsr.turbo;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/** Runs shell snippets as root. Call from a background thread only. */
final class Root {
    private Root() {}

    /** Runs the script, returns the exit code (-1 if su is missing / denied). */
    static int run(String script) {
        try {
            Process pr = new ProcessBuilder("su", "-c", script).redirectErrorStream(true).start();
            BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            while (r.readLine() != null) { /* drain */ }
            return pr.waitFor();
        } catch (Exception e) {
            return -1;
        }
    }

    /** Runs the script and returns its trimmed stdout. */
    static String sh(String script) {
        try {
            Process pr = new ProcessBuilder("su", "-c", script).redirectErrorStream(true).start();
            BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
            pr.waitFor();
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    static final String SVC = "com.htsr.turbo/com.htsr.turbo.DetectService";

    /** Enables the accessibility service and exempts the app from battery limits. Safe to repeat. */
    static int setup() {
        return run("cur=$(settings get secure enabled_accessibility_services); "
                + "case \"$cur\" in *" + SVC + "*) ;; \"\"|null) settings put secure enabled_accessibility_services " + SVC + " ;; "
                + "*) settings put secure enabled_accessibility_services \"$cur:" + SVC + "\" ;; esac; "
                + "settings put secure accessibility_enabled 1; "
                + "dumpsys deviceidle whitelist +com.htsr.turbo; "
                + "appops set com.htsr.turbo RUN_ANY_IN_BACKGROUND allow; "
                + "appops set com.htsr.turbo SYSTEM_ALERT_WINDOW allow; "
                + "appops set com.htsr.turbo 10021 allow");
    }
}
