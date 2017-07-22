package io.github.gmathi.novellibrary.util

import android.app.Activity
import android.content.Intent
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.util.Pair
import android.support.v4.view.ViewCompat
import android.view.View

object TransitionHelper {

    /**
     * Starts a shared transition of activities connected by views
     * <br></br>

     * @param base The calling activity
     * *
     * @param target The view from the calling activity with transition name
     * *
     * @param data Intent with bundle and or activity to start
     */
    fun startSharedTransitionActivity(base: Activity, target: View, data: Intent) {
        val participants = Pair(target, ViewCompat.getTransitionName(target))
        val transitionActivityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(
            base, participants)
        ActivityCompat.startActivity(base, data, transitionActivityOptions.toBundle())
    }

    /**
     * Starts a shared transition of activities connected by views
     * by making use of the provided transition name
     * <br></br>

     * @param base The calling activity
     * *
     * @param target The view from the calling activity with transition name
     * *
     * @param transitionName The name of the target transition
     * *
     * @param data Intent with bundle and or activity to start
     */
    fun startSharedImageTransition(base: Activity, target: View, transitionName: String, data: Intent) {
        val transition = ActivityOptionsCompat.makeSceneTransitionAnimation(
            base, target, transitionName)
        base.startActivity(data, transition.toBundle())
    }

}
