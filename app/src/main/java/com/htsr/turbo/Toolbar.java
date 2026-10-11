package com.htsr.turbo;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

/**
 * In-game toolbar: a thin handle on the left edge. Tap it and the two glass panels open on the left and
 * right, leaving the middle of the screen free. Nothing runs while it is closed.
 */
final class Toolbar {
    private final Context ctx;
    private final WindowManager wm;
    private final Engine eng;
    private View handle, left, right;

    Toolbar(Context c, WindowManager w, Engine e) {
        ctx = c;
        wm = w;
        eng = e;
    }

    void show() {
        if (handle != null || left != null) return;
        addHandle();
    }

    void hide() {
        remove(handle);
        remove(left);
        remove(right);
        handle = left = right = null;
    }

    /** App overlay sits below the status bar and notification shade; the accessibility overlay is only a fallback. */
    private int type() {
        return Build.VERSION.SDK_INT >= 26 && Settings.canDrawOverlays(ctx)
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
    }

    private WindowManager.LayoutParams params(int w, int h, int gravity, int x, boolean blur) {
        WindowManager.LayoutParams l = new WindowManager.LayoutParams(w, h, type(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        l.gravity = gravity;
        l.x = x;
        if (blur && Build.VERSION.SDK_INT >= 31) {
            try {   // blur what is behind the panel (Android 12+); silently skipped if not allowed
                l.flags |= 0x00000004;   // FLAG_BLUR_BEHIND
                l.getClass().getMethod("setBlurBehindRadius", int.class).invoke(l, Ui.dp(ctx, 24));
            } catch (Exception ignored) { }
        }
        return l;
    }

    private void addHandle() {
        View h = new View(ctx);
        h.setBackground(Ui.box(ctx, 0xAA2F8CFF, 0x40FFFFFF, 6));
        h.setContentDescription("Open HTSR toolbar");
        h.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openPanels(); }
        });
        try {
            wm.addView(h, params(Ui.dp(ctx, 14), Ui.dp(ctx, 72), Gravity.START | Gravity.CENTER_VERTICAL, 0, false));
            handle = h;
        } catch (Exception ignored) { }
    }

    private void openPanels() {
        remove(handle);
        handle = null;
        Cfg p = eng.cfg.profile(eng.fg);
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        int w = Math.min(Ui.dp(ctx, 216), dm.widthPixels / 2 - Ui.dp(ctx, 18));   // narrower in portrait games
        int h = Math.min(Ui.dp(ctx, 290), dm.heightPixels - Ui.dp(ctx, 40));
        int m = Ui.dp(ctx, 12);
        View l = Panels.left(ctx, eng, p, null);
        View r = Panels.right(ctx, eng, p, true, new Runnable() {
            @Override public void run() { closePanels(); }
        }, null);
        try {
            wm.addView(l, params(w, h, Gravity.START | Gravity.CENTER_VERTICAL, m, true));
            left = l;
            wm.addView(r, params(w, h, Gravity.END | Gravity.CENTER_VERTICAL, m, true));
            right = r;
        } catch (Exception e) {
            closePanels();
        }
    }

    private void closePanels() {
        remove(left);
        remove(right);
        left = right = null;
        addHandle();
    }

    private void remove(View v) {
        if (v == null) return;
        try { wm.removeView(v); } catch (Exception ignored) { }
    }
}
