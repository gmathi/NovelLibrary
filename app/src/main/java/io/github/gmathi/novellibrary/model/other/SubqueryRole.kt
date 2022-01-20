package io.github.gmathi.novellibrary.model.other

enum class SubqueryRole {
    /**
     * Chapter contents. If query is an empty string or yields no results, uses the `SelectorQuery.query` results.
     * If Subqueries are used have to be present.
     */
    RContent,

    /**
     * Chapter header.
     * If present and found, the `SelectorQuery.appendTitleHeader` is ignored.
     */
    RHeader,

    /**
     * Chapter footer.
     * May contain stuff like pagination for the chapter that cannot be detected explicitly.
     */
    RFooter,

    /**
     * Chapter social media embeds.
     * Currently stripped out. (Prevents the "Like this", "Like loading" and other broken social media elements)
     */
    RShare,

    /**
     * The chapter comments area.
     * Stripped out if "showChapterComments" is disabled.
     */
    RComments,

    /**
     * Chapter metadata.
     * Includes things like post date, post author, like counter, etc.
     * Currently stripped out.
     */
    RMeta,

    /**
     * Navigation between chapters.
     * Stripped out if `enableDirectionalLinks` is disabled.
     */
    RNavigation,

    /**
     * In-chapter page navigation. Could also be used  for buffer pages when applicable.
     */
    RPage,

    /**
     * URL to the real chapter. Because translators oh so love being annoying to their readers.
     */
    RRealChapter,

    /**
     * URL to another (previous/next/specific) chapter. Chapters marked as such would be ignored by chapter merge option.
     */
    RChapterLink,

    /**
     * Matches are explicitly removed from the cleaned chapter.
     */
    RBlacklist,

    /**
     * Matches are explicitly inserted in the cleaned chapter.
     * Do note that no cleanup is performed and IDs, classes and style are left intact.
     */
    RWhitelist,

    /**
     * Used only to execute processing commands and won't be removed nor added to the constructed contents.
     */
    RProcess,
}