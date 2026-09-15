# FairCount

[![Modrinth](https://img.shields.io/badge/Modrinth-FairCount-00AF5C?style=flat&logo=modrinth&logoColor=white)](https://modrinth.com/mod/faircount)
[![CurseForge](https://img.shields.io/badge/CurseForge-FairCount-F16436?style=flat&logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/faircount)
![Fabric](https://img.shields.io/badge/Loader-Fabric-blue?style=flat)
![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2+-brightgreen?style=flat)
![License](https://img.shields.io/badge/License-LGPL--3.0-lightgrey?style=flat)

**FairCount** is a lightweight anti-cheat and integrity verification mod designed for Fabric. It strictly inspects and verifies client-side installed mods and active resource packs, enforcing a fair and regulated gameplay environment on multiplayer servers.

> **Notice**: This mod is currently in active development. Please test thoroughly in a staging environment before deploying to production servers.

---

## Links
* **Modrinth**: [View on Modrinth](https://modrinth.com/mod/faircount)
* **CurseForge**: [View on CurseForge](https://www.curseforge.com/minecraft/mc-mods/faircount)
* **Source Code**: [GitHub](https://github.com/XuJun05/FairCount)
* **Issue Tracker**: [GitHub Issues](https://github.com/XuJun05/FairCount/issues)
* **Community**: [Discord Server](https://discord.gg/faSR5NHecV)

---

## Features

* **Strict Mod Whitelisting**: Automatically validates connecting players' mod lists against a server whitelist. Unauthorized mods result in an immediate disconnect with clear kick reasons.
* **SHA-256 Checksum Verification**: Validates the SHA-256 hash of JAR files in addition to Mod IDs, preventing spoofed Mod IDs or tampered cheat client injection.
* **Resource Pack Inspection & Hashing**: Detects and restricts unauthorized external resource packs (such as X-Ray / ESP packs) with optional SHA-256 hash checking.
* **Deep Nested Mod Detection (JiJ)**: Scans both standalone JAR files and internal Jar-in-Jar dependencies on the client.
* **Mandatory Client Enforcement**: Automatically disconnects players within 3 seconds if they do not have FairCount installed.
* **Automatic Fabric API Pass**: Sub-modules starting with `fabric-` are automatically allowed without manual configuration.
* **OP & Player Bypass List**: Server operators and players registered by UUID can bypass inspection checks.
* **In-Game Hot-Reload Commands**: Manage allowed mods, resource packs, and bypass players directly via commands without restarting the server.

---

## Installation & Requirements

FairCount **must be installed on both the Server and Client**.

1. Requires **Fabric Loader** and **Fabric API**.
2. Place FairCount into the `mods` folder of both the server and client.
3. Start the server once to automatically generate the configuration files under `config/faircount/`.

---

## Commands (Requires OP)

### Mod Management
* `/faircount mod add <mod_id>` - Whitelist a mod ID (all versions allowed).
* `/faircount mod add <mod_id> <sha256>` - Whitelist a mod with a specific SHA-256 checksum.
* `/faircount mod add all` - Whitelist all mods currently loaded by connected clients.
* `/faircount mod remove <mod_id>` - Remove a mod from the whitelist.
* `/faircount mod remove <mod_id> <sha256>` - Remove a specific hash entry.
* `/faircount mod remove all` - Clear all whitelisted mods.
* `/faircount mod list` - List all whitelisted mods and registered hashes.

### Resource Pack Management
* `/faircount pack add <pack_name>` - Whitelist a resource pack by name.
* `/faircount pack add <pack_name> <sha256>` - Whitelist a resource pack with a specific SHA-256 checksum.
* `/faircount pack add all` - Whitelist all resource packs currently loaded by connected clients.
* `/faircount pack remove <pack_name>` - Remove a resource pack from the whitelist.
* `/faircount pack remove <pack_name> <sha256>` - Remove a specific hash entry.
* `/faircount pack remove all` - Clear all whitelisted resource packs.
* `/faircount pack list` - List all allowed resource packs.

### Player Bypass Management
* `/faircount player add <player_name|uuid>` - Add a player to the bypass list.
* `/faircount player remove <player_name|uuid>` - Remove a player from the bypass list.
* `/faircount player list` - List all bypassed players.

---

## Configuration Files (`config/faircount/`)

* `mods.json`: Whitelisted Mod IDs and their allowed SHA-256 hashes.
* `resource_packs.json`: Whitelisted resource pack names and allowed SHA-256 hashes.
* `players.json`: UUID list of players exempted from checks.
