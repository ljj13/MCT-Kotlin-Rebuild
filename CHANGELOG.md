# MCT Kotlin Modernization Changelog

本文档记录了 [MCT-Kotlin-Rebuild](https://github.com/ljj13/MCT-Kotlin-Rebuild) 分支自上游原版 `v4.3.1` (commit `50593d9`, tag `v4.3.1-kotlin-migration-baseline`) 起的完整演进与现代化重构历程。

---

## [v4.3.1-modern.1-preview] - 2026-10-20 (Release Candidate)

### 1. Kotlin 架构完全重构 (Kotlin Migration)
- **100% Kotlin 代码库迁移**：全量 26 个核心业务类、Activity 与工具算法从遗留 Java 完整迁移至 Kotlin 2.2.10，消除了隐式空指针风险，源码净缩减逾 2,800 行；
- **核心算法保真**：`Common`、`MCDiffUtils`、`MCReader` 核心算法全部配备离线 JVM 单元测试基线（`BaselineTest` 18/18 PASS）；
- **反射与序列化契约保护**：所有 Intent Extra 传递（`Array<String>` 形式 Dump、`HashMap` 序列化等）与 Java 互操作注解严格对齐。

### 2. UI 2.0 现代技术工具体系 (KernelSU Style UI 2.0)
- **设计语言演进**：参考 [KernelSU Manager](https://github.com/tiann/KernelSU) 建立紧凑、高对比、层级鲜明的技术工具视觉体系；
- **深色模式层次突破**：告别大面积平铺灰色，引入深沉底色 `#0D1117`、浮层卡片 `#161B22`、浮动弹窗 `#21262D` 与 1dp 精致边框 `#30363D`；
- **全矢量图标库**：新增 15 个标准 Android VectorDrawable（`<vector>`），彻底淘汰 2013 年原版 32x32 模糊低清 PNG 图标；
- **首页响应式重构 (`MainMenu`)**：
  - NFC 状态 Hero 卡片：实时响应硬件开关与仅编辑器模式；
  - 核心读写区域：重构为 2 列纵向居中卡片（高度 104dp），文字横向空间充沛，彻底杜绝折行；
  - 文件管理区域：由狭窄双列重构为单列分组列表（`MctSettingGroup`），完整展示“编辑 / 分析转储文件”与“编辑 / 新建密钥文件”，彻底消除省略号截断；
  - 工具与帮助区域：紧凑 2 列 58dp 卡片，全屏垂直空白削减逾 100dp，标准手机屏幕一屏完整呈现。
- **设置页紧凑列表 (`Preferences`)**：
  - 划分为“日常使用”、“通信与认证”、“显示与语言”三大分组卡片；
  - 设置行间距缩减至 9dp，行高降至 50dp；
  - 复合长标题拆解为短标题与副说明，Switch 独立垂直居中，彻底消除 360dp 窄屏长标题挤压；
  - UID 显示格式改由垂直单选弹窗呈现，彻底解决单行 RadioButton 折行截断。
- **关于界面现代化 (`About`)**：
  - 全新 Compose Material 3 对话框（`MctAboutDialog`），彻底淘汰旧式平台文本弹窗；
  - 修复自适应图标解析闪退缺陷；
  - 明确划分当前重构 Fork 仓库（`ljj13/MCT-Kotlin-Rebuild`）与上游原版（`ikarus23/v4.3.1`），清晰提供 Release 标签与致敬信息。
- **NFC 未开启提示弹窗现代化**：
  - 废除老旧白色系统弹窗，基于 `MctTheme` 重建 24dp 圆角 `MctNfcEnableDialog`，100% 保持转到设置、离线模式、退出应用原有三种业务语义。

### 3. 国际化与语言系统彻底治理 (Localization)
- **首选项语言脱节根治**：抽离纯函数工具类 `LanguageHelper`，增加“跟随系统 (Follow System)”选项，双向联动 `AppCompatDelegate.getApplicationLocales()`，彻底根除“中文系统下打开设置错误显示为 English”的缺陷；
- **历史偏好向下兼容**：保留对历史 `custom_app_language` 索引（`0..6`）的无缝兼容识别，旧用户升级配置不丢失；
- **离线帮助文档中文化 (`help_zh.html`)**：新增全文 8 个大章节的地道简体中文离线文档，排版现代化，保留全部射频专业术语；
- **网页暗色自适应**：重构 `help.html` 与 `help_zh.html`，引入 CSS `@media (prefers-color-scheme: dark)`，消除进入页面时的白屏刺眼；
- **零硬编码中文清理**：清理 Kotlin 源码中 50 余处硬编码字符串，全量接入 `strings.xml` 资源管理。

### 4. 专业编辑器混合架构与安全边界保护
- **生产 DumpEditor 原生冻结**：完全保留生产版原生 `DumpEditor` 布局与多线程编辑逻辑，绝不盲目替换；
- **实验原型隔离**：将 P14 实验性 `DumpEditorPrototype` 隔离至 `src/debug/AndroidManifest.xml`，实测最终 Release Manifest 绝对不暴露实验入口；
- **WriteTag 写入状态机保护**：
  - 保留原生六模式 XML 表单；
  - 冻结写卡状态机、非统一密钥认证时序（写块恒 B→A，写值块失败 B→A，VTR 目的 A→B，写转储按 write-info 编码）；
  - 顶部挂载轻量 M3 `WriteTagShell`，补齐中文化安全警示。

### 5. 测试与质量工程加固
- **JVM 单元测试**：共计 24 项单元测试全部通过（`BaselineTest` 18 项 + `LanguageHelperTest` 6 项）；
- **AndroidTest 编译**：全量 57 个仪器测试方法静态编译通过；
- **真实硬件读卡验收**：继承 P7 与 P15-A 在 OPPO 实体机上完成的 16 扇区/64 块只读闭环测试，转储哈希与官方原版 100% 一致。

### 6. 已知限制与暂未包含功能 (Known Limitations)
- **实体写卡未在此版本进行专项验收**：写卡流程与逻辑完全继承原版代码，但建议用户首次使用时使用专用测试卡并提前备份；
- **R8 资源收缩保持 DEFERRED**：因实测证明 `shrinkResources` 会误剔除动态引用的 `ac_sector_trailer_*` 权限资源，为保证功能稳定性绝对优先，生产 Release 保持 `minifyEnabled = false`；
- **MIUI 仪器测试阻断**：因红米 MIUI `ActivityStarterImpl` 限制，AndroidTest 仪器测试在 MIUI 设备上仍被拦截，不影响普通 APK 正常安装与手动运行；
- **无原生卡模拟功能**：Android 普通应用受底层 HCE 协议（仅限 ISO-DEP/APDU）及 NFC 控制器硬件限制，无法直接模拟 MIFARE Classic 物理卡片。

---

## 历史基线 (Upstream v4.3.1)
- 对应上游官方 Git 提交：`50593d96e9478a4172488b948cd7bc921e31e8ed` (Tag `v4.3.1-kotlin-migration-baseline`)
- 历史变更详见上游原始 [CHANGELOG.txt](CHANGELOG.txt)。
