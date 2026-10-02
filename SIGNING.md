# Building and signing ColorGoogle

Use JDK 17, Android SDK Platform 36 and Build Tools 36.0.0. Set `ANDROID_HOME` to
your local SDK, or create an ignored `local.properties` with `sdk.dir=...`.
The checked-in Gradle wrapper resolves the pinned build/dependency versions.

## Debug builds

No project release key is required:

```bash
bash gradlew testDebugUnitTest assembleDebug lintDebug --no-daemon
```

Windows: use `.\gradlew.bat` with the same task arguments. A debug APK has a different
certificate from the published APK and cannot overwrite it. Never uninstall a user's
working application merely to test a differently signed build.

## Release builds

Copy `keystore.properties.example` to `keystore.properties` and supply your own
keystore path, alias and passwords. Keep the file and key outside Git. Alternatively
provide `MTA_KEYSTORE`, `MTA_STORE_PASSWORD`, `MTA_KEY_ALIAS`, `MTA_KEY_PASSWORD` through
a secure local environment. These variable names are retained for upstream compatibility.

```bash
bash gradlew testReleaseUnitTest assembleRelease lintRelease --no-daemon
python3 tools/release_sanity.py
```

Windows: `.\gradlew.bat testReleaseUnitTest assembleRelease lintRelease --no-daemon`
and `py -3 tools\release_sanity.py`.

Output: `app/build/outputs/apk/release/app-release.apk`. Release tasks intentionally
fail when signing credentials are absent; no unsigned release is silently emitted.
For a clean-source reproduction use your own key. Byte-identical APKs are not claimed.

## Published key and upgrades

ColorGoogle retains the previous private ColorGoogle release key for compatible
in-place upgrades. The official upstream MindTrigger APK uses a different key.
The public package ID remains `dev.evoker.homeholdcts`; Android cannot keep both
installed in the same user or update between different signing identities.
Uninstallation loses configuration and wireless pairing because app backup is disabled.

Published certificate SHA-256:

```text
e52320218f36db099e6971b0070bba40eec451c1a69f634bb073be26a81f236e
```

Verify the downloaded APK with Android SDK `apksigner`:

```text
apksigner verify --verbose --print-certs ColorGoogle-16.3.0.apk
```

Both variants use **APK Signature Scheme v3**; v1, v2 and v4 are disabled (minimum API
32). Expect v3 verification, not v1/v2 success. Compare the file's SHA-256 separately
against `SHA256SUMS` in the release. A file checksum is not a replacement for a signing
identity check.

The private release key is never included in Git, source archives or CI. Keep an offline
backup: losing it prevents compatible updates to existing installations.
