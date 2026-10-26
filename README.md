# SlideAnywhere

全局负一屏：在任意界面从屏幕边缘滑出的卡片面板。目标是复刻已停更的 Android 应用「全局负一屏」，并适配新版 Android（最低 Android 8.0，目标 Android 16）。

## 当前进度

- [x] 边缘触发条：左右两侧可选，可调垂直位置、高度、宽度，可隐藏；避让系统返回手势
- [x] 从边缘向内滑动打开面板，点空白处或返回键关闭；常驻通知也可打开面板
- [x] 权限引导：悬浮窗、通知、忽略电池优化；开机自启
- [x] 卡片：图标卡片（应用、快捷方式、网址/URI 可混放）和便签卡片；多页；编辑模式（长按卡片标题或点编辑按钮）；配置导入导出（JSON）
- [x] 快捷方式：通知栏、快捷设置、最近任务、主屏幕、返回、锁屏、截屏、分屏（需开启无障碍服务），播放/暂停、上一首、下一首、音量加减、手电筒、语音助手
- [ ] 系统卡片：电量、亮度/音量条、存储、使用统计、最近运行、开关、系统小部件
- [ ] 主题与模糊、图标包、悬浮球、黑名单和情景模式、全局护眼等

## 安装测试版

每次推送后 GitHub Actions 会编译 APK：打开仓库的 Actions 页面，进入最新一次 Android CI 下载。

- `slideanywhere-release-apk`：用固定密钥签名，可以覆盖升级，日常安装用这个。需要在仓库 Secrets 里配置 `ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD` 后才会生成。
- `slideanywhere-debug-apk`：每次签名都不同，升级前要先卸载旧版。

首次打开后先开启「悬浮窗」权限，再打开「启用全局负一屏」开关。锁屏、截屏、通知栏等快捷方式还需要开启「无障碍服务」；Android 13 及以上侧载安装的应用，要先在系统的应用信息页右上角菜单里选「允许受限设置」。

## 本地编译

需要 JDK 17 和 Android SDK（compileSdk 36）：

```
./gradlew testDebugUnitTest assembleDebug
```

## 参考

- [Smart Edge](https://github.com/Imtiaz-Official/Smart-Edge)（MIT）：边缘触发和悬浮面板的实现思路
- Panels（Sidebar）：交互参考
