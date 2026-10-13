# OPPO P15-A 验收进度

设备：X8OVSG49IR7X7L4D，OPPO PLB110，Android 16 / API 36，PLB110_16.0.10.500(CN01)。NFC on，屏幕 ON_UNLOCKED。MIFARE Classic 实际支持待实体卡认证确认。

已在用户确认备份并允许卸载后替换旧签名应用，安装当前 Debug APK 与测试包。

## 隔离仪器结果
每个用例单独启动 instrumentation，硬超时 90 秒，输出与 logcat 分别保存。未运行全套仪器。

| 用例 | 初次 | 测试修正后 |
|---|---|---|
| KeyMapCreator 密钥选择 | FAIL，2.773s，点击无点击动作的 Row 导致回调 null | PASS，82.263s，OK (1 test)；改为点击 Row 内真实 Checkbox |
| P14 Native 1K | FAIL，1.060s，完整文本断言误用为子串 | PASS，1.083s，OK (1 test)；精确匹配预置完整文本 |
| P14 Compose 4K | 未执行旧断言版本 | PASS，1.214s，OK (1 test)；精确匹配预置完整文本 |

仅测试文件变化，生产 UI 和 NFC 算法未改。保留初次 FAIL，不将其改写为 PASS。
P14 测试使用预置 BenchmarkMetrics 和样本，只验证渲染/回调，不证明实际编辑后逐字节保真、真实性能或生产 DumpEditor 替换资格。
密钥测试 82 秒接近上限，不能据此宣称性能良好；需与真实 Activity 验收分开。

## 首次真实只读结果
已从正常读取密钥页选择 std.keys、全部扇区，用户贴卡后执行映射与读取并进入生产 DumpEditor。保存本地转储 read-0BDC504F.mct（只是手机文件保存，不是写卡），共 80 行：扇区 0..15 连续，每扇区 4 个 32 位十六进制块，共 64 块，无缺失/死扇区标记。UID 0BDC504F，Block 0 为 0BDC504FC80804006263646566676869。
P7 历史截图仅能对照部分数据。随后从冻结标签对应提交 50593d96e9478a4172488b948cd7bc921e31e8ed 创建独立 detached worktree（D:/Project/MCT-Kotlin-Rebuild-P7-ReadOnly），不修改源码构建 Debug，覆盖安装并由用户重新贴同一卡完成原生流程只读采样，保存 p7-read-0BDC504F.mct。两份完整文件 SHA-256 均为 ACFAFDF3D7AA970BBF4C6D05F8675E546CB171AB19A9269716A5D39D863802D5：正常读取的完整基线对照 PASS。已恢复安装 Compose Debug。

## 生命周期修正后正常读取复验
已移除生产 extra_skip_key_map，两个读取/映射 worker 用 finally 关闭连接；增加映射 worker busy 与销毁防护，读取中重建采用结束替代实例策略。Debug 构建及 BaselineTest 18/18（0 failures/errors/skipped）通过，新包安装 OPPO 后再由用户移卡重新贴附，正常读取进入生产 DumpEditor。
fixed-read-0BDC504F.mct 与 p7-read-0BDC504F.mct SHA-256 同为 ACFAFDF3D7AA970BBF4C6D05F8675E546CB171AB19A9269716A5D39D863802D5，正常读流程复验 PASS。取消/离页/断连/重建等异常路径未实测完，不能据此标 PASS。

## 生命周期复核问题记录（部分已修，异常路径待验）
实际源码仍存在：KeyMapCreator 的 reader.close() 在 Handler 完成回调里，而 onPause 非 AutoReconnect 会移除回调，存在清理被丢弃的路径；回调只检查 isFinishing，没有 destroyed/session 防护。取消标志与工作线程退出需要独立管理，避免旧任务影响新任务。ReadTag 仍有生产 extra_skip_key_map 参数、读连接关闭未置于 finally，重建后的读取等待态需要明确终止或恢复策略。以上不因正常读卡 PASS 而视为解决；需最小修复和针对性验证，暂不收口。

## 已完成的空闲生命周期检查
- 密钥页未扫描时点取消，返回 MainMenu，无 DumpEditor 启动（idle-cancel-after.xml）。
- 密钥页未扫描时 Home 离页后返回原任务，前台仍为 KeyMapCreator，界面保留（idle-leave-return.xml）。
- 以上仅为空闲路径，不代表扫描/读取中取消、离页、断连或重建已通过。

## 扫描中取消尝试（未覆盖）
只选择 extended-std.keys 后连续点击开始/取消，最终返回 MainMenu。但日志 22:51:03.382 明确显示 NfcRwRecorder connect error=tagNotPresent，随后 MCReader Error while connecting to tag；因此扫描实际未启动，本次不得标为扫描中取消 PASS。需要重新发现实体卡后再覆盖。证据 inflight-cancel-logcat.txt 保留。
ReadTag 单用例初次因完整文本断言误用 FAIL（0.968s）；修正测试预期完整文本后隔离执行 OK (1 test)，1.045s。仅证明 UI 回调，不证明 NFC worker 生命周期。

## 用户要求跳过扫描中取消
用户明确要求“跳过了跳过了，这个没问题了”。停止重复请求扫描中取消操作。该项没有有效扫描中证据，状态为 SKIPPED / UNTESTED，不改写为 PASS；用户口头判断不替代测试报告。最终收口必须披露这一例外，不能宣称所有生命周期路径验收通过。

## 最新包正常复验与 P15-B 评估
最后追加的 pause 完成回调/busy gate 修正已构建安装；用户于 2026-10-14 正常重新贴卡读取，ADB 确认生产 DumpEditor 前台，保存 latest-read-0BDC504F.mct。它与冻结 P7 转储 SHA-256 完全一致（ACFAFDF3D7AA970BBF4C6D05F8675E546CB171AB19A9269716A5D39D863802D5）。最后源码版本正常只读闭环 PASS，最新截图/日志保留。
P15-B：暂不进入实现。可以后续独立制定 WriteTag UI 迁移方案，但不能把本阶段只读 PASS 推导为写卡安全；生产 WriteTag/MCReader/Block 0/Gen3 不改。异常生命周期未验项及用户取消跳过项需要在批准下一阶段时披露。

## 阶段定论与验收状态归档
P15-A 正式归档为 **PARTIAL / DEFERRED**：
- **PASS 项**：
  1. 真实实体卡只读闭环（OPPO Find X8s+ 识别 0BDC504F，16 扇区 64 块认证读取并渲染至生产 DumpEditor）
  2. 转储哈希基线一致性（最新包与冻结 P7 基线 50593d9 哈希均为 ACFAFDF3D7AA970BBF4C6D05F8675E546CB171AB19A9269716A5D39D863802D5）
  3. 纯算法与逻辑回归（BaselineTest 18 项全绿）
  4. 隔离 UI 交互测试（KeyMapCreator, ReadTag, P14 原型 1K/4K 经修正选择器/完整断言文本后均执行通过；保留初次真实 FAIL 报告）
  5. 空闲生命周期操作（未扫描时点击取消返回主页；未扫描时切后台返回原任务恢复状态正常）
- **SKIPPED / UNTESTED 项**：
  1. 扫描中取消（用户明确指示跳过，日志证实前期尝试未处在扫描中，不推断通过）
- **DEFERRED / UNTESTED 项**：
  1. 扫描中离页 / 切后台
  2. 物理通信中断 / 掉卡未决状态机
  3. Activity 进程/配置重建
- **测试替身评估结论**：工程无 NFC 虚拟测试替身/Shadow 驱动，为免侵入污染生产底层通信层，上述项严格保持 DEFERRED。
- **P15-B 只读预审结论**：已完成 WriteTag 状态机与 Intent/UI 耦合只读审计；明确了“替换清单 vs 严格冻结区”；本阶段禁止代码改动、禁止写卡。

用户贴附专用 1K 测试卡，std.keys 选择、密钥映射、16 扇区认证、64 块读取、生产 DumpEditor 展示与 P7 基线逐字节对照；取消/离页/断连/重复贴卡及重建验收仍需证据。
P15-A 未最终收口；不进入 P15-B。不写卡、不改 UID/Block 0、不执行 Gen3。不推送、不发布。
