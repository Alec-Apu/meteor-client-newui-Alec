# Auto Mace 26.2：来源与移植边界

## 候选项目核查

- https://www.curseforge.com/minecraft/mc-mods/automace ：浏览器页面显示作者 invisapple，项目 1730214，Minecraft 1.21.11，Fabric，许可证 All Rights Reserved。未下载、反编译或复制其源码。
- https://www.curseforge.com/minecraft/mc-mods/auto-mace-pvp ：HTTP 请求返回 403，浏览器 DOM 也未成功取得页面。许可证、源码和依赖尚不能确认，因此未采用该项目代码。
- Ashh1321/AutoMaceCore 与 Hydra-cyber-cell/automace-test1 有 CC0 文件，但已检查的入口/模块是空模板或示例渲染逻辑，不能当成自动重锤实现。

## 实际采用的参考代码

仓库：https://github.com/rajputbabita399-sketch/AutoMaceswap

固定提交：`297fbfa07a615e1c032cc941d4a01d04bbb90f55`

参考文件：`src/main/java/com/rajput/automaceswap/mixin/MaceSwapMixin.java`

文件 Git blob：`c4fad8b1875f3979cc06795bc639065816251f2f`

根 LICENSE 为 CC0 1.0 Universal；README 的 License 部分亦注明 CC0。许可证全文随 JAR 放入 `licenses/AutoMaceswap-CC0.txt`。这里只采用该文件的 0..8 快捷栏顺序搜索逻辑，提取为 `MaceAttackPolicy.findMace`。不复制仓库夹带的 Wurst、备份模块、二进制或其他来源不清的文件。

该参考不能视为可直接运行的 26.2 项目：gradle.properties 写 1.21.1 / Yarn 1.21.1+build.3 / Loader 0.16.10，而 build.gradle 写 Minecraft 26.1 / Java 25 / Loader 0.18.4 / Loom 1.15.5，并存在混合命名 API。没有采用这些依赖配置，也没有声称上游构建通过。

## 适配范围

原参考只搜索并选择重锤，没有可靠的自动攻击、冷却、目标过滤、还原或服务器确认。上述部分为本次独立编写的 Meteor 26.2 适配层，并非完整移植某个成熟 Auto Mace 模组，也不是恢复已移除的 1.0.3 模块。

保留当前项目 Minecraft 26.2、Java 25、Fabric Loader 0.19.3、Fabric API 0.154.2+26.2、Gradle 9.6.1 和官方可读命名。使用真实 `setSelectedSlot`、`meteor$syncSelected`、`MaceItem.canSmashAttack`、`isWithinAttackRange`、`ClipContext` 和 `ClientboundDamageEventPacket` 接口；不引入 Yarn 或新运行依赖。

首次编译发现旧水体检测接口 `isInWaterOrBubble` 已不存在。通过当前 Minecraft JAR 的 javap 检查后替换为 `isInWater` / `isInLava`，未屏蔽编译错误。
