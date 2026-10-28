# GitHub Actions — Release

This repo has a single, manually-triggered release workflow. There is no per-push / per-PR CI.

| Workflow | File | Trigger | What it does |
|----------|------|---------|--------------|
| **Release** | `.github/workflows/release.yml` | Manual (**Actions → Release → Run workflow**) | Builds the **signed** `normalRelease` APK, creates the tag, and publishes a GitHub Release with the APK attached. |

## Required repository secrets

Set these under **Settings → Secrets and variables → Actions → New repository secret**.

| Secret | Value |
|--------|-------|
| `GOOGLE_SERVICES_JSON` | base64 of `app/google-services.json` |
| `KEYSTORE_BASE64` | base64 of `apps.keystore` |
| `KEYSTORE_PASSWORD` | store password |
| `KEY_ALIAS` | key alias (`novellibrary`) |
| `KEY_PASSWORD` | key password |

### Generating the base64 values (PowerShell)

```powershell
# google-services.json
[Convert]::ToBase64String([IO.File]::ReadAllBytes("app/google-services.json")) | Set-Clipboard

# keystore
[Convert]::ToBase64String([IO.File]::ReadAllBytes("apps.keystore")) | Set-Clipboard
```

(On Linux/macOS: `base64 -w0 app/google-services.json`.)

## Cutting a release

1. Add the new release entry at the top of
   `app/src/main/java/io/github/gmathi/novellibrary/util/changelog/Changelog.kt`
   (this drives the in-app What's Changed screen and update dialog).
2. Mirror that entry into `.github/RELEASE_NOTES.md` — this is the **GitHub Release body**.
   Keep the two in sync; the app reads `Changelog.kt`, the release page reads this file.
3. Bump `versionCode` / `versionName` in `app/build.gradle`, commit, and push to `main`.
4. Go to the **Actions** tab → **Release** → **Run workflow**.
5. Enter the tag (e.g. `v2.4.0`). Leave **ref** as `main` (or point it at another branch/commit).
6. Run it. The workflow builds the signed APK, creates the tag from that ref, and publishes
   the release at `https://github.com/gmathi/NovelLibrary/releases/tag/v2.4.0`
   with the notes from `.github/RELEASE_NOTES.md`.

The workflow **creates the tag for you** — you do not need to `git tag`/`git push` the tag yourself.
Use a `v`-prefixed tag (`v2.4.0`) for consistency.

## Notes

- Runs on `ubuntu-latest` with JDK 17 (matches the project's Java 17 target).
- The release body comes from `.github/RELEASE_NOTES.md` (`body_path`), **not** auto-generated
  notes — GitHub's auto-generation walks the entire PR history when no prior release tag exists.
- `release.yml` reconstructs `keystore.properties` at build time pointing at the
  decoded `apps.keystore`, matching the local layout (`storeFile=../apps.keystore`).
- `google-services.json` is gitignored, so the `GOOGLE_SERVICES_JSON` secret is required
  even though this is a release-only workflow.
