# Settings Restructuring — Migration Plan

**Status:** PLAN ONLY — not scheduled, no code changed.
**Goal:** Reorganize the current flat/overlapping settings into 7 clearly-named top-level screens with internal section headers, and eliminate duplicated TTS/Reader rows.
**Scope anchor:** `app/src/main/java/io/github/gmathi/novellibrary/activity/settings/**` + `app/src/main/res/values/strings.xml` + `app/src/main/java/io/github/gmathi/novellibrary/util/system/StartIntentExt.kt` + `app/src/main/AndroidManifest.xml`.

---

## 0. Current architecture (what we're migrating from)

Two coexisting UI patterns drive settings today:

| Pattern | Screens using it | How items are defined |
|---|---|---|
| **Legacy A** — `BaseActivity` + `GenericAdapter<String>` | `MainSettingsActivity`, `GeneralSettingsActivity`, `MentionSettingsActivity`, `SyncSettingsSelectionActivity`, `BackupSettingsActivity` (orphaned), `StorageSettingsActivity`, `ReaderBackgroundSettingsActivity` | `R.array.*` string-arrays, click dispatched by `when(item){ getString(R.string.x) -> ... }` on label text |
| **Legacy B** — `BaseSettingsActivity<V,T>` + `ListitemSetting` | `ReaderSettingsActivity`, `ScrollBehaviourSettingsActivity`, `TTSSettingsActivity` | inline `listOf(Setting(...).onBind/.bindChevron/.bindSwitch/.bindHeader)` |
| **Compose** | `AiTtsSettingsActivity` → `AiTtsSettingsScreen` | Composable with `SectionHeader(...)` + `SwitchSettingRow`/`ChevronSettingRow` |

**Key observations that shape the plan:**

- **Legacy B already supports section headers** via `TTSSetting(R.string.header, R.string.empty).bindHeader()`. The TTS screen groups into Playback / Processing / Filters / Remote / Misc. This is the target pattern for the Reader screen (which is currently a flat 17-item list in the same framework — headers are a pure addition, no framework work).
- **Click dispatch in Legacy A is by label string**, so moving an item between screens means moving (a) its `<item>` in the string-array AND (b) its `when` branch in the owning activity. Fragile but mechanical.
- **`startBackupSettingsActivity()` already points at `BackupRestoreActivity`** (the Compose rewrite), NOT the old `BackupSettingsActivity.kt` — the latter is effectively orphaned. Verify and treat `BackupRestoreActivity` as the live backup screen.
- Navigation is centralized in `StartIntentExt.kt` `//#region Settings`. New screens get new `start*Activity()` extensions there.
- Every new Activity must be registered in `AndroidManifest.xml` (settings activities are declared ~line 134+).

---

## 1. Target top-level structure

`MainSettingsActivity` / `R.array.settings_list` becomes:

1. **Appearance & Theme** *(new screen)*
2. **Reader** *(existing, add headers; lose color palette)*
3. **Text-to-Speech** *(new parent wrapping Classic TTS + AI TTS)*
4. **Network & Privacy** *(new screen)*
5. **Storage & Backup** *(new parent / merge)*
6. **Sync** *(unchanged)*
7. **About & Help** *(rename "Mentions", absorb Donate/Updates/About actions)*

---

## 2. Item move map (source → destination)

| Item | String id | From | To |
|---|---|---|---|
| Load Library screen | `first_screen_library` | General | **Appearance** |
| Night Mode | `night_mode` | General | **Appearance** |
| Enable Scrolling Text | `scrolling_text` | General | **Appearance** |
| Show Chapters-Left Badge | `show_chapters_left_badge` | General | **Appearance** |
| Reader Mode Colors (day/night) | `reader_mode_colors` → `ReaderBackgroundSettingsActivity` | Reader | **Appearance** |
| DNS over HTTPS | `dns_over_https` | General | **Network & Privacy** |
| Forced WebView Domains | `forced_webview_domains` | General | **Network & Privacy** |
| Clear Cloudflare Cookies | `clear_cloudflare_cookies` | General | **Network & Privacy** |
| Download Storage Location | `download_storage_location` | General | **Storage & Backup** |
| Backup & Restore | `backup_and_restore` | General | **Storage & Backup** |
| Auto App Update | `auto_app_update` | General | **About & Help** |
| Check for Updates | `check_for_updates` | Main (action) | **About & Help** |
| Donate Developer | `donate_developer` | Main (dialog) | **About & Help** |
| About Us | `about_us` | Main (dialog) | **About & Help** |
| Mentions screen contents | `mention_settings_list` | Main | **About & Help** (rename) |
| Read Aloud (Classic TTS) | `title_read_aloud` | Main | **Text-to-Speech → Classic** |
| AI TTS | `title_activity_ai_tts_settings` | Main | **Text-to-Speech → AI** |
| Enable Notifications | `disable_notifications` | General | **General** (stays — or demote; see §7 open Q) |

After the moves, **General is empty** → remove it as a top-level entry (fold "Enable Notifications" into Appearance or a small "App" screen; decided in §7).

**Reader internal headers (no items leave except colors):**
- *Content*: Reader Mode, Disable JavaScript, Show Comments, Merge Pages, Directional Links, Limit Image Width, Linkify Text, Custom Query Lookups
- *Behavior*: Swipe for Next Chapter, Keep Screen On, Immersive Mode, Show Navbar at Chapter End, Reader-Mode Button Visibility, Scroll Behaviour (sub-screen chevron)
- *Text*: Keep Text Color, Alternative Text Colors

---

## 3. TTS consolidation (the one non-mechanical change)

Problem: `Speech Rate`, `Pitch`, `Auto-read Next Chapter`, `Keep Screen On` exist in **both** `TTSSettingsActivity` (classic) and `AiTtsSettingsScreen` (AI), backed by **different preference stores** — `dataCenter.ttsPreferences.*` vs `dataCenter.aiTtsPreferences.*` / `dataCenter.useAiTts`.

Decision for the plan: **do NOT merge the preference backends.** Keep two engine sub-screens; add a parent `TextToSpeechSettingsActivity` that:
- shows an **Engine** selector (Classic / AI) bound to `dataCenter.useAiTts`,
- lists two chevron rows → Classic TTS screen (existing `TTSSettingsActivity`) and AI TTS screen (existing `AiTtsSettingsActivity`).

Rationale: merging the stores is a data-migration risk out of proportion to a reorg; a parent screen removes the top-level duplication (two TTS entries) without touching persisted preferences. A future "shared rate/pitch" unification is a separate, larger task — explicitly out of scope here.

---

## 4. Phased execution

Each phase is independently buildable and testable (`.\gradlew.bat assembleNormalDebug`), so the work can land as separate PRs/commits (per the repo's per-feature-commit convention).

### Phase 1 — Scaffolding (no behavior change)
- Add new string ids + arrays in `strings.xml`: `title_appearance`, `title_network_privacy`, `title_storage_backup`, `title_about_help`, `title_text_to_speech`; new `appearance_titles_list`/`_subtitles_list`, `network_privacy_titles_list`/`_subtitles_list`, `storage_backup_titles_list`/`_subtitles_list`.
- Create empty Activities on the **Legacy B** (`BaseSettingsActivity<V,T>`) pattern — it's the cleanest and supports headers:
  `AppearanceSettingsActivity`, `NetworkPrivacySettingsActivity`, `StorageBackupSettingsActivity`, `TextToSpeechSettingsActivity`.
- Register all four in `AndroidManifest.xml`.
- Add `start*Activity()` extensions in `StartIntentExt.kt` `//#region Settings`.
- **Test:** build only; screens not yet linked.

### Phase 2 — Appearance
- Move the 4 General toggles + Reader-colors chevron into `AppearanceSettingsActivity` as `ListitemSetting` rows (copy the exact `dataCenter.*` bindings from `GeneralSettingsActivity.bind()` / `ReaderSettingsActivity`).
- Remove those rows from `general_titles_list`/`general_subtitles_list` and their `when` branches / `POSITION_*` constants in `GeneralSettingsActivity`.
- Remove the `reader_mode_colors` chevron from `ReaderSettingsActivity`'s `OPTIONS` list.
- **Test:** toggle each setting, kill/reopen app, confirm persistence; confirm Reader no longer shows Colors and Appearance does.

### Phase 3 — Network & Privacy
- Move DNS dialog (`showDnsSelection`), Forced WebView chevron, Clear Cloudflare action (`confirmClearCloudflareCookies`) out of `GeneralSettingsActivity` into `NetworkPrivacySettingsActivity`.
- **Test:** DNS selection persists (`dataCenter.dohProvider`), Forced WebView screen opens, Clear Cloudflare clears cookies + shows toast.

### Phase 4 — Storage & Backup
- `StorageBackupSettingsActivity` lists two chevrons → `StorageSettingsActivity` and `BackupRestoreActivity`.
- Remove `download_storage_location` + `backup_and_restore` from General.
- **Test:** both sub-screens open; verify `startBackupSettingsActivity()` target is `BackupRestoreActivity` (fix if the plan's assumption is wrong).

### Phase 5 — Text-to-Speech parent
- `TextToSpeechSettingsActivity`: Engine selector (`dataCenter.useAiTts`) + two chevrons to existing Classic/AI screens.
- Replace the two separate `settings_list` entries with one.
- **Test:** engine toggle flips `useAiTts`; both sub-screens reachable; AI screen's own `useAiTts` row stays consistent (shared source of truth).

### Phase 6 — About & Help
- Rename `MentionSettingsActivity` → `AboutHelpActivity` (or keep class, retitle); add Donate/About dialogs + Check-for-Updates + Auto-App-Update toggle + Report-a-Bug (reuse `MainSettingsActivity`'s `menu_settings` bug report + `systemInfo()`).
- Move those out of `MainSettingsActivity.onItemClick` / `settings_list`.
- **Test:** each action/dialog fires; Auto App Update toggle persists.

### Phase 7 — Top-level cleanup
- Rewrite `settings_list` to the 7 entries; delete the now-empty General screen (and dead `BackupSettingsActivity.kt` if confirmed orphaned).
- Update `MainSettingsActivity.bind()` chevron rule (currently `position < 5`) and `onItemClick` dispatch.
- **Test:** full click-through of all 7 screens; the android-cli screen-test loop on `MainSettingsActivity` and each child (see `.kiro/steering/android-cli.md`).

---

## 5. Files touched (checklist)

- `res/values/strings.xml` — new string ids, new arrays, pruned General arrays.
- `activity/settings/` — new: `AppearanceSettingsActivity`, `NetworkPrivacySettingsActivity`, `StorageBackupSettingsActivity`, `TextToSpeechSettingsActivity`; edited: `MainSettingsActivity`, `GeneralSettingsActivity` (shrink then delete), `MentionSettingsActivity`→About, `ReaderSettingsActivity` (headers + drop colors).
- `util/system/StartIntentExt.kt` — new nav extensions.
- `AndroidManifest.xml` — register 4 new activities; drop General/orphans.
- (No `DataCenter` / preference-schema changes — intentional.)

---

## 6. Risks & test strategy

- **Label-string click dispatch (Legacy A):** moving an item without moving its `when` branch silently breaks the tap. Mitigate by migrating moved items to the typed `ListitemSetting` pattern (position/callback-based, not label-based) as they move.
- **Existing tests:** `StorageSettingsActivityItemClickPropertyTest`, `StorageSettingsConfirmationDialogInstrumentedTest`, `DataCenterReaderDisplaySettingsTest`, `ReaderViewModelDisplaySettingsTest` — re-run after Phases 2/4; update any that assert on screen membership.
- **Preference persistence:** no schema change, so a user upgrading keeps all values; verify each moved toggle reads/writes the same `dataCenter` key it did before.
- **Backup orphan:** confirm which backup activity is live before Phase 4.
- Build per phase with `.\gradlew.bat assembleNormalDebug`; targeted unit tests over full suite (host memory is tight).

---

## 7. Open decisions (need owner input before implementing)

1. **Enable Notifications** — General is being deleted. Put it in Appearance, or create a tiny "App" screen, or fold into About & Help?
2. **General screen** — delete entirely, or keep as a thin "App basics" (Notifications + Load-Library-on-start)?
3. **TTS stores** — confirm we are NOT merging `ttsPreferences`/`aiTtsPreferences` in this effort (plan assumes not).
4. **"Mentions" class** — rename the class file (`MentionSettingsActivity` → `AboutHelpActivity`) or keep the class name and only change title/contents?
