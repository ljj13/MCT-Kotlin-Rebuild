# MCT 全应用 UI 2.0 现状审计报告 (UI2_AUDIT.md)

> 审计基线：`feature/compose-modernization` (HEAD `74233f8`)
> 范围：全应用 22 个 Activity 界面、UI 渲染层、语言/主题机制及用户交互体验。

---

## 1. 核心重点问题深度诊断

### 1.1 问题一：Preferences（首选项设置）界面
- **语言显示脱节根因诊断（最严重功能体验缺陷）**:
  - `Preferences.kt` 初始化时调用 `pref.getInt("custom_app_language", 0)`。由于首次安装该键为空，默认返回 `0`；
  - `R.array.supported_locales` 的索引 0 被硬编码为 `English (en)`，且数组中完全缺失“跟随系统 (Follow System)”选项；
  - 启动时若用户未曾在首选项中手动保存语言，`AppCompatDelegate.getApplicationLocales()` 为空，应用自动遵从系统语言（如中文系统显示中文）；
  - 但首选项页面打开时却直接根据索引 0 显示当前语言为 `English (en)`，造成界面为中文而设置项赫然显示为英文的严重割裂；
  - 一旦用户在该状态下点击“保存”，系统会提取 `English (en)` 中的 `en` 标签写入 `AppCompatDelegate.setApplicationLocales`，导致应用被意外强制锁定为英文。
- **排版与控件缺陷**:
  - 单一巨大卡片堆砌所有设置，缺乏清晰分类；
  - 采用老式 `Checkbox` 作为行前置勾选框，触控面积小，不符合现代设置页右侧 `Switch` 的交互标准；
  - **UID 格式选项严重挤压**：将 Hex、十进制(大端序)、十进制(小端序) 三个单选框强行排布于同一横行 (`Row`)，在小屏或稍大字体下发生剧烈文字换行、重叠和截断；
  - 包含大量在 Composable 中硬编码的中文字符串（如 `"读写通信与认证设置"`, `"外观主题与语言"`），未提取至 strings 资源；
  - 底部保存/取消区域使用笨重的卡片包裹，按键对比度不足。

### 1.2 问题二：HelpAndInfo（帮助与信息）界面
- **语言与深色割裂诊断**:
  - 外层虽然使用了 Compose Scaffold 与 MctTopAppBar，但核心内容仅通过 `AndroidView` 嵌入 Chromium WebView，固定离线加载 `file:///android_asset/help/help.html`；
  - `help.html` 纯为 2013 年原作者编写的纯英文 HTML，整个工程 assets 中从未存在任何中文帮助文档；
  - HTML 内容写死 `<body bgcolor="#FFFFFF">` 或默认白底，并使用 90 年代老旧排版标签 (`<font face="sans-serif">`, `<div align="center">`)；
  - 在暗色模式下，用户从黑色界面点进帮助页，瞬间被白底网页强光刺眼，严重破坏视觉连贯性；
  - 缺乏移动端自适应视口与响应式表格，高 DPI 屏幕文字细小局促。

### 1.3 问题三：About（关于）界面
- **老旧弹窗与中英文混杂**:
  - 由 `MainMenu.kt` 中弹出的原生平台 `AlertDialog.Builder` 承载，没有任何 Material 3 质感；
  - 版本号错误传递 `Common.getVersionCode()`（如 70），导致显示为 `"版本：70"` 而非版本名称 `"4.3.1"`；
  - `MainMenuScreen.kt` 顶栏中的关于按钮 `contentDescription = "About"` 直接硬编码英文；
  - 弹窗内文本格式松散，链接未作卡片化分类，排版杂乱。

### 1.4 问题四：MainMenu（主页）界面
- **视觉层级与信息结构**:
  - NFC 状态提示卡片（`NfcStatusBanner`）较为单薄，缺乏 KernelSU 风格的状态卡片冲击力与徽章感；
  - 核心读/写操作卡片与离线编辑卡片采用均等的 2x2 网格，读（安全日常）与写（高危操作）没有建立清晰的视觉安全优先级；
  - 工具箱入口需要点击“工具”卡片弹窗二级选择，层级稍深且界面留白分配不均。

---

## 2. 全页面 UI 现状与改造矩阵表

| # | Activity / 页面 | 当前技术架构 | 旧 UI 残留 / 视觉痛点 | 国际化 / 字符问题 | 改造难度与风险 | 推荐 UI 2.0 改造策略 |
|---|---|---|---|---|---|---|
| 1 | **MainMenu** | 全 Compose (Scaffold) | 状态卡片较平淡；2x2 网格空白大；关于弹窗老旧 | 顶栏图标 `contentDescription` 硬编码英文 | 低 (纯展现) | 重构为 Hero 状态卡片 + 优先级读写操作 + 现代关于 Dialog |
| 2 | **Preferences** | 全 Compose (Scaffold) | 单卡片堆叠；Checkbox 老旧；UID 格式水平挤压折行 | 语言选项无跟随系统；中文标题硬编码 | 中 (需保持 Pref 键兼容) | 分组为 3 个 `MctSettingGroup`，改用 Switch 与单选 Dialog，增加跟随系统 |
| 3 | **HelpAndInfo** | Compose + AndroidView | WebView 内部为纯白英文老网页，暗色模式刺眼 | 纯英文，缺失中文版帮助 | 中 (文档工程量) | 编写 `help_zh.html`，优化 HTML/CSS 支持深浅色自适应与现代排版 |
| 4 | **About (弹窗)** | 原生 AlertDialog | 纯系统文本弹窗，排版杂乱 | 混杂；版本号传递 VersionCode 错误 | 低 (无业务风险) | 改造为现代 Material 3 独立 About 对话框/卡片列表 |
| 5 | **WriteTag** | ComposeView + 原生 XML | 原生表单六模式；顶栏外壳样式偏基础 | 文案已规范，无硬编码 | 极高风险 (冻结写卡机) | 仅升级顶部 ComposeView 外壳为 UI 2.0 科技风警示卡片，原生表单完全不动 |
| 6 | **DumpEditor (Prod)**| 原生 XML | 纯原生布局，高密度 Hex 编辑器 | 原生资源 | 极高风险 (P14 冻结) | 生产保持原生不动，轻量适配系统背景与顶栏，逻辑零侵入 |
| 7 | **ReadTag** | 全 Compose | 卡片与按键样式仍为 M3 默认，对比度偏平 | 正常 | 低 (保护扫描生命周期) | 引入 `MctStatusCard` 与 `MctActionButton`，强化读卡等待呼吸感 |
| 8 | **KeyMapCreator** | 全 Compose | 扇区范围与密钥选择卡片稍杂乱 | 正常 | 低 (保护扫描契约) | 统一使用 `MctSettingGroup` 与精致单选项 |
| 9 | **KeyEditor** | Compose + AndroidView | 底部保存/分享条较为单调 | 正常 | 低 (核心为 AppCompatEditText) | 升级顶栏与底部科技风操作条，保留编辑核心 |
| 10 | **DiffTool** | 全 Compose | 顶部两个 Dump 选取卡片不够精致 | 正常 | 低 (保护 MCDiffUtils) | 升级选取器卡片，保持高密度等宽对齐与字节差异着色 |
| 11 | **TagInfoTool** | 全 Compose | 卡片堆叠较长，芯片参数与 MIFARE 参数层次可优化 | 正常 | 低 (保持 NFC 解析) | 模块化为标签基础参数卡与技术架构卡，提升信息密度 |
| 12 | **CloneUidTool** | 全 Compose | 校验与生成选项排版稍挤 | 正常 | 低 (禁止写卡) | 重新排版 UID 输入与 Block 0 预览，强化安全警告 |
| 13 | **FileChooser** | 全 Compose | 列表项边距稍大，新建文件按钮普通 | 正常 | 低 (保持 Intent 契约) | 采用现代化文件列表项与圆角 FAB/底部操作栏 |
| 14 | **ImportExportTool** | 全 Compose | 功能项卡片排列较为平铺 | 正常 | 低 (保持 SAF 流程) | 整合为“转储文件管理”、“密钥文件管理”与“完整备份”分组卡片 |
| 15 | **UidLogTool** | 全 Compose | 列表卡片稍厚重，清空按钮位置普通 | 正常 | 低 (保持文本读取) | 紧凑化单项日志流，统一复制与分享反馈 |
| 16 | **BccTool** | 全 Compose | 界面元素稀疏，输入框占位大 | 正常 | 低 (纯算法工具) | 居中精致技术卡片布局，强化 Hex 等宽与即时校验 |
| 17 | **HexToAscii** | 全 Compose | 表格与输入区域分隔不显眼 | 正常 | 低 (纯算法工具) | 升级为清晰的输入卡片与双列对照卡片 |
| 18 | **ValueBlockTool** | 全 Compose | 增减值与地址输入行排版稍松散 | 正常 | 低 (纯算法工具) | 采用紧凑的数值操作卡片与等宽预览字段 |
| 19 | **ValueBlocksToInt**| 全 Compose | 文件选定后列表可读性待提升 | 正常 | 低 (纯算法工具) | 统一高密度技术卡片与扇区块折叠容器 |
| 20 | **AccessConditionTool**| 全 Compose | 权限选择网格与权限位预览略微拥挤 | 正常 | 低 (纯算法工具) | 模块化扇区尾块与数据块权限矩阵，提升点按触控舒适度 |
| 21 | **AccessConditionDecoder**| 全 Compose | 矩阵解码结果卡片边框不明显 | 正常 | 低 (纯算法工具) | 强化边框与状态配色，提高技术数据辨识度 |
| 22 | **DumpEditorPrototype**| 调试原型 (已隔离) | 实验性对比视图 | 正常 | 零风险 (仅 Debug) | 保持隔离状态，不参与生产界面发布 |

---

## 3. 语言与暗色主题深度审计结论
1. **Locale 机制缺陷**:
   - `Preferences` 缺乏“跟随系统”选项，且初次启动显示错误的 `English (en)`，保存时意外篡改系统语言。必须重构语言选择器并支持 Locale 双向联动；
2. **WebView 暗色反差**:
   - `HelpAndInfo` 的 HTML 资源亟需配套暗色 CSS 自适应（`@media (prefers-color-scheme: dark)`）与地道简体中文版本 `help_zh.html`；
3. **视觉层次与边界**:
   - 当前 M3 方案在暗色模式下各组件主要依赖轻微的灰阶背景差别，缺乏描边轮廓。全面引入 KernelSU 风格的 `BorderStroke(1.dp, Outline)` 能立竿见影地提升科技质感。
