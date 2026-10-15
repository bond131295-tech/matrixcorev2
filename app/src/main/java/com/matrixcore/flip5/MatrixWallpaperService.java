package com.matrixcore.flip5;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import java.util.Locale;
import java.util.Random;

public class MatrixWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new MatrixEngine();
    }

    static final int GREEN = 0xFF35F07A, CYAN = 0xFF3AD0E0, AMBER = 0xFFE8B341, RED = 0xFFFF5F56,
            MUTED = 0xFF7C8E84, TEXT = 0xFFC5D0C8;

    static final String[] NMAP = {
            "Starting Nmap 7.94 ( https://nmap.org )",
            "Nmap scan report for 10.8.0.1",
            "PORT      STATE  SERVICE  VERSION",
            "22/tcp    open   ssh      OpenSSH 9.6",
            "80/tcp    open   http     nginx 1.24",
            "443/tcp   open   https    nginx 1.24",
            "51820/udp open   wireguard",
            "Nmap scan report for 10.8.0.2",
            "22/tcp    open   ssh      OpenSSH 9.2",
            "3306/tcp  closed mysql",
            "Nmap done: 256 IPs (3 up) scanned"
    };

    class MatrixEngine extends Engine {
        final Handler handler = new Handler(Looper.getMainLooper());
        final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        final long t0 = System.currentTimeMillis();
        boolean visible = false;
        int W = 1080, H = 2640;
        int logColor = TEXT;
        int frames = 0;

        final Runnable loop = new Runnable() {
            @Override
            public void run() {
                drawFrame();
                frames++;
                if (frames % 30 == 0) {
                    try {
                        WidgetRenderer.updateAll(getApplicationContext());
                    } catch (Exception ignored) {
                    }
                }
                if (visible) handler.postDelayed(this, 100);
            }
        };

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            W = width;
            H = height;
            drawFrame();
        }

        @Override
        public void onVisibilityChanged(boolean v) {
            visible = v;
            handler.removeCallbacks(loop);
            if (v) handler.post(loop);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            visible = false;
            handler.removeCallbacks(loop);
            super.onSurfaceDestroyed(holder);
        }

        @Override
        public void onDestroy() {
            visible = false;
            handler.removeCallbacks(loop);
            super.onDestroy();
        }

        void drawFrame() {
            SurfaceHolder sh = getSurfaceHolder();
            Canvas c = null;
            try {
                c = sh.lockCanvas();
                if (c != null) render(c);
            } catch (Exception ignored) {
            } finally {
                if (c != null) {
                    try {
                        sh.unlockCanvasAndPost(c);
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        double rnd(double n) {
            double v = Math.sin(n * 127.1 + 311.7) * 43758.5453;
            return v - Math.floor(v);
        }

        double core(int k, double x) {
            double v = 28 + 20 * Math.sin(x / (2.5 + k)) + 11 * Math.sin(x / (0.9 + k * 0.7) + k)
                    + 9 * rnd(Math.floor(x * 3) * (k + 1));
            return Math.max(3, Math.min(97, v));
        }

        void tx(Canvas c, String s, float x, float y, int color, int alpha) {
            tp.setColor(color);
            tp.setAlpha(alpha);
            c.drawText(s, x, y, tp);
        }

        String clip(String s, int max) {
            return s.length() > max ? s.substring(0, max) : s;
        }

        void label(Canvas c, String s, float y, float mx, float s1) {
            fp.setShader(null);
            fp.setColor(GREEN);
            fp.setAlpha(40);
            fp.setStrokeWidth(Math.max(1f, s1 * 0.5f));
            c.drawLine(mx, y, W - mx, y, fp);
            tx(c, " " + s + " ", mx + 8 * s1, y + 3 * s1, GREEN, 200);
        }

        String logLine(long n, long maxChars) {
            Random r = new Random(n * 7919L + 13);
            int k = r.nextInt(12);
            String ip = (11 + r.nextInt(200)) + "." + r.nextInt(255) + "." + r.nextInt(255) + "." + (1 + r.nextInt(250));
            String ts = String.format(Locale.US, "%tT", t0 + n * 1800L);
            String out;
            if (k <= 4) {
                logColor = AMBER;
                out = ts + " sshd: Failed password root " + ip + ":" + (30000 + r.nextInt(30000));
            } else if (k <= 6) {
                int[] ports = {22, 23, 80, 445, 3389};
                logColor = CYAN;
                out = ts + " ufw: BLOCK SRC=" + ip + " DPT=" + ports[r.nextInt(5)];
            } else if (k == 7) {
                logColor = GREEN;
                out = ts + " sshd: Accepted publickey operator 10.8.0.2";
            } else if (k == 8) {
                logColor = GREEN;
                out = ts + " wg0: handshake ok peer Zb4k..x1Ew";
            } else if (k == 9) {
                logColor = RED;
                out = ts + " fail2ban: Ban " + ip;
            } else {
                logColor = TEXT;
                out = ts + " sudo: operator COMMAND=/usr/bin/nmap";
            }
            return clip(out, (int) maxChars);
        }

        void render(Canvas c) {
            float s = W / 360f;
            long t = System.currentTimeMillis() - t0;
            double sec = t / 1000.0;

            c.drawColor(0xFF070A09);
            fp.setShader(new RadialGradient(W * 0.5f, 0, H * 0.65f, 0xFF0C1B13, 0xFF050807, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, W, H, fp);
            fp.setShader(null);

            tp.setTypeface(Typeface.MONOSPACE);
            tp.setTextSize(8.4f * s);
            float cw = tp.measureText("M");
            float lh = 11.6f * s;
            float mx = 14 * s;
            int maxChars = (int) ((W - 2 * mx) / cw);
            boolean blink = ((t / 500) % 2) == 0;

            // ---- pane 0: nmap ----
            float y = 26 * s;
            label(c, "0:nmap", y, mx, s);
            y = 44 * s;
            tx(c, "┌──(operator@flip5)-[~/ops]", mx, y, GREEN, 210);
            y += lh;
            tx(c, "└─$ nmap -sV -T4 10.8.0.0/24", mx, y, TEXT, 210);
            int n = NMAP.length;
            int step = (int) ((t / 650) % (n + 6));
            int shown = Math.min(n, step);
            for (int i = 0; i < shown; i++) {
                y += lh;
                int col = TEXT;
                String ln = NMAP[i];
                if (ln.contains(" open")) col = GREEN;
                else if (ln.contains("closed")) col = RED;
                else if (ln.startsWith("Nmap")) col = CYAN;
                tx(c, clip(ln, maxChars), mx, y, col, 190);
            }
            if (blink) tx(c, "█", mx, y + lh, GREEN, 230);

            // ---- pane 1: htop ----
            y = 226 * s;
            label(c, "1:htop", y, mx, s);
            y += 16 * s;
            for (int k = 0; k < 8; k++) {
                double p = core(k, sec);
                int barLen = 22;
                int fill = (int) Math.round(p / 100.0 * barLen);
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < fill; i++) b.append('|');
                StringBuilder pad = new StringBuilder();
                for (int i = fill; i < barLen; i++) pad.append(' ');
                int col = p < 50 ? GREEN : (p < 80 ? AMBER : RED);
                tx(c, k + "[", mx, y, MUTED, 220);
                tx(c, b.toString(), mx + cw * 2, y, col, 230);
                tx(c, pad + String.format(Locale.US, "%5.1f%%]", p), mx + cw * (2 + fill), y, MUTED, 220);
                y += lh;
            }
            double l1 = 1.2 + 0.6 * Math.sin(sec / 9);
            tx(c, String.format(Locale.US, "Tasks: 312, 1204 thr   Load: %.2f %.2f %.2f", l1, l1 - 0.1, l1 - 0.2),
                    mx, y, TEXT, 190);
            y += lh;
            tx(c, "  PID USER      %CPU %MEM COMMAND", mx, y, CYAN, 200);
            String[] pr = {"2211 root", "1874 operator", "3302 system", "905  wg-quick"};
            String[] cm = {"sshd", "nmap -sV", "surfaceflinger", "wireguard-go"};
            for (int i = 0; i < 4; i++) {
                y += lh;
                double cpu = 2 + 14 * rnd(Math.floor(sec) + i * 9);
                String row = String.format(Locale.US, "%-14s %5.1f %4.1f %s", pr[i], cpu, 0.4 + i * 0.7, cm[i]);
                tx(c, clip(row, maxChars), mx, y, i == 1 ? GREEN : TEXT, 180);
            }

            // ---- pane 2: auth.log ----
            y = 420 * s;
            label(c, "2:tail -f /var/log/auth.log", y, mx, s);
            y += 16 * s;
            long cur = t / 1800L;
            for (long i = cur - 13; i <= cur; i++) {
                String ln = logLine(i, maxChars);
                int alpha = (i == cur) ? 255 : 170;
                tx(c, ln, mx, y, logColor, alpha);
                y += lh;
            }
            if (blink) tx(c, "█", mx, y, GREEN, 230);

            // ---- pane 3: wg ----
            y = 632 * s;
            label(c, "3:wg show", y, mx, s);
            y += 16 * s;
            double rx = 12.0 + sec * 0.021;
            double txm = 3.4 + sec * 0.008;
            long hs = (long) (sec % 118);
            String[] wg = {
                    "interface: wg0",
                    "  public key: 7Hq3m2...kP9s=",
                    "  listening port: 51820",
                    "peer: Zb4kT8...x1Ew=",
                    "  endpoint: 203.0.113.7:51820",
                    "  latest handshake: " + hs + " seconds ago",
                    String.format(Locale.US, "  transfer: %.2f MiB received, %.2f MiB sent", rx, txm)
            };
            for (int i = 0; i < wg.length; i++) {
                int col = (i == 0 || i == 3) ? GREEN : TEXT;
                tx(c, clip(wg[i], maxChars), mx, y, col, 185);
                y += lh;
            }

            // ---- tmux status bar ----
            float by = H - 20 * s;
            fp.setColor(0xFF0E1A13);
            fp.setAlpha(255);
            c.drawRect(0, by - 12 * s, W, H, fp);
            tx(c, "[ops] 0:nmap 1:htop* 2:logs 3:wg", mx, by, GREEN, 220);
            String clock = String.format(Locale.US, "flip5  %tT", System.currentTimeMillis());
            tx(c, clock, W - mx - cw * clock.length(), by, MUTED, 230);

            // ---- darken so icons stay readable ----
            c.drawColor(0x66000000);
            fp.setShader(new LinearGradient(0, 0, 0, H, new int[]{0x99000000, 0x00000000, 0x00000000, 0xAA000000},
                    new float[]{0f, 0.25f, 0.75f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, W, H, fp);

            // ---- scan sweep ----
            float sy = (float) (((sec / 9.0) % 1.0) * (H + 300) - 300);
            fp.setShader(new LinearGradient(0, sy, 0, sy + 300,
                    new int[]{0x0035F07A, 0x1435F07A, 0x0035F07A}, null, Shader.TileMode.CLAMP));
            c.drawRect(0, sy, W, sy + 300, fp);
            fp.setShader(null);
        }
    }
}
