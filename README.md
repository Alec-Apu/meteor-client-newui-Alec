## Minecraft 26.2 简体中文汉化版

当前版本：1.0.6。右 Shift 打开界面 → 配置 → 视觉 → **Meteor 语言**，选择「简体中文」或「English」，立即刷新并保存；与 Minecraft 语言独立。

维护者：Alec。此版由原始 26.2 源码适配汉化，保留上游版权与 GPL-3.0 许可证。

- 环境：Minecraft 26.2、Java 25、Fabric Loader 0.19.3 或更新兼容版本。
- 安装：将 `meteor-client-26.2-zh-cn-1.0.6.jar` 放入 **26.2 实例**的 `mods` 文件夹，替换该实例已有的 Meteor JAR；右 Shift 打开界面。
- 本包只适用于 26.2，请勿放入 26.3 实例。
- 构建：使用 JDK 25 执行 `./gradlew build -Pbuild_number=zh-cn-1.0.6`；输出位于 `build/libs`。
- 汉化覆盖模块、设置、描述、界面、HUD、命令帮助与反馈；命令语法、配置键和自定义文字保留原值。
- 新增指令本：右 Shift → 指令本，保存指令名称、备注、分类和适用范围，可直接执行、填入聊天框或复制；详情见 `COMMAND_BOOK.md`。
- Auto Mace 核心版：战斗分类 → 自动重锤。先将重锤放入快捷栏，默认不自动起跳。详情见 `AUTO_MACE_REPORT.md` 和 `AUTO_MACE_SOURCES.md`。
- 本地测试结果及逐文件变更见 `CHINESE_TRANSLATION_REVIEW.md`；尚未上传 GitHub。


<p align="center">
<img src="https://meteorclient.com/icon.png" alt="meteor-client-logo" width="15%"/>
</p>

<h1 align="center">Meteor</h1>
<p align="center">A Minecraft Fabric Utility Mod for anarchy servers.</p>

<div align="center">
    <a href="https://discord.gg/bBGQZvd"><img src="https://img.shields.io/discord/689197705683140636?logo=discord" alt="Discord"/></a>
    <br>
    <img src="https://img.shields.io/github/last-commit/MeteorDevelopment/meteor-client" alt="GitHub last commit"/>
    <img src="https://img.shields.io/github/commit-activity/w/MeteorDevelopment/meteor-client" alt="GitHub commit activity"/>
    <img src="https://img.shields.io/github/contributors/MeteorDevelopment/meteor-client" alt="GitHub contributors"/>
    <br>
    <img src="https://img.shields.io/github/languages/code-size/MeteorDevelopment/meteor-client" alt="GitHub code size in bytes"/>
    <img src="https://img.shields.io/endpoint?url=https://ghloc.vercel.app/api/MeteorDevelopment/meteor-client/badge?filter=.java$&label=lines%20of%20code&color=blue" alt="GitHub lines of code"/>
</div>

## Usage

### Building
- Clone this repository
- Run `./gradlew build`

### Installation
Follow the [guide](https://meteorclient.com/faq/installation) on the wiki.

## Contributions
We will review and help with all reasonable pull requests as long as the guidelines below are met.

- The license header must be applied to all java source code files.
- IDE or system-related files should be added to the `.gitignore`, never committed in pull requests.
- In general, check existing code to make sure your code matches relatively close to the code already in the project.
- Favour readability over compactness.
- If you need help, check out the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) for a reference.

## Bugs and Suggestions
Bug reports and suggestions should be made in this repo's [issue tracker](https://github.com/MeteorDevelopment/meteor-client/issues) using the templates provided.  
Please provide as much information as you can to best help us understand your issue and give a better chance of it being resolved.

## Donations
All of our work is completely free and non-profit (donations pay only for hosting costs), therefore we are very grateful for all donations made to support us in running our community.  
Donations can be made via our [website](https://meteorclient.com/donate) and the minimum amount to get donor benefits is €5.  
You will be rewarded with a role on our Discord server and a customisable in-game cape.  
⚠️ _Make sure to create a Meteor account and link your Discord and Minecraft accounts to fully experience your rewards._ ⚠️

## Credits
[Cabaletta](https://github.com/cabaletta) and [WagYourTail](https://github.com/wagyourtail) for [Baritone](https://github.com/cabaletta/baritone)  
The [Fabric Team](https://github.com/FabricMC) for [Fabric](https://github.com/FabricMC/fabric-loader) and [Yarn](https://github.com/FabricMC/yarn)

## Licensing
This project is licensed under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html). 

If you use **ANY** code from the source:
- You must disclose the source code of your modified work and the source code you took from this project. This means you are not allowed to use code from this project (even partially) in a closed-source and/or obfuscated application.
- You must state clearly and obviously to all end users that you are using code from this project.
- Your application must also be licensed under the same license.

*If you have any other questions, check our [FAQ](https://meteorclient.com/faq) or ask in our [Discord](https://meteorclient.com/discord) server.*
