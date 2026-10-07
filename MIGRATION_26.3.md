# Minecraft 26.3 适配说明

基于 NoSetViolin/meteor-client-newui-xh 的 26.2 源码适配，保留原有自定义界面。

维护者：[Alec（Alec-Apu）](https://github.com/Alec-Apu)。上游 Meteor Client 版权与 GPL-3.0 许可证保留。

## 安装

使用 Minecraft 26.3、Java 25 和 Fabric Loader 0.19.3 或更新版本。将 meteor-client-26.3-local.jar 放入该游戏实例的 mods 目录，移除旧版 Meteor JAR。该 JAR 已包含项目需要的 Fabric API 子模块及 tinyfd 文件选择器依赖。

## 主要改动

- 更新游戏与可选模组的编译依赖。
- 迁移到 RenderPearl 渲染 API，包括管线编译、GPU uniform 存储、命令提交、裁剪和 GLSL 输入输出声明。
- 迁移到 SDL 输入 API，并转换旧配置中的 GLFW 键盘、鼠标与修饰键编号。新保存的快捷键配置带有 inputFormat=sdl 标记。GLFW F25 没有对应 SDL 按键，会取消绑定。
- 适配第一人称手部状态与渲染、实体渲染、粒子、物品模型、数据包和 Authlib 服务发现的变化。
- 修复游戏初始化期间首帧渲染访问尚未初始化的 Meteor 状态，以及界面动画中零面积裁剪导致的异常。

## 验证结果

- Java 25 下完整 Gradle build 成功，包含访问声明校验。
- 客户端启动、资源加载及 Meteor 着色器预编译成功。
- 运行时强制加载并检查 Mixin 目标；静态检查覆盖 211 个目标。静态检查不替代每个模块的运行验证。
- 自动打开 Meteor 界面并连续运行，随后进入隔离的单人测试世界。
- 单人世界运行 200 个游戏刻，绘制 576 帧自定义线面及深度几何，无崩溃；正常退出。
- 验证旧 Ctrl+A 配置转换、左右 Ctrl 匹配及新格式序列化往返。
- 打包前已移除临时测试入口，发布 JAR 不包含测试类。

上述渲染测试使用 Windows 11、AMD RX 6800 XT、OpenGL。没有逐项测试全部模块、多人服务器、Vulkan 或第三方模组组合。

## 已知限制

- TheAltening 登录暂不可用：26.3 Authlib 删除了旧登录服务接口。目前会明确记录不可用并返回失败；不影响其他账号类型的代码迁移，但真实在线登录尚未测试。
- 26.3 删除 ServerboundSwingPacket。旧的仅发送挥手包行为不再发送该包；本地挥手仍使用新的 SwingAnimation API。Criticals 延迟逻辑只等待攻击包。
- Sodium、Iris、Lithium、Mod Menu、ViaFabricPlus 的编译接口已适配，但未安装这些可选模组做组合启动测试。Sodium 编译版本为 0.9.3-alpha.1。
- Baritone 编译依赖仍为仓库现有的 26.1-SNAPSHOT；测试未安装 Baritone，不保证该版本可以用于 26.3。
- 测试账号为 Fabric 开发账号，因此 Realms 授权失败属于开发环境限制。

## 从源码构建

设置 JAVA_HOME 为 Java 25，执行 gradlew.bat build（Windows）或 ./gradlew build（其他系统）。生成文件位于 build/libs。首次构建需要联网下载依赖。

许可证沿用上游 GPL-3.0，详见 LICENSE。
