package io.github.gmathi.novellibrary.util.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPagerScriptTest {

    @Test
    fun `build stamps the document generation into the script`() {
        val script = ReaderPagerScript.build(42)

        assertTrue(script.contains("var GEN = 42;"))
        assertFalse(script.contains("__GENERATION__"))
    }

    @Test
    fun `every call back into the app passes the generation`() {
        val script = ReaderPagerScript.build(7)
        val calls = Regex("""bridge\.(\w+)\(([^)]*)\)""").findAll(script).toList()

        assertEquals(
            setOf("pagerInit", "onPageChanged", "onLastPageReached", "onChapterBoundary", "onCenterTap"),
            calls.map { it.groupValues[1] }.toSet()
        )
        calls.forEach { assertTrue("${it.value} must pass GEN first", it.groupValues[2].startsWith("GEN")) }
    }

    @Test
    fun `script can be embedded in a script element`() {
        assertFalse(ReaderPagerScript.build(1).contains("</script", ignoreCase = true))
    }
}
