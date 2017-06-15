package com.mgn.bingenovelreader.utils;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.TypedValue;

import com.mgn.bingenovelreader.R;

public class Util {

    public static int getThemeAccentColor(final Context context) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(
                typedValue.data,
                new int[] { R.attr.colorAccent}
        );
        int color = a.getColor(0, Color.CYAN);
        a.recycle();

        return color;
    }

    public static int getThemePrimaryColor(final Context context) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(
                typedValue.data,
                new int[] { R.attr.colorPrimary}
        );
        int color = a.getColor(0, Color.BLUE);
        a.recycle();

        return color;
    }

    public static int lighten(int color, float factor) {
        int red = (int) ((Color.red(color) * (1 - factor) / 255 + factor) * 255);
        int green = (int) ((Color.green(color) * (1 - factor) / 255 + factor) * 255);
        int blue = (int) ((Color.blue(color) * (1 - factor) / 255 + factor) * 255);
        return Color.argb(Color.alpha(color), red, green, blue);
    }

}
