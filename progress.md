# 迁移进度日志 (progress.md)

## OPPO P15-A 续验（未收口）
- OPPO X8OVSG49IR7X7L4D / PLB110 / Android 16：已安装当前 Debug；真实 std.keys 映射读取 16 扇区、64 块进入生产 DumpEditor。
- 独立冻结 P7 50593d9 重读同一卡，两份完整转储 SHA-256 一致；生命周期第一版修正包复读也一致。最后新增 pause busy gate 修正已构建/安装，但尚未再次读卡复验。
- 隔离 UI：KeyMapCreator、ReadTag、P14 Native 1K / Compose 4K 修正测试选择器/完整文本断言后 PASS，初次 FAIL 保留；仅为 UI 样本断言，不代表 NFC worker 生命周期通过。
- 18 项 JVM 算法测试通过。空闲取消、空闲离页返回通过；扫描中取消尝试实际 tagNotPresent，未覆盖。后续用户回复“好了”但手机已锁屏，近端日志未找到对应 MCReader/取消证据，仍 UNTESTED。
- 待验证扫描中取消/离页、断连、重建及重复贴卡。无写卡、UID/Block 0 修改或 Gen3 操作；不进入 P15-B。


| 时间 | 阶段 | 操作 | 状态 | 产物 / 说明 |
|------|------|------|------|-------------|
| 2026-10-10 | 准备 | 克隆仓库与创建工作分支 `refactor/kotlin-migration` | 完成 | 确保不直接向 master 分支合并 |
| 2026-10-10 | 阶段 0 | 验证 Gradle 9.4.1 与 AGP 9.2.0 环境，首编译成功 | 完成 | `assembleDebug` 成功耗时 2m 57s |
| 2026-10-10 | 阶段 0 | 源码与依赖全面审计 | 完成 | 识别 28 个 Java 文件，共 12,631 行代码，完成依赖、生命周期与异常审计 |
| 2026-10-10 | 阶段 0 | 配置测试套件与建立纯逻辑单元测试基线 | 完成 | 引入 JUnit 4 并完成 `BaselineTest.java`，8 个基线测试全部通过 |
| 2026-10-10 | 阶段 1 | 迁移基础工具与模型层 (IActivityThatReactsToSave, MCDiffUtils, Common) | 完成 | 解决 JVM 11 target 对齐与 assign flow，通过 8 个单元测试与全量打包 |
| 2026-10-10 | 阶段 2 | 迁移独立工具 Activity (HelpAndInfo, HexToAscii, BccTool, ValueBlock*, DataConversion, AccessCondition*) | 完成 | 严格保持 XML onClick 与常量绑定，8 个类全部转为 Kotlin 并通过测试与打包 |
| 2026-10-10 | 阶段 3 | 迁移基类与复杂编辑器/文件管理 Activity (BasicActivity, FileChooser, Preferences, DiffTool, UidLogTool, CloneUidTool, TagInfoTool, KeyEditor, DumpEditor, ImportExportTool) | 完成 | 10 个核心 Activity 完成迁移，严格保证状态持久化、Scoped Storage 与对话框逻辑，测试与打包 100% 成功 |
| 2026-10-10 | 阶段 4 | 迁移 NFC 核心引擎与主流程 (MCReader, KeyMapCreator, ReadTag, WriteTag, MainMenu) | 完成 | 核心硬件通信与读写状态机全部转为 Kotlin，主工程达成 100% Kotlin 覆盖率，Java 编译为 NO-SOURCE |
| 2026-10-10 | 阶段 5 | 全面验证、覆盖率与度量统计、完成最终审计成果汇报 | 完成 | 全部 26 个类转换完毕，Java 文件 0 个，行数缩减 22.6%，测试全绿，Git 状态已确认 |
| 2026-10-10 | 质量审计 | 针对 master 进行全方位语义差异深度核查 | 完成 | 覆盖字节比较/拷贝、空安全、符号扩展、XML 反射、序列化、线程与生命周期 |
| 2026-10-10 | 质量审计 | 规范化 AGP 9.2 内置 Kotlin 配置 | 完成 | 移除废弃选项与外部插件，AGP 9.2 原生内置编译，消除 Variant API 告警 |
| 2026-10-10 | 质量审计 | 修复 MCReader 潜在 NPE 与 isMifareClassic 语义 | 完成 | 解决非 MFC 卡片可能触发平台强转 NPE 隐患，提交 `fix(nfc)` |
| 2026-10-10 | 质量审计 | 扩充针对性回归测试用例至 17 个并全量验证通过 | 完成 | 覆盖 AC 控制位矩阵、大数运算、值块编码、块映射与块 0 校验，提交 `test(core)` |
| 2026-10-11 | P6 真机验收 | 红米 Note 8 Pro / Android 16 真机基础功能与 20 个 Activity 导航 | 完成 | 0 崩溃，拉取基线与验证应用运行时与文件系统 |
| 2026-10-11 | P7 物理读卡 | OPPO Find X8s+ 真实 MIFARE Classic 物理卡片只读认证与 Dump 读取 | 完成 | 成功识别 UID `0BDC504F` (MFC 1K)，读取扇区 0..15 全部数据并截图留存，无危险写入 |
| 2026-10-11 | P7 最终收口 | 最终测试矩阵整理、显式 merge commit 合并至 master、构建并打 Tag | 完成 | 完成收口与合并，创建基线 Tag `v4.3.1-kotlin-migration-baseline` |
| 2026-10-11 | P8 试点 | 在新分支 `feature/compose-modernization` 开展 Compose M3 试点 | 完成 | 配置 Compose 依赖、minSdk 21，建立 MctTheme，迁移 BccTool 并真机截图验收 |
| 2026-10-11 | P9 深度演进 | 联动 MctTheme 与主题设置、测定 Release APK 体积、建立组件规范库 | 完成 | 统一组件间距排版，测定 Release APK 体积 8.41 MB (增量 4.5 MB) |
| 2026-10-11 | P9 工具迁移 | 陆续迁移 HexToAscii, ValueBlockTool, DataConversionTool, AccessConditionTool | 完成 | 4 个独立工具全部迁移至 Compose Material 3，真机截图验收并通过全量测试 |
| 2026-10-11 | P10 首页现代化 | 迁移 MainMenu 首页至 Compose Material 3，整合 9 项工具弹窗与 NFC 状态 | 完成 | 完整保留状态机与生命周期，截图验收，自动化测试全绿，Release APK 8.43 MB |
| 2026-10-11 | P11 状态与测试强化 | MainMenu 广播动态监听 NFC 切换、弹窗高度约束与无障碍标签 | 完成 | 优化测试规则，红米 Note 8 Pro 仪器测试通过 |
| 2026-10-11 | P11 页面迁移 | 迁移 HelpAndInfo 至 Compose M3 并接入离线 WebView | 完成 | 截取并核对 `help_and_info_compose_screenshot.png` |
| 2026-10-11 | P11 页面迁移 | 迁移 TagInfoTool 至 Compose M3，保留 NFC 前台分发与卡型识别 | 完成 | 截取并核对 `tag_info_compose_screenshot.png` |
| 2026-10-11 | P11 页面迁移 | 迁移 Preferences 至 Compose M3，严格对齐 SharedPreferences 数据字段 | 完成 | 验证设置落盘与即时生效，截取 `preferences_compose_screenshot.png` |
| 2026-10-11 | P11 页面迁移 | 迁移 UidLogTool 至 Compose M3，实现时间倒序卡片流与一键清除 | 完成 | 截取并核对 `uid_log_compose_screenshot.png` |
| 2026-10-11 | P11 收口验证 | 全量回归纯逻辑与仪器测试，测定 Release APK 体积 (8.47 MB) | 完成 | 单元测试 17/17 通过，分支独立干净，未动 master |
| 2026-10-11 | P12 状态加固 | 优化 NFC 动态广播导出兼容性，真机验收真实卡片 UID 参数展示 | 完成 | 截取并核对 `tag_info_real_card_screenshot.png`，提交 `fix(nfc)` |
| 2026-10-11 | P12 页面迁移 | 迁移 AccessConditionDecoder 至 Compose M3，实现扇区控制矩阵可视化 | 完成 | 截取并核对 `access_condition_decoder_compose_screenshot.png` |
| 2026-10-11 | P12 页面迁移 | 迁移 ValueBlocksToInt 至 Compose M3，优化值块与整数互转呈现 | 完成 | 截取并核对 `value_blocks_to_int_compose_screenshot.png` |
| 2026-10-11 | P12 页面迁移 | 迁移 FileChooser 至 Compose M3，严格对齐 Intent 契约与文件 CRUD 操作 | 完成 | 截取并核对 `file_chooser_compose_screenshot.png` |
| 2026-10-11 | P12 页面迁移 | 迁移 ImportExportTool 至 Compose M3，保持格式转换与 SAF 备份兼容 | 完成 | 截取并核对 `import_export_tool_compose_screenshot.png` |
| 2026-10-11 | P12 评估与收口 | 深入评估 DiffTool 差异比对复杂度，建议保持稳定，Release APK 8.51 MB | 完成 | 单元测试 17/17 全部通过，保持 master 与既有 Tag 不变 |
| 2026-10-11 | P13 安全复核 | 深入复核 NFC 动态广播 RECEIVER_EXPORTED 必要性与安全性 | 完成 | 统计全量 39 个自动化测试用例，安全审计通过 |
| 2026-10-11 | P13 页面迁移 | 完整迁移 DiffTool 至 Compose M3，实现字节级高亮与扇区折叠 | 完成 | 1K/4K 性能测试通过，截取 `diff_tool_compose_screenshot.png` |
| 2026-10-11 | P13 页面迁移 | 迁移 CloneUidTool 界面与参数校验，保留 BCC 计算与魔术卡选择 | 完成 | 零实体写卡安全防线通过，截取 `clone_uid_compose_screenshot.png` |
| 2026-10-11 | P13 架构评估 | 开展 KeyEditor 与 DumpEditor 只读架构审计，提出混合架构原型设计 | 完成 | 形成详细评估报告与演进建议，Release APK 8.53 MB |
| 2026-10-14 | P15-B.0 | 冻结 WriteTag 六模式完整交互契约并对照基线 | 完成 | 提交 `4514f86`，产物 `docs/p15/WRITE_TAG_CONTRACT.md` |
| 2026-10-14 | P15-B.1 | 引入 M3 混合外壳并保留原生写卡表单 | 完成 | 提交 `20c5f74`，新增 `WriteTagShell.kt`，六模式/写卡业务零改动 |
| 2026-10-14 | P15-B.2 | 补充无实体写入的界面与契约回归测试 | 完成（编译） | 提交 `056e5f6`，新增 4 仪器测试；JVM 18/18；红米未连接未运行 |
| 2026-10-14 | P15-B.4/6 | Debug/Release 构建与阶段报告 | 完成 | Debug 13,888,776 B、Release 9,028,033 B；`docs/p15/P15B_REPORT.md` |
| 2026-10-14 | P15-B.1 真机 | 红米 shell 无响应，设备验收 DEFERRED | 延期 | 4 次 adb 命令挂起；未安装/截图/跑测试，不标 PASS |
| 2026-10-14 | P16 前置 | 全局 UI / 顶栏 / 测试数 / 构建警告 / 未跟踪文件审计 | 完成 | `docs/p15/P16_PREQUALITY.md`；JVM 18/18，仪器 56 方法 0 执行 |
| 2026-10-14 | P15-B.1 真机 | 红米恢复：APK 装好、MainMenu 正常；WriteTag 验收受环境阻断 | 阻断 | `input` 注入被拒 + 仪器测试 API36 挂起；`docs/p15/redmi/DEVICE_ACCEPTANCE_ROUND1.md` |
| 2026-10-14 | P15-B.1 真机 | 用户开启 MIUI 安全设置后完成 WriteTag 手动 UI 验收 | 完成 | M3 外壳+六模式+深浅色/旋转/滚动/返回均截图佐证；仪器测试仍被 MIUI 拦截（`DEVICE_ACCEPTANCE_FINAL.md`） |
| 2026-10-14 | P16 收口 | 全应用现代化质量审计 + 功能等价 + 测试归档 + 构建审计 | 完成 | 结论 PARTIAL；产出 `P16_FINAL_REPORT.md`；.gitignore 卫生修复 |
