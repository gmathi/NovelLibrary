package io.github.gmathi.novellibrary.extensions

import android.view.View
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.util.view.ProgressLayout


fun ProgressLayout.showLoading(
    rawId: Int? = null,
    loadingText: String = context.getString(R.string.loading)
) {
    showLoading(
        rawId,
        null,
        loadingText,
        null,
        null
    )
}


fun ProgressLayout.showEmpty(
    resId: Int? = R.drawable.ic_warning_white_vector,
    isLottieAnimation: Boolean = false,
    emptyText: String,
    buttonText: String? = null,
    onClickListener: View.OnClickListener? = null
) {
    showEmpty(
        if (isLottieAnimation) resId else null,
        if (!isLottieAnimation) resId else null,
        emptyText,
        buttonText,
        onClickListener
    )
}

fun ProgressLayout.showError(
    resId: Int? = R.drawable.ic_warning_white_vector,
    isLottieAnimation: Boolean = false,
    errorText: String,
    buttonText: String? = null,
    onClickListener: View.OnClickListener? = null
) {
    showError(
        if (isLottieAnimation) resId else null,
        if (!isLottieAnimation) resId else null,
        errorText,
        buttonText,
        onClickListener
    )
}

fun ProgressLayout.noInternetError(onClickListener: View.OnClickListener) {
    showError(
        resId = R.raw.no_internet_cat,
        isLottieAnimation = true,
        errorText = context.getString(R.string.no_internet),
        buttonText = context.getString(R.string.try_again),
        onClickListener = onClickListener
    )
}

fun ProgressLayout.dataFetchError(onClickListener: View.OnClickListener) {
    showError(
        errorText = context.getString(R.string.failed_to_load_url),
        buttonText = context.getString(R.string.try_again),
        onClickListener = onClickListener
    )
}