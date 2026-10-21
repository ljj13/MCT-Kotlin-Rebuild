# P16.5 实机反馈精修（四项定点修复）报告 (P16_5_TARGETED_FIXES_REPORT.md)

> 分支：`feature/compose-modernization` (HEAD `7c177cf`)
> 目标：针对用户最新提供的四张实机截图，完成 WriteTag 安全警告中文化、工具箱全量矢量图标补齐与排版对齐、About 页面 Fork 与上游链接精准划分、以及首页核心读写卡片尺寸优化。

---

## 一、四项定点修复详细说明

### 1. WriteTag 安全警告中文化与文案修复
- **问题诊断**：
  `WriteTagShell.kt` 顶部警告文案引用的 `R.string.write_tag_safety_notice` 仅在 `values/strings.xml`（英文）中定义，在 `values-zh/` 与 `values-zh-rTW/` 中缺失，导致中文系统下回退显示为英文：“Writing may overwrite existing data. Use a dedicated test tag and back up first.”；
- **定点修复**：
  1. 在 [values-zh/strings.xml](<Mifare Classic Tool/app/src/main/res/values-zh/strings.xml>) 中补充地道中文翻译：
     `写入可能覆盖卡片原有数据，请使用专用测试卡，并提前备份。`
  2. 在 [values-zh-rTW/strings.xml](<Mifare Classic Tool/app/src/main/res/values-zh-rTW/strings.xml>) 中补充繁体中文翻译：
     `寫入可能覆蓋標籤原有資料，請使用專用測試卡，並提前備份。`
  3. 修改 [WriteTagShellComposeTest.kt](<Mifare Classic Tool/app/src/androidTest/java/de/syss/MifareClassicTool/WriteTagShellComposeTest.kt>)，由硬编码英文断言改为动态读取 `context.getString(R.string.write_tag_safety_notice)`，保证任何语言环境测试均能通过；
  4. 同步检查并修复 [CloneUidScreen.kt](<Mifare Classic Tool/app/src/main/java/de/syss/MifareClassicTool/ui/cloneuid/CloneUidScreen.kt>) 中的硬编码标签，接入 `R.string.dialog_rest_of_block_0_title` 与 `R.string.dialog_key_for_block_0_title`。
- **安全保障**：WriteTag 原生六模式表单、写入时序、认证顺序与写卡状态机零改动。

### 2. 工具箱图标完整性与排版对齐修复
- **问题诊断**：
  原 `MainMenuScreen.kt` 的“可用的工具”弹窗中，数据转换、UID 日志、导入导出 3 项的图标定义为 `null`；当 `iconRes == null` 时不渲染图标也不留占位，导致这三项左侧缺失图标且文字缩进严重不一致；同时其余工具仍在使用 2013 年低清模糊旧 PNG；
- **定点修复**：
  1. 为全部工具创建/建立统一的矢量图形（`<vector>`）：
     - [ic_data_conversion.xml](<Mifare Classic Tool/app/src/main/res/drawable/ic_data_conversion.xml>)（数据转换）
     - [ic_uid_log.xml](<Mifare Classic Tool/app/src/main/res/drawable/ic_uid_log.xml>)（UID 日志历史）
     - [ic_import_export.xml](<Mifare Classic Tool/app/src/main/res/drawable/ic_import_export.xml>)（文件导入/导出/备份）
     - [ic_tag_info.xml](<Mifare Classic Tool/app/src/main/res/drawable/ic_tag_info.xml>)（标签芯片参数）
     - [ic_clone_uid.xml](<Mifare Classic Tool/app/src/main/res/drawable/ic_clone_uid.xml>)（UID 克隆）
     - [ic_access_condition.xml](<Mifare Classic Tool/app/src/main/res/drawable/ic_access_condition.xml>)（访问控制条件矩阵）
     - [ic_value_block.xml](<Mifare Classic Tool/app/src/main/res/drawable/ic_value_block.xml>)（值块操作）
     - [ic_bcc_calc.xml](<Mifare Classic Tool/app/src/main/res/drawable/ic_bcc_calc.xml>)（BCC 校验码计算器）
     - [ic_dump_edit.xml](<Mifare Classic Tool/app/src/main/res/drawable/ic_dump_edit.xml>)（转储比对）
  2. 弹窗内每项统一封装在 34dp 圆角容器（`primaryContainer` 柔和底色）中，保证所有 9 个工具拥有**完全一致的图标尺寸、容器宽度、图标与标题垂直居中以及 100% 对齐的左侧缩进**；
  3. 保留全部点击事件与路由 Intent，弹窗带有高度滚动约束（`heightIn(max = 420.dp)`）与取消按钮，小屏幕不遮挡。

### 3. About 页面仓库与项目链接修正
- **问题诊断**：
  原关于弹窗中的 GitHub 链接直接硬编码指向了上游 `ikarus23/MifareClassicTool`，没有正确指向当前用户的 Fork 仓库；且未清晰区分当前 Fork 与原始上游项目；
- **定点修复**：
  1. 经 `git remote -v` 实测核实，当前 Fork 仓库的真实远程地址为：`https://github.com/ljj13/MCT-Kotlin-Rebuild`；
  2. 在 [MctAboutDialog.kt](<Mifare Classic Tool/app/src/main/java/de/syss/MifareClassicTool/ui/main/MctAboutDialog.kt>) 中严谨划分层次：
     - **当前项目 (Fork)**：
       - 名称：MCT Kotlin / UI 2.0 (Fork)
       - 仓库：`https://github.com/ljj13/MCT-Kotlin-Rebuild`
       - 反馈：`https://github.com/ljj13/MCT-Kotlin-Rebuild/issues`
       - 说明：基于 Kotlin + Jetpack Compose 的 UI 2.0 现代重构版
     - **上游原始项目**：
       - 名称：Mifare Classic Tool
       - 作者：Gerhard Klostermeier (v4.3.1)
       - 仓库：`https://github.com/ikarus23/MifareClassicTool`
       - Release 标签：`https://github.com/ikarus23/MifareClassicTool/tree/4.3.1`
       - 更新日志：`https://raw.github.com/ikarus23/MifareClassicTool/master/CHANGELOG.txt`
       - 文档社区：`http://publications.icaria.de/mct/`
       - 协议：GNU General Public License v3.0 (GPLv3)
  3. 彻底避免误导用户，未虚构 Fork 的 Release 标签。

### 4. MainMenu 核心读写卡片尺寸与排版优化
- **问题诊断**：
  用户实机反馈顶部“读标签”“写标签”两张卡片偏宽，内部空间利用不合理，大字号或长文字可能产生排版失衡；
- **定点修复**：
  1. 保持 2 列响应式布局，在 [MainMenuScreen.kt](<Mifare Classic Tool/app/src/main/java/de/syss/MifareClassicTool/ui/main/MainMenuScreen.kt>) 中重构 `MainActionCard`：
     - 将高度由固定 124dp 优化为自适应最小高度 `defaultMinSize(minHeight = 74.dp)`；
     - 采用横向流式布局：左侧 36dp 图标徽章（带对应语义主题微透明底色），右侧垂直排布主标题（13.5sp 加粗）与副标题（10.5sp 柔和灰色）；
     - 副标题设置 `maxLines = 2, overflow = TextOverflow.Ellipsis`，在大字号或 360dp 窄屏下允许折行展示，**彻底杜绝文字截断**；
     - 卡片内边距设为 `horizontal = 10.dp, vertical = 8.dp`，双卡间隔设为 10dp，空间利用饱满无冗余空白；
  2. 次级卡片（转储编辑、密钥编辑、工具箱、帮助）同步优化为最小高度 58dp，视觉层级鲜明和谐；
  3. 触摸目标高度远超 48dp 无障碍标准，真实 NFC 状态广播实时响应逻辑不变。

---

## 二、测试与构建结果

- **JVM 单元测试 (testDebugUnitTest)**：
  - 执行命令：`.\gradlew.bat testDebugUnitTest`
  - 结果：**24/24 PASS (failures=0, errors=0, skipped=0)**
    - `BaselineTest`: 18 项全部通过；
    - `LanguageHelperTest`: 6 项语言映射全部通过。
- **AndroidTest 编译 (assembleDebugAndroidTest)**：
  - 执行命令：`.\gradlew.bat assembleDebugAndroidTest`
  - 结果：**BUILD SUCCESSFUL**，全量 57 个仪器测试方法（含关于弹窗打开/关闭/重开与 WriteTagShell 动态资源测试）静态编译通过。
- **双版本构建打包**：
  - `assembleDebug`: **BUILD SUCCESSFUL**
  - `assembleRelease`: **BUILD SUCCESSFUL**

---

## 三、最新交付 APK 产物信息

> 用户可直接安装 **Debug APK** 进行实机验证。

### 1. Debug APK（已签名，推荐直接安装验证）
- **完整路径**: `D:\Project\MCT-Kotlin-Rebuild\Mifare Classic Tool\app\build\outputs\apk\debug\app-debug.apk`
- **相对路径**: `Mifare Classic Tool/app/build/outputs/apk/debug/app-debug.apk`
- **文件大小**: `13,644,272` 字节 (~13.0 MB)
- **SHA-256**: `CC49568175DE87D94707BDFBD79E65A769E498D353054DCF1C596BE303C35CE3`
- **签名状态**: **已签名 (Debug Keystore)**，手机直接下载/传输点击即可覆盖安装。

### 2. Release APK（未签名产物）
- **完整路径**: `D:\Project\MCT-Kotlin-Rebuild\Mifare Classic Tool\app\build\outputs\apk\release\app-release-unsigned.apk`
- **文件大小**: `9,112,136` 字节 (~8.69 MB)
- **SHA-256**: `A75C896F2D0690230DAEB6434F9AEED314156FC7290CB1C97977F6F3FB9EF5BA`
- **说明**: 未签名 Release 产物，禁止直接安装。

---

## 四、Git 提交与不可变边界声明

- **当前分支与 HEAD**: `feature/compose-modernization` (HEAD `7c177cf`)
- **Master 分支**: `50593d96e9478a4172488b948cd7bc921e31e8ed`（绝对未合并，未修改）
- **基线 Tag**: `v4.3.1-kotlin-migration-baseline = e156d4c772b1a8a552256988cff3ab93c7a7a4fe`（绝对未变动）
- **本阶段独立提交**:
  - `f06c8c3` `fix(i18n): 补全 WriteTag 写入安全警告多语言资源并清理硬编码文案`
  - `b8c2375` `feat(toolbox): 补齐工具箱全量矢量图标映射并统一缩进排版`
  - `0a5a246` `refactor(about): 明确区分当前 Fork 仓库与上游 v4.3.1 原始项目链接`
  - `7c177cf` `refactor(home): 精修首页读写核心卡片尺寸与响应式空间排版`
- **安全边界遵守**: 零实体 NFC 写卡，零修改 UID/Block 0，严禁修改 `MCReader.kt`、写入状态机与原生 `DumpEditor`，未向远程仓库推送。
