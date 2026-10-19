# P18 NFC 卡模拟技术可行性研究与工程路线报告 (P18_NFC_EMULATION_FEASIBILITY.md)

> 文档性质：MCT 进阶能力架构研究报告（独立于当前 UI 2.0 发布候选）
> 目标：系统解答“能否将 MCT 读取保存的卡片数据直接用手机 NFC 模拟刷卡”的核心命题，深入分析 Android HCE 协议边界、硬件控制器架构及外部设备方案，为后续路线选择提供决策依据。

---

## 核心结论速览 (Executive Summary)

1. **普通 Android 应用无法直接通过手机 NFC 模拟原生 MIFARE Classic 物理卡片**：
   - **根本原因**：MIFARE Classic 基于 ISO/IEC 14443-3A 层专有的 **Crypto1 硬件流密码**，而 Android 官方 HCE 仅工作在 ISO/IEC 14443-4（ISO-DEP）与基于 AID 的 APDU 路由层；
   - **硬件与射频栈限制**：Android 系统的 NFC 控制器（NFC Controller Interface, NCI）不向操作系统（更不向上层普通应用）暴露底层的 Crypto1 状态机与逐位位级握手（Bit-level communication）。手机在收到读卡器发送的 MIFARE Classic 认证请求时，射频芯片会直接报错或丢弃。
2. **手机厂商“模拟门禁卡”功能的实质与局限**：
   - 小米/OPPO/华为等系统自带的“钱包/门禁卡模拟”，实质是在嵌入式安全元件（eSE）或驱动特权层模拟了一个 **仅含固定 UID 的纯卡号（Blank UID-only card）**；
   - 一旦实际门禁系统开启了扇区加密校验（Crypto1 验证 Key A/B），厂商自带的卡模拟同样无法刷开。
3. **最可行、最稳健的下一代工程研发路线**：
   - **采纳路线 B（外部模拟设备联动）**：将 MCT 打造为便携式模拟设备（如 **ChameleonUltra / ChameleonMini**）的管理与卡数据同步前端（通过 Bluetooth LE / USB-OTG 控制）。既能完整实现全扇区 Crypto1 模拟与魔术卡写入，又绝无手机系统崩溃、Root 越权或封号风险。

---

## 一、NFC 射频体系与协议能力矩阵

必须从底层射频协议与上层通信语义两个维度区分不同类型的卡片：

| 技术类型 / 协议 | 射频协议层级 | Android 普通 App 能否读取 | Android 官方 HCE 能否模拟 | 是否需要厂商特权/SE | 保存转储能否完整模拟卡片 | 核心技术瓶颈 / 限制条件 |
| :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| **MIFARE Classic (1K/4K)** | ISO 14443-3A + 专有 Crypto1 | **是** (需 NXP/兼容 NFC 芯片) | ❌ **无法模拟** | 需专用硬件与专有固件 | 否 (转储在手仍缺 Crypto1 模拟能力) | 属于 ISO 14443-3 物理层专有加密流密码，不走 ISO 14443-4 APDU 规范，Android HCE 无法拦截或处理认证帧。 |
| **MIFARE Ultralight / NTAG** | ISO 14443-3A (无密码/基础密码) | **是** | ⚠️ **部分支持** (仅限通过 NDEF 封装) | 否 | 仅限 NDEF 格式数据 | 原生只读分页和特定防撕裂时序在标准 HCE 下无法完全保真。 |
| **MIFARE DESFire (EV1/EV2/EV3)**| ISO 14443-4 (ISO-DEP) + 3DES/AES | **是** | ⚠️ **可模拟 APDU 通道** (但密钥未知) | 需知道主密钥或硬件 SAM | 否 (DESFire 无法导出加密密钥) | 传输层属于标准 APDU，但应用层加密无法被克隆。 |
| **ISO/IEC 14443-4 (ISO-DEP)** | ISO 14443-4 标准 APDU | **是** | ✅ **完全支持 (官方 HCE 核心)** | 否 | **是** (对于自定义 APDU 交互协议) | **Android 官方 HCE 的唯一标准工作模式**。根据 AID 进行服务路由。 |
| **交通卡 / 银行卡 (PBOC / EMV)** | ISO 14443-4 + eSE / Token | 仅限读取公开日志 | ❌ **普通 App 无法模拟** | 必须运营商/银联 eSE 专有签名授权 | 否 | 私钥受金融级硬件安全元件保护，不可导出。 |
| **纯 UID 门禁卡 (非加密 ID)** | ISO 14443-3A 仅防冲撞 UID | **是** | ❌ **普通 HCE 随机 UID** | 需厂商钱包特权或 Root 补丁 | **是** (若门禁仅比对卡号) | Android 原生 HCE 默认向读卡器提供动态生成的随机 UID（以 `0x08` 开头），禁止第三方 App 写入固定 UID。 |

---

## 二、Android 官方 HCE (Host-based Card Emulation) 深度解构

### 2.1 HCE 架构设计与消息流转
Android 从 4.4 (KitKat, API 19) 开始引入 HCE，其架构逻辑如下：
```
[ 外部读卡器 (POS/门禁) ]
         │ (13.56 MHz 射频天线)
         ▼
[ 手机硬件 NFC 控制器 (NFC Controller) ]
         │ (NCI 协议 / 固件路由表)
         ▼
[ Linux NFC 驱动 & AOSP libnfc-nci ]
         │
         ▼
[ Android NFC 系统服务 (NfcService) ]
         │ (AID 匹配: 0xA000000...)
         ▼
[ 第三方 HostApduService (普通 App) ]
```

### 2.2 官方 HCE 的核心工作规则
1. **必须声明 HostApduService 与 AID**：
   HCE 服务必须在 `AndroidManifest.xml` 中声明 `android.nfc.cardemulation.host_apdu_service`，并在 XML 配置中列出应用响应的 Application Identifier (AID)；
2. **读卡器必须首先发送 SELECT AID 指令**：
   外部读卡器贴近手机时，发出的第一条 APDU 指令必须是：
   `00 A4 04 00 [AID Length] [AID Bytes]` (ISO 7816-4 SELECT FILE 指令)；
   NfcService 根据该 AID 唤醒对应的 App 并传递字节数组：
   `processCommandApdu(byte[] commandApdu, Bundle extras): byte[]`；
3. **随机 UID 安全保护 (Random UID Policy)**：
   为保护用户隐私、防止用户行踪被物理设备追踪，Android 明确规定：
   **基于主机的卡模拟（HCE）连接建立时，NFC 芯片向外发送的 Anti-collision UID 必须为动态随机数（通常以 08 开头代表随机 UID）**；
   Google 严格禁止第三方普通应用通过任何公共 API 指定或锁定模拟卡的射频层 UID。

---

## 三、MIFARE Classic 与 Android HCE 的本质冲突

为什么普通应用“读得出转储，却绝对无法自己模拟出卡片”？存在三大不可逾越的底层鸿沟：

### 1. 协议栈断层：APDU 语义层 vs Crypto1 射频层
- **MIFARE Classic 的认证握手**：
  读卡器发出 `0x60` (Auth Key A) 或 `0x61` (Auth Key B) 及其扇区地址，随后标签与读卡器进行 3 轮相互挑战应答（基于 Crypto1 伪随机数发生器与线性反馈移位寄存器 LFSR）；
- **Android HCE 的通信接口**：
  Android 操作系统与应用层只认识 ISO 7816-4 格式的 APDU（Command-Response 结构）。**NFC 控制器固件根本没有将非标准 Crypto1 认证帧上报给 Android 系统的通路**，读卡器发出 0x60/0x61 指令时，NFC 芯片直接因无法识别而终止握手。

### 2. 硬件芯片与固件微码锁死
- 即使手机采用 NXP 出品的 NFC 芯片（支持作为读卡器读取 MFC），其芯片出厂固件内部的“卡模拟路由表”也仅将符合 ISO-DEP 规范的流量路由至 Host CPU；
- 手机不是通用软件定义无线电（SDR），无法通过 Java/Kotlin 代码在纳秒级时序下操控天线逐位模拟 Crypto1 移位寄存器。

### 3. 系统权限防线与随机 UID
- 许多旧式简易门禁系统甚至不校验扇区，只比对卡片 4 字节 UID；
- 但由于 Android HCE 强制使用随机 UID，读卡器每次读到的 UID 都在改变，导致即使是最简单的纯卡号门禁，官方 HCE 也无法刷开。

---

## 四、目标设备硬件档案与特权能力分析

| 设备型号 | NFC 控制器型号 | 官方 HCE 能力 | 系统钱包卡模拟机制 | 普通第三方 App 能否模拟卡 |
| :--- | :--- | :--- | :--- | :---: |
| **Redmi Note 8 Pro** (`begonia`) | NXP PN547 / MediaTek 驱动 | 支持标准 ISO-DEP HCE | 小米钱包支持写入空白门禁卡 (依赖特权服务修改 eSE / NFC 路由表) | ❌ 仅限标准 AID APDU 服务 |
| **OPPO Find X8s+** (`X8OVSG...`) | 现代高集成 NXP / ST 旗舰芯片 | 支持标准 ISO-DEP HCE | 欢太/OPPO 钱包支持白卡模拟与穿戴设备卡包 | ❌ 仅限标准 AID APDU 服务 |

**分析结论**：
手机厂商自带的“模拟门禁卡”之所以能生成固定卡号，是因为厂商系统钱包拥有系统签名（Platform Signature）与特权 SELinux 策略，能直接通过私有 IPC 向底层 NFC 守护进程注入固定 UID 配置。**任何第三方开源软件（包括 MCT）均无权调用此私有接口**。

---

## 五、三种可行性路线深度对比

针对用户未来提出的卡模拟需求，深入论证三种工程路线：

```
                ┌───────────────────────────┐
                │    MCT 卡模拟演进路线     │
                └─────────────┬─────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
   [ 路线 A ]            [ 路线 B ]            [ 路线 C ]
 原生 Android HCE      外部模拟设备联动      系统特权与内核补丁
 (标准 ISO-DEP)       (Chameleon/Proxmark)    (Root / Xposed)
 ─────────────────    ──────────────────    ─────────────────
 适用: 自定义读卡器    适用: 全协议实体门禁   适用: 仅限极客极少机型
 难度: 中等 (协议受限) 难度: 可控 (最佳方案)  难度: 极高 (极易变砖)
 落地: 仅限实验展示    落地: 推荐工程采纳    落地: 不建议采纳
```

### 5.1 路线 A：原生 Android HCE 实验室模块 (官方能力)
- **实现方案**：
  在 MCT 中新增独立的 `HceLabService : HostApduService`；
- **能力边界**：
  - 只能模拟用户自行开发的自定义 ISO-DEP 卡片（例如基于特定 AID 的虚拟数据交互）；
  - **绝不能模拟 MIFARE Classic 门禁卡或物理卡**；
- **评估**：适合作为未来学习、测试标准 NFC 读卡器的实验子模块，但无法满足用户“直接刷真实门禁”的诉求。

### 5.2 路线 B：外部模拟设备联动前端 (强烈推荐 / 最佳落地方案)
- **实现方案**：
  保持手机作为高生产力的“控制大脑与数据仓库”，通过 **BLE (低功耗蓝牙) 或 USB-OTG** 控制专业便携模拟硬件（如 **ChameleonUltra / ChameleonMini**）：
- **硬件对比与评估**：
  1. **ChameleonUltra (变色龙 Ultra)**：
     - 开源生态极其成熟，配备官方公开的蓝牙与串口通讯协议；
     - 硬件内部拥有 FPGA/专用射频芯片，**100% 完美模拟 MIFARE Classic 1K/4K 各种密钥、UID、BCC 及后门响应**；
     - 外形如车钥匙大小，便携且自带电池；
     - MCT 可作为专属管理客户端：在手机上读取并管理卡片转储，一键蓝牙写入 ChameleonUltra 对应卡槽！
  2. **Proxmark3**：
     - 功能强大但偏极客，便携度与蓝牙适配成本高于 ChameleonUltra。
- **评估结论**：这是目前全行业在不违反 Android 安全模型前提下，**唯一成熟、稳定、支持全部加密门禁卡模拟并具备商用可用性的工程路线**。

### 5.3 路线 C：系统特权、Root 补丁与 NFC HAL 篡改 (不推荐)
- **技术原理**：
  借助 KernelSU / Magisk 模块，替换系统 `/system/etc/libnfc-nci.conf`、修改 NFC 动态库或刷入定制固件（如定制驱动强制设定静态 UID）；
- **致命缺陷**：
  - **严重机型依赖**：不同品牌芯片配置截然不同，极易引发 NFC 服务不断崩溃、耗电剧增甚至手机开机变砖；
  - **仍然无法解决 Crypto1 硬件缺失**：绝大多数机型即便修改了静态 UID，依然无法响应扇区密码认证；
- **评估结论**：破坏系统稳定与安全模型，严重违背 MCT 作为大众专业工具的定位，坚决不予采纳。

---

## 六、未来产品功能架构与安全定位建议

若未来版本启动卡模拟相关研发，建议在 UI 上明确隔离三大业务层级，避免用户产生认知误区：

```
MCT 未来功能矩阵
├── 1. 卡片数据中心 (Card Management) ── [已完成] 读取、编辑、转储、密钥字典、文件转换
├── 2. HCE 实验实验室 (HCE Sandbox)   ── [路线 A] 针对标准 ISO-DEP 读卡器测试自定义 APDU
└── 3. 硬件模拟助手 (Emulator Bridge) ── [路线 B] 通过 BLE/USB 同步数据至外部 Chameleon 设备
```

**安全红线严正声明**：
- 严禁向用户承诺“任意门禁一键复制到手机”；
- 严禁尝试绕过金融、交通及涉密系统的安全机制；
- 模拟功能仅限于用户拥有合法使用权与测试授权的场景。

---

## 七、P18 最终技术决策与推荐实施路线图

| 核心决策点 | 权威技术结论 |
| :--- | :--- |
| **A. 官方 HCE 可行性** | **有限可行**。可作为自定义 APDU/NDEF 交互实验，但**无法作为 MIFARE Classic 物理卡片模拟方案**。 |
| **B. 原生 MFC 模拟可行性** | **完全不可行 (Hard Limit)**。受射频 Crypto1 流密码、固件路由及随机 UID 机制物理锁死，第三方普通 App 无法突破。 |
| **C. 最优工程路线选择** | **选择路线 B (外部模拟器联动)**。计划在后续大版本中引入低功耗蓝牙 (BLE) 协议桥，支持与 ChameleonUltra 无缝联动。 |

### 下一阶段推荐执行顺序
1. **当前阶段**：收束 P16/P17，交付 UI 2.0 冻结版 APK，等待用户人工实机验收；
2. **未来研发期 (P18 实施期)**：
   - 第一步：研究 ChameleonUltra 开源 BLE 通讯协议规范；
   - 第二步：在 MCT 中设计独立的设备连接管理界面与卡槽配置下发协议；
   - 第三步：实现将本地 `.mct` 转储一键烧录至外设卡槽完成物理模拟。
