package com.htsr.turbo;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Event-driven game detection (no polling) + the crosshair overlay.
 * Android starts this service by itself after boot once it is enabled.
 * It reads only the package name of the window that came to the front.
 */
public class DetectService extends AccessibilityService implements Engine.Host {
    // Windows that appear over a game without meaning "the game was left".
    private static final Set<String> IGNORE = new HashSet<>(Arrays.asList(
            "com.android.systemui", "android", "com.htsr.turbo",
            "com.miui.securitycenter", "com.miui.gamebooster",   // Xiaomi Game Turbo
            "com.oplus.games"));                                // OnePlus / OPPO Game Space

    private WindowManager wm;
    private XhView xv;
    private Toolbar tb;
    private WindowManager.LayoutParams lp;
    private boolean receiverOn;
    private String lastPkg = "";

    private final BroadcastReceiver screen = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (Intent.ACTION_SCREEN_ON.equals(i.getAction())) Engine.get(c).onScreenOn();
        }
    };

    static boolean ignored(String pkg) {
        return IGNORE.contains(pkg);
    }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Engine e = Engine.get(this);
        e.host = this;
        if (!receiverOn) {
            registerReceiver(screen, new IntentFilter(Intent.ACTION_SCREEN_ON));
            receiverOn = true;
        }
        e.restart();
    }

    private String imePackage() {
        String s = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        return s == null ? "" : s.split("/")[0];
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence cs = e.getPackageName();
        if (cs == null) return;
        String pkg = cs.toString();
        if (ignored(pkg) || pkg.equals(lastPkg) || pkg.equals(imePackage())) return;
        lastPkg = pkg;
        Engine.get(this).onFg(pkg);
    }

    @Override public void onInterrupt() { }

    @Override public boolean onUnbind(Intent i) {
        cleanup();
        return super.onUnbind(i);
    }

    @Override public void onDestroy() {
        cleanup();
        super.onDestroy();
    }

    private void cleanup() {
        Engine e = Engine.get(this);
        if (e.host == this) e.host = null;
        removeOverlay();
        removeToolbar();
        if (receiverOn) {
            try { unregisterReceiver(screen); } catch (Exception ignored) { }
            receiverOn = false;
        }
    }

    // ---------------------------------------------------------------- crosshair overlay

    @Override public void showXh(boolean on) {
        if (!on) { removeOverlay(); return; }
        if (xv == null) {
            xv = new XhView(this);
            lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.CENTER;
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                lp.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
            applyParams();
            try { wm.addView(xv, lp); } catch (Exception ex) { xv = null; }
        } else {
            refreshXh();
        }
    }

    @Override public void refreshXh() {
        if (xv == null) return;
        applyParams();
        try { wm.updateViewLayout(xv, lp); } catch (Exception ignored) { }
    }

    private void applyParams() {
        Cfg cfg = Engine.get(this).activeProfile();
        float d = getResources().getDisplayMetrics().density;
        xv.load(cfg, d);
        lp.width = lp.height = Math.round(cfg.i("xsize", 40) * d);
        lp.x = Math.round(cfg.i("xox", 0) * d);
        lp.y = Math.round(cfg.i("xoy", 0) * d);
    }

    private void removeOverlay() {
        if (xv != null) {
            try { wm.removeView(xv); } catch (Exception ignored) { }
            xv = null;
        }
    }

    // ---------------------------------------------------------------- game toolbar

    @Override public void showToolbar(boolean on) {
        if (!on) { removeToolbar(); return; }
        if (tb == null) tb = new Toolbar(this, wm, Engine.get(this));
        tb.show();
    }

    private void removeToolbar() {
        if (tb != null) {
            tb.hide();
            tb = null;
        }
    }
}
