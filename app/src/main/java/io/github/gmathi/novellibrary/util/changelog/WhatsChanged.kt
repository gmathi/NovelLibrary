package io.github.gmathi.novellibrary.util.changelog

/**
 * Plain-text rendering of the latest release, used by the one-time "What's new"
 * update dialog (see NavDrawerActivity). Derived from [Changelog] so it can
 * never drift from the full "What's Changed" screen.
 */
object WhatsChanged {

    val LATEST: String = buildString {
        val v = Changelog.latest
        append("🎉 v").append(v.version).append(" Release!\n")
        v.headline?.let { append('\n').append(it).append('\n') }
        append('\n')
        v.changes.forEach { c ->
            append(c.emoji).append(' ').append(c.title)
            c.detail?.let { append(" — ").append(it) }
            append('\n')
        }
    }
}
