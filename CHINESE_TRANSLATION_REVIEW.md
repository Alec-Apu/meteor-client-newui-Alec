# Meteor Client 26.3 简体中文汉化检查报告

本地分支：`minecraft-26.3-zh-cn`；基线：`bbe52fd`。未提交发布、未推送 GitHub。维护者仍为 Alec，上游版权和许可证保留。

## 环境与实现

Minecraft 26.3；Java 25；Fabric Loader 0.19.3；Fabric API 0.162.0+26.3；Gradle 9.6.1；Fabric Loom 1.17。
采用显示层本地化：UTF-8 `en_us.json` 与 `zh_cn.json` 使用相同资源键，Localization 根据英文源文本查找中文；资源无法加载时回退英文。汉化版默认中文。内部模块名、设置名、Tab 名、主题名 Meteor、命令关键字、配置键、枚举值和玩家输入保持原样。
覆盖主要 ClickGUI、分类、模块及描述、设置及描述、分组、下拉显示、HUD 编辑器与内置模板、命令帮助和反馈、中文命令错误摘要、提示框、聊天、提示与配置界面。未通过全局拦截渲染文本翻译玩家名称、消息或自定义文本。

## 数量与扫描

- 原始及补充候选英文文本：4506 项（包含源码技术字符串）。
- 候选中提供译文：4317 项。
- 中英文资源：各 4,319 项（包含原有 2 个快捷键项；部分项目为保留缩写）。
- 保留候选：189 项，下表逐项说明。该数字不是玩家界面的未汉化句子数。
- 运行时检查 167 个已注册模块、1,406 个模块设置：名称、描述和分组未发现未翻译的英文自然语言句子，仅保留 HUD 缩写。

## 验证结果

- 最终 Gradle build：BUILD SUCCESSFUL；访问声明检查通过。Gradle 项目没有单元测试源（test NO-SOURCE），运行验证由临时游戏内测试完成。
- 实际客户端正常启动，打开模块列表、杀戮光环设置、配置界面、HUD 编辑器，进入隔离单人世界并正常退出。
- 中文和英文 Kill Aura 搜索均找到 kill-aura；旧枚举存储 When True 保存、读取成功；自定义文字往返保存不变；主题 ID Meteor 保持原值。
- help 与 toggle kill-aura 执行成功；未知命令显示中文错误摘要，保留输入上下文及位置。
- 自定义字体模式下中文 HUD 使用 Minecraft Unicode 字体回退，已验证速度、坐标中文正常渲染。GUI 原有中文字体回退正常，未打包新的巨大字体文件。
- 检查 854×480 窗口截图：中文清晰，模块设置自动切换单栏；面板预留顶部标签栏；长 Tooltip 以测量宽度和 Unicode 码点自动换行。长卡片描述使用省略号，完整描述保留在提示中。
- 修改文件的静态对比：1664 个设置 ID、147 处命令语法、32 处配置键访问及 34 处 Mixin method 目标完全一致。
- 中英文键一致；全部格式占位符、Meteor 颜色标记与 Starscript 花括号表达式检查通过。枚举显示翻译仅存在于下拉组件，EnumSetting 存储及解析保持原代码。
- 最终 JAR 检查：版本 26.3-zh-cn、Minecraft 26.3 依赖正确、语言资源有效、两个本地化类存在；临时 SmokeChinese/Smoke263 测试类和调用已移除。

## 明确保留及测试边界

- 原有已保存的英文 HUD 模板属于用户配置，保留原样；新添加或重建内置预设显示中文。Minecraft 方块、生物、原版界面文字跟随 Minecraft 自身语言，建议在游戏中选择简体中文。
- FPS/TPS/HUD、Ctrl/Alt/Shift、品牌、人名、字体家族、协议和编码名称、开发日志及技术字段保留。命令名称和参数语法使用英文，帮助说明使用中文。
- 第三方附加模组、Baritone 的运行时反馈、Starscript 库内部诊断、账号服务返回错误或服务器消息可能仍含英文；本地化未知内容回退原文，避免改变语义。未修改这些库的解析器，也未测试真实账号登录、远程服务或全部模块的服务器行为。
- 实际游戏截图覆盖的界面如上，不能据此承诺所有第三方组合、所有分辨率或每个模块功能均经过人工游玩测试。没有上传 GitHub。
- 继承的核心 VERSION 解析仅支持三段版本，26.3 的内部版本徽标可能显示 0.0.0；实际 Fabric 元数据及产物目标为 26.3。此问题不属于本次汉化逻辑。

## 构建产物

- JAR：`C:/Users/APP15/Documents/Codex/2026-10-07/https-github-com-nosetviolin-meteor-client/outputs/meteor-client-26.3-zh-cn.jar`
- SHA-256：`15bb687a805f4d12ccdbea07ef0bc5190a7966fb4fcf54fdc6818ea77011aa32`
- 源码压缩包：`meteor-client-26.3-zh-cn-source.zip`（包含本地修改与新增文件；不含 Git 历史、构建缓存、测试存档和临时测试入口）。
- 截图、构建日志、静态验证结果：`zh-cn-test-evidence.zip`。

## 实际修改文件

- `CHANGELOG.md`
- `README.md`
- `src/main/java/meteordevelopment/meteorclient/commands/Command.java`
- `src/main/java/meteordevelopment/meteorclient/commands/arguments/CommandArgumentType.java`
- `src/main/java/meteordevelopment/meteorclient/commands/arguments/MacroArgumentType.java`
- `src/main/java/meteordevelopment/meteorclient/commands/arguments/ModuleArgumentType.java`
- `src/main/java/meteordevelopment/meteorclient/commands/arguments/PlayerArgumentType.java`
- `src/main/java/meteordevelopment/meteorclient/commands/arguments/PlayerListEntryArgumentType.java`
- `src/main/java/meteordevelopment/meteorclient/commands/arguments/ProfileArgumentType.java`
- `src/main/java/meteordevelopment/meteorclient/commands/arguments/SettingArgumentType.java`
- `src/main/java/meteordevelopment/meteorclient/commands/arguments/WaypointArgumentType.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/BindsCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/CommandsCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/DamageCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/DisconnectCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/DropCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/EnchantCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/FakePlayerCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/FriendsCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/GiveCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/HelpCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/InputCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/LocateCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/MacroCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/ModulesCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/NameHistoryCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/NbtCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/NotebotCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/PeekCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/ProfilesCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/ReloadCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/ResetCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/SaveMapCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/ServerCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/SpectateCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/SwarmCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/WaspCommand.java`
- `src/main/java/meteordevelopment/meteorclient/commands/commands/WaypointCommand.java`
- `src/main/java/meteordevelopment/meteorclient/gui/DefaultSettingsWidgetFactory.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/CommitsScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/EditBookTitleAndAuthorScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/EditSystemScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/ModuleScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/ModulesScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/NotebotSongsScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/ProxiesImportScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/ProxiesScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/accounts/AccountInfoScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/accounts/AccountsScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/accounts/AddAlteningAccountScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/accounts/AddCrackedAccountScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/accounts/AddMicrosoftAccountScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/accounts/AddSessionAccountScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/settings/BlockDataSettingScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/settings/BlockListSettingScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/settings/BlockSettingScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/settings/ColorSettingScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/settings/EnchantmentListSettingScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/settings/EntityTypeListSettingScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/settings/FontFaceSettingScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/settings/ItemListSettingScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/settings/ItemSettingScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/settings/ModuleListSettingScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/settings/PacketBoolSettingScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/settings/ParticleTypeListSettingScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/settings/PotionSettingScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/settings/ScreenHandlerSettingScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/settings/SoundEventListSettingScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/settings/StatusEffectAmplifierMapSettingScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/settings/StatusEffectListSettingScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/settings/StorageBlockListSettingScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/screens/settings/base/CollectionMapSettingScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/tabs/TabScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/tabs/WindowTabScreen.java`
- `src/main/java/meteordevelopment/meteorclient/gui/tabs/builtin/GuiTab.java`
- `src/main/java/meteordevelopment/meteorclient/gui/tabs/builtin/HudTab.java`
- `src/main/java/meteordevelopment/meteorclient/gui/tabs/builtin/MacrosTab.java`
- `src/main/java/meteordevelopment/meteorclient/gui/tabs/builtin/ProfilesTab.java`
- `src/main/java/meteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme.java`
- `src/main/java/meteordevelopment/meteorclient/gui/themes/meteor/widgets/input/WMeteorDropdown.java`
- `src/main/java/meteordevelopment/meteorclient/gui/widgets/WAccount.java`
- `src/main/java/meteordevelopment/meteorclient/gui/widgets/WTooltip.java`
- `src/main/java/meteordevelopment/meteorclient/gui/widgets/WTopBar.java`
- `src/main/java/meteordevelopment/meteorclient/gui/widgets/input/WBlockPosEdit.java`
- `src/main/java/meteordevelopment/meteorclient/gui/widgets/input/WDropdown.java`
- `src/main/java/meteordevelopment/meteorclient/gui/widgets/input/WTextBox.java`
- `src/main/java/meteordevelopment/meteorclient/gui/widgets/pressable/WConfirmedButton.java`
- `src/main/java/meteordevelopment/meteorclient/mixin/AbstractContainerScreenMixin.java`
- `src/main/java/meteordevelopment/meteorclient/mixin/BookEditScreenMixin.java`
- `src/main/java/meteordevelopment/meteorclient/mixin/BookViewScreenMixin.java`
- `src/main/java/meteordevelopment/meteorclient/mixin/ClientPacketListenerMixin.java`
- `src/main/java/meteordevelopment/meteorclient/mixin/ConnectionMixin.java`
- `src/main/java/meteordevelopment/meteorclient/mixin/DisconnectedScreenMixin.java`
- `src/main/java/meteordevelopment/meteorclient/mixin/JoinMultiplayerScreenMixin.java`
- `src/main/java/meteordevelopment/meteorclient/mixin/ScreenMixin.java`
- `src/main/java/meteordevelopment/meteorclient/settings/Setting.java`
- `src/main/java/meteordevelopment/meteorclient/settings/StringListSetting.java`
- `src/main/java/meteordevelopment/meteorclient/systems/config/Config.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/Hud.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/HudElementInfo.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/HudRenderer.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/elements/ActiveModulesHud.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/elements/ArmorHud.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/elements/CombatHud.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/elements/CompassHud.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/elements/HoleHud.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/elements/InventoryHud.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/elements/ItemHud.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/elements/LagNotifierHud.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/elements/MapHud.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/elements/MeteorTextHud.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/elements/ModuleInfosHud.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/elements/PlayerModelHud.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/elements/PlayerRadarHud.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/elements/PotionTimersHud.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/elements/TextHud.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/elements/keyboard/KeyboardHud.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/screens/HudEditorScreen.java`
- `src/main/java/meteordevelopment/meteorclient/systems/hud/screens/HudElementScreen.java`
- `src/main/java/meteordevelopment/meteorclient/systems/macros/Macro.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/Module.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/Modules.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/AnchorAura.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/AntiAnchor.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/AntiAnvil.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/AntiBed.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/ArrowDodge.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/AttributeSwap.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/AutoAnvil.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/AutoArmor.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/AutoCity.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/AutoEXP.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/AutoLog.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/AutoTotem.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/AutoTrap.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/AutoWeapon.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/AutoWeb.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/BedAura.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/BowAimbot.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/BowSpam.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/Burrow.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/Criticals.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/CrystalAura.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/Hitboxes.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/HoleFiller.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/KillAura.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/Offhand.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/Quiver.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/SelfTrap.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/SelfWeb.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/Surround.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/misc/AntiPacketKick.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/misc/AutoReconnect.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/misc/BetterChat.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/misc/BookBot.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/misc/DiscordPresence.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/misc/InventoryTweaks.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/misc/MessageAura.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/misc/Notebot.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/misc/Notifier.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/misc/PacketCanceller.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/misc/PacketLogger.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/misc/ServerSpoof.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/misc/SoundBlocker.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/misc/Spam.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/misc/swarm/Swarm.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/misc/swarm/SwarmWorker.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/AirJump.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/Anchor.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/AntiVoid.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/AutoJump.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/AutoWalk.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/AutoWasp.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/Blink.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/ElytraBoost.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/EntityControl.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/FastClimb.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/Flight.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/GUIMove.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/HighJump.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/Jesus.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/LongJump.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/NoFall.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/NoSlow.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/Parkour.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/ReverseStep.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/SafeWalk.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/Scaffold.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/Slippy.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/Sneak.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/Spider.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/Sprint.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/Step.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/TridentBoost.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/Velocity.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/elytrafly/ElytraFly.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/movement/speed/Speed.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/AirPlace.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/AntiAFK.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/AntiHunger.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/AutoClicker.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/AutoEat.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/AutoFish.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/AutoGap.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/AutoMend.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/AutoReplenish.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/AutoTool.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/BreakDelay.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/ChestSwap.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/FakePlayer.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/FastUse.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/InstantRebreak.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/MiddleClickExtra.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/Multitask.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/NameProtect.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/NoInteract.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/NoMiningTrace.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/NoStatusEffects.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/PotionSaver.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/Reach.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/Rotation.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/player/SpeedMine.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/BetterTab.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/BetterTooltips.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/BlockSelection.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/Blur.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/BossStack.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/Breadcrumbs.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/BreakIndicators.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/CameraTweaks.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/Chams.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/CityESP.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/ESP.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/EntityOwner.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/FreeLook.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/Freecam.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/Fullbright.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/HandView.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/HoleESP.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/ItemHighlight.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/ItemPhysics.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/LightOverlay.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/LogoutSpots.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/Nametags.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/NoRender.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/PopChams.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/StorageESP.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/TimeChanger.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/Tracers.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/Trail.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/Trajectories.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/TunnelESP.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/VoidESP.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/WallHack.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/WaypointsModule.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/WeatherChanger.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/Xray.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/Zoom.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/blockesp/BlockESP.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/blockesp/ESPBlockDataScreen.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/marker/BaseMarker.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/marker/CuboidMarker.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/marker/Marker.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/render/marker/Sphere2dMarker.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/Ambience.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/AutoBreed.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/AutoBrewer.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/AutoMount.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/AutoNametag.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/AutoShearer.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/AutoSign.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/AutoSmelter.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/Collisions.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/EChestFarmer.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/EndermanLook.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/Excavator.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/Flamethrower.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/HighwayBuilder.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/InfinityMiner.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/LiquidFiller.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/NoGhostBlocks.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/Nuker.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/PacketMine.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/SpawnProofer.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/StashFinder.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/Timer.java`
- `src/main/java/meteordevelopment/meteorclient/systems/modules/world/VeinMiner.java`
- `src/main/java/meteordevelopment/meteorclient/systems/profiles/Profile.java`
- `src/main/java/meteordevelopment/meteorclient/systems/proxies/Proxies.java`
- `src/main/java/meteordevelopment/meteorclient/systems/proxies/Proxy.java`
- `src/main/java/meteordevelopment/meteorclient/systems/waypoints/Waypoint.java`
- `src/main/java/meteordevelopment/meteorclient/utils/Utils.java`
- `src/main/java/meteordevelopment/meteorclient/utils/misc/Keybind.java`
- `src/main/java/meteordevelopment/meteorclient/utils/player/ChatUtils.java`
- `src/main/java/meteordevelopment/meteorclient/utils/player/TitleScreenCredits.java`
- `src/main/java/meteordevelopment/meteorclient/utils/render/prompts/OkPrompt.java`
- `src/main/java/meteordevelopment/meteorclient/utils/render/prompts/Prompt.java`
- `src/main/java/meteordevelopment/meteorclient/utils/render/prompts/YesNoPrompt.java`
- `src/main/resources/assets/meteor-client/lang/en_us.json`
- `src/main/resources/assets/meteor-client/lang/zh_cn.json`
- `src/main/resources/fabric.mod.json`

## 新增文件

- `CHINESE_TRANSLATION.md`
- `CHINESE_TRANSLATION_REVIEW.md`
- `LOCALIZATION_SOURCES.md`
- `src/main/java/meteordevelopment/meteorclient/utils/misc/CommandErrors.java`
- `src/main/java/meteordevelopment/meteorclient/utils/misc/Localization.java`

## 保留英文逐项审查

| 原文 | 保留原因 |
| --- | --- |
|  /   Data:  | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
|   %s: %d | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
|  %d (%s) | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
|  (%s) | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
|  CPS | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
|  PostProcessShader  | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
|  Readable Bytes Length:  | 格式串、开发诊断或技术字段，保留原文 |
|  Z:  | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
|  bytes. | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
|  cannot be represented by  | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
|  has no vertex format binding | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
|  is not in the mixin package | 格式串、开发诊断或技术字段，保留原文 |
|  x | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| %.1f | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| %.2f MB | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| %.2f kB | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| %.2f%% | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| %.4f MB | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| %d  | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| %d '%c': %s | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| %d '%c': %s (from %s) | 格式串、开发诊断或技术字段，保留原文 |
| %d, %d '%c': %s | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| %d, %d '%c': %s (from %s) | 格式串、开发诊断或技术字段，保留原文 |
| %d:%d:%d /  | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| %s %d (%s) | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| %s %dx | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| %s %s | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| %s (%s) | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| %s (from %s) | 格式串、开发诊断或技术字段，保留原文 |
| %s%s(default) | 格式串、开发诊断或技术字段，保留原文 |
| %s.txt | 格式串、开发诊断或技术字段，保留原文 |
| %s[%s%s%s] %s | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| %sgoto %d %d %d | 命令语法、配置键、数据格式或技术标识，保留兼容性 |
| ' not found | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| (%03d)  | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| (%s) | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| (%sm) | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| (highlight)%s | 格式串、开发诊断或技术字段，保留原文 |
| (highlight)%s(default) | 格式串、开发诊断或技术字段，保留原文 |
| (highlight)(underline)%.0f, %.0f, %.0f(default) | 格式串、开发诊断或技术字段，保留原文 |
| , Y:  | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| , Z:  | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| , noteLevel= | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| --- SUMMARY --- | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| -> C2S | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| .swarm join confirm | 命令语法、配置键、数据格式或技术标识，保留兼容性 |
| /execute in %s run tp %d %d %d | 命令语法、配置键、数据格式或技术标识，保留兼容性 |
| /msg  | 命令语法、配置键、数据格式或技术标识，保留兼容性 |
| <- S2C | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| A. | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| A: | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| ANSI | 协议、编码或技术选项名称，保持技术标识 |
| Addon "%s" is too old and cannot be ran. | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Allocator returned capacity  | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Alt +  | 通用修饰键名称，保留键帽标识 |
| AltGr | 通用修饰键名称，保留键帽标识 |
| Altening | 品牌、人名或专有名称，保持原有署名与品牌 |
| Ascii | 协议、编码或技术选项名称，保持技术标识 |
| B: | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| Bearer  | 协议、编码或技术选项名称，保持技术标识 |
| Buffer too large (overflow):  | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Builtin font ' | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Can't read string that is larger than a buffer! Length:  | 格式串、开发诊断或技术字段，保留原文 |
| Channel content too large to read into ByteBuffer | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Cmd +  | 通用修饰键名称，保留键帽标识 |
| Ctrl +  | 通用修饰键名称，保留键帽标识 |
| CustomTextRenderer.begin() called twice | 格式串、开发诊断或技术字段，保留原文 |
| CustomTextRenderer.end() called without calling begin() | 格式串、开发诊断或技术字段，保留原文 |
| CustomTextRenderer.render() called without calling begin() | 格式串、开发诊断或技术字段，保留原文 |
| DataFlowIssue | 格式串、开发诊断或技术字段，保留原文 |
| E | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| Error running @%s task '%s.%s' | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Exception during addon init "%s". | 格式串、开发诊断或技术字段，保留原文 |
| FIELD | 格式串、开发诊断或技术字段，保留原文 |
| FPS: #1{fps} | 性能统计缩写，保留原值 |
| Failed to decode skin image. | 技术格式、输入示例或库诊断，保留原值；不进行全局字符串替换 |
| Failed to initialize CJK fallback font. | 格式串、开发诊断或技术字段，保留原文 |
| Failed to load Meteor shader  | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Failed to load default font:  | 格式串、开发诊断或技术字段，保留原文 |
| Failed to parse version string. | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| File too large to read into ByteBuffer:  | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Final packet counts (total  | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| G: | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| GET | 协议、编码或技术选项名称，保持技术标识 |
| GitHub | 品牌、人名或专有名称，保持原有署名与品牌 |
| HEAD | 协议、编码或技术选项名称，保持技术标识 |
| Hiragino Sans GB | 字体家族名称，保留字体标识 |
| INVOKE | 格式串、开发诊断或技术字段，保留原文 |
| ISO | 协议、编码或技术选项名称，保持技术标识 |
| Index  | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Index %s out of bounds for length %s | 格式串、开发诊断或技术字段，保留原文 |
| Index buffer offset must be aligned to  | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Indices written to MeshBuilder without calling 'ensureCapacity()' first! | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| JetBrains Mono | 字体家族名称，保留字体标识 |
| Length can't be negative! Length:  | 格式串、开发诊断或技术字段，保留原文 |
| Malgun Gothic | 字体家族名称，保留字体标识 |
| Mesh index buffer | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Mesh vertex buffer | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Mesh.begin() called while already building. | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Mesh.end() called while not building. | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Meteor - Blur UBO | 技术格式、输入示例或库诊断，保留原值；不进行全局字符串替换 |
| Meteor - Image UBO | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Meteor - Mesh UBO | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Meteor - Outline UBO | 技术格式、输入示例或库诊断，保留原值；不进行全局字符串替换 |
| Meteor - Post UBO | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Meteor Client | 品牌、人名或专有名称，保持原有署名与品牌 |
| Meteor MeshRenderer | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Meteor on Crack! | 品牌、人名或专有名称，保持原有署名与品牌 |
| Method "%s" using Init annotations from non-static context | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Microsoft | 品牌、人名或专有名称，保持原有署名与品牌 |
| Microsoft YaHei | 字体家族名称，保留字体标识 |
| MineGame | 品牌、人名或专有名称，保持原有署名与品牌 |
| MineGame159 | 品牌、人名或专有名称，保持原有署名与品牌 |
| Minecraft | 品牌、人名或专有名称，保持原有署名与品牌 |
| Missing Meteor language resources | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Mixin  | 格式串、开发诊断或技术字段，保留原文 |
| Modules.addModule - Module's category was not registered. | 技术格式、输入示例或库诊断，保留原值；不进行全局字符串替换 |
| Modules.registerCategory - Cannot register category outside of onRegisterCategories callback. | 技术格式、输入示例或库诊断，保留原值；不进行全局字符串替换 |
| Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36 | 技术格式、输入示例或库诊断，保留原值；不进行全局字符串替换 |
| N | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| NBT | 命令语法、配置键、数据格式或技术标识，保留兼容性 |
| Noto Sans CJK SC | 字体家族名称，保留字体标识 |
| Noto Sans SC | 字体家族名称，保留字体标识 |
| POST | 协议、编码或技术选项名称，保持技术标识 |
| PaperMC | 品牌、人名或专有名称，保持原有署名与品牌 |
| PingFang SC | 字体家族名称，保留字体标识 |
| Previous instance of MeshRenderer was not ended | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| R: | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| RenderPipeline  | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| S | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| SelectedItem | 命令语法、配置键、数据格式或技术标识，保留兼容性 |
| Shift +  | 通用修饰键名称，保留键帽标识 |
| Snail | 品牌、人名或专有名称，保持原有署名与品牌 |
| Socks4 | 协议、编码或技术选项名称，保持技术标识 |
| Socks5 | 协议、编码或技术选项名称，保持技术标识 |
| Song has already finished loading! | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Song is still loading! | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| TPS | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| TPS: #1{round(server.tps, 1)} | 性能统计缩写，保留原值 |
| TheAltening | 品牌、人名或专有名称，保持原有署名与品牌 |
| Tried to render with a texture with a non-textured Renderer2D | 格式串、开发诊断或技术字段，保留原文 |
| Tw Cen MT | 字体家族名称，保留字体标识 |
| UNIT_1U | 格式串、开发诊断或技术字段，保留原文 |
| UNIT_1_25U | 格式串、开发诊断或技术字段，保留原文 |
| UNIT_1_5U | 格式串、开发诊断或技术字段，保留原文 |
| UNIT_1_75U | 格式串、开发诊断或技术字段，保留原文 |
| UNIT_2U | 格式串、开发诊断或技术字段，保留原文 |
| UNIT_2_25U | 格式串、开发诊断或技术字段，保留原文 |
| UNIT_2_75U | 格式串、开发诊断或技术字段，保留原文 |
| UNIT_6_25U | 格式串、开发诊断或技术字段，保留原文 |
| Unexpected amount of indices written to MeshBuilder. | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Unexpected value:  | 格式串、开发诊断或技术字段，保留原文 |
| Unsupported texture format:  | 格式串、开发诊断或技术字段，保留原文 |
| Utf8 | 协议、编码或技术选项名称，保持技术标识 |
| VanillaTextRenderer.begin() called twice | 格式串、开发诊断或技术字段，保留原文 |
| VanillaTextRenderer.end() called without calling begin() | 格式串、开发诊断或技术字段，保留原文 |
| VanillaTextRenderer.render() called without calling begin() | 格式串、开发诊断或技术字段，保留原文 |
| Version string needs to have 3 numbers. | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| Vertices written to MeshBuilder without calling 'ensureCapacity()' first! | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| W | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| X | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| X+ | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| X+ Z+ | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| X+ Z- | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| X,Z,Chests,Barrels,Shulkers,EnderChests,Furnaces,DispensersDroppers,Hoppers /  | 技术格式、输入示例或库诊断，保留原值；不进行全局字符串替换 |
| X- | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| X- Z+ | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| X- Z- | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| X:  | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| X: %.0f Y: %.0f Z: %.0f | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| XP | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| XiaoHan | 品牌、人名或专有名称，保持原有署名与品牌 |
| Y | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| Z | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| Z+ | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| Z- | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| [^0-9a-f] | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| \b0+\B | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
| currentTheme | 命令语法、配置键、数据格式或技术标识，保留兼容性 |
| custom banner | 开发日志、异常诊断、GPU 标签或源码注解，保持原文 |
| dontShowAgainPrompts | 命令语法、配置键、数据格式或技术标识，保留兼容性 |
| hh:mm:ss, dd/MM/yyyy | 技术格式、输入示例或库诊断，保留原值；不进行全局字符串替换 |
| iterator() not supported by this Iterable, use forEach() instead. | 格式串、开发诊断或技术字段，保留原文 |
| windowConfigs | 命令语法、配置键、数据格式或技术标识，保留兼容性 |
| {"identityToken":"XBL3.0 x= | 技术格式、输入示例或库诊断，保留原值；不进行全局字符串替换 |
| {foo:[aa, bb],bar:15} | 命令语法、配置键、数据格式或技术标识，保留兼容性 |
| {foo:bar} | 命令语法、配置键、数据格式或技术标识，保留兼容性 |
| {foo=bar} | 命令语法、配置键、数据格式或技术标识，保留兼容性 |
| 🍖 %d (💛 %.1f) | 格式占位符、坐标轴、键位或通用缩写，保留原值 |
