package com.matrixcore.flip5;

import android.app.Activity;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;

public class MainActivity extends Activity {

    static final int GREEN = 0xFF35F07A, CYAN = 0xFF3AD0E0, MUTED = 0xFF7C8E84, TEXT = 0xFFC5D0C8;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad * 2, pad, pad * 2);
        root.setBackgroundColor(0xFF070A09);

        root.addView(text("┌──(operator@flip5)-[~/matrix-core]", 12, GREEN, true));
        root.addView(text("└─$ ./install.sh --all", 14, TEXT, false));
        root.addView(text("\nMATRIX CORE // realistic", 20, GREEN, true));
        root.addView(text("Setup for Galaxy Z Flip5. Do the steps in order.\n", 12, MUTED, false));

        root.addView(text("[1] LIVE WALLPAPER (home)", 13, CYAN, true));
        root.addView(text("Animated tmux desktop: nmap, htop, streaming auth.log, wg status.", 12, TEXT, false));
        root.addView(button("[ set live wallpaper ]", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent i = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                    i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            new ComponentName(MainActivity.this, MatrixWallpaperService.class));
                    startActivity(i);
                } catch (Exception e) {
                    startActivity(Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper"));
                }
            }
        }));

        root.addView(text("\n[2] LOCK SCREEN IMAGE", 13, CYAN, true));
        root.addView(text("Sets the MOTD login image on the lock screen only. The system clock stays on top: style it in Good Lock > LockStar.", 12, TEXT, false));
        root.addView(button("[ apply lock screen image ]", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyLock();
            }
        }));

        root.addView(text("\n[3] WIDGETS", 13, CYAN, true));
        root.addView(text("Clock, live monitor, neofetch card. The monitor and neofetch refresh every 3 s while the live wallpaper is showing. If a button does nothing: long-press the home screen > Widgets > Matrix Core.", 12, TEXT, false));
        root.addView(button("[ add clock widget ]", pin(ClockWidget.class)));
        root.addView(button("[ add monitor widget ]", pin(MonitorWidget.class)));
        root.addView(button("[ add neofetch widget ]", pin(NeofetchWidget.class)));

        root.addView(text("\n[4] ICONS", 13, CYAN, true));
        root.addView(text("Samsung's launcher cannot use icon packs. Install Nova Launcher, then Nova settings > Look & feel > Icon style > Icon theme > Matrix Core.", 12, TEXT, false));
        root.addView(button("[ get nova launcher ]", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.teslacoilsw.launcher")));
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.teslacoilsw.launcher")));
                }
            }
        }));

        root.addView(text("\n[5] QUICK PANEL AND COVER SCREEN", 13, CYAN, true));
        root.addView(text("Quick panel: Galaxy Store > Good Lock > install QuickStar. Panel colour black, transparency about 85%, accent #35F07A.\n\nCover screen: Settings > Cover screen > Wallpaper > Gallery > pick wallpaper_cover_720x748.png from the extras folder (download it from the GitHub repo to your phone first).\n\nClock font: Settings > Display > Font style, or any monospace font.", 12, TEXT, false));

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    private View.OnClickListener pin(final Class<?> cls) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    AppWidgetManager m = AppWidgetManager.getInstance(MainActivity.this);
                    if (m.isRequestPinAppWidgetSupported()) {
                        m.requestPinAppWidget(new ComponentName(MainActivity.this, cls), null, null);
                    } else {
                        toast("Long-press home > Widgets > Matrix Core");
                    }
                } catch (Exception e) {
                    toast("Long-press home > Widgets > Matrix Core");
                }
            }
        };
    }

    private void applyLock() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream in = getAssets().open("lock.png");
                    Bitmap bm = BitmapFactory.decodeStream(in);
                    in.close();
                    WallpaperManager.getInstance(MainActivity.this).setBitmap(bm, null, true, WallpaperManager.FLAG_LOCK);
                    toast("Lock screen image applied");
                } catch (Exception e) {
                    toast("Could not apply: " + e.getMessage());
                }
            }
        }).start();
    }

    private void toast(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, s, Toast.LENGTH_LONG).show();
            }
        });
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.MONOSPACE, bold ? Typeface.BOLD : Typeface.NORMAL);
        t.setPadding(0, 4, 0, 4);
        return t;
    }

    private Button button(String s, View.OnClickListener l) {
        Button bt = new Button(this);
        bt.setText(s);
        bt.setAllCaps(false);
        bt.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        bt.setTextColor(GREEN);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFF0D1612);
        g.setStroke(2, GREEN);
        g.setCornerRadius(24f);
        bt.setBackground(g);
        bt.setGravity(Gravity.CENTER);
        bt.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 14;
        bt.setLayoutParams(lp);
        return bt;
    }
}
