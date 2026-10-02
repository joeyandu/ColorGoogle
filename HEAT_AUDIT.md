# Runtime heat audit — R5.2 No Device Admin

This audit is source-derived and intentionally does not change background-survival behavior.

## Findings

1. **Device Admin is not a meaningful continuous heat source.**
   The previous implementation only queried `DevicePolicyManager.isAdminActive()` during foreground-service eligibility/bootstrap checks. It did not own a polling loop, thread, alarm, sensor, network connection, or WakeLock.

2. **Highest continuous CPU candidate: DirectLogdReader.**
   While MindTrigger is enabled, `:watcher` keeps one in-process `SOCK_SEQPACKET` connection to Android `logdr`. The reader subscribes to text buffers `main`, `radio`, `system`, and `crash`, reads packets continuously, parses the tag, and only then drops irrelevant entries. On a device with a high log rate this can keep the watcher CPU active even when no MindTrigger trigger occurs.

3. **No permanent app WakeLock exists in the runtime source.**
   The project configures the `WAKE_LOCK` AppOp during one-shot setup, but the Java runtime contains no `PowerManager.WakeLock.acquire()` path.

4. **Keepalive timers are low-rate compared with the log stream.**
   - Direct-logd heartbeat: every 30 seconds.
   - OverlayGuardian attachment sanity: every 60 seconds.
   - Alarm lease renewal: every 2 minutes; lease timeout: 4 minutes.
   - Persisted rescue JobScheduler job: every 15 minutes.
   - Screen transitions reassert the overlay/FGS on SCREEN_OFF, SCREEN_ON and USER_PRESENT.

5. **No keepalive interval or trigger/runtime path was changed in R5.2.**
   The audit identifies candidates only. DirectLogd, OverlayGuardian, START_STICKY, Alarm lease, JobScheduler rescue, boot/package recovery, classifier and activation behavior remain as in R5.1.

## R5.2 Device Admin removal

- Removed `.MindTriggerAdminReceiver` from the manifest.
- Removed `android.permission.BIND_DEVICE_ADMIN` receiver declaration.
- Removed `app/src/main/res/xml/device_admin.xml`.
- Removed `dpm set-active-admin` from one-shot setup commands.
- Bootstrap readiness now uses the existing self Doze allowlist instead of requiring Device Admin.
- `systemExempted` FGS eligibility now uses `PowerManager.isIgnoringBatteryOptimizations(getPackageName())`; `specialUse` fallback is unchanged.

This keeps the intended one-shot Doze whitelist/background configuration while eliminating the Device Admin role.
