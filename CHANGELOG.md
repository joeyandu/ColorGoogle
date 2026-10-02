# ColorGoogle changelog

## 16.3.0 — 2026-10-02

First public ColorGoogle release, based on MindTrigger Assist v16.2.0.

- Publish English-default and full Chinese README, three copyable Agent prompts
  per language, platform-specific setup, original-setting recovery, limitations
  and developer documentation.
- Default fresh installations to English while retaining explicit language choices.
- Translate added desktop/wireless/wake-status UI into English.
- Replace the private device serial in the activation command with SERIAL and
  include an Android-side activation script suitable for both Mac and Windows.
- Retain package/signing identity and existing activation protocols. No new public
  app API, voice-prefix feature, standalone widget or Hey Google eligibility bypass.

## Earlier private ColorGoogle versions

- **16.2.0-colorgoogle.1:** Direct power markers invoke the existing assistant session,
  including screen-off events; 2500 ms accepted-event debounce suppresses paired
  markers/service tails; notification permission and service-state distinction;
  ColorGoogle name/icon and zero-delay default.
- **.2:** Four-color G and green ribbon supplied by the user.
- **.3:** More icon whitespace and synchronized adaptive/legacy/in-app assets.
- **.4:** Beta switch for exact ColorOS dock-search interception; shell agent and
  removal of stale search tasks so Back returns to the launcher.
- **.5:** Shell-agent wake logs; foreign-UID health verification instead of treating
  own log heartbeats as proof; separate source/status and explicit disconnected UX.
- **.6:** Optional phone-local wireless ADB pairing/recovery, bounded retries,
  lifecycle cleanup, guarded dead-controller replacement and quiet-start health.

Known limitation: disabled wireless debugging prevents phone-local recovery.
No unconditional reboot, force-stop, long-idle, OTA or other-device guarantee.

---

# Historical upstream changelog

# MindTrigger Assist changelog

## v16.2.0 — 2026-09-07

- Promoted the current V16.1 SessionKeeper codebase to the signed V16.2 release.
- Kept the separate `:watcher` foreground-service runtime, direct `/dev/socket/logdr`
  reader, OverlayGuardian, alarm/job/boot recovery, and Android log-approval flow.
- Included the ColorOS-facing setup guidance, Quick Settings log-recovery tile,
  dark-mode dialog fixes, and bounded Assistant provider warm-up.
- Release signing now states the effective policy truthfully: v3 is used for
  the minSdk 32 app; legacy v1/v2 payloads are not shipped.
- No Device Admin, Accessibility service, child-process logcat, runtime Shizuku,
  permanent wakelock, or high-frequency recovery polling.

# V16.1.0 Beta 1

- Added a fifth bottom-navigation destination: **Beta**, using an extension/puzzle icon while preserving the existing Setup/Advanced/Support/About tab IDs.
- Added optional slow-motion tab transitions combining fade, scale and vertical movement. The animation is UI-only and does not change watcher timing or keepalive behavior.
- Moved the existing **Wakeup with voice** control from Advanced into Beta without changing its watcher preference/backend.
- Added per-target custom activation audio for Circle to Search and Google Assistant. Imported audio is limited to the first 3 seconds when the platform muxer supports the selected codec/container; every custom clip is also hard-stopped at 3 seconds during playback.
- Added an experimental **Swap CTS ↔ Google Assistant** control: Home/gesture becomes Assistant and Power long press becomes CTS while swap mode is enabled. Voice wake remains independent.
- Re-aligned page headers and section content padding for a tighter, consistent grid.
- Replaced the visible assistant-command action with **Assistant Settings**, opening Android's digital-assistant settings through the ManageAssist/VOICE_INPUT/default-apps fallback chain.
- Assistant secure-setting commands still run as part of privileged setup, but their raw command strings are redacted from current and previously stored in-app shell logs.
- Device Admin remains removed. DirectLogd, OverlayGuardian, START_STICKY, Alarm lease, JobScheduler rescue, boot/package recovery and log-session bridge behavior are unchanged from R5.2.

# V5 StableUI R5.2 — No Device Admin

- Removed the Device Admin receiver/component and `device_admin.xml`.
- Removed `dpm set-active-admin` from both Shizuku and PC one-shot setup command generation.
- Bootstrap readiness now requires READ_LOGS + the existing self Doze allowlist, not Device Admin.
- `systemExempted` FGS eligibility now uses the existing Doze battery-optimization exemption only; `specialUse` fallback is unchanged.
- Kept DirectLogd, OverlayGuardian, START_STICKY, Alarm lease, persisted JobScheduler rescue, boot/package recovery, trigger classifier and activation logic unchanged.
- Heat audit: no permanent WakeLock exists in the runtime source; the highest continuous CPU candidate is the direct logd stream, which reads text packets from main/radio/system/crash and filters tags in-process. No logd/keepalive behavior was changed in this release.

# V5 StableUI R5.1 — One-time Wizard + Bottom Navigation

- Restored the normal V5-style bottom navigation after onboarding.
- The first-run wizard remains full-screen and is shown only until `stable_ui_setup_finished=true` is committed.
- After onboarding, the Setup tab is a permanent compact scroll page instead of reopening the wizard.
- The permanent Setup tab reuses the same setup/action/verifier builders as onboarding, keeping UI state and behavior synchronized.
- Preserved the R4 navigation recursion fix: user-originated bottom-nav events render directly and never call `setSelectedItemId()` recursively.
- No watcher, DirectLogd, rescue, classifier, activation, bootstrap, or IPC runtime file changed from R4.

## v16.0.0-rc1-v5-stableui-r4-navfix

- Fixed a fatal `StackOverflowError` caused by recursive BottomNavigationView selection: user navigation callbacks now render tabs without calling `setSelectedItemId()` again.
- Runtime watcher/logd/rescue logic remains unchanged from the V5 stable baseline.


## v16.0.0-rc1-keepalive5-stableui

- Rebased on the stable V5 OverlayGuardian runtime; no V6 runtime/resurrection logic is included.
- Rebuilt Setup as a full-screen five-page Android-style wizard with Shizuku first.
- Removed Setup close chrome and hides the normal bottom navigation during setup.
- Added persistent wizard page state and explicit Shizuku/ADB action feedback.
- Kept live Google/Gemini battery-unrestricted verification from V5.
- `Finish & run` returns to the normal application UI after invoking the unchanged V5 start path.
- Disabled Android force-dark on AppTheme so White mode remains white under system dark mode.
## v16.0.0-rc1-keepalive5-overlayguard

- Replaces V4 three-lane direct-logd session with one low-overhead in-process direct-logd socket.
- Adds `OverlayGuardian`: keeps the existing 1x1 `TYPE_APPLICATION_OVERLAY` attached across SCREEN_OFF, SCREEN_ON, USER_PRESENT and watcher recreation.
- Adds `FLAG_SHOW_WHEN_LOCKED` while remaining non-touchable/non-focusable and never turning the display on.
- Adds delayed post-keyguard rechecks and a low-rate 60 s overlay attachment sanity check.
- Adds `onTrimMemory()` / `onLowMemory()` keep-alive reassertion.
- Keeps V4 START_STICKY + Alarm lease + persisted JobScheduler rescue + boot/package-replaced recovery.
- Keeps V1 Active Device Admin + Doze allowlist + `specialUse|systemExempted` FGS.
- No `/system/bin/logcat`, no child/phantom process, no stdout pipe, no Shizuku runtime dependency.

# KeepAlive V4 Resilient — 2026-08-22

- Replaced the single direct-logd socket with three in-process `logdr` lanes.
  All lanes belong to `:watcher`; there is still no `/system/bin/logcat` child.
- Added sequence-based heartbeat verification. A ColorOS freeze pauses both
  heartbeat producer and checker, so thawing does not create a false death.
- A single wedged lane is retired while healthy siblings continue serving
  Home/Power/Voice classification. No late lane respawn is attempted, avoiding
  unnecessary new Android log-access prompts.
- If every lane dies, the existing transparent TOP bridge is surfaced again
  when the screen is interactive / user is present so Android can authorize a
  fresh session.
- Added a pre-armed AlarmManager lease renewed by the watcher. If ColorOS/LMK
  kills `:watcher` without `onDestroy()`, the last system-owned alarm remains
  and attempts to resurrect the foreground service.
- Added persisted JobScheduler rescue as an independent fallback path.
- Added USER_PRESENT recovery retry after screen-off/process recreation.
- Added BOOT_COMPLETED and MY_PACKAGE_REPLACED restore paths.
- Watcher notification opens the log-session bridge directly when recovery is
  required.
- Preserved Device Admin + Doze allowlist + `specialUse|systemExempted` FGS.
- Shizuku remains one-shot provisioning only; runtime has no Shizuku dependency.

Unavoidable Android boundary: a user force-stop disables normal automatic
component delivery until the user launches the app again. A killed watcher also
loses its approved logd file descriptors; after Android's log-access approval
cache expires, a fresh session requires Android's confirmation UI.

# MindTrigger Assist changelog

## v16.0.0-rc1-keepalive3-directlogd — 2026-08-22

- KeepAlive V3: removed the runtime `/system/bin/logcat` child process entirely.
- Added `DirectLogdReader`, which connects from the `:watcher` process directly to Android logd via the reserved `logdr` SOCK_SEQPACKET endpoint.
- Preserved the existing Android privileged-log confirmation flow, READ_LOGS one-shot provisioning, Home/Power/Voice classification, debounce, Active Device Admin, Doze allowlist, and systemExempted FGS fallback.
- Direct reader streams only text buffers needed by the trigger path (`main`, `radio`, `system`, `crash`) and filters the same trigger tags in-process.
- No JNI/NDK and no global phantom-process setting are required for V3.

## v16.0.0-rc1-keepalive1 — 2026-08-22

- KeepAlive V1: add an Active Device Admin endpoint on user 0.
- One-shot Shizuku/PC bootstrap now activates that admin with `dpm set-active-admin`.
- Watcher FGS can use `systemExempted` when Device Admin or Doze-allowlist eligibility is active, while retaining `specialUse` as a runtime fallback.
- Existing READ_LOGS, logcat session, Home/Power/Voice classifiers, and one-shot Shizuku architecture are unchanged.

# Changelog

## v16.0.0 RC1 — repository-link sync

- Updated the in-app GitHub action to open the official MindTrigger Assist repository:
  `https://github.com/evokermc098-coder/MindTriggerAssist`.
- Added the same repository URL to `README.md`.
- Increased `versionCode` to `16000002` for the rebuild; `versionName` remains `v16.0.0-rc1`.


## v16.0.0 RC1 — 2026-08-18

### Release polish

- Rebuilt localization around four fully maintained release locales: vi-VN,
  en-US, id-ID and th-TH. Removed partial preview locale packs.
- Theme and accent changes now preserve the current bottom-navigation tab across
  Activity recreation instead of returning to Setup.
- Reworked user-facing terminology around READ_LOGS, privileged logcat sessions,
  VoiceInteractionManager, the Assistant route, foreground services and the Log
  Session Bridge.
- Changed AI credit to `Chat GPT` without exposing a model variant.
- Replaced topic-specific redistribution wording with a neutral added-content
  responsibility/non-association notice that does not restrict GPL rights.
- Added an in-app open-source license viewer with bundled GPL-3.0, Apache-2.0,
  Shizuku API MIT, MiCTS and AndroidHiddenApiBypass notices.
- Added direct dependency license audit and complete third-party notices.
- Documented Claude Code audio provenance, WAV properties and SHA-256 hashes.
- Preserved v2.1 watcher / transparent log-session bridge runtime behavior.

### Source hardening

- Made the internal `RestartReceiver` non-exported; system and same-app restart broadcasts remain supported.
- Removed the unrelated `USE_FULL_SCREEN_INTENT` AppOp from privileged setup.
- Added `SECURITY_SOURCE_AUDIT.md` covering component exposure, local privileged operations, network/telemetry absence, and audit limits.

### Build metadata

- package: `dev.evoker.homeholdcts`
- versionCode: `16000002`
- versionName: `v16.0.0-rc1`
- compileSdk / targetSdk: 36
- minSdk: 32
- APK signature schemes: v1 + v2 + v3

## v16.1.0-beta2-sessionkeeper

- Fixed device-log recovery timing on Android 13+ by deferring new logd requests until an Activity actually owns window focus/TOP state.
- MainActivity no longer opens a new privileged log session merely from onResume(); reconnect is armed and issued only after focused foreground confirmation.
- LogSessionBridgeActivity now waits for real window focus before asking :watcher to reopen logd, with bounded timeouts.
- Automatic bridge recovery retries up to three times per loss and resets on USER_PRESENT; notification fallback remains available.
- Added a notification action to restore log access explicitly when a session is offline.
- Reduced false direct-logd stall deaths: EOF/exception still fail immediately, while silent heartbeat stalls require three misses with a longer settle window.
- UI now labels READ_LOGS as a package permission and separately proves the live device-log session.
- No Device Admin. No Shizuku runtime dependency. Alarm/Job/overlay/START_STICKY retention paths are preserved.
