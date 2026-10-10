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
- **工作区状态**: Clean（无未提交的更改），未自动合入 `master`。
