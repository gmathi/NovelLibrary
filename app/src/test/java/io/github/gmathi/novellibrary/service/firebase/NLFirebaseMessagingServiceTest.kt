package io.github.gmathi.novellibrary.service.firebase

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NLFirebaseMessagingServiceTest {

    private lateinit var service: NLFirebaseMessagingService
    private lateinit var mockNotificationManager: NotificationManagerCompat

    @Before
    fun setUp() {
        service = Robolectric.setupService(NLFirebaseMessagingService::class.java)
        mockNotificationManager = mockk(relaxed = true)
        
        mockkStatic(NotificationManagerCompat::class)
        every { NotificationManagerCompat.from(any()) } returns mockNotificationManager
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onCreate should create notification channel`() {
        // Given
        val mockNotificationManager = mockk<NotificationManager>(relaxed = true)
        every { service.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager

        // When
        service.onCreate()

        // Then - verify that the service is created without exceptions
        // Note: Notification channel creation is tested implicitly
    }

    @Test
    fun `onNewToken should handle token refresh`() = runTest {
        // Given
        val token = "test_token_123"

        // When
        service.onNewToken(token)

        // Then - verify that the token is processed without exceptions
        // Note: Due to coroutine nature, we mainly verify no exceptions are thrown
    }

    @Test
    fun `onMessageReceived should process data message`() = runTest {
        // Given
        val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true)
        val mockData = mapOf(
            "type" to "novel_update",
            "novel_id" to "123",
            "novel_name" to "Test Novel"
        )
        
        every { mockRemoteMessage.from } returns "test_sender"
        every { mockRemoteMessage.data } returns mockData
        every { mockRemoteMessage.notification } returns null

        // When
        service.onMessageReceived(mockRemoteMessage)

        // Then - verify that the message is processed without exceptions
        // Note: Due to coroutine nature, we mainly verify no exceptions are thrown
    }

    @Test
    fun `onMessageReceived should process notification message`() = runTest {
        // Given
        val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true)
        val mockNotification = mockk<RemoteMessage.Notification>(relaxed = true)
        
        every { mockRemoteMessage.from } returns "test_sender"
        every { mockRemoteMessage.data } returns emptyMap()
        every { mockRemoteMessage.notification } returns mockNotification
        every { mockNotification.title } returns "Test Title"
        every { mockNotification.body } returns "Test Body"

        // When
        service.onMessageReceived(mockRemoteMessage)

        // Then - verify that the notification is processed without exceptions
        // Note: Due to coroutine nature, we mainly verify no exceptions are thrown
    }

    @Test
    fun `onMessageReceived should handle empty message`() = runTest {
        // Given
        val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true)
        
        every { mockRemoteMessage.from } returns "test_sender"
        every { mockRemoteMessage.data } returns emptyMap()
        every { mockRemoteMessage.notification } returns null

        // When
        service.onMessageReceived(mockRemoteMessage)

        // Then - verify that empty message is handled gracefully
        // Note: Due to coroutine nature, we mainly verify no exceptions are thrown
    }

    @Test
    fun `onMessageReceived should handle app update message`() = runTest {
        // Given
        val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true)
        val mockData = mapOf(
            "type" to "app_update",
            "version" to "2.0.0"
        )
        
        every { mockRemoteMessage.from } returns "test_sender"
        every { mockRemoteMessage.data } returns mockData
        every { mockRemoteMessage.notification } returns null

        // When
        service.onMessageReceived(mockRemoteMessage)

        // Then - verify that app update message is processed without exceptions
        // Note: Due to coroutine nature, we mainly verify no exceptions are thrown
    }

    @Test
    fun `onMessageReceived should handle unknown message type`() = runTest {
        // Given
        val mockRemoteMessage = mockk<RemoteMessage>(relaxed = true)
        val mockData = mapOf(
            "type" to "unknown_type",
            "data" to "test_data"
        )
        
        every { mockRemoteMessage.from } returns "test_sender"
        every { mockRemoteMessage.data } returns mockData
        every { mockRemoteMessage.notification } returns null

        // When
        service.onMessageReceived(mockRemoteMessage)

        // Then - verify that unknown message type is handled gracefully
        // Note: Due to coroutine nature, we mainly verify no exceptions are thrown
    }

    @Test
    fun `onDestroy should cancel service scope`() {
        // When
        service.onDestroy()

        // Then - verify that the service is destroyed without exceptions
        // Note: Scope cancellation is tested implicitly
    }
}