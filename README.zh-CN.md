# MIFARE Classic Tool (MCT) - Kotlin & UI 2.0 现代化重构版 (v4.0.0)

[![Release](https://img.shields.io/badge/Release-v4.0.0-blue.svg)](https://github.com/ljj13/MCT-Kotlin-Rebuild/releases)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-purple.svg)](https://kotlinlang.org)
[![Compose Material 3](https://img.shields.io/badge/Compose-Material%203-green.svg)](https://developer.android.com/jetpack/compose)
[![MinSdk](https://img.shields.io/badge/minSdk-21%20(Android%205.0)-orange.svg)](https://developer.android.com)

本仓库是基于 Gerhard Klostermeier 原版开源项目 [MifareClassicTool (v4.3.1)](https://github.com/ikarus23/MifareClassicTool) 进行全面现代化重构的 Fork 版本，首个独立正式里程碑版本设定为 **v4.0.0 (versionCode 71)**。

> **声明**：本项目为独立的开源现代化重构版本，非上游官方发行版。原版 MCT 的著作权属于原作者 Gerhard Klostermeier 与相关贡献者，本项目严格遵循 **GNU 通用公共许可证 v3.0 (GPLv3)** 协议开源。

---

## 一、重构特色与核心亮点

1. **100% Kotlin 架构重写**：
   全工程业务代码从遗留 Java 完整迁移至现代 Kotlin 2.2.10，消除了隐式空指针风险，保障了类型安全与数据流稳固性。
2. **KernelSU 风格 UI 2.0 现代视觉体系**：
   - 参考 [KernelSU Manager](https://github.com/tiann/KernelSU) 建立专业、紧凑、高对比度的技术工具界面；
   - 彻底告别传统深色模式的大面积平铺灰色，引入深沉底色（`#0D1117`）、浮层卡片（`#161B22`）与 1dp 精致边框（`#30363D`）；
   - 全量 15 个矢量图标（VectorDrawable）建立，彻底淘汰模糊旧图；
   - 响应式自适应布局，消除长标题折行碰撞与省略号截断。
3. **地道完备的中文国际化与离线文档**：
   - 抽离 `LanguageHelper`，完美支持“跟随系统 (Follow System)”、简体中文与英文无缝切换；
   - 彻底修复了原版“中文系统下打开首选项错误显示为英文”的脱节缺陷；
   - 内置全量 8 大章节的离线简体中文帮助文档（`help_zh.html`），并为 Web 视图配套暗色模式 CSS 自适应，杜绝白屏刺眼；
   - 源码清理 50 余处硬编码，全量规范化资源管理。
4. **安全保护与混合专业架构**：
   - 生产转储编辑器（`DumpEditor`）完全保留原生高性能多线程核心；
   - 写标签（`WriteTag`）六种模式表单、写入时序与安全确认流程保持 100% 冻结与保护；
   - 实验原型（`DumpEditorPrototype`）严格隔离至 Debug 变体，生产 Release 绝不暴露实验功能。

---

## 二、功能总览

- **读取标签**：基于已知密钥字典，利用字典攻击映射机制依次认证并完整读取 MIFARE Classic 1K/4K 扇区转储；
- **写入与克隆**：
  - 逐块写入指定数据；
  - 完整镜像克隆（Dump 写入）；
  - 工厂格式化恢复；
  - 对支持写入厂商块的特殊标签（魔术卡 Gen2 / CUID）写入 Block 0；
  - 增 / 减值块（Value Block）及转移/恢复；
- **密钥管理**：创建、编辑、保存、分享与去重密钥字典文件（`.keys`）；
- **转储文件分析**：
  - 高亮十六进制展示；
  - 7-bit US-ASCII 字符预览；
  - 访问控制条件（Access Conditions）矩阵表格化解析与编码；
  - 值块（Value Block）与 32 位有符号整数双向换算；
  - 厂商块生产日期（BCD）解析；
  - 双转储逐字节高亮比对（Diff Tool）；
- **实用工具箱**：
  - 标签芯片型号参数推测（UID / ATQA / SAK / ATS）；
  - BCC（Block Check Character）单倍 UID 异或校验码计算；
  - 快捷 UID 克隆辅助；
  - 数据格式转换（ASCII / Hex / Binary）；
  - 历史已读取 UID 日志与一键复制；
  - 文件导入、导出与 ZIP 完整备份还原（支持 `.mct`, `.keys`, `.bin`, `.dump`, `.json`, `.eml`）。

---

## 三、安装与运行环境

- **系统要求**：Android 5.0 (Lollipop, API 21) 及以上版本；
- **硬件要求**：
  - 必须具备内置 NFC 芯片；
  - **重要提醒**：Android 支持 NFC **不等于** 支持 MIFARE Classic！由于 NXP 专利与专有加密协议限制，部分搭载非 NXP NFC 芯片（例如部分博通 Broadcom 控制器，如 Google Pixel 某些早期机型或部分华为机型）无法读取 MIFARE Classic 物理卡片。详见上游 [不兼容设备列表](https://github.com/ikarus23/MifareClassicTool/blob/master/INCOMPATIBLE_DEVICES.md)。

---

## 四、已知限制与安全警告

1. **本应用不是密码破解工具**：
   MCT 无法无中生有地“破解”未知密码。读取扇区的前提是您在字典文件中录入了该卡片的有效密钥（可通过 Proxmark3 等专用硬件先行探测）；
2. **写卡风险与测试声明**：
   向特定控制块（如扇区尾块中的 Access Conditions）写入错误参数可能会导致扇区被永久不可逆锁定！本重构版本对底层写卡状态机进行了 100% 保真与安全隔离，但在未经全面硬件实机写卡专项测试前，**请务必使用专用测试卡进行操作，并提前做好数据备份**；
3. **不支持普通手机直接模拟 MIFARE Classic**：
   Android 官方标准 HCE（基于主机的卡模拟）仅支持 ISO-DEP / APDU 协议（如 ISO 14443-4）。受射频协议与底层硬件控制器限制，普通第三方 Android 应用**无法**直接让手机变身为通用的 MIFARE Classic 门禁卡（详见研究报告 `docs/p18/P18_NFC_EMULATION_FEASIBILITY.md`）。

---

## 五、致敬与开源许可证

- **上游原始项目**：[ikarus23/MifareClassicTool](https://github.com/ikarus23/MifareClassicTool)
- **原始作者**：Gerhard Klostermeier 与 SySS GmbH 及 Aalen 大学（2012/2013-2024）
- **现代化重构版仓库**：[ljj13/MCT-Kotlin-Rebuild](https://github.com/ljj13/MCT-Kotlin-Rebuild)
- **开源许可证**：遵循 [GNU General Public License v3.0 (GPLv3)](LICENSE.txt)。
