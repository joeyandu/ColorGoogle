# Architecture and modification history

ColorGoogle 16.3.0 is based on MindTrigger Assist v16.2.0. It retains the package
`dev.evoker.homeholdcts`, the upstream watcher, assistant/CTS invocation protocol,
permission model and GPL provenance. It is not a clean-room reimplementation.

## Trigger path

1. An explicitly activated `app_process` agent runs as Android shell (UID 2000).
2. Its direct logd reader checks foreign-UID packets and forwards only recognized
   power/gesture/voice markers through the protected wake bridge to `:watcher`.
   Own-UID heartbeats do not establish privileged log health. Unrelated packet
   text is discarded before forwarding.
3. `TriggerClassifier` accepts direct power-key markers immediately. The 2.5-second
   cooldown is measured from accepted events, not extended by duplicates. Paired
   power markers and their speech-service tail cannot each schedule an invocation.
4. The existing activation code opens the configured Google assistant session or
   the inherited MiCTS-derived CTS path. OEM voice routing and swap options retain
   their previous meaning. No prompt-prefix or external launcher API was added.

The Google bridge uses hidden voice-interaction Binder APIs. A returned call does
not prove a visible Gemini/CTS UI; those are separate acceptance boundaries.

## Desktop search

A Beta opt-in uses `IActivityController`. It matches user-0 launcher UID, the exact
ColorOS `com.heytap.quicksearchbox.ui.activity.SearchHomeActivity` component,
`android.intent.action.VIEW`, and `gs://search/gsearch?source=dock`. It cancels that
start before launching Google Search on a worker. Enabling also removes matching
old SearchHomeActivity tasks so Back does not reveal a stale search page.
The original package remains installed for intent resolution. The switch controls
interception independently of wake-log monitoring.

The controller is a shared hidden system slot with no atomic compare-and-set.
Do not combine with other activity-monitoring tools. A dead controller is replaced
only when its retained Binder fails ping and the recorded system identity matches;
unknown ownership produces an error. This is not a universal launcher interceptor.

## Local wireless recovery

Optional recovery runs in the foreground watcher, separately from the shell agent.
It uses the included LibADB 3.1.1-derived module and locally generated pairing key.
NSD results must resolve to a local device address; connection is loopback-only.
Only the fixed activation command is used. No arbitrary-command RPC or inbound
network listener is exposed. Keys live in private no-backup storage.

Binder death triggers bounded attempts (maximum three per endpoint/outage). A new
endpoint/network or explicit retry can rearm the budget. A file lock avoids duplicate
agents. Agent cleanup releases owned resources and explicitly exits even if an
individual cleanup operation fails. Recovery requires a running watcher, connected
Wi-Fi, enabled wireless debugging and valid pairing. It cannot enable wireless
debugging, survive force-stop as a guarantee, or bypass device unlock.

## Boundaries and permissions

- Wake/desktop providers require Android `DUMP` and explicitly check shell UID 2000.
- Agent RPC restricts callers to ColorGoogle or shell; pairing IPC is internal,
  non-exported and same-UID checked.
- `INTERNET`/network state permissions support local wireless ADB, not a telemetry backend.
- `READ_LOGS`, an active log session and a running privileged agent are different states.
- Notification denial is displayed separately from service/log health.
- Upstream Shizuku integration remains a setup route; it does not manage this agent.

## Earlier ColorGoogle increments

| Private version | Changes |
|---|---|
| 16.2.0-colorgoogle.1 | Direct power-trigger activation, event deduplication, notification permission UX, ColorGoogle naming and artwork, zero-delay default |
| .2 | User-supplied four-color G/green-ribbon artwork |
| .3 | More icon whitespace, adaptive/legacy/in-app resources; trigger behavior unchanged |
| .4 | Beta desktop-search interception, shell activation and cleanup of old search tasks |
| .5 | Shared shell-agent wake reader, foreign-UID health verification, honest disconnected states |
| .6 | Optional phone-local wireless pairing/recovery, bounded retries, cleanup and quiet-start health fixes |
| 16.3.0 | Public distribution, English default/new-feature text, generic activation command, bilingual documentation and release validation |

See [CHANGELOG](../CHANGELOG.md) and [validation](VALIDATION.md). Public documentation
must not turn historical device evidence into an unqualified cross-device guarantee.
