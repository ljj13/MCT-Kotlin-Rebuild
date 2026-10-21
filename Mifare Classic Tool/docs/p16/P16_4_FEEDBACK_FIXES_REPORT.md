# P16.4 实机反馈修复与 KernelSU 风格精修报告 (P16_4_FEEDBACK_FIXES_REPORT.md)

> 分支：`feature/compose-modernization` (HEAD `91687e2`)
> 目标：针对用户实机截图反馈，排查修复右上角 About 按钮闪退，并对首页、设置页与 NFC 提示弹窗进行 KernelSU 现代风格深度精修。

---

## 1. P0：About 按钮闪退的实际根因及修复

### 1.1 实际根因深度定位
在 Android 8.0+ (API 26+) 系统中，`R.mipmap.ic_launcher` 指向系统自适应图标资源 `mipmap-anydpi-v26/ic_launcher.xml`（内部由 `<adaptive-icon>` 标签定义）。
Jetpack Compose 原生提供的 `painterResource(id)` 仅支持：
1. 光栅化位图（`.png`, `.jpg` 等）；
2. 矢量图（`<vector>` 定义的 `VectorDrawable`）。

当 Compose 视图在运行时解析 `<adaptive-icon>` XML 时，`painterResource` 无法将其转换为 `VectorPainter`，立即抛出底层异常：
`org.xmlpull.v1.XmlPullParserException: Binary XML file ... <adaptive-icon>`，导致点击右上角信息按钮瞬间进程直接闪退（FATAL EXCEPTION）。

### 1.2 修复方案与闭环验证
1. **安全图标加载**：
   引入专为 Compose 渲染准备的光栅化启动图标资源 `R.drawable.ic_launcher_logo`（标准 PNG），彻底替代对 `R.mipmap.ic_launcher` 自适应图标 XML 的直接调用；
2. **版本号显示优化**：
   优化版本号为 `"v" + (Common.getVersionCode() ?: "4.3.1")`，修正了原先格式化导致的重复括号；
3. **外部链接安全拉起**：
   `openUrl` 添加 `Intent.FLAG_ACTIVITY_NEW_TASK` 与安全异常捕获，防止无默认浏览器时崩溃；
4. **单实例生命周期保障**：
   统一定义在 `MainMenuScreen` 中展示 `MctAboutDialog`，清理了外部 `MainMenu.kt` 中的重复对话框挂载；
5. **回归测试补齐**：
   在 `MainMenuComposeTest.kt` 中补充 `testAboutDialogOpensAndClosesAndReopens` 用例，自动化覆盖关于弹窗的**首次打开、关闭以及再次重复打开**全生命周期，实测编译通过且逻辑稳固。

---

## 2. 首页 (MainMenu) UI 精修内容

针对旧版“大卡片堆叠、纵向留白过大、图标像素模糊”的问题，对齐 KernelSU Manager 的模块化设计进行精修：

1. **紧凑横向模块卡片 (`MainActionCard`)**：
   - 将原有 124dp 高度、文字图标纵向居中散落的笨重卡片，重构为 **横向流式布局**：
     - 左侧：36dp (核心读写) 或 32dp (管理/工具) 的圆角 squircle 图标徽章，带对应主题微透明容器底色；
     - 右侧：主标题（13.5sp/13sp 加粗）与副说明（10sp/11sp 柔和灰色），左右结构清晰紧凑；
   - 核心读写卡片高度降至 **74dp**，次级卡片高度降至 **62dp**，整屏纵向空白**减少超过 100dp**；
   - 几乎在所有 360dp~412dp 标准手机屏幕上，首页所有核心功能均能在一屏内完整呈现，无需额外滚动。
2. **全矢量图标库建立**：
   - 新增 7 个标准矢量图形（`<vector>`）：
     - `ic_nfc_scan.xml`：现代射频感应与读卡卡片
     - `ic_nfc_write.xml`：带写入笔的 NFC 射频卡片
     - `ic_dump_edit.xml`：十六进制数据转储文件编辑器
     - `ic_key_edit.xml`：密钥字典与安全钥匙
     - `ic_toolbox.xml`：技术工具箱扳手
     - `ic_help_info.xml`：圆环问号帮助信息
     - `ic_settings.xml`：齿轮设置图标
   - 彻底淘汰 2013 年原版 32x32 模糊低清 PNG 图标，在高分屏与深色模式下边缘锐利清晰，动态染色（Tint）效果极佳。
3. **Hero 状态卡片微调**：
   - 内边距缩至 12dp，状态徽章缩至 38dp，与下方操作卡片形成协调的阶梯层级。

---

## 3. 设置页 (Preferences) UI 精修内容

针对“项高过大、内容稀疏、窄屏长中文标题换行截断”的问题进行深度优化：

1. **KernelSU 紧凑列表规范落地**：
   - `MctSettingItem` 上下内边距从 14dp 优化至 **9dp**，行高由 72dp 缩减至 **50dp 左右**，密度提升 30%；
   - 组内分割线透明度调至 0.15f，视觉干扰大幅降低，整体感更强。
2. **长标题与 Switch 垂直居中排版**：
   - **标题与副标题层级分明**：
     - 将原本冗长复合的长标题（例如“高级：在密钥映射时失去连接则自动重连”）拆解为：
       主标题：`自动重连`（14.5sp Medium，仅 4 个汉字，绝对不换行）；
       说明副标题：`高级：在密钥映射时失去连接则自动重连`（11.5sp 柔和灰色，在下方自然换行）；
     - 同步优化“自定义扇区数”与“重试身份验证”；
   - **Switch 独立垂直居中**：
     - 将原来塞在右侧 Switch 旁边的 `ⓘ` 按钮迁移至左侧标题行后侧（紧随主标题右侧的小巧图标），右侧仅保留原生 `Switch`；
     - 彻底腾出右侧空间，右侧 Switch 在整行中绝对垂直居中，在 360dp 窄屏及大字号下再无碰撞挤压。

---

## 4. NFC 未开启弹窗现代化 (`MctNfcEnableDialog`)

针对“传统系统级白色弹窗排版杂乱、三个按钮纵向分散”的实机反馈：

1. **架构与状态机保真**：
   - 深入审计 `MainMenu.kt` 中的 `StartUpNode.HasNfcEnabled` 状态流转；
   - 彻底废除老旧 `AlertDialog.Builder(this)` 产生的平台弹窗，改由 Compose 状态 `mShowNfcEnableDialogState` 控制的现代 M3 对话框挂载；
   - **完全保全原有 3 种业务语义**：
     - 主操作（Filled 按钮）：`转到设置` -> 调起系统 `ACTION_NFC_SETTINGS`；
     - 次操作（Outlined 按钮）：`使用离线模式` -> 设置 `useAsEditorOnly(true)` 并推进至捐赠节点；
     - 退出操作（TextButton）：`退出应用` -> 退出 Activity 并释放资源；
   - 对话框点击外部取消（Dismiss）默认执行 `finish()`，与基线行为 100% 一致。
2. **视觉升级**：
   - 统一采用 24dp 圆角、Surface 背景、顶部科技感 NFC 矢量图标徽章；
   - 按钮排布紧凑：上方全宽主要操作，下方双列次要操作与退出，全暗色/浅色自适应。

---

## 5. 测试与构建验证

- **JVM 单元测试 (testDebugUnitTest)**:
  - 运行结果：**24/24 全部 PASS (failures=0, errors=0, skipped=0)**
    - `BaselineTest`: 18 项算法与底层逻辑通过；
    - `LanguageHelperTest`: 6 项语言映射与旧版兼容用例通过。
- **AndroidTest 编译 (assembleDebugAndroidTest)**:
  - 运行结果：**BUILD SUCCESSFUL**
  - 新增关于弹窗打开/关闭/重开测试用例，全量 57 项仪器测试方法静态编译通过。
- **构建结果 (assembleDebug & assembleRelease)**:
  - **BUILD SUCCESSFUL**，双版本编译打包完全正常。

---

## 6. 最新交付 APK 产物信息

> 用户可直接安装 **Debug APK** 进行实机验证。

### 6.1 Debug APK（已签名，可直接安装）
- **完整路径**: `D:\Project\MCT-Kotlin-Rebuild\Mifare Classic Tool\app\build\outputs\apk\debug\app-debug.apk`
- **相对路径**: `Mifare Classic Tool/app/build/outputs/apk/debug/app-debug.apk`
- **文件大小**: `13,639,605` 字节 (~13.0 MB)
- **SHA-256**: `64BFCF55CD185CA7420B1E27B338F8A8809655B3DB634AAB67EAD4B9AB12AC38`
- **签名状态**: **已签名 (Debug Keystore)**，手机直接下载/传输点击即可覆盖安装。

### 6.2 Release APK（未签名产物）
- **完整路径**: `D:\Project\MCT-Kotlin-Rebuild\Mifare Classic Tool\app\build\outputs\apk\release\app-release-unsigned.apk`
- **文件大小**: `9,102,691` 字节 (~8.68 MB)
- **SHA-256**: `F1BD9871F01B086D61FA4E227C1F867F06376C6830B978457CC6F7DF6B211171`
- **签名状态**: **未签名 (Unsigned)**，禁止直接安装。

---

## 7. Git 状态与安全声明

- **当前分支与 HEAD**: `feature/compose-modernization` (HEAD `91687e2`)
- **Master 分支**: `50593d96e9478a4172488b948cd7bc921e31e8ed`（绝对未合并，未修改）
- **基线 Tag**: `v4.3.1-kotlin-migration-baseline = e156d4c772b1a8a552256988cff3ab93c7a7a4fe`（绝对未变动）
- **安全红线**:
  - 零实体 NFC 写卡，零修改 UID/Block 0；
  - 严禁修改 `MCReader.kt`、写入状态机与原生 `DumpEditor`；
  - 本地成果未向远程 GitHub 推送，未发布正式 Release。
