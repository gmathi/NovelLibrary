package io.github.gmathi.novellibrary.model.other

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * A TTS text filtering base.
 *
 * Both lookup and replace support substitutions for chapter-specific data by using ${name} syntax.
 * The following substitutions are supported:
 *  host -> chapter hostname, i.e. `blog.translatorwebsite.com`
 *  domain_top -> top-level domain, i.e. `com`
 *  domain -> hostname excluding top-level domain, i.e. `blog.translatorwebsite`
 *  location -> full chapter URL, i.e. `https://blos.translatorwebsiter.com/our/fancy/translation.html
 *  path -> the local path of the URL, i.e. `our/fancy/translation.html`
 *  dot_host -> chapter hostname, but all dots ('.') are replaced with ' dot '
 */
data class TTSFilter(val type: TTSFilterType, val target: TTSFilterTarget, val lookup: String, val regexFlags: String? = "", val replace: String? = "") {

    fun compile(doc: Document?): CompiledTTSFilter {
        return when (type) {
            TTSFilterType.Regex -> RegexTTSFilter(this, doc)
            TTSFilterType.Plaintext -> PlaintextTTSFilter(this, doc)
            TTSFilterType.Selector -> SelectorTTSFilter(this, doc)
        }
    }

    val regexOptions: Set<RegexOption>
        get() =
            (regexFlags?:"").mapNotNull {
                when (it) {
                    'i' -> RegexOption.IGNORE_CASE
                    'm' -> RegexOption.MULTILINE
                    's' -> RegexOption.DOT_MATCHES_ALL
                    // Non-canonical flags
                    'l' -> RegexOption.LITERAL
                    'n' -> RegexOption.UNIX_LINES
                    'c' -> RegexOption.COMMENTS
                    'C' -> RegexOption.CANON_EQ
                    else -> null
                }
            }.toSet()

}

data class TTSFilterList(val version: String, val list: List<TTSFilter>)
data class TTSFilterSource(val id: String, val version: String, val name: String, val url: String)

abstract class CompiledTTSFilter {
    abstract fun apply(text: String): String

    protected fun substitute(lookup: String, doc: Document?, escape: Boolean = false): String {
        if (doc == null) return lookup
        val url = doc.location()?.toHttpUrlOrNull(); Regex("""(?:asd)""")
        return Regex("""\$\{\w+\}""").replace(lookup) { match ->
            val text = when (match.value) {
                "\${host}" -> url?.host ?: match.value
                "\${dot_host}" -> url?.host?.replace("www.", "")?.replace(".", " dot ") ?: match.value
                "\${domain_top}" -> url?.host?.replace("www.", "")?.substringAfterLast(".") ?: match.value
                "\${domain}" -> url?.host?.replace("www.", "")?.substringBeforeLast(".") ?: match.value
                "\${location}" -> doc.location() ?: match.value
                "\${path}" -> url?.pathSegments?.joinToString("/") ?: match.value
                else -> match.value
            }
            if (escape) text.replace(Regex("""[{.?\[\]|\\()*+^$]"""), "\\$0")
            else text
        }

    }
}

class RegexTTSFilter(base: TTSFilter, doc: Document?) : CompiledTTSFilter() {

    private val lookup: Regex = Regex(substitute(base.lookup, doc, true), base.regexOptions)
    private val replace: String = substitute(base.replace?:"", doc, true)

    override fun apply(text: String): String = lookup.replace(text, replace)

}

class PlaintextTTSFilter(base: TTSFilter, doc: Document?) : CompiledTTSFilter() {

    private val lookup: String = substitute(base.lookup, doc)
    private val replace: String = substitute(base.replace?:"", doc)
    private val ignoreCase = base.regexFlags?.contains('i')?:false
    private val fullMatch = base.regexFlags?.contains('f')?:false

    override fun apply(text: String): String =
        if (fullMatch) {
            if (text.equals(lookup, ignoreCase)) replace
            else text
        } else
            text.replace(lookup, replace, ignoreCase)

}

class SelectorTTSFilter(base: TTSFilter, doc: Document?) : CompiledTTSFilter() {
    private val lookup: String = substitute(base.lookup, doc)

    public fun apply(el: Element) {
        el.select(lookup).remove()
    }

    override fun apply(text: String): String {
        return text;
    }
}

enum class TTSFilterType {
    /**
     * A pure text replacement with exact match. Supports ${} substitutions.
     * Supported regexFlags:
     *  i -> ignore case
     *  f -> full string match
     */
    Plaintext,

    /**
     * An Regex patter to perform search with. Supports ${} substitutions.
     * Supported regexFlags are:
     *  i -> ignore case
     *  m -> multiline
     *  s -> dot matches all
     *  l -> use literal patter parsing
     *  n -> use unix line mode ('\n' as line terminator)
     *  c -> allow comments
     *  C -> use canonical decomposition
     *
     *  It's possible to use capturing groups in replace string with $1, $2, etc.
     */
    Regex,

    /**
     * Apply filtering based on a CSS selector.
     *
     * The replace field is ignored and matching elements are removed instead.
     * Filter target MUST be set to Selector.
     */
    Selector,
}

enum class TTSFilterTarget {
    /**
     * Apply filtering on a whole pure-text html elements.
     * Performed before document is turned into a string.
     */
    Element,

    /**
     * Apply filtering on a chapter text after it was converted from HTML to plain text.
     */
    TextChunk,

    /**
     * Apply filtering on a chapter text after it was decomposed into lines.
     *
     * Substitutes are not supported on that stage.
     */
    Line,

    /**
     * A combination with filter type Selector, both type and target should be set to Selector value.
     */
    Selector,
}
