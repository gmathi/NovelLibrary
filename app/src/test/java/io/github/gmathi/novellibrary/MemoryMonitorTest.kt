import io.github.gmathi.novellibrary.util.system.MemoryMonitor
import io.github.gmathi.novellibrary.util.system.MemoryUsage
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import android.app.ActivityManager
import android.content.Context
import org.junit.Ignore

@Ignore("Disabled due to incompatibility with Mockito and Android SDK classes on JDK 17+.")
@RunWith(MockitoJUnitRunner::class)
class MemoryMonitorTest {
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var activityManager: ActivityManager
    
    private lateinit var memoryMonitor: MemoryMonitor
    
    @Before
    fun setup() {
        `when`(context.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(activityManager)
        memoryMonitor = MemoryMonitor(context)
    }
    
    @Test
    fun testGetMemoryUsage() {
        // Given
        val memoryInfo = ActivityManager.MemoryInfo()
        memoryInfo.availMem = 1000L * 1024 * 1024 // 1GB available
        memoryInfo.totalMem = 4000L * 1024 * 1024 // 4GB total
        memoryInfo.threshold = 500L * 1024 * 1024 // 500MB threshold
        memoryInfo.lowMemory = false
        
        // Mock the getMemoryInfo method
        doAnswer { invocation ->
            val info = invocation.getArgument<ActivityManager.MemoryInfo>(0)
            info.availMem = memoryInfo.availMem
            info.totalMem = memoryInfo.totalMem
            info.threshold = memoryInfo.threshold
            info.lowMemory = memoryInfo.lowMemory
            null
        }.`when`(activityManager).getMemoryInfo(any(ActivityManager.MemoryInfo::class.java))
        
        // When
        val usage = memoryMonitor.getMemoryUsage()
        
        // Then
        assertEquals(1000L * 1024 * 1024, usage.availableMemory)
        assertEquals(4000L * 1024 * 1024, usage.totalMemory)
        assertEquals(500L * 1024 * 1024, usage.threshold)
        assertFalse(usage.lowMemory)
    }
    
    @Test
    fun testShouldCleanupCacheWhenMemoryLow() {
        // Given
        val memoryInfo = ActivityManager.MemoryInfo()
        memoryInfo.availMem = 800L * 1024 * 1024 // 800MB available
        memoryInfo.threshold = 500L * 1024 * 1024 // 500MB threshold
        
        doAnswer { invocation ->
            val info = invocation.getArgument<ActivityManager.MemoryInfo>(0)
            info.availMem = memoryInfo.availMem
            info.threshold = memoryInfo.threshold
            null
        }.`when`(activityManager).getMemoryInfo(any(ActivityManager.MemoryInfo::class.java))
        
        // When
        val shouldCleanup = memoryMonitor.shouldCleanupCache()
        
        // Then
        assertTrue(shouldCleanup) // Available memory < threshold * 2
    }
    
    @Test
    fun testShouldNotCleanupCacheWhenMemoryAdequate() {
        // Given
        val memoryInfo = ActivityManager.MemoryInfo()
        memoryInfo.availMem = 1500L * 1024 * 1024 // 1.5GB available
        memoryInfo.threshold = 500L * 1024 * 1024 // 500MB threshold
        
        doAnswer { invocation ->
            val info = invocation.getArgument<ActivityManager.MemoryInfo>(0)
            info.availMem = memoryInfo.availMem
            info.threshold = memoryInfo.threshold
            null
        }.`when`(activityManager).getMemoryInfo(any(ActivityManager.MemoryInfo::class.java))
        
        // When
        val shouldCleanup = memoryMonitor.shouldCleanupCache()
        
        // Then
        assertFalse(shouldCleanup) // Available memory >= threshold * 2
    }
    
    @Test
    fun testGetMemoryUsagePercentage() {
        // Given
        val memoryInfo = ActivityManager.MemoryInfo()
        memoryInfo.availMem = 1000L * 1024 * 1024 // 1GB available
        memoryInfo.totalMem = 4000L * 1024 * 1024 // 4GB total
        
        doAnswer { invocation ->
            val info = invocation.getArgument<ActivityManager.MemoryInfo>(0)
            info.availMem = memoryInfo.availMem
            info.totalMem = memoryInfo.totalMem
            null
        }.`when`(activityManager).getMemoryInfo(any(ActivityManager.MemoryInfo::class.java))
        
        // When
        val percentage = memoryMonitor.getMemoryUsagePercentage()
        
        // Then
        assertEquals(75.0f, percentage, 0.1f) // 75% used (3000MB / 4000MB)
    }
    
    @Test
    fun testIsLowMemory() {
        // Given
        val memoryInfo = ActivityManager.MemoryInfo()
        memoryInfo.lowMemory = true
        
        doAnswer { invocation ->
            val info = invocation.getArgument<ActivityManager.MemoryInfo>(0)
            info.lowMemory = memoryInfo.lowMemory
            null
        }.`when`(activityManager).getMemoryInfo(any(ActivityManager.MemoryInfo::class.java))
        
        // When
        val isLowMemory = memoryMonitor.isLowMemory()
        
        // Then
        assertTrue(isLowMemory)
    }
    
    @Test
    fun testGetAvailableMemoryMB() {
        // Given
        val memoryInfo = ActivityManager.MemoryInfo()
        memoryInfo.availMem = 2048L * 1024 * 1024 // 2GB available
        
        doAnswer { invocation ->
            val info = invocation.getArgument<ActivityManager.MemoryInfo>(0)
            info.availMem = memoryInfo.availMem
            null
        }.`when`(activityManager).getMemoryInfo(any(ActivityManager.MemoryInfo::class.java))
        
        // When
        val availableMB = memoryMonitor.getAvailableMemoryMB()
        
        // Then
        assertEquals(2048L, availableMB)
    }
    
    @Test
    fun testGetTotalMemoryMB() {
        // Given
        val memoryInfo = ActivityManager.MemoryInfo()
        memoryInfo.totalMem = 8192L * 1024 * 1024 // 8GB total
        
        doAnswer { invocation ->
            val info = invocation.getArgument<ActivityManager.MemoryInfo>(0)
            info.totalMem = memoryInfo.totalMem
            null
        }.`when`(activityManager).getMemoryInfo(any(ActivityManager.MemoryInfo::class.java))
        
        // When
        val totalMB = memoryMonitor.getTotalMemoryMB()
        
        // Then
        assertEquals(8192L, totalMB)
    }
} 