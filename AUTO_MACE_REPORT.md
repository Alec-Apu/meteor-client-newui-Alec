# Auto Mace 26.2 / 汉化发行版 1.0.5

## 实现与文件

- 新增 `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/AutoMace.java`：独立战斗模块、目标选择、提前切换、冷却与距离/遮挡判断、同步攻击、服务器确认和调试显示。
- 新增 `src/main/java/meteordevelopment/meteorclient/systems/modules/combat/automace/MaceAttackPolicy.java`：可测试的状态门禁、CC0 快捷栏搜索和选择位置所有权。
- 新增 `src/test-auto-mace/java/meteordevelopment/meteorclient/systems/modules/combat/automace/MaceAttackPolicyTest.java`：实际运行的 28 项回归测试。
- 修改 `systems/modules/Modules.java`：在 Combat 中仅注册一次。
- 修改 `build.gradle.kts`：默认发行版本 1.0.5，独立测试源集与 JavaExec 测试任务，check 强制执行；无新增游戏运行依赖。
- 修改 `assets/meteor-client/lang/en_us.json`、`zh_cn.json`：模块、设置、描述、状态与提示，两种语言各 4,362 条。
- 新增 `src/main/resources/licenses/AutoMaceswap-CC0.txt`、`AutoMaceswap-NOTICE.txt`、`AUTO_MACE_SOURCES.md`；更新 README、CHANGELOG、RELEASE。

仅采用 CC0 上游快捷栏搜索逻辑，攻击适配层独立编写。候选项目许可证及参考仓库配置不一致的问题见 AUTO_MACE_SOURCES.md；未复制受限项目，未把示例仓库冒充成熟移植。

## 设置与行为

| 配置 ID | 默认值 | 用途 |
|---|---|---|
| target-players | true | 玩家目标；永久过滤好友、队友、创造/旁观玩家 |
| target-mobs | false | 生物目标；永久过滤有主动物 |
| excluded-names | 空 | 精确名称排除 |
| target-range | 8 | 搜索距离；尽早准备重锤 |
| aim-range | 360 | 水平视角范围，度 |
| attack-range | 3 | 眼睛到碰撞箱的距离，另受原版攻击范围限制 |
| target-priority | LowestDistance | 复用目标排序，包括最低生命值 |
| min-fall-distance | 1.5 | 下落距离必须严格超过阈值与余量之和 |
| safety-margin | 0.1 | 额外下落距离 |
| health-threshold | 6 | 最低生命值，单位生命值点 |
| auto-switch | true | 目标在附近时提前装备快捷栏重锤 |
| switch-back | true | 尝试、取消、关闭后恢复原选择位置；尊重后续手动切换 |
| debug-info | false | 屏幕显示目标、真实下落状态、距离、持有物品、重锤位置和状态 |

仅在非触地、垂直速度小于 -0.01、无缓降/鞘翅飞行/水体/岩浆/攀爬/骑乘、原版 canSmashAttack 成立、完整攻击冷却恢复、当前与下一 tick 估计范围均有效且碰撞射线无遮挡时尝试。每次下落最多一次；只有实际触地才重新允许下一次攻击。

选中位置通过当前 26.2 的 setSelectedSlot 与 meteor$syncSelected 同步。独立保存原位置，不使用 InvUtils.previousSlot。旋转发送、攻击、旋转恢复在同一同步调用中完成，不改变本地镜头，不模拟按键，不留下轮转回调。模块关闭/退出世界由现有生命周期清理状态并失效化已排队的伤害事件回调。

KillAura、AutoWeapon、CrystalAura、共享旋转器占用、使用物品、挖掘和菜单打开时暂停。没有重锤则不切换；仅背包中有重锤时提供移到快捷栏的提示，不自动搬动物品。

攻击请求仅记为 Attempted；对应目标及本地攻击者的服务器 MACE_SMASH 伤害事件才记为 Confirmed。等待最多 12 个客户端 tick，无确认记为 Unconfirmed，不据此断言一定没有击中或已经命中。

## 实际构建与检查

基线 1.0.4：原项目 build 成功（up-to-date），日志 D:/codex-project/work/automace-port-baseline.log。

使用 JDK `D:/codex-project/work/toolchains/jdk-25.0.4.1+1`，GRADLE_USER_HOME `D:/codex-project/work/gradle-home`：

```powershell
./gradlew.bat clean build -I ../work/local-modrinth-262.gradle '-Pbuild_number=zh-cn-1.0.5' --offline --console=plain
```

最终执行结果：BUILD SUCCESSFUL in 10s。autoMacePolicyTest 28/28 通过；标准 test 无 JUnit 源码（NO-SOURCE），独立测试任务实际运行，未关闭 failOnNoDiscoveredTests 或跳过断言。首次编译水体接口错误已用真实 API 修复；第一次完整构建中的测试发现错误已通过正确独立源集接入修复。保留上游 j2objc 注解和 discord-ipc 非 semver 警告，不影响最终成功。

日志：D:/codex-project/work/automace-port-build.log。

静态汉化检查：旧配置键、命令语法、Mixin 目标、语言占位符一致，0 项错误。产物检查：包含模块/策略类、CC0 全文和来源说明；不包含测试类或临时启动辅助模组；Fabric ID meteor-client，version 26.2-zh-cn-1.0.5，Minecraft ~26.2、Java >=25、Loader >=0.19.3，JAR 5,080,308 字节。检测记录 D:/codex-project/work/automace-port-validation.json。

## 自动化测试（全部通过）

1. Inactive policy sends no attack — PASS
2. Vanilla 1.5 boundary is insufficient — PASS
3. Safety margin boundary is insufficient — PASS
4. Non-finite fall distance rejected — PASS
5. Missing mace prevents attack — PASS
6. Dead or disappeared target cancels attack — PASS
7. Out-of-range target cancels attack — PASS
8. Blocked hitbox cancels attack — PASS
9. Slow falling / elytra / water prevents smash attempt — PASS
10. Grounded or ascending player cannot attack — PASS
11. Low health safety gate prevents attack — PASS
12. Switch sync and attack cooldown must finish — PASS
13. Competing combat module pauses attacks — PASS
14. Valid cooled vanilla fall permits one attempt — PASS
15. 200 subsequent ticks cannot repeat attack in one fall — PASS
16. Target loss and reacquisition cannot rearm same fall — PASS
17. Landing rearms next fall — PASS
18. Disable clears state and blocks later actions — PASS
19. Module-active guard invalidates late work — PASS
20. No mace produces no slot — PASS
21. CC0 selector finds hotbar mace — PASS
22. Hotbar selector cannot select backpack as hotbar — PASS
23. Missing mace cannot acquire weapon switch — PASS
24. Owned switch restores original slot — PASS
25. Switch Back disabled leaves equipped slot — PASS
26. Manual selection is respected — PASS
27. Changed hotbar contents cancel restoration — PASS
28. Restoration clears lease and cannot run twice — PASS

## 游戏启动状态

用户保存退出游戏后，已在 E:/pcl/.minecraft/versions/Alec-26.2 实际启动生产 JAR，Fabric Loader 0.19.5。临时辅助模组验证：模块注册、13 项设置 NBT 保存/恢复、中英文标题与描述、无世界关闭清理全部 PASS；设置菜单实际渲染 PASS，截图已人工检查。测试客户端自动正常退出，未打开任何存档；临时辅助模组已从 mods 清理，不进入交付 JAR。

启动日志：D:/codex-project/work/automace-port-client.log。截图：D:/codex-project/work/automace-port-menu.png。离线测试账号导致 Realms 授权失败，以及实例其他模组的缺失可选 Mixin/翻译警告，不影响菜单测试；未更改用户登录凭据。

新实现尚未完成真实服务器猛击测试。以前已移除的 1.0.3 的测试结果不作为本次验证结果。

## 限制与可重复验证流程

没有自动风弹、起跳、追击或连击；可手动使用风弹。不能预测服务器实际命中、不能防止摔落伤害，低生命值门禁不是防摔功能。客户端每次下落最多一次，短落程或临时换武器可能来不及恢复完整冷却而不攻击；应提前持锤。预测仅看下一 tick 位置，不保证目标随后不移动。不对真实服务器成功率或反作弊作承诺。

1. 使用用户指定 Alec-26.2 实例加载新版；右 Shift → 战斗 → 自动重锤，打开调试信息。暂时关闭 KillAura、AutoWeapon、CrystalAura，保留图腾、补给等保护模块。
2. 在允许测试的安全平地旁使用专用测试存档/测试服务器，装备完整护甲并准备落地水池。选择非好友/队友的服务端假人（例如 Carpet 假人）或允许攻击的生物；本地 Meteor 假人不作为服务器验证对象。
3. 重锤放快捷栏。保持目标在搜索范围内，等重锤完全冷却；手动自然下落或风弹起跳，不使用 TP。
4. 先验证低于 1.6 格时不攻击，再用可控高度超过 1.6 格的下落进入目标碰撞箱攻击范围。查看 Attempted 与服务器 Confirmed 的区别，并对照服务端伤害记录，不能把挥手动画当作猛击。
5. 分别让目标死亡/离开、加实体与方块遮挡、移走重锤、手动换槽、给予缓降、关闭模块，检查停止与恢复行为。每次失败安全落地后再开始，避免在高空依赖自动攻击防摔。
6. 测试结束关闭模块，确认不再攻击、选择位置和镜头恢复正常。默认不开自动起跳，风弹不足不会由本模块触发错误使用。

## 交付

JAR：D:/codex-project/outputs/meteor-client-26.2-zh-cn-1.0.5/meteor-client-26.2-zh-cn-1.0.5.jar
源码：同目录 meteor-client-26.2-zh-cn-1.0.5-source.zip
每次更新前后源码及 JAR 均保存至 D:/codex-project/update-backups。不上传 GitHub，不更新 26.3。
