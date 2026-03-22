package io.github.gmathi.novellibrary.common.model

import android.graphics.drawable.ColorDrawable
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

/**
 * Unit tests for ReaderMenu model class
 */
class ReaderMenuTest : StringSpec({
    
    "ReaderMenu should be created with icon and title" {
        val icon = mockk<ColorDrawable>()
        val title = "Test Menu"
        
        val menu = ReaderMenu(icon = icon, title = title)
        
        menu.icon shouldBe icon
        menu.title shouldBe title
    }
    
    "ReaderMenu should allow modifying icon" {
        val icon1 = mockk<ColorDrawable>()
        val icon2 = mockk<ColorDrawable>()
        val menu = ReaderMenu(icon = icon1, title = "Test")
        
        menu.icon = icon2
        
        menu.icon shouldBe icon2
    }
    
    "ReaderMenu should allow modifying title" {
        val icon = mockk<ColorDrawable>()
        val menu = ReaderMenu(icon = icon, title = "Original")
        
        menu.title = "Modified"
        
        menu.title shouldBe "Modified"
    }
})
