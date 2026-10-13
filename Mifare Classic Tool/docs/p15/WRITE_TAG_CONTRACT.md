# WriteTag 功能契约（冻结）

> 依据 `feature/compose-modernization` 分支当前源码（HEAD `0109f72`）与冻结 Kotlin 迁移基线
> `v4.3.1-kotlin-migration-baseline`（`master` = `50593d9`）逐行核对提取。
> 本文档仅描述事实，不推断、不重命名内部实现。后续 P15-B UI 改造不得改动下表冻结项。

## 1. Activity 与生命周期

| 项 | 事实 |
| :--- | :--- |
| 类 | `de.syss.MifareClassicTool.Activities.WriteTag : BasicActivity` |
| 基类 | `BasicActivity : AppCompatActivity`（在 `onResume`/`onPause` 内做 NFC 前台分发，`onNewIntent` 将新 Intent 当作新标签并可能跳转 `TagInfoTool`） |
| Manifest | `configChanges="keyboardHidden|orientation|screenSize"`，`label=title_activity_write_tag`，`icon=write_tag` |
| 主题 | `AppTheme`（`Theme.AppCompat.DayNight`）；API 35+ 显式 `android:windowOptOutEdgeToEdgeEnforcement=true`，即非 edge-to-edge，内容起点在系统栏之下 |
| `onCreate` | `setContentView(R.layout.activity_write_tag)` 后同步 `findViewById` 初始化全部控件；`mWriteModeLayouts` 依次加入六个模式布局；恢复 `savedInstanceState`（`write_manuf_block`、`dump_with_pos`）；处理 `EXTRA_DUMP` |
| `onSaveInstanceState` | 保存 `write_manuf_block`（布尔）与 `dump_with_pos`（`HashMap<Int, HashMap<Int, ByteArray>>`，`Serializable`） |
| `onActivityResult` | 按 request code 分派；`resultCode != OK` 时记录 `ckmError`；`ckmError == 4` 显示 `info_strange_error` |
| 返回/关闭 | 无覆写 `onBackPressed`；系统返回直接结束 Activity；`finish()` 只结束 Activity，**不终止已启动的工作线程写卡** |

## 2. 布局与控件 ID → 事件处理映射

根结构：`ScrollView#scrollViewWriteTag` → `LinearLayout#linearLayoutWriteTag` → `RadioGroup#radioGroupWriteTagMode`
（六个模式选择 RadioButton 与其模式布局交错排列在该 RadioGroup 内）。

### 2.1 模式选择（`android:onClick="onChangeWriteMode"`）

| RadioButton ID | tag | 默认 checked |
| :--- | :--- | :--- |
| `radioButtonWriteTagWriteBlock` | `write_block` | 是 |
| `radioButtonWriteTagWriteDump` | `write_dump` | 否 |
| `radioButtonWriteTagCloneUid` | `clone_uid` | 否 |
| `radioButtonWriteTagFactoryFormat` | `factory_format` | 否 |
| `radioButtonWriteTagWriteValueBlock` | `write_value_block` | 否 |
| `radioButtonWriteTagTransferRestore` | `writetag_transfer_restore` | 否 |

`onChangeWriteMode(view)`：先把 `mWriteModeLayouts` 中全部布局置 `GONE`，再用
`linearLayoutWriteTag.findViewWithTag(view.tag + "_layout")` 定位目标布局置 `VISIBLE`。
`mWriteModeLayouts` 顺序：write_block → write_dump → clone_uid → factory_format → write_value_block → writetag_transfer_restore。

### 2.2 六模式布局与控件

| 模式 | 布局 ID（tag） | 控件 | 事件 |
| :--- | :--- | :--- | :--- |
| 写块 | `relativeLayoutWriteTagWriteBlock`（`write_block_layout`） | `editTextWriteTagSector`(number,2)、`editTextWriteTagBlock`(number,2)、`editTextWriteTagData`(hex,32)、`buttonWriteTagBlock` | `onWriteBlock` |
| 写转储 | `linearLayoutWriteTagDump`（`write_dump_layout`） | `buttonWriteTagDump`、`checkBoxWriteTagDumpOptions`、`checkBoxWriteTagDumpStaticAC`、`editTextWriteTagDumpStaticAC`(hex,6,默认 `text_static_ac_default`)、`imageButtonWriteTagDumpStaticACInfo`、`checkBoxWriteTagDumpWriteManuf`、`imageButtonWriteTagDumpWriteManufInfo` | `onWriteDump`、`onShowOptions`、`onShowStaticACInfo`、`onShowWriteManufInfo` |
| 克隆 UID | `linearLayoutWriteTagCloneUid`（`clone_uid_layout`） | `buttonWriteTagCloneUid` | `onCloneUid`（`startActivity(CloneUidTool)`，无结果回传） |
| 出厂格式化 | `linearLayoutWriteTagFactoryFormat`（`factory_format_layout`） | `buttonWriteTagFactoryFormat`、`textViewWriteTatFactoryFormatInfo` | `onFactoryFormat` |
| 值块 | `relativeLayoutWriteTagValueBlock`（`write_value_block_layout`） | `editTextWriteTagValueBlockSector`(number,2)、`editTextWriteTagValueBlockBlock`(number,2)、`editTextWriteTagValueBlockValue`(number,10)、`radioGroupWriteTagValueBlock`(`radioButtonWriteTagWriteValueBlockIncr` 默认 checked / `radioButtonWriteTagWriteValueBlockDecr`)、`buttonWriteTagValueBlock` | `onWriteValue` |
| Transfer/Restore | `relativeLayoutWriteTagTransferRestore`（`writetag_transfer_restore_layout`） | `editTextWriteTagTransferRestoreStagingSector/StagingBlock/DestinationSector/DestinationBlock`(number,2)、`editTextWriteTagTransferRestoreValue`(number\|signed,10)、`editTextWriteTagTransferRestoreAddress`(hex,2)、`buttonWriteTagTransferRestore` | `onWriteValueBlockTransferRestore` |

## 3. Intent / Request Code / Result Code 契约

| 方向 | 常量 | 值 | 说明 |
| :--- | :--- | :--- | :--- |
| 输入 | `EXTRA_DUMP` | `de.syss.MifareClassicTool.Activity.DUMP` | `DumpEditor` 传入 `String[]`；置 `mWriteDumpFromEditor=true`、`mDumpFromEditor`，程序化点击 `radioButtonWriteTagWriteDump`，禁用 `radioButtonWriteTagWriteBlock` 与 `radioButtonWriteTagFactoryFormat`，`buttonWriteTagDump` 文案改为 `action_write_dump` |
| 派发 | `FC_WRITE_DUMP` | 1 | `FileChooser` 选转储文件（`EXTRA_DIR=Common.DUMPS_DIR`、`EXTRA_TITLE=text_open_dump_title`、`EXTRA_CHOOSER_TEXT=text_choose_dump_to_write`、`EXTRA_BUTTON_TEXT=action_write_full_dump`）→ `readDumpFromFile` |
| 派发 | `CKM_WRITE_DUMP` | 2 | `KeyMapCreator`（`keys_from_dump.keys` 临时密钥 + 扇区范围）→ `checkDumpAgainstTag` |
| 派发 | `CKM_WRITE_BLOCK` | 3 | `KeyMapCreator`（单扇区）→ `writeBlock` |
| 派发 | `CKM_FACTORY_FORMAT` | 4 | `KeyMapCreator`（全卡）→ `createFactoryFormattedDump` |
| 派发 | `CKM_WRITE_NEW_VALUE` | 5 | `KeyMapCreator`（单扇区，值块文案）→ `writeValueBlock` |
| 派发 | `CKM_VALUE_TRANSFER_RESTORE` | 6 | `KeyMapCreator`（范围）→ `runValueTransferRestore` |
| 输出 | — | — | 写成功仅 Toast `info_write_successful` + `finish()`，不向调用方回传破坏性结果 |

## 4. 确认对话框与危险入口

| 对话框 | 触发 | 确认动作 |
| :--- | :--- | :--- |
| 扇区尾块警告 `dialog_sector_trailer_warning` | `onWriteBlock` 且 block ∈ {3,15}，且 `checkAccessConditions(data,true)!=1` | 正键 `action_i_know_what_i_am_doing` → `createKeyMapForBlock(sector,false)`；负键 `action_cancel` |
| 块 0 厂商块警告 `dialog_block0_writing` | `onWriteBlock` 且 sector==0 && block==0 且 `checkBlock0` 通过 | 正键 `action_i_know_what_i_am_doing` → `createKeyMapForBlock(0,false)` |
| 块 0 静态信息 `showWriteManufInfo(false)` | `imageButtonWriteTagDumpWriteManufInfo` | 正键 `action_ok`（无写动作） |
| 静态 AC 信息 `dialog_static_ac` | `imageButtonWriteTagDumpStaticACInfo` | 正键 `action_ok` |
| 扇区选择 `dialog_write_sectors` | `checkDumpAndShowSectorChooserDialog` | 正键实际重绑定：`initDumpWithPosAndKeysFromDump` → 按勾选移除扇区 → `isSectorInRange` → `createKeyMapForDump` → `dialog.dismiss()` |
| 写前问题清单 `dialog_write_issues_title` | `checkDumpAgainstTag` 且问题列表非空 | 正键 `action_skip_blocks` → `writeDump(writeOnPosSafe,keyMap)`；负键 `action_cancel_all`；列表为空时直接 `writeDump`，无统一最终确认 |
| 写入中 `dialog_wait_write_tag_title`（Indeterminate ProgressBar + TextView） | `writeDump` 内部 | 无按钮；**未 `setCancelable(false)`**；关闭对话框不等于停止写卡 |

## 5. 写卡入口与线程模型（冻结）

| 入口 | 线程 | 说明 |
| :--- | :--- | :--- |
| `writeBlock()` | 主线程同步 | `Common.checkForTagAndCreateReader` → `reader.writeBlock`，Key B(`keys[1]`) 先、Key A(`keys[0]`) 后（**即使 B 成功仍尝试 A**）；结果合并：任一 0→0；任一 2→2；否则 -1。0 成功、2 块不在扇区、-1 错误 |
| `writeValueBlock()` | 主线程同步 | Key B 先；`result == -1 && keys[0]!=null` 才用 Key A；2 / -1 报错并 `return`，成功 Toast + `finish` |
| `runValueTransferRestore()` | 主线程同步 | 暂存键按写权限码选（1→A、2→B、3→B 优先 A）；目标键按 `i in 0..1`（A 后 B）遍历；`reader.valueTransferRestore` |
| `checkDumpAgainstTag()` | 主线程同步 | `reader.isWritableOnPositions`；逐块生成写权限码与问题清单 |
| `writeDump()` | **独立 `Thread`** | 循环 `writeOnPos.keys` → 逐块 `reader.writeBlock`，**单块硬编码重试 2 次**；写权限码选键（1/4→A、2/5/6→B）；失败即 Toast `info_write_error` + `reader.close()` + `warning.cancel()` + `return@Thread`；成功 Toast `info_write_successful` + `a.finish()` |

> 关键事实：**只有 `writeDump()` 使用工作线程**；`writeBlock`/`writeValueBlock`/`runValueTransferRestore` 均在主线程同步执行（原有行为，不在 P15-B 改动范围）。

## 6. 校验、错误与异常路径

- `checkSectorAndBlock`：扇区/块为空 → `info_data_location_not_set`；扇区 >39 或 <0 → `info_sector_out_of_range`；块 >3 或 <0 → `info_block_out_of_range`。
- `onWriteBlock`：`Common.isHexAnd16Byte(data,this)` 校验 16 字节 hex；`checkAccessConditions`/`checkBlock0` 走危险确认或报错。
- `onWriteDump`：静态 AC 若开启，须 `[0-9A-Fa-f]+` 且长度 6，否则 `info_ac_not_hex` / `info_ac_not_3_byte`。
- `readDumpFromFile`：`Common.readFileLineByLine` 失败由 `checkDumpAndShowSectorChooserDialog` 的 `Common.isValidDump` 报错（`Common.isValidDumpErrorToast`）。
- `checkDumpAgainstTag`：读卡失败 `info_tag_lost_check_dump`；卡片过小 `info_tag_too_small`；`keyMap==null` 直接返回。
- 部分成功：MIFARE Classic 无事务/回滚；`writeDump` 失败即中止，已写入前序块不可撤销（只提示，不恢复）。

## 7. 允许 / 禁止修改边界

**允许（P15-B.1 外壳层）**：新增 M3 顶栏/说明卡/信息区；调整外层圆角、间距、分组、标题与滚动容器；新增非写入 Compose UI 测试；纯展示性安全提示文案。

**禁止（本轮冻结）**：改动 `writeBlock/writeDump/writeValueBlock/runValueTransferRestore/createFactoryFormattedDump`；改动重试次数、密钥顺序、扇区/块遍历顺序；改动 `KeyMapCreator` 请求码与 `keys_from_dump.keys`；改动写卡工作线程/连接管理/异常处理/进度对话框控制语义；将关闭对话框解释为终止写卡；绕过确认与访问条件校验；改动 `MCReader`、`MCReader.writeBlock0Gen3` 及 Gen3/Gen2/UID/Block0/SAK/ATQA 逻辑；改动生产 `DumpEditor`。

## 8. 与基线差异核对

- 对照 `v4.3.1-kotlin-migration-baseline`（`50593d9`），`WriteTag`、`activity_write_tag.xml`、`dialog_write_sectors.xml`、`MCReader` 当前分支与基线**无业务差异**。
- 差异仅存在于 P15-A 已提交的 `ReadTag.kt`、`KeyMapCreator.kt` 及其测试（不影响 WriteTag）。
- 新增文档/测试不改变生产写卡行为。
