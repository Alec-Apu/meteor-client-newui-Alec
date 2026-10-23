# Auto Mace — 26.2-zh-cn-1.0.3

维护者：Alec。此功能加入现有 Meteor 26.2 汉化项目，不更改 Fabric ID、原模块或上游许可。

## 使用

在 Alec-26.2 实例启动，右 Shift → 战斗（Combat）→ 自动重锤（Auto Mace）。左键开关，右键打开设置。默认模块关闭、攻击玩家开启、攻击生物关闭、风弹和连击关闭。先在单人新测试世界或自有测试服开启“攻击生物”和“调试信息”。重锤必须在快捷栏，不从背包搬运物品。

原有独立中英文切换继续有效；模块 ID `auto-mace`、设置 ID 和用户输入名称不翻译。不要同时放入两份 Meteor 客户端 JAR。

## 实现和设置

- 目标：复用 TargetUtils、SortPriority、Friends 和计分板队伍。按目标搜索距离、完整视角范围、玩家/生物开关、好友/队友开关、排除名称/实体过滤。排除死亡、已移除、创造/旁观玩家和已驯服生物。优先级包含最近和最低生命值。
- 命中判定：从眼睛到实际碰撞箱计算距离，同时受设置上限、游戏交互范围和武器范围约束；检查方块、其他实体遮挡和下一刻运动预测。只在真实下降、原版重锤判定有效、下落超过最低值加余量、攻击冷却完成时尝试。默认最低 1.5 格、余量 0.15 格、最大攻击距离 3 格。
- 换槽：自动切到快捷栏重锤，发送原框架的槽位同步，默认等待 2 个完整游戏刻。这个等待不等于服务器确认。独立记录原槽，避免改动 InvUtils 的共享 previousSlot。手动换槽或保护模块换槽后不抢回选择。开关“切回原槽”控制尝试后恢复；停用时始终释放仍属于本模块的临时槽位。
- 安全：最低伤害收益默认 4 点，最低生命值加吸收默认 8 点，保留生命默认 2 点。下方 48 格脚点探测无法确认地面，或预计裸摔落伤害可能耗尽保留生命时暂停；不把护甲或图腾当作保证。缓降、鞘翅、流体、使用物品、打开菜单时暂停。不得把“停止攻击”理解成已消除原本的摔落风险。
- 冲突：KillAura、AutoWeapon、CrystalAura、BedAura、AnchorAura、Criticals、AutoClicker、NoFall、共享旋转操作、手动攻击或服务器停顿时暂停。不会关闭这些模块，更不会关闭图腾或补给保护。NoFall 开启时 Auto Mace 会显示冲突原因。
- 风弹：可选地面起跳，每次启用只尝试一次，快捷栏缺风弹、冷却、空间不足或使用失败时不盲目重复。使用包实际朝下，客户端临时视角通过 finally 恢复；必须观察到实际离地，20 刻仍未离地视失败。
- 连击：独立开关，只有匹配目标、攻击者为本玩家、伤害类型为原版 `mace_smash` 的服务端伤害包，才允许落地后再起跳。不会自动移动或追赶目标。
- 调试：可选屏幕文字显示目标、下降、下落距离、手持物品、重锤槽、当前状态、最近结果和未攻击原因。默认关闭，不刷聊天或日志。

共 22 项原框架设置：`target-players`, `target-mobs`, `target-range`, `aim-range`, `target-priority`, `ignore-friends`, `ignore-teammates`, `excluded-names`, `excluded-entities`, `max-attack-distance`, `min-fall-distance`, `safety-margin`, `auto-switch`, `switch-back`, `switch-delay`, `confirmation-timeout`, `min-damage-gain`, `health-threshold`, `health-reserve`, `use-wind-charges`, `auto-combo`, `debug-info`。

状态机每个空中阶段最多发出一次攻击请求。普通伤害、确认超时、目标丢失或立即复核失败都不算特殊命中。没有按键模拟，没有排队旋转/攻击回调；停用清除待确认目标，迟到的网络回调按启用代数丢弃。服务器视角恢复不覆盖共享旋转系统。

## 实际构建和测试

环境：Minecraft 26.2，官方可读名称；Java 25；Gradle 9.6.1；Loom 1.17.21（目录依赖 1.17-SNAPSHOT）；编译 Fabric Loader 0.19.3、Fabric API 0.154.2+26.2。实际 Alec-26.2 实例使用 Loader 0.19.5。

在 D:/codex-project/meteor-client-26.2-zh-cn 执行（PowerShell）：

```powershell
$env:JAVA_HOME='D:/codex-project/work/toolchains/jdk-25.0.4.1+1'
$env:GRADLE_USER_HOME='D:/codex-project/work/gradle-home'
./gradlew.bat testAutoMace -I ../work/local-modrinth-262.gradle '-Pbuild_number=zh-cn-1.0.3' --offline --console=plain
./gradlew.bat build -I ../work/local-modrinth-262.gradle '-Pbuild_number=zh-cn-1.0.3' --offline --console=plain
```

local-modrinth-262.gradle 复用机器上已有依赖；其他机器有网络时可移除 `-I ...` 和 `--offline`，使用 JDK 25 和标准仓库构建。

| 验证 | 实际结果 |
| --- | --- |
| 修改前原始 build | 成功，11 秒；无基线编译错误 |
| 新模块首次 compileJava | 成功，11 秒 |
| testAutoMace | 20/20 通过，Gradle 命令成功 |
| 完整 build（包括 testAutoMace 和 check） | 成功 |
| 传统 Gradle test | NO-SOURCE；项目原本没有 JUnit 测试，不计作通过 |
| JAR 元数据、模块类、内嵌 javassist、测试类排除 | 通过：Minecraft ~26.2、Java >=25、版本 26.2-zh-cn-1.0.3 |
| 原有本地化配置键、命令和 Mixin 检查 | 通过；两种语言各 4,406 项 |
| 指定 Alec-26.2 实际图形客户端启动 | 通过，主菜单和模块设置屏幕正常渲染 |
| 游戏进程内 22 项设置 NBT 保存/恢复、中英文元数据、注册及无世界生命周期 | 通过，独立临时测试 mod 执行并已移除 |
| 实际单人或服务器重锤/风弹战斗 | 未执行；不能把状态机模拟和主菜单启动当真实命中测试 |

无需下载测试依赖：`src/autoMaceTest` 的可执行 Java 回归测试直接运行生产纯逻辑状态机。它通过 `check` 接入构建，任何断言失败均使构建失败。测试输入模拟观测和服务端包事实，不伪造游戏内命中结果。

| 测试 | 结果 |
| --- | --- |
| 1. 下落距离不足不攻击 | 通过 |
| 2. 无重锤不错误换槽 | 通过 |
| 3. 目标死亡/消失取消待确认攻击 | 通过 |
| 4. 搜索范围/攻击范围分别生效 | 通过 |
| 5. 缓降与过时下落值不触发攻击 | 通过 |
| 6. 致命摔落和未知落点暂停 | 通过 |
| 7. 停用恢复槽位并拒绝后续确认 | 通过 |
| 8. 连续 100 个空中 tick 只尝试一次 | 通过 |
| 9. 换槽等待与原槽恢复 | 通过 |
| 10. 风弹缺失/冷却/失败无循环 | 通过 |
| 11. 错误攻击者/目标及普通伤害不确认重锤 | 通过 |
| 12. 仅特殊命中确认允许连击 | 通过 |
| 13. 不覆盖手动快捷栏选择 | 通过 |
| 14. 冷却、视线、预测及收益门槛 | 通过 |
| 15. 飞行、流体、低血、界面及战斗冲突 | 通过 |
| 16. 风弹必须实际离地，超时不重试 | 通过 |
| 17. 自动换槽/切回开关正确生效 | 通过 |
| 18. 上升和原版判定不满足时不攻击 | 通过 |
| 19. 迟到和取消后的包不确认 | 通过 |
| 20. 原版裸重锤增伤公式边界 | 通过 |

日志位于 D:/codex-project/work/auto-mace-{baseline,compile,tests,build,runtime}.log，界面截图为 auto-mace-menu.png。启动时存在整合包资源包格式/纹理警告，以及离线测试身份的认证 401；此次测试未修改这些资源包或用户账号，客户端仍正常启动并结束。

## 改动文件

- 新增 src/main/java/meteordevelopment/meteorclient/systems/modules/combat/AutoMace.java。
- 新增 src/main/java/meteordevelopment/meteorclient/systems/modules/combat/mace/AutoMaceController.java。
- 新增 src/autoMaceTest/java/meteordevelopment/meteorclient/systems/modules/combat/mace/AutoMaceControllerTest.java。
- 修改 src/main/java/meteordevelopment/meteorclient/systems/modules/Modules.java，仅增加战斗模块注册。
- 修改 build.gradle.kts，默认版本升至 1.0.3，并添加 testAutoMace 和 check 依赖。
- 修改 assets/meteor-client/lang/en_us.json、zh_cn.json，补充新模块中英文文本。
- 更新 README.md、CHANGELOG.md、RELEASE.md，并新增本文档；D:/codex-project/AGENTS.md 更新当前交付版本。

原有未提交汉化变更保留，26.3 不改动。临时测试 mod、辅助 Java 和启动参数只在 D:/codex-project/work，不打进交付 JAR。

## 限制和可重复实战验收

客户端无法保证网络延迟、服务器战斗修改、受击动画或重锤机制与预测一致。地面探测及下一刻运动是保守估计，不能保证目标突然移动后仍能命中，也不能保证防摔。特别是在移动平台、地形边缘、远落点和高延迟环境中，优先停止自动尝试。同步等待并非槽位服务端确认；等待过久也可能错过短落距攻击。风弹/连击需要实战验收后再用于常用世界。

1. 新建单独的超平坦测试世界，或使用获准测试的私人服务器；不要在主存档或公共服首次测试。开启作弊以便复位，关闭敌对生物自然生成。
2. 放置 3–4 格高的固定平台，地面平坦无障碍，在平台旁召唤静止、无 AI 的高血量测试生物；把普通武器放第 1 槽，重锤放第 3 槽。
3. 满血，无缓降、鞘翅或流体。Auto Mace 开启攻击生物和调试，关闭攻击玩家、风弹和连击。保留图腾/补给保护；若已启用其他冲突模块，应查看暂停原因，并由你自行决定是否在测试世界临时停用。
4. 面向目标自然走落平台，不按攻击键。观察换到重锤、等待下落、一次攻击请求、服务端确认及恢复第 1 槽。普通挥手或普通伤害不能显示“服务器已确认重锤特殊命中”。每次回到平台复位位置、生命和目标。
5. 分别测试 1 格落距、目标移出 3 格、实体/方块遮挡、缓降、低血、重锤移出快捷栏、下落中停用模块；不应产生错误攻击或残留槽位/视角操作。
6. 先仅开风弹、不开连击，在宽阔平地测试一次起跳；移除风弹、置于冷却或遮住上方空间，检查不会无限使用。确认基本行为后才开启连击：没有匹配特殊伤害确认时绝不再次起跳；目标离开搜索范围后停止。
7. 私服复测不同延迟，服务端记录真实 damage source、生命和攻击次数，对照 HUD；超时应显示失败而非保证命中。不同服务器需要独立验证，无反作弊绕过承诺。

交付 JAR：D:/codex-project/outputs/meteor-client-26.2-zh-cn-1.0.3/meteor-client-26.2-zh-cn-1.0.3.jar。源码 ZIP 同目录。更新前后源码与 JAR 已按版本存于 D:/codex-project/update-backups，未上传 GitHub。
