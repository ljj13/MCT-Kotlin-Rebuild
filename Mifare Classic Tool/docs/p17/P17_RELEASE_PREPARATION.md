# P17-B Fork 版本规划与发布候选准备报告 (v1.0.0 (4.3.1))

> 分支：`feature/compose-modernization`
> 阶段目标：落实用户关于放弃旧版号、建立独立版本序列的正式决策，确立 **MCT Kotlin 1.0.0（4.3.1）** 最终发布规范，完成元数据更新与发布候选材料准备。

---

## 一、Fork 独立版本体系与最终命名决策

### 1.1 最终版本命名决议
用户已正式决定废弃之前暂定的 `4.0.0` 与 `4.3.1-modern.1-preview` 命名方案，为本项目确立独立的版本序列：
- **正式发布名称**：**MCT Kotlin 1.0.0（4.3.1）**
  - `1.0.0`：代表当前 Kotlin + Jetpack Compose 现代化 Fork 自身的独立首发大版本；
  - `4.3.1`：标注对齐的原始上游 Mifare Classic Tool 基础版本；
- **具体参数配置表**：

| 项目 | 最终设置 | 说明 |
| :--- | :--- | :--- |
| **Git Tag** | `v1.0.0` | 最终合并至 master 后的不可变正式版本标签 |
| **versionName** | `1.0.0` | 写入 `app/build.gradle` 的正式版本字符串 |
| **versionCode** | `71` | 严格单调递增于上游官方的 `70`，确保升级路径畅通 |
| **About 版本显示** | `1.0.0（4.3.1）` | 呈现于应用关于对话框的来源标识 |
| **GitHub Release 标题**| `MCT Kotlin v1.0.0（4.3.1）` | GitHub 正式 Release 标题 |
| **正式发布 APK 文件名**| `MCT-Kotlin-v1.0.0.apk` | 最终签名的正式发布产物 |

> **历史记录废弃说明**：早期阶段性报告中关于 `v4.0.0` 的叙述属于当时探索记录，已全部正式废弃，统一收口为 `v1.0.0（4.3.1）`。上游历史基线 Tag `v4.3.1-kotlin-migration-baseline` 保持不可变，绝不修改或覆盖。

### 1.2 现有安装与签名升级策略分析
- **签名不兼容现状**：
  - 上游官方 Release APK 采用原作者 Gerhard Klostermeier 的私有签名密钥；
  - 当前 Fork 的 Debug APK 采用 Android SDK 本地 Debug Keystore 自动签名；
  - 当前 Fork 的 Release APK 处于未签名状态（`unsigned`）；
  - **升级约束**：因签名证书不一致，Android 底层 `PackageManager` 强制拦截直接覆盖安装（报 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`）。用户若已安装官方原版，必须先在原版中通过“导出”功能备份转储与密钥文件，卸载原版后再安装本 Fork 版本。
- **versionCode 递增保证**：
  - 上游官方 v4.3.1 对应的 `versionCode` 为 **`70`**；
  - 本次发布设定为 **`71`**，确保后续升级通道畅通，绝不发生 `INSTALL_FAILED_VERSION_DOWNGRADE`。

---

## 二、发布策略对比与最终建议

| 策略方案 | 适用条件 | 优缺点分析 | 最终建议 |
| :--- | :--- | :--- | :---: |
| **方案 A：Preview / Beta (预发布/测试候选)** | Kotlin 迁移完整、全量 22 页面 UI 2.0 完成、正常只读通过、物理写卡未全面硬件验收 | **优势**：实事求是，既能交付最新高质感 UI 与修复成果，又明确警示用户注意备份；<br>**风险**：无欺骗性宣称，用户心理预期健康。 | **推荐采纳 (RECOMMENDED)** |
| **方案 B：Stable (正式稳定版)** | 需要经过多款 NFC 芯片机型的长期实机物理写入、异常断连压测与生产签名验证 | **缺点**：目前实体写卡和异常硬件场景仍标记为 UNTESTED，直接宣称 Stable 存在误导和损坏卡片风险。 | **不予采纳** |

**最终结论**：采纳 **方案 A (Preview / Beta)** 作为本项目首个交付阶段的产品定位。

---

## 三、最新发布候选产物交付清单

### 1. Debug APK（已签名，推荐用户直接安装验证）
- **完整绝对路径**: `D:\Project\MCT-Kotlin-Rebuild\Mifare Classic Tool\app\build\outputs\apk\debug\app-debug.apk`
- **工作区相对路径**: `Mifare Classic Tool/app/build/outputs/apk/debug/app-debug.apk`
- **文件大小**: `13,644,272` 字节 (~13.0 MB)
- **SHA-256**: `CC49568175DE87D94707BDFBD79E65A769E498D353054DCF1C596BE303C35CE3`
- **签名状态**: **已签名 (Android SDK Debug Keystore)**；
- **安装指引**: 手机下载后直接点击即可安装（若曾安装过官方版，因签名密钥不同需先卸载原版，卸载前请务必使用“导出”备份密钥与转储文件）。

### 2. Release APK（构建产物，未签名）
- **完整绝对路径**: `D:\Project\MCT-Kotlin-Rebuild\Mifare Classic Tool\app\build\outputs\apk\release\app-release-unsigned.apk`
- **文件大小**: `9,112,136` 字节 (~8.69 MB)
- **SHA-256**: `A75C896F2D0690230DAEB6434F9AEED314156FC7290CB1C97977F6F3FB9EF5BA`
- **签名状态**: **未签名 (Unsigned)**；
- **声明**: Android 系统安全机制禁止直接安装未签名包，需等待用户配置正式 Keystore 签名并批准后方可发布。

---

## 四、文档与开源元数据更新状态

1. **项目说明文档**：
   - 全新重构 [README.zh-CN.md](../../README.zh-CN.md)，包含现代化架构、KernelSU UI 2.0 特色、系统安装要求、已知限制与原作者致敬；
   - 同步更新 [README.md](../../README.md)（英文主文档），提供双语切换。
2. **变更日志**：
   - 编写全量演进日志 [CHANGELOG.md](../../CHANGELOG.md)，系统梳理自基线起的 75 个提交、8 大重构领域及已知限制。
3. **许可证与著作权**：
   - 完整保留 GPLv3 许可证全文，原作者 Gerhard Klostermeier 与 SySS GmbH 署名 100% 保留。

---

## 五、用户人工验收交接说明

- **当前验收状态**：`USER_ACCEPTANCE_PENDING`（等待用户实机确认）；
- **用户建议检查要点**：
  1. 首页“读标签”“写标签”卡片说明文字是否优雅单行居中呈现，无不自然折行；
  2. 首页“文件与转储管理”单列列表是否完整显示“编辑 / 分析转储文件”与“编辑 / 新建密钥文件”，**确认无省略号截断**；
  3. 点击首页右上角“ⓘ”按钮，确认弹出的关于界面**不再闪退**，核对 Fork 与上游链接；
  4. 首选项设置项行高与排版是否紧凑，UID 格式点击弹窗是否完整无折行；
  5. 帮助信息文档在浅色/深色模式下是否均为地道中文，深色模式背景是否为舒适深灰。

---

## 六、安全与发布权限边界声明

- **禁止跨越边界**：
  - 未执行 `git push` 向远程仓库推送；
  - 未向 `master` 分支执行合并（`master` 保持 `50593d9`）；
  - 未创建正式发布 Tag（基线 Tag `v4.3.1-kotlin-migration-baseline` 保持 `e156d4c`）；
  - 未在 GitHub 上发布正式 Release；
  - 严禁擅自生成长期生产签名密钥。
- **工作流顺序**：离线准备完成 -> 用户实机验收反馈 -> 用户下达发布授权 -> 签署正式签名并发布。
