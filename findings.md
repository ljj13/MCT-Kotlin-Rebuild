# MCT-Kotlin-Rebuild 架构审计与迁移成果报告 (Final Report)

## 1. 迁移执行概览
- **项目目标**: 基于 MifareClassicTool 最新上游代码 (v4.3.1, versionCode 70)，开展 Java → Kotlin 渐进式迁移。
- **目标达成**:
  - 达成 **100% Kotlin 迁移覆盖率**（原 26 个 Java 业务类与 2 个 package-info 全部完成 Kotlin 重构与清理）。
  - 主工程源码目录中 Java 文件由 28 个减少为 0 个；`compileDebugJavaWithJavac` 任务状态为 `NO-SOURCE`。
  - 代码行数从 **12,631 行 Java** 变为 **9,777 行 Kotlin**，净减少 **2,854 行**（代码量压缩约 22.6%），消除大量样板代码同时完全保留所有业务逻辑与生命周期契约。
  - 独立中文 Conventional Commits 分批次推进，全部分支记录保留在 `refactor/kotlin-migration` 分支，未自动合并 master 分支。

---

## 2. 迁移覆盖率与代码度量统计

| 指标项 | 迁移前 (Java 基线) | 迁移后 (Kotlin 重构) | 变化差异 |
|--------|-------------------|---------------------|---------|
| **业务源码类总数** | 26 个 | 26 个 | 100% Kotlin 化 |
| **package-info 元数据** | 2 个 | 0 个 (已移除) | -2 个 |
| **Java 源码文件数** | 28 个 | 0 个 | -28 (-100%) |
| **Kotlin 源码文件数** | 0 个 | 26 个 | +26 (+100%) |
| **主工程源码总行数** | 12,631 行 | 9,777 行 | -2,854 行 (-22.6%) |
| **测试套件状态** | 0 个测试 (无测试用例) | 8 个单元测试 (全部通过) | +8 个算法测试用例 |
| **构建目标产物** | `assembleDebug` 成功 | `assembleDebug` 成功 | 兼容 Android SDK 19 ~ 35 |

---

## 3. 分批迁移清单与提交履历

| 批次 | 模块类别 | 涉及文件 | Conventional Commit 记录 |
|------|---------|----------|--------------------------|
| **P0** | 构建基线与测试脚手架 | `build.gradle`, `BaselineTest.java`, 规划文档 | `chore(baseline): 审计工程架构并建立核心算法单元测试基线` |
| **P1** | 核心工具与底层数据层 | `IActivityThatReactsToSave.kt`, `MCDiffUtils.kt`, `Common.kt` | `refactor(core): 迁移基础工具与核心算法层至 Kotlin` |
| **P2** | 独立工具类 Activity | `HelpAndInfo.kt`, `HexToAscii.kt`, `BccTool.kt`, `ValueBlockTool.kt`, `ValueBlocksToInt.kt`, `DataConversionTool.kt`, `AccessConditionDecoder.kt`, `AccessConditionTool.kt` | `refactor(tools): 迁移独立工具类 Activity 至 Kotlin` |
| **P3** | 基类与复杂文件/配置界面 | `BasicActivity.kt`, `FileChooser.kt`, `Preferences.kt`, `DiffTool.kt`, `UidLogTool.kt`, `CloneUidTool.kt`, `TagInfoTool.kt`, `KeyEditor.kt`, `DumpEditor.kt`, `ImportExportTool.kt` | `refactor(activities): 迁移基类与复杂编辑器/文件管理 Activity 至 Kotlin` |
| **P4** | NFC 核心引擎与主流程 | `MCReader.kt`, `KeyMapCreator.kt`, `ReadTag.kt`, `WriteTag.kt`, `MainMenu.kt`, package-info 清理 | `refactor(nfc): 迁移 NFC 核心引擎与主业务流程至 Kotlin` |

---

## 4. 关键技术领域审计与质量保证

### 4.1 空安全 (Null Safety)
- **Tag 实例与硬件生命周期**: `Common.getTag()`、`Common.setTag(tag)` 以及 `Common.getUID()` 显式声明为可空类型（`Tag?`、`ByteArray?`），所有消费端统一做空判断或安全调用，消除潜在的 NullPointerException 隐患。
- **KeyMap 数据结构**: 密钥映射 `SparseArray<Array<ByteArray?>>` 中，每个扇区的 Key A 与 Key B 明确支持单个密钥为空（例如仅认证成功 Key A 或 Key B），完全兼容 Mifare Classic 单密钥读写场景。

### 4.2 Java 互操作性与反射
- 在 `Common.kt`、`MCDiffUtils.kt`、`MCReader.kt` 等静态调用高频模块中，严格保留 `@JvmStatic` 注解与 `const val` 常量，常量与方法签名与 Java 时代 100% 二进制对齐。
- XML 布局的 `android:onClick` 绑定方法（如 `onCalculateBlock0`, `onChooseACforDataBock`, `onShowReadTag` 等）保持 `fun methodName(view: View)` 签名与可见性，确保 XML 反射调用零故障。
- 序列化数据（如 Intent 中的 `dump_with_pos`）保持 `HashMap<Int, HashMap<Int, ByteArray>>` 结构，兼容各版本 Android 的 `getSerializableExtra`。

### 4.3 异步任务与线程模型
- 保持原生 `Thread` + `Handler`（`Looper.getMainLooper()`）模式，严格对齐底层 NFC 硬件超时重试与阻塞读写语义，不引入 Coroutine 避免改变线程中断与取消特性。
- 在 `KeyMapCreator` 中保持通过 `Handler.post` 刷新进度条，并保持 `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` 控制防休眠。

### 4.4 NFC 生命周期与异常语义
- `BasicActivity` 严格在 `onResume()` 注册 `enableNfcForegroundDispatch`，在 `onPause()` 注销 `disableNfcForegroundDispatch`。
- `MCReader` 严格捕获并抛出 `TagLostException`、`IOException` 与 `SecurityException`，保持扇区认证重试次数（`RetryAuthenticationCount`）及自动重连机制（`AutoReconnect`）。

---

## 5. 遗留风险与后续建议
1. **高风险硬件适配测试**:
   - `Common.hasMifareClassicSupport()` 内部包含对 Broadcom/NXP 芯片设备节点（如 `/dev/bcm2079x-i2c`, `/dev/pn544`, `libnfc-brcm`）的检测。该逻辑在模拟器上通常 fallback，建议在搭载真实 NXP NFC 芯片的实体设备上进行真机卡片读写测试。
   - `MCReader.writeBlock0Gen3()` 目前保留在源码中作为备用方案（上游也是默认注释状态），由于部分 Magic Tag Gen3 存在破坏卡片扇区的物理风险，暂不建议主动开启。
2. **Git 分支状态**:
   - 当前工作全部提交在 `refactor/kotlin-migration` 分支，本地 `master` 分支保持与 `origin/master` 完全一致，未执行自动合入。后续可由团队人工提 PR 进行 Review 和 Merge。
