package com.google.ai.edge.gallery.data.chathistory

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "conversations")
data class ConversationEntity(
  @PrimaryKey val id: String,
  val title: String,
  val modelName: String,
  val createdAt: Long,
  val updatedAt: Long,
  val messageCount: Int = 0,
)

@Entity(tableName = "messages")
data class MessageEntity(
  @PrimaryKey val id: String,
  val conversationId: String,
  val role: String,
  val text: String,
  val timestamp: Long,
  val tokensGenerated: Int = 0,
  val latencyMs: Long = 0,
)

@Dao
interface ChatHistoryDao {

  @Query("SELECT * FROM conversations ORDER BY updatedAt DESC LIMIT :limit")
  fun getRecentConversations(limit: Int): Flow<List<ConversationEntity>>

  @Query("SELECT COUNT(*) FROM conversations")
  fun getConversationCountFlow(): Flow<Int>

  @Query("SELECT COUNT(*) FROM conversations")
  fun getConversationCount(): Int

  @Query("SELECT * FROM conversations ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset")
  fun getConversationsPage(offset: Int, limit: Int): List<ConversationEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertConversation(conversation: ConversationEntity)

  @Query("UPDATE conversations SET title = :title WHERE id = :id")
  fun updateTitle(id: String, title: String)

  @Query(
    "UPDATE conversations SET updatedAt = :updatedAt, messageCount = messageCount + 1 WHERE id = :id"
  )
  fun incrementMessageCount(id: String, updatedAt: Long)

  @Delete
  fun deleteConversation(conversation: ConversationEntity)

  @Query("DELETE FROM messages WHERE conversationId = :conversationId")
  fun deleteMessages(conversationId: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertMessage(message: MessageEntity)

  @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
  fun getMessages(conversationId: String): List<MessageEntity>
}

@Database(entities = [ConversationEntity::class, MessageEntity::class], version = 1, exportSchema = false)
abstract class ChatHistoryDatabase : RoomDatabase() {
  abstract fun dao(): ChatHistoryDao
}
