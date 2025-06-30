package io.github.gmathi.novellibrary.util.system

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ObjectPool<T>(
    private val factory: () -> T,
    private val reset: (T) -> Unit,
    private val maxSize: Int = 10
) {
    private val pool = ArrayDeque<T>(maxSize)
    private val lock = ReentrantLock()
    
    fun acquire(): T {
        return lock.withLock {
            pool.removeFirstOrNull() ?: factory()
        }
    }
    
    fun release(obj: T) {
        lock.withLock {
            if (pool.size < maxSize) {
                reset(obj)
                pool.addLast(obj)
            }
        }
    }
    
    fun size(): Int {
        return lock.withLock { pool.size }
    }
    
    fun clear() {
        lock.withLock { pool.clear() }
    }
    
    fun isPoolEmpty(): Boolean {
        return lock.withLock { pool.isEmpty() }
    }
    
    fun isPoolFull(): Boolean {
        return lock.withLock { pool.size >= maxSize }
    }
}

// Convenience extension for using object pools with automatic release
inline fun <T, R> ObjectPool<T>.useObject(block: (T) -> R): R {
    val obj = acquire()
    return try {
        block(obj)
    } finally {
        release(obj)
    }
} 