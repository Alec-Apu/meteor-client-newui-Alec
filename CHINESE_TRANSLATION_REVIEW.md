# Meteor Client 26.2 — 1.0.2 检查报告

日期：2026-10-10。维护者：Alec；界面品牌 Alec-apu。本次只更新 26.2，未上传 GitHub。

## 新功能

右 Shift → 配置 → 视觉 → Meteor 语言。可独立选择简体中文或 English，默认简体中文；立即刷新并保存。英文界面的路径为 Config → Visual → Meteor Language。

切换刷新模块名称和说明、设置名称和说明、枚举显示、命令描述与 HUD 元数据。已打开的 Meteor WidgetScreen 在下一帧重建界面；窗口标题同步刷新。描述保存英文源文本，使用弱引用监听刷新，避免保留已关闭窗口或删除的 HUD 元素。

资源只用于显示；现有模块 ID、命令语法、枚举存储和玩家自定义文字不变。新增配置键 meteor-language 使用 zh_cn/en_us 保存。中英文模块搜索在两种显示语言下均可使用。已有自定义 HUD 文本及已发送聊天消息不会被重写。

## 验证

- Gradle build 成功；Java 25、Minecraft 26.2、Fabric Loader 0.19.3。
- 在独立发布 JAR 实例中完成两个进程测试，启动类路径排除开发源码与外置 Meteor 库，依赖由成品 JAR 提供。
- 英文检查覆盖 167 个模块、1,406 个模块设置及 HUD 元数据；名称、说明、HUD 预设未发现残留中文。
- 中文 → 英文 → 中文 → NBT 恢复，模块、设置说明和枚举同步变化；切换后新建设置可正确显示，并在后续切换中刷新。
- 已打开配置界面的即时刷新测试通过，检查中英文配置和模块截图。
- 独立重启恢复英文选择，再切回中文成功；Minecraft 的语言值不变。
- 中英文搜索、模块 ID、自定义文字、缓存命令错误的英文反馈均通过。
- 静态检查：保留 1,664 个原设置 ID、147 个命令语法定义、31 个持久化键访问和 34 个 Mixin 目标；唯一新增设置键为 meteor-language。
- 中英文各 4,325 项资源，键、格式标记和 Starscript 表达式一致。
- JAR 元数据版本 26.2-zh-cn-1.0.2，Minecraft 依赖 ~26.2，内嵌 javassist；不含测试入口。

## 限制

第三方模组、服务器消息及在线服务返回的文本可能保留原文。未测试真实账号或线上服务；离线测试日志中的 Realms 授权错误不属于语言切换失败。内部版本徽标仍受上游三段版本解析限制，Fabric 元数据版本正确。

## 产物

- meteor-client-26.2-zh-cn-1.0.2.jar
- SHA-256：2be16fcfa2356be901afcc9afe08170e164189834ba86604ea270da149f5a637
- meteor-client-26.2-zh-cn-1.0.2-source.zip
- 交付目录：D:/codex-project/outputs/meteor-client-26.2-zh-cn-1.0.2
- 更新前后源码与安装包备份：D:/codex-project/update-backups

## 相比 1.0.1 修改的源码和文件

- `CHANGELOG.md`
- `CHINESE_TRANSLATION_REVIEW.md`
- `README.md`
- `RELEASE.md`
- `build.gradle.kts`
- `src/main/java/meteordevelopment/meteorclient/commands/Command.java`
- `src/main/java/meteordevelopment/meteorclient/gui/WidgetScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/ModulesScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/widgets/containers/WWindow.java`
- `src/main/java/meteordevelopment/meteorclient/settings/Setting.java`
- `src/main/java/meteordevelopment/meteorclient/systems/config/Config.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/HudElementInfo.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/Module.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/Modules.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/misc/InventoryTweaks.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/elytrafly/ElytraFly.java`
- `src/main/java/meteordevelopment/meteorclient/utils/misc/CommandErrors.java`
- `src/main/java/meteordevelopment/meteorclient/utils/misc/Localization.java`
- `src/main/resources/assets/meteor-client/lang/en_us.json`
- `src/main/resources/assets/meteor-client/lang/zh_cn.json`
