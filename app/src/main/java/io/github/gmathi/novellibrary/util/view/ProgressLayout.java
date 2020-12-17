package io.github.gmathi.novellibrary.util.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.gmathi.novellibrary.R;

import static android.animation.ValueAnimator.INFINITE;

public class ProgressLayout extends RelativeLayout {

    private static final String TAG_LOADING = "ProgressView.TAG_LOADING";
    private static final String TAG_EMPTY = "ProgressView.TAG_EMPTY";
    private static final String TAG_ERROR = "ProgressView.TAG_ERROR";

    final String CONTENT = "type_content";
    final String LOADING = "type_loading";
    final String EMPTY = "type_empty";
    final String ERROR = "type_error";

    LayoutInflater inflater;
    View view;
    LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT);
    Drawable currentBackground;

    List<View> contentViews = new ArrayList<>();

    ConstraintLayout loadingStateConstraintLayout;
    LottieAnimationView loadingAnimationView;
    ProgressBar loadingStateProgressBar;
    TextView loadingStateTextView;

    ConstraintLayout emptyStateConstraintLayout;
    LottieAnimationView emptyAnimationView;
    ImageView emptyStateImageView;
    TextView emptyStateContentTextView;
    Button emptyStateButton;

    ConstraintLayout errorStateConstraintLayout;
    LottieAnimationView errorAnimationView;
    ImageView errorStateImageView;
    TextView errorStateContentTextView;
    Button errorStateButton;

    int loadingStateBackgroundColor;
    int emptyStateBackgroundColor;
    int errorStateBackgroundColor;

    private String state = CONTENT;

    public ProgressLayout(Context context) {
        super(context);
    }

    public ProgressLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProgressLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        currentBackground = this.getBackground();
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (child.getTag() == null || (!child.getTag().equals(TAG_LOADING)
                && !child.getTag().equals(TAG_EMPTY) && !child.getTag().equals(TAG_ERROR))) {
            contentViews.add(child);
        }
    }

    /**
     * Content States
     */

    public void showContent() {
        switchState(CONTENT, null, null, null, null, null, Collections.emptyList());
    }

    public void showContent(List<Integer> skipIds) {
        switchState(CONTENT, null, null, null, null, null, skipIds);
    }

    public void showLoading(@Nullable @RawRes Integer rawId, @Nullable @DrawableRes Integer drawableId, @Nullable String message, @Nullable String buttonText, @Nullable OnClickListener onClickListener) {
        switchState(LOADING, rawId, drawableId, message, buttonText, onClickListener, Collections.emptyList());
    }

    public void updateLoadingStatus(String value) {
        loadingStateTextView.setText(value);
    }

    public void showEmpty(@Nullable @RawRes Integer rawId, @Nullable @DrawableRes Integer drawableId, @Nullable String message, @Nullable String buttonText, @Nullable OnClickListener onClickListener) {
        switchState(EMPTY, rawId, drawableId, message, buttonText, onClickListener, Collections.emptyList());
    }

    public void showError(@Nullable @RawRes Integer rawId, @Nullable @DrawableRes Integer drawableId, @Nullable String message, @Nullable String buttonText, @Nullable OnClickListener onClickListener) {
        switchState(ERROR, rawId, drawableId, message, buttonText, onClickListener, Collections.emptyList());
    }


    //region - Helper functions

    public String getState() {
        return state;
    }

    public boolean isContent() {
        return state.equals(CONTENT);
    }

    public boolean isLoading() {
        return state.equals(LOADING);
    }

    public boolean isEmpty() {
        return state.equals(EMPTY);
    }

    public boolean isError() {
        return state.equals(ERROR);
    }

    //endregion

    /**
     * Switches the state on the layout to show one of these layouts - LOADING, EMPTY, ERROR, and CONTENT
     *
     * @param state           The states which are LOADING, EMPTY, ERROR, and CONTENT
     * @param rawResId        The Lotto Animation ResId of the Raw file
     * @param drawableId      The ResId of the drawable file
     * @param message         The contextual message to show on the screen
     * @param buttonText      The text of the button
     * @param onClickListener The onClickListener for the button
     * @param skipIds         Ids of views to not hide
     */
    private void switchState(String state, @Nullable @RawRes Integer rawResId, @Nullable @DrawableRes Integer drawableId, String message,
                             String buttonText, OnClickListener onClickListener, List<Integer> skipIds) {
        this.state = state;

        switch (state) {
            case CONTENT:
                //Hide all state views to display content
                hideLoadingView();
                hideEmptyView();
                hideErrorView();
                setContentVisibility(true, skipIds);
                break;

            case LOADING:
                hideEmptyView();
                hideErrorView();
                setLoadingView();

                if (rawResId != null) {
                    loadingAnimationView.setAnimation(rawResId);
                    loadingAnimationView.resumeAnimation();
                    loadingAnimationView.setVisibility(View.VISIBLE);
                    loadingStateProgressBar.setVisibility(View.GONE);
                } else {
                    loadingAnimationView.setVisibility(View.GONE);
                    loadingStateProgressBar.setVisibility(View.VISIBLE);
                }
                if (message != null && !message.isEmpty())
                    loadingStateTextView.setText(message);

                setContentVisibility(false, skipIds);
                break;
            case EMPTY:
                hideLoadingView();
                hideErrorView();
                setEmptyView();

                if (rawResId != null) {
                    emptyAnimationView.setAnimation(rawResId);
                    emptyAnimationView.resumeAnimation();
                    setVerticalBias(0.37f, emptyStateConstraintLayout, R.id.emptyLinearLayout);
                } else {
                    emptyAnimationView.setVisibility(View.GONE);
                }
                if (drawableId != null) {
                    emptyStateImageView.setImageResource(drawableId);
                    emptyStateImageView.setVisibility(View.VISIBLE);
                    setVerticalBias(0.50f, emptyStateConstraintLayout, R.id.emptyLinearLayout);
                } else {
                    emptyStateImageView.setVisibility(View.GONE);
                }
                emptyStateContentTextView.setText(message);
                if (buttonText != null) {
                    emptyStateButton.setVisibility(View.VISIBLE);
                    emptyStateButton.setText(buttonText);
                    emptyStateButton.setOnClickListener(onClickListener);
                } else {
                    emptyStateButton.setVisibility(View.GONE);
                }

                setContentVisibility(false, skipIds);
                break;
            case ERROR:
                hideLoadingView();
                hideEmptyView();
                setErrorView();

                if (rawResId != null) {
                    errorAnimationView.setAnimation(rawResId);
                    errorAnimationView.resumeAnimation();
                    errorAnimationView.setVisibility(View.VISIBLE);
                    setVerticalBias(0.37f, errorStateConstraintLayout, R.id.errorLinearLayout);
                } else {
                    errorAnimationView.setVisibility(View.GONE);
                }
                if (drawableId != null) {
                    errorStateImageView.setImageResource(drawableId);
                    errorStateImageView.setVisibility(View.VISIBLE);
                    setVerticalBias(0.50f, errorStateConstraintLayout, R.id.errorLinearLayout);
                } else {
                    errorStateImageView.setVisibility(View.GONE);
                }
                errorStateContentTextView.setText(message);
                if (buttonText != null) {
                    errorStateButton.setVisibility(View.VISIBLE);
                    errorStateButton.setText(buttonText);
                    errorStateButton.setOnClickListener(onClickListener);
                } else {
                    errorStateButton.setVisibility(View.GONE);
                }

                setContentVisibility(false, skipIds);
                break;
        }
    }

    @SuppressLint("InflateParams")
    private void setLoadingView() {
        if (loadingStateConstraintLayout == null) {
            view = inflater.inflate(R.layout.generic_loading_view, null);
            loadingStateConstraintLayout = view.findViewById(R.id.loadingStateConstraintLayout);
            loadingStateConstraintLayout.setTag(TAG_LOADING);

            loadingStateProgressBar = view.findViewById(R.id.loadingStateSpinner);
            loadingStateTextView = view.findViewById(R.id.loadingStateTextView);
            loadingAnimationView = view.findViewById(R.id.loadingStateAnimationView);

            addView(loadingStateConstraintLayout, layoutParams);
        } else {
            loadingStateConstraintLayout.setVisibility(VISIBLE);
        }
    }

    private void setEmptyView() {
        if (emptyStateConstraintLayout == null) {
            view = inflater.inflate(R.layout.generic_empty_view, null);
            emptyStateConstraintLayout = view.findViewById(R.id.emptyStateConstraintLayout);
            emptyStateConstraintLayout.setTag(TAG_EMPTY);

            emptyStateImageView = view.findViewById(R.id.emptyStateImageView);
            emptyStateContentTextView = view.findViewById(R.id.emptyStateTextView);
            emptyStateButton = view.findViewById(R.id.emptyStateButton);
            emptyAnimationView = view.findViewById(R.id.emptyStateAnimationView);

            addView(emptyStateConstraintLayout, layoutParams);
        } else {
            emptyStateConstraintLayout.setVisibility(VISIBLE);
        }
    }

    private void setErrorView() {
        if (errorStateConstraintLayout == null) {
            view = inflater.inflate(R.layout.generic_error_view, null);
            errorStateConstraintLayout = view.findViewById(R.id.errorStateConstraintLayout);
            errorStateConstraintLayout.setTag(TAG_ERROR);

            errorStateImageView = view.findViewById(R.id.errorStateImageView);
            errorStateContentTextView = view.findViewById(R.id.errorStateTextView);
            errorStateButton = view.findViewById(R.id.errorStateButton);
            errorAnimationView = view.findViewById(R.id.errorStateAnimationView);

            addView(errorStateConstraintLayout, layoutParams);
        } else {
            errorStateConstraintLayout.setVisibility(VISIBLE);
        }
    }

    private void setContentVisibility(boolean visible, List<Integer> skipIds) {
        for (View v : contentViews) {
            if (!skipIds.contains(v.getId())) {
                v.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void hideLoadingView() {
        if (loadingStateConstraintLayout != null) {
            loadingStateConstraintLayout.setVisibility(GONE);
            //Restore the background color if not TRANSPARENT
            if (loadingStateBackgroundColor != Color.TRANSPARENT) {
                this.setBackground(currentBackground);
            }
        }
    }

    private void hideEmptyView() {
        if (emptyStateConstraintLayout != null) {
            emptyStateConstraintLayout.setVisibility(GONE);

            //Restore the background color if not TRANSPARENT
            if (emptyStateBackgroundColor != Color.TRANSPARENT) {
                this.setBackground(currentBackground);
            }
        }
    }

    private void hideErrorView() {
        if (errorStateConstraintLayout != null) {
            errorStateConstraintLayout.setVisibility(GONE);

            //Restore the background color if not TRANSPARENT
            if (errorStateBackgroundColor != Color.TRANSPARENT) {
                this.setBackground(currentBackground);
            }
        }
    }

    private void setVerticalBias(Float biasedValue, ConstraintLayout constraintLayout, int resId) {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);
        constraintSet.setVerticalBias(resId, biasedValue);
        constraintSet.applyTo(constraintLayout);
    }


}