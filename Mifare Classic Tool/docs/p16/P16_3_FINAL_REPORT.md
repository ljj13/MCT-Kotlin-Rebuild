# P16.3 离线质量收口与人工验收交付报告 (P16_3_FINAL_REPORT.md)

> 分支：`feature/compose-modernization` (HEAD `d2e1aee`)
> 阶段目标：完成 KernelSU 风格 UI 2.0 离线质量收口、语言系统映射核查、构建与产物度量，交付可供用户直接安装的人工验收 Debug APK。

---

## 1. 语言兼容性核查结果

| 核查维度 | 机制说明 | 验证结果 |
| :--- | :--- | :---: |
| **跟随系统 (Follow System)** | 当 `AppCompatDelegate.getApplicationLocales()` 为空时，UI 精准选中索引 0（“跟随系统”）。中文系统加载中文，英文系统加载英文，不再错误显示 `English (en)`。 | **PASS** |
| **自定义语言下发** | 用户选择具体语言并保存后，正确写入 `LocaleListCompat` 并即时生效；重启进程由 AppCompat 自动恢复。 | **PASS** |
| **旧版用户偏好兼容** | 抽取独立工具类 `LanguageHelper`：若旧版保存了 `1..6`（非默认），自动映射为 `legacyIndex + 1`，保证旧用户升级不丢失设置；若历史键不存在，安全默认为“跟随系统”，杜绝强制英文锁定。 | **PASS** |
| **语言一致性跨组件对齐** | - Compose 页面：全量使用 `stringResource()`，零硬编码中文；<br>- 原生/离线文档：WebView 根据当前 Locale 自动分流加载 `help_zh.html` 或 `help.html`；<br>- About 对话框：全面采用 Compose `MctAboutDialog`，消除旧平台弹窗中英文混杂问题。 | **PASS** |
| **离线映射单元测试** | 新增 `LanguageHelperTest` 覆盖标签提取、双向索引映射、AppCompat 状态推断、保存状态计算等 6 个核心用例。 | **6/6 PASS** |

---

## 2. UI 静态审计与细节排查

本轮针对核心页面（`MainMenu`, `Preferences`, `HelpAndInfo`, `About`）及各工具页面开展离线静态审查：

1. **MainMenu（主页）**:
   - 状态栏与导航栏 Insets：`Scaffold(innerPadding)` 贯穿全屏，规避内容被系统状态栏遮挡；
   - 文本溢出防护：操作卡片主标题限制 `maxLines = 1`，副标题 `maxLines = 1` 并设置清晰的字体比例（14sp/11sp）；
   - 无障碍标签：设置与关于图标均设置规范的 `contentDescription`（如 `stringResource(R.string.dialog_about_mct_title)`），杜绝硬编码英文。
2. **Preferences（首选项）**:
   - 滚动容器约束：主容器设置 `verticalScroll(rememberScrollState())`；
   - UID 格式排版：改用 `MctSelectionDialog` 垂直单选弹窗，彻底根除旧版横向排布在大字号下的换行碰撞与截断；
   - 纯数字键盘适配：自定义扇区数与重试次数限制 `singleLine = true` 并指定 `KeyboardType.Number`。
3. **HelpAndInfo（帮助与信息）**:
   - 暗色背景融合：WebView 显式注入 `setBackgroundColor(0)` 透明背景，配合 HTML 内部 `@media (prefers-color-scheme: dark)`，消除进入页面瞬间的白屏刺眼；
   - 导航历史回退：`BackHandler` 拦截物理返回键，优先响应 WebView 内部历史后退，末页后退退出 Activity。
4. **About（关于对话框）**:
   - 小屏与横屏保护：对话框内容区包裹 `verticalScroll`，在窄屏或横屏旋转下不会出现按钮被挤出屏幕外的问题；
   - 链接跳转异常保护：`openUrl` 增加安全 try/catch，防止设备无默认浏览器时发生崩溃。

---

## 3. 测试与构建结果准确统计

- **JVM 单元测试 (Unit Tests)**:
  - 运行命令：`.\gradlew.bat cleanTestDebugUnitTest testDebugUnitTest`
  - 结果：**24 项全部通过 (failures=0, errors=0, skipped=0)**
    - `BaselineTest`: 18 项 PASS（Common, MCDiffUtils 核心算法与数据结构保真）
    - `LanguageHelperTest`: 6 项 PASS（多语言双向映射与兼容逻辑）
- **AndroidTest 编译 (Instrumented Tests)**:
  - 运行命令：`.\gradlew.bat assembleDebugAndroidTest`
  - 结果：**BUILD SUCCESSFUL**
  - 说明：包含全量 56 项仪器测试方法，**仅完成静态编译通过，未在真实设备上执行**（因红米离线且保留 MIUI instrumentation 限制）。
- **Release 构建 (assembleRelease)**: **BUILD SUCCESSFUL**
- **Debug 构建 (assembleDebug)**: **BUILD SUCCESSFUL**

---

## 4. 交付 APK 产物信息

> 用户可直接将 **Debug APK** 安装至手机进行人工视觉与交互验收。

### 4.1 Debug APK（推荐人工验收安装）
- **相对路径**: `Mifare Classic Tool/app/build/outputs/apk/debug/app-debug.apk`
- **绝对路径**: `D:\Project\MCT-Kotlin-Rebuild\Mifare Classic Tool\app\build\outputs\apk\debug\app-debug.apk`
- **文件大小**: `13,637,930` 字节 (~13.0 MB)
- **SHA-256**: `B4784ED6BB00C311ECD9FDE4E3A2C69C3C65401F3AD0E97052B9DE045DA8D99A`
- **签名状态**: **已签名**（由 Android SDK 内置 Debug Keystore 自动完成 V1/V2 签名，普通手机可直接安装运行）。

### 4.2 Release APK（构建产物）
- **相对路径**: `Mifare Classic Tool/app/build/outputs/apk/release/app-release-unsigned.apk`
- **绝对路径**: `D:\Project\MCT-Kotlin-Rebuild\Mifare Classic Tool\app\build\outputs\apk\release\app-release-unsigned.apk`
- **文件大小**: `9,090,507` 字节 (~8.67 MB)
- **SHA-256**: `D449F4E79CDAFAC80C9E0530FC4D35150622B5D5C972C17546AB4B31A7726696`
- **签名状态**: **未签名 (Unsigned)**。在配置正式发布密钥前，Android 系统安全机制**禁止直接安装**此未签名包。

---

## 5. Release Manifest 隔离与 R8 状态核查

1. **实验性 DumpEditorPrototype 隔离确认**:
   - `src/main/AndroidManifest.xml`：无 DumpEditorPrototype 声明；
   - `src/debug/AndroidManifest.xml`：包含 DumpEditorPrototype 声明；
   - **最终合并 Release Manifest 实测**：`DumpEditorPrototype` = **False**（绝对未暴露在生产 Manifest 中，生产 DumpEditor 正常保留）。
2. **R8 配置现状核实**:
   - 当前 `app/build.gradle` 真实配置为：`minifyEnabled false`、`shrinkResources false`（**保持 DEFERRED 状态**）；
   - 原因：P16.1 实测证明 `shrinkResources` 会误剔除通过 `Resources.getIdentifier()` 动态引用的 `ac_sector_trailer_*` 资源。本阶段未在设备上验证 `tools:keep` 前，不盲目开启，确保运行稳定性绝对第一。

---

## 6. Git 状态与安全边界

- **当前分支与 HEAD**: `feature/compose-modernization` (HEAD `d2e1aee`)
- **Master 分支**: `50593d96e9478a4172488b948cd7bc921e31e8ed`（绝对未合并，未修改）
- **基线 Tag**: `v4.3.1-kotlin-migration-baseline = e156d4c772b1a8a552256988cff3ab93c7a7a4fe`（绝对未变动）
- **安全红线**:
  - 零实体 NFC 写卡测试，零 UID/Block 0/Gen2 修改；
  - 零修改 `MCReader.kt`、写入状态机与底层认证算法；
  - 生产 `DumpEditor` 保持原生实现，未向远程推送，未发布正式 Release。

---

## 7. 用户人工验收待检查建议清单

用户在手机上安装 `app-debug.apk` 后，建议重点检查以下项目：

1. **语言环境切换**:
   - 进入“首选项”-> 检查当前显示是否为“跟随系统”；
   - 切换为“English (en)”并保存 -> 检查应用是否全量切换为英文；
   - 切换回“跟随系统”或“简体中文”并保存 -> 检查界面是否正确恢复中文。
2. **首选项 UID 格式交互**:
   - 点击“UID 显示格式” -> 检查是否弹出清晰的垂直单选列表，确认“十进制 (小端序)”是否排版美观无截断。
3. **帮助文档深浅色与中文**:
   - 在系统浅色与深色模式下分别打开“帮助信息”；
   - 检查文档是否为简体中文，确认暗色模式下背景是否为舒适深灰而非刺眼白色。
4. **关于对话框 (About)**:
   - 点击首页右上角“ⓘ”图标；
   - 检查版本号是否为 `v4.3.1 (70)`，核对开源协议、GitHub 链接与致敬信息排版。
5. **首页与工具箱 (MainMenu & Tools)**:
   - 检查顶部 NFC 状态英雄卡片；
   - 点击“工具箱”检查 9 项子工具弹窗的列表样式与点按响应。
