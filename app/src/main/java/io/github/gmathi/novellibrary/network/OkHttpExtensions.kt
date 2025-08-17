package io.github.gmathi.novellibrary.network

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.internal.closeQuietly
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.fullType
import java.io.IOException
import kotlin.coroutines.resumeWithException

val jsonMime = "application/json; charset=utf-8".toMediaType()

// Based on https://github.com/gildor/kotlin-coroutines-okhttp
suspend fun Call.await(): Response {
    return suspendCancellableCoroutine { continuation ->
        enqueue(
            object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        continuation.resumeWithException(Exception("HTTP error ${response.code}"))
                        return
                    }

                    continuation.resume(response) {
                        response.body?.closeQuietly()
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    // Don't bother with resuming the continuation if it is already cancelled.
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(e)
                }
            }
        )

        continuation.invokeOnCancellation {
            try {
                cancel()
            } catch (ex: Throwable) {
                // Ignore cancel exception
            }
        }
    }
}



/**
 * Coroutine-based replacement for asObservableSuccess().
 * Executes the call and throws an exception if not successful.
 */
suspend fun Call.awaitSuccess(): Response {
    val response = await()
    if (!response.isSuccessful) {
        response.close()
        throw Exception("HTTP error ${response.code}")
    }
    return response
}

fun OkHttpClient.newCallWithProgress(request: Request, listener: ProgressListener): Call {
    val progressClient = newBuilder()
        .cache(null)
        .addNetworkInterceptor { chain ->
            val originalResponse = chain.proceed(chain.request())
            originalResponse.newBuilder()
                .body(ProgressResponseBody(originalResponse.body!!, listener))
                .build()
        }
        .build()

    return progressClient.newCall(request)
}

@ExperimentalSerializationApi
inline fun <reified T> Response.parseAs(): T {
    // Avoiding Injekt.get<Json>() due to compiler issues
    val json = Injekt.getInstance<Json>(fullType<Json>().type)
    this.use {
        val responseBody = it.body?.string().orEmpty()
        return json.decodeFromString(responseBody)
    }
}
