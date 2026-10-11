package com.htsr.turbo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Game Turbo-style extras, all done with root shell commands and all reversible.
 * Nothing here runs by itself: Engine calls apply() when a game comes to the front or
 * leaves, and the toolbar calls the one-shot actions (screenshot, record, free RAM, stats).
 * Original values are saved in Cfg before anything is changed and put back afterwards.
 */
final class GameTurbo {
    static final int PERF = 1, RAM = 2, DND = 4, BRIGHT = 8, NET = 16, AWAKE = 32;

    private static final Pattern GOV_FILE = Pattern.compile(
            "^/sys/(devices/system/cpu/cpufreq/policy[0-9]+/scaling_governor"
                    + "|class/devfreq/[A-Za-z0-9_.:@-]+/governor"
                    + "|class/kgsl/kgsl-3d0/devfreq/governor)$");
    private static final Pattern GOV = Pattern.compile("^[a-z_]+$");
    private static final Pattern NUM = Pattern.compile("^[0-9]+$");

    // CPU clusters + GPU (Mali on MediaTek/Exynos, Adreno on Snapdragon). Only nodes that exist
    // AND offer "performance" are touched, so other phones simply skip what they don't have.
    private static final String LIST_GOV =
            "for d in /sys/devices/system/cpu/cpufreq/policy* /sys/class/devfreq/*gpu* "
                    + "/sys/class/devfreq/*mali* /sys/class/kgsl/kgsl-3d0/devfreq; do "
                    + "[ -e \"$d\" ] || continue; f=$d/scaling_governor; [ -e \"$f\" ] || f=$d/governor; "
                    + "[ -e \"$f\" ] || continue; "
                    + "grep -qw performance $d/scaling_available_governors $d/available_governors 2>/dev/null || continue; "
                    + "echo \"$f $(cat $f)\"; done";

    static volatile boolean recording;

    private GameTurbo() {}

    static int mask(Cfg c) {
        int m = 0;
        if (c.i("gt_perf", 0) == 1) m |= PERF;
        if (c.i("gt_ram", 1) == 1) m |= RAM;
        if (c.i("gt_dnd", 1) == 1) m |= DND;
        if (c.i("gt_bright", 0) == 1) m |= BRIGHT;
        if (c.i("gt_net", 0) == 1) m |= NET;
        if (c.i("gt_awake", 0) == 1) m |= AWAKE;
        return m;
    }

    /** Switches on what is newly wanted and puts back what is no longer wanted. */
    static void apply(Cfg c, int from, int to) {
        int[] bits = {PERF, RAM, DND, BRIGHT, NET, AWAKE};
        for (int b : bits) {
            boolean was = (from & b) != 0, now = (to & b) != 0;
            if (now && !was) on(c, b);
            else if (was && !now) off(c, b);
        }
    }

    private static void on(Cfg c, int bit) {
        switch (bit) {
            case PERF: perfOn(c); break;
            case RAM: freeRam(); break;
            case DND:
                save(c, "sv_zen", "global", "zen_mode", "0");
                save(c, "sv_heads", "global", "heads_up_notifications_enabled", "1");
                Root.run("settings put global zen_mode 1; settings put global heads_up_notifications_enabled 0");
                break;
            case BRIGHT:
                save(c, "sv_bm", "system", "screen_brightness_mode", "1");
                Root.run("settings put system screen_brightness_mode 0");
                break;
            case NET:
                Root.run("cmd wifi force-low-latency-mode enabled; cmd wifi force-hi-perf-mode enabled");
                break;
            case AWAKE:
                save(c, "sv_sot", "system", "screen_off_timeout", "60000");
                Root.run("settings put system screen_off_timeout 1800000");
                break;
            default: break;
        }
    }

    private static void off(Cfg c, int bit) {
        switch (bit) {
            case PERF: perfOff(c); break;
            case DND:
                restore(c, "sv_zen", "global", "zen_mode");
                restore(c, "sv_heads", "global", "heads_up_notifications_enabled");
                break;
            case BRIGHT: restore(c, "sv_bm", "system", "screen_brightness_mode"); break;
            case NET:
                Root.run("cmd wifi force-low-latency-mode disabled; cmd wifi force-hi-perf-mode disabled");
                break;
            case AWAKE: restore(c, "sv_sot", "system", "screen_off_timeout"); break;
            default: break;   // RAM is one-shot, nothing to undo
        }
    }

    // ---------------------------------------------------------------- saved settings

    private static void save(Cfg c, String key, String ns, String name, String fallback) {
        if (!c.s(key, "").isEmpty()) return;          // already saved, never overwrite the original
        String v = Root.sh("settings get " + ns + " " + name);
        c.put(key, NUM.matcher(v).matches() ? v : fallback);
    }

    private static void restore(Cfg c, String key, String ns, String name) {
        String v = c.s(key, "");
        if (!NUM.matcher(v).matches()) return;
        Root.run("settings put " + ns + " " + name + " " + v);
        c.put(key, "");
    }

    // ---------------------------------------------------------------- performance governors

    private static void perfOn(Cfg c) {
        String saved = c.s("sv_gov", "");
        if (saved.isEmpty()) {
            StringBuilder keep = new StringBuilder();
            for (String ln : Root.sh(LIST_GOV).split("\n")) {
                String[] p = ln.trim().split(" ");
                if (p.length == 2 && GOV_FILE.matcher(p[0]).matches() && GOV.matcher(p[1]).matches()
                        && !p[1].equals("performance")) {
                    keep.append(p[0]).append(' ').append(p[1]).append('\n');
                }
            }
            saved = keep.toString();
            c.put("sv_gov", saved);
        }
        StringBuilder cmd = new StringBuilder();
        for (String ln : saved.split("\n")) {
            String[] p = ln.trim().split(" ");
            if (p.length == 2 && GOV_FILE.matcher(p[0]).matches()) cmd.append("echo performance > ").append(p[0]).append("; ");
        }
        if (cmd.length() > 0) Root.run(cmd.toString());
    }

    private static void perfOff(Cfg c) {
        StringBuilder cmd = new StringBuilder();
        for (String ln : c.s("sv_gov", "").split("\n")) {
            String[] p = ln.trim().split(" ");
            if (p.length == 2 && GOV_FILE.matcher(p[0]).matches() && GOV.matcher(p[1]).matches()) {
                cmd.append("echo ").append(p[1]).append(" > ").append(p[0]).append("; ");
            }
        }
        if (cmd.length() > 0) Root.run(cmd.toString());
        c.put("sv_gov", "");
    }

    // ---------------------------------------------------------------- one-shot actions (toolbar)

    /** Kills cached background apps and drops file caches. The game in front is not touched. */
    static void freeRam() {
        Root.run("am kill-all; sync; echo 3 > /proc/sys/vm/drop_caches");
    }

    static int memFreeMb() {
        String s = Root.sh("grep MemAvailable /proc/meminfo").replaceAll("[^0-9]", "");
        try { return (int) (Long.parseLong(s) / 1024); } catch (Exception e) { return -1; }
    }

    private static String stamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    }

    /** Returns the file name, or null if it failed. */
    static String screenshot() {
        String name = "HTSR_" + stamp() + ".png";
        String f = "/sdcard/Pictures/" + name;
        int rc = Root.run("mkdir -p /sdcard/Pictures; screencap -p " + f + " && chmod 666 " + f
                + "; am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://" + f + " >/dev/null 2>&1");
        return rc == 0 ? name : null;
    }

    interface RecDone { void done(String name); }

    /** One clip, up to 3 minutes (the screenrecord limit). Runs on its own thread until stopped. */
    static void startRecord(final RecDone cb) {
        if (recording) return;
        recording = true;
        final String name = "HTSR_" + stamp() + ".mp4";
        final String f = "/sdcard/Movies/" + name;
        new Thread(new Runnable() {
            @Override public void run() {
                Root.run("mkdir -p /sdcard/Movies; screenrecord --bit-rate 12000000 " + f
                        + "; chmod 666 " + f
                        + "; am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://" + f + " >/dev/null 2>&1");
                recording = false;
                cb.done(name);
            }
        }).start();
    }

    /** SIGINT makes screenrecord finish and close the file properly. */
    static void stopRecord() {
        Root.run("pkill -2 screenrecord || killall -2 screenrecord");
    }

    /** One quick reading: battery, hottest thermal zone, free RAM. */
    static String stats() {
        String o = Root.sh("b=$(cat /sys/class/power_supply/battery/temp 2>/dev/null); "
                + "c=$(cat /sys/class/power_supply/battery/capacity 2>/dev/null); "
                + "t=$(cat /sys/class/thermal/thermal_zone*/temp 2>/dev/null | sort -n | tail -1); "
                + "echo \"${b:-0} ${c:-0} ${t:-0}\"");
        String[] p = o.trim().split(" ");
        try {
            double bt = Long.parseLong(p[0]) / 10.0;
            int cap = Integer.parseInt(p[1]);
            long t = Long.parseLong(p[2]);
            double hot = t > 1000 ? t / 1000.0 : t;
            return String.format(Locale.US, "%.0f\u00B0C \u00B7 %.1f GB free \u00B7 %d%%",
                    Math.max(bt, hot), memFreeMb() / 1024.0, cap);
        } catch (Exception e) {
            return "Free RAM " + memFreeMb() + " MB";
        }
    }
}
