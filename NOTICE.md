# MindTrigger Assist — upstream and modification notice

MindTrigger Assist is distributed under GNU GPL-3.0-only.

MindTrigger Assist modifications:
Copyright (C) 2026 EvokerUniverse

## CTS upstream

The Circle to Search invocation path was implemented with reference to:

- Project: MiCTS
- Repository: https://github.com/parallelcc/MiCTS
- Upstream maintainer: parallelcc
- Upstream license: GNU GPL version 3

MindTrigger Assist does not claim endorsement by MiCTS or its contributors.

## MindTrigger Assist-specific work

Project-specific work includes ColorOS long-press signal detection, the isolated
`:watcher` foreground-service lifecycle, trigger classification/debounce,
privileged logcat session recovery, the transparent Log Session Bridge, Google
readiness handling, Shizuku/PC setup, ColorOS setup guidance, localization, and
the Material 3 application interface.

Modified for MindTrigger Assist by @EvokerUniverse.

AI coding assistance: Chat GPT.

Bundled activation audio provenance: generated with Claude Code at the project
author's direction; see `AUDIO_PROVENANCE.md`.

Third-party dependencies retain their own licenses and copyright notices. See
`THIRD_PARTY_NOTICES.md` and `LICENSE_AUDIT.md`.

## ColorGoogle modifications

Copyright (C) 2026 joeyandu and ColorGoogle contributors.
Based on MindTrigger Assist v16.2.0 by EvokerUniverse, retaining its GPL-3.0-only
license and MiCTS-derived invocation path. See CHANGELOG.md and docs/ARCHITECTURE.md
for modifications. ColorGoogle is not an official OPPO or Google product.
Brand names and artwork do not imply endorsement or grant trademark rights.
