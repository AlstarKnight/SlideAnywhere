# SlideAnywhere

全局负一屏：在任意界面从屏幕边缘滑出的卡片面板。目标是复刻已停更的 Android 应用「全局负一屏」，并适配新版 Android（最低 Android 8.0，目标 Android 16）。

## 当前进度

- [x] 边缘触发条：左右两侧可选，可调垂直位置、高度、宽度，可隐藏；避让系统返回手势
- [x] 从边缘向内滑动打开面板，点空白处或返回键关闭；常驻通知也可打开面板
- [x] 权限引导：悬浮窗、通知、忽略电池优化；开机自启
- [ ] 卡片：应用、快捷方式、网址/URI、便签；多页；编辑模式；配置导入导出
- [ ] 系统卡片：电量、亮度/音量条、存储、使用统计、最近运行、开关、系统小部件
- [ ] 主题与模糊、图标包、悬浮球、黑名单和情景模式、全局护眼等

## 安装测试版

每次推送后 GitHub Actions 会编译 debug APK：打开仓库的 Actions 页面，进入最新一次 Android CI，下载 `slideanywhere-debug-apk`。

首次打开后先开启「悬浮窗」权限，再打开「启用全局负一屏」开关。

## 本地编译

需要 JDK 17 和 Android SDK（compileSdk 36）：

```
./gradlew testDebugUnitTest assembleDebug
```

## 参考

- [Smart Edge](https://github.com/Imtiaz-Official/Smart-Edge)（MIT）：边缘触发和悬浮面板的实现思路
- Panels（Sidebar）：交互参考
