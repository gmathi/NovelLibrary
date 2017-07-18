package com.mgn.bingenovelreader.util;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mgn.bingenovelreader.R;

public class ToolbarAlphaScrollBehavior extends CoordinatorLayout.Behavior<android.support.v7.widget.Toolbar> {
    private ColorDrawable mStatusBarColorDrawable;
    private int mStatusBarColor;
    private TextView mTitleView;
    private boolean searchedForTitleView = false;

    public ToolbarAlphaScrollBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        mStatusBarColor = ContextCompat.getColor(context, R.color.colorPrimary_dark);
        mStatusBarColor = getColorWithAlpha(0, mStatusBarColor);
        mStatusBarColorDrawable = new ColorDrawable(mStatusBarColor);
    }

    public static int getColorWithAlpha(float alpha, int baseColor) {
        int a = Math.min(255, Math.max(0, (int) (alpha * 255))) << 24;
        int rgb = 0x00ffffff & baseColor;
        return a + rgb;
    }

    public ToolbarAlphaScrollBehavior() {
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, Toolbar child, View dependency) {
        return dependency instanceof AppBarLayout;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, Toolbar child, MotionEvent ev) {
        return ev == null || super.onInterceptTouchEvent(parent, child, ev);
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, Toolbar child, View dependency) {
        if (dependency instanceof AppBarLayout) {
            float ratio = (float) getCurrentScrollValue(child, dependency) / getTotalScrollRange(child, dependency);
            float alpha = 1f - Math.min(1f, Math.max(0f, ratio));
            int drawableAlpha = (int) (alpha * 255);
            //  Log.i("ToolbarAlphaScrollBehavior", "Alpha: " + alpha);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                child.getBackground().setAlpha(drawableAlpha);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                ViewGroup toolbarParent = (ViewGroup) child.getParent();
                if (toolbarParent.getChildCount() == 2) {
                    int count = toolbarParent.getChildCount();
                    for (int i = count - 1; i >= 0; i--) {
                        toolbarParent.getChildAt(i).getBackground().setAlpha(drawableAlpha);
                    }
                }
            } else {
                child.getBackground().setAlpha(drawableAlpha);
            }

            //     setStatusBarColor(parent, drawableAlpha);
            if (mTitleView != null) {
                ViewCompat.setAlpha(mTitleView, alpha);
                return false;
            }
            if (!searchedForTitleView) {
                mTitleView = getTitleView(child);
                searchedForTitleView = true;
            }

        }
        return false;
    }

    private TextView getTitleView(Toolbar toolbar) {
        for (int i = 0; i < toolbar.getChildCount(); ++i) {
            View child = toolbar.getChildAt(i);
            if (child instanceof TextView)
                return (TextView) child;
        }
        return null;
    }

    private void setStatusBarColor(CoordinatorLayout parent, int alpha) {
        ColorDrawable statusBarBackground = (ColorDrawable) parent.getStatusBarBackground();
        statusBarBackground.setColor(getColorWithAlpha(alpha, statusBarBackground.getColor()));
        parent.setStatusBarBackground(statusBarBackground);
    }

    private int getCurrentScrollValue(Toolbar child, View dependency) {
        return dependency.getBottom() - child.getTop();
    }

    private float getTotalScrollRange(Toolbar child, View dependency) {
        return Math.max(dependency.getHeight(), ((AppBarLayout) dependency).getTotalScrollRange()) - child.getTop();
    }

}
