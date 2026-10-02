# V16.1 Beta branch note

Baseline lineage remains V5 StableUI R5.2 NoAdmin. V16.1 intentionally changes only requested Beta/UI/action-routing surfaces. Background-retention mechanisms are not weakened or retimed.

Unchanged keepalive/runtime infrastructure from R5.2:

- `DirectLogdReader.java`
- `SingleDirectLogdSession.java`
- `OverlayGuardian.java`
- `RestartReceiver.java`
- `WatcherRescueJobService.java`
- `LogSessionBridgeActivity.java`
- Alarm lease / persisted JobScheduler cadence
- START_STICKY / boot / package-replaced recovery

V16.1 requested runtime deltas:

- `HomeHoldService.java`: one additional synchronized preference for CTS/Assistant swapping and only the final target-selection branch changed.
- `WatcherIpc.java`: one Boolean preference key for swap state.
- `ActivationSoundPlayer.java`: custom local clip import/playback with a 3-second cap.
- `FirstRunBootstrap.java`: UI log redaction for internal Assistant secure-setting commands; command execution itself is unchanged.
- `SettingsNavigator.java`: Assistant Settings navigation helper.
- `MainActivity.java`: Beta tab, animations, sound pickers, voice-wake relocation, swap UI, alignment polish.

Device Admin remains absent. Shizuku remains one-shot setup only.

---

# Stable V5 UI Rebase

Baseline: `MindTriggerAssist-EvokerUniverse-KeepAlive-V5-Over.zip`.

This branch intentionally changes UI/setup only. The following runtime/core files are byte-identical to the V5 baseline:

- `HomeHoldService.java`
- `DirectLogdReader.java`
- `SingleDirectLogdSession.java`
- `OverlayGuardian.java`
- `RestartReceiver.java`
- `WatcherRescueJobService.java`
- `ActivationRunner.java`
- `FirstRunBootstrap.java`
- `SetupCommands.java`

UI changes:

- Full-screen Android/OOBE-style 5-page setup flow.
- No Close button and no app BottomNavigationView while setup is open.
- Shizuku/privileged bootstrap is page 1.
- Explicit visible feedback for Shizuku and copied ADB actions.
- Page index is committed synchronously and restored after process recreation.
- Google/Gemini battery state still uses V5's live `PowerManager.isIgnoringBatteryOptimizations(package)` check.
- Final `Finish & run` uses the original V5 `runWatcherWithGate()` and then returns to the normal app UI.
- `android:forceDarkAllowed=false` added to prevent system dark mode from force-darkening White mode.


## R5.1 UI correction

R5.1 keeps the R4 bottom navigation. The onboarding wizard is full-screen only while `stable_ui_setup_finished` is false. Once finished, the Setup destination becomes a normal scrollable Setup & Health tab and the wizard cannot be reopened by bottom navigation or live-state verification. The post-onboarding Setup tab reuses the same action/verifier builders as the wizard; runtime core files are unchanged from R4.


## R5.2 Device Admin removal

R5.2 removes the Device Admin endpoint and activation command. The existing one-shot self Doze allowlist remains the eligibility source for `systemExempted`; `specialUse` fallback, DirectLogd, OverlayGuardian, START_STICKY, Alarm lease, persisted JobScheduler rescue, boot/package recovery, classifiers and activation paths are unchanged.

### v16.1 beta2 SessionKeeper
This release changes log-session recovery only. The keepalive topology is not weakened. New logd requests are TOP/focus-gated, false heartbeat stall detection is less aggressive, and recovery has bounded retries plus a notification action.
