# P16.1 RELEASE HARDENING REPORT —— Release 构建加固、实验组件隔离与发布审查准备

> 分支 `feature/compose-modernization`，起点 HEAD `81fc721` → 终点 HEAD `96d9daa`。
> 继承 `P16_FINAL_REPORT.md` 全部 PASS/FAIL/BLOCKED/UNTESTED/SKIPPED/UNMEASURED 状态，不覆盖历史结论。
> 未触碰 NFC 底层算法、写入状态机、密钥遍历顺序、生产 DumpEditor、已验证读取链路。未实体写卡/UID/Block0/Gen2/Gen3。未建 Tag、未合并 master、未推送、未签发/发布。

---

## 1. Release 实验组件隔离证据

- 将 `DumpEditorPrototype` 的 `<activity>` 声明从 `src/main/AndroidManifest.xml` 移除，新增 `src/debug/AndroidManifest.xml`（Build Variant / Manifest 合并机制）。
- **最终合并 Manifest 实测**（非仅源码宣称）：
  - Release 合并 Manifest：`DumpEditorPrototype` **不存在** ✅
  - Debug 合并 Manifest：`DumpEditorPrototype` **存在** ✅（原型保留，便于将来开发）
  - 生产 `DumpEditor` 在两者中均存在 ✅（Intent 契约不受影响）
- 原型无 Deep Link、无调试入口暴露；生产 `DumpEditor` 及其 `EXTRA_DUMP` 契约未改。
- **字节码诚实区分**：R8 未启用（minify=false），且 `DumpEditorPrototype.kt` 源码仍位于 `src/main`，因此其**字节码仍打包进 Release dex**，但 Release Manifest 不声明该组件 → 属「**组件不可启动**」（component not launchable），**非**「代码未进入 Release」。若 P17 要求彻底排除原型代码，需将 `Activities/DumpEditorPrototype.kt` + `ui/dumpeditor/*` 源码移至 `src/debug`（变体级源码隔离），本次未做以避免影响 androidTest 引用与 P14 证据。

---

## 2. R8 开启前后配置与 APK 对比

| 状态 | 配置 | Release APK 字节数 |
| :--- | :--- | ---: |
| 未优化（P16 基线，含原型声明） | `minifyEnabled=false` `shrinkResources=false` | 9,028,033 B |
| 未优化（本轮，隔离后） | `minifyEnabled=false` `shrinkResources=false` | 9,027,981 B |
| **R8 优化（本轮实验，已回退）** | `minifyEnabled=true` `shrinkResources=true` + `proguard-android-optimize.txt` | **2,797,319 B** |

- **实际压缩比**：2,797,319 / 9,027,981 ≈ **31.0%**（体积减少 **69.0%**，约 3.23×）。
- 优化构建 `minifyReleaseWithR8` + `shrinkResources` + `packageRelease` + `lintVitalRelease` 均成功（`BUILD SUCCESSFUL in 3m 6s`）。
- R8 符号化材料已生成：`mapping.txt`（34 MB）、`seeds.txt`、`usage.txt`、`resources.txt`、`configuration.txt`。

---

## 3. 混淆与资源收缩兼容性检查

| 检查项 | 结果 |
| :--- | :--- |
| XML `android:onClick` 方法 | ✅ 保留（seeds.txt 实测含 `onChangeWriteMode`/`onWriteBlock`/`onWriteDump`/`onCloneUid`/`onFactoryFormat`/`onWriteValue`/`onUpdateColors`） |
| Activity 与 Manifest 入口 | ✅ 保留（`WriteTag`/`MainMenu`/`Common`/`DumpEditor` 在 seeds.txt） |
| 核心类 MCReader | ✅ 保留但被混淆（mapping.txt 显示 `ReadTag.readTag$lambda$7(MCReader)` 仍引用） |
| 实验原型 DumpEditorPrototype | ✅ 被 R8 剥离（seeds.txt 无） |
| Intent Serializable/Parcelable | ✅ 无自定义类（均平台类 `Intent`/`Tag`/`HashMap`） |
| SharedPreferences 兼容 | ✅ 无反射/序列化风险 |
| WebView 离线帮助资源 | ✅ assets 不受 shrinkResources 影响（`assets/help/help.html` 等） |
| NFC 平台调用 | ✅ 平台 `android.nfc.*` 不被剥离 |
| 反射/动态加载 | ✅ 无 `Class.forName`/`getMethod`/`invoke()` |
| **`Resources.getIdentifier()` 动态资源** | ❌ **发现收缩缺陷（见下）** |

**关键缺陷证据**：`shrinkResources` 移除了仅经 `Resources.getIdentifier()` 引用的字符串资源——`resources.txt` 显示 `ac_sector_trailer_4`、`ac_data_block_no_keyb_0` 被列为移除（无 `reachable=true`），而 `ac_data_block_0..5`、`tag_*` 系列被保留。这会导致 **AccessConditionDecoder 在运行时 `getIdentifier` 返回 0、功能失效**（TagInfoTool 的 `tag_*` 本次侥幸保留，但同样依赖不可靠的静态追踪）。

**结论**：R8 + `shrinkResources` 对 `getIdentifier` 动态资源引用**不可靠**，无法在不经真机运行验证的情况下确认安全。

---

## 4. Manifest 与权限审计

| 项 | 结论 |
| :--- | :--- |
| 包名/组件声明 | `de.syss.MifareClassicTool`；22 Activity + 1 alias + FileProvider + AppLocales 服务 |
| `android:exported` | MainMenu + MainMenuAlias `true`；其余 `false`（最小暴露） |
| Debug/androidTest 专用组件 | `ui-tooling`/`ui-test-manifest` 仅 `debugImplementation`；`DumpEditorPrototype` 仅 Debug Manifest |
| 系统权限 | 仅 `android.permission.NFC`（最小化）✅ |
| NFC Intent Filter | MainMenuAlias `TECH_DISCOVERED` + `@xml/nfc_tech_filter` ✅ |
| minSdk/targetSdk/compileSdk | 21 / 35 / 35 ✅（未升级） |
| Debug 与 Release 差异 | 仅 `DumpEditorPrototype` 声明差异 ✅ |
| 测试依赖进入 Release | 无（均 debug/androidTest 作用域）✅ |
| APK 签名/安装/发布 | `app-release-unsigned.apk` **未签名、未安装验证、未发布** |

---

## 5. 实际测试运行统计

| 类别 | 结果 |
| :--- | :--- |
| 本轮 JVM 单元测试（`BaselineTest`，强制重跑） | **18/18 PASS**（failures=0 errors=0） |
| 本轮构建 | `assembleDebug` ✅（13,888,776 B）、`assembleRelease` ✅（9,027,981 B）、R8 优化构建 ✅（实验，已回退） |
| 本轮静态检查 | 合并 Manifest 隔离 ✅、R8 seeds/mapping/resources 检查 ✅ |

**历史状态继承（不重跑、不覆盖）**：
- OPPO 历史 PASS：4 项隔离 UI 测试 + NFC 真实只读链路（P15-A）。
- 红米 MIUI 阻断：56 项仪器方法（33 Compose + 23 DeviceAcceptance）BLOCKED。
- 编译通过但未执行：同上 56 项（红米 0 执行）。
- NFC 异常生命周期：UNTESTED。
- 实体写卡：UNTESTED（全程禁止）。
- 性能：UNMEASURED。

未为全绿修改 MIUI 设置、Root 权限或 NFC 驱动。

---

## 6. 历史 PASS / BLOCKED / UNTESTED 状态继承

完整继承 `P16_FINAL_REPORT.md` 五类状态，本阶段仅新增：Release 实验组件隔离（PASS，实测）、R8 收缩缺陷（DEFERRED 证据）。历史 PASS 未清零，也**未**将历史 PASS 视为当前 Release APK 已验收。

---

## 7. 未跟踪文件分类及证据保护

逐个审计 10 个未跟踪文件：

| 文件 | 分类 |
| :--- | :--- |
| `keymap_creator.png` / `keymap_range.png` / `keymap_selected.png` / `read_tag_entry.png` / `read_tag_waiting.png`（根目录） | **与已提交 `docs/p15/` 逐字节重复**（SHA256 相同），冗余副本 |
| `read_tag_waiting2.png`、`Mifare Classic Tool/read_tag_waiting.png` | 独立残留截图（未归档） |
| `keymap_instrument.txt` | KeyMapCreator 测试崩溃记录（`Process crashed`，无敏感数据） |
| `keymap_instrument.err` | 空文件 |
| `uidump.xml` | UI dump（扫描无 UID/ATQA/SAK/序列号等敏感文本） |

- 文本文件无敏感数据；PNG 可能含卡片 UID，未做自动脱敏（需人工复核后决定是否外发）。
- **未执行 `git clean -fdx`、未批量删除任何原始证据**；5 个逐字节重复副本建议清理但保持原状。

---

## 8. 修改文件与独立 Git 提交

| 提交 | 内容 |
| :--- | :--- |
| `3b6a822` | `chore(p16.1): 隔离实验性 DumpEditorPrototype 到 Debug 变体`（改主 Manifest + 新增 debug Manifest） |
| `96d9daa` | `build(p16.1): 添加 R8 最小保留规则（休眠，minify 关闭并记录资源收缩风险）`（新增 proguard-rules.pro） |

- `build.gradle` 无净变更（R8 实验后回退 `minifyEnabled=false`）。
- 未升级 AGP/Kotlin/Compose BOM/targetSdk。

---

## 9. master / Tag / 远程状态

- `master = 50593d9`、Tag `v4.3.1-kotlin-migration-baseline = e156d4c`（树内容 `b52568c8…`）**均未变**。
- HEAD `96d9daa`；远程 `origin`（github）**未推送**。

---

## 10. P17 发布审查准入结论

**PARTIAL**

- **已完成**：实验组件隔离（Release Manifest 实测排除 + Debug 保留）、R8 专项实验（实测优化 69.0% 并定位到资源收缩缺陷）、Manifest/权限审计、JVM 18/18、Debug/Release 构建、静态验证、证据分类归档、独立中文提交、master/Tag 不可变。
- **明确延期项（阻断进入 READY）**：
  1. **R8 DEFERRED**——已证明 `shrinkResources` 会移除 `getIdentifier` 动态字符串（`ac_sector_trailer_4`/`ac_data_block_no_keyb_0`），需在 P17 补充 `res/values/keep.xml`（`tools:keep="@string/ac_*,@string/tag_*"`）并**真机运行验证**后方可启用。
  2. 56 项仪器测试红米 MIUI BLOCKED（0 执行，仅编译通过）。
  3. NFC 异常生命周期 UNTESTED、实体写卡 UNTESTED、性能 UNMEASURED、TalkBack SKIPPED。

**结论依据实际证据，不因构建成功而宣称正式 Release 已通过。** 未自动合并、未推送、未签发、未发布。
