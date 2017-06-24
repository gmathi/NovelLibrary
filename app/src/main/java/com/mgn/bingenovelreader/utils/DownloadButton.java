package com.mgn.bingenovelreader.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.mgn.bingenovelreader.R;


public class DownloadButton extends RelativeLayout {


    private int mPrimaryColor;
    private int mAccentColor;
    private int mAccentColorLight;

    public DownloadButton(Context context) {
        this(context, null, 0);
    }

    public DownloadButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DownloadButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        View view = inflate(getContext(), R.layout.download_button, this);
        // Initializing color values
        mPrimaryColor = Util.INSTANCE.getThemePrimaryColor(context);
        mAccentColor = Util.INSTANCE.getThemeAccentColor(context);
        mAccentColorLight = Util.INSTANCE.lighten(mAccentColor, 0.5f);
    }





}
