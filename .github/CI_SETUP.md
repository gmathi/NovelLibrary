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

1. Bump `versionCode` / `versionName` in `app/build.gradle`, commit, and push to `main`.
2. Go to the **Actions** tab → **Release** → **Run workflow**.
3. Enter the tag (e.g. `v2.4.0`). Leave **ref** as `main` (or point it at another branch/commit).
4. Run it. The workflow builds the signed APKs, creates the tag from that ref, and publishes the release at
   `https://github.com/gmathi/NovelLibrary/releases/tag/v2.4.0` with auto-generated notes.

The workflow **creates the tag for you** — you do not need to `git tag`/`git push` the tag yourself.

## Notes

- Runs on `ubuntu-latest` with JDK 17 (matches the project's Java 17 target).
- `release.yml` reconstructs `keystore.properties` at build time pointing at the
  decoded `apps.keystore`, matching the local layout (`storeFile=../apps.keystore`).
- `google-services.json` is gitignored, so the `GOOGLE_SERVICES_JSON` secret is required
  even though this is a release-only workflow.
