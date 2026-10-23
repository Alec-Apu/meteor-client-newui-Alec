# 本地化资源来源

本汉化版基于当前 Minecraft 26.3 分支，显示资源为简体中文 zh_cn。内部模块名、设置键、枚举值、命令语法和配置序列化标识保持原样。

部分基础译文参考下列公开项目，并经占位符检查、术语统一与 26.3 补译。原项目的许可证和贡献者权利保留：

- wzmwayne/meteor-client-auto_cn：scripts/translations.json，GPL-3.0，与本项目 GPL-3.0 许可证兼容。https://github.com/wzmwayne/meteor-client-auto_cn
- QHX-like-play-MC/Meteor-CN-Reborn：src/main/resources/assets/yalu/lang/zh_cn.json，CC0-1.0。https://github.com/QHX-like-play-MC/Meteor-CN-Reborn

没有复制这些项目的模块逻辑或 Mixin。中文译文独立置于 assets/meteor-client/lang/zh_cn.json，英文源文与稳定新增语言键置于 en_us.json。已有快捷键语言键保持不变。汉化版默认显示中文，即使 Minecraft 本身使用英文；资源缺失时回退英文以避免影响功能。
