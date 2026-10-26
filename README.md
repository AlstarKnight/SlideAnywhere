# SlideX

全局负一屏（仓库名 SlideAnywhere）：在任意界面从屏幕边缘滑出的卡片面板。目标是复刻已停更的 Android 应用「全局负一屏」，并适配新版 Android（最低 Android 8.0，目标 Android 16）。

## 当前进度

- [x] 边缘触发条：左右两侧可选，可调垂直位置、高度、宽度，可隐藏；避让系统返回手势
- [x] 悬浮图标触发：圆形或方形，可调大小和不透明度，拖动后自动贴边，面板从图标所在一侧滑出
- [x] 主题预设（参考 Minipad Next）：星空、Material（Android 12+ 跟随壁纸取色）、纸感、经典；深浅色可跟随系统或固定
- [x] 从边缘向内滑动打开面板，点空白处或返回键关闭；常驻通知也可打开面板
- [x] 权限引导：悬浮窗、通知、忽略电池优化；开机自启
- [x] 卡片：图标卡片（应用、快捷方式、网址/URI 可混放，每行 2–6 个）和便签卡片；多页；编辑模式（长按卡片标题或点编辑按钮）；配置导入导出（JSON）
- [x] 快捷方式：通知栏、快捷设置、最近任务、主屏幕、返回、锁屏、截屏、分屏、电源菜单（需开启无障碍服务），播放/暂停、上一首、下一首、音量加减、语音助手、应用抽屉、相机、相册、拨号、系统设置
- [x] 开关（图标显示开关状态）：手电筒、自动旋转、自动亮度、屏幕常亮、全局护眼、静音、震动直接切换；WLAN、蓝牙、移动数据、热点、定位、NFC、飞行模式打开系统开关面板（Android 10 起应用不能直接切换）
- [x] 系统卡片：电量（含温度、电压、健康）、亮度条、音量条（媒体、铃声、闹钟）、内存与存储（含开机时间）
- [ ] 使用统计、最近运行、系统小部件、应用静态快捷方式
- [ ] 背景模糊、图标包、黑名单和情景模式、验证码复制、一言/RSS 等

## 安装测试版

每次推送后 GitHub Actions 会编译 APK：打开仓库的 Actions 页面，进入最新一次 Android CI 下载。

- 固定下载链接（最近一次 CI 编译的签名版）：https://github.com/AlstarKnight/SlideAnywhere/releases/download/latest/SlideX.apk
- `slidex-release-apk`：用固定密钥签名，可以覆盖升级，日常安装用这个。需要在仓库 Secrets 里配置 `ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD` 后才会生成。
- `slidex-debug-apk`：每次签名都不同，升级前要先卸载旧版。

首次打开后先开启「悬浮窗」权限，再打开「启用全局负一屏」开关。锁屏、截屏、通知栏等快捷方式还需要开启「无障碍服务」；Android 13 及以上侧载安装的应用，要先在系统的应用信息页右上角菜单里选「允许受限设置」。

## 本地编译

需要 JDK 17 和 Android SDK（compileSdk 36）：

```
./gradlew testDebugUnitTest assembleDebug
```

## 参考

- [Smart Edge](https://github.com/Imtiaz-Official/Smart-Edge)（MIT）：边缘触发和悬浮面板的实现思路
- Panels（Sidebar）：交互参考
- [Material Symbols](https://github.com/google/material-design-icons)（Apache 2.0）：快捷方式和开关的图标
