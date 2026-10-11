package com.htsr.turbo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

/**
 * The four-axis touch chart. Order of values: swipe response, continuous-touch sensitivity,
 * tap stability, aiming accuracy (clockwise from top-left). Levels 1-5.
 */
final class RadarView extends View {
    interface Listener {
        void onCommit(int[] v);
    }

    private static final float S = 0.7071f;
    private static final float[][] U = {{-S, -S}, {S, -S}, {S, S}, {-S, S}};
    private static final String[][] LABELS = {
            {"Swipe response"}, {"Sensitivity to", "continuous touch"}, {"Tap stability"}, {"Aiming accuracy"}};

    final int[] vals = {3, 3, 3, 3};
    boolean editable;
    Listener listener;

    private final float d;
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axis = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint num = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float cx, cy, r;
    private int drag = -1;

    RadarView(Context c) {
        super(c);
        d = c.getResources().getDisplayMetrics().density;
        ring.setStyle(Paint.Style.STROKE); ring.setStrokeWidth(d); ring.setColor(0xFF1D3050);
        axis.setStyle(Paint.Style.STROKE); axis.setStrokeWidth(d); axis.setColor(0xFF1D3050);
        fill.setStyle(Paint.Style.FILL); fill.setColor(0x472F8CFF);
        line.setStyle(Paint.Style.STROKE); line.setStrokeWidth(1.5f * d); line.setColor(0xFF2F8CFF);
        dot.setStyle(Paint.Style.FILL);
        label.setColor(0xFFCFD9EA); label.setTextSize(12.5f * d);
        num.setColor(0xFF5FB0FF); num.setTextSize(11f * d); num.setTextAlign(Paint.Align.CENTER);
        num.setFakeBoldText(true);
    }

    void setValues(int a, int b, int c, int e) {
        vals[0] = a; vals[1] = b; vals[2] = c; vals[3] = e;
        invalidate();
    }

    @Override protected void onMeasure(int w, int h) {
        int width = MeasureSpec.getSize(w);
        setMeasuredDimension(width, (int) (width * 0.92f));
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        cx = w / 2f;
        cy = h / 2f;
        r = Math.min(w, h) * 0.30f;
    }

    private float px(int i) { return cx + U[i][0] * r * vals[i] / 5f; }
    private float py(int i) { return cy + U[i][1] * r * vals[i] / 5f; }

    @Override protected void onDraw(Canvas c) {
        for (int k = 1; k <= 5; k++) c.drawCircle(cx, cy, r * k / 5f, ring);
        for (int i = 0; i < 4; i++) {
            c.drawLine(cx, cy, cx + U[i][0] * r * 1.12f, cy + U[i][1] * r * 1.12f, axis);
        }
        Path p = new Path();
        for (int i = 0; i < 4; i++) {
            if (i == 0) p.moveTo(px(i), py(i)); else p.lineTo(px(i), py(i));
        }
        p.close();
        fill.setAlpha(editable ? 0x47 : 0x30);
        c.drawPath(p, fill);
        c.drawPath(p, line);
        dot.setColor(editable ? 0xFF2F8CFF : 0xFF3D6FB3);
        for (int i = 0; i < 4; i++) {
            c.drawCircle(px(i), py(i), 6f * d, dot);
            c.drawText(String.valueOf(vals[i]), px(i), py(i) - 12f * d, num);
        }
        float w = getWidth(), h = getHeight(), line1 = 15f * d, pad = 10f * d;
        drawLabel(c, LABELS[0], pad, 22f * d, false);
        drawLabel(c, LABELS[1], w - pad, 22f * d, true);
        drawLabel(c, LABELS[2], w - pad, h - pad - line1 * (LABELS[2].length - 1), true);
        drawLabel(c, LABELS[3], pad, h - pad - line1 * (LABELS[3].length - 1), false);
    }

    private void drawLabel(Canvas c, String[] lines, float x, float y, boolean right) {
        label.setTextAlign(right ? Paint.Align.RIGHT : Paint.Align.LEFT);
        for (int i = 0; i < lines.length; i++) c.drawText(lines[i], x, y + i * 15f * d, label);
    }

    private int nearest(float x, float y) {
        int best = -1;
        float bd = 56f * d;
        for (int i = 0; i < 4; i++) {
            float dx = x - px(i), dy = y - py(i);
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < bd) { bd = dist; best = i; }
        }
        return best;
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (!editable) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                drag = nearest(e.getX(), e.getY());
                if (drag < 0) return false;
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (drag < 0) return false;
                float proj = (e.getX() - cx) * U[drag][0] + (e.getY() - cy) * U[drag][1];
                int lv = Math.max(1, Math.min(5, Math.round(proj / r * 5f)));
                if (lv != vals[drag]) { vals[drag] = lv; invalidate(); }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (drag >= 0) {
                    drag = -1;
                    if (listener != null) listener.onCommit(vals);
                }
                return true;
            default:
                return true;
        }
    }
}
