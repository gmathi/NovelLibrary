# Hide Images in Reader Mode — Design

**Date:** 2026-07-25
**Status:** Approved, pending implementation

## Problem

Reader mode carries images through from the source page into the cleaned chapter view. On some sites those images are ad-slot placeholders that add nothing to the chapter.

Investigated case: `https://freewebnovel.com/novel/shadow-slave/chapter-1`. Every `<img>` on that page is site chrome or an ad placeholder (`/static/freewebnovel/images/slot-state/wait-0N.webp`, 300×250). Five of them sit *inside* the `#article` content container, so the reader-mode content extraction keeps them. The page contains no story images at all.

Images are relevant to some novels, so removal must be opt-in and user-controlled rather than automatic.

## Solution

A user preference that strips images from the document before the reader renders it. Default off — current behaviour is unchanged for anyone who does not enable it.

The setting affects **reader mode only**. With reader mode off the raw page is loaded untouched, matching how every other cleaner-driven setting behaves.

### Accepted tradeoff

The toggle is deliberately blunt: when on, it removes *all* images, including genuine story art on other novels. This was chosen over a three-way (show / hide / tap-to-load placeholder) option and over an always-on ad-image heuristic, for predictability and a minimal change surface. A narrower future fix for the freewebnovel case specifically would be an `RBlacklist` subquery targeting the ad slots, requiring no setting at all.

## Design

### 1. Preference

`DataCenter` (`model/preference/DataCenter.kt`):

- `private const val HIDE_IMAGES = "hideImages"`
- `var hideImages: Boolean`, backed by `prefs`, default `false`

Follows the existing `limitImageWidth` pattern exactly.

### 2. Stripping

New `open fun removeImages(doc: Document)` on `HtmlCleaner` (`cleaner/HtmlCleaner.kt`).

When `dataCenter.hideImages` is true it removes from the parsed document:

- `img`
- `picture`
- `svg`
- any `figure` left with no text content after the above (so a caption whose image is gone does not linger as an orphan)

When the preference is false the method is a no-op.

Because this mutates the Jsoup `Document` before `loadDataWithBaseURL`, the WebView never issues the image requests. This saves bandwidth, which CSS `display: none` would not.

### 3. Call sites

`WebPageDBFragment` (`fragment/WebPageDBFragment.kt`) — call `removeImages(doc)` immediately after each `additionalProcessing(doc)`:

| Approx. line | Path |
|---|---|
| 362 | main document, `loadFromWeb` path |
| 375 | merged linked page, `loadFromWeb` path |
| 430 | main document, downloaded-file path |
| 449 | merged linked page, downloaded-file path |

Called explicitly rather than folded into `additionalProcessing`, because subclass cleaners override that method and do not all call `super`.

**Downloaded chapters** are handled for free: the pipeline runs against the document parsed from the saved file, so the toggle applies live to already-downloaded chapters and the downloaded files themselves keep their images. This is a display setting, not a destructive one.

### 4. Live toggling

`limitImageWidth` posts `ReaderSettingsEvent.NIGHT_MODE`, which routes to `applyTheme()` — that re-runs `toggleTheme` against the *existing in-memory* `doc` and reloads it. That is sufficient for a CSS-only setting.

It is **not** sufficient here. `removeImages` mutates the document; once images are stripped, re-running `toggleTheme` cannot restore them, so turning the setting back off would appear to do nothing until the chapter was reopened.

Therefore:

- Add `const val HIDE_IMAGES = "hideImages"` to `ReaderSettingsEvent` (`model/other/Events.kt`).
- Handle it in `WebPageDBFragment.onReaderSettingsChanged` with `loadData()`, forcing a full reparse from source or file.

### 5. UI

Exposed on both surfaces, mirroring how `limitImageWidth` is exposed.

**Reader Settings screen** — `activity/settings/reader/ReaderSettingsActivity.kt`, a `bindSwitch` entry placed after "Fit images". New strings in `res/values/strings.xml`:

- `hide_images` — the title
- `hide_images_description` — explains that images are removed from reader mode and that some novels use images as story content

**In-reader quick settings** — `compose/reader/ReaderSettingsPanel.kt`, a `SettingToggle` using `Icons.Outlined.HideImage`, next to "Limit Image Width".

**ViewModel** — `viewmodel/ReaderViewModel.kt`:

- `hideImages: Boolean = false` in `ReaderUiState`
- initialised from `dataCenter.hideImages`
- `setHideImages(enabled: Boolean)` writes the pref, updates `_uiState`, and posts `ReaderSettingsEvent(HIDE_IMAGES)`

## Files touched

| File | Change |
|---|---|
| `model/preference/DataCenter.kt` | new `hideImages` pref |
| `cleaner/HtmlCleaner.kt` | new `removeImages(doc)` |
| `fragment/WebPageDBFragment.kt` | 4 pipeline calls, 1 new event branch |
| `model/other/Events.kt` | new `HIDE_IMAGES` event constant |
| `activity/settings/reader/ReaderSettingsActivity.kt` | new switch |
| `compose/reader/ReaderSettingsPanel.kt` | new toggle |
| `viewmodel/ReaderViewModel.kt` | ui state field + setter |
| `res/values/strings.xml` | 2 new strings |

## Verification

Manual, in the reader:

1. Open the Shadow Slave chapter 1 above with reader mode on and the setting off — the five ad placeholders are present.
2. Enable "Hide images" — chapter reloads, placeholders gone, no blank gaps left behind.
3. Disable it — chapter reloads, images return. This is the case that fails if the event routes to `applyTheme()` instead of `loadData()`.
4. Repeat 2–3 on a downloaded chapter, confirming the toggle still applies and the download is not modified.
5. Open a novel that uses images as story content, confirm they are stripped only when the setting is on.
6. Turn reader mode off, confirm the setting has no effect on the raw page.

Unit test coverage in this repo is minimal and `removeImages` is straightforward Jsoup selection; a focused JUnit test over a small HTML fixture (ad-slot markup in, empty content out; `figure` caption handling) is worth adding under `app/src/test/` since it needs no Android runtime.
