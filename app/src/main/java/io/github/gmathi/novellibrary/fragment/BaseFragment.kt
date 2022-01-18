package io.github.gmathi.novellibrary.fragment

import android.os.Bundle
import android.webkit.CookieManager
import androidx.fragment.app.Fragment
import com.google.firebase.analytics.FirebaseAnalytics
import com.zhkrb.cloudflare_scrape_webview.CfCallback
import com.zhkrb.cloudflare_scrape_webview.Cloudflare
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.model.source.online.HttpSource
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.system.DataAccessor
import uy.kohesive.injekt.injectLazy
import java.net.HttpCookie


open class BaseFragment : Fragment(), DataAccessor {

    override val firebaseAnalytics: FirebaseAnalytics by injectLazy()
    override val dataCenter: DataCenter by injectLazy()
    override val dbHelper: DBHelper by injectLazy()
    override val sourceManager: SourceManager by injectLazy()
    override val networkHelper: NetworkHelper by injectLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun resolveCloudflare(url: String, completionBlock: (success: Boolean, url: String, errorMessage: String?) -> Void) {
        val cf = Cloudflare(requireActivity(), url);
        cf.user_agent = HttpSource.DEFAULT_USER_AGENT
        cf.setCfCallback(object : CfCallback {
            override fun onSuccess(cookieList: MutableList<HttpCookie>?, hasNewUrl: Boolean, newUrl: String?) {
                val manager = CookieManager.getInstance()
                cookieList?.forEach { manager.setCookie(newUrl ?: url, it.toString()) }
                completionBlock(true, newUrl ?: url, null)
            }

            override fun onFail(code: Int, msg: String?) {
                completionBlock(false, url, msg)
            }
        })
        cf.getCookies()
    }
}
