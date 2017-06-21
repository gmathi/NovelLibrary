package com.mgn.bingenovelreader.utils;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;

import com.mgn.bingenovelreader.BuildConfig;
import com.mgn.bingenovelreader.R;

import java.io.ByteArrayOutputStream;

public class Util {

    //region color utils
    public static int getThemeAccentColor(final Context context) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(
                typedValue.data,
                new int[]{R.attr.colorAccent}
        );
        int color = a.getColor(0, Color.CYAN);
        a.recycle();

        return color;
    }

    public static int getThemePrimaryColor(final Context context) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(
                typedValue.data,
                new int[]{R.attr.colorPrimary}
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
    //endregion

    //region Bitmap to byte[] Conversions

    public static byte[] getBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        return stream.toByteArray();
    }

    // convert from byte array to bitmap
    public static Bitmap getImage(byte[] image) {
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

    //endregion

    //region UtilLogs
    public static void debug(String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message);
        }
    }

    public static void info(String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message);
        }
    }

    public static void warning(String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message);
        }
    }

    public static void error(String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message);
        }
    }

    public static void error(String tag, String message, Throwable throwable) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message, throwable);
        }
    }


    //endregion

    public static void something() {

    }

}
