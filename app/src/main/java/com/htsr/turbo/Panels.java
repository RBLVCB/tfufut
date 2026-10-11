package com.htsr.turbo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The two glass panels. The same code is used on the game profile screen and in the in-game toolbar,
 * so both always show the same settings. Everything is read from / written to one game's profile.
 */
final class Panels {
    interface Changed { void on(); }

    static final int[] SWATCHES = {0xFF3B30, 0xFF9500, 0xFFD60A, 0x30D158, 0x00E5FF, 0x2F8CFF, 0xBF5AF2, 0xFFFFFF};
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Panels() {}

    static void toast(final Context c, final String s) {
        MAIN.post(new Runnable() {
            @Override public void run() { Toast.makeText(c, s, Toast.LENGTH_SHORT).show(); }
        });
    }

    private static LinearLayout shell(Context c) {
        LinearLayout b = new LinearLayout(c);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setBackground(Ui.glass(c, 26));
        int p = Ui.dp(c, 14);
        b.setPadding(p, p, p, p);
        return b;
    }

    private static LinearLayout.LayoutParams top(Context c, int dp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(c, dp);
        return lp;
    }

    private static TextView label(Context c, String s) {
        TextView t = Ui.text(c, s.toUpperCase(), 11, Ui.DIM);
        t.setLetterSpacing(0.12f);
        return t;
    }

    private static void fire(Changed ch) { if (ch != null) ch.on(); }

    // ================================================================ left: touch + display

    static View left(final Context c, final Engine eng, final Cfg p, final Changed ch) {
        LinearLayout box = shell(c);
        final LinearLayout body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(body);

        final String[] tab = {"t"};
        final Runnable[] fill = new Runnable[1];
        fill[0] = new Runnable() {
            @Override public void run() {
                body.removeAllViews();
                if (tab[0].equals("t")) touchTab(c, eng, p, body, fill[0]);
                else displayTab(c, eng, p, ch, body);
            }
        };
        box.addView(new Ui.Seg(c, new String[]{"Touch", "Display"}, new String[]{"t", "d"}, "t", new Ui.KeyCb() {
            @Override public void on(String k) { tab[0] = k; fill[0].run(); }
        }));
        box.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        fill[0].run();
        return box;
    }

    private static void touchTab(final Context c, final Engine eng, final Cfg p, LinearLayout body, final Runnable refill) {
        body.addView(label(c, "Touch boost"), top(c, 14));
        body.addView(new Ui.Seg(c, new String[]{"On", "Off"}, new String[]{"on", "off"}, p.s("hmode", "on"),
                new Ui.KeyCb() {
                    @Override public void on(String k) { p.put("hmode", k); eng.onConfig(Engine.W_GT); }
                }), top(c, 6));
        String m = p.s("tmode", "classic");
        body.addView(new Ui.Seg(c, new String[]{"Classic", "Pro", "Custom"}, new String[]{"classic", "pro", "custom"}, m,
                new Ui.KeyCb() {
                    @Override public void on(String k) {
                        p.put("tmode", k);
                        eng.onConfig(Engine.W_TOUCH);
                        refill.run();
                    }
                }), top(c, 8));
        if (m.equals("pro")) {
            body.addView(label(c, "Preset"), top(c, 14));
            body.addView(new Ui.Seg(c, new String[]{"1", "2", "3"}, new String[]{"1", "2", "3"},
                    String.valueOf(p.i("pro", 1)), new Ui.KeyCb() {
                        @Override public void on(String k) {
                            p.put("pro", Integer.parseInt(k));
                            eng.onConfig(Engine.W_TOUCH);
                        }
                    }), top(c, 6));
            return;
        }
        boolean custom = m.equals("custom");
        body.addView(label(c, custom ? "Custom levels" : "Default levels"), top(c, 14));
        tSlider(c, eng, p, body, "Tap", "up", "dup", custom);
        tSlider(c, eng, p, body, "Swipe", "tol", "dtol", custom);
        tSlider(c, eng, p, body, "Aim", "aim", "daim", custom);
        tSlider(c, eng, p, body, "Steady", "stab", "dstab", custom);
    }

    private static void tSlider(Context c, final Engine eng, final Cfg p, LinearLayout body, String label,
                                final String key, String defKey, boolean custom) {
        int v = custom ? p.i(key, 3) : eng.cfg.i(defKey, 3);
        body.addView(Ui.slider(c, label, 54, 24, 1, 5, 1, v, "", null, new Ui.IntCb() {
            @Override public void on(int x) { p.put(key, x); eng.onConfig(Engine.W_TOUCH); }
        }, custom));
    }

    private static void displayTab(final Context c, final Engine eng, final Cfg p, final Changed ch, LinearLayout body) {
        body.addView(label(c, "Crosshair"), top(c, 14));
        body.addView(new Ui.Seg(c, new String[]{"On", "Off"}, new String[]{"on", "off"}, p.s("xmode", "off"),
                new Ui.KeyCb() {
                    @Override public void on(String k) { p.put("xmode", k); fire(ch); eng.onConfig(Engine.W_XH); }
                }), top(c, 6));
        body.addView(new Ui.Seg(c, new String[]{"Cross", "Cross+", "Dot", "Ring"},
                new String[]{"cross", "crossdot", "dot", "circle"}, p.s("xstyle", "cross"), new Ui.KeyCb() {
                    @Override public void on(String k) { p.put("xstyle", k); fire(ch); eng.onConfig(Engine.W_XH); }
                }), top(c, 8));

        final View[] sw = new View[SWATCHES.length];
        final Runnable paint = new Runnable() {
            @Override public void run() {
                int cur = p.i("xcolor", 0xFF3B30) & 0xFFFFFF;
                for (int i = 0; i < sw.length; i++) {
                    boolean sel = SWATCHES[i] == cur;
                    sw[i].setBackground(Ui.box(c, 0xFF000000 | SWATCHES[i], sel ? 0xFFFFFFFF : 0x33FFFFFF, 99));
                }
            }
        };
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int i = r * 4; i < r * 4 + 4; i++) {
                final int rgb = SWATCHES[i];
                View v = new View(c);
                v.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View x) {
                        p.put("xcolor", rgb);
                        paint.run();
                        fire(ch);
                        eng.onConfig(Engine.W_XH);
                    }
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(c, 30), 1f);
                lp.setMargins(Ui.dp(c, 4), Ui.dp(c, 4), Ui.dp(c, 4), Ui.dp(c, 4));
                row.addView(v, lp);
                sw[i] = v;
            }
            body.addView(row, top(c, r == 0 ? 8 : 0));
        }
        paint.run();

        xs(c, eng, p, ch, body, "Size", "xsize", 8, 120, 40);
        xs(c, eng, p, ch, body, "Thick", "xthick", 1, 10, 3);
        xs(c, eng, p, ch, body, "Gap", "xgap", 0, 40, 6);
        xs(c, eng, p, ch, body, "Opacity", "xalpha", 10, 100, 90);
        xs(c, eng, p, ch, body, "Move X", "xox", -200, 200, 0);
        xs(c, eng, p, ch, body, "Move Y", "xoy", -200, 200, 0);

        body.addView(label(c, "Saturation"), top(c, 16));
        body.addView(new Ui.Seg(c, new String[]{"On", "Off"}, new String[]{"on", "off"}, p.s("cmode", "off"),
                new Ui.KeyCb() {
                    @Override public void on(String k) { p.put("cmode", k); eng.onConfig(Engine.W_COLOR); }
                }), top(c, 6));
        body.addView(Ui.slider(c, "Level", 54, 44, 100, 200, 5, p.i("cpct", 125), "%", null, new Ui.IntCb() {
            @Override public void on(int v) { p.put("cpct", v); eng.onConfig(Engine.W_COLOR); }
        }, true), top(c, 4));
        TextView reset = Ui.button(c, "Reset colors now", false);
        reset.setTextSize(12);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { eng.resetColor(); toast(c, "Colors reset"); }
        });
        body.addView(reset, top(c, 8));
    }

    private static void xs(Context c, final Engine eng, final Cfg p, final Changed ch, LinearLayout body,
                           String label, final String key, int min, int max, int def) {
        body.addView(Ui.slider(c, label, 54, 36, min, max, 1, p.i(key, def), "", new Ui.IntCb() {
            @Override public void on(int v) { p.put(key, v); fire(ch); eng.onConfig(Engine.W_XH); }
        }, null, true));
    }

    // ================================================================ right: turbo + actions

    static View right(final Context c, final Engine eng, final Cfg p, final boolean inGame,
                      final Runnable close, final Changed ch) {
        LinearLayout box = shell(c);

        LinearLayout head = new LinearLayout(c);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(label(c, "Turbo"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (close != null) {
            TextView x = Ui.text(c, "\u2715", 16, Ui.TEXT);
            x.setGravity(Gravity.CENTER);
            x.setContentDescription("Close toolbar");
            x.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { close.run(); }
            });
            head.addView(x, new LinearLayout.LayoutParams(Ui.dp(c, 40), Ui.dp(c, 40)));
        }
        box.addView(head);

        if (inGame) {
            final TextView stats = Ui.text(c, "Reading\u2026", 12, Ui.DIM);
            box.addView(stats);
            eng.post(new Runnable() {
                @Override public void run() {
                    final String s = GameTurbo.stats();      // one reading when the panel opens
                    MAIN.post(new Runnable() { @Override public void run() { stats.setText(s); } });
                }
            });
        }

        LinearLayout list = new LinearLayout(c);
        list.setOrientation(LinearLayout.VERTICAL);
        tg(c, eng, p, list, "Do not disturb", "gt_dnd", 1);
        tg(c, eng, p, list, "Free RAM on start", "gt_ram", 1);
        tg(c, eng, p, list, "Performance mode", "gt_perf", 0);
        tg(c, eng, p, list, "Lock brightness", "gt_bright", 0);
        tg(c, eng, p, list, "Keep screen on", "gt_awake", 0);
        tg(c, eng, p, list, "Wi-Fi low latency", "gt_net", 0);
        if (!inGame) tg(c, eng, p, list, "Toolbar in game", "gt_tool", 1);
        ScrollView sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(list);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        slp.topMargin = Ui.dp(c, 6);
        box.addView(sv, slp);

        if (inGame) box.addView(actions(c, eng, close), top(c, 8));
        return box;
    }

    private static void tg(Context c, final Engine eng, final Cfg p, LinearLayout list, String label,
                           final String key, int def) {
        list.addView(Ui.toggleRow(c, label, p.i(key, def) == 1, new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton b, boolean on) {
                p.put(key, on ? 1 : 0);
                eng.onConfig(Engine.W_GT);
            }
        }));
    }

    private static LinearLayout actions(final Context c, final Engine eng, final Runnable close) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        IconBtn shot = new IconBtn(c, IconBtn.CAM, "Screenshot");
        shot.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (close != null) close.run();            // so the panels are not in the picture
                eng.post(new Runnable() {
                    @Override public void run() {
                        try { Thread.sleep(400); } catch (InterruptedException ignored) { }
                        String n = GameTurbo.screenshot();
                        toast(c, n != null ? "Saved Pictures/" + n : "Screenshot failed");
                    }
                });
            }
        });

        final IconBtn rec = new IconBtn(c, IconBtn.REC, "Record");
        rec.setActive(GameTurbo.recording);
        rec.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (GameTurbo.recording) {
                    eng.post(new Runnable() { @Override public void run() { GameTurbo.stopRecord(); } });
                    rec.setActive(false);
                    return;
                }
                if (close != null) close.run();
                toast(c, "Recording (max 3 min). Open the toolbar and tap the record button to stop.");
                GameTurbo.startRecord(new GameTurbo.RecDone() {
                    @Override public void done(String name) { toast(c, "Saved Movies/" + name); }
                });
            }
        });

        IconBtn ram = new IconBtn(c, IconBtn.BOLT, "Free RAM now");
        ram.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                eng.post(new Runnable() {
                    @Override public void run() {
                        int a = GameTurbo.memFreeMb();
                        GameTurbo.freeRam();
                        toast(c, "Free RAM " + a + " \u2192 " + GameTurbo.memFreeMb() + " MB");
                    }
                });
            }
        });

        int s = Ui.dp(c, 46);
        row.addView(shot, new LinearLayout.LayoutParams(s, s));
        row.addView(new View(c), new LinearLayout.LayoutParams(0, 1, 1f));
        row.addView(rec, new LinearLayout.LayoutParams(s, s));
        row.addView(new View(c), new LinearLayout.LayoutParams(0, 1, 1f));
        row.addView(ram, new LinearLayout.LayoutParams(s, s));
        return row;
    }

    /** Round glass button with a hand-drawn icon. */
    static final class IconBtn extends View {
        static final int CAM = 0, REC = 1, BOLT = 2;
        private final int type;
        private boolean active;
        private final Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final float d;

        IconBtn(Context c, int type, String desc) {
            super(c);
            this.type = type;
            d = c.getResources().getDisplayMetrics().density;
            setContentDescription(desc);
            setBackground(Ui.glass(c, 23));
            setClickable(true);
            pt.setStrokeCap(Paint.Cap.ROUND);
            pt.setStrokeJoin(Paint.Join.ROUND);
            pt.setStrokeWidth(1.8f * d);
        }

        void setActive(boolean a) { active = a; invalidate(); }

        @Override protected void onDraw(Canvas cv) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            pt.setStyle(Paint.Style.STROKE);
            pt.setColor(0xFFFFFFFF);
            if (type == CAM) {
                cv.drawRoundRect(new RectF(cx - 9 * d, cy - 6 * d, cx + 9 * d, cy + 7 * d), 3 * d, 3 * d, pt);
                cv.drawCircle(cx, cy + 0.5f * d, 3.5f * d, pt);
            } else if (type == REC) {
                cv.drawCircle(cx, cy, 8 * d, pt);
                pt.setStyle(Paint.Style.FILL);
                pt.setColor(active ? 0xFFFF453A : 0xFFFFFFFF);
                cv.drawCircle(cx, cy, 3.5f * d, pt);
            } else {
                path.reset();
                path.moveTo(cx + 1 * d, cy - 9 * d);
                path.lineTo(cx - 6 * d, cy + 2 * d);
                path.lineTo(cx, cy + 2 * d);
                path.lineTo(cx - 1 * d, cy + 9 * d);
                path.lineTo(cx + 6 * d, cy - 2 * d);
                path.lineTo(cx, cy - 2 * d);
                path.close();
                cv.drawPath(path, pt);
            }
        }
    }
}
