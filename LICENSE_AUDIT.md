# ColorGoogle license audit context

The historical upstream audit follows. ColorGoogle also includes LibADB and its
SPAKE2, Conscrypt and Bouncy Castle dependencies; see THIRD_PARTY_NOTICES.md and
adbclient/COLORGOOGLE-NOTICE.md. Do not interpret the old direct-dependency list
as the complete current inventory. The release ships matching source and notices.

---

# MindTrigger Assist v16 — license/compliance audit

Audit date: 2026-08-18

This is a project compliance review, not legal advice.

## Result

**No direct-license conflict was identified in the v16 source tree.** Keeping
MindTrigger Assist under `GPL-3.0-only` is the conservative release choice for
the current provenance because the Circle to Search bridge is treated as an
upstream-derived MiCTS path and MiCTS is distributed under GNU GPL version 3.

## Project / upstream

### MindTrigger Assist

- Declared project license: `GPL-3.0-only`.
- All 16 Java source files in the app package carry
  `SPDX-License-Identifier: GPL-3.0-only`.
- Full GPLv3 license text is present in `LICENSE` and bundled for in-app viewing.

### MiCTS

- Upstream: https://github.com/parallelcc/MiCTS
- GitHub reports the repository license as GPL-3.0.
- MindTrigger Assist treats its CTS invocation path as implemented with reference
  to MiCTS rather than as a clean-room independent implementation.

Conclusion: distributing the combined project under GPL-3.0-only is consistent
with this provenance model and avoids pretending the CTS path is permissively
licensed.

## Direct dependencies

### Material Components for Android 1.14.0

- Upstream: https://github.com/material-components/material-components-android
- License: Apache License 2.0.
- The upstream repository identifies 1.14.0 as a release and Apache-2.0 as its
  license.

### Google Material Icons

- Upstream: https://github.com/google/material-design-icons
- License: Apache License 2.0.

### Shizuku API/provider 13.1.5

- Upstream: https://github.com/RikkaApps/Shizuku-API
- License: MIT License.
- The upstream POM configuration identifies the library license as MIT.
- Required copyright/license text is bundled in
  `THIRD_PARTY_LICENSES/Shizuku_API_MIT.txt`.

### AndroidHiddenApiBypass 6.1

- Upstream: https://github.com/LSPosed/AndroidHiddenApiBypass
- License: Apache License 2.0.
- Upstream copyright notice: Copyright 2021-2025 LSPosed.
- Apache text and attribution are bundled.

## Compatibility with GPLv3

The Free Software Foundation explicitly identifies Apache License 2.0 as
compatible with GPLv3. The Shizuku license text is the common Expat-form MIT license; the FSF lists the Expat license as GPL-compatible.
The combined MindTrigger Assist work remains distributed under GPL-3.0-only,
while required third-party notices are preserved.

References:

- https://www.gnu.org/licenses/quick-guide-gplv3.html
- https://www.gnu.org/licenses/license-list.html
- https://www.gnu.org/licenses/gpl-faq.html


## Project disclaimer / GPL additional restrictions

The redistribution/added-content disclaimer is written as a non-normative
responsibility statement. It explicitly says that it does not limit GPL rights
or add a field-of-use restriction. It therefore is not intended to create an
additional use/distribution condition on recipients.

## Audio assets

Known project provenance says `aura_cts.wav` and `aura_gemini.wav` were generated
with Claude Code at the project author's direction. Release inspection confirms
both are plain PCM WAV files and records their hashes in `AUDIO_PROVENANCE.md`.

No embedded ownership metadata or known imported third-party sample was found.
That does not prove global originality. Because copyrightability of AI-generated
output can vary by jurisdiction, the project uses a conservative rights statement
instead of asserting exclusive copyright merely because AI was involved.

## Distribution obligations to keep in mind

For a public APK/object-code release, the distributor must satisfy the applicable
GPLv3 source-distribution requirements for that build and preserve license/
notice obligations. Keeping the exact source ZIP or a public source repository
for the released APK is the simplest operational approach.

The in-app license viewer is convenience only; it does not replace source-level
license/notice files.

## Scope limitation

This audit covers:

- project source and bundled assets present in this source tree;
- direct Gradle dependencies declared by `app/build.gradle`;
- upstream licenses checked above.

A full built-APK transitive dependency/SBOM audit was not possible in this
packaging environment because it does not contain the Android SDK/Gradle build
toolchain. Before a store-scale distribution, a built artifact dependency report
is still advisable.
