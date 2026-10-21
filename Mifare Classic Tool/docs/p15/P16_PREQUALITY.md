# P15-B.1 红米真机验收 + P16 前置质量检查报告

> 分支 `feature/compose-modernization`，HEAD `de34962`。本报告仅记录实际执行结果。

## 1. 设备测试结果（DEFERRED）

| 项 | 结果 |
| :--- | :--- |
| 目标设备 | Redmi Note 8 Pro（begonia），序列号 `pnq47xf6899t4huc` |
| `adb devices` | 列出为 `device`（另有华为 MatePad `3FYBB24914202624`，非目标、不操作） |
| 可用性 | **shell 无响应**：`adb install`、`adb shell pm path`、`adb shell echo/getprop`、`adb shell true` 均挂起超时（≥4 次尝试） |
| 判定 | 疑似此前 `vendor.nxp.hardware.nfc@1.1-service` 崩溃导致系统级无响应；按“不反复重试、不重启、不修改内核”停止设备操作 |
| 结论 | 设备相关验收 **DEFERRED**；未安装 APK、未采集截图/日志、未运行仪器测试 |

> 不将“设备列出但 shell 无响应”视作在线可用；不虚构任何真机验证结果。

## 2. 4 项仪器测试状态

| 测试 | 方法数 | 是否触碰 NFC/写卡 | 状态 |
| :--- | :---: | :--- | :--- |
| `WriteTagShellComposeTest` | 1 | 否（纯 Compose 外壳，无 Activity） | 已编译，未运行（DEFERRED） |
| `WriteTagComposeTest` | 3 | 仅启动 Activity（`enableNfcForegroundDispatch` 为 null 适配器时 no-op），不点击任何写/确认/格式化按钮 | 已编译，未运行（DEFERRED） |

- 隔离可靠性评估：4 个测试均**不触发实体写卡**、不做标签 IO；唯一 NFC 接触是 `BasicActivity.onResume` 的 `enableNfcForegroundDispatch`（`adapter==null || !isEnabled` 时直接返回，且 try/catch 包裹）。
- 因红米 shell 无响应，无法在真机执行；未执行即不标 PASS。待设备恢复后再跑。

## 3. 全局 UI 审计

### 3.1 Activity → UI 技术栈分布

| 分类 | Activity | 数量 |
| :--- | :--- | :---: |
| 全 Compose（`setContent` + `MctTheme` + `MctTopAppBar`） | MainMenu、HelpAndInfo、Preferences、BccTool、ValueBlockTool、DataConversionTool、AccessConditionTool、TagInfoTool、UidLogTool、CloneUidTool、ImportExportTool、FileChooser、KeyEditor、DiffTool、AccessConditionDecoder、ValueBlocksToInt、HexToAscii、DumpEditorPrototype、KeyMapCreator、ReadTag | 20 |
| 混合（`setContentView` + `ComposeView` 外壳 + 原生表单） | WriteTag（本轮 P15-B.1） | 1 |
| 原生（`setContentView`，保留原生 ActionBar） | DumpEditor（生产，P14 冻结） | 1 |

### 3.2 顶栏 / Insets 一致性

- 21/22 Activity 调用 `supportActionBar?.hide()` 并以 `MctTopAppBar` 提供标题与返回；**无重复顶栏**。
- 唯一例外：生产 `DumpEditor` 仍用原生 ActionBar（P14 明确冻结，属已知刻意差异，非回归）。
- 主题 `Theme.AppCompat.DayNight`；API35+ 显式 `windowOptOutEdgeToEdgeEnforcement=true`，非 edge-to-edge；各 Compose 页由 `Scaffold`+`MctTopAppBar` 处理 Insets。
- WriteTag 的 `ComposeView` 外壳为部分 ComposeView：非 edge-to-edge 下状态栏 inset 为 0，预计无双重 padding，但**需真机确认**（当前 DEFERRED）。

### 3.3 主题 / 无障碍 / 导航

- 所有 Compose 页统一 `MctTheme`（深/浅/动态取色由 `isMctDarkTheme()` 决定）。
- 组件统一走 `MctComponents`（`MctTopAppBar`/`MctSectionCard`/`MctActionButton`/`MctMonospaceField`）。
- 顶栏返回按钮 testTag 统一 `top_bar_back`；WriteTag 外壳沿用，语义一致。
- 无障碍：本轮未做 TalkBack 真机验证（DEFERRED）；Compose 页已有 `contentDescription`/`testTag` 基础，WriteTag 原生表单沿用 `labelFor`。

## 4. 基线功能入口保留对照

- 主入口 `MainMenu` 九项工具弹窗 + NFC 状态（P10/P11 迁移时已验证）；本轮未改动 `MainMenu`。
- `WriteTag` 六模式入口、`CloneUidTool` 跳转、`FileChooser`/`KeyMapCreator` 派发契约均保留（见 `WRITE_TAG_CONTRACT.md`）。
- 对照 `50593d9`：本轮仅增写外壳/测试/文档，未改任何业务 Activity 的入口与 Intent。

## 5. 测试实际执行数量核实

| 套件 | 总数 | 已执行 | 未执行 | 备注 |
| :--- | :---: | :---: | :---: | :--- |
| JVM `BaselineTest` | 18 | 18 PASS | 0 | 本机 Gradle `testDebugUnitTest` |
| Compose 仪器（18 类） | 33 | 0 | 33 | 无可用设备 |
| 设备验收 `DeviceAcceptanceTest` | 23 | 0 | 23 | 需真机 |
| **仪器合计** | **56** | **0** | **56** | 红米 shell 无响应 |

> 新增 WriteTag 4 个仪器测试已计入 33（`WriteTagShellComposeTest` 1 + `WriteTagComposeTest` 3）。

## 6. 资源 / 废弃 XML / 构建警告 / 依赖

- `gradle.properties` 含多组 AGP 9.2 已废弃选项（构建日志已逐条告警，仅记录、不清理）：
  `android.usesSdkInManifest.disallowed=false`、`android.sdk.defaultTargetSdkToCompileSdkIfUnset=false`、`android.enableAppCompileTimeRClass=false`、`android.newDsl=false`、`android.r8.optimizedResourceShrinking=false`、`android.defaults.buildfeatures.resvalues=true`、`android.dependency.excludeLibraryComponentsFromConstraints`（建议启用）。
- `values-v35/styles.xml` 的 edge-to-edge opt-out 为显式规避方案，随 targetSdk 升 36 需重新评估（记录，不修改）。
- 依赖（`build.gradle`）：未发现明显未使用大库；`ui-tooling-preview`/`ui-tooling` 为 Compose 标准件。完整未用依赖判定需跑 `lint`/依赖报告，列为 P16 任务。

## 7. 未跟踪文件分类（保护，不清理）

| 路径 | 类别 | 处置 |
| :--- | :--- | :--- |
| `Mifare Classic Tool/.kotlin/` | 构建缓存 | 保留，加入 `.gitignore`（建议） |
| `Mifare Classic Tool/hs_err_pid*.log` | JVM 崩溃日志 | 保留，供排查 |
| `Mifare Classic Tool/read_tag_waiting.png`、根目录 `keymap_*.png`、`read_tag_*.png`、`keymap_selected.png`、`uidump.xml`、`keymap_instrument.{txt,err}` | 测试截图/日志/UI dump | 保留（验收证据），不 `git clean` |

> 未发现用户个人数据文件；按“保护已有截图、日志”原则一律不删除、不执行 `git clean -fdx`。

## 8. 遗留风险

1. 红米 shell 无响应（疑似 NFC HAL 崩溃后遗症），WriteTag 真机验收与 4 仪器测试未执行。
2. WriteTag 部分 ComposeView 的 Insets/滚动/旋转/IME/无障碍需真机确认。
3. 生产 `DumpEditor` 原生 ActionBar 与其他页 M3 顶栏不一致（P14 刻意冻结）。
4. `MCReader`、`DumpEditor`、WriteTag 写卡状态机未改；实体写卡/UID/Block0/SAK/ATQA/Gen2/Gen3 全程零操作。

## 9. P16 可执行计划（阻断 / 可延期）

**阻断（需先解决）**
- [P16-B1] 恢复红米可用性（用户排查或换设备）后再跑 WriteTag 4 仪器测试与全局 UI 真机验收。

**可延期 / 建议**
- [P16-D1] 跑 `lint` + Gradle 依赖报告，识别未用依赖与资源。
- [P16-D2] 评估 targetSdk 36 时 edge-to-edge 强制化的迁移（`windowOptOutEdgeToEdgeEnforcement` 失效后的 Insets 适配）。
- [P16-D3] 将 `.kotlin/`、`hs_err_pid*.log`、测试截图/日志加入 `.gitignore`（低风险，仅影响忽略规则）。
- [P16-D4] 统一生产 `DumpEditor` 顶栏（需单独授权，涉及冻结页）。
- [P16-D5] 完整 Compose 表单替换 / 写入状态机重构（需单独授权，本轮禁止）。
- [P16-D6] 真机测量 WriteTag 启动耗时、内存、滚动、TalkBack 焦点顺序（单次测量，不造统计结论）。

## 10. Git 状态

- 分支 `feature/compose-modernization`，HEAD `de34962`；本轮未产生新提交（设备 DEFERRED，审计记录入文档后按阶段提交）。
- `master = 50593d9`、Tag `v4.3.1-kotlin-migration-baseline = e156d4c` 未变。
- 未推送、未发布、未合并、未移动 Tag。
