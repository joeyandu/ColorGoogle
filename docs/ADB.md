# ADB setup and recovery / ADB 设置与恢复

[English README](../README.md) · [中文 README](../README.zh-CN.md)

Use a **selected physical device**, not an emulator. Replace `SERIAL`. These examples target **Android user 0**, the only tested configuration. Confirm `adb -s SERIAL shell am get-current-user` returns `0`; stop on another user. Before changing anything, check Google is installed and initialized. Read commands before running them.

选择实体手机并替换 `SERIAL`。示例只适用于已测试的 **Android 用户 0**；先检查当前用户，其他用户停止操作。先确认 Google 已安装并完成首次设置。不要把每段都当作必须执行的一键命令。

## 1. Save original values / 保存原值

In macOS Terminal, save privately (not in a public issue):

```bash
mkdir -p colorgoogle-backup
adb -s SERIAL shell am get-current-user
adb -s SERIAL shell pm path com.google.android.googlequicksearchbox
adb -s SERIAL shell settings --user 0 get secure assistant > colorgoogle-backup/assistant.txt
adb -s SERIAL shell settings --user 0 get secure voice_interaction_service > colorgoogle-backup/voice-service.txt
adb -s SERIAL shell dumpsys package dev.evoker.homeholdcts > colorgoogle-backup/package.txt
adb -s SERIAL shell dumpsys deviceidle whitelist > colorgoogle-backup/whitelist.txt
```

Windows PowerShell:

```powershell
$adb = "$env:USERPROFILE\Downloads\platform-tools\adb.exe"
New-Item -ItemType Directory -Force .\colorgoogle-backup | Out-Null
& $adb -s SERIAL shell am get-current-user
& $adb -s SERIAL shell pm path com.google.android.googlequicksearchbox
& $adb -s SERIAL shell settings --user 0 get secure assistant | Set-Content .\colorgoogle-backup\assistant.txt
& $adb -s SERIAL shell settings --user 0 get secure voice_interaction_service | Set-Content .\colorgoogle-backup\voice-service.txt
& $adb -s SERIAL shell dumpsys package dev.evoker.homeholdcts | Set-Content .\colorgoogle-backup\package.txt
& $adb -s SERIAL shell dumpsys deviceidle whitelist | Set-Content .\colorgoogle-backup\whitelist.txt
```

Read exit/error output after **each** command; do not continue after a failure. Record whether `android.permission.READ_LOGS` was already `granted=true` in the package dump. Do not overwrite the backup when reactivating later.

每条命令后检查结果，失败就停止。记录原来 READ_LOGS 是否已授权，重新激活时不要覆盖首次备份。

## 2. Basic setup / 基础配置

The `adb` commands below work in a macOS terminal. In PowerShell, replace their `adb` prefix with `& $adb` (defined above). Arguments here deliberately do not use Bash variables or multiline shell quoting.

下列命令在 Mac 终端执行；PowerShell 将开头的 `adb` 换成 `& $adb`，其余参数相同。

```bash
adb -s SERIAL shell pm grant dev.evoker.homeholdcts android.permission.READ_LOGS
adb -s SERIAL shell settings --user 0 put secure assistant com.google.android.googlequicksearchbox/com.google.android.voiceinteraction.GsaVoiceInteractionService
adb -s SERIAL shell settings --user 0 put secure voice_interaction_service com.google.android.googlequicksearchbox/com.google.android.voiceinteraction.GsaVoiceInteractionService
adb -s SERIAL shell settings --user 0 get secure assistant
adb -s SERIAL shell settings --user 0 get secure voice_interaction_service
adb -s SERIAL shell dumpsys package dev.evoker.homeholdcts
```

The final reads must reflect the Google service and granted permission. Open ColorGoogle and complete phone-side notification, overlay and main-service setup. Set Gemini in Google's own assistant settings. Then use the **agent activation script in the README**. These settings alone cannot create a live shell agent or prove a functioning log session.

确认读取结果为 Google 服务，权限已授予。打开 ColorGoogle 完成手机端权限和主服务设置，在 Google 内选择 Gemini，然后执行 **README 的代理启动脚本**。静态配置不代表代理运行或日志会话正常。

## 3. App-level retention, optional / 可选应用级保活

Prefer the phone's app-specific background/autostart controls first. If necessary, add these **installed** apps to Android's Doze whitelist after saving the list:

优先使用系统应用级后台、自启动设置。确有需要时，先保存白名单再对已安装应用执行：

```bash
adb -s SERIAL shell dumpsys deviceidle whitelist +dev.evoker.homeholdcts
adb -s SERIAL shell dumpsys deviceidle whitelist +com.google.android.googlequicksearchbox
adb -s SERIAL shell dumpsys deviceidle whitelist +com.google.android.apps.bard
adb -s SERIAL shell dumpsys deviceidle whitelist
```

Read back and confirm additions; missing-package failures are not successes. Exemptions can increase background battery use and do not guarantee survival. To restore, remove **only entries you added that were absent before**, e.g. `adb -s SERIAL shell dumpsys deviceidle whitelist -dev.evoker.homeholdcts`. Preserve pre-existing entries. Restore phone UI toggles to your recorded choices.

白名单可能增加后台耗电，不保证永不退出。恢复时只移除本次新增且原来不存在的条目，不能清空全部白名单；手机界面开关按原记录恢复。

## 4. Broader inherited setup / 上游更广泛的一键设置

The app retains upstream one-shot PC/Shizuku setup. It can change global phantom-process monitoring and cached-app freezer behavior, ColorOS power/freeze switches, and background AppOps for ColorGoogle/Google/Gemini/Google services. The Shizuku preparation route can additionally uninstall competing OEM components for user 0. **It is not the minimal recommended route above.** Inspect [SetupCommands.java](../app/src/main/java/dev/evoker/homeholdcts/SetupCommands.java) before choosing it.

应用保留上游 PC/Shizuku 一键设置，会修改系统进程监控、缓存冻结、ColorOS 省电/冻结开关和多个应用的后台 AppOps；Shizuku 准备步骤还可能移除用户 0 的小布组件。它与上面的基础流程不同，执行前必须了解范围。

Record the following before broader changes; keep output private:

| Namespace | Key |
|---|---|
| global | `settings_enable_monitor_phantom_procs`, `cached_apps_freezer` |
| secure (user 0) | `app_disable_switch`, `auto_switch`, `auto_frozen_time`, `app_frozen_switch_close` |
| system | `auto_power_protect_state` |

For each use `adb -s SERIAL shell settings --user 0 get NAMESPACE KEY`. Restore with `settings --user 0 put NAMESPACE KEY ORIGINAL_VALUE`; if the original read was `null`, use `settings --user 0 delete NAMESPACE KEY`. For each affected package also record `cmd appops get PACKAGE`, `am get-inactive PACKAGE`, `am get-standby-bucket PACKAGE` and the whitelist. Restore only changed AppOps individually with `cmd appops set PACKAGE OP ORIGINAL_MODE` (`default` when originally unset), not a blanket AppOps reset. Restore standby/inactive values individually. Never overwrite another app's settings or the entire OEM freeze list.

每项保存原值，原值为 `null` 表示应删除后来写入的键，而不是写入字符串 null。AppOps 逐项恢复原模式，不做全局 reset；保留其他应用的值和 OEM 冻结列表。**不要额外关闭开发者选项中的“系统优化”。**

## OEM conflicts (optional)

On the tested firmware, competing UI components were removed from user 0 while `com.oplus.ovoicemanager` was retained for “小布小布”. This is **optional and firmware-specific**, not a requirement for every phone. First save enabled/installed state and test whether there is a conflict. Removing user packages may lose data; the following uses `-k` to request retaining their data, but is not a backup guarantee. Explain the loss of XiaoBu voice UI/screen recognition before proceeding.

测试固件曾移除小布界面组件，并保留 `com.oplus.ovoicemanager` 用于语音唤醒。这是可选的机型相关处理；先记录安装/启用状态、确认冲突并说明小布语音界面和识屏功能将停止，再决定。`-k` 请求保留数据，但不能替代备份。

```bash
adb -s SERIAL shell dumpsys package com.heytap.speechassist
adb -s SERIAL shell dumpsys package com.coloros.colordirectservice
# Only after the user chooses to remove these conflicting components:
adb -s SERIAL shell pm uninstall -k --user 0 com.heytap.speechassist
adb -s SERIAL shell pm uninstall -k --user 0 com.coloros.colordirectservice
```

If they were originally installed and enabled, restore with:

```bash
adb -s SERIAL shell cmd package install-existing --user 0 com.heytap.speechassist
adb -s SERIAL shell pm enable --user 0 com.heytap.speechassist
adb -s SERIAL shell cmd package install-existing --user 0 com.coloros.colordirectservice
adb -s SERIAL shell pm enable --user 0 com.coloros.colordirectservice
```

Restore their original enabled state instead if it differed. Do **not** remove `com.oplus.ovoicemanager` for OEM voice wake, or `com.heytap.quicksearchbox` for launcher interception. Do not install unknown replacement system packages.

如果原来未启用，恢复原状态而不是强制启用。不要为了本项目移除上述语音唤醒管理器或系统全局搜索包，也不要安装未知来源的系统替代包。

## 5. Restore basic changes / 恢复基础配置

First disable automatic recovery in Beta, disable desktop redirection, and turn off the main service. Stop the remaining agent:

先关闭 Beta 自恢复、桌面搜索转向和主服务，再停止代理：

```bash
adb -s SERIAL shell content call --uri content://dev.evoker.homeholdcts.desktopsearch --method stop
```

Restore `assistant` and `voice_interaction_service` from your saved files. For each non-null original value:

```bash
adb -s SERIAL shell settings --user 0 put secure assistant ORIGINAL_ASSISTANT_COMPONENT
adb -s SERIAL shell settings --user 0 put secure voice_interaction_service ORIGINAL_VOICE_COMPONENT
```

If the original value was `null`, use `settings --user 0 delete secure assistant` or `settings --user 0 delete secure voice_interaction_service` instead. Only if READ_LOGS was absent before, revoke it with `adb -s SERIAL shell pm revoke dev.evoker.homeholdcts android.permission.READ_LOGS`. Remove only newly added whitelist entries and restore any optional changes separately. Re-read values to verify restoration.

按备份恢复两个助手设置；原值为 null 时删除键。只有原先没有 READ_LOGS 时才撤销它；只移除新增白名单，其他可选改动独立恢复，并重新读取核验。

## Recovery is not reinstallation / 重新激活不等于重装

If the app/settings remain installed but the agent is disconnected, first rerun `tools/activate-agent.sh` through the selected computer ADB connection. Do not uninstall or blindly repeat every setup command. Wireless recovery is optional and cannot operate with wireless debugging disabled. Android device-log approval, package permissions and shell-agent lifetime are different states.

软件和设置还在但代理断开时，先用电脑重新执行代理脚本，不需要卸载或把全部授权重做一遍。设备日志批准、应用权限和 shell 代理寿命是不同的状态。
