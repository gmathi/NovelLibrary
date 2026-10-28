package io.github.gmathi.novellibrary.util.changelog

/**
 * Single source of truth for the app changelog.
 *
 * Both the "What's Changed" screen (full history) and the one-time update
 * dialog ([WhatsChanged.LATEST]) are derived from this list, so the two can
 * never drift apart again. Add a new release by prepending a [ChangelogVersion]
 * to [Changelog.versions] — the newest entry is automatically the "latest".
 *
 * This replaces the old `assets/changelog.json` + hand-maintained constants,
 * which had diverged (the JSON held only 2.2.1 while the constants held
 * 2.2.0/2.1.0, each describing 2.2.1 differently).
 */

/** One line of a release, e.g. 🛡️ "Cloudflare fix" + its description. */
data class ChangelogChange(
    val emoji: String,
    val title: String,
    val detail: String? = null,
    /** Highlight a headline/major change with stronger emphasis in the UI. */
    val major: Boolean = false,
)

data class ChangelogVersion(
    val version: String,
    /** Short release date label, e.g. "Oct 2026". Nullable for unreleased/dev. */
    val date: String? = null,
    /** One-line summary shown under the version header. */
    val headline: String? = null,
    val changes: List<ChangelogChange>,
)

object Changelog {

    /**
     * Newest first. The first entry is treated as the latest release.
     */
    val versions: List<ChangelogVersion> = listOf(

        ChangelogVersion(
            version = "2.4.1",
            date = "Oct 2026",
            headline = "Storage-migration and backup data-safety fixes, plus a font-picker crash fix.",
            changes = listOf(
                ChangelogChange(
                    emoji = "🗂️",
                    title = "Storage migration only moves your novels",
                    detail = "Switching the download location no longer relocates and deletes " +
                        "non-novel folders (voice models, app data) that sit beside your novel " +
                        "downloads. Folders that aren't novels are now left in place, and the " +
                        "result tells you how many were skipped.",
                    major = true,
                ),
                ChangelogChange(
                    emoji = "💾",
                    title = "Backups include downloaded chapters from the SD card",
                    detail = "A 'backup with chapters' now reads from your active download " +
                        "location, so moving downloads to the SD card no longer silently leaves " +
                        "the chapters out of the backup.",
                    major = true,
                ),
                ChangelogChange(
                    emoji = "🔤",
                    title = "Font picker no longer crashes on a missing font",
                    detail = "A per-novel font whose file was removed or lives on unavailable " +
                        "storage now falls back to the default instead of crashing the font " +
                        "dialog.",
                ),
            ),
        ),

        ChangelogVersion(
            version = "2.4.0",
            date = "Oct 2026",
            headline = "Offline reading that stays put, a tap-to-open reader, and reliable RoyalRoad loading.",
            changes = listOf(
                ChangelogChange(
                    emoji = "📶",
                    title = "Downloaded chapters stay visible offline",
                    detail = "Opening a novel with no connection no longer hides your list. " +
                        "Downloaded chapters are served straight from the device, and the app " +
                        "falls back to them if a refresh can't reach the network — so your " +
                        "offline library is always there.",
                    major = true,
                ),
                ChangelogChange(
                    emoji = "👆",
                    title = "Tap the screen to open the reader menu",
                    detail = "Tap anywhere in the reading area to toggle the reader menu — no more " +
                        "hunting for the floating button (now hidden). Taps on links, images and " +
                        "text selection keep working as before, and the system bars reveal " +
                        "alongside the reader bars.",
                    major = true,
                ),
                ChangelogChange(
                    emoji = "🛡️",
                    title = "RoyalRoad loads reliably again",
                    detail = "RoyalRoad's new JavaScript anti-bot check broke chapter loading. " +
                        "RoyalRoad now always routes through the in-app browser fetcher, which " +
                        "runs the verification like a real browser, so chapters load again.",
                ),
                ChangelogChange(
                    emoji = "📱",
                    title = "App icon resumes where you left off",
                    detail = "Tapping the app icon now returns to whatever you had open (such as the " +
                        "reader) instead of dropping you back on the main screen.",
                ),
                ChangelogChange(
                    emoji = "🔊",
                    title = "Cleaner TTS notification behaviour",
                    detail = "Tapping the text-to-speech notification no longer stacks duplicate " +
                        "reader and controls screens — it reuses the one already open.",
                ),
                ChangelogChange(
                    emoji = "🗓️",
                    title = "Accurate \"Last Updated\" after a refresh",
                    detail = "Refreshing a single novel now updates its Last Updated date, matching " +
                        "the behaviour of a full category sync.",
                ),
                ChangelogChange(
                    emoji = "🐛",
                    title = "Fewer false error reports",
                    detail = "Cancelling a fetch (for example during Cloudflare verification or a " +
                        "screen rotation) no longer shows a spurious error prompting you to report " +
                        "a non-bug.",
                ),
                ChangelogChange(
                    emoji = "📜",
                    title = "Chapter-list scrollbar restored",
                    detail = "The vertical scrollbar is back in the chapter list.",
                ),
            ),
        ),

        ChangelogVersion(
            version = "2.3.0",
            date = "Oct 2026",
            headline = "A rebuilt Settings experience and smarter, hands-off Cloudflare handling.",
            changes = listOf(
                ChangelogChange(
                    emoji = "⚙️",
                    title = "Redesigned Settings",
                    detail = "A brand-new Material settings experience, reorganised into clear " +
                        "categories (Appearance, Reader, Network & Privacy, Storage & Backup, " +
                        "About & Help). Prefer the old layout? Switch back any time from " +
                        "Settings → Switch to classic.",
                    major = true,
                ),
                ChangelogChange(
                    emoji = "🛡️",
                    title = "Automatic Cloudflare handling",
                    detail = "The manual \"Use API for Chapters\" toggle is gone. The app now tries " +
                        "the fast API path first and only falls back to in-app browser fetching " +
                        "per domain when that path keeps failing — no setup required.",
                    major = true,
                ),
                ChangelogChange(
                    emoji = "🌐",
                    title = "Forced WebView Domains screen",
                    detail = "A new screen under Settings → Network & Privacy lists any domains that " +
                        "fell back to WebView fetching. Each one self-heals back to the fast API " +
                        "path automatically after 15 days, or you can clear it manually.",
                ),
                ChangelogChange(
                    emoji = "✅",
                    title = "Guided Cloudflare verification",
                    detail = "When a challenge can't be solved automatically you're routed to manual " +
                        "verification; once you solve it, the fetch resumes on its own.",
                ),
                ChangelogChange(
                    emoji = "📡",
                    title = "Live loading status",
                    detail = "Search, novel details, chapter lists and the reader now show what's " +
                        "actually happening — checking cache, verifying with Cloudflare, falling " +
                        "back to the in-app browser, and so on.",
                ),
                ChangelogChange(
                    emoji = "📖",
                    title = "Reader overlay rework",
                    detail = "The floating menu button is centred, the JavaScript toggle moved to the " +
                        "bottom bar (styled like Reader Mode), and the Font control moved into the " +
                        "settings menu.",
                ),
                ChangelogChange(
                    emoji = "🔑",
                    title = "NovelUpdates login awareness",
                    detail = "Chapter loading now checks for your login cookies and prompts you to log " +
                        "in when they're missing.",
                ),
                ChangelogChange(
                    emoji = "🐛",
                    title = "Stability & bug fixes",
                    detail = "Bug-report flow fixes and general stability improvements.",
                ),
            ),
        ),

        ChangelogVersion(
            version = "2.2.1",
            date = "Oct 2026",
            headline = "Cloudflare and reader-mode fixes.",
            changes = listOf(
                ChangelogChange("🛡️", "Cloudflare fix", "Check General Settings for WebFetcher. A better fix is on the way."),
                ChangelogChange("📖", "Reader mode fixes", "A batch of reader rework and fixes."),
                ChangelogChange("🐛", "Stability fixes", "General bug fixes and stability improvements."),
            ),
        ),

        ChangelogVersion(
            version = "2.2.0",
            date = "2026",
            changes = listOf(
                ChangelogChange(
                    emoji = "✨",
                    title = "Per-novel Reader Mode",
                    detail = "Reader mode, font, size and theme are now remembered per novel, so each " +
                        "novel keeps its own reading setup. New novels start from your default " +
                        "reader settings.",
                    major = true,
                ),
                ChangelogChange(
                    emoji = "✨",
                    title = "Move downloads to other storage",
                    detail = "Move your downloads to another mounted storage location (such as an SD " +
                        "card). Pick which storage to use from settings and your existing downloads " +
                        "move over.",
                    major = true,
                ),
                ChangelogChange("🛡️", "Cloudflare fix", "Improved Cloudflare handling so protected sites load more reliably."),
                ChangelogChange("📐", "Layout fixes", "Fixed screen layouts, including the Android 3-button navigation overlay."),
                ChangelogChange("📖", "Reader mode fixes", "Reader mode now works on more websites, with font fixes."),
                ChangelogChange("🐛", "Stability fixes", "General bug fixes and stability improvements."),
            ),
        ),

        ChangelogVersion(
            version = "2.1.0",
            date = "2026",
            changes = listOf(
                ChangelogChange("📥", "Redesigned Downloads screen", "New Compose UI with novel- and chapter-level views, sorting and real-time progress."),
                ChangelogChange("🛡️", "Better downloads reliability", "Improved Cloudflare handling and rate limiting for more stable downloads."),
                ChangelogChange("🌗", "Day/Night theme", "New light and dark theme support with a night-mode toggle."),
                ChangelogChange("💾", "New Backup & Restore screen", "Rebuilt in Compose with live progress."),
                ChangelogChange("📚", "Refreshed Novel Details", "Rebuilt on a cleaner, more reliable architecture."),
                ChangelogChange("🐛", "Stability fixes", "Resolved several crashes and improved overall app stability."),
            ),
        ),
    )

    /** The newest release. */
    val latest: ChangelogVersion get() = versions.first()
}
