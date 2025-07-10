package io.github.gmathi.novellibrary.util.lang

import org.junit.Test
import org.junit.Assert.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class DateExtensionsTest {

    @Test
    fun `test toDateTimestampString with custom formatter`() {
        val date = Date(1640995200000) // 2022-01-01 00:00:00 UTC
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        val result = date.toDateTimestampString(formatter)
        println("[toDateTimestampString] actual: '$result'")
        
        assertNotNull(result)
        // Accept either 2021-12-31 or 2022-01-01 depending on timezone
        assertTrue(result.startsWith("2021-12-31") || result.startsWith("2022-01-01"))
        assertTrue(result.contains(":")) // Should contain time
        assertTrue(result.contains(" ")) // Should contain space between date and time
    }

    @Test
    fun `test toDateTimestampString with different locale`() {
        val date = Date(1640995200000) // 2022-01-01 00:00:00 UTC
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        
        val result = date.toDateTimestampString(formatter)
        println("[toDateTimestampString different locale] actual: '$result'")
        
        assertNotNull(result)
        // Accept either 31/12/2021 or 01/01/2022 depending on timezone
        assertTrue(result.contains("31/12/2021") || result.contains("01/01/2022"))
        assertTrue(result.contains(":")) // Should contain time
        assertTrue(result.contains(" ")) // Should contain space between date and time
    }

    @Test
    fun `test toTimestampString`() {
        val date = Date(1640995200000) // 2022-01-01 00:00:00 UTC
        
        val result = date.toTimestampString()
        
        assertNotNull(result)
        // The time format depends on locale, so we just check it's not empty and contains a colon
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains(":"))
    }

    @Test
    fun `test toDateKey with positive timestamp`() {
        val timestamp = 1640995200000L // 2022-01-01 00:00:00 UTC
        val result = timestamp.toDateKey()
        
        assertNotNull(result)
        
        val calendar = Calendar.getInstance()
        calendar.time = result
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
        assertEquals(0, calendar.get(Calendar.SECOND))
        assertEquals(0, calendar.get(Calendar.MILLISECOND))
    }

    @Test
    fun `test toDateKey with zero timestamp`() {
        val timestamp = 0L
        val result = timestamp.toDateKey()
        
        assertNotNull(result)
        
        val calendar = Calendar.getInstance()
        calendar.time = result
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
        assertEquals(0, calendar.get(Calendar.SECOND))
        assertEquals(0, calendar.get(Calendar.MILLISECOND))
    }

    @Test
    fun `test toDateKey with negative timestamp`() {
        val timestamp = -1640995200000L
        val result = timestamp.toDateKey()
        
        assertNotNull(result)
        
        val calendar = Calendar.getInstance()
        calendar.time = result
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
        assertEquals(0, calendar.get(Calendar.SECOND))
        assertEquals(0, calendar.get(Calendar.MILLISECOND))
    }

    @Test
    fun `test toCalendar with positive timestamp`() {
        val timestamp = 1640995200000L // 2022-01-01 00:00:00 UTC
        val result = timestamp.toCalendar()
        
        assertNotNull(result)
        assertEquals(timestamp, result?.timeInMillis)
    }

    @Test
    fun `test toCalendar with zero timestamp returns null`() {
        val timestamp = 0L
        val result = timestamp.toCalendar()
        
        assertNull(result)
    }

    @Test
    fun `test toCalendar with negative timestamp`() {
        val timestamp = -1640995200000L
        val result = timestamp.toCalendar()
        
        assertNotNull(result)
        assertEquals(timestamp, result?.timeInMillis)
    }

    @Test
    fun `test toDateKey preserves date information`() {
        val timestamp = 1640995200000L // 2022-01-01 00:00:00 UTC
        val originalDate = Date(timestamp)
        val result = timestamp.toDateKey()
        
        val originalCalendar = Calendar.getInstance()
        originalCalendar.time = originalDate
        
        val resultCalendar = Calendar.getInstance()
        resultCalendar.time = result
        
        assertEquals(originalCalendar.get(Calendar.YEAR), resultCalendar.get(Calendar.YEAR))
        assertEquals(originalCalendar.get(Calendar.MONTH), resultCalendar.get(Calendar.MONTH))
        assertEquals(originalCalendar.get(Calendar.DAY_OF_MONTH), resultCalendar.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `test toDateTimestampString with different time zones`() {
        val date = Date(1640995200000) // 2022-01-01 00:00:00 UTC
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        
        val result = date.toDateTimestampString(formatter)
        
        assertNotNull(result)
        assertTrue(result.contains("2022-01-01"))
    }

    @Test
    fun `test toTimestampString with different locales`() {
        val date = Date(1640995200000) // 2022-01-01 00:00:00 UTC
        
        // Test with different default locales
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            val usResult = date.toTimestampString()
            assertNotNull(usResult)
            
            Locale.setDefault(Locale.FRANCE)
            val frResult = date.toTimestampString()
            assertNotNull(frResult)
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `test toDateKey edge cases`() {
        // Test with very large timestamp
        val largeTimestamp = Long.MAX_VALUE
        val largeResult = largeTimestamp.toDateKey()
        assertNotNull(largeResult)
        
        // Test with very small timestamp
        val smallTimestamp = Long.MIN_VALUE
        val smallResult = smallTimestamp.toDateKey()
        assertNotNull(smallResult)
    }

    @Test
    fun `test toCalendar edge cases`() {
        // Test with very large timestamp
        val largeTimestamp = Long.MAX_VALUE
        val largeResult = largeTimestamp.toCalendar()
        assertNotNull(largeResult)
        
        // Test with very small timestamp
        val smallTimestamp = Long.MIN_VALUE
        val smallResult = smallTimestamp.toCalendar()
        assertNotNull(smallResult)
    }
} 