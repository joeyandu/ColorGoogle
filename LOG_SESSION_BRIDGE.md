# Automatic Log Session Bridge — v16

When the isolated `:watcher` process detects that a previously active privileged
logcat session has died, it makes one best-effort attempt to start
`LogSessionBridgeActivity`.

The bridge:

- is transparent;
- is not exported;
- is excluded from Recents;
- uses a separate task affinity;
- has no history;
- never renders MainActivity;
- binds to `HomeHoldService` and sends one `MSG_RECONNECT_LOGCAT` request;
- disappears when Android takes over with its mandatory device-log dialog.

The feature still requires the user-granted `SYSTEM_ALERT_WINDOW` capability
used by the existing watcher overlay. Android/OEM background-activity policy can
still block the best-effort Activity launch, in which case the watcher
notification remains the fallback.
