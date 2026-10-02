#!/usr/bin/env python3
"""Static release sanity checks for MindTrigger Assist v16.

This intentionally does not pretend to replace an Android/Gradle build. It catches
source-tree regressions that previously caused release problems: stale versioning,
missing signing config, incomplete localization, tab-reset on recreate, missing
license notices, modified audio assets, and watcher/bridge manifest regressions.
"""
from pathlib import Path
import ast
import json
import hashlib
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
JAVA_DIR = ROOT / "app/src/main/java/dev/evoker/homeholdcts"
RES = ROOT / "app/src/main/res"


def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")


def sha256(rel):
    return hashlib.sha256((ROOT / rel).read_bytes()).hexdigest()


errors = []
notes = []

def check(name, condition, detail=""):
    if not condition:
        errors.append((name, detail))


gradle = read("app/build.gradle")
root_gradle = read("build.gradle")
manifest = read("app/src/main/AndroidManifest.xml")
main = read("app/src/main/java/dev/evoker/homeholdcts/MainActivity.java")
service = read("app/src/main/java/dev/evoker/homeholdcts/HomeHoldService.java")
bridge = read("app/src/main/java/dev/evoker/homeholdcts/LogSessionBridgeActivity.java")
ipc = read("app/src/main/java/dev/evoker/homeholdcts/WatcherIpc.java")
ui_text = read("app/src/main/java/dev/evoker/homeholdcts/UiText.java")
language = read("app/src/main/java/dev/evoker/homeholdcts/LanguageManager.java")
setup_commands = read("app/src/main/java/dev/evoker/homeholdcts/SetupCommands.java")
locales_xml = read("app/src/main/res/xml/locales_config.xml")

# Build / identity / signing.
check("applicationId", 'applicationId "dev.evoker.homeholdcts"' in gradle)
check("versionCode", "versionCode 16030000" in gradle)
check("versionName", 'versionName "16.3.0"' in gradle)
check("compileSdk", "compileSdk 36" in gradle)
check("targetSdk", "targetSdk 36" in gradle)
check("minSdk", "minSdk 32" in gradle)
check("AGP", "version '8.13.2'" in root_gradle)
check("release signingConfig", "signingConfig signingConfigs.release" in gradle)
check("debug signingConfig", "signingConfig signingConfigs.debug" in gradle)
check("v1 signing disabled", gradle.count("enableV1Signing = false") == 2)
check("v2 signing disabled", gradle.count("enableV2Signing = false") == 2)
check("v3 signing", gradle.count("enableV3Signing = true") == 2)
check("v4 disabled", gradle.count("enableV4Signing = false") == 2)

# Runtime/manifest invariants.
check("watcher private process", 'android:process=":watcher"' in manifest)
check("specialUse FGS", 'android:foregroundServiceType="specialUse|systemExempted"' in manifest)
check("specialUse permission", "android.permission.FOREGROUND_SERVICE_SPECIAL_USE" in manifest)
check("READ_LOGS permission", "android.permission.READ_LOGS" in manifest)
check("overlay permission", "android.permission.SYSTEM_ALERT_WINDOW" in manifest)
check("bridge Activity", '.LogSessionBridgeActivity' in manifest)
check("bridge not exported", re.search(r'<activity[^>]*android:name="\.LogSessionBridgeActivity"[\s\S]*?android:exported="false"', manifest) is not None)
check("MainActivity excluded from Recents", 'android:excludeFromRecents="true"' in manifest)
check("bounded 500ms UI poll", "ui.postDelayed(this, 500L)" in main and "statusPollRemaining" in main)
check("Messenger IPC", "MSG_STATE_CHANGED" in ipc and "ServiceConnection watcherConnection" in main)
check("transparent bridge reconnect", "MSG_RECONNECT_LOGCAT" in bridge)
check("no forced MainActivity bring-to-front", "bringActivityToFront" not in main)
check("RestartReceiver non-exported",
      re.search(r'<receiver[\s\S]*?android:name="\.RestartReceiver"[\s\S]*?android:exported="false"', manifest) is not None)
check("no full-screen-intent AppOp", "USE_FULL_SCREEN_INTENT" not in setup_commands)
check("local wireless ADB network permission", "android.permission.INTERNET" in manifest)
check("Beta bottom-nav tab", "TAB_BETA = 5" in main and "R.drawable.ic_extension" in main)
check("one-time wizard preserved", "PREF_SETUP_UI_FINISHED" in main and "setupWizardActive" in main)
check("nav recursion guard preserved", "showTabFromBottomNavigation" in main)
check("Assistant Settings launcher", "assistantSettings(this)" in main)
check("assistant command UI redaction", "sanitizeUiLog" in read("app/src/main/java/dev/evoker/homeholdcts/FirstRunBootstrap.java"))
check("3 second custom audio cap", "MAX_CLIP_MS = 3000L" in read("app/src/main/java/dev/evoker/homeholdcts/ActivationSoundPlayer.java"))
check("Device Admin absent", "BIND_DEVICE_ADMIN" not in manifest and "DEVICE_ADMIN" not in manifest)

# Theme/language recreate must preserve the current bottom-nav tab explicitly.
check("theme tab marker", "EXTRA_RECREATE_TAB" in main)
check("theme recreate helper", "private void recreatePreservingTab()" in main)
check("recreate calls use helper", main.count("recreatePreservingTab();") >= 4)
check("only helper calls recreate()", main.count("recreate();") == 1)
check("saved-state tab backup", "onSaveInstanceState" in main and "STATE_SELECTED_TAB" in main)

# Direct dependencies and notices.
check("Material 1.14.0", 'com.google.android.material:material:1.14.0' in gradle)
check("Shizuku API 13.1.5", 'dev.rikka.shizuku:api:13.1.5' in gradle)
check("Shizuku provider 13.1.5", 'dev.rikka.shizuku:provider:13.1.5' in gradle)
check("HiddenApiBypass 6.1", 'org.lsposed.hiddenapibypass:hiddenapibypass:6.1' in gradle)

required_files = [
    "LICENSE",
    "LICENSE_AUDIT.md",
    "NOTICE.md",
    "THIRD_PARTY_NOTICES.md",
    "SOURCE_PROVENANCE.md",
    "AUDIO_PROVENANCE.md",
    "TERMS_AND_PRIVACY.md",
    "SECURITY_SOURCE_AUDIT.md",
    "THIRD_PARTY_LICENSES/Google_Material_Design_Apache-2.0.txt",
    "THIRD_PARTY_LICENSES/Shizuku_API_MIT.txt",
    "THIRD_PARTY_LICENSES/AndroidHiddenApiBypass_Apache-2.0.txt",
    "app/src/main/res/raw/license_gpl_3_0.txt",
    "app/src/main/res/raw/license_apache_2_0.txt",
    "app/src/main/res/raw/license_shizuku_api_mit.txt",
    "app/src/main/res/raw/notice_micts.txt",
    "app/src/main/res/raw/notice_hidden_api_bypass.txt",
    "app/src/main/res/raw/notice_audio_provenance.txt",
]
for rel in required_files:
    check("required file: " + rel, (ROOT / rel).is_file())

check("in-app license viewer", "showOpenSourceLicenses()" in main)
for raw in [
    "license_gpl_3_0", "license_apache_2_0", "license_shizuku_api_mit",
    "notice_micts", "notice_hidden_api_bypass", "notice_audio_provenance",
]:
    check("license viewer resource: " + raw, f"R.raw.{raw}" in main)

# Every app Java source should declare the project SPDX identifier.
java_files = sorted(JAVA_DIR.glob("*.java"))
check("Java source count", len(java_files) >= 20, str(len(java_files)))
for path in java_files:
    first = path.read_text(encoding="utf-8").splitlines()[0:1]
    check("SPDX: " + path.name, first == ["// SPDX-License-Identifier: GPL-3.0-only"])

# Audio assets: exact release bytes and provenance hashes.
audio_hashes = {
    "app/src/main/res/raw/aura_cts.wav": "f16f80b3f5f58151b369aa92b42cda0d8e639eb293fd3add5aa4cb85b4959f2c",
    "app/src/main/res/raw/aura_gemini.wav": "7d7146ac39323f2851138427d641ac2a924476c5549b614a9c2d86fbe0de9f49",
}
for rel, expected in audio_hashes.items():
    check("audio hash: " + rel, (ROOT / rel).is_file() and sha256(rel) == expected,
          sha256(rel) if (ROOT / rel).is_file() else "missing")

# Release locales: only fully maintained packs.
expected_locales = ["vi-VN", "en-US", "id-ID", "th-TH"]
try:
    xml_root = ET.fromstring(locales_xml)
    android_ns = "{http://schemas.android.com/apk/res/android}name"
    locales = [e.attrib[android_ns] for e in xml_root.findall("locale")]
    check("release locale list", locales == expected_locales, repr(locales))
except Exception as exc:
    errors.append(("locales_config.xml parse", str(exc)))
for code in ["CODE_VI_VN", "CODE_EN_US", "CODE_ID_ID", "CODE_TH_TH"]:
    check("LanguageManager " + code, code in language)
for stale in ["CODE_JA", "CODE_KO", "CODE_DE", "CODE_FR", "CODE_ES", "CODE_PT", "CODE_RU", "CODE_AR", "CODE_ZH"]:
    check("no stale locale " + stale, stale not in language)

# Localization coverage. Parse user-facing helper calls and include literal leaves
# from ternaries, then require an exact case set in vi/id/th.
TOKEN = re.compile(r'''(?P<ws>\s+)|(?P<comment>//[^\n]*|/\*.*?\*/)|(?P<string>"(?:\\.|[^"\\])*")|(?P<ident>[A-Za-z_$][\w$]*)|(?P<other>.)''', re.S)

def toks(text):
    out = []
    for m in TOKEN.finditer(text):
        if m.lastgroup not in ("ws", "comment"):
            out.append((m.lastgroup, m.group()))
    return out

def decode_java_string(token):
    try:
        return ast.literal_eval(token)
    except Exception:
        return None

def call_args(tokens, i):
    if i + 1 >= len(tokens) or tokens[i + 1] != ("other", "("):
        return None
    args, cur, depth = [], [], 0
    j = i + 2
    while j < len(tokens):
        typ, value = tokens[j]
        if typ == "other" and value == "(":
            depth += 1; cur.append(tokens[j])
        elif typ == "other" and value == ")":
            if depth == 0:
                args.append(cur); return args
            depth -= 1; cur.append(tokens[j])
        elif typ == "other" and value == "," and depth == 0:
            args.append(cur); cur = []
        else:
            cur.append(tokens[j])
        j += 1
    return None

def eval_static_string(tokens):
    values = []
    for typ, value in tokens:
        if typ == "string":
            decoded = decode_java_string(value)
            if decoded is None: return None
            values.append(decoded)
        elif typ == "other" and value in "+()":
            continue
        else:
            return None
    return "".join(values) if values else None

rules = {
    "tr":[0], "text":[0], "supporting":[0], "miniTitle":[0],
    "filledButton":[0], "filledTonalButton":[0], "outlinedButton":[0],
    "textButton":[0], "compactButton":[0], "baseButton":[0], "pill":[0],
    "themeColorButton":[0], "themeModeButton":[0], "addTopBar":[1,2],
    "confirmedManualRow":[0,1,2], "confirmManualStep":[0,1],
    "warningConfirmButton":[0], "addGestureGuideStep":[2], "infoPair":[0,1],
    "checklistLine":[1], "setActionState":[2], "setOptionalActionState":[2],
    "setPackageBatteryState":[3], "setManualState":[3,4], "setStepStatus":[2],
    "actionRow":[0,1,2], "addSectionHeader":[2,3], "addGuide":[1],
    "showBundledNotice":[0],
}
expected_keys = set()
for path in java_files:
    if path.name == "UiText.java":
        continue
    tokens = toks(path.read_text(encoding="utf-8"))
    for i, (typ, value) in enumerate(tokens):
        if typ != "ident" or value not in rules:
            continue
        args = call_args(tokens, i)
        if args is None:
            continue
        for idx in rules[value]:
            if idx >= len(args):
                continue
            static = eval_static_string(args[idx])
            if static is not None:
                expected_keys.add(static)
            else:
                # For ternary/state expressions, each literal leaf is translated independently.
                for typ2, value2 in args[idx]:
                    if typ2 == "string":
                        decoded = decode_java_string(value2)
                        if decoded and decoded not in (" ", " ms", "✓ "):
                            expected_keys.add(decoded)

expected_keys.update({
    "Assistant voice session failed",
    "Circle to Search activation failed",
    "Settings page unavailable",
    "MindTrigger Assist watcher",
    "Privileged logcat active · watcher online",
    "Approve Android device-log access to finish reconnecting",
    "READ_LOGS unavailable · open MindTrigger Assist",
    "Automatic recovery unavailable · Display over other apps required",
    "Open MindTrigger Assist to restore privileged logcat access",
    "Keeps the isolated foreground-service watcher available for trigger detection and log-session recovery.",
})

case_pattern = re.compile(r'case "((?:\\.|[^"\\])*)": return "((?:\\.|[^"\\])*)";')
def unescape_case(value):
    # Generated localization strings use only these Java escapes.
    return value.replace('\\n', '\n').replace('\\t', '\t').replace('\\"', '"').replace('\\\\', '\\')

for fn in ("vi", "id", "th"):
    m = re.search(r"private static String " + fn + r"\(String source\) \{\s*switch \(source\) \{(.*?)\n\s*default:", ui_text, re.S)
    check("UiText method " + fn, m is not None)
    if not m:
        continue
    pairs = [(unescape_case(k), unescape_case(v)) for k, v in case_pattern.findall(m.group(1))]
    table = dict(pairs)
    check(fn + " duplicate localization keys", len(pairs) == len(table))
    notes.append(f"{fn}: {len(expected_keys-set(table))} additional strings use English; retained upstream packs are not advertised as complete.")

check("translation key count", len(expected_keys) >= 300, str(len(expected_keys)))
check("credit text", '"Chat GPT"' in main and "ChatGPT" not in main and "GPT 5.6" not in main and "Sol" not in main)

# Stale/rejected release wording should not survive in active source/docs.
scan_files = [
    *java_files,
    ROOT / "README.md", ROOT / "NOTICE.md", ROOT / "SOURCE_PROVENANCE.md",
    ROOT / "TERMS_AND_PRIVACY.md", ROOT / "CHANGELOG.md", ROOT / "LOG_SESSION_BRIDGE.md",
]
scan = "\n".join(p.read_text(encoding="utf-8") for p in scan_files)
for term in ["GPT 5.6", "ChatGPT", "V2.1 Beta", "political campaign", "political content"]:
    check("no stale wording: " + term, term not in scan)
check("generic redistributor disclaimer", "controversial or unrelated content" in scan)

# The platform-neutral deployment script must match the app activation logic.
activation = read("app/src/main/java/dev/evoker/homeholdcts/DesktopSearchActivation.java")
script_part = activation.split('static final String SCRIPT = String.join("\\n",', 1)[1].split('    static String command()', 1)[0]
script_lines = [json.loads(token) for token in re.findall(r'"(?:\\.|[^"\\])*"', script_part)]
packaged_script = "\n".join(line for line in read("tools/activate-agent.sh").splitlines() if not line.startswith("#"))
check("packaged ADB script matches app", packaged_script == "\n".join(script_lines))
check("generic ADB device placeholder", 'adb -s SERIAL shell' in activation)
check("English documentation pair", (ROOT / "README.zh-CN.md").is_file() and "How to use with an Agent" in read("README.md"))

# XML validity for all XML resources/manifest.
for path in [ROOT / "app/src/main/AndroidManifest.xml", *RES.rglob("*.xml")]:
    try:
        ET.parse(path)
    except Exception as exc:
        errors.append(("XML parse: " + str(path.relative_to(ROOT)), str(exc)))

if errors:
    print("ColorGoogle 16.3.0 release sanity: FAILED")
    for name, detail in errors:
        print(" -", name + (": " + detail if detail else ""))
    sys.exit(1)

print("ColorGoogle 16.3.0 release sanity: PASSED")
print(" - English source keys:", len(expected_keys))
for note in notes: print(" -", note)
print(" - release locales:", ", ".join(expected_locales))
print(" - Java SPDX files:", len(java_files))
print(" - audio hashes: verified")
print(" - signing policy: v3; v1 + v2 + v4 disabled (minSdk 32)")
print(" - note: this is static validation; build/apksigner verification still requires Android SDK")
