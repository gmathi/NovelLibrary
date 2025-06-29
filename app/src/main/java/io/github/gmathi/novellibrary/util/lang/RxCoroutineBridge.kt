package io.github.gmathi.novellibrary.util.lang

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import rx.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/*
 * Util functions for bridging RxJava and coroutines. Taken from Tachiyomi EH/SY.
 */

@ExperimentalCoroutinesApi
suspend fun <T> Single<T>.await(subscribeOn: Scheduler? = null): T {
    return suspendCancellableCoroutine { continuation ->
        val self = if (subscribeOn != null) subscribeOn(subscribeOn) else this
        @Suppress("JoinDeclarationAndAssignment")
        lateinit var sub: Subscription
        sub = self.subscribe(
            {
                continuation.resume(it) {
                    sub.unsubscribe()
                }
            },
            {
                if (!continuation.isCancelled) {
                    continuation.resumeWithException(it)
                }
            }
        )

        continuation.invokeOnCancellation {
            sub.unsubscribe()
        }
    }
}

suspend fun Completable.awaitSuspending(subscribeOn: Scheduler? = null) {
    return suspendCancellableCoroutine { continuation ->
        val self = if (subscribeOn != null) subscribeOn(subscribeOn) else this
        lateinit var sub: Subscription
        sub = self.subscribe(
            {
                continuation.resume(Unit) {
                    sub.unsubscribe()
                }
            },
            {
                if (!continuation.isCancelled) {
                    continuation.resumeWithException(it)
                }
            }
        )

        continuation.invokeOnCancellation {
            sub.unsubscribe()
        }
    }
}

suspend fun Completable.awaitCompleted(): Unit = suspendCancellableCoroutine { cont ->
    subscribe(
        object : CompletableSubscriber {
            override fun onSubscribe(s: Subscription) {
                cont.unsubscribeOnCancellation(s)
            }

            override fun onCompleted() {
                cont.resume(Unit)
            }

            override fun onError(e: Throwable) {
                cont.resumeWithException(e)
            }
        }
    )
}

suspend fun <T> Single<T>.await(): T = suspendCancellableCoroutine { cont ->
    cont.unsubscribeOnCancellation(
        subscribe(
            object : SingleSubscriber<T>() {
                override fun onSuccess(t: T) {
                    cont.resume(t)
                }

                override fun onError(error: Throwable) {
                    cont.resumeWithException(error)
                }
            }
        )
    )
}

@OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
suspend fun <T> Observable<T>.awaitFirst(): T = first().awaitOne()

@OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
suspend fun <T> Observable<T>.awaitFirstOrDefault(default: T): T = firstOrDefault(default).awaitOne()

@OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
suspend fun <T> Observable<T>.awaitFirstOrNull(): T? = firstOrDefault(null).awaitOne()

@OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
suspend fun <T> Observable<T>.awaitFirstOrElse(defaultValue: () -> T): T = switchIfEmpty(
    Observable.fromCallable(
        defaultValue
    )
).first().awaitOne()

@OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
suspend fun <T> Observable<T>.awaitLast(): T = last().awaitOne()

@OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
suspend fun <T> Observable<T>.awaitSingle(): T = single().awaitOne()

suspend fun <T> Observable<T>.awaitSingleOrDefault(default: T): T = singleOrDefault(default).awaitOne()

suspend fun <T> Observable<T>.awaitSingleOrNull(): T? = singleOrDefault(null).awaitOne()

@OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
private suspend fun <T> Observable<T>.awaitOne(): T = suspendCancellableCoroutine { cont ->
    cont.unsubscribeOnCancellation(
        subscribe(
            object : Subscriber<T>() {
                override fun onStart() {
                    request(1)
                }

                override fun onNext(t: T) {
                    cont.resume(t)
                }

                override fun onCompleted() {
                    if (cont.isActive) cont.resumeWithException(
                        IllegalStateException(
                            "Should have invoked onNext"
                        )
                    )
                }

                override fun onError(e: Throwable) {
                    /*
                       * Rx1 observable throws NoSuchElementException if cancellation happened before
                       * element emission. To mitigate this we try to atomically resume continuation with exception:
                       * if resume failed, then we know that continuation successfully cancelled itself
                       */
                    val token = cont.tryResumeWithException(e)
                    if (token != null) {
                        cont.completeResume(token)
                    }
                }
            }
        )
    )
}

internal fun <T> CancellableContinuation<T>.unsubscribeOnCancellation(sub: Subscription) =
    invokeOnCancellation { sub.unsubscribe() }

fun <T : Any> Observable<T>.asFlow(): Flow<T> = callbackFlow {
    val observer = object : Observer<T> {
        override fun onNext(t: T) {
            trySend(t).isSuccess
        }

        override fun onError(e: Throwable) {
            close(e)
        }

        override fun onCompleted() {
            close()
        }
    }
    val subscription = subscribe(observer)
    awaitClose { subscription.unsubscribe() }
}

fun <T : Any> Flow<T>.asObservable(backpressureMode: Emitter.BackpressureMode = Emitter.BackpressureMode.NONE): Observable<T> {
    return Observable.create(
        { emitter ->
            /*
         * ATOMIC is used here to provide stable behaviour of subscribe+dispose pair even if
         * asObservable is already invoked from unconfined
         */
            val job = CoroutineScope(Dispatchers.Unconfined + SupervisorJob()).launch(start = CoroutineStart.ATOMIC) {
                try {
                    collect { emitter.onNext(it) }
                    emitter.onCompleted()
                } catch (e: Throwable) {
                    // Ignore `CancellationException` as error, since it indicates "normal cancellation"
                    if (e !is CancellationException) {
                        emitter.onError(e)
                    } else {
                        emitter.onCompleted()
                    }
                }
            }
            emitter.setCancellation { job.cancel() }
        },
        backpressureMode
    )
}
