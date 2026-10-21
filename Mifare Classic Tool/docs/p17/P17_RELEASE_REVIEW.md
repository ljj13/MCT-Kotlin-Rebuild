# P17-A 发布前最终工程审查报告 (P17_RELEASE_REVIEW.md)

> 分支：`feature/compose-modernization` (HEAD `951330b`)
> 审查基线：`master = 50593d9`，Tag `v4.3.1-kotlin-migration-baseline = e156d4c`（树内容一致 `b52568c8…`，均绝对未动）。
> 性质：首个 Kotlin + Compose Material 3 现代化版本（UI 2.0 冻结版）的发布候选审查。

---

## 1. Git 状态与历史基线审计

| 审计项 | 实测结果 | 结论 |
| :--- | :--- | :---: |
| **当前分支与 HEAD** | `feature/compose-modernization` (HEAD `951330b`) | ✅ 正常 |
| **主分支 master** | `50593d96e9478a4172488b948cd7bc921e31e8ed` | ✅ 绝对未修改、未合并 |
| **基线 Tag** | `v4.3.1-kotlin-migration-baseline = e156d4c` | ✅ 绝对未修改、未移动 |
| **现代化提交数** | 自基线起累计 **75 个** 独立中文 Conventional Commits | ✅ 历史清晰 |
| **代码修改范围** | 220 个文件变动 (`+42,601 / -2,290` 行) | ✅ 范围受控 |
| **工作区未跟踪文件** | 10 个残留调试截图与转储日志（经审计无敏感密钥泄漏，未执行 git clean） | ✅ 证据妥善保存 |

---

## 2. 全功能等价性审计

对照原 Kotlin 稳定基线，对全应用 22 个 Activity 生产组件逐项核实：

| 业务模块 / Activity | 架构分类 | 契约与功能等价性核实 | 状态 |
| :--- | :--- | :--- | :---: |
| **MainMenu** | 全 Compose | StartUpNode 状态机、NFC 动态广播监听、全部工具入口映射、关于/设置入口完全保留 | **PASS** |
| **ReadTag** | 全 Compose | 字典攻击映射流程、扇区范围过滤、贴卡等待与转储结果呈现完全保留 | **PASS** |
| **KeyMapCreator** | 全 Compose | `EXTRA_KEYS_DIR`、`EXTRA_SECTOR_CHOOSER_*`、`EXTRA_USE_KEYS_FROM_DUMP` 契约保真 | **PASS** |
| **WriteTag** | 混合 (ComposeView + XML) | 六模式表单 (`write_block`, `write_dump`, `clone_uid`, `factory_format`, `write_value_block`, `writetag_transfer_restore`)、非统一密钥认证时序、写卡确认与状态机 **100% 冻结** | **PASS** |
| **DumpEditor (生产)** | 原生 View (P14 冻结) | 生产完全保留原生布局与编辑逻辑，`EXTRA_DUMP` 契约保真，未被实验原型替换 | **PASS** |
| **KeyEditor** | Compose + AndroidView | AppCompatEditText 原生编辑核心保留，保存、分享、去重功能等价 | **PASS** |
| **FileChooser** | 全 Compose | `RESULT_OK`、取消、`EXTRA_CHOSEN_FILE`、`EXTRA_CHOSEN_FILENAME` 契约保真 | **PASS** |
| **ImportExportTool** | 全 Compose | SAF DocumentPicker 授权、内置目录导入导出、ZIP 备份还原流程保真 | **PASS** |
| **DiffTool** | 全 Compose | MCDiffUtils 纯算法保真，双转储选取与字节级高亮差异比对保真 | **PASS** |
| **CloneUidTool** | 全 Compose | UID 格式输入、BCC 算法计算、魔术卡（Gen2）参数生成保真（绝不触发自动写入） | **PASS** |
| **TagInfoTool** | 全 Compose | 芯片型号动态推测、ATQA/SAK/ATS 矩阵计算、MFC 硬件支持检测保真 | **PASS** |
| **Preferences** | 全 Compose | 10 项 SharedPreferences 键值保真，`LanguageHelper` 双向同步与旧版兼容性保真 | **PASS** |
| **HelpAndInfo** | Compose + AndroidView | Chromium WebView 离线加载，中英双语文档自动分流，深色自适应保真 | **PASS** |
| **UidLogTool** | 全 Compose | `uid-log-file.txt` 数据解析、倒序卡片流、一键复制与清空二次确认保真 | **PASS** |
| **独立计算工具 (7个)** | 全 Compose | BccTool, HexToAscii, ValueBlockTool, ValueBlocksToInt, AccessConditionTool, AccessConditionDecoder, DataConversionTool 纯算法无业务漂移 | **PASS** |
| **BasicActivity 基类** | 原生抽象基类 | `onResume` / `onPause` 成对注册与注销 NFC 前台分发系统，`onNewIntent` 新卡分发保真 | **PASS** |

---

## 3. Manifest 与实验功能隔离审计

### 3.1 实验性 DumpEditorPrototype 隔离确认
- **Manifest 隔离实测（非源码宣称）**：
  - `src/main/AndroidManifest.xml`：无 DumpEditorPrototype 声明；
  - `src/debug/AndroidManifest.xml`：包含 DumpEditorPrototype 声明；
  - **Release 合并 Manifest**：`DumpEditorPrototype` = **False**（绝对未暴露在生产包中，生产 DumpEditor 正常保留）；
  - **Debug 合并 Manifest**：`DumpEditorPrototype` = **True**（保留便于后续实验研究）。
- **组件状态与字节码区分**：
  - 因 R8 混淆关闭，`DumpEditorPrototype.kt` 源码仍在 `src/main`，故其字节码仍被编译打包进 Release dex；
  - 严谨界定为：**“组件不可启动 (Component Not Launchable)”**，非“代码物理未进入 Release”；
  - 生产包无该 Activity 任何入口、无 Deep Link、无 IntentFilter，用户无法唤起。

### 3.2 权限与 SDK 审计
- **系统权限**：仅申请 `android.permission.NFC`，无冗余多余权限，权限最小化；
- **SDK 版本**：`minSdk = 21`，`targetSdk = 35`，`compileSdk = 35`（严格遵从基线，未盲目升级）；
- **组件导出**：仅 `MainMenu` 与 `MainMenuAlias` 声明 `exported="true"`，其余 Activity 全部 `exported="false"`，外部攻击面最小。

---

## 4. R8 与 Release 构建审查

| 构建维度 | 实际配置 | 依据与安全性评估 |
| :--- | :--- | :--- |
| **minifyEnabled** | `false` | **保持 DEFERRED 状态**。P16.1 实测优化构建虽可减小体积 69%，但证明其存在动态资源误删风险。 |
| **shrinkResources** | `false` | **保持 DEFERRED 状态**。P16.1 实测证明 `shrinkResources` 会误剔除通过 `Resources.getIdentifier()` 动态引用的 `ac_sector_trailer_*` 资源，破坏权限解码功能。 |
| **混淆规则储备** | `proguard-rules.pro` | 已添加 Activity反射与 onClick 保留规则，作为未来设备验证后的加固储备。 |
| **工程原则遵循** | 稳定性第一 | **功能正确性与稳定性高于 APK 体积优化**，不冒功能损坏风险盲目开启 R8。 |

---

## 5. 自动化测试与证据归档

| 测试类别 | 统计数量 | 执行结果 | 详细说明 |
| :--- | :---: | :---: | :--- |
| **JVM 单元测试** | 24 项 | **24/24 PASS** | `BaselineTest` 18 项全部通过；`LanguageHelperTest` 6 项全部通过 |
| **AndroidTest 编译** | 57 项 | **COMPILE PASS** | 包含关于弹窗生命周期测试在内的 57 个方法编译全部通过 |
| **红米真机仪器测试** | 57 项 | **BLOCKED** | 红米 Note 8 Pro 处于离线状态，且其 MIUI `ActivityStarterImpl` 拦截 instrumentation 启动 Activity（`result code=102`） |
| **OPPO 实体卡读卡测试** | 16 扇区/64 块 | **PASS (历史继承)** | P7 与 P15-A 在 OPPO Find X8s+ 上完成 16 扇区只读闭环，转储哈希与官方原版完全一致 |
| **实体卡写卡测试** | — | **UNTESTED** | 涉及实体卡损坏与不可逆写入风险，全流程严格禁止自动实体写入 |
| **异常生命周期中断** | — | **UNTESTED** | P15-A 识别的扫描中快速移开/断连等异常场景保持原有未测状态 |

---

## 6. 审查结论

**PARTIAL (发布审查准入通过，等待人工实机验收)**：
- 代码质量、全功能等价、国际化、布局稳定性、Manifest 隔离与构建完全合格；
- 实体写卡与异常硬件生命周期诚实标记为 UNTESTED；
- 具备进入 P17-B 发布候选准备阶段的全部条件。
