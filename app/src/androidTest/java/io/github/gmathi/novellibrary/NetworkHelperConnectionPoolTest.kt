import io.github.gmathi.novellibrary.network.NetworkHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import android.content.Context
import java.util.concurrent.TimeUnit
import androidx.test.core.app.ApplicationProvider

class NetworkHelperConnectionPoolTest {
    @Test
    fun testConnectionPoolConfiguration() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val helper = NetworkHelper(context)
        val pool = helper.client.connectionPool
        val maxIdleField = pool.javaClass.getDeclaredField("maxIdleConnections")
        maxIdleField.isAccessible = true
        val maxIdle = maxIdleField.getInt(pool)
        val keepAliveField = pool.javaClass.getDeclaredField("keepAliveDurationNs")
        keepAliveField.isAccessible = true
        val keepAlive = keepAliveField.getLong(pool)
        assertEquals(20, maxIdle)
        assertEquals(2, keepAlive / java.util.concurrent.TimeUnit.MINUTES.toNanos(1))
    }
} 