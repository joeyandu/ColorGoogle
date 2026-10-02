# SessionKeeper background fix

Android 13+ treats READ_LOGS and a live privileged log access session as separate states. READ_LOGS can remain granted while a logd reader session is gone.

A new log access request is eligible for Android's confirmation dialog only while the requesting UID is in PROCESS_STATE_TOP. `Activity.onResume()` alone is not a strong enough timing signal on OEM builds; the request is now delayed until the Activity owns window focus.

Recovery flow:

1. :watcher detects EOF/exception or a proven silent stall.
2. State becomes NEEDS_RECONNECT and the foreground notification updates.
3. If the screen is interactive, the transparent LogSessionBridgeActivity is surfaced.
4. The bridge waits for actual window focus, then asks :watcher to create the new direct-logd session.
5. Android SystemUI can then present its own mandatory device-log confirmation.
6. If an OEM blocks the bridge, recovery retries are bounded and the notification exposes a Restore log access action.

The stable background-retention mechanisms are unchanged: Doze allowlist eligibility, specialUse/systemExempted FGS, OverlayGuardian, START_STICKY, Alarm lease, JobScheduler rescue, boot/package recovery and the isolated :watcher process.
