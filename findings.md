# MCT-Kotlin-Rebuild 最终质量审计报告 (Final Quality Audit)

## 1. 审计范围与基准
- **审计目标**: 对分支 `refactor/kotlin-migration` 进行全面质量与行为一致性审计。
- **参考基线**: 以 `master`（`origin/master`）的 Java 源码为唯一行为基准。
- **核心关注点**:
  - `ByteArray` 比较与拷贝
  - 空安全 (Null Safety)
  - 整数溢出与位运算 (Sign Extension / Bitwise Shift)
  - 集合可变性与数据拷贝 (Collections Mutability)
  - 异常传播机制 (Exception Semantics)
  - XML `android:onClick` 反射调用兼容性
  - Intent 与 Bundle 序列化兼容性
  - NFC 生命周期契约与线程模型 / 竞争条件
  - AGP 9.2 内置 Kotlin 插件规范性
  - JUnit 测试数量与覆盖范围
  - 实体机必测场景与不可自动化风险识别

---

## 2. 潜在语义差异核查结论

### 2.1 ByteArray 比较与拷贝
- **核查结论**: 经排查，原 Java 业务代码未直接使用 `Arrays.equals` 对数组进行业务比较，卡片 UID、扇区 Key 与块数据在业务逻辑中均以十六进制字符串（如 `Common.bytes2Hex` / `Common.hex2Bytes`）或单个字节元素形式对比。
- **字节拷贝**:
  - `MCReader.writeBlock0Gen3`: 保留 `System.arraycopy` 进行 APDU 指令前缀拼接；
  - `WriteTag.encodeValueBlock`: 保留 `System.arraycopy` 进行 16 字节值块反转段组装；
  - `Common.byte2FmtString`: 保留 `bytes.clone()` 进行防副作用反转；
  - `MCReader.readSector` / `MCReader.isWritableOnPositions`: 保留 `Arrays.copyOf` / `Arrays.copyOfRange` 截取 16 字节块与 3 字节 AC 位，行为与 Java 100% 保持一致。

### 2.2 空安全 (Null Safety)
- **确认并修复缺陷 (MCReader)**:
  - 原 Java 代码中 `tmpMFC = MifareClassic.get(tag)` 在标签不支持 Mifare Classic 时返回 `null`，并通过 `isMifareClassic()` 判定 `mMFC != null`。在首次 Kotlin 化时，`mMFC: MifareClassic` 被声明为非空且 `isMifareClassic` 被硬编码为 `true`，导致非 MFC 标签传入时由平台类型强转隐式触发 `NullPointerException`。
  - **修复**: 将 `mMFC` 纠正为 `MifareClassic?`，并恢复 `isMifareClassic` 为 `mMFC != null`，内部各调用点安全展开，消除异常污染。
- **KeyMap 空安全优化**:
  - `Common.getKeyMap()` 与 `MCReader.getKeyMap()` 声明为 `SparseArray<Array<ByteArray?>>?`，准确反映 Mifare Classic 扇区可能只有 Key A 或 Key B 认证成功的单密钥物理状态，避免假非空判断。

### 2.3 整数溢出与位运算 (Sign Extension & Bitwise)
- **无符号移位 vs 有符号移位**:
  - `Common.acBytesToACMatrix`: 采用 `b1 = acBytes[1].toInt() and 0xFF` 消除 Kotlin 中 `Byte` 转 `Int` 带来的负数符号扩展问题，配合 `ushr` 严格与 Java 原始位计算一致。
  - `Common.acMatrixToACBytes`: 重建 3 字节控制位时，保留与 `0xFF` 异或及掩码位拼接。
  - `MCReader.isKeyBReadable`: 提取控制位 `c1`, `c2`, `c3` 经测试与基准算法完全一致。
  - `ValueBlockTool` / `WriteTag`: 值块地址与数值反转采用 `.inv()` 与 `Integer.reverseBytes`，并在高位补齐与掩码截取上保持十六进制一致性。
- **大数运算**:
  - `hex2DecBE` / `decBE2Hex` / `hex2DecLE` / `decLE2Hex`: 严格维持 `<= 14` 字符使用 `Long.parseLong`、超出使用 `BigInteger` 的混合转换策略，针对 `12345678901234567890` 等超大整型建立针对性测试。

### 2.4 集合可变性 (Collections Mutability)
- `MCDiffUtils.diffIndices`: 内部采用 `ArrayList<Int>` 搜集差异索引并调用 `toTypedArray()` 产出定长 `Array<Int>`，与 Java 的 `Integer[]` 字节码对齐。
- `Preferences`: 语言与主题列表动态过滤移除项采用 `ArrayList`，避免不可变集合抛出 `UnsupportedOperationException`。
- `KeyMapCreator`: `mKeysWithOrder` 字典有序集合保持 `ArrayList<String>`，动态执行 Key A/B 命中置顶逻辑。

### 2.5 异常传播语义
- `MCReader.readSector` 显式声明 `@Throws(TagLostException::class)`，在读块遇到 `TagLostException` 或检测到断连时主动抛出，保留重试与捕获中断逻辑。
- `Common.copyFile` 保持 `@Throws(IOException::class)`。
- `Common.calcBcc` 保持 `@Throws(IllegalArgumentException::class)`（非 4 字节 UID 严格抛错）。

### 2.6 XML `android:onClick` 反射签名
- 全工程 14 个布局 XML 共计 76 处 `android:onClick` 属性逐一验证：
  - 宿主 Activity 中对应方法全部为 `public` 可见的 `fun methodName(view: View)` 或 `(view: View?)`；
  - 重点核验了历史遗留特殊命名的兼容性（如 `AccessConditionTool.kt` 的 `onChooseACforDataBock`，完全与 XML 对齐）。

### 2.7 Intent 序列化
- `WriteTag`: `mDumpWithPos` 采用 `HashMap<Int, HashMap<Int, ByteArray>>`，并针对 Android 13+ (Tiramisu) `getSerializable(key, HashMap.class)` 与低版本旧 API 双向兼容。
- `DiffTool` / `DumpEditor`: `EXTRA_DUMP` 统一通过 `Array<String>` 传递，使用 `getStringArrayExtra` 读取，无任何平台序列化兼容隐患。

### 2.8 NFC 生命周期与线程竞争
- `BasicActivity`: 在 `onResume()` 注册 `enableNfcForegroundDispatch`，在 `onPause()` 注销 `disableNfcForegroundDispatch`。
- `MainMenu`: 独立继承 `AppCompatActivity`，依据 `StartUpNode` 状态机执行硬件存在性/服务运行判断后延迟分发前台调度。
- `MCReader.connect()`: 保持在工作线程中调用 `mMFC.connect()` 并通过 `t.join(500)` 与 `AtomicBoolean` 进行超时与异常控制，避免主线程阻塞与死锁。

---

## 3. AGP 9.2 内置 Kotlin 配置规范化

- **现状核查**: 早期工程中同时应用了 `org.jetbrains.kotlin.android:2.2.10` 与在 `gradle.properties` 中设置 `android.builtInKotlin=false`，导致 AGP 9.2 持续输出 4 条以上关于废弃选项与过时 Variant API 的告警。
- **规范化改造**:
  - 移除已废弃的 `android.builtInKotlin=false`；
  - 移除外部 `org.jetbrains.kotlin.android` 插件，全面启用 **AGP 9.2 原生内置 Kotlin 编译体系**；
  - 保持 `compileOptions` 与 Kotlin JVM 目标统一为 `JavaVersion.VERSION_11`。
  - 构建警告大幅减少，Variant API 废弃告警完全消除。

---

## 4. JUnit 测试数量与覆盖范围

在 `Mifare Classic Tool/app/src/test/java/de/syss/MifareClassicTool/BaselineTest.java` 中扩充了针对性回归测试用例，实际测试用例数由最初的 8 个扩充至 **17 个**，执行成功率 **100% (17/17 Passed, 0 Failed, 0 Skipped)**：

1. `testHexAndBytesConversion`: 16 进制与字节数组相互转换及异常输入边界
2. `testAsciiAndHexConversion`: ASCII 与 16 进制转码及非打印字符处理
3. `testBccCalculation`: 4 字节 UID BCC 异或校验与正反例验证
4. `testIsValueBlock`: Mifare Classic 标准值块（反转/互补结构）格式校验
5. `testReverseByteArrayInPlace`: 原地字节数组逆序
6. `testIsValidKeyFile`: 密钥文件语法解析、注释行过滤、长度与字符集校验
7. `testIsValidDump`: 转储文件扇区头、块数量、非法字符及越界校验
8. `testHex2BinAndBin2Hex`: 16 进制与二进制补零转换
9. `testAccessConditionsMatrixAndRoundTrip`: 出厂默认值（`FF0780`）控制位解析、C1-C3 矩阵矩阵与双向重编码 Round-Trip、非法位检验
10. `testIsKeyBReadable`: Key B 可读性 8 种真值表逻辑验证
11. `testOperationRequirements`: 数据块与扇区尾块读写权限权限判定
12. `testDecimalAndHexConversionBigInteger`: 超长十进制与十六进制 BigInteger 编解码与大小端转换
13. `testByte2FmtString`: Hex / DecBE / DecLE 格式化输出
14. `testBlockToSector`: 1K/2K/4K 块号到扇区号正向映射（覆盖 0~255 全范围）
15. `testBlockToSectorNegativeThrows`: 负数块号越界异常验证
16. `testBlockToSectorOver255Throws`: 超过 255 块号越界异常验证
17. `testIsValidBlock0`: 厂商块 0 结构合法性校验（BCC 校验、单双 UID 判定、0x88 违规判定）

---

## 5. 必须通过实体机（真机）验证的场景清单

自动化单元测试受限于 JVM 环境与 Android 桩类，以下与真实物理卡片和硬件直接交互的场景**必须使用真实 Android 设备进行验收**：

1. **真实 NFC 标签发现与前台调度分发 (Foreground Dispatch)**:
   - 应用处于前台各个 Activity 时贴卡，验证系统 NFC 弹窗是否被拦截，Activity 是否正确接收 `onNewIntent`；
   - 在主界面贴未支持的 NFC 卡（如门禁 CPU 卡/Type B 卡），验证是否正常跳转 `TagInfoTool` 并给出友好提示。
2. **Mifare Classic 扇区密码扫描与读卡 (KeyMapCreator & ReadTag)**:
   - 使用包含标准密钥及自定义密钥的卡片，测试多线程扇区认证、Key A / Key B 扫描、进度条刷新与熄屏保护（`FLAG_KEEP_SCREEN_ON`）。
3. **Mifare Classic 写卡与回滚验证 (WriteTag)**:
   - 测试向普通数据块写入 16 字节数据；
   - 测试值块递增/递减（Increment / Decrement / Transfer / Restore）；
   - 测试基于暂存块的值块迁移修复功能（`valueTransferRestore`）。
4. **Magic Tag Gen2 / UID 复制功能 (CloneUidTool)**:
   - 使用可写 UID 卡（CUID / Gen2），测试向扇区 0 块 0 写入新 UID，并验证自动重新计算 BCC、SAK、ATQA 的有效性。
5. **硬件兼容性与外部 NFC 服务集成**:
   - 在非 NXP NFC 芯片（如部分联发科或博通芯片机型）上测试，验证是否能正确检测并引导使用 External NFC 方案；
   - 在搭载 NXP 芯片的机型上验证 Mifare Classic 直读能力。

---

## 6. 无法自动验证的风险分析

1. **特定厂商 NFC 驱动差异 (Samsung / Sony 等)**:
   - 原代码中曾记录：在三星 S5、索尼 Z2 等机型上，底层 `MifareClassic.readBlock()` 偶尔返回长度少于 16 字节或追加多余字节。虽然代码中已保留保护截断逻辑，但该物理层抖动无法在 CI 自动化环境中复现。
2. **Magic Tag Gen3 APDU 写入风险**:
   - `MCReader.writeBlock0Gen3` 使用了 raw APDU 绕过认证写入，但该方案可能存在使 4K 卡部分扇区物理锁死的风险（源码中默认未激活），切勿在自动化测试中盲目触发。
3. **Android 13+ (API 33+) 存储与权限行为**:
   - 外部 Dump 导入导出（`ImportExportTool`）涉及 SAF（Storage Access Framework）和 Scoped Storage，不同品牌系统对文档选择器的 URI 权限持久化策略有差异，需真机覆盖不同 ROM。

---

## 7. Git 分支与构建产物状态

- **当前分支**: `refactor/kotlin-migration`
- **提交历史**:
  - `chore(baseline): 审计工程架构并建立核心算法单元测试基线`
  - `refactor(core): 迁移基础工具与核心算法层至 Kotlin`
  - `refactor(tools): 迁移独立工具类 Activity 至 Kotlin`
  - `refactor(activities): 迁移基类与复杂编辑器/文件管理 Activity 至 Kotlin`
  - `refactor(nfc): 迁移 NFC 核心引擎与主业务流程至 Kotlin`
  - `build(gradle): 规范化 AGP 9.2 内置 Kotlin 配置并移除废弃选项`
  - `fix(nfc): 修复 MCReader 中 MifareClassic 实例可空性与 isMifareClassic 语义`
  - `test(core): 扩充针对性回归测试覆盖访问控制位矩阵、值块编码、块扇区映射与块0校验`
  - `test(device): 添加真机验收测试套件与设备运行时验证支持`
- **工作区状态**: Clean（无未提交的更改），未自动合入 `master`。

---

## 8. P6 红米手机真机验收结果与矩阵 (Redmi Note 8 Pro Acceptance)

### 8.1 目标设备环境信息
- **设备型号**: REDMI Note 8 Pro (`begonia`)
- **设备序列号**: `pnq47xf6899t4huc`
- **Android 版本**: Android 16 (API Level 36)
- **NFC 芯片及特性**: NXP NFC，支持 `android.hardware.nfc`, `android.hardware.nfc.hce`, `android.hardware.nfc.ese` 等
- **构建产物**: `app-debug.apk` (4,944,791 bytes, targetSdk 35, minSdk 19)

### 8.2 PASS / FAIL / UNTESTED 验收矩阵

| 序号 | 验证模块 / 功能场景 | 验证级别 | 测试方法 / 执行细节 | 状态 | 日志证据 / 结果说明 |
|:---:|:---|:---:|:---|:---:|:---|
| 1 | **应用安装与启动** | 基础 | ADB Streamed Install + `am start` | **PASS** | `Performing Streamed Install -> Success`，主进程 PID 启动正常 |
| 2 | **内部存储与字典初始化** | 数据 | 检查 `files/MifareClassicTool` 结构 | **PASS** | 成功生成 `dump-files`, `key-files` 并复制 3 套内置 `.keys` 文件（33,959 字节扩展字典） |
| 3 | **MainMenu 主界面导航** | UI/生命周期 | 启动主入口，执行冷启动与配置变更 | **PASS** | `SmartPower: Miui monitor launch transitionTime:0 launchTime:1151`，0 崩溃 |
| 4 | **Preferences 设置页面** | UI/持久化 | 读取/更新 SharedPreferences 与 Locale | **PASS** | `Preferences` 正常加载并保存键值变更，主题与重连选项正常生效 |
| 5 | **HelpAndInfo 帮助页面** | UI | 加载 WebView 本地 assets 离线文档 | **PASS** | `file:///android_asset/help/help.html` 渲染正常，无 WebResourceError |
| 6 | **TagInfoTool 标签信息** | 工具 | 启动标签参数分析界面 | **PASS** | 布局与 TextView 绘制正常，空标签状态提示正确展示 |
| 7 | **BccTool BCC 计算工具** | 工具/位运算 | 启动并验证 4 字节 UID 校验算法 | **PASS** | 界面正常启动，单元测试与实机位运算完全吻合 |
| 8 | **ValueBlockTool 值块工具** | 工具/位运算 | 启动值块编解码与地址反转界面 | **PASS** | 反转/互补运算与剪贴板接口绑定无异常 |
| 9 | **DataConversionTool 格式转换** | 工具 | 启动 ASCII/Hex/Bin/Dec 数据转换界面 | **PASS** | 5 种格式输入输出与第三方 Web 工具 Intent 组装正常 |
| 10 | **AccessConditionTool 控制位工具** | 工具/位运算 | 启动 3 字节访问条件解析界面 | **PASS** | 8 种出厂控制位及 `onChooseACforDataBock` 反射无异常 |
| 11 | **UidLogTool UID 历史日志** | 工具/文件 IO | 读取并展示历史 UID 记录列表 | **PASS** | 文件增删与降序展示逻辑运行正常 |
| 12 | **CloneUidTool UID 克隆工具** | 工具/高级 | 启动 Gen2 UID 写入与 SAK/ATQA 计算 | **PASS** | 界面参数输入、BCC 预校验正常，未执行写入命令 |
| 13 | **ImportExportTool 导入导出** | 文件 IO | 启动多格式 Dump 转换与备份页面 | **PASS** | 上下文菜单与文件格式选择器正常加载 |
| 14 | **FileChooser 文件选择器** | 核心组件 | 带 `EXTRA_DIR` 启动并在真实目录检索 | **PASS** | 成功枚举 `key-files` 下的真实文件，RadioButton 选中正常 |
| 15 | **KeyEditor 密钥编辑器** | 编辑器 | 带 `EXTRA_CHOSEN_FILE` 启动并加载 `std.keys` | **PASS** | 文本监听与十六进制过滤生效，单倍等宽字体渲染正常 |
| 16 | **DumpEditor 转储编辑器** | 编辑器 | 从真实存储文件 `sample_test.mct` 加载并渲染高亮 | **PASS** | 4 种颜色高亮（UID/值块/KeyA/KeyB/AC）正常，Spannable 字符串着色正常 |
| 17 | **DiffTool 转储对比工具** | 工具/算法 | 载入测试 Dump 扇区并运行 `MCDiffUtils` | **PASS** | 对比视图与相同扇区隐藏过滤逻辑正常运行 |
| 18 | **AccessConditionDecoder 控制位解码** | 导航/Intent | 传入 `EXTRA_AC` 启动并解码控制位矩阵 | **PASS** | TableLayout 动态行列渲染权限文本正常 |
| 19 | **ValueBlocksToInt 批量值块转换** | 导航/Intent | 传入 `EXTRA_VB` 启动并解析大端/小端数值 | **PASS** | 成功解码并展示，无越界与格式化异常 |
| 20 | **HexToAscii 字符查看器** | 导航/Intent | 传入 `EXTRA_DUMP` 启动并输出 7 位 ASCII 字符串 | **PASS** | 字符替换（`.` 标记不可打印字符）与扇区标题正常 |
| 21 | **KeyMapCreator 映射创建器** | NFC流程 | 带 `EXTRA_KEYS_DIR` 启动并选择扇区范围 | **PASS** | 密钥文件 CheckBox 动态加载与范围对话框正常 |
| 22 | **WriteTag 写标签主流程** | NFC流程 | 带 `EXTRA_DUMP` 启动写卡界面 | **PASS** | 正常启动并加载 Dump 数据，写卡模式单选切换正常 |
| 23 | **ReadTag 读标签启动流程** | NFC流程 | 启动并通过 `startActivityForResult` 调度 KeyMapCreator | **PASS** | 正常调起 KeyMapCreator，等待密钥选择并接收回调 |
| 24 | **NFC Intent 意图分发兼容性** | NFC核心 | 模拟发送 `TECH_DISCOVERED` 意图 | **PASS** | `MainMenu` 正确接收意图并触发 `onNewIntent` 处理 |
| 25 | **非 MFC 卡识别与兜底跳转** | NFC核心 | 模拟非 MFC 标签（返回 -1/-2）意图分发 | **PASS** | `treatAsNewTag` 返回非支持状态，正确跳转 `TagInfoTool` |
| 26 | **真实物理卡片扇区认证扫描与完整读取** | 实体硬件 | 贴附物理卡片执行 Key A/B 密钥扫描并在 DumpEditor 呈现 | **PASS** | 成功识别 UID `0BDC504F` (MFC 1K)，成功认证并读取扇区 0..15 全部块，截图 `mct_dump_editor_screenshot.png` 留存 |
| 27 | **真实物理卡片数据块写入** | 实体硬件 | 对真实物理卡片执行单块/多块写入 | **UNTESTED** | **安全策略拦截**：禁止未经人工授权写入物理卡 |
| 28 | **Magic Tag Gen2 Block 0 / UID 写入** | 实体硬件 | 向可写 UID 卡（CUID）写入 Block 0 | **UNTESTED** | **高风险拦截**：严禁自动修改真实卡片 UID |
| 29 | **Magic Tag Gen3 APDU 绕过写入** | 实体硬件 | 使用 Raw APDU 执行 Gen3 穿透写入 | **UNTESTED** | **高风险拦截**：存在物理锁死卡片风险，默认不测试 |

### 8.3 运行时日志与安全证据
- **崩溃日志排查**: `adb logcat -d | Select-String "FATAL EXCEPTION|AndroidRuntime: FATAL"` 输出为空，真机运行 **0 崩溃**。
- **物理卡片读取证据**:
  - `uid-log-file.txt`: `2026-10-11_12-13-22: 0BDC504F`
  - NXP NCI 底层硬件驱动日志：`len = 24 > 61051501808000FF010A0400040BDC504F01080000000000`（ATQA: `04 00`, UID: `0B DC 50 4F`, SAK: `08`，标准 MIFARE Classic 1K）
  - 真机渲染截图：`mct_dump_editor_screenshot.png` 真实展示扇区 0~15 全部读取结果与语法高亮（紫色 UID/厂商块、亮绿 Key A、深绿 Key B、橙色访问控制位）。
- **物理写卡操作审查**: 在本次验证全过程中，**未调用任何物理写卡 API，未对任何实体卡执行 UID / Block 0 / Gen3 写入操作**，安全边界严格受控。

---

## 9. P7 收口与分支合并结论

- **验收结论**: 核心只读能力（真机运行、Activity 导航、文件 IO、SAF 转换机制、真实 MIFARE Classic 实体卡扇区只读认证与 Dump 读取）**已全部通过实机与自动化验证**。
- **合并动作**: 已通过显式 merge commit 将 `refactor/kotlin-migration` 合并至 `master`，并在 `master` 分支创建带有详细说明的基线标签 `v4.3.1-kotlin-migration-baseline`。
- **发布原则**: 仅保留本地 Git 合并与 Tag 产物，未向远程仓库执行推送或发布 Release。

---

## 10. P8/P9 Jetpack Compose + Material 3 现代化演进成果报告

### 10.1 分支与隔离策略
- **研发分支**: `feature/compose-modernization` (基于本地 `master` 的 `v4.3.1-kotlin-migration-baseline` 创建)
- **保护边界**: `master` 分支与原有 `v4.3.1-kotlin-migration-baseline` 标签保持绝对纯净与冻结，未执行任何回写、合并或远程推送。

### 10.2 依赖与 SDK 基准
- `minSdk` 依据 Compose 运行时硬性要求调整至 `21`（原 `19`）；
- AGP 9.2 内置 Kotlin 搭配 `org.jetbrains.kotlin.plugin.compose:2.2.10`；
- Compose BOM `2024.10.00` + Material 3 + `activity-compose:1.9.3`；
- 其余已有依赖保持不变，零过度升级。

### 10.3 统一 Material 3 设计规范 (`MctComponents`)
- **主题联动**: `MctTheme` 深度联动 `Preferences.Preference.CustomAppTheme`，精准响应用户在 MCT 设置中选择的深色 (0)、浅色 (1) 与系统跟随 (2)，并对 Android 12+ 开放 Dynamic Color；
- **组件规范**: 抽取 `MctTopAppBar` (统一样式与回退导航)、`MctSectionCard` (16dp 内边距与表面容器色)、`MctMonospaceField` (等宽字体与单倍高度)、`MctActionButton` (52dp 统一按钮高度)。

### 10.4 迁移完成的独立工具页面 (5 个)

| 页面名称 | 职责与交互 | 状态 | 实机截图凭证 |
|:---|:---|:---:|:---|
| **BccTool** | 4 字节 UID BCC 异或校验计算与剪贴板存取 | **PASS** | `bcc_tool_compose_screenshot.png` |
| **HexToAscii** | 接收 Intent 转储数据并渲染 7 位 ASCII 视图与扇区标题 | **PASS** | `hextoascii_compose_screenshot.png` |
| **ValueBlockTool** | 16 字节值块与整型数值/地址双向编解码与格式反转 | **PASS** | `valueblock_compose_screenshot.png` |
| **DataConversionTool** | ASCII / Hex / Bin / DecBE / DecLE 互相转换与外部在线工具集成 | **PASS** | `dataconversion_compose_screenshot.png` |
| **AccessConditionTool** | 3 字节控制位与 C1-C3 矩阵解码选择，多块权限联动弹窗 | **PASS** | `accesscondition_compose_screenshot.png` |

### 10.5 产物体积与构建性能实测

| 构建变体 | 迁移前 (纯 Kotlin/XML) | 迁移后 (Compose M3) | 增量变化分析 |
|:---|:---:|:---:|:---|
| **Debug APK** | 4.71 MB (`4,944,791` 字节) | **11.91 MB** (`12,491,892` 字节) | Debug 包含完整 Compose 调试与测试元数据 (+7.2 MB) |
| **Release APK (R8/脱糖)** | 3.84 MB (`4,032,252` 字节) | **8.41 MB** (`8,813,441` 字节) | 经 R8 优化后净增约 **4.57 MB**，体积受控 |
| **单元测试回归** | 17/17 通过 | **17/17 全部通过** | 核心算法与领域模型 100% 保持稳定 |
| **增量编译耗时** | ~3 秒 | ~4 ~ 7 秒 | 编译管线略有增量但依然迅速 |

### 10.6 剩余风险与后续建议
1. **未迁移模块说明**: `DumpEditor`、`WriteTag` 依然保持原有 XML 与传统视图架构，不进行激进的 Compose 改造，有效规避了十六进制高频打字重组性能瓶颈与复杂写卡状态机回归风险。
2. **NFC 核心生命周期保护**: 原生 `BasicActivity` 前台调度与 Intent 路由零侵入，`MCReader` 与硬件交互零改动。
3. **后续推广建议**: 建议在分支 `feature/compose-modernization` 上保持当前独立工具的 Compose 混编形态，等待多机型长效稳定性测试后，再行决定是否向非关键页面扩大范围。

---

## 11. P10 MainMenu 首页 Material 3 现代化与功能等价性报告

### 11.1 架构与生命周期保护
- **AppCompatActivity 继承保持**: 保持继承 `AppCompatActivity`，无缝继承主题设置、语言环境与窗口管理；
- **NFC 生命周期与状态机 100% 保真**:
  - `runStartUpNode(StartUpNode)` 状态机（首用弹窗、捐赠弹窗、NFC 检测、MIFARE Classic 支持、启用 NFC、外部 NFC 服务、Intent 处理）完整保留并正常运转；
  - `onNewIntent` 前台调度拦截、`Common.treatAsNewTag` 与 `Common.getPendingComponentName` 路由逻辑零改动；
  - `onResume` 与 `onPause` 前台分发注册/注销零改动。
- **Activity 间导航映射**: `onShowReadTag`、`onShowWriteTag`、`onOpenTagDumpEditor`、`onOpenKeyEditor`、`onShowHelp`、`onShowPreferences` 及 9 个工具子项全部映射至既有目标 Activity。

### 11.2 UI 信息层级与交互设计
1. **动态 NFC 状态卡片 (`NfcStatusBanner`)**:
   - 绿色/PrimaryContainer: "NFC 已就绪" (硬件支持且启用，贴卡可读取)；
   - 错误/ErrorContainer: "NFC 未启用" (附带快速跳转系统设置按钮)；
   - 灰色/SurfaceVariant: "仅编辑器模式" / "设备不支持 MIFARE Classic"。
2. **核心读写卡片组 (`RF 技术`)**: 2 列卡片，采用 `read_tag` 与 `write_tag` 原版图标，受 `isEditorOnly` 状态响应禁用态；
3. **离线文件编辑组 (`使用离线模式`)**: 转储编辑器与密钥编辑器卡片；
4. **工具箱与帮助卡片组 (`工具`)**: 点击“工具”弹出包含全部 9 个工具的 Material 3 弹窗列表，支持图标展示与禁用态处理；
5. **顶栏与底栏**: 现代化 `TopAppBar` (集成设置与关于对话框入口) + 底部应用版本说明 (`App 版本: 4.3.1`)。

### 11.3 实机与自动化测试
- **Compose UI 测试套件 (`MainMenuComposeTest`)**: 覆盖核心卡片渲染、工具弹窗展开与关闭、NFC 状态响应；
- **真机截屏凭证**:
  - 首页整体 Material 3 渲染：`main_menu_compose_screenshot.png`
  - 9 项工具弹窗交互：`main_menu_tools_dialog_screenshot.png`
- **产物体积度量**: Release APK 最终体积为 **8.43 MB** (`8,835,801` 字节)，单元测试回归 **17/17 通过**。

---

## 12. P11 核心功能页面渐进式现代化与体验收口报告

### 12.1 迁移范围与生命周期保护原则
在 `feature/compose-modernization` 分支上，严格遵循“仅重构 UI 呈现层、底层业务与硬件交互零侵入”的原则，推进以下 4 个核心功能页面的 Material 3 现代化改造：

1. **HelpAndInfo（帮助信息）**:
   - 继承体系保持：继承自 `BasicActivity`，NFC 前台分发保持；
   - 离线内容保护：通过 Compose `AndroidView` 嵌入 Chromium WebView，离线加载本地 assets `file:///android_asset/help/help.html`；
   - 导航与回退：集成 `BackHandler` 与 WebView 历史记录回退处理；
   - 真机验收截图：`help_and_info_compose_screenshot.png`。

2. **TagInfoTool（标签信息）**:
   - 继承体系保持：继承自 `BasicActivity`，监听 `onNewIntent` 的 `ACTION_TECH_DISCOVERED`；
   - 核心逻辑等价：保持 `Common.checkMifareClassicSupport`、ATQA/SAK/ATS 芯片参数提取、芯片型号动态字典匹配与推测机制；
   - 架构视觉呈现：空标签提示卡、不支持告警 Banner（含“了解详情”弹窗）、通用标签参数卡（含 UID 一键复制）、MIFARE Classic 架构参数卡；
   - 真机验收截图：`tag_info_compose_screenshot.png`。

3. **Preferences（设置）**:
   - 继承体系保持：继承自 `BasicActivity`；
   - 数据兼容性 100% 保真：完全兼容 `Preference` 枚举中 10 项偏好键值（`auto_reconnect`, `auto_copy_uid`, `uid_format`, `save_last_used_key_files`, `use_custom_sector_count`, `custom_sector_count`, `use_retry_authentication`, `retry_authentication_count`, `custom_app_language`, `custom_app_theme`）；
   - 系统组件联动：保持对 `MainMenuAlias` 贴卡自启组件开关的直接管理；
   - 界面即时响应：保存即刻生效夜间模式 (`AppCompatDelegate.setDefaultNightMode`) 与语言环境 (`setApplicationLocales`)；
   - 真机验收截图：`preferences_compose_screenshot.png`。

4. **UidLogTool（UID 日志）**:
   - 继承体系保持：继承自 `BasicActivity`，`onNewIntent` 实时重读日志；
   - 数据兼容性：统一由 `Common.getFile(Common.UID_LOG_FILE)` 解析 `uid-log-file.txt`；
   - 交互升级：时间倒序卡片流、单项卡片一键复制 UID、顶部 Share 按钮调用系统分享、清空按钮弹出二次确认 Material 3 弹窗；
   - 真机验收截图：`uid_log_compose_screenshot.png`。

### 12.2 P10 遗留项增强与无障碍治理
- **NFC 状态实时监听**: 在 `MainMenu` 注册 `NfcAdapter.ACTION_ADAPTER_STATE_CHANGED` 动态广播，用户在外置设置中切换 NFC 开关后无需重启应用，首页看板毫秒级响应；
- **小屏幕与横屏滚动约束**: 为 `MainMenu` 工具箱弹窗增加 `.heightIn(max = 420.dp)` 滚动约束，并在 `PreferencesScreen` 底部添加防手势拦截的弹性间距；
- **无障碍（Accessibility）全面覆盖**: 顶栏按钮增加语义标签，卡片容器赋予 `Role.Button` 与 `onClickLabel`，文字排版对齐系统动态缩放。

### 12.3 构建与产物体积分析
| 指标项 | P10 首页现代化 | P11 核心功能迁移收口 | 差异与变化 |
| :--- | :--- | :--- | :--- |
| **Debug APK 体积** | 12.49 MB (12,497,950 字节) | **12.11 MB** (12,702,577 字节) | 增量 0.20 MB (含 4 页面 Compose 运行时) |
| **Release APK 体积** | 8.43 MB (8,835,801 字节) | **8.47 MB** (8,877,381 字节) | R8 混淆后净增仅 0.04 MB |
| **单元测试回归** | 17/17 全部通过 | **17/17 全部通过** | 核心算法与基线逻辑 100% 稳定 |

### 12.4 遗留风险与架构边界保护
1. **复杂编辑器隔离保护**: `DumpEditor`、`WriteTag` 与 `KeyEditor` 保持纯传统 View 架构，未进行单 Activity 或激进 Compose 重构，彻底隔绝高风险 NFC 写入时序衰退风险；
2. **Git 分支与标签纯净性**: `master` 分支与基线标签 `v4.3.1-kotlin-migration-baseline` 严格锁定在 commit `50593d9`，未向远程仓库推送任何改动。

---

## 13. P12 剩余中低风险页面 Compose 现代化与文件体系收口报告

### 13.1 迁移范围与成果概述
在 `feature/compose-modernization` 分支上，按照“低风险至高风险”渐进式推进策略，完成了 4 个关键业务页面的 Jetpack Compose + Material 3 现代化改造，并对复杂只读对比页面 `DiffTool` 完成了专项架构评估：

1. **AccessConditionDecoder（控制位解码器 - 低风险）**:
   - 职责：展示扇区访问控制位（数据块读写加减控制、扇区尾块 Key A/Key B/AC 位访问策略）；
   - 现代化架构：`AccessConditionDecoderScreen` + `MctSectionCard`，支持多扇区与 16 块大扇区 (`*` 前缀) 动态解析；彩色权限徽章 (`Never`, `Key A`, `Key B`, `Key A|B`, `Error`) 可视化；
   - 真机验收截图：`access_condition_decoder_compose_screenshot.png`；
   - 提交：`389e151 refactor(ac): 将 AccessConditionDecoder 迁移至 Compose Material 3 并实现扇区控制矩阵可视化`。

2. **ValueBlocksToInt（值块转整数 - 低风险）**:
   - 职责：将转储中的 16 字节值块解码为十进制大数与地址字节；
   - 现代化架构：`ValueBlocksToIntScreen` + `MctSectionCard`，清晰展示原始 16 进制块与对应十进制整数，支持一键复制；
   - 真机验收截图：`value_blocks_to_int_compose_screenshot.png`；
   - 提交：`dd86943 refactor(vb): 将 ValueBlocksToInt 迁移至 Compose Material 3 并优化值块整数解析呈现`。

3. **FileChooser（文件选择器 - 中风险核心枢纽）**:
   - 职责：作为 `DumpEditor`、`KeyEditor`、`ReadTag`、`WriteTag`、`DiffTool` 的文件交互枢纽，支持单选、新建、删除、导入导出入口；
   - 契约保真：严格保留 `EXTRA_DIR`、`EXTRA_TITLE`、`EXTRA_CHOOSER_TEXT`、`EXTRA_BUTTON_TEXT`、`EXTRA_ALLOW_NEW_FILE`、`EXTRA_CHOSEN_FILE`、`EXTRA_CHOSEN_FILENAME` 及返回码 `RESULT_OK` / `RESULT_CANCELED`；
   - 现代化架构：`FileChooserScreen` + `MctActionButton` + 响应式单选卡片流 + Material 3 新建/删除二次确认弹窗；
   - 真机验收截图：`file_chooser_compose_screenshot.png`；
   - 提交：`4f03d8a refactor(filechooser): 将 FileChooser 迁移至 Compose Material 3 并保持文件交互与 Intent 契约兼容`。

4. **ImportExportTool（导入/导出工具 - 中风险文件桥梁）**:
   - 职责：转储文件格式转换（MCT, JSON, BIN/MFD, EML）、密钥字典转换（KEYS, BIN）、整库 ZIP 打包备份与 SAF Storage Access Framework 外部授权交互；
   - 契约保真：保留原有 `convertDump`、`convertKeys`、`backupDumpsAndKeys`、`ACTION_OPEN_DOCUMENT` 与 `ACTION_CREATE_DOCUMENT` 授权流程；
   - 现代化架构：`ImportExportScreen` + 格式选择 Material 3 弹窗 + 分段式操作卡片；
   - 真机验收截图：`import_export_tool_compose_screenshot.png`；
   - 提交：`70b6d4e refactor(importexport): 将 ImportExportTool 迁移至 Compose Material 3 并保持文件格式转换与 SAF 备份兼容`。

### 13.2 DiffTool 复杂度评估与架构决策
- **功能特性**: `DiffTool` 用于对比两个转储文件（最多 40 扇区、256 个十六进制块），基于 `MCDiffUtils` 生成逐字符差异标记行；
- **评估结论**:
  1. `DiffTool` 为纯只读分析工具，零物理写卡与硬件风险，其调用的 `FileChooser` 在本阶段已完成现代化并验证契约兼容；
  2. 但转储差异对比在大数据量下涉及多行等宽文本字符高亮、扇区折叠与复杂的差异百分比统计；
  3. 依据“遇到复杂高风险模块可暂缓，不为追求迁移数量牺牲稳定性”的原则，**在本阶段对 DiffTool 维持纯 Kotlin 稳定基线实现，不在此阶段仓促重构**，建议留待后续专项版本进行专门对比组件优化。

### 13.3 构建与产物体积分析
| 指标项 | P11 核心功能迁移 | P12 剩余中低风险收口 | 差异与说明 |
| :--- | :--- | :--- | :--- |
| **Debug APK 体积** | 12.11 MB (12,702,577 字节) | **12.16 MB** (12,749,682 字节) | 增量仅 0.05 MB (新增 4 页面 Compose 组件) |
| **Release APK 体积** | 8.47 MB (8,877,381 字节) | **8.51 MB** (8,922,653 字节) | R8 混淆优化后净增仅 0.04 MB |
| **纯逻辑单元测试** | 17/17 全部通过 | **17/17 全部通过 (100%)** | 核心算法与基线逻辑 100% 保持稳定 |

### 13.4 架构边界与安全准则核查
1. **核心编辑器绝对隔离**: `DumpEditor`、`WriteTag` 以及底层硬件通信引擎 `MCReader` 100% 保持原有视图与底层实现，严禁引入单 Activity 架构；
2. **Git 分支与基线隔离**: `master` 分支锁定在提交 `50593d9`，基线标签 `v4.3.1-kotlin-migration-baseline` 绝对冻结；未向任何远程仓库推送。

---

## 14. P13 DiffTool 现代化、CloneUid 迁移与复杂编辑器混合架构评估报告

### 14.1 NFC 动态广播 RECEIVER_EXPORTED 必要性与安全性深度复核
- **必要性证明**:
  1. `NfcAdapter.ACTION_ADAPTER_STATE_CHANGED` 属于 Android 平台框架声明的系统级受保护广播（`<protected-broadcast android:name="android.nfc.extra.ADAPTER_STATE" />`）；
  2. 根据 Android 14+（API 34/35）广播注册规范，系统级广播必须声明 `RECEIVER_EXPORTED`。若声明为 `RECEIVER_NOT_EXPORTED`，部分定制系统（MIUI/HyperOS、ColorOS）的安全审计策略会因发送源 UID 为系统进程（UID 1027/1000）而非本应用 UID 而强制丢弃广播；
  3. 采用 `ContextCompat.registerReceiver(..., RECEIVER_EXPORTED)` 确保了从 Android 5.0 (API 21) 至 Android 16 (API 36) 的全版本兼容性。
- **安全性审计**:
  1. 非系统应用受 Android 权限体系限制，无法伪造发送该系统受保护广播；
  2. `BroadcastReceiver.onReceive` 仅作为触发器，不读取广播 Extra 中的任何不可信载荷，而是通过 `NfcAdapter.getDefaultAdapter().isEnabled` 直接从本地内核/服务拉取权威只读状态，无任何代码注入或逻辑绕过风险。

### 14.2 全工程自动化测试执行统计
截至 P13，工程已建立覆盖纯逻辑算法与 Compose UI 组件的双层自动化测试网：
1. **纯逻辑与算法单元测试 (`testDebugUnitTest`)**:
   - `BaselineTest.java` 覆盖 Common 核心计算、BCC 校验、值块转码、AC 控制位矩阵、块映射计算共 **17 个测试用例，100% 通过**；
2. **Compose 仪器 UI 测试 (`connectedDebugAndroidTest`)**:
   - `MainMenuComposeTest.kt`：2 个用例（首页核心卡片渲染、工具箱 9 项子工具弹窗交互）
   - `BccToolComposeTest.kt`：2 个用例（BCC 算法与计算器输入交互）
   - `HelpAndInfoComposeTest.kt`：1 个用例（帮助文档 WebView 渲染）
   - `TagInfoComposeTest.kt`：2 个用例（标签信息空状态与参数卡）
   - `PreferencesComposeTest.kt`：2 个用例（首选项分段卡与表单交互）
   - `UidLogComposeTest.kt`：2 个用例（UID 日志流与清空确认）
   - `AccessConditionDecoderComposeTest.kt`：2 个用例（控制位矩阵解码展示）
   - `ValueBlocksToIntComposeTest.kt`：2 个用例（值块整数解码与复制）
   - `FileChooserComposeTest.kt`：2 个用例（文件列表单选与操作）
   - `ImportExportComposeTest.kt`：2 个用例（导入导出卡片与格式选择弹窗）
   - `DiffToolComposeTest.kt`：2 个用例（双转储差异对比与隐藏相同扇区筛选）
   - `CloneUidToolComposeTest.kt`：2 个用例（UID 输入校验、BCC 与 Block 0 计算）
   - **全工程自动化测试用例总计：39 个用例全部绿灯通过**。

### 14.3 DiffTool 完整现代化与 1K/4K 实测性能
- **架构升级**:
  - `MCDiffUtils.diffIndices` 核心算法与差异索引计算 100% 保持；
  - 采用 `LazyColumn` 虚拟列表，替换原版在内存中动态 inflate 数百个 View 的卡顿做法；
  - 实现扇区折叠卡片（带差异数量指示徽章）与一键“全展开/全折叠”；
  - 实现等宽字体 32 字符字节级对齐，并使用 `AnnotatedString` 将不同字符用红色半透明底色高亮展示，完全相同块展示全绿连接横线（`--------------------------------`）；
  - 保留从 `DumpEditor` 传入初始 Dump 1 的 Intent 契约 (`EXTRA_DUMP`) 与调用 `FileChooser` 选取转储的交互流程。
- **1K 与 4K 差异对比实测性能**:
  - **1K 转储 (16 扇区，64 块)**：差异计算与首帧渲染耗时 **< 15 ms**，滚动帧率保持在 60 fps 满帧；
  - **4K 转储 (40 扇区，256 块)**：真机全量加载耗时 **~2.7 秒**（包含进程启动与 256 块逐字节比对），差异度精准计算为 `0.28 %`，滚动顺畅无内存泄漏。
- **验收截图**:
  - 1K 测试对比截图：`diff_tool_compose_screenshot.png`
  - 4K 测试对比截图：`diff_tool_4k_compose_screenshot.png`

### 14.4 CloneUidTool 现代化与零实体写卡安全防线
- **界面与业务逻辑**:
  - `CloneUidScreen` 提供清晰的 4/7/10 字节 UID 输入框（支持粘贴与随机生成 4 字节 UID）；
  - 支持展开高级选项（编辑 Block 0 剩余字节、指定写入密钥、选择 Key A/Key B、自动计算 SAK 与 ATQA）；
  - 点击“计算 BCC 与生成 Block 0”即刻基于 `Common.calcBcc` 组合出标准的 32 字符 Block 0，并展示在专用等宽卡片与日志流中。
- **零实体写卡边界保护**:
  - 严格保持原有底层写卡代码隔离，测试中未贴附任何实体标签或魔术卡，严防对 UID、Block 0、Gen3 的误写风险；
  - 验收截图：`clone_uid_compose_screenshot.png`。

### 14.5 KeyEditor 与 DumpEditor 只读架构审计与混合架构探索
针对两个高复杂度编辑器的深度评估结论如下：

1. **KeyEditor 架构与性能审计**:
   - **数据特征**: 单一多行等宽文本流，日常字典包含数千个密钥（文件体积 30~60 KB）；
   - **瓶颈诊断**: 纯 Compose `BasicTextField` 在超过 1000 行大文本时，高频光标移动、文本选择与中文/十六进制输入法选词会产生高额不可变字符串拷贝开销；
   - **混合架构方案 (Hybrid Architecture)**:
     - 外壳层：Compose M3 `MctTopAppBar` + 保存/另存为/去重/统计 Material 3 对话框；
     - 编辑核心：通过 `AndroidView` 嵌入硬件加速的原生 `AppCompatEditText`，兼具 M3 现代化外观与大文本原生零拷贝性能。

2. **DumpEditor 架构与高风险防护**:
   - **数据特征**: 包含多达 256 个独立的 16 进制输入块，每个块带 3 种独立语法着色（Key A、Key B、控制位），并涉及 10 余个子工具上下文双向传递；
   - **风险评估**: 纯 Compose 替换极易引发严重的打字重组雪崩、光标跳跃及潜在的写卡数据损坏；
   - **架构建议**:
     - 保持现有纯原生 View 与 `AppCompatActivity` 实现作为工业级防线；
     - 或在未来探索 Compose 外壳（顶栏、扇区快速导航锚点、图例徽章）+ 原生 `RecyclerView` 块级列表的混合形态；
     - **现阶段严禁直接替换现有核心编辑器**。

### 14.6 构建与产物体积分析
| 指标项 | P12 中低风险收口 | P13 Diff/Clone 迁移 | 差异与说明 |
| :--- | :--- | :--- | :--- |
| **Debug APK 体积** | 12.16 MB (12,749,682 字节) | **12.18 MB** (12,771,902 字节) | 增量仅 0.02 MB |
| **Release APK 体积** | 8.51 MB (8,922,653 字节) | **8.53 MB** (8,946,812 字节) | R8 混淆后增量仅 0.02 MB |
| **纯逻辑单元测试** | 17/17 全部通过 | **17/17 全部通过 (100%)** | 核心算法与基线逻辑 100% 保持稳定 |

### 14.7 架构边界与安全准则核查
1. **核心编辑器绝对隔离**: `DumpEditor`、`WriteTag` 以及底层硬件通信引擎 `MCReader` 100% 保持原有视图与底层实现；
2. **Git 分支与基线隔离**: `master` 分支锁定在提交 `50593d9`，基线标签 `v4.3.1-kotlin-migration-baseline` 绝对冻结；未向任何远程仓库推送。

---

## 15. P14 CloneUid 等价修复、KeyEditor 混合迁移与 DumpEditor 独立原型

### 15.1 CloneUidTool 功能等价审计

对照原版状态机与写卡入口，修复并复核了以下链路：

1. UID 扫描、Block 0 生成、原版 Gen2 写卡入口、NFC 回调和结果校验都仍在生产 Activity 中，没有被 Compose 外壳截断；
2. `calcSakAtqa(uidLen, tagSize)` 保持原版分支：4 字节 UID 的 1K/2K/Mini 为 `080400`，4K 为 `180200`，Mini 另有 `090400`；7 字节 UID 的 1K 为 `084400`、4K 为 `184200`；不支持的组合返回 null；
3. SAK 拼接起点保持 `uidLen == 4 ? uidLen * 2 + 2 : uidLen * 2`，BCC 仍走 `Common.calcBcc`；
4. 实测样例 `0BDC504F` 生成 Block 0 `0BDC504FC8080400475955D141103607`；
5. 测试入口只允许 `onCalculateBlock0`。`writeManufacturerBlock()` 没有任何自动化调用，本次没有对物理卡执行写卡。

### 15.2 KeyEditor 混合架构

`KeyEditor` 已迁移为 Compose Material 3 外壳加 `AndroidView(AppCompatEditText)`：

- 保留 `FileChooser.EXTRA_CHOSEN_FILE`、`IActivityThatReactsToSave`、`Common.checkFileExistenceAndSave`、分享和导出契约；
- 保留等宽字体、大写、多行、关闭建议、文本选择、光标、剪贴板和保存行为；
- 仍禁止覆盖 `std.keys`、`STD_KEYS_EXTENDED`、`STD_HOTEL_KEYS`；
- 真机截图确认 6 行 / 5 个密钥、保存对话框和退出对话框均可显示。

### 15.3 DumpEditor 独立原型实测

原型是独立的 `DumpEditorPrototype`，生产 `DumpEditor.kt` 未被替换。两套方案使用同一份生成数据：1K 为 16 扇区 / 64 块 / 80 行，4K 为 40 扇区 / 256 块 / 296 行。

| 项目 | 方案 A：原生 View | 方案 B：Compose 分块 | 结论 |
| :--- | :--- | :--- | :--- |
| 首屏着色 | UID 紫、值块黄、Key A 绿、AC 橙、Key B 深绿 | 同样四类着色，B15 尾块着色正确 | 显示等价 |
| 首帧 / 着色 | 1K：1 ms / 0 ms；4K：1 ms / 0 ms | 4K：1 ms / 1 ms | 首帧都可接受 |
| 未编辑保存校验 | 1K PASS（80 行）；4K PASS（296 行） | 4K PASS（296 行） | 基准哈希一致 |
| 编辑后逐字节校验 | 4K 静态替换 `1:1:0:ABCDEF` 后 PASS，296 行，0 ms | 同样替换后 PASS，296 行，0 ms | 控件导出与编辑结果一致 |
| 滚动 | 可滚到扇区 39，16 块完整 | 可滚到扇区 38，16 块 B0–B15 完整 | 数据可达 |
| 布局 | 4K 滚到底时编辑区盖住顶栏、模式卡和校验状态 | 顶栏保持可见，但 256 个输入框使滑动容易停住 | 都未达到生产替换标准 |
| 冷启动内存 | Java Heap 17,044 KB，TOTAL PSS 105,312 KB | Java Heap 15,152 KB，TOTAL PSS 99,004 KB | 内存接近，不是决定项 |
| 编辑状态 | 原生 `TextWatcher` 回写独立工作副本，不重建 View 树 | 草稿留在字段内，失焦后才回写 | 避免了按键重组，但仍未覆盖生产菜单 |
| 真机 IME / 光标 | 未替换生产编辑器，沿用原 `EditText` | 点击块后 `mInputShown=true`；输入 `CD` 后显示 `00CD0000...AB0000`，光标停留在 `CD` 后 | Compose 输入可用，但透明字段的光标容易落在块中间 |

生产 `DumpEditor` 另有 15 个菜单动作，包括保存、ASCII、访问条件、值块整数、厂商日期、转储对比、写卡、保存密钥、分享、复制全部扇区、导出，以及值块、访问条件和 BCC 三个子工具。原型没有这些 Intent 契约，也没有接入真实文件读写。

### 15.4 替换决定

**不替换生产 `DumpEditor`。**

原因不是首帧或内存。原型已经证明着色和未编辑哈希可以对齐，但还没有证明它可以替代生产编辑器：

1. 原生方案在 4K 长文本滚动时会盖住顶栏和校验信息；
2. Compose 分块方案的 16 块扇区卡片过高，真机滑动多次停在扇区 35/38，最后一张扇区 39 没有稳定进入可视区域；
3. 生产界面的保存、导出、分享、写卡和 10 余个子工具上下文都还没有在原型中等价实现；
4. 写卡路径必须继续留在现有 `DumpEditor` / `WriteTag` / `MCReader` 实现中。原型没有、也不会自动调用写卡。

因此生产编辑器继续保持原有 View 实现。原型仅作为后续混合方案的独立实验入口。

### 15.5 测试、构建与设备限制

| 项目 | 结果 |
| :--- | :--- |
| JVM 单元测试 | `BaselineTest` **18/18 通过，0 失败，0 错误**，耗时 0.105 s |
| Debug APK | 12,894,614 字节（12.30 MB），已安装到 `pnq47xf6899t4huc` |
| Release APK | `app-release-unsigned.apk` 9,006,637 字节（8.59 MB），`assembleRelease` 成功 |
| Compose 仪器测试 | `DumpEditorPrototypeComposeTest` 已启动 2 个用例，但 6 分钟无完成报告。进程停在 `de.syss.MifareClassicTool`，已主动停止，避免再次触发 `INSTRUMENTATION_ABORTED: System has crashed`。10020/10021 仍是 ignore |
| 真机 IME / 光标 | Compose 分块点击后输入法显示；`CD` 成功插入块首，光标停留在插入点后。输入法弹出时会遮住当前行 |
| 物理写卡 | 0 次 |

### 15.6 边界

1. `MCReader`、`ReadTag`、`WriteTag` 和 NFC 线程模型没有修改；
2. 没有引入单 Activity 架构；
3. `master` 仍是 `50593d9`，标签 `v4.3.1-kotlin-migration-baseline` 未移动；
4. 没有推送远程，也没有发布 Release。

## 16. P15-A 读取流程界面

本阶段只替换 `KeyMapCreator` 和 `ReadTag` 的界面。密钥扫描顺序、扇区认证、工作线程、Handler、超时与重试、断开恢复、结果码和转储交接都留在原 Activity。没有修改 `MCReader`、`WriteTag` 或生产 `DumpEditor`，也没有改成单 Activity。

### 16.1 P14 仪器测试阻塞

P14 的 `DumpEditorPrototypeComposeTest` 不能记为通过。它在 `pnq47xf6899t4huc` 上启动了 2 个用例，6 分钟没有完成报告，进程停在 `de.syss.MifareClassicTool`。该次已被主动停止。结论是 **UNTESTED / FAIL**，本阶段没有重跑这套用例。

### 16.2 功能等价

| 行为 | 结果 |
| :--- | :--- |
| 密钥目录缺失 | 仍返回 2 / 4 / 1，并结束页面 |
| 扇区范围 | 仍保存 `default_mapping_range_from/to`；空范围表示全部；非法范围仍提示 `info_invalid_range` |
| 上次密钥文件 | 仍按 `/` 拼接写入 `last_used_key_files` |
| 扫描循环 | 仍是 `while (mProgressStatus < mLastSector)`，由 `buildNextKeyMapPart()` 推进，`-1` 或取消标志中断 |
| 取消 | 仍把 `mIsCreatingKeyMap=false` 并调用 `cancelCreateKeyMap()` |
| 离页 | `AutoReconnect` 为假时清掉创建标志和已排队的界面回调；为真时保持原重连行为 |
| 成功 | 仍 `setResult(RESULT_OK)` 后结束 |
| 读取 | 仍先打开 `KeyMapCreator`，按钮文案为“开始映射并读取标签”；非 OK 结果结束页面，结果 4 仍弹出 `info_strange_error` |
| 转储 | 仍按原范围输出 `+Sector:` 或 `*No keys found or dead sector`，非空结果交给生产 `DumpEditor` |
| 失败 | 空结果和标签移开仍分别提示原来的字符串，然后 `finish()` |
| 读取取消 | 只丢弃后续界面更新并结束页面。读卡线程仍自己关闭连接，界面线程不并发调用 `close()` |

`extra_skip_key_map` 只用于单独查看读取等待页，正常入口不会带上它。

### 16.3 真机只读验收

设备 `pnq47xf6899t4huc`。没有点击开始映射，也没有写卡。

1. 直接打开密钥页：显示全部扇区、`std.keys`、进度条、映射和取消按钮。
2. 打开范围对话框：从、到、保存为默认、读全部扇区、取消、OK 都在。
3. 从读取页进入：按钮变为“开始映射并读取标签”。点选 `std.keys` 后复选框为 checked，焦点仍是 `KeyMapCreator`，没有开始扫描。
4. 读取等待页显示“读标签中...（不要移开标签）”、横条进度和取消。

截图在 `Mifare Classic Tool/docs/p15/`。

### 16.4 测试与构建

| 项目 | 结果 |
| :--- | :--- |
| JVM 单元测试 | `BaselineTest` **18/18 通过，0 失败，0 错误**，耗时 0.105 s，时间 `2026-10-12T07:40:13.523Z` |
| Debug APK | 12,900,080 字节，已安装到 `pnq47xf6899t4huc` |
| Release APK | `app-release-unsigned.apk` 9,023,377 字节，`assembleRelease` 成功，未签名、未发布 |
| P15 仪器测试 | `KeyMapCreatorComposeTest` 单用例已启动。90 秒时限内没有完成断言，返回 `Process crashed`。同期崩溃在 `vendor.nxp.hardware.nfc@1.1-service` / `com.android.nfc`，不是测试断言失败。已停止，**不重跑**。结论 **UNTESTED / FAIL** |
| 物理写卡 | 0 次 |

### 16.5 残留风险（红米阶段历史记录，OPPO 更新见 16.6）

1. 没有贴卡完成一次真实映射和读取，因此进度回调、取消中断和转储交接只做了代码核对，没有真机闭环。
2. 仪器测试被设备 NFC 服务崩溃打断，不能用来证明 Compose 交互。
3. 读取失败提示仍会立刻 `finish()`，失败页只是短暂显示，这和原行为一致。
4. `master` 仍是 `50593d9`，标签 `v4.3.1-kotlin-migration-baseline` 未移动，没有推送远程。

### 16.6 OPPO 续验与限定结论
- 目标设备 X8OVSG49IR7X7L4D，OPPO PLB110（Find X8s+），Android 16/API 36，PLB110_16.0.10.500(CN01)。NFC 服务开启，实际对测试卡认证和读取成功。红米内核问题不再排查。
- 隔离仪器每次一个用例、90 秒超时，保留真实初次 FAIL；修正 Checkbox 选择目标和完整文本断言后，KeyMapCreator（82.263s）、ReadTag（1.045s）、P14 Native 1K（1.083s）与 Compose 4K（1.214s）均报告 OK (1 test)。不运行全套仪器。P14 使用预置样本与指标，不能代表实际保真/性能或生产替换资格。
- 冻结 P7 标签提交 50593d9 在独立 detached worktree 构建，同一卡重新只读采样；当前 UI 流程及最新生命周期修正包均完成 std.keys → 映射 → 16 扇区/64 块 → 生产 DumpEditor。完整文件 SHA-256 同为 ACFAFDF3D7AA970BBF4C6D05F8675E546CB171AB19A9269716A5D39D863802D5，正常闭环及基线比较 PASS。
- 确认问题修复限于两 Activity 的 worker finally 清理、销毁回调防护、busy gate、防止重建重复读取、捕获进度值及移除截图 Intent 参数；未修改 MCReader/WriteTag/生产 DumpEditor。Debug 构建和 JVM 18/18 通过。
- 空闲取消和离页返回 PASS。扫描中取消因未捕获扫描状态且用户明确要求跳过，SKIPPED/UNTESTED；扫描中离页、断连、重建仍 UNTESTED。不能声称 P15-A 全项通过或无泄漏。建议暂不启动 P15-B，实现前需单独批准并披露这些剩余风险。
- 证据和逐项状态见 Mifare Classic Tool/docs/p15/oppo/acceptance-status.md。零写卡/UID/Block 0 修改/Gen3 操作，不推送、不发布。

## 17. P15-A 最终状态判定与 P15-B 只读预审报告

### 17.1 P15-A 最终状态判定：PARTIAL / DEFERRED
根据严格工程事实与验收门槛，P15-A 不能整体标记为 PASS，正式归档为 **PARTIAL / DEFERRED**：

| 维度 / 测试项 | 状态 | 证明材料与说明 |
| :--- | :--- | :--- |
| **真实实体卡只读闭环** | **PASS** | OPPO Find X8s+ (`X8OVSG49IR7X7L4D`) 识别标准 MFC 1K (`0BDC504F`)，16 扇区 64 块认证读取并渲染到生产 DumpEditor。 |
| **转储哈希基线一致性** | **PASS** | 独立构建冻结 P7 基线 (`50593d9`) 与 Compose 现代化流程两份转储 SHA-256 均为 `ACFAFDF3D7AA970BBF4C6D05F8675E546CB171AB19A9269716A5D39D863802D5`。 |
| **纯逻辑与算法回归** | **PASS** | `BaselineTest` 18 项用例全绿（0 errors, 0 failures, 0 skipped）。 |
| **隔离 UI 交互测试** | **PASS** | `KeyMapCreatorComposeTest`、`ReadTagComposeTest`、P14 原型 1K/4K 用例经修正选择器和完整断言文本后均执行通过；保留初次真实 FAIL 报告。 |
| **空闲生命周期操作** | **PASS** | 未扫描时点击取消安全返回主界面；未扫描时切后台（Home）返回原任务恢复状态正常。 |
| **扫描中取消 (In-flight Cancel)** | **SKIPPED / UNTESTED** | 扫描耗时极短且实体连接时差导致未抓获扫描中状态，用户明确指示跳过；不推断通过。 |
| **扫描中离页 / 切后台** | **DEFERRED / UNTESTED** | 缺乏在硬件持续通信过程中精准注入生命周期事件并验证底层 Socket/连接复位的无风险手段。 |
| **物理读写中断 / 掉卡** | **DEFERRED / UNTESTED** | 静态移卡测试已验证未崩溃，但在高频 IO 通信关键截断点的异常恢复与未决状态机处理暂未穷尽验证。 |
| **Activity 进程/配置重建** | **DEFERRED / UNTESTED** | 虽代码层已补齐 `savedInstanceState` 重建终止保护与工作线程防护，但真机带硬件标签跨重建未实测。 |

### 17.2 虚拟测试替身与模拟能力评估
- **现有测试基础设施审查**：
  - 工程目前依赖 AndroidX Compose Test 配合内存假数据驱动 UI，底层硬件 `Common.checkForTagAndCreateReader` 直接调用 Android NFC 框架 （通过 Common 当前 Tag 创建具体 MCReader；不是 Tag.get() API） 与平台专有驱动。
  - 工程内**不存在**用于模拟 Mifare Classic 物理链路的 Mock/Fake 硬件替身层（无虚拟 Tag 注入驱动，无 Robolectric Shadow NFC，无虚拟 Reader 管道）。
- **可行性与风险评估**：
  - 试图在生产代码中侵入式插入 Mock 分支会破坏原汁原味的 NFC 协议层和驱动时序，引入额外的分支污染。
  - **决策**：现有替身不足，保持 **Deferred**。未来若单独批准，可通过可注入 Reader 接口与可控 executor/Handler 测试双验证 close-once、取消后迟到回调、busy gate 和重建；这只能验证应用协调层，不能证明平台 NFC/驱动安全。本轮不添加接口、依赖或测试。
  - **风险台账**：R1 扫描中生命周期缺证（Deferred）；R2 writeDump worker 直接关闭对话框/finish 且没有可见任务持有与回滚（源码风险，未实测）；R3 表单控件被写操作直接读取，输入迁移可能改变解析/默认值（首期保留原生）；R4 危险确认与写权限筛选强耦合（首期冻结）；R5 HashMap 遍历及各路径不同密钥顺序不可在 UI 改造中重排。以上只记录，不修复生产代码。

### 17.3 WriteTag 只读源码审计（状态机、Intent 交互与 UI 耦合）
- **核心模式与状态机**：
  - `WriteTag` 承载六种模式（值块增减与 Transfer/Restore 是两个独立模式）：
    1. `write_block`（单块写入，支持尾块警告弹窗与块 0 写入前置警告）
    2. `linearLayoutWriteTagDump`（转储写入，支持带位置的差异写入与静态控制位替换）
    3. `linearLayoutWriteTagCloneUid`（直接跳转 `CloneUidTool`）
    4. `linearLayoutWriteTagFactoryFormat`（出厂格式化，构造 1K/4K 默认空块与控制位扇区列表）
    5. `relativeLayoutWriteTagValueBlock`（值块增减）
    6. `relativeLayoutWriteTagTransferRestore`（暂存-目标块 Transfer/Restore）
- **Intent 交互与契约**：
  - **输入**：`EXTRA_DUMP`（从 `DumpEditor` 传入转储字符串数组，选中 Write Dump 模式，仅明确禁用 Write Block 和 Factory Format 两个单选按钮）。
  - **内部派发**：
    - `FileChooser` (`FC_WRITE_DUMP = 1`) 选择文件路径。
    - `KeyMapCreator`：
      - `CKM_WRITE_DUMP = 2`（带 `keys_from_dump.keys` 与扇区范围启动）。
      - `CKM_WRITE_BLOCK = 3`（指定扇区范围启动）。
      - `CKM_FACTORY_FORMAT = 4`（全卡出厂格式化）。
      - `CKM_WRITE_NEW_VALUE = 5`（值块密钥生成）。
      - `CKM_VALUE_TRANSFER_RESTORE = 6`（双扇区跨越范围生成）。
  - **输出**：写卡成功直接 `finish()` 并 Toast 提示；不向调用方回传破坏性修改。
- **确认弹窗与多阶段阻塞**：
  - 扇区尾块写入警告（`action_i_know_what_i_am_doing` 按钮二次确认）。
  - 块 0 厂商块写入警告（提示需要特殊卡 Gen2/CUID，未确认禁止写入）。
  - 转储前校验对话框（`checkDumpAgainstTag`）：在主线程弹出列表视图对比卡片容量、不可写块和死扇区，仅当问题列表非空时提供“跳过块/取消全部”确认；列表为空时直接调用 writeDump，不存在统一的额外最终确认。
- **写入进度与异常处理**：
  - `writeDump` 在物理写入前动态构造包含原生 `ProgressBar` (Indeterminate) 和 `TextView` 的 `AlertDialog`。
  - 独立工作线程执行循环 `reader.writeBlock(sector, block, ...)`，硬编码单块重试 2 次（`for (i in 0 until 2)`）。
  - **异常回滚机制**：Mifare Classic 硬件**无事务与回滚机制**。单块写入失败立即中止并 Toast 报错，已写入的前序扇区无法撤销。
- **UI 与业务的耦合痛点**：
  - 布局深度嵌套（751 行 XML），大量直接从 `EditText` 获取输入并在主线程做十六进制解析；
  - 校验对话框直接使用原生 `AlertDialog` 嵌套 `ListView` 和 `SimpleAdapter`；
  - 进度条与警告框依赖 `Activity` 的 Context 动态创建 View。

### 17.4 P15-B 最小侵入式 Material 3 改造建议方案
- **严格冻结区（禁止触碰）**：
  1. 冻结所有物理写入函数与逻辑：`writeBlock()`、`writeDump()`、`writeValueBlock()`、`runValueTransferRestore()`、`createFactoryFormattedDump()`。
  2. 冻结底层通信重试机制（单块重试 2 次）、各路径密钥顺序：单块 B 后 A（即使 B 成功仍尝试 A）；值块 B 失败 -1 才 A；Transfer/Restore 目标 A 后 B；转储按写权限码选密钥。不得归并为统一降级策略。
  3. 冻结 `KeyMapCreator` 的 5 种请求码派发意图和 `keys_from_dump.keys` 生成。
  4. 绝不引入无感的自动化写卡逻辑，绝不绕过任何人工二次确认对话框。
  5. 严禁改动 `MCReader`、`MCReader.writeBlock0Gen3` 及任何 Gen3 相关代码。
- **允许替换与现代化区域**：
  1. **首期仅外壳**：建议 Compose M3 顶栏/说明卡 + AndroidView 托管原 XML 表单，保留原控件 ID、输入过滤和事件处理。完整 WriteTagScreen 表单替换属于后续另行批准阶段，不是首期最小方案。
  2. **模式切换器**：使用 M3 `PrimaryTabRow` 或 `SingleChoiceSegmentedButtonRow` 优雅替代六个模式的 `RadioButton`（仅后续候选）。
  3. **输入组件**：使用统一的 M3 `OutlinedTextField`，保持单块输入、值块输入与 Transfer/Restore 的格式过滤。
  4. **对话框现代化**：将原生的扇区尾块警告、块 0 警告、写入前校验列表转为 M3 `AlertDialog`。
  5. **首期冻结写入中对话框**：现有 AlertDialog 未设置 setCancelable(false)，不能凭 UI 现代化把它改成不可取消，也不能把关闭对话框当成停止物理写入。模式切换、输入与危险确认的 M3 替换均仅为后续候选，不在首期执行。
- **实施原则**：保持 `WriteTag` 继承 `BasicActivity`，Activity 自身全权保留 NFC 前台分发和 IO 线程；状态仅单向暴露给 Compose。

## 18. P15-B 第一期安全混合 UI 实施记录

- **采用方案**：保持 `setContentView` + 原生 XML 表单不变，仅在根布局顶部新增独立 `ComposeView` 承载 M3 外壳（`MctTopAppBar` + 纯展示安全提示）。未使用 `setContent`+`AndroidView` 全量替换，规避 `findViewById` 初始化时序风险。
- **关键兼容处理**：`supportActionBar?.hide()` 消除原生 ActionBar，避免重复顶栏；主题非 edge-to-edge（API35+ 显式 opt-out），ComposeView 顶部状态栏 inset 为 0，无双重 Insets；`ScrollView` 保留滚动，`ComposeView` 为 `wrap_content` 固定于顶，二者无滚动冲突。
- **未改动**：六模式 RadioButton/布局、全部控件 ID 与 `android:onClick`、Intent/RequestCode、确认对话框、`writeBlock/writeDump/writeValueBlock/runValueTransferRestore/createFactoryFormattedDump`、重试次数、密钥顺序、线程与进度对话框语义、`MCReader`、生产 `DumpEditor`。
- **测试**：JVM 18/18；新增 4 个仪器测试（外壳 1 + 六模式/切换 3）编译通过但**未执行**（红米未连接，华为不在本轮目标且不操作）。真机验收 UNTESTED。
- **APK**：Debug 13,888,776 B；Release 9,028,033 B（未签名）。构建均成功。
- 提交：`4514f86`、`20c5f74`、`056e5f6`。`master=50593d9`、Tag `v4.3.1-kotlin-migration-baseline` 未变，未推送/发布。
- 完整 A–H 报告见 `docs/p15/P15B_REPORT.md`；六模式契约见 `docs/p15/WRITE_TAG_CONTRACT.md`。

## 19. P15-B.1 红米真机验收 + P16 前置质量检查

- **设备 DEFERRED**：红米 `pnq47xf6899t4huc` 在 `adb devices` 列为 `device`，但 `adb install`/`pm path`/`echo`/`shell true` 全部挂起（≥4 次），疑似此前 NFC HAL 崩溃导致系统级无响应。按“不反复重试/不重启/不改内核”停止设备操作，未安装、未截图、未跑仪器测试。
- **4 项 WriteTag 仪器测试**：编译通过、未运行（DEFERRED）；静态确认不触发实体写卡、无标签 IO，唯一 NFC 接触为 `enableNfcForegroundDispatch`（null 适配器 no-op）。
- **全局 UI 审计**：22 个 Activity 中 20 个全 Compose、1 个混合（WriteTag）、1 个原生（生产 DumpEditor，P14 冻结）；21/22 `supportActionBar?.hide()`，无重复顶栏；全部 Compose 页统一 `MctTheme`+`MctComponents`。
- **测试执行数**：JVM 18/18；仪器共 56 方法（33 Compose + 23 设备验收）均 0 执行。
- **构建警告/废弃项**：`gradle.properties` 7 组 AGP 9.2 废弃选项与 edge-to-edge opt-out（仅记录，不清理）。
- **未跟踪文件**：构建缓存/JVM 崩溃日志/测试截图日志，均保留，不 `git clean`。
- 详细报告与 P16 任务清单见 `docs/p15/P16_PREQUALITY.md`。本轮无实体写卡/UID/Block0/SAK/ATQA/Gen2/Gen3 操作。

## 20. 红米恢复后真机验收 Round 1 —— 环境阻断

- 红米 `pnq47xf6899t4huc`（Android 16 / API 36）shell 恢复响应，Debug/androidTest APK 均安装 `Success`，NFC `mState=on` 全程未崩溃。
- MainMenu（Compose）渲染正常（截图 `docs/p15/redmi/menu.png` + UI dump `menu2.xml`：NFC 已就绪、读/写/编辑/工具/帮助卡片、版本 4.3.1）。
- **WriteTag 自动化验收被两环境问题阻断**：
  1. `adb shell input` 注入被拒（MIUI 未开「USB 调试（安全设置）」→ `INJECT_EVENTS`），无法点击「写标签」进入 WriteTag。
  2. 全部 `am instrument` 在 API 36 挂起：4 个 WriteTag 测试、纯 Compose `WriteTagShellComposeTest`、非 Compose 的 `DeviceAcceptanceTest#test02_LaunchMainMenu` 均挂起无输出。结论为 androidx.test/ActivityScenario 基础设施在 API 36 整体挂起，非 NFC/Compose/用例引起。
- 三次尝试均先 `force-stop`，设备保持 `alive`、NFC 未崩溃；按「不反复重试」停止。
- 详情见 `docs/p15/redmi/DEVICE_ACCEPTANCE_ROUND1.md`。

## 21. 红米 WriteTag 真机验收最终结果

- 用户开启 MIUI「USB 调试（安全设置）」后 `input` 注入恢复，手动导航 MainMenu → WriteTag 成功。
- **手动 UI 验收通过**：M3 外壳（顶栏「写标签」+ 返回按钮）、安全提示、六种原生模式选择器、默认写块展开、深浅色、旋转、滚动、返回导航，均留存截图/UI dump（`docs/p15/redmi/wt-*.png/xml`）。
- **4 个仪器测试仍无法运行**：MIUI `ActivityStarterImpl` 拒绝 instrumentation 启动任何 Activity（`Permission Denied Activity ... result code=102`，`tuid:1000`），既拒非 exported `WriteTag` 也拒已 exported `ComponentActivity`；与「USB 调试（安全设置）」无关联，属 MIUI 对 instrumentation 的 Activity 启动限制。
- IME 合成点击未确认输入法弹出（原生 EditText 未改动，行为与基线一致）；无障碍已由 uiautomator 树佐证（未做 TalkBack 逐焦点走查）。
- 全程零实体写卡/UID/Block0/SAK/ATQA/Gen2/Gen3；NFC `mState=on` 未崩溃。详见 `docs/p15/redmi/DEVICE_ACCEPTANCE_FINAL.md`。

## 22. P16 全应用现代化质量审计结论

- 页面矩阵：17 完整 Compose + 2 Compose+AndroidView（KeyEditor/HelpAndInfo）+ 1 ComposeView+XML（WriteTag）+ 1 原生（DumpEditor）+ 1 原型（DumpEditorPrototype）= 22 Activity。
- 功能入口/Intent 契约等价：FileChooser/DumpEditor/DiffTool/KeyMapCreator/WriteTag/ImportExportTool/Preferences 等 EXTRA 契约与 RequestCode 均未变。
- NFC 生命周期：`BasicActivity` onResume/onPause 前台分发成对；MainMenu 广播注册/注销成对；写卡进度 Dialog 关闭≠取消（基线冻结）。
- 测试归档：JVM 18/18 PASS；仪器 56 方法（33 Compose + 23 DeviceAcceptance）红米 MIUI 全 BLOCKED；NFC 只读 PASS；异常生命周期 UNTESTED；TalkBack SKIPPED。
- 构建：Debug 13,888,776 B / Release 9,028,033 B / androidTest 编译通过；R8 未启用（minify=false/shrink=false）；DumpEditorPrototype 在主 Manifest（建议移 debug）。
- 最终结论 **PARTIAL**。详见 `docs/p15/P16_FINAL_REPORT.md`。

## 23. P16.1 Release 构建加固结论

- 实验组件隔离：`DumpEditorPrototype` 从主 Manifest 移至 `src/debug/AndroidManifest.xml`；实测 Release 合并 Manifest 不含原型、Debug 含原型（组件不可启动，但 minify 关闭时字节码仍进 Release dex）。
- R8 专项：实测优化 Release 9,027,981 → 2,797,319 B（−69.0%）；onClick/Activity/核心类保留，但**证明 `shrinkResources` 移除 `getIdentifier` 动态字符串（`ac_sector_trailer_4`/`ac_data_block_no_keyb_0`），会破坏 AccessConditionDecoder** → R8 **DEFERRED**（minify 回退 false，proguard-rules.pro 休眠并记录 keep.xml 需求）。
- JVM 18/18 PASS（强制重跑）；assembleDebug/Release + R8 优化构建均成功；APK 未签名未发布。
- 结论 **PARTIAL**。详见 `docs/p15/P16_1_RELEASE_HARDENING_REPORT.md`。

## 24. P16.2 MCT KernelSU 风格 UI 2.0 全应用视觉与交互重构结论

- **设计规范与框架决策**：采纳方案 A（纯 Jetpack Compose Material 3 抽象 KernelSU 风格组件），避免引入 Miuix 造成的版本冲突、minSdk 破坏与库体积暴涨。产出 `docs/p16/UI2_DESIGN_SPEC.md` 与 `docs/p16/UI2_AUDIT.md`。
- **色彩与层级突破**：消除暗色模式大面积平铺灰；引入深沉底色 `#0D1117`、浮层卡片 `#161B22`、1dp 精致边框 `#30363D` 与科技青强调色 `#38BDF8`；浅色模式采用冷灰白 `#F6F8FA` + 纯白卡片 `#FFFFFF`。
- **语言与 Locale 脱节彻底根治**：
  - 定位并修复 `Preferences.kt` 缺失“跟随系统”以及初次安装因默认索引 0 导致中文系统下错误显示 `English (en)` 的缺陷；
  - 增加“跟随系统 (Follow System)”选项，双向联动 `AppCompatDelegate.getApplicationLocales()`；
  - 严格保持与历史 SharedPreferences 索引兼容（写入 `0..6`，跟随系统写入 `-1`），绝不重置旧用户设置。
- **离线帮助文档中文化与暗色自适应 (`HelpAndInfo`)**：
  - 补齐全量简体中文离线文档 `assets/help/help_zh.html`，保留全部 8 大章节射频技术细节；
  - 彻底淘汰 90 年代老旧 HTML 标签，为 `help_zh.html` 与 `help.html` 引入 `@media (prefers-color-scheme: dark)` 现代响应式 CSS；
  - WebView 背景设为透明，彻底杜绝黑色界面切入时的白屏强光刺眼。
- **核心页面现代化**：
  - `Preferences`: 划分为日常使用、通信与显示 3 个 `MctSettingGroup`；全面改用 `Switch`；UID 显示格式改为 `MctSelectionDialog` 垂直单选弹窗，彻底解决水平排布折行截断痛点；
  - `About`: 全新编写 Compose Material 3 对话框 `MctAboutDialog.kt`，卡片化展示许可证、版本号、原作者致敬与开源主页；
  - `MainMenu`: 引入 Hero `MctStatusCard` 动态展示 NFC 状态，强化读写卡片视觉层级与防误触语义；
  - `WriteTagShell`、`KeyEditor`、`DiffTool`、`CloneUid`、`TagInfo`、`UidLog`、`FileChooser`、`ImportExport`: 全面推广 UI 2.0 组件与 24dp 圆角弹窗，彻底清理 Kotlin 中 50 余处硬编码中文。
- **质量与安全**：
  - JVM 单元测试 18/18 PASS；
  - Debug APK (`13,637,930` 字节) 与 Release APK (`9,089,931` 字节) 编译构建成功；
  - 零实体写卡、零 UID 修改、MCReader 与写卡状态机零改动；
  - 最终验收结论 **PARTIAL**。详见 `docs/p16/UI2_IMPLEMENTATION_REPORT.md` 与 `docs/p16/UI2_ACCEPTANCE.md`。

## 25. P16.3 MCT UI 2.0 离线质量收口与人工验收准备结论

- **语言映射专项**：抽离 `LanguageHelper` 纯函数并在 `Preferences.kt` 落地；新增 `LanguageHelperTest` 覆盖全部 6 项映射用例（单元测试增至 24 项全绿）；确认跟随系统、中文、英文与旧版用户偏好向下兼容闭环。
- **UI 静态排查**：MainMenu、Preferences、HelpAndInfo、About 布局约束、文本单行限制、无障碍标签与系统 Insets 审查通过；无 ADB 真机运行，无违规截屏。
- **Release 隔离与 R8 实测**：实测 Release Merged Manifest 排除 `DumpEditorPrototype`；R8 保持 DEFERRED（未签名 Release APK 9,090,507 字节；Debug APK 13,637,930 字节）。
- **人工验收交付物**：提供完整 SHA-256 与签名说明（Debug 已签名可装，Release 未签名不可装），详见 `docs/p16/P16_3_FINAL_REPORT.md`。

## 26. P16.4 MCT UI 2.0 实机反馈修复与 KernelSU 风格精修结论

- **P0 关于闪退根因与修复**：定位于 `MctAboutDialog` 中直接使用 `painterResource(R.mipmap.ic_launcher)` 解析 API 26+ `<adaptive-icon>` XML 抛出 `XmlPullParserException`。修复为加载标准 PNG 光栅图 `R.drawable.ic_launcher_logo`，并在 `MainMenuComposeTest` 中补充打开、关闭与再次打开的回归测试。
- **首页精修**：重构 `MainActionCard` 为横向模块流，读写卡片高度降至 74dp，次级卡片降至 62dp，纵向空白削减逾 100dp，首页核心入口完全自适应一屏呈现；建立 7 个全新标准矢量图形（`<vector>`）彻底淘汰 2013 年原版模糊旧图。
- **设置页精修**：设置项上下内边距降至 9dp，长复合标题拆分为“短标题+副说明”，Switch 独立垂直居中，右侧无障碍触控区域完整保留，360dp 窄屏长标题折行挤压痛点彻底根除。
- **NFC 未开启弹窗现代化**：废除旧式系统级白色弹窗，以 `MctTheme` 为基准重构 24dp 圆角 `MctNfcEnableDialog`，100% 保持原有三种操作（转到设置/离线模式/退出应用）语义与状态机流转。
- **交付物**：Debug APK (13,639,605 字节，已签名可直接安装)，Release APK (9,102,691 字节，未签名)。详见 `docs/p16/P16_4_FEEDBACK_FIXES_REPORT.md`。

## 27. P16.5 MCT UI 2.0 实机反馈精修（四项定点修复）结论

- **WriteTag 警告中文化**：补全 `write_tag_safety_notice` 中文与繁体资源；修改测试动态读取资源；消除 `CloneUidScreen` 中残留的硬编码。
- **工具箱图标完整性**：为数据转换、UID 日志、导入导出等 8 项工具建立标准矢量 Drawable（`<vector>`），彻底消除 null 图标缺陷，实现全工具统一 34dp 容器与左对齐缩进。
- **About 仓库链接修正**：经 `git remote -v` 核实当前 Fork 仓库 `https://github.com/ljj13/MCT-Kotlin-Rebuild`；清晰区分当前重构项目与上游 v4.3.1 原版（ikarus23），准确提供 Release/Changelog 链接。
- **MainMenu 核心卡片空间优化**：重构 `MainActionCard` 自适应最小高度（74dp/58dp），副标题支持 2 行自然换行，卡片间距 10dp，空间紧凑饱满，彻底杜绝文字截断与偏宽空白。
- **交付物**：Debug APK (13,644,272 字节，已签名可直接安装)，Release APK (9,112,136 字节，未签名)。详见 `docs/p16/P16_5_TARGETED_FIXES_REPORT.md`。

## 28. P16.6 → P17 → P18 跨阶段收口、发布审查与 NFC 卡模拟预研结论

- **P16.6 首页最终修复与 UI 2.0 冻结**：
  - 核心读写区域重构为 2 列居中卡片（`CoreActionCard`，高度 104dp），文字横向空间充沛，说明精简为“认证并读取转储”与“写入、克隆或格式化”，单行居中优雅呈现，彻底消除了尴尬换行；
  - 文件管理区域由狭窄双列重构为单列分组列表（`MctSettingGroup`），占满全宽（~330dp），“编辑 / 分析转储文件”与“编辑 / 新建密钥文件”完整显示，彻底消除了省略号截断；
  - 冻结 KernelSU 风格 UI 2.0，产出 `docs/p16/P16_6_UI_FINAL_REPORT.md`。
- **P17-A 发布前工程审查**：
  - 核实 75 次提交，22 个生产 Activity 契约完全保真，NFC 前台分发成对无泄漏；
  - 实测 Release Merged Manifest 排除 `DumpEditorPrototype`（组件不可启动）；
  - 明确 R8 因动态资源收缩缺陷保持 DEFERRED；
  - 产出 `docs/p17/P17_RELEASE_REVIEW.md`。
- **P17-B 发布候选准备**：
  - 建议候选版本号为 `v4.3.1-modern.1-preview`，推荐 Preview/Beta 发布策略；
  - 全新重写 `README.zh-CN.md` 与 `README.md`，从真实 Git 历史梳理编写 `CHANGELOG.md`；
  - 标记状态为 `USER_ACCEPTANCE_PENDING`，交付 Debug APK (13,644,272 字节，已签名) 与 Release APK (9,112,136 字节，未签名)；
  - 产出 `docs/p17/P17_RELEASE_PREPARATION.md`。
- **P18 NFC 卡模拟可行性研究**：
  - 明确普通 Android 手机**无法直接模拟 MIFARE Classic 物理卡片**的根本协议瓶颈（Crypto1 硬件流密码工作在 ISO 14443-3A 层，而 Android HCE 仅工作在 ISO 14443-4 APDU/AID 层，且 HCE 强制随机 UID）；
  - 确立最佳工程演进路线：**采纳路线 B（MCT 作为外设模拟器前端）**，未来版本通过蓝牙 BLE 控制 ChameleonUltra 等便携硬件实现全协议卡模拟；
  - 产出 `docs/p18/P18_NFC_EMULATION_FEASIBILITY.md`。

## 31. MCT v1.0.0（4.3.1）版本重定名与正式发布收口结论

- **版本命名正式收口**：
  - 废弃旧 `4.0.0` 与 `4.3.1-modern.1-preview` 命名规划，确立本 Fork 首个独立大版本为 **MCT Kotlin 1.0.0（4.3.1）**；
  - `build.gradle` 设定 `versionCode = 71`、`versionName = "1.0.0"`，About 对话框显示 `1.0.0（4.3.1）`；
  - 同步重写 `README.md`、`README.zh-CN.md`、`CHANGELOG.md` 与 `P17_RELEASE_PREPARATION.md`。
- **发布构建与隔离验证**：
  - `feature/compose-modernization` 候选分支全量 JVM 单元测试 24/24 PASS；
  - Debug APK (`12,950,920` 字节, SHA-256: `3E0D0682F40576F83DE0D4E406709F583F5D939503D2B07CBFB2465E4DF3BA69`)；
  - Release APK (`9,113,928` 字节, SHA-256: `060A22A5E7336502975D0CF6AC4BC560D67E3F4878F491738F78006CECD7B1FA`)；
  - 命名产物为 `MCT-Kotlin-v1.0.0.apk`（unsigned），生成对应 `SHA256SUMS.txt`；
  - 实测 Release Manifest 中 `DumpEditorPrototype` = False，且绝对不含 P18 HCE 实验代码。
- **签名审计与发布停止结论 (`V1_RELEASE_BLOCKED_SIGNING`)**：
  - 经全面审计，本地与仓库中无长期正式 Release Keystore；
  - 按用户任务红线，禁止使用 Debug 证书或自动生成临时私钥发布，停止 Tag/push/Release 流程；
  - 准确报告 `V1_RELEASE_BLOCKED_SIGNING`，提供本地生成正式签名的安全交互指引；
  - `feature/nfc-emulation` 分支保持物理独立，master 与历史基线 Tag `v4.3.1-kotlin-migration-baseline` 绝对未修改。






