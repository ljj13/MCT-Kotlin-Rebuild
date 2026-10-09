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

### 阶段 3：基础 Activity 与高级工具/文件管理迁移 (Base & Complex Activities) [in_progress]
- [ ] `BasicActivity.java` → `BasicActivity.kt`（所有 Activity 基类，处理 NFC 前台调度与公共菜单）
- [ ] `FileChooser.java` → `FileChooser.kt`
- [ ] `Preferences.java` → `Preferences.kt`
- [ ] `DiffTool.java` → `DiffTool.kt`
- [ ] `UidLogTool.java` → `UidLogTool.kt`
- [ ] `CloneUidTool.java` → `CloneUidTool.kt`
- [ ] `TagInfoTool.java` → `TagInfoTool.kt`
- [ ] `KeyEditor.java` → `KeyEditor.kt`
- [ ] `DumpEditor.java` → `DumpEditor.kt`
- [ ] `ImportExportTool.java` → `ImportExportTool.kt`
- [ ] 编译验证与单元测试
- [ ] 中文 Conventional Commit (`refactor(activities): ...`)

### 阶段 4：NFC 核心引擎与主流程迁移 (Core NFC Engine & Main Activities) [pending]
- [ ] `MCReader.java` → `MCReader.kt`（核心 NFC 扇区认证、数据读写、转码与异常处理）
- [ ] `KeyMapCreator.java` → `KeyMapCreator.kt`
- [ ] `ReadTag.java` → `ReadTag.kt`
- [ ] `WriteTag.java` → `WriteTag.kt`
- [ ] `MainMenu.java` → `MainMenu.kt`
- [ ] 处理 `package-info.java`
- [ ] 编译验证与单元测试
- [ ] 中文 Conventional Commit (`refactor(nfc): ...`)

### 阶段 5：全面验证、覆盖率盘点与总结汇报 [pending]
- [ ] 全量 `assembleDebug` 与 `testDebugUnitTest` 回归
- [ ] 统计 Kotlin 迁移率 (类数量、代码行数变化)
- [ ] 遗留风险审计（高风险模块审查、异常语义保持、Android 生命周期契约）
- [ ] Git 状态确认（保留在 `refactor/kotlin-migration` 分支，绝不合入 master）
- [ ] 生成最终审计与迁移成果报告
