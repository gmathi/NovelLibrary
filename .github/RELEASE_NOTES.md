## 🎉 v2.4.1

A redesigned tag-based library, more ways to act on chapters, and storage/backup data-safety fixes.

- 🏷️ **New tag-based library experience** — An optional redesigned library built on tags instead of fixed sections. Filter by one or many tags (Any/All), sort by 12 options, and manage tags — create, rename, recolour and delete — from a dedicated Library Tags screen. Your existing sections are migrated to coloured tags automatically, and the classic library stays available: toggle it from Settings → Appearance → General.
- 🔲 **Adjustable library grid** — Choose how many cover columns the library grid shows (1–4, default 2) from Settings → Appearance. The sort popup is now a pair of compact dropdowns, and covers refresh with a non-blocking progress strip so you can keep browsing while a sync runs.
- 📤 **Share a link to add it to your library** — Share a supported novel URL to NovelLibrary and it opens that novel's details ready to add.
- 🖼️ **Refresh library covers** — A new action re-fetches every cover image, clearing the cache so covers update even when the image changed behind an unchanged URL.
- ⬇️ **Auto-download new chapters on update** — Optionally download newly found chapters automatically whenever the library updates.
- 👉 **Swipe chapters to act** — Swipe a chapter right to toggle read/unread, or left to toggle the bookmark — each with an Undo snackbar.
- 🔊 **Volume keys switch chapters at the edges** — With volume-key scrolling on, volume-up at the top of a chapter jumps to the previous chapter and volume-down at the bottom jumps to the next; elsewhere they scroll as before.
- 🛡️ **Manual Cloudflare verification where it was missing** — Novel-details and reader-mode fetches that hit a Cloudflare challenge now open the manual verification on the correct gated URL instead of failing silently or opening the wrong site.
- ⚡ **Faster library sync** — Library updates now fetch novels in parallel with per-domain throttling, so a full sync finishes noticeably faster.
- 🗂️ **Storage migration only moves your novels** — Switching the download location no longer relocates and deletes non-novel folders (voice models, app data) that sit beside your novel downloads. Folders that aren't novels are now left in place, and the result tells you how many were skipped.
- 💾 **Backups include downloaded chapters from the SD card** — A "backup with chapters" now reads from your active download location, so moving downloads to the SD card no longer silently leaves the chapters out of the backup.
- 🔤 **Font picker no longer crashes on a missing font** — A per-novel font whose file was removed or lives on unavailable storage now falls back to the default instead of crashing the font dialog.

## 🎉 v2.4.0

Offline reading that stays put, a tap-to-open reader, and reliable RoyalRoad loading.

- 📶 **Downloaded chapters stay visible offline** — Opening a novel with no connection no longer hides your list. Downloaded chapters are served straight from the device, and the app falls back to them if a refresh can't reach the network — so your offline library is always there.
- 👆 **Tap the screen to open the reader menu** — Tap anywhere in the reading area to toggle the reader menu — no more hunting for the floating button (now hidden). Taps on links, images and text selection keep working as before, and the system bars reveal alongside the reader bars.
- 🛡️ **RoyalRoad loads reliably again** — RoyalRoad's new JavaScript anti-bot check broke chapter loading. RoyalRoad now always routes through the in-app browser fetcher, which runs the verification like a real browser, so chapters load again.
- 📱 **App icon resumes where you left off** — Tapping the app icon now returns to whatever you had open (such as the reader) instead of dropping you back on the main screen.
- 🔊 **Cleaner TTS notification behaviour** — Tapping the text-to-speech notification no longer stacks duplicate reader and controls screens — it reuses the one already open.
- 🗓️ **Accurate "Last Updated" after a refresh** — Refreshing a single novel now updates its Last Updated date, matching the behaviour of a full category sync.
- 🐛 **Fewer false error reports** — Cancelling a fetch (for example during Cloudflare verification or a screen rotation) no longer shows a spurious error prompting you to report a non-bug.
- 📜 **Chapter-list scrollbar restored** — The vertical scrollbar is back in the chapter list.
