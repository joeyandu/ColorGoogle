# Target SDK 36 audit

This project targets Android 16 / API 36.

## Build toolchain

`compileSdk 36` requires Android Gradle Plugin 8.9.1 or newer.

MindTrigger Assist uses:

- AGP: 8.9.1
- Java: 17
- compileSdk: 36
- targetSdk: 36
- minSdk: 32

Android Studio should use a Gradle version compatible with AGP 8.9.1
(Gradle 8.11.1 is the AGP 8.9 baseline).

## Foreground service

Android 14+ requires every foreground service in an app targeting API 34+
to declare an appropriate foreground-service type.

`HomeHoldService` is declared as:

- foreground service type: `specialUse`
- permission: `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`
- subtype: persistent local device-log trigger watcher for user-enabled
  assistant shortcuts

The foreground-service declaration is build/platform metadata; the v16 runtime watcher and Log Session Bridge behavior is documented separately in `LOG_SESSION_BRIDGE.md`.

## Background restart caveat

Android 12+ restricts foreground-service starts from the background.
Android 15+ also narrows the SYSTEM_ALERT_WINDOW exemption: the overlay must
actually be visible when relying on that exemption.

BOOT_COMPLETED remains a platform-defined background-start exemption, but
Android 15 restricts some FGS types. `specialUse` is not in the prohibited
BOOT_COMPLETED type list.

The existing ColorOS survival design still relies on the user completing the
background/overlay/battery setup exposed by the application.
