package io.github.gmathi.novellibrary.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.system.DataAccessor
import javax.inject.Inject


/**
 * Base fragment without type parameters for Hilt compatibility.
 * All fragments should extend this class and use @AndroidEntryPoint.
 */
open class BaseFragment : Fragment(), DataAccessor {

    @Inject override lateinit var firebaseAnalytics: FirebaseAnalytics
    @Inject override lateinit var dataCenter: DataCenter
    @Inject override lateinit var dbHelper: DBHelper
    @Inject override lateinit var sourceManager: SourceManager
    @Inject override lateinit var networkHelper: NetworkHelper

    /**
     * Override this method to return the layout resource ID for the fragment.
     * This will be used for ViewBinding inflation in subclasses.
     */
    protected open fun getLayoutId(): Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutId = getLayoutId()
        return if (layoutId != 0) {
            inflater.inflate(layoutId, container, false)
        } else {
            super.onCreateView(inflater, container, savedInstanceState)
        }
    }
}

/**
 * Base fragment with ViewBinding support.
 * Use this for fragments that need ViewBinding with type safety.
 * Do NOT add @AndroidEntryPoint to this class - add it to concrete implementations.
 */
open class BaseViewBindingFragment<VB : ViewBinding> : BaseFragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Set the binding instance. Should be called by subclasses in onCreateView.
     */
    protected fun setBinding(binding: VB) {
        _binding = binding
    }
}
