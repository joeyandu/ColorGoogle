# Third-party notices

This file records direct libraries/assets intentionally used by the v16 source.
It is not a substitute for the license text files bundled with the project.

## MiCTS

- Project: MiCTS
- Upstream: parallelcc/MiCTS
- Repository: https://github.com/parallelcc/MiCTS
- License: GNU GPL version 3

The Circle to Search invocation path in MindTrigger Assist was implemented with
reference to MiCTS and is treated as an upstream-derived GPL path. Project-level
release license: GPL-3.0-only.

The full GPL text is in `LICENSE` and is also bundled for in-app viewing.

## Google Material Components for Android

- Artifact: `com.google.android.material:material:1.14.0`
- Upstream: https://github.com/material-components/material-components-android
- License: Apache License 2.0

License text: `THIRD_PARTY_LICENSES/Google_Material_Design_Apache-2.0.txt`

## Google Material Icons

The bottom-navigation icon geometry uses Google Material Icons designs.

- Upstream: https://github.com/google/material-design-icons
- License: Apache License 2.0

The original MindTrigger launcher icon was a project asset. ColorGoogle replaces it
with user-supplied G/ribbon artwork; see SOURCE_PROVENANCE.md. It is not a Material
icon, and no Google/OPPO endorsement or trademark permission is claimed.

## Shizuku API

Direct artifacts:

- `dev.rikka.shizuku:api:13.1.5`
- `dev.rikka.shizuku:provider:13.1.5`

Upstream: https://github.com/RikkaApps/Shizuku-API
License: MIT License
Copyright (c) 2021 RikkaW

License text: `THIRD_PARTY_LICENSES/Shizuku_API_MIT.txt`

Note: the Shizuku manager application is a separate product. The direct library
used by this source is Shizuku-API, whose repository declares the MIT License.

## AndroidHiddenApiBypass

- Artifact: `org.lsposed.hiddenapibypass:hiddenapibypass:6.1`
- Upstream: https://github.com/LSPosed/AndroidHiddenApiBypass
- License: Apache License 2.0
- Copyright: 2021-2025 LSPosed

License text:
`THIRD_PARTY_LICENSES/AndroidHiddenApiBypass_Apache-2.0.txt`

## Bundled audio

`aura_cts.wav` and `aura_gemini.wav` are project audio assets. Their known
provenance and SHA-256 hashes are documented in `AUDIO_PROVENANCE.md`; they are
not third-party library assets.


## ColorGoogle .6 local wireless ADB client

- LibADB Android 3.1.1, Muntashir Al-Islam: dual Apache-2.0 or GPL-3.0-or-later.
  ColorGoogle uses Apache-2.0; source headers, bundled licenses and local changes
  are in `adbclient/` and `adbclient/COLORGOOGLE-NOTICE.md`.
- `com.github.MuntashirAkon.spake2-java:spake2-android:2.2.1` declares GNU LGPL v3.0
  in its published POM. Source: https://github.com/MuntashirAkon/spake2-java/tree/2.2.1
  Its source ZIP is also supplied alongside the APK in this release, with unmodified
  Java sources and native submodule spake2-c at
  `0d15933e5ba3e662cb01245a7ac0dc9fca3eac31` included.
  To modify/relink it, build that source and replace the Gradle dependency with
  your rebuilt local artifact, then rebuild/sign ColorGoogle with your own key.
- `org.conscrypt:conscrypt-android:2.5.3` declares Apache 2 in its published POM.
  Source: https://github.com/google/conscrypt
- Bouncy Castle `bcprov-jdk15to18`, `bcpkix-jdk15to18` and their dependencies, 1.81,
  provide certificate construction and cryptography. Source/license:
  https://github.com/bcgit/bc-java/blob/main/LICENSE.html

These are build dependencies resolved by Gradle. No Shizuku manager is needed
for ColorGoogle's phone-local recovery feature.
