import io.github.gmathi.novellibrary.network.NetworkHelper
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import android.content.Context
import androidx.test.core.app.ApplicationProvider

class NetworkHelperCacheControlTest {
    @Test
    fun testCacheableRequestBuilder() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val helper = NetworkHelper(context)
        val req = Request.Builder().url("http://test.com").build()
        val cacheable = helper.cacheableRequestBuilder(req, 123).build()
        assertEquals("public, max-age=123", cacheable.header("Cache-Control"))
    }

    @Test
    fun testForceRefreshRequestBuilder() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val helper = NetworkHelper(context)
        val req = Request.Builder().url("http://test.com").build()
        val refresh = helper.forceRefreshRequestBuilder(req).build()
        assertEquals("no-cache, max-age=0", refresh.header("Cache-Control"))
    }

    @Test
    fun testOfflineOnlyRequestBuilder() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val helper = NetworkHelper(context)
        val req = Request.Builder().url("http://test.com").build()
        val offline = helper.offlineOnlyRequestBuilder(req).build()
        assertEquals("only-if-cached, max-stale=86400", offline.header("Cache-Control"))
    }
} 