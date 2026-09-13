/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.voice

/**
 * 把通话秒数格式化成人看的时长。
 *
 * 用冒号分隔而不是"x分y秒"：通话时长是给人扫一眼的，跟系统通话记录的写法保持一致。
 * 超过一小时才显示小时位，免得绝大多数几分钟的通话前面挂个没用的 0。
 */
fun formatCallDuration(totalSeconds: Int): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%d:%02d".format(m, s)
    }
}
