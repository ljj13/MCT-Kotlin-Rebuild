# P16 FINAL REPORT —— MCT 全应用现代化质量审计、功能等价验证与发布前收口

> 分支 `feature/compose-modernization`，HEAD `c771463`。
> 基线：`master = 50593d9`，Tag `v4.3.1-kotlin-migration-baseline = e156d4c`（两者树内容一致 `b52568c8…`，均未改动）。
> 审计期间禁止实体写卡/UID/Block0/Gen2/Gen3；未改 MCReader、生产 DumpEditor、WriteTag 写卡状态机；未合并 master、未推送、未移动/创建 Tag、未发布。

---

## 1. 实际现代化页面矩阵（P16.0）

共 22 个 Activity（`MainMenu` 由 1 个 Activity + 1 个 `activity-alias` 组成）。分类如下：

| 类别 | 数量 | Activity |
| :--- | :---: | :--- |
| ① 完整 Compose（`setContent` + `MctTheme`，无 AndroidView） | 17 | MainMenu、ReadTag、KeyMapCreator、FileChooser、AccessConditionDecoder、ValueBlocksToInt、HexToAscii、DiffTool、BccTool、CloneUidTool、TagInfoTool、AccessConditionTool、ValueBlockTool、Preferences、ImportExportTool、UidLogTool、DataConversionTool |
| ② Compose + AndroidView 混合 | 2 | KeyEditor（AndroidView 嵌入 AppCompatEditText 编辑核心）、HelpAndInfo（AndroidView 承载帮助内容） |
| ③ ComposeView + 原生 XML 混合 | 1 | WriteTag（`setContentView` + 顶部 `ComposeView` 外壳 + 原生六模式表单） |
| ④ 完全保留原生 View | 1 | DumpEditor（生产，P14 冻结，`setContentView`） |
| ⑤ 仅实验原型（未替换生产） | 1 | DumpEditorPrototype（P14 独立原型，非生产入口） |

- 未将 `DumpEditorPrototype` 统计为生产迁移完成；生产 Dump 编辑仍走原生 `DumpEditor`。
- 入口映射：`MainMenu`（`MAIN/LAUNCHER`）→ 各工具 Activity；`MainMenuAlias`（`TECH_DISCOVERED` + `@xml/nfc_tech_filter`）→ 指向 `MainMenu`。

---

## 2. 全部功能入口等价审计（P16.1）

对照原 Kotlin 基线逐项核对 Intent 契约，**未发现功能丢失、默认值改变、枚举重排、RequestCode 变更或 Intent 类型破坏**：

| 契约 | 结论 |
| :--- | :--- |
| MainMenu 全部功能入口（读标签/写标签/编辑转储/密钥/工具/帮助） | ✅ 等价，`NFC 已就绪` 门控保留 |
| ReadTag / KeyMapCreator 数据交接（`EXTRA_KEYS_DIR`/`EXTRA_SECTOR_CHOOSER_*`/`EXTRA_USE_KEYS_FROM_DUMP`） | ✅ 等价 |
| WriteTag 六模式（write_block / write_dump / clone_uid / factory_format / write_value_block / transfer_restore） | ✅ 六模式齐全，默认 write_block，写卡状态机未动 |
| FileChooser `RESULT_OK`/取消/路径返回（`EXTRA_CHOSEN_FILE`/`EXTRA_CHOSEN_FILENAME`/`EXTRA_DIR`/`EXTRA_ALLOW_NEW_FILE`） | ✅ 等价 |
| DumpEditor / DiffTool `EXTRA_DUMP` 数据传递 | ✅ 等价 |
| KeyEditor 保存/重载 | ✅ 等价（AndroidView 编辑核心保留） |
| ImportExportTool SAF 权限 | ✅ 等价 |
| Preferences 数据格式/旧版兼容 | ✅ 等价（`preference` 1.2.1） |
| TagInfoTool NFC Intent（`BasicActivity.onNewIntent`） | ✅ 等价 |
| CloneUidTool 写入入口/校验链 | ✅ 等价（仅 UI 现代化，校验链未改） |
| 所有 XML 菜单/按钮/对话框原有功能 | ✅ 等价 |

> 备注：`FileChooser.EXTRA_CHOSEN_FILENAME` 常量值含冗余 `EXTRA_` 前缀（`…Activity.EXTRA_CHOSEN_FILENAME`），与 `EXTRA_CHOSEN_FILE`（`…Activity.CHOSEN_FILE`）命名不对称；但读写两侧均引用同一常量，**功能等价**，仅属命名瑕疵，未改动（避免不必要的契约扰动）。

---

## 3. UI 一致性检查与修复（P16.3）

**共享组件体系已统一**（`ui/components/MctComponents.kt`）：
- `MctTopAppBar`（统一顶栏高度 + 返回按钮）、`MctSectionCard`（统一卡片圆角/内边距）、`MctMonospaceField`（等宽 Hex/UID 字体）、`MctActionButton`（统一按钮尺寸/禁用态）。
- 全部 Compose 页面经 `MctTheme`（`ui/theme/Theme.kt`）：深/浅色 `ColorScheme`，`dynamicColor=true`（Android 12+ 动态取色），`isMctDarkTheme()` 跟随系统。

**真机证据**：`docs/p15/redmi/menu.png`（MainMenu 浅色）、`wt-default.png`（浅色）、`wt-dark.png`（深色）、`wt-land.png`（横屏）、`wt-scroll.png`（滚动）。深浅色/旋转/滚动/返回导航均已真机确认。

**未逐项真机核验（标记 UNTESTED，未标 PASS）**：小屏密集布局、输入法遮挡、Dialog 最大高度滚动、空/错状态、无障碍 TalkBack 逐焦点走查、等宽字体在极端数据下的显示。这些依赖更完整的真机/无障碍自动化，本次未具备条件。

**修复**：本阶段未发现确凿的低风险布局/排版/主题缺陷，故未做 UI 代码改动；仅完成 `.gitignore` 卫生修复（见 §8）。

---

## 4. NFC 与生命周期风险（P16.2）

静态审计结论：

- **NFC 前台分发成对**：`BasicActivity.onResume()` 调 `Common.enableNfcForegroundDispatch`，`onPause()` 调 `disableNfcForegroundDispatch`，成对无泄漏。
- **MainMenu 广播接收器成对**：`registerReceiver` 与 `unregisterReceiver` 成对（生命周期内注销）。
- **`onNewIntent` 语义独立保留**：各 Activity 经 `BasicActivity` 将新 Intent 视为新 Tag，非 MIFARE Classic 时转 `TagInfoTool`。
- **写卡进度 Dialog 关闭 ≠ 取消写入**（WriteTag 冻结行为，未改）：`writeDump` 进度 `AlertDialog` 非 `setCancelable(false)`，对话框关闭不改变底层写卡任务语义——此与基线一致。
- **写入线程/认证顺序未统一**：writeBlock 恒 B→A、writeValueBlock 失败时 B→A、VTR 目的 A→B、dump 按 write-info 编码——全部保留，未重构。

**风险（记录，不擅自重构）**：
1. Activity 重建（旋转）时 Compose 状态恢复与原生表单（WriteTag）的一致性问题——WriteTag 已在 Manifest 声明 `configChanges="keyboardHidden|orientation|screenSize"` 规避重建，其余 Compose 页面未逐项真机覆盖。
2. 写入中离页是否产生重复请求——属写卡业务语义，本次静态审计未发现代码路径变化，但**未经真实写卡压力测试，标记 UNTESTED**。
3. P15-A 已识别的异常生命周期场景（快速读卡/中断）仍 UNTESTED。

**结论**：静态审计未发现成对注销缺失或生命周期泄漏；凡未经真实 NFC/生命周期压力测试的项均标 UNTESTED，不标 PASS。

---

## 5. 自动化及真机测试统计（P16.4）

| 类别 | 数量 | 状态 |
| :--- | :---: | :--- |
| JVM 单元测试（`BaselineTest`） | 18 | **PASS**（本会话多次 18/18） |
| Compose 仪器测试（33 方法） | 33 | **BLOCKED**（红米 MIUI `ActivityStarterImpl` 拒绝 instrumentation 启动 Activity，`result code=102`） |
| DeviceAcceptance 仪器测试（23 方法） | 23 | **BLOCKED**（同上） |
| 手动 UI 验收 —— WriteTag | — | **PASS**（红米，M3 外壳 + 六模式 + 深浅色/旋转/滚动/返回） |
| 手动 UI 验收 —— 其余 Compose 页面 | — | **PASS**（P9–P13 截图佐证） |
| NFC 真实硬件只读 | — | **PASS**（P15-A，OPPO 正常只读链路） |
| NFC 异常生命周期场景 | — | **UNTESTED** |
| 历史隔离 UI 测试（OPPO，P15-A） | 4 | **PASS**（设备已离线，不复跑） |
| 无障碍 TalkBack 逐焦点走查 | — | **SKIPPED**（需人工/授权自动化） |

- 未将「编译成功」计作运行通过；56 个仪器方法在红米 0 执行。
- 保留记录：P14 仪器测试挂起、P15-A 原始失败与修复后只读 PASS、P15-B MIUI 阻断。
- 未关闭任何断言、未删除测试、未改内核。

---

## 6. APK 与 Manifest 审计（P16.5）

| 项 | 值 | 结论 |
| :--- | :--- | :--- |
| minSdk / targetSdk / compileSdk | 21 / 35 / 35 | ✅ 符合预期 |
| AGP / Kotlin Compose 插件 | 9.2.0 / 2.2.10（内置 Kotlin 2.2.10） | ✅ |
| Compose BOM / activity-compose | 2024.10.00 / 1.9.3 | ✅ |
| JVM target | 11 | ✅ |
| R8 混淆 / 资源收缩 | `minifyEnabled=false` / `shrinkResources=false` | ⚠️ 见下 |
| 权限最小化 | 仅 `android.permission.NFC` | ✅ |
| `uses-feature nfc required=true` | 是 | ⚠️ 仅 NFC 设备可安装（与产品定位一致） |
| Manifest `exported` | MainMenu + MainMenuAlias `true`，其余 22 Activity `false` | ✅ 最小暴露 |
| Debug 专用组件 | `ui-tooling`、`ui-test-manifest` 仅 `debugImplementation` | ✅ |
| **Release 含实验原型** | `DumpEditorPrototype` 声明于主 Manifest | ⚠️ 建议移至 debug manifest |

**构建状态**（本会话已执行并产出）：
- `assembleDebug` ✅（APK 13,888,776 B）；`assembleRelease` ✅（APK 9,028,033 B）；`assembleDebugAndroidTest` ✅（56 方法编译通过）。
- JVM 测试 ✅ 18/18。

**发现（低风险，记录 + 建议，不擅自改）**：
1. **R8 未启用**：Release 未混淆/未收缩，APK 未做体积与安全加固。发布前建议评估开启 `minifyEnabled=true` + `shrinkResources=true`（需额外 ProGuard 规则验证，涉及反射/序列化风险，故本次不改）。
2. **`DumpEditorPrototype` 在主 Manifest**：实验原型进入 Release 构建（虽非 exported、无入口）。建议移至 `src/debug/AndroidManifest.xml`。本次记录，未改动（避免影响 androidTest 引用与 P14 独立状态）。

> 构建、签名、发布为不同状态：本次仅完成构建（Debug/Release APK 产物），**未签名发布、未发布**。

---

## 7. 性能结果（P16.6）

| 项 | 结果 |
| :--- | :--- |
| 启动耗时 / 滚动卡顿 / 内存占用 / 大文本输入 / 焦点切换 / 配置变化恢复 | **UNMEASURED**（无可靠采样条件，未编造性能数字） |

- 生产 `DumpEditor` 未替换；`DumpEditorPrototype` 保持独立，未纳入生产入口。
- 唯一真机观察：MainMenu/WriteTag 真机导航流畅、无可见卡顿（定性观察，非量化采样）。

---

## 8. 工作区文件分类（P16.7）

**已忽略（缓存/可重建，`.gitignore` 本次新增）**：
- `Mifare Classic Tool/.kotlin/`（Kotlin 构建缓存）
- `**/hs_err_pid*.log`（JVM 崩溃转储）

**仍为未跟踪（真机证据，未删除、未忽略，建议归档 `docs/p15/`）**：
- 截图：`keymap_creator.png`、`keymap_range.png`、`keymap_selected.png`、`read_tag_entry.png`、`read_tag_waiting.png`、`read_tag_waiting2.png`、`Mifare Classic Tool/read_tag_waiting.png`
- 日志/转储：`keymap_instrument.txt`、`keymap_instrument.err`、`uidump.xml`

**已跟踪证据**：`docs/p15/**`（P15-A/P15-B 报告、MainMenu/WriteTag 截图与 UI dump）。

未执行 `git clean -fdx`；未删除任何原始证据。

---

## 9. 独立 Git 提交

本 P16 阶段提交（均中文 Conventional Commit）：
- 本轮：`.gitignore` 卫生修复（`chore(p16): …`，见下提交哈希）
- 继承前序：`c771463`、`1ae403f`、`3777272`、`478d161`、`1a64709`（P15-B）等，共 49 次提交，184 文件 `+38876/-2267`。

`master = 50593d9`、Tag `e156d4c` 不变；未推送、未合并、未发布。

---

## 10. 发布前剩余阻断项

| # | 项 | 严重度 | 状态 |
| :--- | :--- | :--- | :--- |
| 1 | WriteTag 4 项仪器测试 MIUI 阻断（instrumentation 启动 Activity 被拒） | 中 | BLOCKED（设备限制，非 App 缺陷） |
| 2 | 56 项仪器测试红米 0 执行（仅编译通过） | 中 | BLOCKED（同上） |
| 3 | P15-A 异常生命周期场景 | 中 | UNTESTED |
| 4 | 无障碍 TalkBack 逐焦点走查 | 低 | SKIPPED |
| 5 | 性能量化（启动/滚动/内存/大输入） | 低 | UNMEASURED |
| 6 | R8 混淆/资源收缩未启用 | 低 | 建议（发布前评估） |
| 7 | `DumpEditorPrototype` 在 Release Manifest | 低 | 建议（移至 debug） |

---

## 最终结论：**PARTIAL**

- **主体完成**：现代化页面矩阵准确、全功能入口等价、NFC 生命周期静态审计无泄漏、JVM 18/18 PASS、NFC 真实只读 PASS、WriteTag 混合 UI 手动验收 PASS、APK/Manifest 主体合规、构建三目标（Debug/Release/androidTest）通过。
- **明确延期项目**：WriteTag/全部仪器测试受 MIUI 阻断（BLOCKED）、P15-A 异常生命周期 UNTESTED、性能 UNMEASURED、无障碍 SKIPPED、R8 未启用与实验原型进 Release 的发布前优化建议。

未发现影响安全、功能等价或基本稳定性的阻断缺陷（无 BLOCKED 级功能/安全缺陷）；因存在上述明确延期与未测项，结论为 **PARTIAL**，而非 READY_FOR_RELEASE_REVIEW。P16 主体审计已自主收束，不自动进入合并或发布。
