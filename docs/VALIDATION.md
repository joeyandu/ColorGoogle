# ColorGoogle 16.3.0 validation

Date: 2026-10-02. This record separates source/build checks, observed device state
and user-operated acceptance. Private raw logs and device identifiers are not published.

## Build and source checks

- JDK 17, Android SDK 36; release build, 23 release unit tests and release Lint passed.
- Tests cover nine trigger/routing sequences, four desktop matching/debounce cases,
  six log-packet/UID cases and four bounded-recovery cases; zero failures/errors.
- App lint: 0 new errors, 97 warnings, six inherited error instances filtered by the
  checked-in baseline. ADB module lint: 0 errors, 23 warnings. This is not warning-free.
- Release sanity passed, including SPDX/audio checks, manifest/signing checks,
  generic ADB device placeholder and packaged activation-script parity with app code.
- A clean exported source tree without `keystore.properties`, `local.properties`
  or private delivery files built Debug successfully with JDK 17/SDK 36 and passed
  its 23 unit tests and Debug Lint. No project release key is needed for that build.
- The shipped release uses v3 signing, with the certificate documented in SIGNING.md.
- Reviewed publication files for personal paths/device identifiers and credentials;
  private logs, signing/pairing material and build outputs are excluded.
- macOS commands and Android activation script were executed. Windows commands were
  reviewed for PowerShell syntax/quoting; **not executed on a Windows host**.
- Fresh-install English selection was reviewed in source. Existing explicit language
  preferences take priority. No production app data was cleared to test onboarding.

## Device checks on this release

Device: OPPO Find X8 Ultra (PKJ110), Android 16, ColorOS 16.0.10.501, user 0.

- In-place installation succeeded with the original ColorGoogle signing key. Existing
  desktop-search and wireless-recovery switches, pairing, notification and READ_LOGS
  grants were retained.
- The documented PC activation script started the version-16030000 agent and verified
  foreign-UID wake-log health (`wakeActive=true`) with empty error fields.
- UI shows English desktop/wireless controls and the actual 16.3.0 version.
- After unplugged acceptance, the agent PID was unchanged, desktop redirection had
  incremented to one, wake health remained active and error fields were empty.
  Agent receipts include paired power markers, OEM voice/gesture service events
  and the blocked desktop-search start followed by a successful Google request.
  Release app logcat did not retain per-invocation entries, so exact invocation
  counts are covered by unit tests/user-visible acceptance rather than claimed
  as measured from those missing entries.
- The APK pulled back from the device is byte-identical to the release APK.
- With wireless debugging off, terminating the agent produced a disconnected state
  and the visible instruction to enable system wireless debugging.
- Re-enabling wireless debugging allowed the existing paired recovery client to
  restore the agent and wake-log health without running the PC activation script.
- With wireless debugging on, a second abrupt agent termination recovered to a new
  PID with desktop interception enabled and healthy wake logs.
- **User-operated unplugged acceptance passed:** the user confirmed power-key Gemini,
  gesture Circle to Search, desktop Google Search/Back, and screen-off OEM voice wake
  were normal after disconnecting USB. The test instructions included screen-off
  power invocation. This is user-reported visible behavior, not an inferred success
  from the Binder return value.

## Historical device evidence and limits

Earlier ColorGoogle builds on this same phone had user-confirmed screen-off Gemini,
OEM voice wake, gesture Circle to Search and desktop search/Back, including disconnected
USB tests. Those historical confirmations do not replace current-release acceptance.
The current release does not change the trigger/classifier/assistant/CTS protocols.

Wireless debugging has repeatedly been found disabled when the agent was absent.
The cause of that setting change is unknown. Recovery cannot enable it. Long idle,
reboot, force-stop, OTA, other launchers/models and all Gemini connected-app actions
are not promised by this release. Bright locked-screen power triggering was tested
historically, not independently repeated in this release's acceptance group.
Notification denial behavior is inherited from prior work and source-reviewed here;
this release does not claim a new denial/regrant device test.

## Screenshots

The README screenshots were taken from 16.3.0 and inspected before publication.
They contain app settings only, not account information, pairing codes or private logs.
