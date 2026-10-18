# MCT-Kotlin-Rebuild 渐进式迁移计划

## 迁移目标
对 MifareClassicTool (MCT) 最新上游代码开展 Java → Kotlin 渐进式迁移。
保持现有 XML UI、功能、数据格式、包名、NFC 交互及兼容性不变，不引入额外架构重构。
重点保障空安全、Java 互操作、反射/序列化、异步任务、NFC 连接生命周期与异常语义。

## OPPO 真机验收续项（历史计划；现正式归档 PARTIAL / DEFERRED）
- 当前结论：正常实体读卡/64 块与 P7 哈希对照、18 项 JVM、4 项隔离 UI PASS；扫描中取消 SKIPPED/UNTESTED，扫描中离页/断连/重建 DEFERRED/UNTESTED。
- P15-B 本轮仅源码预审，六个模式与每条写入时序冻结；推荐首期 M3 外壳 + 原生表单，不实施迁移。下列连接等待内容为历史记录，不代表当前设备状态。
- 目标：OPPO Find X8s+，硬件序列号 X8OVSG49IR7X7L4D；所有设备命令必须显式指定已核实的 ADB 目标。
- 无线配对曾成功，但尚未建立在线连接。用户提供的连接地址 192.168.137.23:42091 返回 10061（拒绝连接）；mDNS 当前无服务，ADB 仅有红米和华为。
- 不再全端口扫描；等待无线调试主页面当前 IP:端口。红米内核 NFC 问题不再排查。
- 待办：核实 OPPO 系统/NFC/MIFARE 支持、安装当前分支 APK、隔离运行密钥页和 P14 原型用例并保存日志、用户贴卡完成 16 扇区/64 块只读闭环、与 P7 转储对照。
- P15-A 尚未达到最终验收条件；旧报告中的“完成”不代表真实 NFC 闭环通过。未开始 P15-B。禁止写卡、UID/Block 0 修改及 Gen3 命令。

## P15-B 第一期：安全混合 UI（当前阶段）
- **P15-B.0 契约冻结 [complete]**：`docs/p15/WRITE_TAG_CONTRACT.md` 记录六模式、控件→事件映射、Intent/RequestCode、确认对话框、写卡线程模型、允许/禁止边界，并对照 `50593d9` 基线确认无业务差异。
- **P15-B.1 混合外壳 [complete]**：`activity_write_tag.xml` 根节点改为纵向 `LinearLayout` + 顶部 `ComposeView`，原 `ScrollView` 表单保留；`WriteTag.kt` 增 `supportActionBar?.hide()` 与 `WriteTagShell` 挂载；新增 `ui/writetag/WriteTagShell.kt`（`MctTopAppBar` + 纯展示安全提示）与 `write_tag_safety_notice` 文案。六模式与写卡业务零改动。
- **P15-B.2 非写卡测试 [complete，编译通过未运行]**：新增 `WriteTagShellComposeTest`（1）与 `WriteTagComposeTest`（3）；`assembleDebugAndroidTest` 通过；因红米未连接未执行，一律 UNTESTED。JVM 18/18。
- **P15-B.3 真机验收 [deferred]**：红米 `pnq47xf6899t4huc` 未连接，仅华为 MatePad 在线（非目标且不操作）。无截图/日志/真机证据。
- **P15-B.4 布局/性能 [static only]**：静态确认无重复顶栏（ActionBar 已隐藏）、无双重 Insets（非 edge-to-edge）、ScrollView/ComposeView 无冲突。APK：Debug 13,888,776 B、Release 9,028,033 B（未签名）。性能/内存/旋转/IME 需真机，未测。
- **P15-B.5 修复 [无]**：未发现需修复项。
- **P15-B.6 收口 [报告已提交，真机项未达成]**：完整 A–H 报告见 `docs/p15/P15B_REPORT.md`。
- 提交：`4514f86`、`20c5f74`、`056e5f6`。禁止实体写卡/UID/Block0/SAK/ATQA/Gen2/Gen3，`MCReader`/生产 `DumpEditor`/`master`/Tag 不变，不推送不发布。

## P16 前置质量检查（红米 DEFERRED，离线审计完成）
- 红米 `pnq47xf6899t4huc` 列为 `device` 但 shell 无响应（≥4 次命令挂起，疑似 NFC HAL 崩溃后遗症）；设备验收 DEFERRED，未安装/截图/跑测试。
- 4 项 WriteTag 仪器测试已编译、未运行；静态确认不触发写卡。
- 全局 UI：22 Activity = 20 Compose + 1 混合(WriteTag) + 1 原生(生产 DumpEditor)；21/22 隐藏 ActionBar，无重复顶栏。
- 测试执行数：JVM 18/18；仪器 56 方法 0 执行。
- 构建警告/废弃项/未跟踪文件已记录。详见 `docs/p15/P16_PREQUALITY.md`。

## P15-B.1 红米真机验收（Round 1 环境阻断）
- 红米 shell 恢复、APK 安装成功、NFC 正常、MainMenu Compose 渲染正常。
- WriteTag 验收被阻断：`input` 注入被拒（MIUI 安全设置未开）+ 仪器测试在 API 36 挂起（ActivityScenario 基础设施）。
- 需用户开启「USB 调试（安全设置）」或就 API 36 仪器挂起给出处置。见 `docs/p15/redmi/DEVICE_ACCEPTANCE_ROUND1.md`。

## 阶段规划

### 阶段 0：P0 源码与架构审计、构建基线建立 [complete]
- [x] 代码库初检与克隆、分支创建 (`refactor/kotlin-migration`)
- [x] Gradle 9.4.1 + AGP 9.2.0 + Kotlin 2.2.10 构建可复现性验证
- [x] 源码架构审计（包结构、类清单、依赖关系、NFC 生命周期、异步/反射/序列化、测试覆盖）
- [x] 建立核心算法单元测试基线（针对 Common、MCDiffUtils 等纯算法与核心逻辑编写测试套件并保证通过）
- [x] 记录基线指标与审计报告至 `findings.md`
- [x] 阶段 0 提交基线测试与配置

### 阶段 1：基础工具与算法层迁移 (Foundation & Utils) [complete]
- [x] `IActivityThatReactsToSave.java` → `IActivityThatReactsToSave.kt`
- [x] `MCDiffUtils.java` → `MCDiffUtils.kt`
- [x] `Common.java` → `Common.kt`（含常量、文件/算法/NFC 工具方法、静态度与 Java 互操作注解 `@JvmStatic` / `@JvmField`）
- [x] 单元测试验证与全量编译验证 (`testDebugUnitTest` 8/8 通过，`assembleDebug` 成功)
- [x] 中文 Conventional Commit (`refactor(core): ...`)

### 阶段 2：独立工具 Activity 迁移 (Leaf Tool Activities) [complete]
- [x] `HelpAndInfo.java` → `HelpAndInfo.kt`
- [x] `HexToAscii.java` → `HexToAscii.kt`
- [x] `BccTool.java` → `BccTool.kt`
- [x] `ValueBlocksToInt.java` → `ValueBlocksToInt.kt`
- [x] `ValueBlockTool.java` → `ValueBlockTool.kt`
- [x] `DataConversionTool.java` → `DataConversionTool.kt`
- [x] `AccessConditionDecoder.java` → `AccessConditionDecoder.kt`
- [x] `AccessConditionTool.java` → `AccessConditionTool.kt`
- [x] 编译验证与单元测试 (`testDebugUnitTest` 8/8 通过，`assembleDebug` 成功)
- [x] 中文 Conventional Commit (`refactor(tools): ...`)

### 阶段 3：基础 Activity 与高级工具/文件管理迁移 (Base & Complex Activities) [complete]
- [x] `BasicActivity.java` → `BasicActivity.kt`（所有 Activity 基类，处理 NFC 前台调度与公共菜单）
- [x] `FileChooser.java` → `FileChooser.kt`
- [x] `Preferences.java` → `Preferences.kt`
- [x] `DiffTool.java` → `DiffTool.kt`
- [x] `UidLogTool.java` → `UidLogTool.kt`
- [x] `CloneUidTool.java` → `CloneUidTool.kt`
- [x] `TagInfoTool.java` → `TagInfoTool.kt`
- [x] `KeyEditor.java` → `KeyEditor.kt`
- [x] `DumpEditor.java` → `DumpEditor.kt`
- [x] `ImportExportTool.java` → `ImportExportTool.kt`
- [x] 编译验证与单元测试 (`testDebugUnitTest` 8/8 通过，`assembleDebug` 成功)
- [x] 中文 Conventional Commit (`refactor(activities): ...`)

### 阶段 4：NFC 核心引擎与主流程迁移 (Core NFC Engine & Main Activities) [complete]
- [x] `MCReader.java` → `MCReader.kt`（核心 NFC 扇区认证、数据读写、转码与异常处理）
- [x] `KeyMapCreator.java` → `KeyMapCreator.kt`
- [x] `ReadTag.java` → `ReadTag.kt`
- [x] `WriteTag.java` → `WriteTag.kt`
- [x] `MainMenu.java` → `MainMenu.kt`
- [x] 处理 `package-info.java`
- [x] 编译验证与单元测试 (`testDebugUnitTest` 8/8 通过，`compileDebugJavaWithJavac NO-SOURCE`, `assembleDebug` 成功)
- [x] 中文 Conventional Commit (`refactor(nfc): ...`)

### 阶段 5：全面验证、覆盖率盘点与总结汇报 [complete]
- [x] 全量 `assembleDebug` 与 `testDebugUnitTest` 回归（全部成功通过，Java 编译为 NO-SOURCE）
- [x] 统计 Kotlin 迁移率（26/26 业务类完成迁移，100% Kotlin 覆盖，源码净减少 2,854 行）
- [x] 遗留风险审计（高风险模块审查、异常语义保持、Android 生命周期契约）
- [x] Git 状态确认（保留在 `refactor/kotlin-migration` 分支，绝不合入 master）
- [x] 生成最终审计与迁移成果报告

### 阶段 6：P6 红米手机真机验收与行为对照 [complete]
- [x] 确认 ADB 设备：红米 Note 8 Pro (`pnq47xf6899t4huc`, Android 16 / SDK 36, NXP NFC)
- [x] 构建并安装最新 Debug APK 与 AndroidTest APK
- [x] 自动验证应用启动、全量 Activity 导航与运行时生命周期切换
- [x] 实时采集 logcat，排查并确认 0 崩溃 (0 FATAL EXCEPTION)
- [x] 校验文件系统与数据结构（`dump-files`, `key-files` 与内置字典）
- [x] 模拟 NFC 标签意图 (`TECH_DISCOVERED`) 与非 MFC 卡兼容性分发
- [x] 严格实施安全边界：未对实体卡执行任何未经确认的写入或 UID/Block0/Gen3 危险写操作
- [x] 输出完整 PASS / FAIL / UNTESTED 验收矩阵、日志证据、剩余风险与合并建议

### 阶段 7：P7 真实卡片读卡验收与最终收口合并 [complete]
- [x] 用户配合连接支持 NFC 的 OPPO Find X8s+ 设备并授权 ADB
- [x] 备份拉取官方原版 Java APK 为 `original-java-mct-v4.3.1.apk`
- [x] 替换安装 Kotlin Debug APK 并验证应用沙盒文件初始化
- [x] 贴卡只读认证：成功读取真实 MIFARE Classic 1K 测试卡（UID `0BDC504F`，ATQA `0400`，SAK `08`）
- [x] Dump 读取验收：全 16 扇区（0..15）成功认证并完整读取，截取并保存真实渲染证据 `mct_dump_editor_screenshot.png`
- [x] 严格执行只读验收，零实体写卡操作
- [x] 显式 merge commit 将 `refactor/kotlin-migration` 合并至 `master`
- [x] 重新构建并打基线标签 `v4.3.1-kotlin-migration-baseline`，保留本地结果未向远程推送

### 阶段 8：P8 Compose M3 现代化试点与 BccTool 迁移 [complete]
- [x] 基于本地 master 新建 `feature/compose-modernization` 分支
- [x] 核对 AGP 9.2 + Compose Compiler (2.2.10) + Compose BOM (2024.10.00) 依赖，设定 minSdk 21
- [x] 建立 MctTheme 设计系统（支持深浅色与 Android 12+ 动态取色）
- [x] 迁移 BccTool 至 Compose Material 3 并编写 BccToolComposeTest
- [x] 真机验收并截取 `bcc_tool_compose_screenshot.png`

### 阶段 9：P9 独立工具页面 Material 3 全面现代化 [complete]
- [x] 联动 MctTheme 与原有主题偏好 (`CustomAppTheme`)，实测 Release APK 体积 (8.41 MB)
- [x] 建立统一的 MctComponents 组件库（TopAppBar、SectionCard、MonospaceField、ActionButton）
- [x] 迁移 HexToAscii 至 Compose M3，真机验收并截取 `hextoascii_compose_screenshot.png`
- [x] 迁移 ValueBlockTool 至 Compose M3，真机验收并截取 `valueblock_compose_screenshot.png`
- [x] 迁移 DataConversionTool 至 Compose M3，真机验收并截取 `dataconversion_compose_screenshot.png`
- [x] 迁移 AccessConditionTool 至 Compose M3，真机验收并截取 `accesscondition_compose_screenshot.png`
- [x] 全量测试回归与 Release APK 产物体积度量
- [x] 保持 master 和原有 tag 不变，未向远程推送或发布 Release

### 阶段 10：P10 首页 MainMenu 完整 UI 现代化 [complete]
- [x] 保留原有 NFC 前台分发、onNewIntent 意图路由、StartUpNode 状态机与全部 Activity 导航
- [x] 使用 MctTheme + Material 3 重新设计 MainMenu 界面层级与入口映射
- [x] 引入动态 NfcStatusBanner（实时反应就绪、未开启、仅编辑器、不支持状态）
- [x] 重新组织读/写核心卡片、离线转储/密钥编辑卡片与 9 项工具弹窗
- [x] 编写针对性 MainMenuComposeTest 覆盖全部入口、弹窗与状态显示
- [x] 红米 Note 8 Pro 真机验证与前后台切换，截取 `main_menu_compose_screenshot.png` 与 `main_menu_tools_dialog_screenshot.png`
- [x] 全量测试回归与 Release 构建验证（Release APK 体积 8.43 MB）
- [x] 保持 master 和基线 tag 不变，未向远程推送或发布 Release

### 阶段 11：P11 核心功能页面渐进式现代化与体验收口 [complete]
- [x] 补齐 P10 遗留项：增加 NfcAdapter 状态广播监听 (`ACTION_ADAPTER_STATE_CHANGED`)，弹窗滚动高度约束与无障碍标签增强
- [x] 迁移 HelpAndInfo 至 Compose Material 3，保留离线 WebView 与 HTML 资源解析，真机截图 `help_and_info_compose_screenshot.png`
- [x] 迁移 TagInfoTool 至 Compose M3，保留 NFC 前台分发、新卡解析、SAK/ATQA/ATS 矩阵计算与不兼容提示，真机截图 `tag_info_compose_screenshot.png`
- [x] 迁移 Preferences 至 Compose M3，100% 保持 SharedPreferences 键值规范与即时夜间/语言模式联动，真机截图 `preferences_compose_screenshot.png`
- [x] 迁移 UidLogTool 至 Compose M3，优化为倒序卡片流、一键复制与日志清除确认，真机截图 `uid_log_compose_screenshot.png`
- [x] 编写各页面针对性 Compose UI 自动化测试套件与单元测试回归 (17/17 通过)
- [x] Release 构建验证 (体积 8.47 MB)，保持 master 和原有 tag 不变，未向远程推送

### 阶段 12：P12 剩余中低风险页面 Compose 现代化与文件体系收口 [complete]
- [x] 补齐 P11 遗留项：优化 NFC 动态广播注册生命周期（兼容 API 33+ `RECEIVER_NOT_EXPORTED`），真机贴卡/模拟卡信息渲染截图，Compose 自动化测试加固
- [x] 迁移 AccessConditionDecoder 至 Compose M3，保留矩阵解码算法与权限解析展示，真机截图 `access_condition_decoder_compose_screenshot.png`
- [x] 迁移 ValueBlocksToInt 至 Compose M3，保留转储数据块与整数互转呈现，真机截图 `value_blocks_to_int_compose_screenshot.png`
- [x] 迁移 FileChooser 至 Compose M3，严格对齐 Intent 契约（单选、多选、新建、删除、重命名与原有编辑器调用兼容），真机截图 `file_chooser_compose_screenshot.png`
- [x] 迁移 ImportExportTool 至 Compose M3，保留 SAF DocumentPicker 授权、内置目录导入导出逻辑，真机截图 `import_export_tool_compose_screenshot.png`
- [x] 深入评估 DiffTool 差异比对复杂度并给出架构决策（按需迁移或隔离保护）
- [x] 全量回归单元测试 (17/17) 与 Release APK 构建度量 (8.51 MB)，输出功能等价性报告与最终 Git 状态

### 阶段 13：P13 DiffTool 现代化、CloneUid 迁移与复杂编辑器混合架构评估 [complete]
- [x] 复核 NFC 动态广播的 RECEIVER_EXPORTED 必要性与安全性，汇总统计全量 Compose 仪器测试执行结果 (全工程共计 39 个自动化测试用例)
- [x] 完整迁移 DiffTool 至 Compose Material 3（MCDiffUtils 算法保真、Intent 契约、等宽对齐、字节差异高亮、扇区折叠、LazyColumn 虚拟列表），开展 1K/4K 性能测试并真机截屏 `diff_tool_compose_screenshot.png`
- [x] 迁移 CloneUidTool 界面与参数校验，保留 BCC 计算与魔术卡型号选择，绝不触发实体写卡，真机截屏 `clone_uid_compose_screenshot.png`
- [x] 开展 KeyEditor 与 DumpEditor 只读架构审计与性能分析，探索 Compose + 原生 View 混合架构原型设计，给出最终落地建议
- [x] 每项独立提交中文 Conventional Commit，执行构建、测试与真机验收，保持 master 与既有 Tag 不变，不推送远程

### 阶段 17：P16.2 KernelSU 风格 UI 2.0 全应用视觉与交互重构 [complete]
- [x] **Phase A：设计审计与规范制定**
  - [x] 深入分析 KernelSU Manager 现代 Android 设计语言（设计层次、卡片、设置项、状态、深浅色、信息密度）
  - [x] 全面审计 MCT 全部生产页面（特别是 Preferences、HelpAndInfo、About、MainMenu），输出问题矩阵
  - [x] 产出 `UI2_DESIGN_SPEC.md` 与 `UI2_AUDIT.md`
- [x] **Phase B：语言与主题底层重构 (最高优先级缺陷治理)**
  - [x] 定位并彻底修复 `custom_app_language` 与 `AppCompatDelegate.getApplicationLocales()` 同步缺陷（支持跟随系统、中文、英文）
  - [x] 重构 `MctTheme` 与 `MctComponents`：设计 KernelSU 风格配色、暗色层级与描边、Typography、间距、Shapes
  - [x] 建立标准 UI 2.0 组件：`MctTopAppBar`, `MctStatusCard`, `MctSettingGroup`, `MctSettingItem`, `MctSwitchItem`, `MctSelectionItem`, `MctSectionCard`, `MctActionButton`, `MctDangerButton`, `MctConfirmDialog`, `MctSelectionDialog`, `MctMonospaceField`, `MctEmptyState`
- [x] **Phase C：核心视觉页面重设计**
  - [x] Preferences：分组卡片（日常使用、读写与扫描、显示与语言）、UID 格式改为纵向/独立弹窗、Switch 右对齐、统一保存/取消
  - [x] HelpAndInfo：创建高质量简体中文离线帮助文档 `help_zh.html`，更新 `help.html` 采用现代响应式排版与暗色模式 CSS，按系统 Locale 动态加载
  - [x] About：全新现代 Compose Material 3 对话框/页面，展示版本、许可证、GitHub/文档链接、原作者致敬与 Kotlin Fork 声明
  - [x] MainMenu：KernelSU 状态卡片风格 Hero NFC Card、高优先级读写卡片、整洁管理分组、现代化工具箱入口
- [x] **Phase D：全局页面视觉统一与组件推广**
  - [x] 统一各个独立工具页面（BccTool, HexToAscii, ValueBlockTool, ValueBlocksToInt, DataConversionTool, AccessConditionTool, AccessConditionDecoder, TagInfoTool, UidLogTool, FileChooser, ImportExportTool, DiffTool, CloneUidTool）
- [x] **Phase E：专业页面适配与安全边界保护**
  - [x] KeyEditor：保留 AndroidView AppCompatEditText 核心，升级外壳顶栏与底部操作栏
  - [x] ReadTag 与 KeyMapCreator：现代状态与卡片，保护 NFC 扫描逻辑
  - [x] WriteTag：保留六模式表单与写卡状态机，升级 M3 外壳
  - [x] 严格禁止实体写卡、修改写卡状态机、修改 MCReader、修改生产 DumpEditor 逻辑
- [x] **Phase F：验收、回归测试与归档**
  - [x] `testDebugUnitTest` 单元测试回归 (18/18 PASS)
  - [x] `assembleDebug` 与 `assembleRelease` 构建验证 (BUILD SUCCESSFUL)
  - [x] 产出 `UI2_DESIGN_SPEC.md`, `UI2_AUDIT.md`, `UI2_IMPLEMENTATION_REPORT.md`, `UI2_ACCEPTANCE.md`
  - [x] 分阶段独立中文 Conventional Commits，确认 master 与 Tag 不可变，不推送远程，不发布 Release

### 阶段 18：P16.3 MCT UI 2.0 离线质量收口与人工验收准备 [complete]
- [x] **语言系统专项核查**：抽离 `LanguageHelper`，编写 `LanguageHelperTest` 6 项用例全部 PASS；跟随系统、中文、英文与旧版兼容性闭环验证
- [x] **UI 静态代码审查**：MainMenu, Preferences, HelpAndInfo, About 布局约束、文本溢出保护、深浅色、无障碍、系统 Insets 审查
- [x] **发布构建与清单隔离核验**：实测 Release Merged Manifest 排除 DumpEditorPrototype，核实 R8 保持 DEFERRED
- [x] **产物体积与哈希度量**：Debug APK (13,637,930 字节，已签名可直接装)，Release APK (9,090,507 字节，未签名)
- [x] **交付报告**：产出 `docs/p16/P16_3_FINAL_REPORT.md`，master 与基线 Tag 不变，不推送不发布
