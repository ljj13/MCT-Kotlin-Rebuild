# P15-B 第一期安全混合 UI 阶段报告

> 分支 `feature/compose-modernization`。本报告如实记录实际执行结果，不虚构真机验收。

## A. 实际完成情况

| 文件 | 变更 |
| :--- | :--- |
| `app/src/main/res/layout/activity_write_tag.xml` | 根节点由 `ScrollView` 改为纵向 `LinearLayout`，顶部新增 `ComposeView#composeViewWriteTagShell`，原 `ScrollView`（含六模式原生表单）作为第二子项以 `layout_weight=1` 承载滚动；**所有控件 ID、`android:onClick`、可见性、顺序保持不变** |
| `app/src/main/java/.../Activities/WriteTag.kt` | +10 行：新增 `ComposeView`/`MctTheme`/`WriteTagShell` 导入；`onCreate` 增 `supportActionBar?.hide()`（消除原生 ActionBar，避免重复顶栏）；末尾对 ComposeView `setContent { MctTheme { WriteTagShell(onBackClick = { finish() }) } }`。**未改动任何写卡函数、线程、Intent 契约** |
| `app/src/main/java/.../ui/writetag/WriteTagShell.kt` | 新增 M3 外壳 Composable：`MctTopAppBar`（标题 `Write Tag`、返回入口）+ `errorContainer` 色纯展示安全提示 |
| `app/src/main/res/values/strings.xml` | 新增 `write_tag_safety_notice` 文案 |
| `app/src/androidTest/.../WriteTagShellComposeTest.kt` | 新增纯 Compose 外壳测试（无 NFC、无写卡） |
| `app/src/androidTest/.../WriteTagComposeTest.kt` | 新增 Activity 级六模式/模式切换测试（不点击任何写/确认/格式化按钮） |
| `docs/p15/WRITE_TAG_CONTRACT.md` | 新增六模式完整交互契约（冻结基线对照） |

## B. 六模式等价矩阵

| 模式 | 保留 | 入口可达 | 输入行为 | 底层调用 |
| :--- | :---: | :---: | :---: | :---: |
| 写块（Write Block，默认） | 是 | 是 | 未改（扇区/块 number·2，数据 hex·32） | 未改 `writeBlock`（B 后 A，即使 B 成功仍试 A） |
| 写转储（Write Dump/Clone） | 是 | 是 | 未改（静态 AC、写厂商块选项） | 未改 `writeDump`（工作线程，单块重试 2 次） |
| 克隆 UID（Clone UID） | 是 | 是 | 未改（跳转 `CloneUidTool`） | 未改 |
| 出厂格式化（Factory Format） | 是 | 是 | 未改 | 未改 `createFactoryFormattedDump` |
| 值块（Value Block 增减） | 是 | 是 | 未改（增/减单选默认增） | 未改 `writeValueBlock`（B 失败才 A） |
| Transfer/Restore 值块 | 是 | 是 | 未改（暂存/目标扇区块、值·地址） | 未改 `runValueTransferRestore` |

选项数量、顺序、默认选中、字段启用/禁用逻辑、输入规则、校验消息、写入入口均未改变。

## C. 测试结果

| 套件 | 总数 | PASS | FAIL | SKIPPED / UNTESTED | 环境 |
| :--- | :---: | :---: | :---: | :---: | :--- |
| JVM `BaselineTest` | 18 | 18 | 0 | 0 | 本机 Gradle，`assembleDebug`+`testDebugUnitTest` 通过 |
| Compose 仪器 `WriteTagShellComposeTest` | 1 | 0 执行 | 0 | 1（编译通过，未运行） | 无红米设备 |
| Compose 仪器 `WriteTagComposeTest` | 3 | 0 执行 | 0 | 3（编译通过，未运行） | 无红米设备 |

- 新增 4 个仪器测试方法已 `assembleDebugAndroidTest` 编译通过；因红米未连接，**未执行**，一律不标 PASS。
- 真实硬件测试：0 项（无设备）。
- 无法验证：全部实体写卡路径（按设计保持 UNTESTED）。

## D. 真机验证

**未执行。** `adb devices` 当前仅在线 `3FYBB24914202624`（华为 MatePad，非本轮目标且明确不操作）；目标红米 `pnq47xf6899t4huc` 未连接。按要求完成全部可离线工作，不虚构验收，不擅自改用其他设备。

## E. 安全审计（静态）

- **重复写入**：新增外壳仅含静态 `MctTopAppBar` 与展示性提示，无任何可写回调，未新增可触发写入的按钮。
- **UI 自动触发危险操作**：Compose 状态仅 `onBackClick={finish()}`，不触发任何 NFC/写卡动作。
- **生命周期风险**：沿用原 `WriteTag` Activity 与 `onSaveInstanceState`（`write_manuf_block`、`dump_with_pos`），未新增自动重试或恢复写入；页面重建不自动发起写操作。
- **确认路径**：所有原生危险确认对话框与写前问题清单保留；外壳未新增绕过确认的入口。
- **进度对话框语义**：`writeDump` 的 AlertDialog 未改动，关闭对话框不等于终止写卡。

## F. APK 指标

| 构建 | 大小 | 结果 |
| :--- | :---: | :--- |
| Debug `app-debug.apk` | 13,888,776 字节 | 构建成功 |
| Release `app-release-unsigned.apk` | 9,028,033 字节 | 构建成功（未签名，未发布） |

> 未在本会话留存 P15-A 精确 APK 字节基线，故不做体积增减断言。

## G. Git 状态

- 分支：`feature/compose-modernization`
- `master`：`50593d9`（未变）
- Tag `v4.3.1-kotlin-migration-baseline`：`e156d4c`（未变）
- 本阶段提交：
  - `4514f86` docs(writetag): 冻结六种写入模式及完整交互契约
  - `20c5f74` refactor(writetag): 引入 Material 3 混合界面并保留原生写卡表单
  - `056e5f6` test(writetag): 补充无实体写入的界面与契约回归测试
- 未推送、未创建 Release、未合并、未新建 Tag。工作树仍有既往未跟踪临时文件（`*.png`/`*.txt`/`*.err`/`uidump.xml`/`hs_err_pid*.log`/`.kotlin/`），未纳入提交。

## H. 后续建议

- **P15-B 已完成**：M3 混合外壳实现、六模式契约冻结、非写卡测试编写并编译通过、JVM 18/18、Debug/Release 构建通过。
- **P15-B 尚未验证**：仪器测试执行、红米真机 UI 验收（六模式表单、模式切换、深浅色、旋转、IME、无障碍、滚动、Insets）、APK 体积对比、性能/内存单次测量。
- **P15-B 明确延期**：完整 Compose 表单替换、写入状态机重构（均需单独授权）。
- **P16 可继续**：待红米在线后运行 `WriteTagShellComposeTest`/`WriteTagComposeTest` 与既有隔离测试并留存截图/日志。
- **必须额外授权**：任何实体写卡测试、UID/Block0/SAK/ATQA/Gen2/Gen3 相关验证、以及触及写卡线程/连接/重试/密钥顺序的改动。
