package com.htsr.turbo;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

/** One game's profile: the same two glass panels as the in-game toolbar, with a crosshair preview in the middle. */
public class ProfileActivity extends Activity {
    private Cfg p;
    private XhView preview;
    private float density;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        String pkg = getIntent().getStringExtra("pkg");
        if (pkg == null) { finish(); return; }
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        p = new Cfg(this).profile(pkg);
        Engine eng = Engine.get(this);
        density = getResources().getDisplayMetrics().density;

        String label = pkg;
        try {
            PackageManager pm = getPackageManager();
            label = String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
        } catch (PackageManager.NameNotFoundException ignored) { }

        FrameLayout root = new FrameLayout(this);
        root.setBackground(Ui.background(this));
        root.setFitsSystemWindows(true);

        // crosshair preview
        FrameLayout box = new FrameLayout(this);
        box.setBackground(Ui.glass(this, 26));
        preview = new XhView(this);
        box.addView(preview, new FrameLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40), Gravity.CENTER));
        TextView cap = Ui.text(this, "PREVIEW", 11, Ui.DIM);
        cap.setLetterSpacing(0.12f);
        box.addView(cap, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));
        ((FrameLayout.LayoutParams) cap.getLayoutParams()).bottomMargin = Ui.dp(this, 10);
        root.addView(box, new FrameLayout.LayoutParams(Ui.dp(this, 200), Ui.dp(this, 150), Gravity.CENTER));

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int h = Math.min(Ui.dp(this, 290), dm.heightPixels - Ui.dp(this, 130));
        int w = Ui.dp(this, 216), m = Ui.dp(this, 20);
        Panels.Changed ch = new Panels.Changed() {
            @Override public void on() { updatePreview(); }
        };
        FrameLayout.LayoutParams llp = new FrameLayout.LayoutParams(w, h, Gravity.START | Gravity.CENTER_VERTICAL);
        llp.leftMargin = m;
        root.addView(Panels.left(this, eng, p, ch), llp);
        FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(w, h, Gravity.END | Gravity.CENTER_VERTICAL);
        rlp.rightMargin = m;
        root.addView(Panels.right(this, eng, p, false, null, ch), rlp);

        // top bar
        TextView back = Ui.text(this, "\u2039", 26, Ui.TEXT);
        back.setGravity(Gravity.CENTER);
        back.setBackground(Ui.glass(this, 20));
        back.setContentDescription("Back");
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40), Gravity.TOP | Gravity.START);
        blp.leftMargin = m;
        blp.topMargin = Ui.dp(this, 8);
        root.addView(back, blp);

        TextView title = Ui.text(this, label + "  \u00B7  profile", 13, Ui.TEXT);
        Ui.bold(title);
        title.setBackground(Ui.glass(this, 20));
        title.setPadding(Ui.dp(this, 18), Ui.dp(this, 10), Ui.dp(this, 18), Ui.dp(this, 10));
        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        tlp.topMargin = Ui.dp(this, 8);
        root.addView(title, tlp);

        TextView done = Ui.text(this, "Done", 13, Ui.TEXT);
        Ui.bold(done);
        done.setGravity(Gravity.CENTER);
        done.setBackground(Ui.glass(this, 20));
        done.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(Ui.dp(this, 84), Ui.dp(this, 40), Gravity.TOP | Gravity.END);
        dlp.rightMargin = m;
        dlp.topMargin = Ui.dp(this, 8);
        root.addView(done, dlp);

        setContentView(root);
        updatePreview();
    }

    private void updatePreview() {
        preview.load(p, density);
        int sz = Math.min(p.i("xsize", 40), 100);
        preview.setLayoutParams(new FrameLayout.LayoutParams(Ui.dp(this, sz), Ui.dp(this, sz), Gravity.CENTER));
    }
}
