package io.github.gmathi.novellibrary.model.source.online

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [NovelUpdatesSource.parseCoverUrl] must find the series cover across the markup variants
 * NovelUpdates has used, and must return null (never a bogus value) when there is no cover, so
 * that the details parser keeps the cover URL that came from the search result.
 */
class NovelUpdatesCoverParseTest {

    private val base = "https://www.novelupdates.com/series/supreme-magus/"
    private val cover = "https://cdn.novelupdates.com/images/2019/06/supreme-magus.jpg"

    private fun parse(body: String) =
        NovelUpdatesSource.parseCoverUrl(Jsoup.parse("<html><head></head><body>$body</body></html>", base))

    @Test
    fun directChildImage() {
        assertEquals(cover, parse("""<div class="seriesimg"><img src="$cover" alt=""></div>"""))
    }

    @Test
    fun imageNestedInsideLink() {
        assertEquals(cover, parse("""<div class="seriesimg"><a href="$cover"><img src="$cover"></a></div>"""))
    }

    @Test
    fun imageNestedInsidePicture() {
        assertEquals(cover, parse("""<div class="seriesimg"><picture><source srcset="x.webp"><img src="$cover"></picture></div>"""))
    }

    @Test
    fun lazyLoadedImageWithPlaceholderSrc() {
        val html = """<div class="seriesimg"><img src="data:image/gif;base64,R0lGODlhAQABAAAAACw=" data-src="$cover"></div>"""
        assertEquals(cover, parse(html))
    }

    @Test
    fun lazyLoadedImageWithLazySrcAttribute() {
        assertEquals(cover, parse("""<div class="seriesimg"><img data-lazy-src="$cover"></div>"""))
    }

    @Test
    fun relativeUrlIsResolvedAgainstPage() {
        assertEquals(
            "https://www.novelupdates.com/images/2019/06/supreme-magus.jpg",
            parse("""<div class="seriesimg"><img src="/images/2019/06/supreme-magus.jpg"></div>""")
        )
    }

    @Test
    fun editPageVariant() {
        assertEquals(cover, parse("""<div class="serieseditimg"><img src="$cover"></div>"""))
    }

    @Test
    fun fallsBackToOpenGraphImage() {
        val doc = Jsoup.parse(
            """<html><head><meta property="og:image" content="$cover"></head><body><div class="w-blog-content">no cover block</div></body></html>""",
            base
        )
        assertEquals(cover, NovelUpdatesSource.parseCoverUrl(doc))
    }

    @Test
    fun coverBlockWithoutUsableUrlStillFallsBackToOpenGraph() {
        val doc = Jsoup.parse(
            """<html><head><meta property="og:image" content="$cover"></head><body><div class="seriesimg"><img src="data:image/gif;base64,R0lGODlhAQABAAAAACw="></div></body></html>""",
            base
        )
        assertEquals(cover, NovelUpdatesSource.parseCoverUrl(doc))
    }

    @Test
    fun returnsNullWhenNothingUsable() {
        assertNull(parse("""<div class="seriestitlenu">Supreme Magus</div>"""))
        assertNull(parse("""<div class="seriesimg"><img src=""></div>"""))
    }
}
