package com.htsr.turbo;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/** Small helpers so the screens can be built in code. Liquid-glass look: translucent fills, light edges. */
final class Ui {
    static final int BG = 0xFF070B16, PANEL = 0x1FFFFFFF, LINE = 0x29FFFFFF;
    static final int TEXT = 0xF0FFFFFF, DIM = 0x99FFFFFF, ACC = 0xFF2F8CFF, ACC2 = 0xFF8FC3FF, FILL = 0x592F8CFF;

    private Ui() {}

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable box(Context c, int fill, int stroke, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        if (stroke != 0) g.setStroke(Math.max(1, dp(c, 1)), stroke);
        return g;
    }

    /** Frosted glass: a light diagonal sheen with a thin bright edge. */
    static GradientDrawable glass(Context c, float radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0x2EFFFFFF, 0x0FFFFFFF});
        g.setCornerRadius(dp(c, radiusDp));
        g.setStroke(Math.max(1, dp(c, 1)), 0x29FFFFFF);
        return g;
    }

    /** Selected state: a tinted blue glass. */
    static GradientDrawable selected(Context c, float radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x808FC3FF, 0x592F8CFF});
        g.setCornerRadius(dp(c, radiusDp));
        g.setStroke(Math.max(1, dp(c, 1)), 0x40FFFFFF);
        return g;
    }

    /** Dark base with three soft colour glows, so the glass has something to refract. */
    static Drawable background(Context c) {
        Drawable[] l = new Drawable[4];
        l[0] = new GradientDrawable();
        ((GradientDrawable) l[0]).setColor(BG);
        l[1] = glow(c, 0x992F8CFF, 0.05f, 0.0f, 360);
        l[2] = glow(c, 0x807C5CFF, 0.95f, 0.7f, 360);
        l[3] = glow(c, 0x4D19D3C5, 0.4f, 1.1f, 320);
        return new LayerDrawable(l);
    }

    private static GradientDrawable glow(Context c, int color, float cx, float cy, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setColors(new int[]{color, color & 0x00FFFFFF});
        g.setGradientCenter(cx, cy);
        g.setGradientRadius(dp(c, radiusDp));
        return g;
    }

    static TextView text(Context c, String s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    /** A glass panel with a small uppercase title; returns the panel to add content to. */
    static LinearLayout section(Context c, LinearLayout parent, String title) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(glass(c, 22));
        int p = dp(c, 14);
        box.setPadding(p, p, p, p);
        TextView t = text(c, title.toUpperCase(), 11, DIM);
        t.setLetterSpacing(0.12f);
        t.setPadding(0, 0, 0, dp(c, 10));
        box.addView(t);
        LinearLayout.LayoutParams lp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 14);
        parent.addView(box, lp);
        return box;
    }

    static TextView button(Context c, String s, boolean selected) {
        TextView t = text(c, s, 14, selected ? TEXT : DIM);
        t.setGravity(Gravity.CENTER);
        int ph = dp(c, 14), pv = dp(c, 10);
        t.setPadding(ph, pv, ph, pv);
        style(c, t, selected);
        return t;
    }

    static void style(Context c, TextView t, boolean selected) {
        t.setTextColor(selected ? 0xFFFFFFFF : DIM);
        t.setBackground(selected ? selected(c, 12) : box(c, 0x12FFFFFF, 0, 12));
    }

    // ---------------------------------------------------------------- segmented control

    interface KeyCb { void on(String key); }

    static final class Seg extends LinearLayout {
        private final String[] keys;
        private final TextView[] views;
        private final Context ctx;

        Seg(Context c, String[] labels, String[] keys, String current, final KeyCb cb) {
            super(c);
            ctx = c;
            this.keys = keys;
            views = new TextView[keys.length];
            setOrientation(HORIZONTAL);
            setBackground(box(c, 0x12FFFFFF, 0, 14));
            int pad = dp(c, 3);
            setPadding(pad, pad, pad, pad);
            for (int i = 0; i < keys.length; i++) {
                final String k = keys[i];
                TextView b = text(c, labels[i], 12, DIM);
                b.setGravity(Gravity.CENTER);
                b.setPadding(dp(c, 3), dp(c, 11), dp(c, 3), dp(c, 11));
                b.setSingleLine(true);
                segStyle(b, k.equals(current));
                b.setOnClickListener(new OnClickListener() {
                    @Override public void onClick(View v) { set(k); cb.on(k); }
                });
                LayoutParams lp = new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                if (i > 0) lp.leftMargin = dp(c, 3);
                addView(b, lp);
                views[i] = b;
            }
        }

        private void segStyle(TextView b, boolean sel) {
            b.setTextColor(sel ? 0xFFFFFFFF : DIM);
            b.setBackground(sel ? selected(ctx, 11) : box(ctx, 0, 0, 11));
        }

        void set(String key) {
            for (int i = 0; i < keys.length; i++) segStyle(views[i], keys[i].equals(key));
        }
    }

    // ---------------------------------------------------------------- labelled slider

    interface IntCb { void on(int v); }

    static LinearLayout slider(Context c, String label, int min, int max, int step,
                               int value, String unit, IntCb onChange, IntCb onCommit) {
        return slider(c, label, 96, 52, min, max, step, value, unit, onChange, onCommit, true);
    }

    /** label | seekbar | value. real value = min + progress * step. */
    static LinearLayout slider(Context c, String label, int labelDp, int valueDp, final int min, int max,
                               final int step, int value, final String unit, final IntCb onChange,
                               final IntCb onCommit, boolean enabled) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(c, 40));

        TextView l = text(c, label, 12, DIM);
        row.addView(l, new LinearLayout.LayoutParams(dp(c, labelDp), ViewGroup.LayoutParams.WRAP_CONTENT));

        final TextView val = text(c, "", 12, ACC2);
        val.setGravity(Gravity.END);
        SeekBar sb = new SeekBar(c);
        sb.setMax((max - min) / step);
        sb.setProgress((value - min) / step);
        sb.setProgressTintList(ColorStateList.valueOf(ACC2));
        sb.setProgressBackgroundTintList(ColorStateList.valueOf(0x40FFFFFF));
        sb.setThumbTintList(ColorStateList.valueOf(0xFFFFFFFF));
        sb.setEnabled(enabled);
        row.setAlpha(enabled ? 1f : 0.45f);
        val.setText(value + unit);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                int v = min + p * step;
                val.setText(v + unit);
                if (user && onChange != null) onChange.on(v);
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) {
                if (onCommit != null) onCommit.on(min + s.getProgress() * step);
            }
        });
        row.addView(sb, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(val, new LinearLayout.LayoutParams(dp(c, valueDp), ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    /** Label on the left, glass switch on the right, whole row tappable (44dp tall). */
    static LinearLayout toggleRow(Context c, String label, boolean on, CompoundButton.OnCheckedChangeListener l) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setMinimumHeight(dp(c, 44));
        r.addView(text(c, label, 13, TEXT), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final Switch sw = new Switch(c);
        sw.setChecked(on);
        sw.setThumbTintList(ColorStateList.valueOf(0xFFFFFFFF));
        sw.setTrackTintList(new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}}, new int[]{0xFF2F8CFF, 0x47FFFFFF}));
        sw.setOnCheckedChangeListener(l);
        r.addView(sw);
        r.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { sw.toggle(); }
        });
        return r;
    }

    static void bold(TextView t) {
        t.setTypeface(Typeface.DEFAULT_BOLD);
    }
}
