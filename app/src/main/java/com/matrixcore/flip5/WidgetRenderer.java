package com.matrixcore.flip5;

import android.app.ActivityManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Build;
import android.os.SystemClock;
import android.widget.RemoteViews;

import java.util.Locale;

public class WidgetRenderer {
    static final int GREEN = 0xFF35F07A, CYAN = 0xFF3AD0E0, AMBER = 0xFFE8B341, RED = 0xFFFF5F56,
            MUTED = 0xFF7C8E84, TEXT = 0xFFC5D0C8, TRACK = 0xFF16231C;
    static final long T0 = System.currentTimeMillis();

    static double rnd(double n) {
        double v = Math.sin(n * 127.1 + 311.7) * 43758.5453;
        return v - Math.floor(v);
    }

    static double core(int k, double x) {
        double v = 28 + 20 * Math.sin(x / (2.5 + k)) + 11 * Math.sin(x / (0.9 + k * 0.7) + k)
                + 9 * rnd(Math.floor(x * 3) * (k + 1));
        return Math.max(3, Math.min(97, v));
    }

    public static void updateAll(Context ctx) {
        AppWidgetManager m = AppWidgetManager.getInstance(ctx);
        upd(ctx, m, MonitorWidget.class, 0);
        upd(ctx, m, NeofetchWidget.class, 1);
    }

    static void upd(Context ctx, AppWidgetManager m, Class<?> cls, int type) {
        try {
            int[] ids = m.getAppWidgetIds(new ComponentName(ctx, cls));
            if (ids == null || ids.length == 0) return;
            Bitmap b = type == 0 ? monitor(ctx) : neofetch(ctx);
            RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_img);
            rv.setImageViewBitmap(R.id.img, b);
            m.updateAppWidget(ids, rv);
        } catch (Exception ignored) {
        }
    }

    static Paint paint(float size, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(Typeface.create(Typeface.MONOSPACE, bold ? Typeface.BOLD : Typeface.NORMAL));
        p.setTextSize(size);
        return p;
    }

    static void txt(Canvas c, Paint p, String s, float x, float y, int col) {
        p.setColor(col);
        c.drawText(s, x, y, p);
    }

    static void panel(Canvas c, int W, int H) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xEE070A09);
        c.drawRoundRect(new RectF(1, 1, W - 1, H - 1), 26, 26, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2);
        p.setColor(0x6635F07A);
        c.drawRoundRect(new RectF(1, 1, W - 1, H - 1), 26, 26, p);
    }

    static void bar(Canvas c, float x, float y, float w, float h, double pct) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(TRACK);
        c.drawRoundRect(new RectF(x, y, x + w, y + h), 3, 3, p);
        p.setColor(pct < 50 ? GREEN : (pct < 80 ? AMBER : RED));
        c.drawRoundRect(new RectF(x, y, x + (float) (w * pct / 100.0), y + h), 3, 3, p);
    }

    static double[] mem(Context ctx) {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        double total = mi.totalMem / 1073741824.0;
        double used = (mi.totalMem - mi.availMem) / 1073741824.0;
        return new double[]{used, total};
    }

    static Bitmap monitor(Context ctx) {
        int W = 440, H = 200;
        Bitmap b = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        panel(c, W, H);
        double sec = (System.currentTimeMillis() - T0) / 1000.0 + 50;
        Paint p = paint(15f, true);
        txt(c, p, "flip5 // monitor", 16, 26, GREEN);
        p = paint(13f, false);
        for (int k = 0; k < 8; k++) {
            int col = k / 4, row = k % 4;
            float x = 16 + col * 214, y = 52 + row * 19;
            double v = core(k, sec);
            txt(c, p, "c" + k, x, y, MUTED);
            bar(c, x + 28, y - 9, 120, 9, v);
            txt(c, p, String.format(Locale.US, "%3.0f%%", v), x + 156, y, TEXT);
        }
        double[] mm = mem(ctx);
        txt(c, p, "mem", 16, 142, MUTED);
        bar(c, 52, 133, 140, 9, mm[0] / mm[1] * 100);
        txt(c, p, String.format(Locale.US, "%.1f/%.1fG", mm[0], mm[1]), 200, 142, TEXT);
        double dn = 0.4 + 2.6 * Math.abs(Math.sin(sec / 3.1)), up = 0.1 + 0.5 * Math.abs(Math.sin(sec / 4.3));
        txt(c, p, String.format(Locale.US, "down %.2f  up %.2f MB/s", dn, up), 16, 162, CYAN);
        Path path = new Path();
        for (int i = 0; i <= 40; i++) {
            double x = sec - (40 - i) * 0.5, avg = 0;
            for (int k = 0; k < 8; k++) avg += core(k, x);
            avg /= 8.0;
            float px = 16 + i * (408f / 40f), py = (float) (192 - avg / 100.0 * 22);
            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
        sp.setStyle(Paint.Style.STROKE);
        sp.setStrokeWidth(2.5f);
        sp.setColor(GREEN);
        c.drawPath(path, sp);
        return b;
    }

    static Bitmap neofetch(Context ctx) {
        int W = 440, H = 270;
        Bitmap b = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        panel(c, W, H);
        Paint bold = paint(16f, true);
        txt(c, bold, "operator", 16, 28, GREEN);
        txt(c, bold, "@", 16 + 8 * 9.6f, 28, CYAN);
        txt(c, bold, "flip5", 16 + 9 * 9.6f, 28, GREEN);
        Paint p = paint(15f, false);
        long up = SystemClock.elapsedRealtime() / 1000;
        String upS = (up / 86400) + "d " + (up % 86400 / 3600) + "h " + (up % 3600 / 60) + "m";
        double[] mm = mem(ctx);
        Intent bi = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int lvl = 0;
        if (bi != null) {
            int sc = Math.max(1, bi.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
            lvl = bi.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) * 100 / sc;
        }
        String kern = System.getProperty("os.version");
        if (kern == null) kern = "6.1";
        if (kern.length() > 22) kern = kern.substring(0, 22);
        String[] k = {"OS", "Host", "Kernel", "Uptime", "Shell", "CPU", "Mem", "Bat"};
        String[] v = {"Android " + Build.VERSION.RELEASE + " aarch64", Build.MODEL, kern, upS, "zsh 5.9",
                "Snapdragon 8 Gen 2", String.format(Locale.US, "%.2fG / %.2fG", mm[0], mm[1]), ""};
        float y = 56;
        for (int i = 0; i < 8; i++) {
            txt(c, p, k[i], 16, y, GREEN);
            if (i < 7) {
                txt(c, p, v[i], 16 + 8 * 9f, y, TEXT);
            } else {
                bar(c, 16 + 8 * 9f, y - 11, 150, 11, 100 - lvl < 0 ? 0 : lvl);
                txt(c, p, lvl + "%", 16 + 8 * 9f + 160, y, TEXT);
            }
            y += 22;
        }
        int[] cols = {0xFF1B1F1D, RED, GREEN, AMBER, CYAN, 0xFFC678DD, TEXT};
        Paint fp = new Paint();
        for (int i = 0; i < cols.length; i++) {
            fp.setColor(cols[i]);
            c.drawRect(16 + i * 24, 236, 16 + i * 24 + 24, 252, fp);
        }
        return b;
    }
}
