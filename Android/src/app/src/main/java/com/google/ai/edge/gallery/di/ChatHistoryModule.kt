package com.google.ai.edge.gallery.di

import android.content.Context
import androidx.room.Room
import com.google.ai.edge.gallery.data.chathistory.ChatHistoryDao
import com.google.ai.edge.gallery.data.chathistory.ChatHistoryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatHistoryModule {

  @Provides
  @Singleton
  fun provideChatHistoryDatabase(@ApplicationContext context: Context): ChatHistoryDatabase {
    return Room.databaseBuilder(context, ChatHistoryDatabase::class.java, "chat_history.db")
      .build()
  }

  @Provides
  @Singleton
  fun provideChatHistoryDao(db: ChatHistoryDatabase): ChatHistoryDao = db.dao()
}
