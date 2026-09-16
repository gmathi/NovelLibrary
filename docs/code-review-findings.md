# Code Review Findings

## Overview

Static review of the NovelLibrary codebase at commit `9f70362` ("Ready for 2.1.0"), focused on
bugs/correctness and code quality/structure. The review was read-only analysis; no build or
device testing was performed because the review environment has no Android SDK and no
`google-services.json`. All fixes listed under "Applied fixes" were checked against the standalone
Kotlin compiler for syntax only, and should be built and smoke-tested before merging.

Paths below are relative to `app/src/main/java/io/github/gmathi/novellibrary/`.

## Applied fixes

| Area | File | Problem | Fix |
|------|------|---------|-----|
| Cleaner | `cleaner/HtmlCleaner.kt` (`getInstance`) | Host matching used exact `equals` on the full host, so `www.wattpad.com` never matched `wattpad.com`; the download path passed a bare host string, which `toHttpUrlOrNull()` rejects, so no site-specific cleaner ever ran during downloads. | Compute the host once, accept bare host strings, and match by suffix. `DownloadWebPageThread` now passes `doc.location()`. |
| Cleaner | `cleaner/HtmlCleaner.kt` (`fixStyleWhileRetainingColors`) | `getNodeColor(el) ?: el.removeAttr("style")` returned the `Element` on null, so the element's outer HTML was written into the `style` attribute. `matchEntire` only matched when `color:` was the whole style string. | Explicit null branch; use `find` instead of `matchEntire`. |
| Cleaner | `cleaner/HtmlCleaner.kt` | `hasClass(".code-block")` never matches (class names have no dot). | Removed the dot. |
| Cleaner | `cleaner/HtmlCleaner.kt` (`downloadImage`) | `FileOutputStream` never closed. | Wrapped in `use`. |
| Cleaner | `cleaner/HtmlCleaner.kt` (injected JS) | `var dt = e.timeStamp = img._pressTime` assigned instead of subtracting, so every tap counted as a long-press. | `-` instead of `=`. |
| Cleaner | `cleaner/GoogleDocsCleaner.kt` | `tag != "a" \|\| tag != "img"` is always true, stripping attributes from links and images. | `&&`. |
| Source | `model/source/online/NovelUpdatesSource.kt` | `document.select(...) != null` is always true, so `hasNextPage` was always true and pagination never stopped. | `selectFirst(...) != null`. |
| Source | `model/source/online/NovelUpdatesSource.kt` | `parallelStream().forEach` wrote into plain `HashMap`s from multiple threads. | Sequential iteration (work is trivial); removed the unused `getMaxPageNum` duplicate. |
| Network | `network/postProxy/JsonContentProxy.kt` | `extractJson(doc)` (a network call) was invoked twice per chapter. | Call once. |
| Network | `network/OkHttpExtensions.kt` (`Call.await`) | Response not closed on non-2xx, leaking the connection. | `response.close()` before resuming with the exception. |
| Network | `network/proxy/FoxTellerProxy.kt` | `values("Set-Cookie")[0]` throws when no cookie is set (e.g. cached response); response leaked on non-200. | `firstOrNull()`; close/`use` the response. |
| Extensions | `extension/util/ExtensionLoader.kt` | `metaData` dereferenced without a null check, `getString(...)!!`, and `toDouble()` on the version string; any malformed extension APK crashed the app at startup. | Return `LoadResult.Error` for each malformed case. |
| UI | `fragment/LibraryFragment.kt` (reset novel) | `runBlocking { GlobalScope.launch {...}.join() }` on the main thread around a network call. | `viewLifecycleOwner.lifecycleScope.launch` with `withContext(Dispatchers.IO)`. |
| UI | `activity/NavDrawerActivity.kt` | `intent.extras!!.remove("novel")` operates on a copy, so the notification extra was never consumed and the chapters screen re-opened on every recreation. | `intent.removeExtra("novel")`. |
| UI | `fragment/WebPageDBFragment.kt` | EventBus registered only when `savedInstanceState == null`, so restored reader pages ignored settings events; `getWebPageSettings(...)!!` in a loop. | Register when not already registered; `?: return@forEach`. |
| UI | `activity/ReaderDBPagerActivity.kt` (`checkUrl`) | Index computed from DB order while the pager list is reversed when "swipe right for next chapter" is on, so in-page links opened the mirrored chapter. | Index into the pager's own list. |
| UI | `activity/ChaptersPagerActivity.kt` | Null novel left the activity alive without `vm.init`, later throwing from `vm.novel`; duplicated `removeFromDataSet` call. | `finish()` on null; removed the duplicate. |
| UI | `fragment/ExtensionsFragment.kt` | The "Installed: NSFW" `when` branch was shadowed by the more general branch above it. | Reordered. |
| UI | `viewmodel/SearchUrlViewModel.kt` | A reset load was dropped when a previous load was in flight, and the old load then wrote stale results for the new tab. | Track the load `Job`, cancel it on reset. |
| Database | `database/GenreHelper.kt` | Cursor leaked on every successful lookup (`return` before `close()`). | `use`. |
| Database | `database/DownloadHelper.kt` | One malformed `metadata` cell threw from every downloads query. | Try/catch with an empty map fallback, matching the other row mappers. |
| Model | `model/database/WebPageSettings.kt` | `hashCode()` included mutable fields that `equals()` ignores, breaking hash-based collections. | Hash only `url` and `novelId`. |
| Model | `model/other/TTSFilter.kt` | Stray `Regex("(?:asd)")` compiled and discarded on every call. | Removed. |
| Util | `util/logging/Logs.kt` | `error(tag, msg, throwable)` dropped the throwable in release builds, so no non-fatal reached Crashlytics. | Pass the throwable through. |
| Util | `util/Utils.kt` (`unzip`) | `ZipInputStream` never closed; zip entries could escape the target directory. | `use`; reject entries whose canonical path leaves the target directory. |
| Util | `util/Utils.kt` (`copyErrorToClipboard`) | Operator precedence dropped the stack trace whenever a message was present. | Parenthesised. |
| Util | `util/view/ProgressNotificationManager.kt` | `NotificationQueue.close()` never closed its channels, so the consumer coroutine stayed parked forever (one leak per backup/restore run). | Close the channels; consumer exits on `ClosedReceiveChannelException`. |
| Workers | `worker/BackupWorker.kt`, `worker/RestoreWorker.kt` | `getString(resId, formatArgs)` passed the vararg array as a single argument, rendering `[Ljava.lang.Object;@…`. | Spread operator. |
| Workers | `worker/RestoreWorker.kt` | `BufferedReader` never closed. | `use`. |
| Sync | `service/sync/BackgroundNovelSyncTask.kt` | `Worker.doWork()` launched the sync in `GlobalScope` on the main thread and returned success immediately, so WorkManager considered the job done before it ran and `Result.retry()` was unreachable. | Converted to `CoroutineWorker`; sync runs to completion on `Dispatchers.IO`. |
| Download | `service/download/DownloadNovelService.kt` | `notifyFirst()` returned before `startForeground()` when `POST_NOTIFICATIONS` was denied, so the service was never foreground and was killed after backgrounding. | `startForeground()` unconditionally; `notify()` still checks the permission. |
| TTS | `service/tts/TTSPlayer.kt` | `MediaPlayer.stop()` followed by `start()` is an invalid transition; the silence track stopped working after the first pause. | `pause()`. |
| AI TTS | `service/ai_tts/AiTtsModelManager.kt` | ONNX sessions were dropped without `release()` on every voice switch and on delete. | `release()` in `unloadModel()`; `deleteModel()` delegates to it. |
| AI TTS | `service/ai_tts/AiTtsService.kt` | Notification transport buttons were broadcast to androidx `MediaButtonReceiver`, which resolves to the only service with a media-browser intent-filter: the legacy `TTSService`. | Send the media-button intents to `AiTtsService` itself; `onStartCommand` already forwards them to its media session. |

## Findings not fixed

These were verified by reading the code but need design decisions, broader refactoring, or
device testing that this review could not do. Severity is the reviewer's estimate.

### High

- **`network/cloudflare/CloudflareInterceptor.kt:47`** `intercept()` is `@Synchronized` around
  `chain.proceed()`, serialising every request on the shared client to one at a time. The download
  service's five parallel workers therefore run sequentially, and a 15 s WebView bypass on one host
  stalls all others. Suggested fix: lock per host and only around the bypass, and make
  `bypassAttempts` a `ConcurrentHashMap`. Related: at line 83 a stale `cf_clearance` cookie causes
  the interceptor to re-send the same request and return the challenge page instead of bypassing.
- **`service/download/DownloadNovelThread.kt:365` with `DownloadNovelService.kt:155`**
  `DownloadNovelThread` extends `Thread` but is submitted to an executor as a `Runnable`, so
  `isAlive` is always false, `interrupt()` targets nothing, and `threadPool.remove(thread)` is a
  no-op. Pause followed by resume can start a second thread for the same novel. Suggested fix:
  keep a `ConcurrentHashMap<Long, Future<*>>`, cancel via `future.cancel(true)`, and make the
  classes plain `Runnable`s.
- **`network/MultiTrustManager.kt:59` with `NovelLibraryApplication.kt:169`**
  `addDefaultTrustManager()` installs a no-op `X509TrustManager` as the process-wide
  `HttpsURLConnection` factory, so `Jsoup.connect(...)` calls (`proxy/WattPadProxy.kt:28`,
  `cleaner/HtmlCleaner.kt` image download) perform no certificate validation. OkHttp traffic is
  unaffected. Not changed here because it may be intentional for sources with broken certificates;
  the safer route is to send those calls through `NetworkHelper`'s OkHttp client.

### Medium

- `NovelLibraryApplication.kt:53`, `database/DBHelper.kt:44`, `AppModule.kt:21`: `DBHelper` is
  cached as an Injekt singleton, so `refreshInstance()` after database corruption leaves every
  `injectLazy<DBHelper>()` consumer on the old, deleted handle. Only the UI path masks this with an
  app restart.
- `service/tts/TTSPlayer.kt:742`: web-loaded chapters mutate player state and call `tts.speak`
  from `Dispatchers.IO` while the main thread reads the same `lines` list and command queue.
  Hop to the main dispatcher after the network fetch.
- `service/ai_tts/AiTtsPlayer.kt:187`: `join(2000)` on the player thread before releasing the
  `AudioTrack`, on the main thread, while the thread is blocked in `AudioTrack.write()`. Pause and
  flush the track first, then join. `destroy()` also uses `runBlocking` on the main thread.
- `service/ai_tts/AiTtsService.kt:156`: the service leaves the foreground while paused and calls
  `startForeground()` again on audio-focus gain, which is subject to the API 31+ background
  foreground-service restriction. Marked plausible, not confirmed on a device.
- `worker/AiTtsModelDownloadWorker.kt:119`: download loop ignores cancellation and has no HTTP
  timeouts; with `ExistingWorkPolicy.REPLACE` two workers can write the same model file.
- `viewmodel/AiTtsManageModelsViewModel.kt:45`: `observeForever` on WorkManager `LiveData` with
  no `onCleared()`, leaking one observer set per screen open.
- `fragment/ExtensionsFragment.kt:69`: Rx subscriptions from `bindToExtensionsObservable()` and
  install/update observers are never unsubscribed; each swipe-refresh adds another.
- `activity/NovelDetailsActivity.kt:454`: `yield()` does not wait for `refreshFromDatabase()`,
  so the "Add to Library" button is rebuilt from the stale novel.
- `network/sync/NovelUpdatesSync.kt:143`: `fetchCategories()` swallows all exceptions and returns
  an empty list, and `addSection`/`removeSection` immediately post that list back, which can wipe
  the user's remote reading lists on a transient error. The response at line 210 is also never
  closed.
- `database/NovelHelper.kt:27`: `createNovel` never writes `order_id` and `getNovelFromCursor`
  never reads it, so "reset novel" loses the user's library ordering.
- `database/AppDatabase.kt` and `database/dao/*`: the Room layer has no callers, while
  `DBKeys.DATABASE_VERSION` was bumped to 11 with no matching step in `DBHelper.onUpgrade`. If
  Room is wired in as-is, schema validation will fail on existing installs.
- Main-thread database and file I/O in legacy UI: `LibraryFragment.kt:260` (one query per row
  bind), `WebPageDBFragment.kt:266` (parsing chapter files), `ChaptersFragment.kt:139`,
  `ReaderDBPagerActivity.kt:129`, `NovelSectionsActivity.kt:66`, `NovelDetailsActivity.kt:421`.

### Low

- Dead code that can be deleted: `fragment/SearchFragment.kt`, `SearchTermFragment.kt`,
  `SearchUrlFragment.kt`, `activity/settings/BackupSettingsActivity.kt` (unreachable; only the
  manifest references it), `compose/search/SearchExamples.kt`,
  `network/cloudflare/CloudflareBypassHelper.kt`, `network/JsoupNetworkHelper.kt` (fully
  commented out), `network/ProgressResponseBody.kt` and `newCallWithProgress`,
  `service/tts/TTSEventListener.kt`, `extension/ExtensionUpdateJob.setupTask` (would throw:
  periodic work cannot be expedited), `util/system/Base64Ext.kt`, `util/system/StartIntentExt.kt:43`
  (calls `registerForActivityResult` after start).
- The header comment in `compose/search/SearchFragmentCompose.kt` states that `SearchFragment.kt`
  is canonical; it is the other way round.
- `util/storage/FileExt.kt:46` `getReadableSize()` never formats (`"%.1234.5f EB"`), and
  `util/lang/StringExtensions.kt:90` uses `Regex.fromLiteral` for a character class. Both unused.
- `worker/WorkBuilder.kt:9`: `ONE_TIME_BACKUP_WORK_TAG` and `PERIODIC_BACKUP_WORK_TAG` share the
  same string, so tag-based cancellation cannot distinguish them.
- `worker/AiTtsModelDownloadWorker.kt:185`: `postError` ignores its message, so download failures
  are silent.
- `cleaner/GenericSelectorQueryCleaner.kt:164`: `AddAttribute` indexes `split[1]` without checking
  that `=` exists and writes to the unfiltered element list.
- `activity/TextToSpeechControlsActivity.kt:271`: the frame-rate timer runnable is not removed in
  `onStop`.
- Duplication worth consolidating: `network/proxy/BaseProxyHelper.kt` and
  `network/postProxy/BasePostProxyHelper.kt` are identical; `TTSNotificationBuilder` and
  `AiTtsNotificationBuilder` differ only in constants; five cleaner subclasses repeat the same
  "walk up to body removing siblings" loop; `TTSPlayer` has five copies of the chapter-navigation
  template.
- Naming trap: `DBKeys.KEY_NEW_RELEASES_COUNT = "chapter_count"` and
  `KEY_CHAPTERS_COUNT = "new_chapter_count"` are swapped relative to their columns.

## Areas not covered

`compose/**` previews, `model/source/filter/*`, `util/view/ProgressLayout.java`,
`util/view/TwoWaySeekBar.java`, `util/lang/HtmlToPlainText.java`, `TTSWrapper`'s AudioTrack
marker engine, `AndroidCookieJar` string parsing, `ExtensionInstaller.activeDownloads` thread
safety, and XML layouts were skimmed or skipped.

## Validation

The changed files were run through the standalone Kotlin 2.1 compiler without the Android
classpath; no syntax errors were reported (only expected unresolved Android references). A full
`./gradlew assembleNormalDebug` and manual testing of downloads, the reader, TTS, AI TTS
notification controls, backup/restore, and the background sync are still required.
