package io.github.gmathi.novellibrary.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.github.gmathi.novellibrary.util.Logs
import java.util.concurrent.TimeUnit
import kotlin.concurrent.withLock
import java.util.concurrent.locks.ReentrantLock

/**
 * Database connection pool manager for improved performance and resource management.
 * Manages a pool of SQLiteDatabase connections to reduce connection overhead.
 */
class DatabaseManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "DatabaseManager"
        private const val MAX_POOL_SIZE = 3
        private const val CONNECTION_TIMEOUT_MS = 5000L
        
        @Volatile
        private var instance: DatabaseManager? = null
        
        @Synchronized
        fun getInstance(context: Context): DatabaseManager {
            return instance ?: DatabaseManager(context.applicationContext).also {
                instance = it
            }
        }
        
        @Synchronized
        fun refreshInstance(context: Context): DatabaseManager {
            instance?.close()
            return DatabaseManager(context.applicationContext).also {
                instance = it
            }
        }
    }
    
    private val databasePool = mutableListOf<SQLiteDatabase>()
    private val poolLock = ReentrantLock()
    private val dbHelper = DBHelper.getInstance(context)
    private var isClosed = false
    
    /**
     * Get a database connection from the pool.
     * Creates a new connection if pool is empty.
     */
    fun getDatabase(): SQLiteDatabase {
        check(!isClosed) { "DatabaseManager is closed" }
        
        return poolLock.withLock {
            if (databasePool.isNotEmpty()) {
                val db = databasePool.removeAt(0)
                if (db.isOpen) {
                    Logs.debug(TAG, "Reusing database connection from pool")
                    db
                } else {
                    Logs.debug(TAG, "Database connection was closed, creating new one")
                    createNewDatabase()
                }
            } else {
                createNewDatabase()
            }
        }
    }
    
    /**
     * Get a writable database connection from the pool.
     * Creates a new writable connection if pool is empty.
     */
    fun getWritableDatabase(): SQLiteDatabase {
        check(!isClosed) { "DatabaseManager is closed" }
        
        return poolLock.withLock {
            if (databasePool.isNotEmpty()) {
                val db = databasePool.removeAt(0)
                if (db.isOpen && !db.isReadOnly) {
                    Logs.debug(TAG, "Reusing writable database connection from pool")
                    db
                } else {
                    Logs.debug(TAG, "Creating new writable database connection")
                    createNewWritableDatabase()
                }
            } else {
                createNewWritableDatabase()
            }
        }
    }
    
    /**
     * Return a database connection to the pool.
     * Closes the connection if pool is full.
     */
    fun returnDatabase(db: SQLiteDatabase) {
        if (isClosed) {
            db.close()
            return
        }
        
        poolLock.withLock {
            if (databasePool.size < MAX_POOL_SIZE && db.isOpen) {
                Logs.debug(TAG, "Returning database connection to pool (size: ${databasePool.size + 1})")
                databasePool.add(db)
            } else {
                Logs.debug(TAG, "Closing database connection (pool full or connection closed)")
                db.close()
            }
        }
    }
    
    /**
     * Execute a database operation with automatic connection management.
     */
    fun <T> executeWithDatabase(operation: (SQLiteDatabase) -> T): T {
        val db = getDatabase()
        return try {
            operation(db)
        } finally {
            returnDatabase(db)
        }
    }
    
    /**
     * Execute a writable database operation with automatic connection management.
     */
    fun <T> executeWithWritableDatabase(operation: (SQLiteDatabase) -> T): T {
        val db = getWritableDatabase()
        return try {
            operation(db)
        } finally {
            returnDatabase(db)
        }
    }
    
    /**
     * Execute a database transaction with automatic connection management.
     */
    fun <T> executeTransaction(operation: (SQLiteDatabase) -> T): T {
        return executeWithWritableDatabase { db ->
            var result: T? = null
            db.runTransaction { transactionDb ->
                result = operation(transactionDb)
            }
            result!!
        }
    }
    
    /**
     * Get pool statistics for monitoring.
     */
    fun getPoolStats(): PoolStats {
        return poolLock.withLock {
            PoolStats(
                poolSize = databasePool.size,
                maxPoolSize = MAX_POOL_SIZE,
                isClosed = isClosed
            )
        }
    }
    
    /**
     * Close all database connections and cleanup resources.
     */
    fun close() {
        if (isClosed) return
        
        isClosed = true
        poolLock.withLock {
            databasePool.forEach { db ->
                if (db.isOpen) {
                    db.close()
                }
            }
            databasePool.clear()
        }
        Logs.debug(TAG, "DatabaseManager closed, all connections cleaned up")
    }
    
    private fun createNewDatabase(): SQLiteDatabase {
        Logs.debug(TAG, "Creating new database connection")
        return dbHelper.readableDatabase
    }
    
    private fun createNewWritableDatabase(): SQLiteDatabase {
        Logs.debug(TAG, "Creating new writable database connection")
        return dbHelper.writableDatabase
    }
    
    /**
     * Statistics about the database connection pool.
     */
    data class PoolStats(
        val poolSize: Int,
        val maxPoolSize: Int,
        val isClosed: Boolean
    ) {
        val poolUtilization: Float = poolSize.toFloat() / maxPoolSize
        val isPoolFull: Boolean = poolSize >= maxPoolSize
    }
}

/**
 * Extension function to run database operations with automatic connection management.
 */
fun <T> DatabaseManager.withDatabase(operation: (SQLiteDatabase) -> T): T {
    return executeWithDatabase(operation)
}

/**
 * Extension function to run writable database operations with automatic connection management.
 */
fun <T> DatabaseManager.withWritableDatabase(operation: (SQLiteDatabase) -> T): T {
    return executeWithWritableDatabase(operation)
}

/**
 * Extension function to run database transactions with automatic connection management.
 */
fun <T> DatabaseManager.withTransaction(operation: (SQLiteDatabase) -> T): T {
    return executeTransaction(operation)
} 