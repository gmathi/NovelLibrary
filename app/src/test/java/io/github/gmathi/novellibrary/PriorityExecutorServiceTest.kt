import io.github.gmathi.novellibrary.network.PriorityExecutorService
import io.github.gmathi.novellibrary.network.RequestPriority
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

class PriorityExecutorServiceTest {
    @Test
    fun testPriorityExecutionOrder() {
        val executor = PriorityExecutorService(3, 3, 60L, TimeUnit.SECONDS)
        val results = mutableListOf<Int>()
        val latch = java.util.concurrent.CountDownLatch(3)

        executor.submit(Callable {
            Thread.sleep(50)
            results.add(1)
            latch.countDown()
        }, RequestPriority.LOW)
        executor.submit(Callable {
            results.add(2)
            latch.countDown()
        }, RequestPriority.HIGH)
        executor.submit(Callable {
            results.add(3)
            latch.countDown()
        }, RequestPriority.NORMAL)

        latch.await(1, TimeUnit.SECONDS)
        // The HIGH priority task should be first, then NORMAL, then LOW
        assertEquals(listOf(2, 3, 1), results)
    }

    @Test
    fun testSamePriorityOrder() {
        val executor = PriorityExecutorService(1, 1, 60L, TimeUnit.SECONDS)
        val results = mutableListOf<Int>()
        val latch = java.util.concurrent.CountDownLatch(2)

        executor.submit(Callable {
            results.add(1)
            latch.countDown()
        }, RequestPriority.NORMAL)
        executor.submit(Callable {
            results.add(2)
            latch.countDown()
        }, RequestPriority.NORMAL)

        latch.await(1, TimeUnit.SECONDS)
        // For same priority, order of submission should be preserved
        assertEquals(listOf(1, 2), results)
    }
} 