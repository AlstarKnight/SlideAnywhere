package io.github.alstarknight.slidex

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.StringRes

/** A ready-made link into a page of another app, such as Alipay's pay code. */
class LinkPreset(@StringRes val labelRes: Int, val uri: String)

/** A link into another app, with what search finds it by: the app's name followed by the link's. */
class AppLink(val item: LinkItem, val search: SearchKey)

/** The links into one app, under the app's name as the system shows it. */
class AppLinks(val packageName: String, val appLabel: String, val links: List<AppLink>)

/** [AppLinks] for [items], with their search keys. Call off the main thread. */
fun appLinks(packageName: String, appLabel: String, items: List<LinkItem>) = AppLinks(
    packageName,
    appLabel,
    items.map { AppLink(it, searchKey(appLabel + it.label, packageName, Pinyin::syllable)) },
)

/**
 * The links listed first: the installed apps' links from [COMMON_PRESETS] in [presets], with Douyin's scanner from
 * its own [shortcuts] since it has no link for other apps, in the order of [COMMON_APPS].
 */
fun commonLinks(presets: List<AppLinks>, shortcuts: List<AppLinks>): List<AppLinks> {
    val douyinScanner = shortcuts
        .filter { it.packageName == DOUYIN }
        .map { app -> AppLinks(app.packageName, app.appLabel, app.links.filter { "扫" in it.item.label }) }
        .filter { it.links.isNotEmpty() }
    return (presets + douyinScanner).sortedBy { COMMON_APPS.indexOf(it.packageName) }
}

private const val DOUYIN = "com.ss.android.ugc.aweme"

/** The apps with common links, in the order they are listed. */
private val COMMON_APPS =
    listOf("com.eg.android.AlipayGphone", "com.tencent.mm", "com.sankuai.meituan", DOUYIN, "com.ss.android.lark")

/** The links from [presets] for the apps that are installed and can still open them. Call off the main thread. */
fun installedPresets(context: Context, presets: List<Pair<String, List<LinkPreset>>>): List<AppLinks> {
    val pm = context.packageManager
    return presets.mapNotNull { (packageName, appPresets) ->
        val app = try {
            pm.getApplicationInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return@mapNotNull null
        }
        val usable = appPresets.filter { preset ->
            val intent = ItemLauncher.linkIntent(preset.uri)
            intent != null && pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
        }
        if (usable.isEmpty()) {
            null
        } else {
            val links = usable.map { LinkItem(context.getString(it.labelRes), it.uri) }
            appLinks(packageName, app.loadLabel(pm).toString(), links)
        }
    }
}

private const val ALIPAY = "alipays://platformapi/startapp?saId="

/** The links most people want, listed first. */
val COMMON_PRESETS = listOf(
    "com.eg.android.AlipayGphone" to listOf(
        LinkPreset(R.string.preset_scan, "${ALIPAY}10000007"),
        LinkPreset(R.string.preset_pay_code, "${ALIPAY}20000056"),
    ),
    // WeChat's own launcher shortcut for the scanner. Its pay code has no link other apps may open.
    "com.tencent.mm" to listOf(
        LinkPreset(
            R.string.preset_scan,
            "intent:#Intent;component=com.tencent.mm/.ui.LauncherUI;" +
                "B.LauncherUI.From.Scaner.Shortcut=true;launchFlags=0x14000000;end",
        ),
    ),
    // From here on the links come from public scheme lists and were not checked on every app version.
    "com.sankuai.meituan" to listOf(LinkPreset(R.string.preset_scan, "imeituan://www.meituan.com/scanQRCode")),
    "com.ss.android.lark" to listOf(
        LinkPreset(
            R.string.preset_scan,
            "intent://applink.feishu.cn/client/qrcode/main#Intent;scheme=https;package=com.ss.android.lark;end",
        ),
    ),
)

/** The other ready-made links, listed under the common ones. Apart from Alipay's, from public scheme lists. */
val MORE_PRESETS = listOf(
    "com.eg.android.AlipayGphone" to listOf(
        LinkPreset(R.string.preset_receive_code, "${ALIPAY}20000123"),
        LinkPreset(R.string.preset_bus_code, "${ALIPAY}200011235"),
        LinkPreset(R.string.preset_transfer, "${ALIPAY}20000221"),
        LinkPreset(R.string.preset_ant_forest, "${ALIPAY}60000002"),
    ),
    "com.unionpay" to listOf(
        LinkPreset(R.string.preset_scan, "upwallet://native/scanCode"),
        LinkPreset(R.string.preset_pay_code, "upwallet://pay"),
    ),
    "com.jingdong.app.mall" to listOf(
        LinkPreset(R.string.preset_scan, "openapp.jdmobile://virtual?params={\"category\":\"jump\",\"des\":\"saoasao\"}"),
    ),
    "com.taobao.taobao" to listOf(LinkPreset(R.string.preset_scan, "taobao://tb.cn/n/scancode")),
    "com.tencent.mobileqq" to listOf(
        LinkPreset(R.string.preset_scan, "mqqapi://qrcode/scan_qrcode?version=1&src_type=app"),
    ),
    "com.jingyao.easybike" to listOf(LinkPreset(R.string.preset_scan, "hellobike://hellobike.com/scan_qr")),
    "com.alibaba.android.rimet" to listOf(LinkPreset(R.string.preset_scan, "dingtalk://dingtalkclient/page/scan")),
    "tv.danmaku.bili" to listOf(LinkPreset(R.string.preset_scan, "bilibili://qrcode")),
    "com.sina.weibo" to listOf(
        LinkPreset(R.string.preset_scan, "sinaweibo://qrcode"),
        LinkPreset(R.string.preset_post_weibo, "sinaweibo://sendweibo"),
    ),
)
