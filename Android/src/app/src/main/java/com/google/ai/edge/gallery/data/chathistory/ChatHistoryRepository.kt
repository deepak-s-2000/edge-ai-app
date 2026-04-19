package com.google.ai.edge.gallery.data.chathistory

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Singleton
class ChatHistoryRepository @Inject constructor(private val dao: ChatHistoryDao) {

  fun getRecentConversations(limit: Int = 10): Flow<List<ConversationEntity>> =
    dao.getRecentConversations(limit)

  fun getConversationCountFlow(): Flow<Int> = dao.getConversationCountFlow()

  suspend fun getConversationsPage(offset: Int, limit: Int = 20): List<ConversationEntity> =
    withContext(Dispatchers.IO) { dao.getConversationsPage(offset, limit) }

  suspend fun getTotalCount(): Int =
    withContext(Dispatchers.IO) { dao.getConversationCount() }

  suspend fun createConversation(id: String, title: String, modelName: String) =
    withContext(Dispatchers.IO) {
      val now = System.currentTimeMillis()
      dao.insertConversation(
        ConversationEntity(
          id = id,
          title = title,
          modelName = modelName,
          createdAt = now,
          updatedAt = now,
        )
      )
    }

  suspend fun updateTitle(id: String, title: String) =
    withContext(Dispatchers.IO) { dao.updateTitle(id, title) }

  suspend fun addMessage(
    conversationId: String,
    role: String,
    text: String,
    tokensGenerated: Int = 0,
    latencyMs: Long = 0,
  ) = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    dao.insertMessage(
      MessageEntity(
        id = UUID.randomUUID().toString(),
        conversationId = conversationId,
        role = role,
        text = text,
        timestamp = now,
        tokensGenerated = tokensGenerated,
        latencyMs = latencyMs,
      )
    )
    dao.incrementMessageCount(id = conversationId, updatedAt = now)
  }

  suspend fun getMessages(conversationId: String): List<MessageEntity> =
    withContext(Dispatchers.IO) { dao.getMessages(conversationId) }

  suspend fun deleteConversation(conversation: ConversationEntity) =
    withContext(Dispatchers.IO) {
      dao.deleteMessages(conversation.id)
      dao.deleteConversation(conversation)
    }
}
