# Unit Test Notes (`app/src/test`)

This module has **no Robolectric and no mocking framework** configured — only plain JUnit 5
(via jqwik's JUnit Platform integration), JUnit 4 (legacy, vintage engine), and jqwik for
property-based tests. Keep that in mind before adding new tests: anything that needs a real
`Context`, `SQLiteDatabase`, `SharedPreferences`, `WebView`, etc. cannot be instantiated here.

## Established patterns for testing Android-dependent code

- **Pure-seam extraction**: where a function needs a `Context` only to resolve something (e.g.
  `Utils.getNovelDir` resolving the active storage root via `Context`), extract the pure logic
  into a sibling overload that takes the already-resolved value directly (e.g.
  `Utils.buildNovelDir(root: File, ...)`). Test the pure overload; leave the `Context`-based one
  as a thin wrapper.
- **Fakes over mocks**: for framework interfaces that are cheap to fake (e.g.
  `SharedPreferences`), write a minimal in-memory fake implementing just the methods exercised
  (see `FakeSharedPreferences`) rather than pulling in Mockito/Robolectric.
- **Model the algorithm, don't invoke it**: for classes that are expensive to make testable
  (e.g. `StorageMigrator`, which needs a real `DBHelper`), some tests replicate the exact
  algorithm/branch logic against real temp directories instead of instantiating the class (see
  `StorageMigratorSafetyPropertyTest`, `StorageMigratorSourceDirRemovalTest`). Keep these in sync
  with the production code they mirror — the doc comment on each such test names the exact method
  it mirrors for this reason.
- **Windows path safety**: `java.io.File` normalizes `/storage/...` paths to backslash form on
  Windows, which breaks any logic doing literal `/storage/` substring matching (this was a real,
  previously-hidden bug in `StorageLocationResolver.extractVolumeId`, fixed by normalizing
  separators before matching). When a test needs to fabricate Android-style absolute paths,
  either rely on that normalization already being in place in production code, or use a `File`
  subclass that pins `path`/`absolutePath` to the Android-style string (see
  `AndroidStylePathFile` in `StorageLocationResolverPropertyTest`).

## Known brittleness: `DownloadMigrationGuardTest`

`DownloadMigrationGuardTest` (`service/download/`) needs to flip
`StorageMigrator.isMigrationInProgress`, but that property has a `private set` and
`StorageMigrator` can't be constructed here (real `Context`/`DBHelper`/`DataCenter` required), so
there's no legitimate way to drive it via a real `migrate()` call. The test instead uses
reflection to invoke the Kotlin-compiler-generated static bridge method
(`access$setMigrationInProgress$cp`) that backs the companion object's `@Volatile private set`
field.

This works today, but it depends on:
- Kotlin's current strategy of hoisting `@Volatile` companion backing fields onto the outer class
  and generating an `access$...$cp` bridge for private setters.
- The exact generated method name, which is not part of any public API contract.

**If this test starts failing after a Kotlin version bump** (e.g. `NoSuchMethodException` /
`NoSuchFieldException`), that's the first thing to suspect — not a real regression in the
migration-lock logic. If it becomes a recurring problem, the more robust fix is to give
`StorageMigrator` a small `@VisibleForTesting` internal setter (or an internal test-only seam)
instead of reflecting into compiler internals.

Also note: `isMigrationInProgress` is process-global mutable state. Every test that touches it
must reset it in a `finally` block, or it can leak into other `StorageMigrator`-related tests
(`StorageMigratorInsufficientSpacePropertyTest`, `StorageMigratorSafetyPropertyTest`,
`StorageMigratorSourceDirRemovalTest`, etc.) running in the same JVM/test run.