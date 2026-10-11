package com.htsr.turbo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Runs once after boot (no loops): re-applies the root setup, then exits. Replaces the old boot module. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(final Context c, Intent i) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) return;
        final PendingResult pr = goAsync();
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    Thread.sleep(5000);      // let the system settle once
                    Root.setup();
                } catch (InterruptedException ignored) {
                } finally {
                    pr.finish();
                }
            }
        }).start();
    }
}
