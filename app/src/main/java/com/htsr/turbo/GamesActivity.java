package com.htsr.turbo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Pick which installed apps count as games. */
public class GamesActivity extends Activity {
    private static final class App {
        String pkg, label;
        Drawable icon;
    }

    private Cfg cfg;
    private final Set<String> selected = new HashSet<>();
    private final List<App> all = new ArrayList<>();
    private final List<App> shown = new ArrayList<>();
    private BaseAdapter adapter;
    private TextView title;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.immersive(this);
        cfg = new Cfg(this);
        selected.addAll(cfg.games());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        int p = Ui.dp(this, 14);
        root.setPadding(Ui.dp(this, 48), Ui.dp(this, 20), Ui.dp(this, 48), 0);

        title = Ui.text(this, "", 18, Ui.TEXT);
        Ui.bold(title);
        root.addView(title);

        EditText search = new EditText(this);
        search.setHint("Search apps");
        search.setHintTextColor(Ui.DIM);
        search.setTextColor(Ui.TEXT);
        search.setSingleLine(true);
        search.setBackground(Ui.box(this, Ui.PANEL, Ui.LINE, 10));
        int sp = Ui.dp(this, 12);
        search.setPadding(sp, sp, sp, sp);
        LinearLayout.LayoutParams slp = Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(this, 10);
        slp.bottomMargin = Ui.dp(this, 8);
        root.addView(search, slp);

        ListView list = new ListView(this);
        list.setDividerHeight(0);
        adapter = new Adapter(this);
        list.setAdapter(adapter);
        root.addView(list, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, 0));
        ((LinearLayout.LayoutParams) list.getLayoutParams()).weight = 1f;

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void afterTextChanged(Editable s) { filter(s.toString()); }
        });

        setContentView(root);
        updateTitle();

        new Thread(new Runnable() {
            @Override public void run() { load(); }
        }).start();
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Ui.immersive(this);
    }

    private void updateTitle() {
        title.setText("Games (" + selected.size() + " selected)");
    }

    private void load() {
        PackageManager pm = getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> ris = pm.queryIntentActivities(i, 0);
        final List<App> tmp = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo ri : ris) {
            String pkg = ri.activityInfo.packageName;
            if (!seen.add(pkg) || pkg.equals(getPackageName())) continue;
            App a = new App();
            a.pkg = pkg;
            a.label = String.valueOf(ri.loadLabel(pm));
            a.icon = ri.loadIcon(pm);
            tmp.add(a);
        }
        // selected first, then alphabetical
        Collections.sort(tmp, new Comparator<App>() {
            @Override public int compare(App x, App y) {
                boolean sx = selected.contains(x.pkg), sy = selected.contains(y.pkg);
                if (sx != sy) return sx ? -1 : 1;
                return x.label.compareToIgnoreCase(y.label);
            }
        });
        runOnUiThread(new Runnable() {
            @Override public void run() {
                all.clear();
                all.addAll(tmp);
                filter("");
            }
        });
    }

    private void filter(String q) {
        String s = q.toLowerCase(Locale.ROOT).trim();
        shown.clear();
        for (App a : all) {
            if (s.isEmpty() || a.label.toLowerCase(Locale.ROOT).contains(s) || a.pkg.contains(s)) shown.add(a);
        }
        adapter.notifyDataSetChanged();
    }

    private void toggle(App a) {
        if (!selected.remove(a.pkg)) selected.add(a.pkg);
        cfg.setGames(selected);
        updateTitle();
        Engine.get(this).onConfig(Engine.W_HTSR);
        adapter.notifyDataSetChanged();
    }

    private final class Adapter extends BaseAdapter {
        private final Context c;

        Adapter(Context c) { this.c = c; }

        @Override public int getCount() { return shown.size(); }
        @Override public Object getItem(int i) { return shown.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override public View getView(int pos, View convert, ViewGroup parent) {
            final App a = shown.get(pos);
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int pad = Ui.dp(c, 8);
            row.setPadding(0, pad, 0, pad);

            ImageView iv = new ImageView(c);
            iv.setImageDrawable(a.icon);
            row.addView(iv, new LinearLayout.LayoutParams(Ui.dp(c, 40), Ui.dp(c, 40)));

            LinearLayout col = new LinearLayout(c);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setPadding(Ui.dp(c, 12), 0, 0, 0);
            col.addView(Ui.text(c, a.label, 15, Ui.TEXT));
            col.addView(Ui.text(c, a.pkg, 11, Ui.DIM));
            row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            CheckBox cb = new CheckBox(c);
            cb.setChecked(selected.contains(a.pkg));
            cb.setClickable(false);
            row.addView(cb);

            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { toggle(a); }
            });
            return row;
        }
    }
}
