# 红米真机验收 Round 1 —— 设备已恢复但自动化验收受环境阻断

> 分支 `feature/compose-modernization`，HEAD `779d30d`。仅记录实际执行结果。

## 结论速览

红米 Note 8 Pro（`pnq47xf6899t4huc`，Android 16 / API 36）**shell 已恢复响应**，APK 安装与主菜单渲染正常；但 **WriteTag 自动化 UI 验收被两个环境问题阻断**，未产生真机写卡页验收证据，不虚构 PASS。

## 已完成并验证

| 项 | 结果 | 证据 |
| :--- | :--- | :--- |
| 设备恢复 | shell 响应正常（`echo`/`getprop`/`screencap`/`dumpsys`/`install` 均可用） | 本目录日志 |
| NFC 服务 | `mState=on`，全程未崩溃 | `dumpsys nfc` |
| Debug APK 安装 | `Success` | `adb install` |
| androidTest APK 安装 | `Success` | `adb install` |
| MainMenu（Compose）渲染 | 正常：NFC 已就绪横幅、读/写/编辑/工具/帮助卡片、App 版本 4.3.1 | `menu.png` + `menu2.xml` UI dump |

## 阻断项（环境，非 App 缺陷）

1. **`adb shell input` 注入被拒**（`SecurityException: INJECT_EVENTS`）：MIUI 未开启「USB 调试（安全设置）」，无法模拟点击/按键，因此无法从 MainMenu 点击「写标签」进入 WriteTag。
2. **WriteTag 非 exported 且无法替代启动**：`am start`（拒绝）、`run-as`（`package=com.android.shell does not belong to uid=10264`）均失败。
3. **全部仪器测试在 API 36 上挂起**：
   - `WriteTagComposeTest`（3）+ `WriteTagShellComposeTest`（1）：挂起，无输出、无崩溃。
   - 隔离验证 `WriteTagShellComposeTest`（纯 Compose、无 Activity/NFC）：仍挂起 → 与 NFC 无关。
   - 隔离验证 `DeviceAcceptanceTest#test02_LaunchMainMenu`（纯 `ActivityScenario.launch`、无 Compose 测试规则、无 NFC IO）：仍挂起 → 与 Compose 无关。
   - 结论：`am instrument` → `AndroidJUnitRunner` → `ActivityScenario` 基础设施在本机（target/compile 35 应用运行于 API 36）整体挂起，非测试用例或 NFC 引起。

> 上述三次仪器测试尝试均先 `am force-stop` 测试与应用，且设备保持 `alive`、NFC `mState=on`，未造成设备崩溃；按「不反复重试」停止继续尝试。

### Round 1 补充（复测仍阻断）

- 发现屏幕处于休眠（`mWakefulness=Asleep`/`mScreenOn=false`），已通过 `input keyevent KEYCODE_WAKEUP` 唤醒并 `svc power stayon true` 保持常亮（`mScreenOn=true`）。
- 屏幕唤醒后**重跑 4 个 WriteTag 仪器测试仍挂起**（>150s 无输出，`mCurrentFocus` 回到桌面，`dumpsys activity` 仅见 `PACKAGE_RESTARTED/UNSTOPPED`）。
- 结论：仪器测试挂起与屏幕休眠、NFC、Compose、用例类型均无关，确认为 androidx.test/`ActivityScenario` 在 API 36 上的基础设施级问题。
- `adb shell input` 注入（tap/keyevent）持续被拒（`INJECT_EVENTS`），无法手动导航。
- 测试后已 `force-stop` 两包并关闭 stayon；设备 `alive`、NFC `mState=on`，未崩溃。

### Round 2 根因定位（logcat）

- 捕获纯 Compose `WriteTagShellComposeTest` 挂起时的 logcat，关键行：
  - `ActivityStarterImpl: MIUILOG- Permission Denied Activity : Intent { ... cmp=de.syss.MifareClassicTool/androidx.activity.ComponentActivity } pkg : de.syss.MifareClassicTool uid : 10264 tuid : 10140`
  - `ActivityTaskManager: START u0 { ... cmp=de.syss.MifareClassicTool/androidx.activity.ComponentActivity } ... result code=102`
- 已确认合并后的 Debug 清单**正确声明** `androidx.activity.ComponentActivity android:exported="true"`（来自 `ui-test-manifest`），但 MIUI `ActivityStarterImpl` 仍以「Permission Denied」拒绝（`result code=102` = START_PERMISSION_DENIED）。
- **统一根因**：MIUI 的「USB 调试（安全设置）」未开启，导致 (1) `adb shell input` 注入被拒（`INJECT_EVENTS`）与 (2) 仪器测试启动 Activity 被 MIUI `ActivityStarterImpl` 拒绝，两者同源。非 App 缺陷、非 androidx.test/API 36 缺陷。
- 需用户开启红米「开发者选项 → USB 调试（安全设置）」（部分 MIUI 版本需登录小米账号）。

## 遗留风险

- WriteTag M3 外壳 + 六模式表单在本机无法通过自动化触达，真机验收仍为 UNTESTED。
- 4 个 WriteTag 仪器测试在本机无法运行（API 36 仪器基础设施挂起），仍为 UNTESTED。
- 未做实体写卡、UID/Block0/SAK/ATQA/Gen2/Gen3、未改 MCReader/生产 DumpEditor/写卡状态机。

## 需要用户处置（二选一或组合）

1. 在红米上开启 MIUI「开发者选项 → USB 调试（安全设置）」，以允许 `adb shell input` 注入，从而手动导航到 WriteTag 截图验收。
2. 就 API 36 上仪器测试挂起给出处置（例如：接受「本机无法跑 Compose 仪器测试」为已知限制、改用 API ≤ 35 设备、或授权排查 androidx.test/Compose BOM 与 API 36 兼容性）。
