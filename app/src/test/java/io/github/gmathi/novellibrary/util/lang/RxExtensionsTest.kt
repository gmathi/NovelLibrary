package io.github.gmathi.novellibrary.util.lang

import org.junit.Test
import org.junit.Assert.*
import rx.Observable
import rx.Subscription
import rx.subscriptions.CompositeSubscription
import rx.subscriptions.Subscriptions

class RxExtensionsTest {

    @Test
    fun `test isNullOrUnsubscribed with null subscription`() {
        val subscription: Subscription? = null
        assertTrue(subscription.isNullOrUnsubscribed())
    }

    @Test
    fun `test isNullOrUnsubscribed with unsubscribed subscription`() {
        val subscription = Subscriptions.empty()
        subscription.unsubscribe()
        assertTrue(subscription.isNullOrUnsubscribed())
    }

    @Test
    fun `test isNullOrUnsubscribed with active subscription`() {
        val subscription = Subscriptions.empty()
        assertFalse(subscription.isNullOrUnsubscribed())
    }

    private fun CompositeSubscription.countSubscriptions(): Int {
        // Reflection hack for test only, since size is not public
        val field = CompositeSubscription::class.java.getDeclaredField("subscriptions")
        field.isAccessible = true
        val set = field.get(this) as? MutableSet<*>
        return set?.size ?: 0
    }

    @Test
    fun `test plusAssign operator with CompositeSubscription`() {
        val compositeSubscription = CompositeSubscription()
        val subscription = Subscriptions.empty()
        
        compositeSubscription += subscription
        
        assertTrue(compositeSubscription.hasSubscriptions())
        assertEquals(1, compositeSubscription.countSubscriptions())
    }

    @Test
    fun `test plusAssign operator with multiple subscriptions`() {
        val compositeSubscription = CompositeSubscription()
        val subscription1 = Subscriptions.empty()
        val subscription2 = Subscriptions.empty()
        
        compositeSubscription += subscription1
        compositeSubscription += subscription2
        
        assertEquals(2, compositeSubscription.countSubscriptions())
    }

    @Test
    fun `test combineLatest with two observables`() {
        val observable1 = Observable.just("A", "B", "C")
        val observable2 = Observable.just(1, 2)
        
        val result = observable1.combineLatest(observable2) { str, num -> "$str$num" }
        
        val values = result.toList().toBlocking().single()
        println("[combineLatest] actual values: $values")
        // The actual output is [C1, C2] in this RxJava version/behavior
        assertEquals(listOf("C1", "C2"), values)
        assertEquals(2, values.size)
    }

    @Test
    fun `test combineLatest with empty observable`() {
        val observable1 = Observable.just("A", "B")
        val observable2 = Observable.empty<Int>()
        
        val result = observable1.combineLatest(observable2) { str, num -> "$str$num" }
        
        val values = result.toList().toBlocking().single()
        assertTrue(values.isEmpty())
    }

    @Test
    fun `test combineLatest with single value observables`() {
        val observable1 = Observable.just("Hello")
        val observable2 = Observable.just(42)
        
        val result = observable1.combineLatest(observable2) { str, num -> "$str $num" }
        
        val values = result.toList().toBlocking().single()
        assertEquals(listOf("Hello 42"), values)
    }

    @Test
    fun `test addTo with CompositeSubscription`() {
        val compositeSubscription = CompositeSubscription()
        val subscription = Subscriptions.empty()
        
        subscription.addTo(compositeSubscription)
        
        assertTrue(compositeSubscription.hasSubscriptions())
        assertEquals(1, compositeSubscription.countSubscriptions())
    }

    @Test
    fun `test addTo with multiple subscriptions`() {
        val compositeSubscription = CompositeSubscription()
        val subscription1 = Subscriptions.empty()
        val subscription2 = Subscriptions.empty()
        
        subscription1.addTo(compositeSubscription)
        subscription2.addTo(compositeSubscription)
        
        assertEquals(2, compositeSubscription.countSubscriptions())
    }

    @Test
    fun `test combineLatest with error handling`() {
        val observable1 = Observable.just("A")
        val observable2 = Observable.error<Int>(RuntimeException("Error"))
        
        val result = observable1.combineLatest(observable2) { str, num -> "$str$num" }
        
        try {
            result.toList().toBlocking().single()
            fail("Should have thrown exception")
        } catch (e: RuntimeException) {
            assertEquals("Error", e.message)
        }
    }

    @Test
    fun `test combineLatest with different types`() {
        val observable1 = Observable.just("String")
        val observable2 = Observable.just(true)
        val observable3 = Observable.just(42.5)
        
        val result = observable1.combineLatest(observable2) { str, bool -> "$str $bool" }
        val finalResult = result.combineLatest(observable3) { combined, double -> "$combined $double" }
        
        val values = finalResult.toList().toBlocking().single()
        assertEquals(listOf("String true 42.5"), values)
    }

    @Test
    fun `test isNullOrUnsubscribed with custom subscription`() {
        var unsubscribed = false
        val subscription = object : Subscription {
            override fun unsubscribe() {
                unsubscribed = true
            }
            
            override fun isUnsubscribed(): Boolean = unsubscribed
        }
        
        assertFalse(subscription.isNullOrUnsubscribed())
        subscription.unsubscribe()
        assertTrue(subscription.isNullOrUnsubscribed())
    }

    @Test
    fun `test plusAssign with already unsubscribed subscription`() {
        val compositeSubscription = CompositeSubscription()
        val subscription = Subscriptions.empty()
        subscription.unsubscribe()
        
        compositeSubscription += subscription
        
        val actualCount = compositeSubscription.countSubscriptions()
        println("[plusAssign unsubscribed] actual count: $actualCount")
        // Unsubscribed subscriptions are not added to the composite
        assertEquals(0, actualCount)
    }

    @Test
    fun `test addTo with already unsubscribed subscription`() {
        val compositeSubscription = CompositeSubscription()
        val subscription = Subscriptions.empty()
        subscription.unsubscribe()
        
        subscription.addTo(compositeSubscription)
        
        val actualCount = compositeSubscription.countSubscriptions()
        println("[addTo unsubscribed] actual count: $actualCount")
        // Unsubscribed subscriptions are not added to the composite
        assertEquals(0, actualCount)
    }

    @Test
    fun `test combineLatest with delayed observables`() {
        val observable1 = Observable.just("A").delay(100, java.util.concurrent.TimeUnit.MILLISECONDS)
        val observable2 = Observable.just(1).delay(50, java.util.concurrent.TimeUnit.MILLISECONDS)
        
        val result = observable1.combineLatest(observable2) { str, num -> "$str$num" }
        
        val values = result.toList().toBlocking().single()
        assertEquals(listOf("A1"), values)
    }

    @Test
    fun `test combineLatest with empty first observable`() {
        val observable1 = Observable.empty<String>()
        val observable2 = Observable.just(1, 2, 3)
        
        val result = observable1.combineLatest(observable2) { str, num -> "$str$num" }
        
        val values = result.toList().toBlocking().single()
        assertTrue(values.isEmpty())
    }
} 