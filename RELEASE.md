# Minecraft 26.2 — 1.0.5

安装包：meteor-client-26.2-zh-cn-1.0.5.jar
源码包：meteor-client-26.2-zh-cn-1.0.5-source.zip
交付目录：D:/codex-project/outputs/meteor-client-26.2-zh-cn-1.0.5

新增 Auto Mace 核心适配模块，采用 CC0 快捷栏搜索逻辑，攻击适配层独立实现。详细来源、测试和限制分别见 AUTO_MACE_SOURCES.md、AUTO_MACE_REPORT.md。不是恢复此前已移除的 1.0.3 自研模块。

右 Shift → 战斗 → 自动重锤。重锤须放在快捷栏；准备足够的冷却后通过自然下落或手动风弹起跳进行测试。关闭 KillAura、AutoWeapon、CrystalAura，确保目标非好友/队友。默认攻击玩家，不攻击生物；生物需打开对应选项。调试信息默认关闭。

右 Shift → 配置 → 视觉 → Meteor 语言，可独立选择简体中文或 English。

需要 Minecraft 26.2、Java 25 和兼容 Fabric Loader（最低 0.19.3）。测试与安装实例：E:/pcl/.minecraft/versions/Alec-26.2。只保留一份 Meteor 客户端 JAR。用户存档、账号和设置保留；测试不自动打开用户存档。

本次不更新 26.3，也不上传 GitHub。每次修改前后源码与 JAR 均备份到 D:/codex-project/update-backups。
