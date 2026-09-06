/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.ui.context.LocalTTSState
import me.rerere.rikkahub.utils.extractQuotedContentAsText
import me.rerere.rikkahub.utils.stripTtsInternalMarkup

@Composable
fun TTSAutoPlay(vm: ChatVM, setting: Settings, conversation: Conversation) {
    // Auto-play TTS after generation completes
    val tts = LocalTTSState.current
    val currentConversation by rememberUpdatedState(conversation)
    val updatedSetting by rememberUpdatedState(setting)
    LaunchedEffect(Unit) {
        vm.generationDoneFlow.collect { conversationId ->
            val display = updatedSetting.displaySetting
            if (!display.autoPlayTTSAfterGeneration && !display.autoVoiceMessageAfterGeneration) {
                return@collect
            }
            val lastNode = currentConversation.messageNodes.lastOrNull() ?: return@collect
            val lastMessage = lastNode.messages.getOrNull(lastNode.selectIndex) ?: return@collect
            if (lastMessage.role != MessageRole.ASSISTANT) return@collect

            val text = lastMessage.toText().stripTtsInternalMarkup()
            val textToSpeak = if (display.ttsOnlyReadQuoted) {
                text.extractQuotedContentAsText() ?: text
            } else {
                text
            }
            if (textToSpeak.isBlank()) return@collect

            // 语音条优先：要留一条能回放的，就不再另外念一遍，否则同一句会响两次。
            if (display.autoVoiceMessageAfterGeneration) {
                // 已经有语音条就跳过——重开会话或重复触发时别再花一次合成的钱。
                if (lastMessage.parts.any { it is UIMessagePart.VoiceMessage }) return@collect
                // 交给 ViewModel 作用域去合成：这里是页面的 LaunchedEffect，
                // 用户合成期间切走页面会被取消，钱花了却没留下语音条。
                vm.generateVoiceMessageFor(
                    nodeId = lastNode.id,
                    messageId = lastMessage.id,
                    text = textToSpeak,
                ) { tts.createVoiceMessage(it) }
            } else {
                tts.speak(textToSpeak)
            }
        }
    }
}
