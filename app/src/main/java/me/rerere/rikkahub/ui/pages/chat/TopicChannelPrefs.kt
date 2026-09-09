/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.chat

import android.content.Context
import android.util.Log
import kotlin.uuid.Uuid

/**
 * 话题频道（复用现有文件夹）的"当前所在频道"记忆。
 *
 * 目的：用户在某个频道里点新建对话时，新对话应该留在同一个频道，
 * 而不是掉回「未归类」。新对话是懒保存的，创建时机和抽屉不在同一个 VM，
 * 所以用一个轻量的 SharedPreferences 传递选中的频道。
 */
object TopicChannelPrefs {
    private const val TAG = "TopicChannelPrefs"
    private const val PREFS_NAME = "topic_channel_prefs"
    private const val KEY_SELECTED_FOLDER = "selected_folder_id"

    /** 读取当前选中的频道，null 表示未归类 */
    fun read(context: Context): Uuid? {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED_FOLDER, null)
            ?: return null
        return runCatching { Uuid.parse(raw) }
            .onFailure { Log.w(TAG, "解析已保存的频道 id 失败: $raw", it) }
            .getOrNull()
    }

    /** 记录当前选中的频道，传 null 表示回到未归类 */
    fun write(context: Context, folderId: Uuid?) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .apply {
                if (folderId == null) {
                    remove(KEY_SELECTED_FOLDER)
                } else {
                    putString(KEY_SELECTED_FOLDER, folderId.toString())
                }
            }
            .apply()
    }
}
