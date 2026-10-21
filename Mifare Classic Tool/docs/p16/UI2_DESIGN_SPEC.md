# MCT UI 2.0 设计规范 (UI2_DESIGN_SPEC.md)

> 基于 KernelSU Manager 现代 Android 设计语言与 Jetpack Compose Material 3 体系。
> 适用于 MifareClassicTool (MCT) Kotlin + Compose 现代化工程 (`feature/compose-modernization`)。

---

## 1. 核心设计哲学与参考依据

### 1.1 参考项目
1. **KernelSU Manager** (`tiann/KernelSU`):
   - 现代高质感 Android 技术工具标杆，具备清晰的系统状态英雄卡片（Status Hero Card）、分组圆角卡片（Grouped Squircles）、紧凑而精致的设置行（Setting Items with Switch / Chevron）、深邃且边界分明的暗色主题。
2. **KernelSU Style UI Kit** (`chenaizhang/KernelSU-Style-UI-Kit`):
   - 提取自 KernelSU 的设计组件规范，定义了卡片容器、设置项、开关、单选项及对话框交互模式。
3. **Jetpack Compose Material 3**:
   - 现代 Android 官方规范，提供 Dynamic Color、M3 Typography Scale 与无障碍无缝支持。

### 1.2 架构方案选型与决策
- **评估方案 A（基于 Compose Material 3 抽象 KernelSU 风格）**:
  - 优点：完全兼容现有 Compose BOM (2024.10.00)、Kotlin 2.2.10 与 AGP 9.2；完美支持 `minSdk 21`；零引入外部大型 UI 库，无运行时冲突，APK 零膨胀；高度自由定制配色与层级。
- **评估方案 B（引入 Miuix）**:
  - 风险：minSdk 限制、库体积增加、与现有 Material 3 基础组件共存成本高、更新维护依赖第三方。
- **最终决策：采纳方案 A**。通过封装与扩展 `MctTheme` 与 `MctComponents`，在纯正 Material 3 基础之上完整实现 KernelSU 视觉语言。

---

## 2. 视觉层级与色彩系统 (Color & Surface System)

### 2.1 暗色主题层次消除“大面积平铺灰色”
KernelSU 最突出的设计特征是**深邃的底色 + 清晰微凸的卡片容器 + 精致的边框描边 (Subtle Stroke)**，彻底解决传统 Material 3 在暗色下各级 Surface 灰阶区分度弱、容易糊成一片的问题：

| 元素 | 颜色代码 (Dark Mode) | 视觉语义 |
| :--- | :--- | :--- |
| **Scaffold Background** | `#0D1117` (Deep Slate / Charcoal) | 页面最底层背景，深沉纯粹 |
| **Card / Group Container** | `#161B22` (Elevated Surface) | 分组卡片与功能容器，带来清晰浮层感 |
| **Card Container High / Dialog** | `#21262D` (Surface High) | 弹窗、下拉菜单与浮动操作层 |
| **Subtle Card Border** | `#30363D` (1dp Outline) | 精致细微的描边，在暗色下勾勒出极其分明的轮廓 |
| **Primary Accent** | `#38BDF8` (Tech Cyan / Electric Blue) | 现代科技感主强调色，高辨识度 |
| **Primary Container** | `#0C4A6E` | 主强调色浅容器背景 |
| **Success / NFC Ready** | `#10B981` / Container `#064E3B` | 状态绿色：NFC 已就绪、数据校验通过 |
| **Warning / Notice** | `#F59E0B` / Container `#78350F` | 状态黄色：NFC 未开启、提示信息 |
| **Danger / Destructive** | `#EF4444` / Container `#7F1D1D` | 状态红色：写入操作、清空日志、错误 |
| **Text Primary** | `#F0F6FC` | 高对比度主标题与正文 |
| **Text Secondary / Muted** | `#8B949E` | 辅助文字、说明文案与参数标签 |

### 2.2 浅色主题设计 (Light Mode)
| 元素 | 颜色代码 (Light Mode) | 视觉语义 |
| :--- | :--- | :--- |
| **Scaffold Background** | `#F6F8FA` (Cool Off-White) | 干净、护眼的浅冷灰底色 |
| **Card / Group Container** | `#FFFFFF` (Pure White) | 纯白容器卡片，与底色形成微对比 |
| **Card Container High / Dialog** | `#FFFFFF` | 弹窗容器 |
| **Subtle Card Border** | `#D0D7DE` (1dp Soft Outline) | 柔和的浅色描边，清晰划分边界 |
| **Primary Accent** | `#0284C7` (Tech Blue) | 现代技术主强调色 |
| **Primary Container** | `#E0F2FE` | 浅蓝背景容器 |
| **Success / NFC Ready** | `#059669` / Container `#D1FAE5` | 状态绿色 |
| **Warning / Notice** | `#D97706` / Container `#FEF3C7` | 状态黄色 |
| **Danger / Destructive** | `#DC2626` / Container `#FEE2E2` | 状态红色 |
| **Text Primary** | `#1F2328` | 深黑高对比度文字 |
| **Text Secondary / Muted** | `#656D76` | 柔和灰色辅助文字 |

### 2.3 专业 NFC 语法高亮保留
严格保留 MCT 固有的十六进制数据块语义色彩（不可改动其业务含义）：
- **UID / 厂商块**: 强调蓝 `#38BDF8` (Dark) / `#0284C7` (Light)
- **Key A**: 绿色 `#10B981` (Dark) / `#059669` (Light)
- **Access Bits (AC)**: 橙黄 `#F59E0B` (Dark) / `#D97706` (Light)
- **Key B**: 紫色 / 青蓝 `#818CF8` (Dark) / `#4F46E5` (Light)
- **Value Block 整数**: 翠绿 `#34D399`

---

## 3. 排版体系 (Typography Scale)

| 角色 | 字号 / 行高 | 字重 | 典型应用场景 |
| :--- | :--- | :--- | :--- |
| **Page Title** | 22sp / 28sp | Bold / SemiBold | 页面顶栏主标题、Hero 卡片主状态 |
| **Group Header** | 13sp / 18sp | Bold (AllCaps / Subtle) | 设置分组标题、卡片分类标签 (例如: 日常使用) |
| **Setting Title** | 16sp / 22sp | Medium / SemiBold | 设置项名称、操作卡片主标题 |
| **Setting Description**| 13sp / 18sp | Regular | 设置项辅助说明、次级信息 |
| **Body / Content** | 14sp / 20sp | Regular | 弹窗正文、帮助文档正文、列表内容 |
| **Label / Caption** | 12sp / 16sp | Medium | 芯片参数标签、小角标、状态 Pill |
| **Monospace Technical**| 13sp / 18sp | Monospace (SemiBold) | UID、Hex 字节流、扇区/块地址、ASCII 字符 |

---

## 4. 核心组件规范 (UI 2.0 Component Library)

### 4.1 MctTopAppBar
- 居中或居左大标题，高度 56dp–64dp。
- 平滑透明或 Surface 纯净背景，滚动时不产生刺眼分割线。
- 导航图标（返回箭头）、动作按钮（设置、关于、分享等）具备 48dp 无障碍点击区域与柔和圆形水波纹反馈。

### 4.2 MctStatusCard (KernelSU 风格状态卡片)
- 用于首页 NFC 状态与读卡等待状态。
- 左侧为圆形或圆角矩形状态图标徽章（带对应状态的主题容器色）。
- 中间展示大状态标题（如 `NFC 已就绪` / `NFC 未启用`）与详细说明（如 `贴附 MIFARE Classic 卡片即可读取转储`）。
- 右侧或底部包含状态操作 Chip（如 `开启 NFC` 跳转设置，或芯片支持状态 Pill）。
- 外框具有圆角 16dp 与 1dp 精致描边。

### 4.3 MctSettingGroup (设置分组容器)
- 仿照 KernelSU Manager 设置页面的核心容器。
- 外部带有微小的大写/彩色标题 (`MctSectionHeader`)，例如 `日常使用与快捷操作`。
- 分组容器为一个 `Surface`（圆角 16dp，暗色 `#161B22` + 描边 `#30363D`）。
- 内部子项垂直排列，每一项之间添加带内边距的极浅分割线 (`HorizontalDivider`)。

### 4.4 MctSettingItem & MctSwitchItem (设置单元行)
- **MctSettingItem**: 点击行，左侧可选图标（位于小 squircle 内），中间为标题与次级描述，右侧为当前值文字 + Chevron 箭头 (`>`)。
- **MctSwitchItem**: 开关行，右侧为现代 Material 3 `Switch`，整行具有无障碍点击映射，杜绝仅有小方块 Checkbox 的旧式交互。
- **MctSelectionItem**: 单选行，用于弹窗或多选列表，带有 RadioButton 或 Checkmark 图标。

### 4.5 MctSelectionDialog & MctConfirmDialog
- 弹窗圆角统一为 **24dp**。
- 单选弹窗（如 UID 格式、语言、主题选择）：提供垂直滚动单选列表，项高 48dp+，带清晰的勾选态与波纹反馈，彻底淘汰水平挤压的 RadioButton。
- 二次确认弹窗：包含明确的标题、说明、取消按钮与确定/危险操作按钮。

### 4.6 MctActionButton & MctDangerButton
- 按钮圆角统一为 **12dp**，高度 44dp–48dp。
- Primary: 科技蓝高亮填充。
- Tonal: 柔和容器背景，用于次要操作。
- Danger: 红色半透明背景与红字，用于写卡确认、清空、重置等危险操作。

---

## 5. 语言与国际化架构规范 (i18n & Locale System)

1. **彻底解决设置显示与实际生效语言脱节**:
   - 增加 **跟随系统 (Follow System)** 选项作为首选索引。
   - 双向同步：读取时优先以 `AppCompatDelegate.getApplicationLocales()` 与系统语言判断实际激活语言；写入时使用 `LocaleListCompat` 正确下发。
2. **零硬编码中文**:
   - 所有页面标题、分组名称、按钮、弹窗、辅助文案统一迁移至 `strings.xml` 与 `values-zh/strings.xml`。
3. **帮助文档双语化 (Help & Info)**:
   - 编写地道、专业的中文帮助文档 `help_zh.html`，保持 NFC 技术术语严谨一致；
   - 改造现有 `help.html` 与 `help_zh.html` 采用现代响应式排版与深色自适应 CSS；
   - 页面自动根据当前系统/应用 Locale 加载对应的 HTML。
4. **关于页面 (About)**:
   - 完整中英双语支持，规范显示应用版本、项目开源主页、文档与上游致谢，淘汰中英混杂的旧 Platform AlertDialog。

---

## 6. 安全与高密度页面原则
- **高密度页面** (`DiffTool`, `KeyEditor`, `DumpEditor`):
  - 允许使用更紧凑的 8dp 内边距，不强制大圆角和大间距牺牲十六进制显示效率。
- **生命周期与状态机隔离**:
  - UI 2.0 仅重构 Compose 视图层与 XML 样式外壳，绝不触发、也不通过任何动画或重组自动执行任何实体 NFC 写入操作。
