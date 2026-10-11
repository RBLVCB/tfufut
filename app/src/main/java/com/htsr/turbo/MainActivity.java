package com.htsr.turbo;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Home: pick a game, open its profile. Always landscape. */
public class MainActivity extends Activity {
    private Cfg cfg;
    private Engine eng;
    private final Handler ui = new Handler();
    private TextView chip, enable;
    private LinearLayout cards;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.immersive(this);
        cfg = new Cfg(this);
        eng = Engine.get(this);
        eng.start();

        FrameLayout root = new FrameLayout(this);
        root.setBackground(Ui.background(this));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(this, 48), Ui.dp(this, 24), Ui.dp(this, 48), Ui.dp(this, 16));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = Ui.text(this, "HTSR Turbo", 24, Ui.TEXT);
        Ui.bold(title);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        enable = Ui.button(this, "Enable (root)", true);
        enable.setTextSize(12);
        enable.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { enableService(); }
        });
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        elp.rightMargin = Ui.dp(this, 10);
        head.addView(enable, elp);
        chip = Ui.text(this, "", 12, Ui.TEXT);
        chip.setBackground(Ui.glass(this, 20));
        chip.setPadding(Ui.dp(this, 16), Ui.dp(this, 9), Ui.dp(this, 16), Ui.dp(this, 9));
        head.addView(chip);
        col.addView(head);

        TextView lab = Ui.text(this, "GAMES", 11, Ui.DIM);
        lab.setLetterSpacing(0.12f);
        lab.setPadding(0, Ui.dp(this, 22), 0, Ui.dp(this, 10));
        col.addView(lab);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setClipChildren(false);
        cards = new LinearLayout(this);
        cards.setOrientation(LinearLayout.HORIZONTAL);
        hs.addView(cards);
        col.addView(hs);

        TextView hint = Ui.text(this, "Tap a game to edit its profile. The same settings open from the toolbar inside the game.",
                12, Ui.DIM);
        hint.setPadding(0, Ui.dp(this, 14), 0, 0);
        col.addView(hint);

        root.addView(col, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Ui.immersive(this);
    }

    @Override protected void onResume() {
        super.onResume();
        refreshCards();
        ui.post(tick);
    }

    @Override protected void onPause() {
        super.onPause();
        ui.removeCallbacks(tick);
    }

    // refreshes the status chip only while this screen is open
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            boolean on = eng.serviceConnected();
            chip.setText(on ? "Service on" : "Service off");
            chip.setTextColor(on ? Ui.TEXT : 0xFFFFB4A8);
            enable.setVisibility(on ? View.GONE : View.VISIBLE);
            ui.postDelayed(this, 1500);
        }
    };

    private void enableService() {
        new Thread(new Runnable() {
            @Override public void run() {
                final int rc = Root.setup();
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        Panels.toast(MainActivity.this, rc == 0 ? "Enabled" : "Root was not granted");
                    }
                });
            }
        }).start();
    }

    private void refreshCards() {
        cards.removeAllViews();
        PackageManager pm = getPackageManager();
        List<String> games = new ArrayList<>(cfg.games());
        Collections.sort(games);
        for (String pkg : games) {
            String label;
            Drawable icon;
            try {
                label = String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
                icon = pm.getApplicationIcon(pkg);
            } catch (PackageManager.NameNotFoundException e) {
                continue;   // uninstalled
            }
            cards.addView(card(pkg, label, icon), cardLp());
        }
        cards.addView(addCard(), cardLp());
    }

    private LinearLayout.LayoutParams cardLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 190), Ui.dp(this, 200));
        lp.rightMargin = Ui.dp(this, 12);
        return lp;
    }

    private View card(final String pkg, String label, Drawable icon) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(Ui.glass(this, 26));
        c.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18));
        c.setClickable(true);
        c.setContentDescription(label + " profile");
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(icon);
        c.addView(iv, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 52)));
        c.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));
        TextView n = Ui.text(this, label, 15, Ui.TEXT);
        Ui.bold(n);
        n.setSingleLine(true);
        c.addView(n);
        TextView s = Ui.text(this, summary(cfg.profile(pkg)), 12, Ui.DIM);
        s.setMaxLines(2);
        c.addView(s);
        c.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class).putExtra("pkg", pkg));
            }
        });
        return c;
    }

    private View addCard() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setBackground(Ui.glass(this, 26));
        c.setClickable(true);
        c.setContentDescription("Add game");
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cl.gravity = Gravity.CENTER_HORIZONTAL;
        c.addView(Ui.text(this, "+", 32, Ui.DIM), cl);
        c.addView(Ui.text(this, "Add game", 13, Ui.DIM), cl);
        c.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, GamesActivity.class));
            }
        });
        return c;
    }

    private static String summary(Cfg p) {
        List<String> parts = new ArrayList<>();
        if (p.s("hmode", "on").equals("off")) {
            parts.add("Touch off");
        } else {
            String m = p.s("tmode", "classic");
            parts.add(m.equals("pro") ? "Pro touch" : m.equals("custom") ? "Custom touch" : "Classic touch");
        }
        if (p.s("xmode", "off").equals("on")) parts.add("Crosshair");
        if (p.s("cmode", "off").equals("on")) parts.add("Saturation");
        if (GameTurbo.mask(p) != 0) parts.add("Turbo");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) sb.append(i > 0 ? " \u00B7 " : "").append(parts.get(i));
        return sb.toString();
    }
}
