#!/bin/bash
# Session startup hook for NovelLibrary Android project.
# Installs tooling needed to profile, test, load, and debug Android apps
# in cloud (Claude Code on the web) sessions.
set -euo pipefail

# Only run in remote/cloud environments
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

echo "=== NovelLibrary Android Dev Environment Setup ==="

JAVA_HOME_PATH="/usr/lib/jvm/java-21-openjdk-amd64"
ANDROID_SDK_DIR="/usr/lib/android-sdk"
BUILD_TOOLS_VERSION="29.0.3"

# -------------------------------------------------------
# 1. Java / JVM
# -------------------------------------------------------
echo "[1/5] Configuring Java 21..."
echo "export JAVA_HOME=${JAVA_HOME_PATH}" >> "$CLAUDE_ENV_FILE"
echo "export PATH=\${JAVA_HOME}/bin:\${PATH}" >> "$CLAUDE_ENV_FILE"
export JAVA_HOME="${JAVA_HOME_PATH}"

# -------------------------------------------------------
# 2. Android SDK tools (via apt)
#    Provides: adb, aapt, aapt2, apksigner, zipalign,
#              dexdump, sqlite3
# -------------------------------------------------------
echo "[2/5] Installing Android SDK tools via apt..."

# Ensure package lists are fresh; ignore failures from blocked PPAs
apt-get update -qq 2>/dev/null || true

# Install core Android tooling; use --fix-broken to handle any
# pre-existing broken packages (e.g. libprotobuf32 mismatch)
DEBIAN_FRONTEND=noninteractive apt-get install -y -q --fix-missing \
  adb \
  android-sdk-build-tools \
  apksigner \
  aapt \
  zipalign \
  dexdump \
  sqlite3 \
  2>/dev/null || DEBIAN_FRONTEND=noninteractive apt-get install -y -q --fix-broken 2>/dev/null || true

# -------------------------------------------------------
# 3. Android SDK environment variables
# -------------------------------------------------------
echo "[3/5] Setting Android SDK environment..."
echo "export ANDROID_HOME=${ANDROID_SDK_DIR}" >> "$CLAUDE_ENV_FILE"
echo "export ANDROID_SDK_ROOT=${ANDROID_SDK_DIR}" >> "$CLAUDE_ENV_FILE"
echo "export PATH=\${PATH}:${ANDROID_SDK_DIR}/platform-tools:${ANDROID_SDK_DIR}/build-tools/${BUILD_TOOLS_VERSION}" >> "$CLAUDE_ENV_FILE"
export ANDROID_HOME="${ANDROID_SDK_DIR}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_DIR}"
export PATH="${PATH}:${ANDROID_SDK_DIR}/platform-tools:${ANDROID_SDK_DIR}/build-tools/${BUILD_TOOLS_VERSION}"

# -------------------------------------------------------
# 4. Gradle daemon JVM — point at the local OpenJDK 21
#    so Gradle does not try to download a JetBrains JVM
#    from api.foojay.io (blocked in cloud sessions).
# -------------------------------------------------------
echo "[4/5] Configuring Gradle JVM..."
mkdir -p "${HOME}/.gradle"
GRADLE_PROPS="${HOME}/.gradle/gradle.properties"
if ! grep -q "org.gradle.java.home" "${GRADLE_PROPS}" 2>/dev/null; then
  echo "org.gradle.java.home=${JAVA_HOME_PATH}" >> "${GRADLE_PROPS}"
fi

# -------------------------------------------------------
# 5. Stub google-services.json (required for compilation)
#    The real file is git-ignored; the stub satisfies the
#    Firebase Gradle plugin so the project can compile.
#    Runtime Firebase features will not work with this stub.
# -------------------------------------------------------
echo "[5/5] Checking google-services.json..."
GOOGLE_SERVICES="${CLAUDE_PROJECT_DIR}/app/google-services.json"
if [ ! -f "${GOOGLE_SERVICES}" ]; then
  echo "  Creating stub google-services.json..."
  cat > "${GOOGLE_SERVICES}" << 'GSJSON'
{
  "project_info": {
    "project_number": "000000000000",
    "project_id": "novellibrary-stub",
    "storage_bucket": "novellibrary-stub.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:000000000000:android:0000000000000000000000",
        "android_client_info": { "package_name": "io.github.gmathi.novellibrary" }
      },
      "oauth_client": [],
      "api_key": [{ "current_key": "stub_api_key" }],
      "services": { "appinvite_service": { "other_platform_oauth_client": [] } }
    },
    {
      "client_info": {
        "mobilesdk_app_id": "1:000000000000:android:1111111111111111111111",
        "android_client_info": { "package_name": "io.github.gmathi.novellibrary.mirror" }
      },
      "oauth_client": [],
      "api_key": [{ "current_key": "stub_api_key_mirror" }],
      "services": { "appinvite_service": { "other_platform_oauth_client": [] } }
    }
  ],
  "configuration_version": "1"
}
GSJSON
fi

# -------------------------------------------------------
# Warm up Gradle wrapper (downloads distribution if needed)
# Full dependency resolution requires Google Maven which is
# network-restricted in cloud sessions; the warm-up is
# best-effort and will not fail the hook on errors.
# -------------------------------------------------------
echo ""
echo "Warming up Gradle wrapper..."
cd "${CLAUDE_PROJECT_DIR}"
./gradlew --version --no-daemon 2>/dev/null || true

# -------------------------------------------------------
# Summary
# -------------------------------------------------------
echo ""
echo "=== Environment Ready ==="
echo "Java:            $(java -version 2>&1 | head -1)"
echo "ADB:             $(adb version 2>/dev/null | head -1 || echo 'not installed')"
echo "AAPT:            $(aapt version 2>/dev/null || echo 'not installed')"
echo "DexDump:         $(which dexdump 2>/dev/null || echo 'not installed')"
echo "Apksigner:       $(apksigner --version 2>/dev/null || echo 'not installed')"
echo "SQLite3:         $(sqlite3 --version 2>/dev/null || echo 'not installed')"
echo "ANDROID_HOME:    ${ANDROID_HOME}"
echo "Build-tools:     ${ANDROID_SDK_DIR}/build-tools/${BUILD_TOOLS_VERSION}"
echo ""
echo "NOTE: Full Gradle builds (assembleNormalDebug, test, lint) require"
echo "Android Gradle Plugin dependencies from Google Maven (dl.google.com)."
echo "If dependencies are not cached, pre-populate them locally with:"
echo "  ./gradlew dependencies --write-verification-metadata sha256"
echo "and commit the resulting verification-metadata.xml, or run once on a"
echo "machine with Google Maven access so the Gradle cache is populated."
