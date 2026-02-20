# AGENT.md

## 目标与范围
本仓库是 `Arknights: Endfield` 同人主题的 NeoForge 模组，面向 `Minecraft 1.21.1`。

- 模组 ID: `arknights_endfield`
- 主要语言: Java 21
- Loader: NeoForge `21.1.215`
- 主要依赖: GeckoLib `4.8.2`、JEI `19.25.0.322`
- 当前代码与资源规模:
`src/main/java` 约 244 个 Java 文件，
`src/main/resources` 约 356 个文件，
`src/generated/resources` 约 610 个文件

## 环境要求
构建前必须满足以下条件:

1. 使用 JDK 21（不是 JRE）。
2. `JAVA_HOME` 指向 JDK 21。
3. 使用仓库自带的 `gradlew`/`gradlew.bat`。

本地若出现 `Gradle requires JVM 17 or later ... configured to use JVM 11`，先修复 JDK 再继续。

## 常用命令
Windows 下优先使用:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat runData
```

说明:

1. CI 仅执行 `build`（见 `.github/workflows/build.yml`）。
2. `runData` 输出目录是 `src/generated/resources`，并且该目录已被加入运行资源路径（见 `build.gradle` 的 `sourceSets` 配置）。

## 项目结构速览
核心入口:

1. `src/main/java/com/besson/endfield/ArknightsEndField.java`
2. `src/main/java/com/besson/endfield/ArknightsEndFieldClient.java`

主要模块:

1. `block/` 与 `block/custom/`: 方块定义与放置/拆除逻辑。
2. `blockEntity/` 与 `blockEntity/custom/`: 方块实体、Tick、库存、配方处理、供电行为。
3. `screen/` 与 `screen/custom/`: 容器菜单与 GUI。
4. `recipe/` 与 `recipe/custom/`: 自定义配方类型、序列化器、输入结构。
5. `item/` 与 `item/custom/`: 物品注册、食物属性、自定义物品行为。
6. `renderer/`, `model/`: GeckoLib 方块实体与物品模型渲染。
7. `compat/`: JEI 插件与各配方分类。
8. `network/`: 自定义网络包（当前用于 Crafter 切换配方）。
9. `power/`: 全局电网状态与分配管理。

资源目录:

1. `src/main/resources/assets/arknights_endfield`: 手写/主资源（纹理、geo、动画、blockstates、models）。
2. `src/generated/resources`: 生成数据与语言文件（recipe、loot table、tags、lang 等）。

## 架构要点
注册链路（通用）:

1. 在 `Mod*` 注册类里注册对象（方块/物品/方块实体/菜单/配方/实体等）。
2. 在主入口 `ArknightsEndField` 构造函数中绑定到 mod event bus。
3. 客户端渲染与菜单在 `ArknightsEndFieldClient` 和 `ModBlockEntityRenderers` 中注册。

多机器系统（高耦合）:

1. 多数机器包含完整链路:
`Block` -> `BlockEntity` -> `ScreenHandler` -> `Screen` -> `Recipe` -> `JEI Category`。
2. 多方块机器存在 “主方块 + side 方块” 模式（例如 `RefiningUnit` 一类），放置/拆除会批量创建或销毁 side 方块并维护父子坐标。
3. 自动化输入输出通过 `ModCapabilities` 暴露，通常按朝向区分 input/output capability。

电力系统:

1. `PowerNetworkManager` 以 `SavedData` 持久化电量并在服务器 Tick 分配功率。
2. 发电端通过 `registerGenerator` 注册（如 `ThermalBank`, `ProtocolAnchorCore`）。
3. 用电端通过 `registerConsumer` 注册（如 `ElectricPylon` 再分发给周边 `ElectrifiableDevice`）。
4. 修改供配电逻辑时必须同时检查:
`power/`、`blockEntity/custom/*Pylon*`、`blockEntity/custom/*Core*`、`blockEntity/custom/*ThermalBank*`。

## 新增内容改动清单
新增“机器方块”（尤其是可交互机器）时，至少检查以下位置:

1. `ModBlocks` 注册方块（是否自动注册 BlockItem）。
2. `ModItems` 注册对应物品（若为 GeckoLib 物品需自定义 item class + renderer + model）。
3. `ModBlockEntities` 注册方块实体类型。
4. `ModScreens` 注册菜单类型。
5. `ArknightsEndFieldClient#registerScreens` 注册 GUI。
6. `ModBlockEntityRenderers` 注册方块实体渲染器（若使用 GeckoLib 动态渲染）。
7. `ModCapabilities` 注册自动化能力（Item/Energy 输入输出）。
8. `ModRecipes` 注册 RecipeType 与 RecipeSerializer（若有自定义工艺）。
9. `compat/ModJEIPlugin` + `compat/custom/*Category*` 接入 JEI（分类、配方、催化剂、点击区域）。
10. `src/main/resources/assets/...` 与 `src/generated/resources/...` 补齐模型/贴图/配方/语言/战利品等资源。

## 修改约束与实践
1. 保持注册名、资源文件名、JSON id 一致，不要只改 Java 常量名。
2. 方块实体状态字段变更时，同步维护 `saveAdditional` / `loadAdditional` / `getUpdateTag`。
3. 机器工作状态变化要显式 `setChanged()`，必要时 `sendBlockUpdated(...)` 同步客户端。
4. 供配电注册生命周期保持成对:
`setLevel` 注册，`setRemoved` 注销。
5. Side block 逻辑改动时，务必验证放置/拆除是否会遗留孤立 side 方块。
6. 不要假定存在完整 datagen Java 代码；当前仓库的很多运行资源直接来自 `src/generated/resources`。

## 已知注意点
1. 仓库包含中文内容，终端读取时建议显式 `UTF-8`（PowerShell 示例: `Get-Content README.md -Encoding UTF8`）。
2. 当前无 `src/test` 自动化测试目录，回归主要依赖:
`build` + 本地客户端/服务端手测。
3. `ModLootTableModifier`、部分注册名与命名存在历史兼容写法，未确认影响前不要“顺手重命名”。
4. 许可边界:
代码 GPL-3.0；资源 CC BY-NC-SA 4.0，禁止商业使用。

## 最低验证清单
提交前至少完成:

1. `./gradlew build` 通过（JDK 21 环境）。
2. 客户端能进入世界并打开变更涉及的机器 GUI。
3. 新增/修改配方能在游戏内匹配，且 JEI 展示正常（若项目启用 JEI）。
4. 若改动了多方块机器，验证放置、拆除、掉落和 side 方块清理。
5. 若改动了能力或电力，验证自动化输入输出与供电行为。
