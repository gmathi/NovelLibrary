package io.github.gmathi.novellibrary.model.other

@Suppress("KDocUnresolvedReference")
enum class SubqueryProcessingCommand {
    /**
     * Unwraps the element contents top elements.
     * Example use-case: Wattpad wrapping the contents into <pre> element, causing fonts to be monospace.
     * @param commandArgument If not empty, will apply Wrap command with the tag name specified.
     */
    Unwrap,

    /**
     * Wraps tbe element with a new tag.
     * @param commandArgument The name of the new tag.
     */
    Wrap,

    /**
     * Changes the element tag name to the one specified.
     * @param commandArgument The new tag name of the element.
     */
    ChangeTag,

    /**
     * Adds specified classes to the element.
     * @param commandArgument The comma-separated class list without dot.
     */
    AddClass,

    /**
     * Sets an ID for the element.
     * @param commandArgument The new element ID.
     */
    AddId,

    /**
     * Adds an extra attribute to the element.
     * @param commandArgument The attribute name and value separated by "=" equals sign.
     */
    AddAttribute,

    /**
     * Removes attributes from the element.
     * @param commandArgument If not empty, removes only specified attribute names, otherwise removes all.
     */
    RemoveAttributes,

    /**
     * Remove all classes from the element.
     * @param commandArgument If not empty, removes only specified classes, otherwise removes all.
     */
    RemoveClasses,

    /**
     * Remove the id attribute from the field.
     * @param commandArgument If not empty, removes IDs only matching to the text, otherwise removes in any case.
     */
    RemoveId,

    /**
     * Filters the subquery search results and keeps only ones that do not contain specified string in the element text.
     * Note that filter commands do not affect the pre-selection process of required subqueries.
     * @param commandArgument The string that should be present in the text to discard element.
     */
    FilterNotString,

    /**
     * Filters the subquery search results and keeps only ones that do not match the specified regular expression in the element text.
     * Note that filter commands do not affect the pre-selection process of required subqueries.
     * @param commandArgument The regular expression that should match with the text to discard element.
     */
    FilterNotRegex,

    /**
     * Filters the subquery search results and keeps only ones that contain specified string in the element text.
     * Note that filter commands do not affect the pre-selection process of required subqueries.
     * @param commandArgument The string that should be present in the text in order to keep the element.
     */
    FilterOnlyString,

    /**
     * Filters the subquery search results and keeps only ones that match the specified regular expression in the element text.
     * Note that filter commands do not affect the pre-selection process of required subqueries.
     * @param commandArgument The regular expression that should match with text in order to keep the element.
     */
    FilterOnlyRegex,

    /**
     * Marks the elements as ones that should be avoid from being read by TTS.
     * @param commandArgument If not empty, the element contents will be substituted by the argument text.
     */
    DisableTTS,

    /**
     * Marks the elements as one that contains the URL that leads outside of the buffer page if condition passes.
     * Only applies to tags that contain `href` attribute.
     * @param commandArgument
     * TODO: Figure out how to do a condition with a string.
     */
    MarkBufferLink

}