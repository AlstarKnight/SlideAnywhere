# APK 体积：为什么比全局负一屏大，怎么压下来

_2026-10-27，数字都来自 CI 出的 release 包（PR #4），不是估算。_

## 结论

- 压缩前 SlideX 是 **7.24 MB**（7,588,069 字节），原版全局负一屏 3.2.9 是 **1.44 MB**（1,513,911 字节）。
- 主要原因是 release 包没开 R8：Compose 界面库、Material 3、Kotlin 标准库整份打进 APK，其中我们自己的代码只占约 5%。原版用的是系统自带的 View 界面，界面代码不用打进 APK。
- 第二个原因是 Compose 库自带约 85 种语言的字符串，放在不压缩的 `resources.arsc` 里（537 KB）。
- 打开 R8 和资源压缩后，SlideX 是 **1.67 MB**（1,750,112 字节），比原来小 77%。语言资源按用户要求全部保留（体积不是重点）；只留中文时是 1.47 MB。

## 各部分实测（APK 里压缩后的大小，KB）

| 部分 | 压缩前 | 现在 | 原版 3.2.9 |
|---|---|---|---|
| 代码 classes*.dex | 6,665（2 个文件） | 1,279 | 565 |
| resources.arsc（语言字符串等） | 537 | 233 | 12 |
| res/ 图标和布局 | 65 | 62 | 274 |
| lib/ 原生库 .so | 37 | 37 | 496 |
| 其他（签名、清单、assets 等） | 42 | 38 | 98 |

压缩前 dex 里各部分占比（按字节码估算）：Compose ui 18%、Material 3 17%、Compose foundation 16%、Compose runtime 5%、SlideX 自己的代码 5%、其余是 androidx 和 Kotlin 库。
原版 dex 里：它自己的代码 51%、OkHttp 20%、Bmob 云服务 SDK 17%、support 库 12%。原版的大头反而在 lib/（一个 1.7 MB 未压缩的 RenderScript 库，只带 32 位版本）。

## 做了什么（PR #4）

- `isMinifyEnabled = true`、`isShrinkResources = true`：R8 删掉没用到的代码和资源，并做优化。
- 删掉没用到的 `ui-tooling`、`ui-tooling-preview` 依赖（代码里没有 `@Preview`）。
- `-dontobfuscate` 加 `-keepattributes SourceFile,LineNumberTable`：类名、方法名、行号保留，崩溃日志能直接读。

检查过：清单里的 8 个组件类和所有 `$$serializer` 类都在；配置 JSON 用到的名字（`type`、`grid`、`from_who` 等）都在；库自带的语言都在。

## 没做的，和原因

- **只留中文资源**（`androidResources.localeFilters`）：能省约 200 KB，用户说语言不要删。
- **混淆类名**：能再省约 80 KB（只留中文时实测 1,543,908 → 1,461,729 字节）。没开，因为「latest」每次 CI 都会被替换，用户手上那版的 mapping 文件找不到，崩溃日志就没法读。
- **去掉 x86 / 32 位的 `libandroidx.graphics.path.so`**：只省约 25 KB，换来部分设备或模拟器装不上，不值得。
- **改回 View 界面**：还能再小（具体多少未验证），但等于整个界面重写，不做。
- Compose 运行时压缩后约 1 MB 是下限，之后加功能主要增加的是我们自己的代码，增长会很慢。

## 以后开发要注意

- release 现在会被 R8 处理。新代码如果用反射、按类名字符串找类、或用 `getIdentifier` 按名字取**自己的**资源，要在 `app/proguard-rules.pro` 加 keep 规则，否则 debug 版正常、release 版崩溃。
