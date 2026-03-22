package io.github.gmathi.novellibrary.common.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unit tests for SettingItem model class
 */
class SettingItemTest : StringSpec({
    
    "SettingItem should be created with name and description" {
        val item = SettingItem<Any, Any>(name = 1, description = 2)
        
        item.name shouldBe 1
        item.description shouldBe 2
        item.bindCallback shouldBe null
        item.clickCallback shouldBe null
    }
    
    "SettingItem should allow setting bind callback" {
        val item = SettingItem<Any, Any>(name = 1, description = 2)
        val callback: SettingItemBindingCallback<Any, Any> = { _, _, _ -> }
        
        val result = item.onBind(callback)
        
        result shouldBe item
        item.bindCallback shouldNotBe null
    }
    
    "SettingItem should allow setting click callback" {
        val item = SettingItem<Any, Any>(name = 1, description = 2)
        val callback: SettingItemClickCallback<Any, Any> = { _, _ -> }
        
        val result = item.onClick(callback)
        
        result shouldBe item
        item.clickCallback shouldNotBe null
    }
    
    "SettingItem should support method chaining" {
        val item = SettingItem<Any, Any>(name = 1, description = 2)
        val bindCallback: SettingItemBindingCallback<Any, Any> = { _, _, _ -> }
        val clickCallback: SettingItemClickCallback<Any, Any> = { _, _ -> }
        
        val result = item.onBind(bindCallback).onClick(clickCallback)
        
        result shouldBe item
        item.bindCallback shouldNotBe null
        item.clickCallback shouldNotBe null
    }
})
