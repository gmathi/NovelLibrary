package io.github.gmathi.novellibrary.util;

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

import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.gmathi.novellibrary.R;

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
    ProgressBar loadingStateProgressBar;
    TextView loadingStateTextView;

    ConstraintLayout emptyStateConstraintLayout;
    ImageView emptyStateImageView;
    TextView emptyStateContentTextView;

    ConstraintLayout errorStateConstraintLayout;
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
     * Hide all other states and show content
     */
    public void showContent() {
        switchState(CONTENT, null, null, null, null, Collections.emptyList());
    }

    /**
     * Hide all other states and show content
     *
     * @param skipIds Ids of views not to show
     */
    @SuppressWarnings("unused")
    public void showContent(List<Integer> skipIds) {
        switchState(CONTENT, null, null, null, null, skipIds);
    }

    /**
     * Hide content and show the progress bar
     */
    public void showLoading() {
        switchState(LOADING, null, null, null, null, Collections.emptyList());
    }

    public void showLoading(String loadingText) {
        switchState(LOADING, null, loadingText, null, null, Collections.emptyList());
    }

    /**
     * Hide content and show the progress bar
     *
     * @param skipIds Ids of views to not hide
     */
    @SuppressWarnings("unused")
    public void showLoading(List<Integer> skipIds) {
        switchState(LOADING, null, null, null, null, skipIds);
    }

    /**
     * Show empty view when there are not data to show
     *
     * @param emptyImageDrawable Drawable to show
     * @param emptyTextContent   Content of the empty view to show
     */
    public void showEmpty(Drawable emptyImageDrawable, String emptyTextContent) {
        switchState(EMPTY, emptyImageDrawable, emptyTextContent, null, null, Collections.emptyList());
    }

    /**
     * Show empty view when there are not data to show
     *
     * @param emptyImageDrawable Drawable to show
     * @param emptyTextContent   Content of the empty view to show
     * @param skipIds            Ids of views to not hide
     */
    @SuppressWarnings("unused")
    public void showEmpty(Drawable emptyImageDrawable, String emptyTextContent, List<Integer> skipIds) {
        switchState(EMPTY, emptyImageDrawable, emptyTextContent, null, null, skipIds);
    }

    /**
     * Show error view with a button when something goes wrong and prompting the user to try again
     *
     * @param errorImageDrawable Drawable to show
     * @param errorTextContent   Content of the error view to show
     * @param errorButtonText    Text on the error view button to show
     * @param onClickListener    Listener of the error view button
     */
    public void showError(Drawable errorImageDrawable, String errorTextContent, String errorButtonText, OnClickListener onClickListener) {
        switchState(ERROR, errorImageDrawable, errorTextContent, errorButtonText, onClickListener, Collections.emptyList());
    }

    /**
     * Show error view with a button when something goes wrong and prompting the user to try again
     *
     * @param errorImageDrawable Drawable to show
     * @param errorTextContent   Content of the error view to show
     * @param errorButtonText    Text on the error view button to show
     * @param onClickListener    Listener of the error view button
     * @param skipIds            Ids of views to not hide
     */
    @SuppressWarnings("unused")
    public void showError(Drawable errorImageDrawable, String errorTextContent, String errorButtonText, OnClickListener onClickListener, List<Integer> skipIds) {
        switchState(ERROR, errorImageDrawable, errorTextContent, errorButtonText, onClickListener, skipIds);
    }

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

    private void switchState(String state, Drawable drawable, String errorTextContent,
                             String errorButtonText, OnClickListener onClickListener, List<Integer> skipIds) {
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

                if (errorTextContent != null && !errorTextContent.isEmpty())
                    loadingStateTextView.setText(errorTextContent);
                setContentVisibility(false, skipIds);
                break;
            case EMPTY:
                hideLoadingView();
                hideErrorView();

                setEmptyView();
                if (drawable != null) {
                    emptyStateImageView.setImageDrawable(drawable);
                    emptyStateImageView.setVisibility(View.VISIBLE);
                } else {
                    emptyStateImageView.setVisibility(View.GONE);
                }
                emptyStateContentTextView.setText(errorTextContent);
                setContentVisibility(false, skipIds);
                break;
            case ERROR:
                hideLoadingView();
                hideEmptyView();

                setErrorView();

                if (drawable != null) {
                    errorStateImageView.setImageDrawable(drawable);
                    errorStateImageView.setVisibility(View.VISIBLE);
                } else {
                    errorStateImageView.setVisibility(View.GONE);
                }
                errorStateContentTextView.setText(errorTextContent);
                errorStateButton.setText(errorButtonText);
                errorStateButton.setOnClickListener(onClickListener);
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


}