# ColorGoogle security review scope

For current architecture and component boundaries see docs/ARCHITECTURE.md.
The inherited audit below applies to upstream v16 only: its statement about
absence of INTERNET/socket clients does **not** apply to ColorGoogle .6/16.3.0,
which adds loopback wireless ADB and its dependencies. This is not a security
certification or penetration test. Release validation is in docs/VALIDATION.md.

---

# MindTrigger Assist v16 — source/security audit

Audit date: 2026-08-18

This is a static source review of the v16 release candidate. It is not a
penetration test and does not replace testing the built APK on-device.

## Network / telemetry

The application manifest does **not** request `android.permission.INTERNET`.
No HTTP client, socket client, analytics SDK, crash-reporting SDK, advertising
SDK, or developer-operated telemetry endpoint is present in the app source.

The only project/support URLs in Java source are opened through an external
browser Intent (GitHub and Ko-fi); MindTrigger Assist itself does not perform
network transport for those links.

## Privileged local operations

The privileged setup can intentionally:

- grant the app `READ_LOGS`;
- set Google as the default Android assistant / voice-interaction service;
- apply device-idle / standby / background AppOps settings to selected local
  packages;
- on the Shizuku first-run path, remove `com.heytap.speechassist` and
  `com.coloros.colordirectservice` from **user 0** with
  `pm uninstall --user 0`.

The system-partition APKs are not erased by `pm uninstall --user 0`.
The unrelated `USE_FULL_SCREEN_INTENT` AppOp previously present in the setup
list was removed in v16 because MindTrigger Assist does not use a full-screen
intent path.

## Component exposure

- `MainActivity`: exported intentionally because it is the launcher Activity.
- `LogSessionBridgeActivity`: not exported.
- `HomeHoldService`: not exported and runs in the isolated app process suffix
  `:watcher` (same app UID).
- `RestartReceiver`: now not exported. It still receives system broadcasts and
  explicit same-app restart alarms without allowing arbitrary third-party apps
  to invoke the internal restart path.
- ShizukuProvider: exported as required by the Shizuku API integration and is
  protected by its declared Android permission.

## Local data

Configuration/state is stored in app-private `SharedPreferences`. The manifest
sets `android:allowBackup="false"`.

The privileged logcat reader launches the platform `logcat` executable with a
narrow tag filter. Log events are evaluated locally for trigger detection; the
source contains no upload path for those logs.

## Static review limits

The review can establish what is present in this source tree. It cannot prove
how an OEM-modified Android build will behave at runtime, and it does not audit
undeclared transitive code inside a future built APK. A final release APK should
still be verified with Gradle dependency reports and `apksigner` after build.
