# ColorGoogle — Privacy and permission notice

ColorGoogle adds a shell-agent log path, desktop interception and optional local
wireless ADB recovery to MindTrigger Assist. There is no developer telemetry
backend. INTERNET and network-state permissions are used for local wireless ADB;
NSD discovers local endpoints and the client connects only to loopback. Pairing
keys live in private no-backup storage. No microphone permission is requested;
OEM software performs voice wake. Diagnostic shell logs may contain device
details and should be reviewed before sharing.

The foreground watcher and actual shell-agent/log-session states are distinct.
READ_LOGS does not guarantee a live agent. Recovery cannot enable disabled
wireless debugging and cannot run while the app is force-stopped.

The inherited app-UID log bridge described below remains available, but the
ADB-selected path reads through shell after explicit activation. See docs/ADB.md
for the minimal recommended route and restoration; inherited one-shot setup
contains broader system-wide changes.

## Inherited upstream notice

MindTrigger Assist itself does not collect, store, sell, or upload personal data,
analytics, telemetry, screenshots, queries, or system logs to a
developer-operated server.

The app reads selected local Android/ColorOS log events only to detect the
configured long-press trigger. Those events are processed locally by the
watcher.

`READ_LOGS` is a package-level permission retained across reboot. Privileged
logcat access is session-scoped. When a session must be recreated, Android may
require its own device-log access confirmation. MindTrigger Assist does not
bypass that system confirmation.

When Circle to Search or an Assistant session is invoked, Google software and
services may process screen context, account information, queries, or other data
under Google's own terms and privacy policies. That processing is outside
MindTrigger Assist.

Shizuku setup changes local device state. On the first-run path the app can:

- grant `READ_LOGS`;
- set the configured Google voice-interaction service as the Android assistant;
- apply the listed Doze / standby / AppOps background settings;
- run `pm uninstall --user 0 com.heytap.speechassist`;
- run `pm uninstall --user 0 com.coloros.colordirectservice`.

The two `pm uninstall --user 0` operations remove those packages from user 0;
they do not erase the system-partition APKs.

The watcher runs in the private `:watcher` process. `Display over other apps` is
used by the non-interactive watcher overlay and transparent Log Session Bridge.
If an active privileged logcat session is lost, Android may present its own
access confirmation; the bridge does not bypass it.

MindTrigger Assist is an unofficial compatibility utility. ColorOS or Google
updates can change these behaviors. Review privileged setup commands before
applying them.

## Redistribution and added-content notice

Redistribution must comply with GPL-3.0-only and applicable copyright, notice,
and corresponding-source requirements.

Content, branding, commentary, screenshots, links, or other material added by a
redistributor is solely that redistributor's responsibility and does not
represent the original MindTrigger Assist author. The original author assumes no
responsibility for such added material, including controversial or unrelated
content.

This statement is informational only. It does not limit the permissions or add a
field-of-use restriction to rights granted by GPL-3.0-only.

## No warranty

The project is provided subject to the warranty/liability terms of GPLv3. This
notice does not replace or modify the license text in `LICENSE`.
