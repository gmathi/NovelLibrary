package io.github.gmathi.novellibrary.util.lang

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import java.text.DateFormat
import java.util.*

/**
 * Unit tests for Date extension functions
 */
class DateExtensionsTest : StringSpec({
    
    "toDateTimestampString should format date with time" {
        val date = Date(1609459200000L) // 2021-01-01 00:00:00 UTC
        val dateFormatter = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
        val result = date.toDateTimestampString(dateFormatter)
        
        // Result should contain both date and time components
        result shouldNotBe ""
        result.length shouldBeGreaterThan 5
    }
    
    "toTimestampString should format time only" {
        val date = Date(1609459200000L)
        val result = date.toTimestampString()
        
        // Result should be a time string
        result shouldNotBe ""
        result.length shouldBeGreaterThan 3
    }
    
    "toDateKey should zero out time components" {
        val timestamp = 1609459200000L + (3600000 * 5) + (60000 * 30) + 45000 // Add 5h 30m 45s
        val dateKey = timestamp.toDateKey()
        
        val cal = Calendar.getInstance()
        cal.time = dateKey
        
        // Time components should be zeroed
        cal[Calendar.HOUR_OF_DAY] shouldBe 0
        cal[Calendar.MINUTE] shouldBe 0
        cal[Calendar.SECOND] shouldBe 0
        cal[Calendar.MILLISECOND] shouldBe 0
    }
    
    "toCalendar should convert epoch to Calendar" {
        val timestamp = 1609459200000L
        val calendar = timestamp.toCalendar()
        
        calendar shouldNotBe null
        calendar!!.timeInMillis shouldBe timestamp
    }
    
    "toCalendar should return null for zero epoch" {
        val calendar = 0L.toCalendar()
        calendar shouldBe null
    }
})
