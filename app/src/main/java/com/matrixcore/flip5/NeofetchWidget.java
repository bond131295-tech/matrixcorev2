package com.matrixcore.flip5;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

public class NeofetchWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        WidgetRenderer.upd(c, m, NeofetchWidget.class, 1);
    }
}
