package com.google.ai.edge.gallery.ui.edgeai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.chathistory.ConversationEntity
import com.google.ai.edge.gallery.data.chathistory.ChatHistoryRepository
import com.google.ai.edge.gallery.data.chathistory.MessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatHistoryState(
  val recentConversations: List<ConversationEntity> = emptyList(),
  val allConversations: List<ConversationEntity> = emptyList(),
  val totalCount: Int = 0,
  val hasMore: Boolean = false,
  val isLoadingMore: Boolean = false,
)

@HiltViewModel
class ChatHistoryViewModel @Inject constructor(
  private val repository: ChatHistoryRepository,
) : ViewModel() {

  private val _state = MutableStateFlow(ChatHistoryState())
  val state: StateFlow<ChatHistoryState> = _state.asStateFlow()

  private val PAGE_SIZE = 20
  private var allConversationsOffset = 0

  init {
    viewModelScope.launch {
      combine(
        repository.getRecentConversations(10),
        repository.getConversationCountFlow(),
      ) { recent, count ->
        Pair(recent, count)
      }.collect { (recent, count) ->
        _state.update { it.copy(recentConversations = recent, totalCount = count) }
      }
    }
  }

  fun createConversation(id: String, title: String, modelName: String) {
    viewModelScope.launch { repository.createConversation(id, title, modelName) }
  }

  fun saveUserMessage(conversationId: String, text: String) {
    viewModelScope.launch { repository.addMessage(conversationId, "user", text) }
  }

  fun saveAssistantMessage(conversationId: String, text: String) {
    viewModelScope.launch { repository.addMessage(conversationId, "assistant", text) }
  }

  suspend fun loadConversationMessages(conversationId: String): List<MessageEntity> =
    repository.getMessages(conversationId)

  fun deleteConversation(conversation: ConversationEntity) {
    viewModelScope.launch {
      repository.deleteConversation(conversation)
      val newOffset = (allConversationsOffset - 1).coerceAtLeast(0)
      allConversationsOffset = newOffset
      _state.update { s ->
        val updated = s.allConversations.filter { it.id != conversation.id }
        s.copy(
          allConversations = updated,
          hasMore = newOffset < s.totalCount - 1,
        )
      }
    }
  }

  fun loadAllConversations() {
    allConversationsOffset = 0
    _state.update { it.copy(allConversations = emptyList(), isLoadingMore = true) }
    viewModelScope.launch {
      val total = repository.getTotalCount()
      val page = repository.getConversationsPage(0, PAGE_SIZE)
      allConversationsOffset = page.size
      _state.update {
        it.copy(
          allConversations = page,
          totalCount = total,
          hasMore = page.size < total,
          isLoadingMore = false,
        )
      }
    }
  }

  fun loadMoreConversations() {
    if (_state.value.isLoadingMore || !_state.value.hasMore) return
    _state.update { it.copy(isLoadingMore = true) }
    viewModelScope.launch {
      val page = repository.getConversationsPage(allConversationsOffset, PAGE_SIZE)
      allConversationsOffset += page.size
      _state.update { s ->
        s.copy(
          allConversations = s.allConversations + page,
          hasMore = allConversationsOffset < s.totalCount,
          isLoadingMore = false,
        )
      }
    }
  }
}
