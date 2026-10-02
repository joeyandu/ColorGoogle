# Source provenance

## MindTrigger Assist-specific implementation

MindTrigger Assist-specific work includes:

- ColorOS long-press Home/gesture signal detection;
- isolated `:watcher` foreground-service lifecycle;
- privileged logcat access/session state handling;
- transparent `LogSessionBridgeActivity` recovery flow;
- trigger classification/debounce and activation orchestration;
- Google/Gemini readiness handling;
- Shizuku / PC one-shot setup;
- ColorOS Recent Tasks and OEM setup guidance;
- Material 3 UI, theme handling and localization.

MindTrigger Assist modifications are attributed to @EvokerUniverse.
AI coding assistance is credited as Chat GPT.

## CTS invocation bridge

The stable Circle to Search invocation path was implemented with reference to
the public MiCTS project and is therefore treated as an upstream-derived GPL
path for licensing and attribution purposes.

The source intentionally preserves protocol elements required by the tested CTS
behavior, including the `voiceinteraction` binder route and CTS invocation
arguments. Cosmetic renaming is not used as a substitute for license compliance
or provenance disclosure.

Upstream: https://github.com/parallelcc/MiCTS

See `NOTICE.md`, `LICENSE`, and `LICENSE_AUDIT.md`.

## ColorGoogle public release

ColorGoogle 16.3.0 starts from the official MindTrigger Assist v16.2.0 tag,
then includes the local ColorGoogle .1–.6 changes and public-release preparation.
Upstream: https://github.com/evokermc098-coder/MindTriggerAssist/tree/v16.2.0
The annotated tag object is c06db1c700bee28552ae93f09538a7a52d92680f, pointing to
commit 365f2924ee4281f226adb04ea50490bfe6c4d024.
No upstream Git authorship is reassigned; this independent repository preserves
source headers/notices and identifies the original release explicitly.

Additional dependency provenance is in adbclient/COLORGOOGLE-NOTICE.md.
The user-supplied four-color G/ribbon artwork and spacing edit are retained in
artwork/. Spacing was produced with image-generation assistance; the original
and prompt are included. Google/OPPO branding is not claimed as an original
ColorGoogle trademark or as covered by code-license trademark permissions.
