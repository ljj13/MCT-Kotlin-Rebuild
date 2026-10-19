# MIFARE Classic Tool (MCT) - Kotlin & UI 2.0 Modernization Fork

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-purple.svg)](https://kotlinlang.org)
[![Compose Material 3](https://img.shields.io/badge/Compose-Material%203-green.svg)](https://developer.android.com/jetpack/compose)
[![MinSdk](https://img.shields.io/badge/minSdk-21%20(Android%205.0)-orange.svg)](https://developer.android.com)

A modernized, high-performance Android NFC application for reading, writing, and analyzing MIFARE Classic RFID tags, completely rewritten in **Kotlin** with a **KernelSU-inspired Material 3 UI 2.0**.

> **Note**: This project is an independent modernization fork of the original open-source [MifareClassicTool](https://github.com/ikarus23/MifareClassicTool) created by Gerhard Klostermeier. It is licensed under the **GNU General Public License v3.0 (GPLv3)**.

Read this information in other languages:
* [简体中文说明 (Recommended / 完整中文文档)](README.zh-CN.md)
* [English (This document)](README.md)
* [正體中文](README.zh-TW.md)

---

## Modernization Highlights

- **100% Kotlin Rebuild**: All 26 core classes, activities, and cryptographic/diff algorithms migrated to idiomatic Kotlin 2.2.10 with full null-safety and zero Java compilation steps.
- **KernelSU Style UI 2.0**: Sleek technical tool visual system with deep slate dark mode (`#0D1117`), elevated surface containers (`#161B22`), 1dp subtle outlines (`#30363D`), and full VectorDrawable icons.
- **Enhanced Internationalization**: Complete bi-directional synchronization with `AppCompatDelegate.getApplicationLocales()`, full Chinese offline help documentation with dark-mode responsive CSS, and "Follow System" default.
- **Protected Safety Architecture**: Production `DumpEditor` retains its multi-threaded native view core; `WriteTag` write state machine, retry sequences, and key fallback logic remain 100% frozen and protected.
- **Zero Heavy Dependencies**: Built entirely on standard AndroidX and Jetpack Compose Material 3 without third-party UI framework bloat, preserving `minSdk 21` support.

---

## Core Features

- Read MIFARE Classic 1K and 4K tags using dictionary-attack mapping.
- Save, edit, share, and compare tag dumps.
- Write data to tags (block-wise, full dump, factory format, value block increment/decrement).
- Write to manufacturer block (block 0) of compatible magic tags (Gen2 / CUID).
- Comprehensive toolbox: Tag Info, BCC Calculator, UID Clone, Value Block Decoder/Encoder, Access Condition Matrix Table, Data Format Converter, and UID Scan Logs.
- Offline in-app documentation in English and Simplified Chinese.

---

## Installation & Requirements

- **Operating System**: Android 5.0 (Lollipop, API 21) or higher.
- **Hardware Requirement**: An Android device with an NFC controller that supports MIFARE Classic (typically NXP NFC chips).  
  *Important*: NFC support in Android does **not** guarantee MIFARE Classic support (e.g. certain Broadcom NFC controllers cannot read physical MIFARE Classic cards). See the upstream [Incompatible Devices List](https://github.com/ikarus23/MifareClassicTool/blob/master/INCOMPATIBLE_DEVICES.md).

---

## Authors & License

- **Original Project**: [ikarus23/MifareClassicTool](https://github.com/ikarus23/MifareClassicTool) by Gerhard Klostermeier & SySS GmbH / Aalen University (2012/2013-2024).
- **Modernization Fork**: [ljj13/MCT-Kotlin-Rebuild](https://github.com/ljj13/MCT-Kotlin-Rebuild).
- **License**: [GNU General Public License v3.0 (GPLv3)](LICENSE.txt).
