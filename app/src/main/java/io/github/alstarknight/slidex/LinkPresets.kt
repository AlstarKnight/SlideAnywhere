package io.github.alstarknight.slidex

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.StringRes

/** A ready-made link into a page of another app, such as Alipay's pay code. */
class LinkPreset(@StringRes val labelRes: Int, val uri: String)

/** The presets for one app, with the app's name as the system shows it. */
class PresetGroup(val appLabel: String, val presets: List<LinkPreset>)

/** Ready-made links for the apps that are installed and can still open them. */
fun installedPresets(context: Context): List<PresetGroup> {
    val pm = context.packageManager
    return LINK_PRESETS.mapNotNull { (packageName, presets) ->
        val app = try {
            pm.getApplicationInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return@mapNotNull null
        }
        val usable = presets.filter { preset ->
            val intent = ItemLauncher.linkIntent(preset.uri)
            intent != null && pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
        }
        if (usable.isEmpty()) null else PresetGroup(app.loadLabel(pm).toString(), usable)
    }
}

private const val ALIPAY = "alipays://platformapi/startapp?saId="

private val LINK_PRESETS = listOf(
    "com.eg.android.AlipayGphone" to listOf(
        LinkPreset(R.string.preset_scan, "${ALIPAY}10000007"),
        LinkPreset(R.string.preset_pay_code, "${ALIPAY}20000056"),
        LinkPreset(R.string.preset_receive_code, "${ALIPAY}20000123"),
        LinkPreset(R.string.preset_bus_code, "${ALIPAY}200011235"),
        LinkPreset(R.string.preset_transfer, "${ALIPAY}20000221"),
        LinkPreset(R.string.preset_ant_forest, "${ALIPAY}60000002"),
    ),
    // WeChat's own launcher shortcut for the scanner. Its pay code has no documented link for other apps; the one
    // here is from iOS scheme lists and may only open WeChat.
    "com.tencent.mm" to listOf(
        LinkPreset(
            R.string.preset_scan,
            "intent:#Intent;component=com.tencent.mm/.ui.LauncherUI;" +
                "B.LauncherUI.From.Scaner.Shortcut=true;launchFlags=0x14000000;end",
        ),
        LinkPreset(R.string.preset_pay_code_trial, "weixin://widget/pay"),
    ),
    // The links below come from public scheme lists and were not checked on every app version.
    "com.unionpay" to listOf(
        LinkPreset(R.string.preset_scan, "upwallet://native/scanCode"),
        LinkPreset(R.string.preset_pay_code, "upwallet://pay"),
    ),
    "com.sankuai.meituan" to listOf(LinkPreset(R.string.preset_scan, "imeituan://www.meituan.com/scanQRCode")),
    "com.jingdong.app.mall" to listOf(
        LinkPreset(R.string.preset_scan, "openapp.jdmobile://virtual?params={\"category\":\"jump\",\"des\":\"saoasao\"}"),
    ),
    "com.taobao.taobao" to listOf(LinkPreset(R.string.preset_scan, "taobao://tb.cn/n/scancode")),
    "com.tencent.mobileqq" to listOf(
        LinkPreset(R.string.preset_scan, "mqqapi://qrcode/scan_qrcode?version=1&src_type=app"),
    ),
    "com.jingyao.easybike" to listOf(LinkPreset(R.string.preset_scan, "hellobike://hellobike.com/scan_qr")),
    "com.alibaba.android.rimet" to listOf(LinkPreset(R.string.preset_scan, "dingtalk://dingtalkclient/page/scan")),
    "com.ss.android.lark" to listOf(
        LinkPreset(
            R.string.preset_scan,
            "intent://applink.feishu.cn/client/qrcode/main#Intent;scheme=https;package=com.ss.android.lark;end",
        ),
    ),
    "tv.danmaku.bili" to listOf(LinkPreset(R.string.preset_scan, "bilibili://qrcode")),
    "com.sina.weibo" to listOf(
        LinkPreset(R.string.preset_scan, "sinaweibo://qrcode"),
        LinkPreset(R.string.preset_post_weibo, "sinaweibo://sendweibo"),
    ),
)
