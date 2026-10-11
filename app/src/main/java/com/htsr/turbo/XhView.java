package com.htsr.turbo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Draws the crosshair. Sizes are in px. */
final class XhView extends View {
    String style = "cross";
    int color = 0xFFFF3B30;
    int thick = 6;
    int gap = 12;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    XhView(Context c) { super(c); }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float r = Math.min(w, h) / 2f - thick;
        p.setColor(color);

        if (style.equals("dot")) {
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(cx, cy, Math.max(thick, r / 6f), p);
            return;
        }
        if (style.equals("circle")) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(thick);
            c.drawCircle(cx, cy, r, p);
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(cx, cy, Math.max(thick / 1.5f, 2f), p);
            return;
        }
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(thick);
        c.drawLine(cx - r, cy, cx - gap, cy, p);
        c.drawLine(cx + gap, cy, cx + r, cy, p);
        c.drawLine(cx, cy - r, cx, cy - gap, p);
        c.drawLine(cx, cy + gap, cx, cy + r, p);
        if (style.equals("crossdot")) {
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(cx, cy, Math.max(thick / 1.5f, 2f), p);
        }
    }

    /** Copies the saved crosshair settings into this view. d = screen density. */
    void load(Cfg cfg, float d) {
        style = cfg.s("xstyle", "cross");
        int rgb = cfg.i("xcolor", 0xFF3B30) & 0xFFFFFF;
        int a = Math.max(10, Math.min(100, cfg.i("xalpha", 90))) * 255 / 100;
        color = (a << 24) | rgb;
        thick = Math.max(1, Math.round(cfg.i("xthick", 3) * d));
        gap = Math.round(cfg.i("xgap", 6) * d);
        invalidate();
    }
}
