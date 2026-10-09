# MCT-Kotlin-Rebuild 架构与迁移审计报告 (Phase 0 Baseline Audit)

## 1. 概览与背景
- **项目目标**: 将 Mifare Classic Tool (Android) 现有 Java 源码分批、渐进式重构为 Kotlin 代码。
- **基线版本**: 基于 MifareClassicTool 最新上游代码 (v4.3.1, versionCode 70)。
- **核心原则**:
  1. 保持现有 XML UI、数据格式、包名与对外行为完全兼容；
  2. 不引入 Jetpack Compose、Miuix 或额外架构重组；
  3. 严格保障空安全、Java 互操作、反射/序列化一致性、线程模型及 NFC 异常语义；
  4. 优先完整 Kotlin 化；高风险模块若遇阻保留 Java 并记录原因；
  5. 分阶段推进，每批次保证全量编译与单元测试通过，并保留独立的中文 Conventional Commit。

---

## 2. 工程与 Gradle 配置审计
- **Gradle 版本**: 9.4.1 (通过 Gradle Wrapper 运行)
- **Android Gradle Plugin (AGP)**: 9.2.0
- **Kotlin Gradle Plugin**: 2.2.10
- **目标 SDK**: compileSdk = 35, minSdk = 19, targetSdk = 35
- **JVM 环境**: OpenJDK 21.0.2 (LTS)
- **第三方依赖情况**:
  - `androidx.core:core:1.13.1`
  - `androidx.preference:preference:1.2.1`
  - `androidx.appcompat:appcompat:1.6.1`
  - 依赖关系极其轻量纯粹，无其他复杂的非 AndroidX 依赖。
- **测试框架补充**:
  - 初始状态：无测试依赖、无测试目录 (`testDebugUnitTest NO-SOURCE`)。
  - 基线建立：在 `build.gradle` 中引入 `testImplementation "junit:junit:4.13.2"` 与 `testOptions.unitTests.returnDefaultValues = true`，并创建了纯逻辑算法单元测试基线 `BaselineTest.java`（涵盖 16 进制/字节数组转换、ASCII 编解码、BCC 校验、值块结构验证、大小端转换、Dump 校验、KeyFile 校验）。

---

## 3. 源码规模与类架构详细清单
共计 28 个 Java 文件（26 个业务逻辑类 + 2 个 `package-info.java`），代码总行数 12,631 行。

| 模块类别 | 类名 | 行数 | 职责说明 | 迁移风险/重点 |
|---------|------|------|----------|--------------|
| **基础接口/算法** | `IActivityThatReactsToSave.java` | 36 | 统一保存反应接口 | 极低 |
| **基础接口/算法** | `MCDiffUtils.java` | 85 | 对比 Dump 差异扇区与块 | 低（依赖 SparseArray） |
| **核心工具类** | `Common.java` | 2,149 | 全局工具类、常量定义、NFC 全局 Tag、Toast、权限、文件 IO、字节转换 | **高**（静态方法极多，需强 Java 互操作 `@JvmStatic` / `@JvmField`，保持空安全） |
| **独立工具** | `HelpAndInfo.java` | 38 | 帮助与关于界面 | 极低 |
| **独立工具** | `HexToAscii.java` | 67 | 16进制转 ASCII 转换器 | 低 |
| **独立工具** | `BccTool.java` | 87 | 4字节 UID BCC 计算器 | 低 |
| **独立工具** | `ValueBlocksToInt.java` | 142 | 批量值块转整型计算器 | 低 |
| **独立工具** | `ValueBlockTool.java` | 140 | 单值块解析与修改工具 | 低 |
| **独立工具** | `DataConversionTool.java` | 186 | 数据编解码与格式转换 | 低 |
| **独立工具** | `AccessConditionDecoder.java` | 277 | 访问控制位 (Access Conditions) 解码器 | 中 |
| **独立工具** | `AccessConditionTool.java` | 456 | 访问控制位生成与修改器 | 中 |
| **基类与通用** | `BasicActivity.java` | 68 | 所有 Activity 基类，负责 NFC 前台分发生命周期 | **中**（多 Activity 继承，生命周期回调） |
| **功能界面** | `FileChooser.java` | 377 | 文件选择器 Activity | 中 |
| **功能界面** | `Preferences.java` | 419 | 设置项 Activity（支持暗色模式、NFC 选项） | 中 |
| **功能界面** | `DiffTool.java` | 344 | Dump 对比 Activity | 中 |
| **功能界面** | `UidLogTool.java` | 128 | UID 历史日志查看器 | 低 |
| **功能界面** | `CloneUidTool.java` | 461 | 复制 UID 工具（Magic Tag 写入第 0 块） | 中 |
| **功能界面** | `TagInfoTool.java` | 298 | 标签原始参数与结构分析 | 中 |
| **功能界面** | `KeyEditor.java` | 419 | 密钥文件编辑器 | 中 |
| **功能界面** | `DumpEditor.java` | 1,010 | Dump 文件可视化与十六进制编辑器 | 中（交互复杂、数据结构绑定） |
| **功能界面** | `ImportExportTool.java` | 858 | 外部存储 Dump 与 Key 导入导出 | 中（Scoped Storage、URI/File IO） |
| **NFC 核心引擎** | `MCReader.java` | 1,200 | NFC MifareClassic 原始技术交互、扇区认证、数据读写、重试机制 | **高**（底层硬件通信、线程阻塞、异常语义） |
| **NFC 业务流程** | `KeyMapCreator.java` | 643 | 密钥映射构建引擎与进度 Activity（多线程认证） | **高**（多线程后台认证、Handler 消息） |
| **NFC 业务流程** | `ReadTag.java` | 138 | 读卡主流程 Activity | 中（与 MCReader、KeyMapCreator 紧密联动） |
| **NFC 业务流程** | `WriteTag.java` | 1,660 | 写卡、格式化、写特定块/值块主流程 Activity | **高**（复杂业务状态机、多模式写卡、回滚校验） |
| **入口与菜单** | `MainMenu.java` | 937 | 主菜单入口、Intent 分发、NFC 标签发现中心 | **高**（入口点、NFC 意图分发、权限检查） |
| **包元数据** | `package-info.java` (x2) | 8 | Javadoc 说明 | 极低 |

---

## 4. 关键技术点审计

### 4.1 NFC 生命周期模型
- `BasicActivity` 在 `onResume()` 调用 `Common.enableNfcForegroundDispatch(this)`，在 `onPause()` 调用 `Common.disableNfcForegroundDispatch(this)`。
- `MainMenu` 在 `onNewIntent(Intent intent)` 中接收 `NfcAdapter.ACTION_TECH_DISCOVERED`，调用 `Common.treatAsNewTag(intent, this)`。
- `Common.setTag(Tag)` 暂存当前活跃的 NFC 标签实例；子 Activity 在各自的 `onResume()` 或流程中读取 `Common.getTag()`。
- **迁移重点**:
  - 必须确保 `Common.getTag()` 和 `Common.setTag()` 的可空性与线程可见性（`Tag?`）。
  - 禁止在迁移中破坏 `Intent` 传递中的 Extra（`NfcAdapter.EXTRA_TAG` 等）。
  - `MifareClassic.get(tag)` 在部分设备上若返回 null 需严格保持现有的 fallback/提示语义。

### 4.2 线程模型与异步任务
- MCT 早期基于 `Thread` + `Handler` 模式设计：
  - 例如 `KeyMapCreator` 使用单独的 Worker Thread 循环验证扇区 KeyA / KeyB，通过 `Handler.sendMessage()` 向主线程通知进度并更新 ProgressDialog。
  - 例如 `WriteTag` 同样在 Worker Thread 中连接 `MCReader` 执行多扇区写入，通过 `Handler` 回传错误码与进度。
- **迁移重点**:
  - 保持 Thread + Handler 结构不变（或精准封装为 Kotlin 代码），不随意改写为 Coroutine，避免引入复杂的协程作用域生命周期取消与线程调度开销，防止底层 NFC 硬件超时产生意外 race condition。

### 4.3 序列化与数据传输 (Java 互操作)
- Activity 之间通过 Intent 传递数据结构：
  - `HashMap<Integer, HashMap<Integer, byte[]>>` (Key Map)
  - `ArrayList<String>` / `String[]` (Keys / Dumps)
  - `Tag` (Parcelable)
- **迁移重点**:
  - Kotlin 与 Java 集合类型的映射（如 `java.util.HashMap` 与 Kotlin `HashMap`），确保 Intent `putExtra` / `getSerializableExtra` 的签名完全一致。
  - 保持公共静态常量（如 `EXTRA_DUMP`, `EXTRA_KEYS`, `EXTRA_TAG`）为 `const val` 或 `@JvmField`。

### 4.4 异常语义
- NFC 操作普遍抛出 `IOException`、`SecurityException`、`TagLostException`。
- Kotlin 无受检异常（Checked Exception），但内部必须捕获并在必要时向外部正确抛出或处理，不得静默吞掉 `IOException`，否则会导致重试逻辑失效。

---

## 5. 迁移实施批次计划 (按依赖拓扑)
1. **批次 1 (P1-Foundation)**: `IActivityThatReactsToSave`, `MCDiffUtils`, `Common`
   - 目标：建立 Kotlin 工具与数据基石；提供完整的 `@JvmStatic` 保证后续未迁移 Java 类无缝调用。
2. **批次 2 (P2-LeafTools)**: `HelpAndInfo`, `HexToAscii`, `BccTool`, `ValueBlocksToInt`, `ValueBlockTool`, `DataConversionTool`, `AccessConditionDecoder`, `AccessConditionTool`
   - 目标：将无下游依赖的独立计算与工具页面全部迁移为 Kotlin。
3. **批次 3 (P3-BaseAndComplex)**: `BasicActivity`, `FileChooser`, `Preferences`, `DiffTool`, `UidLogTool`, `CloneUidTool`, `TagInfoTool`, `KeyEditor`, `DumpEditor`, `ImportExportTool`
   - 目标：基类、复杂编辑器与文件存储/首选项模块迁移。
4. **批次 4 (P4-NFCAndMain)**: `MCReader`, `KeyMapCreator`, `ReadTag`, `WriteTag`, `MainMenu`
   - 目标：核心 NFC 读写器、密钥映射创建、读写标签与主入口 Activity 迁移，处理 package-info。
5. **批次 5 (P5-Verification)**: 全面构建验证、覆盖率盘点、风险汇总。
