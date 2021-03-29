package io.github.gmathi.novellibrary.fragment

import android.webkit.CookieManager
import androidx.fragment.app.Fragment
import com.zhkrb.cloudflare_scrape_webview.CfCallback
import com.zhkrb.cloudflare_scrape_webview.Cloudflare
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.model.source.online.HttpSource
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.DataCenter
import uy.kohesive.injekt.injectLazy
import java.net.HttpCookie


open class BaseFragment : Fragment() {
    val dataCenter: DataCenter by injectLazy()
    val dbHelper: DBHelper by injectLazy()
    val sourceManager: SourceManager by injectLazy()
    val networkHelper: NetworkHelper by injectLazy()

    fun resolveCloudflare(url: String, completionBlock: (success: Boolean, url: String, errorMessage: String?) -> Void) {
        val cf = Cloudflare(requireActivity(), url);
        cf.user_agent = HttpSource.DEFAULT_USERAGENT
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
