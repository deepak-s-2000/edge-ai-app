package com.google.ai.edge.gallery.ui.edgeai

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.data.chathistory.ConversationEntity
import com.google.ai.edge.gallery.ui.theme.appFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatHistoryScreen(
  historyState: ChatHistoryState,
  onBack: () -> Unit,
  onConversationClick: (ConversationEntity) -> Unit,
  onDeleteConversation: (ConversationEntity) -> Unit,
  onLoadMore: () -> Unit,
  onScreenEnter: () -> Unit,
) {
  LaunchedEffect(Unit) { onScreenEnter() }

  val listState = rememberLazyListState()

  val shouldLoadMore by remember {
    derivedStateOf {
      val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()
      val total = listState.layoutInfo.totalItemsCount
      lastVisible != null && lastVisible.index >= total - 3 && historyState.hasMore
    }
  }

  LaunchedEffect(shouldLoadMore) {
    if (shouldLoadMore) onLoadMore()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(EdgeBg)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
      // Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack) {
          Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back",
            tint = EdgeTextDim,
          )
        }
        Spacer(Modifier.width(8.dp))
        Text(
          text = "Chat History",
          color = EdgeText,
          fontSize = 16.sp,
          fontFamily = appFontFamily,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        Text(
          text = "${historyState.totalCount} chats",
          color = EdgeTextMute,
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.width(16.dp))
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(EdgeBorderStrong)
      )

      if (historyState.allConversations.isEmpty() && !historyState.isLoadingMore) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "No conversations yet",
              color = EdgeTextMute,
              fontSize = 14.sp,
              fontFamily = appFontFamily,
            )
            Spacer(Modifier.height(6.dp))
            Text(
              text = "Start chatting to see history here",
              color = EdgeTextMute.copy(alpha = 0.6f),
              fontSize = 12.sp,
              fontFamily = appFontFamily,
            )
          }
        }
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize(),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
        ) {
          items(historyState.allConversations, key = { it.id }) { conv ->
            SwipeToDismissConversationItem(
              conversation = conv,
              onClick = { onConversationClick(conv) },
              onDelete = { onDeleteConversation(conv) },
            )
          }

          if (historyState.isLoadingMore) {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(24.dp),
                contentAlignment = Alignment.Center,
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(24.dp),
                  color = EdgeAccent,
                  strokeWidth = 2.dp,
                )
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissConversationItem(
  conversation: ConversationEntity,
  onClick: () -> Unit,
  onDelete: () -> Unit,
) {
  val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { value ->
      if (value == SwipeToDismissBoxValue.EndToStart) {
        onDelete()
        true
      } else false
    }
  )

  val bgColor by animateColorAsState(
    targetValue = when (dismissState.targetValue) {
      SwipeToDismissBoxValue.EndToStart -> Color(0xFF3B1010)
      else -> EdgeSurface
    },
    label = "dismissBg"
  )

  SwipeToDismissBox(
    state = dismissState,
    backgroundContent = {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(bgColor)
          .padding(end = 20.dp),
        contentAlignment = Alignment.CenterEnd,
      ) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "Delete",
          tint = Color(0xFFFF6B6B),
          modifier = Modifier.size(20.dp),
        )
      }
    },
    enableDismissFromStartToEnd = false,
  ) {
    ConversationListItem(conversation = conversation, onClick = onClick)
  }
}

@Composable
fun ConversationListItem(
  conversation: ConversationEntity,
  onClick: () -> Unit,
  showDeleteIcon: Boolean = false,
  onDelete: (() -> Unit)? = null,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(EdgeBg)
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Avatar
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .background(EdgeSurface2)
        .border(1.dp, EdgeBorderStrong, CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "E",
        color = EdgeAccent,
        fontSize = 13.sp,
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Bold,
      )
    }

    Spacer(Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = conversation.title,
        color = EdgeText,
        fontSize = 13.sp,
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(Modifier.height(2.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = conversation.updatedAt.toRelativeTime(),
          color = EdgeTextMute,
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
        )
        Text(
          text = "·",
          color = EdgeTextMute,
          fontSize = 11.sp,
        )
        Text(
          text = "${conversation.messageCount} msgs",
          color = EdgeTextMute,
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
        )
      }
    }

    if (showDeleteIcon && onDelete != null) {
      IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "Delete",
          tint = EdgeTextMute,
          modifier = Modifier.size(16.dp),
        )
      }
    }
  }
}

fun Long.toRelativeTime(): String {
  val diff = System.currentTimeMillis() - this
  return when {
    diff < 60_000L -> "Just now"
    diff < 3_600_000L -> "${diff / 60_000}m ago"
    diff < 86_400_000L -> "${diff / 3_600_000}h ago"
    diff < 604_800_000L -> "${diff / 86_400_000}d ago"
    else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(this))
  }
}
