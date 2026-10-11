package com.htsr.turbo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The brain: decides what should be on (from settings + foreground game + screen state)
 * and applies it with root. Everything runs on one worker thread, nothing polls.
 *
 * HTSR = the touch driver's "game mode" flag. The driver exposes the same function the
 * /dev/xiaomi-touch ioctl uses as a sysfs file:  echo "<mode> <value>" > .../touchgame
 *   mode 0 = game mode on/off, 2 = tap sensitivity, 3 = swipe response,
 *   4 = aim sensitivity, 5 = tap stability, 6 = expert (Pro) preset 1-3.
 */
final class Engine {
    interface Host {
        void showXh(boolean on);
        void refreshXh();
        void showToolbar(boolean on);
    }

    static final int W_HTSR = 0, W_TOUCH = 1, W_COLOR = 2, W_XH = 3, W_GT = 4;
    private static final Pattern FOCUS = Pattern.compile(" ([A-Za-z0-9_.]+)/");
    private static final Pattern SAFE_PATH = Pattern.compile("^/sys/[A-Za-z0-9_./:@-]+$");

    private static Engine inst;

    static synchronized Engine get(Context c) {
        if (inst == null) inst = new Engine(c.getApplicationContext());
        return inst;
    }

    final Cfg cfg;
    volatile Host host;
    volatile String fg = "";
    volatile boolean rootOk;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService ex = Executors.newSingleThreadExecutor();
    private String node;
    private boolean started;
    private int aHtsr = -1, aColor = -1, aXh = -1, aTool = -1, aGt = 0;
    private String aProf = "";   // which game's profile is applied   // what we last applied (-1 = unknown)

    private Engine(Context c) {
        cfg = new Cfg(c);
    }

    // ---------------------------------------------------------------- lifecycle

    synchronized void start() {
        if (started) return;
        started = true;
        ex.execute(this::bootstrap);
    }

    /** Forget what we applied and re-apply everything (service reconnected). */
    synchronized void restart() {
        started = false;
        start();
    }

    boolean serviceConnected() { return host != null; }

    /** Settings of the game in front (or the last one). */
    Cfg activeProfile() { return cfg.profile(fg); }

    /** Runs a job on the worker thread (root commands must not run on the UI thread). */
    void post(Runnable r) { ex.execute(r); }

    private void bootstrap() {
        aHtsr = aColor = aXh = aTool = -1;
        aProf = "";
        aGt = cfg.i("gtApplied", 0);   // what a previous run left switched on, so sync() can put it back
        rootOk = Root.run("id") == 0;
        node = findNode();
        readDefaults();
        String p = parseFocus(Root.sh("dumpsys window 2>/dev/null | grep -m1 mCurrentFocus"));
        if (p != null && !DetectService.ignored(p)) fg = p;
        sync(false);
    }

    private String findNode() {
        String n = cfg.s("node", "");
        if (SAFE_PATH.matcher(n).matches() && Root.run("[ -e " + n + " ]") == 0) return n;
        n = Root.sh("ls /sys/bus/spi/devices/*/touchgame 2>/dev/null | head -1");
        if (n.isEmpty()) n = Root.sh("find /sys/devices -maxdepth 8 -name touchgame 2>/dev/null | head -1");
        if (!SAFE_PATH.matcher(n).matches()) return null;
        cfg.put("node", n);
        return n;
    }

    /** Stock tuning values live in the device tree (fts,touch-*-def). */
    private void readDefaults() {
        if (cfg.i("defsOk", 0) == 1) return;
        int a = dtu("fts,touch-follow-performance-def");
        int b = dtu("fts,touch-tap-sensitivity-def");
        int c = dtu("fts,touch-aim-sensitivity-def");
        int d = dtu("fts,touch-tap-stability-def");
        cfg.put("dtol", a > 0 ? a : 3);
        cfg.put("dup", b > 0 ? b : 3);
        cfg.put("daim", c > 0 ? c : 3);
        cfg.put("dstab", d > 0 ? d : 3);
        if (a > 0 && b > 0 && c > 0 && d > 0) cfg.put("defsOk", 1);
    }

    private int dtu(String name) {
        String o = Root.sh("p=$(find /sys/firmware/devicetree/base -name '" + name
                + "' 2>/dev/null | head -1); [ -n \"$p\" ] && od -An -tx1 -N4 \"$p\"");
        try {
            int v = (int) Long.parseLong(o.replaceAll("\\s", ""), 16);
            if (v >= 1 && v <= 5) return v;
        } catch (Exception ignored) { }
        return 0;
    }

    static String parseFocus(String line) {
        if (line == null) return null;
        Matcher m = FOCUS.matcher(line);
        return m.find() ? m.group(1) : null;
    }

    // ---------------------------------------------------------------- events in

    void onFg(String pkg) {
        if (pkg.equals(fg)) return;
        fg = pkg;
        ex.execute(() -> sync(false));
    }

    void onScreenOn() {
        ex.execute(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException ignored) { }   // let the touch chip resume
            sync(true);
        });
    }

    /** The user changed a setting in the UI. */
    void onConfig(int what) {
        ex.execute(() -> {
            if (what == W_TOUCH) {
                applyTouch();
            } else if (what == W_COLOR) {
                if (aColor == 1) applyColor(1);
            } else if (what == W_XH) {
                main.post(() -> { Host h = host; if (h != null) h.refreshXh(); });
            }
            sync(false);
        });
    }

    void resetColor() {
        ex.execute(() -> applyColor(0));
    }

    // ---------------------------------------------------------------- the decision

    private void sync(boolean woke) {
        boolean inGame = cfg.games().contains(fg);
        Cfg pf = cfg.profile(fg);
        boolean same = fg.equals(aProf);

        int hw = inGame && pf.s("hmode", "on").equals("on") ? 1 : 0;
        if (hw == 1) {
            if (woke || aHtsr != 1) applyHtsr(true, woke);
            else if (!same) applyTouch();          // switched straight to another game
        } else if (aHtsr != 0) {
            applyHtsr(false, false);
        }

        int cw = inGame && pf.s("cmode", "off").equals("on") ? 1 : 0;
        if (cw != aColor || (cw == 1 && (woke || !same))) applyColor(cw);

        int xw = inGame && pf.s("xmode", "off").equals("on") ? 1 : 0;
        if (xw != aXh || (xw == 1 && !same)) {
            aXh = xw;
            final boolean on = xw == 1;
            main.post(() -> {
                Host h = host;
                if (h == null) return;
                h.showXh(on);
                if (on) h.refreshXh();
            });
        }

        // Game Turbo extras: switched on in a game, put back when it leaves
        int mask = inGame ? GameTurbo.mask(pf) : 0;
        if (mask != aGt) {
            GameTurbo.apply(cfg, aGt, mask);
            aGt = mask;
            cfg.put("gtApplied", mask);
        }
        int tool = inGame && pf.i("gt_tool", 1) == 1 ? 1 : 0;
        if (tool != aTool) {
            aTool = tool;
            final boolean tb = tool == 1;
            main.post(() -> { Host h = host; if (h != null) h.showToolbar(tb); });
        }
        aProf = fg;
    }

    // ---------------------------------------------------------------- appliers

    private static String tw(String n, int mode, int value) {
        return "echo '" + mode + " " + value + "' > " + n + "; ";
    }

    private String touchWrite(String n) {
        Cfg pf = activeProfile();
        String m = pf.s("tmode", "classic");
        if (m.equals("pro")) return tw(n, 6, pf.i("pro", 1));
        boolean custom = m.equals("custom");
        int tol = custom ? pf.i("tol", 3) : cfg.i("dtol", 3);
        int up = custom ? pf.i("up", 3) : cfg.i("dup", 3);
        int aim = custom ? pf.i("aim", 3) : cfg.i("daim", 3);
        int stab = custom ? pf.i("stab", 3) : cfg.i("dstab", 3);
        return tw(n, 3, tol) + tw(n, 2, up) + tw(n, 4, aim) + tw(n, 5, stab);
    }

    /**
     * The driver only re-sends to the chip when a value CHANGES, and the chip forgets the
     * game mode when the screen goes off. So: flip to 0, wait for the driver, then set 1.
     */
    private void applyHtsr(boolean on, boolean woke) {
        if (node == null) { aHtsr = on ? 1 : 0; return; }
        StringBuilder s = new StringBuilder();
        if (on) {
            if (woke) s.append(tw(node, 0, 0)).append("sleep 1; ");
            s.append(touchWrite(node)).append(tw(node, 0, 1));
        } else {
            s.append(tw(node, 0, 0));
        }
        Root.run(s.toString());
        aHtsr = on ? 1 : 0;
    }

    /** User changed touch settings: poke a different value first so the driver notices the change. */
    private void applyTouch() {
        if (node == null) return;
        Cfg pf = activeProfile();
        String m = pf.s("tmode", "classic");
        String poke;
        if (m.equals("pro")) {
            poke = tw(node, 6, pf.i("pro", 1) % 3 + 1);
        } else {
            int tol = m.equals("custom") ? pf.i("tol", 3) : cfg.i("dtol", 3);
            poke = tw(node, 3, tol % 5 + 1);
        }
        Root.run(poke + "sleep 1; " + touchWrite(node) + "sleep 1; " + touchWrite(node));
    }

    /** SurfaceFlinger saturation (1.0 = stock, 2.0 max). */
    private void applyColor(int on) {
        int pct = activeProfile().i("cpct", 125);
        if (on == 1 && pct == 100) pct = 101;   // exactly 1.0 can be a no-op in "saturated" colour mode
        String v = on == 1 ? String.format(Locale.US, "%d.%02d", pct / 100, pct % 100) : "1.0";
        Root.run("service call SurfaceFlinger 1022 f " + v);
        aColor = on;
    }
}
