package com.htsr.turbo;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** All settings live here (SharedPreferences). */
final class Cfg {
    static final String[] DEFAULT_GAMES = {
            "com.tencent.ig", "com.activision.callofduty.shooter", "com.mobile.legends"};

    private final SharedPreferences p;
    private final String pre;

    Cfg(Context c) {
        this(c.getSharedPreferences("cfg", Context.MODE_PRIVATE), "");
    }

    private Cfg(SharedPreferences p, String pre) {
        this.p = p;
        this.pre = pre;
    }

    /** The settings of one game: same keys, stored under that game's package name. */
    Cfg profile(String pkg) { return new Cfg(p, "g." + pkg + "."); }

    String s(String k, String d) { return p.getString(pre + k, d); }
    int i(String k, int d) { return p.getInt(pre + k, d); }
    void put(String k, String v) { p.edit().putString(pre + k, v).apply(); }
    void put(String k, int v) { p.edit().putInt(pre + k, v).apply(); }

    Set<String> games() {
        Set<String> g = p.getStringSet("games", null);
        if (g == null) return new HashSet<>(Arrays.asList(DEFAULT_GAMES));
        return new HashSet<>(g);
    }

    void setGames(Set<String> g) {
        p.edit().putStringSet("games", new HashSet<>(g)).apply();
    }
}
