package com.google.ai.edge.gallery.ui.edgeai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.edge.gallery.data.ModelDownloadStatusType
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.chathistory.ConversationEntity
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageText
import com.google.ai.edge.gallery.ui.common.chat.ChatSide
import com.google.ai.edge.gallery.ui.llmchat.LlmChatViewModel
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import java.util.UUID
import kotlinx.coroutines.launch

// Simple message type used by the new UI layer
data class Message(
  val id: String = UUID.randomUUID().toString(),
  val role: String, // "user" or "assistant"
  val text: String,
)

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun EdgeAiEntryPoint(modelManagerViewModel: ModelManagerViewModel) {
  val llmChatViewModel: LlmChatViewModel = hiltViewModel()
  val chatHistoryViewModel: ChatHistoryViewModel = hiltViewModel()
  val context = LocalContext.current
  var onboarded by remember { mutableStateOf(isOnboarded(context)) }

  if (!onboarded) {
    EdgeOnboarding(
      modelManagerViewModel = modelManagerViewModel,
      onDone = { onboarded = true },
    )
  } else {
    EdgeAiScreen(
      modelManagerViewModel = modelManagerViewModel,
      llmChatViewModel = llmChatViewModel,
      chatHistoryViewModel = chatHistoryViewModel,
    )
  }
}

// ── Main screen ───────────────────────────────────────────────────────────────

enum class EdgeScreen { CHAT, MODELS, SETTINGS, HISTORY }

@Composable
fun EdgeAiScreen(
  modelManagerViewModel: ModelManagerViewModel,
  llmChatViewModel: LlmChatViewModel,
  chatHistoryViewModel: ChatHistoryViewModel,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val mmState by modelManagerViewModel.uiState.collectAsState()
  val chatState by llmChatViewModel.uiState.collectAsState()
  val historyState by chatHistoryViewModel.state.collectAsState()

  var screen by remember { mutableStateOf(EdgeScreen.CHAT) }
  var drawerOpen by remember { mutableStateOf(false) }
  var voiceModeOpen by remember { mutableStateOf(false) }

  // Conversation tracking state
  var currentConversationId by remember { mutableStateOf<String?>(null) }
  var loadedHistoryMessages by remember { mutableStateOf<List<Message>>(emptyList()) }
  var waitingForAssistant by remember { mutableStateOf(false) }

  // Resolve the LLM_CHAT task and active model
  val llmTask = mmState.tasks.find { it.id == BuiltInTaskId.LLM_CHAT }
  val activeModel = mmState.selectedModel
    .takeIf { it.name.isNotEmpty() && it.isLlm }
    ?: llmTask?.models?.firstOrNull {
      mmState.modelDownloadStatus[it.name]?.status == ModelDownloadStatusType.SUCCEEDED
    }
    ?: llmTask?.models?.firstOrNull()

  // Initialize the model when it becomes available
  LaunchedEffect(activeModel?.name) {
    val model = activeModel ?: return@LaunchedEffect
    val task = llmTask ?: return@LaunchedEffect
    val initStatus = mmState.modelInitializationStatus[model.name]
    if (initStatus == null ||
      initStatus.status == com.google.ai.edge.gallery.ui.modelmanager.ModelInitializationStatusType.NOT_INITIALIZED
    ) {
      modelManagerViewModel.initializeModel(context, task, model) {}
    }
  }

  // Reset chat session when active model changes
  LaunchedEffect(activeModel?.name) {
    val model = activeModel ?: return@LaunchedEffect
    val task = llmTask ?: return@LaunchedEffect
    llmChatViewModel.resetSession(task = task, model = model)
    // Clear conversation state when model changes
    currentConversationId = null
    loadedHistoryMessages = emptyList()
    waitingForAssistant = false
  }

  // Convert real ChatMessages → simple Message type
  val realMessages = activeModel?.let { model ->
    chatState.messagesByModel[model.name]
      ?.filterIsInstance<ChatMessageText>()
      ?.map { msg ->
        Message(
          role = if (msg.side == ChatSide.USER) "user" else "assistant",
          text = msg.content,
        )
      } ?: emptyList()
  } ?: emptyList()

  // Displayed messages = historical (if viewing past conv) + live LLM messages
  val displayedMessages = loadedHistoryMessages + realMessages

  val streamingText = activeModel?.let { model ->
    (chatState.streamingMessagesByModel[model.name] as? ChatMessageText)?.content ?: ""
  } ?: ""

  val streaming = chatState.inProgress
  val activeModelName = activeModel?.displayName?.ifEmpty { activeModel.name } ?: "No model"

  // Auto-save assistant message when streaming completes
  LaunchedEffect(streaming) {
    if (!streaming && waitingForAssistant) {
      waitingForAssistant = false
      val convId = currentConversationId ?: return@LaunchedEffect
      val lastMsg = (loadedHistoryMessages + realMessages).lastOrNull() ?: return@LaunchedEffect
      if (lastMsg.role == "assistant") {
        chatHistoryViewModel.saveAssistantMessage(convId, lastMsg.text)
      }
    }
  }

  fun sendMessage(text: String) {
    val model = activeModel ?: return
    val convId = currentConversationId ?: run {
      val newId = UUID.randomUUID().toString()
      chatHistoryViewModel.createConversation(
        id = newId,
        title = text.take(60),
        modelName = activeModelName,
      )
      currentConversationId = newId
      newId
    }
    chatHistoryViewModel.saveUserMessage(convId, text)
    waitingForAssistant = true

    llmChatViewModel.addMessage(
      model = model,
      message = ChatMessageText(content = text, side = ChatSide.USER),
    )
    llmChatViewModel.generateResponse(
      model = model,
      input = text,
      onError = {},
    )
  }

  fun newChat() {
    val model = activeModel ?: return
    val task = llmTask ?: return
    currentConversationId = null
    loadedHistoryMessages = emptyList()
    waitingForAssistant = false
    llmChatViewModel.resetSession(task = task, model = model)
  }

  fun openConversation(conv: ConversationEntity) {
    scope.launch {
      val messages = chatHistoryViewModel.loadConversationMessages(conv.id)
      loadedHistoryMessages = messages.map { entity ->
        Message(id = entity.id, role = entity.role, text = entity.text)
      }
      currentConversationId = conv.id
      waitingForAssistant = false
      val model = activeModel ?: return@launch
      val task = llmTask ?: return@launch
      llmChatViewModel.resetSession(task = task, model = model)
      drawerOpen = false
      screen = EdgeScreen.CHAT
    }
  }

  EdgeChatScreen(
    messages = displayedMessages,
    streaming = streaming,
    streamingText = streamingText,
    activeModelName = activeModelName,
    drawerOpen = drawerOpen,
    historyItems = historyState.recentConversations,
    hasMoreHistory = historyState.totalCount > 10,
    currentConversationId = currentConversationId,
    onMenuClick = { drawerOpen = true },
    onDrawerClose = { drawerOpen = false },
    onNewChat = { newChat() },
    onSend = { sendMessage(it) },
    onModelChipClick = { screen = EdgeScreen.MODELS },
    onVoiceClick = { voiceModeOpen = true },
    onModelsNav = { screen = EdgeScreen.MODELS },
    onSettingsNav = { screen = EdgeScreen.SETTINGS },
    onHistoryItemClick = { conv -> openConversation(conv) },
    onDeleteConversation = { conv -> chatHistoryViewModel.deleteConversation(conv) },
    onMoreHistory = { screen = EdgeScreen.HISTORY },
  )

  if (screen == EdgeScreen.MODELS) {
    EdgeModelHub(
      modelManagerViewModel = modelManagerViewModel,
      onBack = { screen = EdgeScreen.CHAT },
      onModelSelected = { model ->
        modelManagerViewModel.selectModel(model)
        screen = EdgeScreen.CHAT
      },
    )
  }

  if (screen == EdgeScreen.SETTINGS) {
    EdgeSettings(onBack = { screen = EdgeScreen.CHAT })
  }

  if (screen == EdgeScreen.HISTORY) {
    ChatHistoryScreen(
      historyState = historyState,
      onBack = { screen = EdgeScreen.CHAT },
      onConversationClick = { conv -> openConversation(conv) },
      onDeleteConversation = { conv -> chatHistoryViewModel.deleteConversation(conv) },
      onLoadMore = { chatHistoryViewModel.loadMoreConversations() },
      onScreenEnter = { chatHistoryViewModel.loadAllConversations() },
    )
  }

  if (voiceModeOpen) {
    EdgeVoiceMode(onDismiss = { voiceModeOpen = false })
  }
}
