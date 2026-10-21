# MCT UI 2.0 实施与重构报告 (UI2_IMPLEMENTATION_REPORT.md)

> 工程：MifareClassicTool (MCT) Kotlin + Compose 现代化分支 `feature/compose-modernization`
> 阶段目标：基于 KernelSU Manager 现代 Android 设计语言，完成全应用 UI 2.0 视觉与交互重构，彻底解决语言脱节与旧 UI 割裂。

---

## 1. 架构选型与实施策略

### 1.1 方案 A（基于 Compose Material 3 抽象 KernelSU 设计）落地
- 经 Phase A 深度审计与评估，正式采纳 **方案 A**：
  - 坚持基于官方 Jetpack Compose Material 3、Compose BOM 2024.10.00 及现有项目架构；
  - 避免引入 Miuix 或第三方完整 App 框架，成功保全 `minSdk 21` 兼容性、极速离线编译能力与零依赖膨胀；
  - 在 `MctTheme` 与 `MctComponents` 中抽象完整的 KernelSU 设计组件，实现现代技术工具质感。

---

## 2. 设计系统实现 (MctTheme & MctComponents)

### 2.1 主题与色彩升级 (`Color.kt`, `Theme.kt`)
- **暗色模式彻底告别平铺灰色**：
  - 背景底色：`#0D1117` (Deep Charcoal / Slate)；
  - 卡片容器：`#161B22` (Elevated Surface)；
  - 浮层/弹窗：`#21262D` (Surface Container High)；
  - 边框描边：`#30363D` (1dp 细致轮廓线，在暗色下勾勒出锋利的层次)；
  - 主强调色：`#38BDF8` (科技青 / 天蓝色)；
  - 辅助强调色：`#34D399` (薄荷绿)。
- **浅色模式清爽通透**：
  - 底色采用 `#F6F8FA` 冷白微灰；卡片容器纯白 `#FFFFFF` 并附带 `#D0D7DE` 柔和边框。
- **动态取色 (Dynamic Color) 保全**：
  - 在 Android 12+ (API 31+) 设备上，若启用动态取色，自动保留深邃深灰底色与卡片轮廓，同时无缝融合系统壁纸主强调色。
- **高亮语义保留**：
  - 严格保持 UID (`#38BDF8`)、Key A (`#10B981`)、Access Bits (`#F59E0B`)、Key B (`#818CF8`) 及 Value (`#34D399`) 的业务颜色映射。

### 2.2 KernelSU 风格组件库 (`MctComponents.kt`)
封装了 15 个即插即用的现代化组件：
1. `MctTopAppBar`: 现代科技风顶栏，支持无缝大标题、返回导航与动作图标；
2. `MctSectionHeader`: 小字号加粗主色分类标签，提供清晰的分组导引；
3. `MctSettingGroup`: 16dp 圆角表面卡片，作为设置项或信息的分组容器；
4. `MctSettingItem`: 设置行，带圆角 squircle 图标、标题、描述、当前值与 Chevron 箭头；
5. `MctSwitchItem`: 现代右对齐 Material 3 `Switch` 设置项，全行点击触发；
6. `MctSelectionItem`: 适用于弹窗的垂直单选项，带 RadioButton 与选中高亮；
7. `MctStatusCard`: 仿照 KernelSU 首页的 Hero 状态卡片，带语义图标徽章、标题、副标题与操作 Chip；
8. `MctSectionCard`: 通用卡片容器，升级为 16dp 圆角 + 1dp 轮廓描边，完全向下兼容全工程；
9. `MctActionButton`: 12dp 圆角 50dp 高度主操作按钮；
10. `MctDangerButton`: 红色半透明背景高危操作按钮；
11. `MctSelectionDialog`: 24dp 圆角弹窗，垂直滚动单选列表，彻底替代横向排布；
12. `MctConfirmDialog`: 24dp 圆角现代二次确认弹窗；
13. `MctMonospaceField`: 12dp 圆角等宽十六进制数据输入/展示框；
14. `MctInfoCard`: 12dp 圆角警示/提示信息卡片；
15. `MctEmptyState`: 空数据占位提示组件。

---

## 3. 语言与 Locale 缺陷彻底治理 (最高优先级)

### 3.1 根因分析与问题闭环
- **历史缺陷**：
  - 首选项默认调用 `pref.getInt("custom_app_language", 0)`；首次安装由于键值不存在返回 `0`；
  - `supported_locales` 数组的索引 0 恰好是 `English (en)`，且缺失“跟随系统”；
  - 即使中文系统界面由于 `AppCompatDelegate.getApplicationLocales()` 为空而呈现中文，首选项下拉框依然显示 `English (en)`；
  - 用户一旦保存，便意外将应用锁死在英文环境。
- **治理方案**：
  1. 在 `Preferences.kt` 中引入双向映射：索引 0 统一作为 **跟随系统 (Follow System / 跟随系统)**，后续索引映射具体语言代码；
  2. 初始化时检查 `AppCompatDelegate.getApplicationLocales()`：若为空，则表示未设单应用语言，精准高亮“跟随系统”；若非空，则根据语言 Tag 匹配具体语言；
  3. 保存时若选跟随系统，向 `custom_app_language` 写入 `-1` 并向 `AppCompatDelegate` 下发空 Locale 列表，完全遵从系统调度；
  4. 若选具体语言，写入兼容历史索引 `0..6`（如中文写入 5，英文写入 0），并下发对应语言 Tag；
  5. 兼容历史数据：已存在历史配置的旧用户升级后无缝识别，绝不重置。

---

## 4. 离线帮助文档中文化与暗色适配 (`HelpAndInfo`)

### 4.1 全量简体中文离线文档 (`help_zh.html`)
- 新增 `Mifare Classic Tool/app/src/main/assets/help/help_zh.html`，完成全文 8 个大章节的地道中文重构翻译；
- 术语准确：严格遵循射频行业规范，准确保留 MIFARE Classic、扇区 (Sector)、块 (Block)、密钥 A/B (Key A/B)、访问控制条件 (Access Conditions)、厂商块 (Block 0)、BCC 等概念；
- 包含完整安全警告与魔术卡（Gen2/CUID）说明。

### 4.2 网页暗色模式与现代排版
- 彻底移除老旧 `<font face="sans-serif">` 与 `<body bgcolor="#FFFFFF">`；
- 为 `help_zh.html` 与 `help.html` 引入 CSS 现代媒体查询 `@media (prefers-color-scheme: dark)`；
- 在深色模式下，WebView 背景自动转为深灰 `#0D1117`，卡片容器为 `#161B22`，文字为高对比冷白 `#F0F6FC`；
- `HelpAndInfoScreen.kt` 初始化 WebView 时显式设置 `setBackgroundColor(0)` 透明背景，彻底消除白屏闪烁；
- 视 Locale 自动分流：中文系统加载 `help_zh.html`，其他环境加载 `help.html`。

---

## 5. 核心页面重设计落地

### 5.1 Preferences（首选项设置）
- 分组为 3 个 `MctSettingGroup`：
  - **日常使用与字典便利**：自启开关、保存上次使用密钥、自动复制 UID（全改用 Switch）；
  - **读写通信与认证设置**：自动重连、自定义扇区数、重试认证（带信息对话框按钮）；
  - **外观主题与语言**：外观主题、应用语言、UID 显示格式；
- **UID 显示格式彻底解决挤压换行**：
  - 弃用横向单行 RadioButton 堆叠；
  - 改为点击弹出 `MctSelectionDialog` 垂直单选列表（Hex / Dec BE / Dec LE），屏幕适配率 100%；
- 底部操作栏由笨重卡片简化为扁平取消/保存操作行。

### 5.2 About（关于弹窗）
- 全新编写 `MctAboutDialog.kt`（Compose Material 3 对话框）；
- 包含应用大图标、正确版本号展示 `v4.3.1 (70)`、Kotlin 现代化分支说明；
- 卡片化呈现许可证 (GPLv3)、原始项目上游致谢 (Gerhard Klostermeier)、源码仓库、反馈入口与更新日志；
- 修复顶栏关于按钮硬编码英文 `contentDescription = "About"` 的问题。

### 5.3 MainMenu（主页）
- 引入 Hero `MctStatusCard` 动态呈现 NFC 状态（就绪/未启用/仅编辑器/不支持）；
- 核心读写卡片组建立视觉优先级（读卡为科技蓝主强调色，写卡带安全防误触提示色）；
- 2x2 操作网格优化为紧凑科技感卡片，消除留白失衡；
- 全部标题、副标题与状态文案统一迁移至字符串资源，杜绝硬编码。

### 5.4 独立工具页面推广 (Phase D & E)
- `WriteTagShell`: 顶部警示条改造为 KernelSU 风格 `Surface` 描边卡片，原生六模式表单与写卡状态机零改动；
- `KeyEditor`: 编辑核心容器增加 `BorderStroke` 描边，保存与退出弹窗升级为 24dp 圆角；
- `DiffTool`: 消除“转储文件选择与筛选”硬编码，统一组件容器；
- `CloneUid`: 引入 `MctInfoCard` 安全告示，输入框圆角规范化；
- `TagInfo`: 消除空状态下硬编码中文字符，统一使用字符串资源；
- `UidLog`: 清空确认弹窗升级为危险操作 Red 标识，复制按钮统一；
- `FileChooser` & `ImportExport`: 弹窗圆角统一为 24dp，格式选择器列表卡片化。

---

## 6. 安全边界与不可变项坚守

- 零物理实体写卡操作，零 UID/Block 0/Gen2 修改；
- 严格禁止修改 `MCReader.kt`、写入状态机与认证算法；
- 生产 `DumpEditor` 与 `WriteTag` 原生表单保持不变；
- `master` 分支 (`50593d9`) 与基线 Tag (`v4.3.1-kotlin-migration-baseline = e156d4c`) 保持绝对一致且未做任何修改；
- 本地成果未向远程推送，未发布 Release。
