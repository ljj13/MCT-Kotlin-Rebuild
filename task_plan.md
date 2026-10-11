# MCT-Kotlin-Rebuild 渐进式迁移计划

## 迁移目标
对 MifareClassicTool (MCT) 最新上游代码开展 Java → Kotlin 渐进式迁移。
保持现有 XML UI、功能、数据格式、包名、NFC 交互及兼容性不变，不引入额外架构重构。
重点保障空安全、Java 互操作、反射/序列化、异步任务、NFC 连接生命周期与异常语义。

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
