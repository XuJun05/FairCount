> [!WARNING]
> **⚠️ Important Notice and Disclaimer Regarding the Beta Version**
> Please be aware that this mod is currently in its beta version, meaning there may be countless bugs and unexpected issues.
> The developer takes absolutely no responsibility for any damages or problems caused by using this mod, including but not limited to server crashes, data corruption, or unintended player kicks. If you plan to use this on a production (public) server, please thoroughly test it beforehand and use it at your own risk.

# 🛡️ FairCount

**FairCount** is a Fabric mod designed to strictly manage the mods installed by players joining your server, enforcing a fair gameplay environment (it must be installed on both the server and the client).

## ✨ Key Features

* **Strict Mod Whitelist**: Automatically detects and kicks players using unapproved mods that are not on the server's whitelist.
* **Complete Client Monitoring**: When a client joins the server, it sends information about all loaded mods (including standalone JARs and nested/JiJ mods) to the server for verification.
* **Blocks Uninstalled Players**: Clients who do not have this mod installed cannot join the server, preventing any loopholes in the monitoring system.
* **Automatic Fabric API Allowance**: Mod IDs starting with `fabric-` are automatically allowed, saving you the hassle of writing all the minor API-related mods into the configuration file.

## ⚙️ Setup and Usage

1. Install FairCount in the `mods` folder of **both the server and the client**.
2. Start the server once to automatically generate the `faircount_whitelist.txt` file inside the `config` folder.
3. In this file, write the IDs of the mods you want to allow players to use, with one ID per line (e.g., `sodium`, `jei`).
4. Any player attempting to join with unauthorized mods (such as cheat mods or rule-breaking utility mods) will be immediately disconnected, along with a message indicating **which mod caused the kick**.

## 🎯 Recommended For

* Servers that want to absolutely maintain the fairness of a vanilla environment.
* Server administrators looking to completely eliminate unfair advantages provided by specific client-side mods in PvP, events, and other competitive scenarios.

---
**Requirements**: Fabric Loader & Fabric API
