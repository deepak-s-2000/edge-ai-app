/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.ui.edgeai

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.data.MAX_ATTACHMENT_COUNT
import com.google.ai.edge.gallery.data.chathistory.ConversationEntity
import com.google.ai.edge.gallery.ui.theme.appFontFamily

// ── Top-level chat screen ─────────────────────────────────────────────────────

@Composable
fun EdgeChatScreen(
  messages: List<Message>,
  streaming: Boolean,
  streamingText: String,
  activeModelName: String,
  drawerOpen: Boolean,
  historyItems: List<ConversationEntity>,
  hasMoreHistory: Boolean,
  currentConversationId: String?,
  onMenuClick: () -> Unit,
  onDrawerClose: () -> Unit,
  onNewChat: () -> Unit,
  onSend: (String, List<EdgeAttachment>) -> Unit,
  onModelChipClick: () -> Unit,
  onVoiceClick: () -> Unit,
  onModelsNav: () -> Unit,
  onSettingsNav: () -> Unit,
  onHistoryItemClick: (ConversationEntity) -> Unit,
  onDeleteConversation: (ConversationEntity) -> Unit,
  onMoreHistory: () -> Unit,
) {
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
        .imePadding()
    ) {
      EdgeChatHeader(
        activeModelName = activeModelName,
        onMenuClick = onMenuClick,
        onNewChatClick = onNewChat,
        onModelChipClick = onModelChipClick,
      )

      Box(modifier = Modifier.weight(1f)) {
        if (messages.isEmpty() && !streaming) {
          EdgeChatLanding(onSuggestion = { onSend(it, emptyList()) })
        } else {
          EdgeChatStream(
            messages = messages,
            streaming = streaming,
            streamingText = streamingText,
          )
        }
      }

      EdgeComposer(
        onSend = onSend,
        onVoiceClick = onVoiceClick,
        streaming = streaming,
        activeModelName = activeModelName,
      )
    }

    // Drawer overlay
    if (drawerOpen) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.6f))
          .clickable(onClick = onDrawerClose)
      )
      EdgeDrawer(
        activeModelName = activeModelName,
        historyItems = historyItems,
        hasMoreHistory = hasMoreHistory,
        currentConversationId = currentConversationId,
        onClose = onDrawerClose,
        onNewChat = {
          onNewChat()
          onDrawerClose()
        },
        onHistoryItemClick = { conv ->
          onHistoryItemClick(conv)
          // drawer close happens inside openConversation()
        },
        onDeleteConversation = onDeleteConversation,
        onMoreHistory = {
          onDrawerClose()
          onMoreHistory()
        },
        onModelsNav = {
          onDrawerClose()
          onModelsNav()
        },
        onSettingsNav = {
          onDrawerClose()
          onSettingsNav()
        },
      )
    }
  }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
fun EdgeChatHeader(
  activeModelName: String,
  onMenuClick: () -> Unit,
  onNewChatClick: () -> Unit,
  onModelChipClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(56.dp)
      .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    IconButton(onClick = onMenuClick) {
      Icon(
        imageVector = Icons.Default.Menu,
        contentDescription = "Menu",
        tint = EdgeTextDim,
        modifier = Modifier.size(22.dp),
      )
    }

    Spacer(Modifier.weight(1f))

    // Model pill
    Row(
      modifier = Modifier
        .clip(RoundedCornerShape(50.dp))
        .background(EdgeSurface)
        .border(1.dp, EdgeBorderStrong, RoundedCornerShape(50.dp))
        .clickable(onClick = onModelChipClick)
        .padding(horizontal = 14.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Box(
        modifier = Modifier
          .size(7.dp)
          .clip(CircleShape)
          .background(EdgeAccent)
      )
      Text(
        text = activeModelName,
        color = EdgeText,
        fontSize = 13.sp,
        fontFamily = appFontFamily,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Icon(
        imageVector = Icons.Default.KeyboardArrowDown,
        contentDescription = null,
        tint = EdgeTextMute,
        modifier = Modifier.size(16.dp),
      )
    }

    Spacer(Modifier.weight(1f))

    IconButton(onClick = onNewChatClick) {
      Icon(
        imageVector = Icons.Default.Add,
        contentDescription = "New chat",
        tint = EdgeTextDim,
        modifier = Modifier.size(22.dp),
      )
    }
  }
}

// ── Landing (empty state) ────────────────────────────────────────────────────

private data class Suggestion(val emoji: String, val text: String, val tag: String)

private val SUGGESTIONS = listOf(
  Suggestion("✦", "Explain a concept simply", "WRITE"),
  Suggestion("🖼", "Describe an image I take", "SEE"),
  Suggestion("📄", "Summarize a PDF", "READ"),
  Suggestion("✨", "Generate an image", "MAKE"),
)

@Composable
fun EdgeChatLanding(onSuggestion: (String) -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    EdgeMarkLogo(size = 48.dp)

    Spacer(Modifier.height(20.dp))

    Text(
      text = "What can I help with?",
      color = EdgeText,
      fontSize = 22.sp,
      fontFamily = appFontFamily,
      fontWeight = FontWeight.Bold,
    )

    Spacer(Modifier.height(6.dp))

    Text(
      text = "On-device · Offline",
      color = EdgeTextMute,
      fontSize = 11.sp,
      fontFamily = FontFamily.Monospace,
      letterSpacing = 1.sp,
    )

    Spacer(Modifier.height(36.dp))

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      SUGGESTIONS.forEach { s ->
        SuggestionButton(
          emoji = s.emoji,
          text = s.text,
          tag = s.tag,
          onClick = { onSuggestion(s.text) },
        )
      }
    }
  }
}

@Composable
private fun SuggestionButton(
  emoji: String,
  text: String,
  tag: String,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(EdgeRadius))
      .background(EdgeSurface)
      .border(1.dp, EdgeBorder, RoundedCornerShape(EdgeRadius))
      .clickable(onClick = onClick)
      .padding(horizontal = 18.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = emoji,
      fontSize = 18.sp,
    )
    Spacer(Modifier.width(12.dp))
    Text(
      text = text,
      color = EdgeTextDim,
      fontSize = 14.sp,
      fontFamily = appFontFamily,
      modifier = Modifier.weight(1f),
    )
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(4.dp))
        .background(EdgeSurface2)
        .border(1.dp, EdgeBorderStrong, RoundedCornerShape(4.dp))
        .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
      Text(
        text = tag,
        color = EdgeTextMute,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp,
      )
    }
  }
}

// ── Message stream ────────────────────────────────────────────────────────────

@Composable
fun EdgeChatStream(
  messages: List<Message>,
  streaming: Boolean,
  streamingText: String,
) {
  val listState = rememberLazyListState()
  val streamingMsgId = remember { "__streaming__" }
  val allMessages = remember(messages, streaming, streamingText) {
    if (streaming) {
      messages + Message(id = streamingMsgId, role = "assistant", text = streamingText)
    } else {
      messages
    }
  }

  LaunchedEffect(allMessages.size, streamingText) {
    if (allMessages.isNotEmpty()) {
      listState.animateScrollToItem(allMessages.size - 1)
    }
  }

  LazyColumn(
    state = listState,
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(vertical = 16.dp),
  ) {
    items(allMessages, key = { it.id }) { msg ->
      val isCurrentlyStreaming = streaming && msg == allMessages.last() && msg.role == "assistant"
      MessageBubble(
        message = msg,
        isStreaming = isCurrentlyStreaming,
      )
    }
  }
}

@Composable
private fun MessageBubble(
  message: Message,
  isStreaming: Boolean,
) {
  if (message.role == "user") {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End,
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(0.82f)
          .clip(RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp))
          .background(Color(0xFF1F1208))
          .border(1.dp, EdgeAccent.copy(alpha = 0.4f), RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp))
          .padding(horizontal = 16.dp, vertical = 10.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          AttachmentBubbles(message.attachments)
          if (message.text.isNotBlank()) {
            Text(
              text = message.text,
              color = EdgeText,
              fontSize = 14.sp,
              fontFamily = appFontFamily,
              lineHeight = 20.sp,
              softWrap = true,
            )
          }
        }
      }
    }
  } else {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Start,
      verticalAlignment = Alignment.Top,
    ) {
      // Avatar
      Box(
        modifier = Modifier
          .size(30.dp)
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

      Spacer(Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = message.text,
          color = EdgeText,
          fontSize = 14.sp,
          fontFamily = appFontFamily,
          lineHeight = 22.sp,
          softWrap = true,
        )

        if (isStreaming) {
          StreamingCursor()
        } else {
          Spacer(Modifier.height(4.dp))
        }
      }
    }
  }
}

@Composable
private fun StreamingCursor() {
  val infiniteTransition = rememberInfiniteTransition(label = "cursor")
  val alpha by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(500, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "cursorAlpha",
  )
  Spacer(Modifier.height(4.dp))
  Box(
    modifier = Modifier
      .size(width = 8.dp, height = 14.dp)
      .background(EdgeAccent.copy(alpha = alpha))
  )
}

// ── Composer ──────────────────────────────────────────────────────────────────

@Composable
fun EdgeComposer(
  onSend: (String, List<EdgeAttachment>) -> Unit,
  onVoiceClick: () -> Unit,
  streaming: Boolean,
  activeModelName: String,
) {
  val context = LocalContext.current
  var text by remember { mutableStateOf("") }
  var attachments by remember { mutableStateOf<List<EdgeAttachment>>(emptyList()) }
  var showAttachmentPicker by remember { mutableStateOf(false) }

  val remainingSlots = (MAX_ATTACHMENT_COUNT - attachments.size).coerceAtLeast(0)

  val imagePickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      val data = result.data ?: return@rememberLauncherForActivityResult
      val uris = buildList {
        data.clipData?.let { clipData ->
          for (index in 0 until clipData.itemCount) {
            add(clipData.getItemAt(index).uri)
          }
        } ?: data.data?.let { add(it) }
      }
      if (uris.isNotEmpty()) {
        attachments = attachments + uris
          .take(remainingSlots)
          .map { uri ->
            takePersistablePermission(context, data, uri)
            getPersistedEdgeAttachment(context, uri, preferDocument = false)
          }
      }
    }

  val documentPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      val data = result.data ?: return@rememberLauncherForActivityResult
      val uris = buildList {
        data.clipData?.let { clipData ->
          for (index in 0 until clipData.itemCount) {
            add(clipData.getItemAt(index).uri)
          }
        } ?: data.data?.let { add(it) }
      }
      if (uris.isNotEmpty()) {
        attachments = attachments + uris
          .take(remainingSlots)
          .map { uri ->
            takePersistablePermission(context, data, uri)
            getPersistedEdgeAttachment(context, uri, preferDocument = true)
          }
      }
    }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(EdgeBg)
  ) {
    // Input box
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp)
        .clip(RoundedCornerShape(18.dp))
        .background(EdgeSurface)
        .border(1.dp, EdgeBorderStrong, RoundedCornerShape(18.dp))
        .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
      Column {
        AttachmentPreviewRow(
          attachments = attachments,
          onRemove = { removed -> attachments = attachments.filterNot { it.id == removed.id } },
        )
        if (attachments.isNotEmpty()) {
          Spacer(Modifier.height(10.dp))
        }

        BasicTextField(
          value = text,
          onValueChange = { text = it },
          modifier = Modifier.fillMaxWidth(),
          textStyle = TextStyle(
            color = EdgeText,
            fontSize = 14.sp,
            fontFamily = appFontFamily,
            lineHeight = 20.sp,
          ),
          cursorBrush = SolidColor(EdgeAccent),
          maxLines = 6,
          decorationBox = { innerTextField ->
            Box {
              if (text.isEmpty()) {
                Text(
                  text = "Message…",
                  color = EdgeTextMute,
                  fontSize = 14.sp,
                  fontFamily = appFontFamily,
                  lineHeight = 20.sp,
                )
              }
              innerTextField()
            }
          },
        )

        Spacer(Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(EdgeSurface2)
              .border(1.dp, EdgeBorderStrong, CircleShape)
              .clickable(enabled = remainingSlots > 0) { showAttachmentPicker = true },
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Attach",
              tint = EdgeTextDim,
              modifier = Modifier.size(18.dp),
            )
          }

          Spacer(Modifier.width(8.dp))

          if (attachments.isNotEmpty()) {
            Text(
              text = "${attachments.size}/$MAX_ATTACHMENT_COUNT",
              color = EdgeTextMute,
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.width(8.dp))
          }

          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(EdgeSurface2)
              .border(1.dp, EdgeBorderStrong, CircleShape)
              .clickable(onClick = onVoiceClick),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.Mic,
              contentDescription = "Voice",
              tint = EdgeTextDim,
              modifier = Modifier.size(18.dp),
            )
          }

          Spacer(Modifier.weight(1f))

          // Send button
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(
                if ((text.isNotBlank() || attachments.isNotEmpty()) && !streaming) {
                  EdgeAccent
                } else {
                  EdgeSurface2
                }
              )
              .border(
                1.dp,
                if ((text.isNotBlank() || attachments.isNotEmpty()) && !streaming) {
                  Color.Transparent
                } else {
                  EdgeBorderStrong
                },
                CircleShape
              )
              .clickable {
                if ((text.isNotBlank() || attachments.isNotEmpty()) && !streaming) {
                  onSend(text, attachments)
                  text = ""
                  attachments = emptyList()
                }
              },
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.Send,
              contentDescription = "Send",
              tint = if ((text.isNotBlank() || attachments.isNotEmpty()) && !streaming) {
                Color.Black
              } else {
                EdgeTextMute
              },
              modifier = Modifier.size(17.dp),
            )
          }
        }
      }
    }

    Spacer(Modifier.height(8.dp))

    // Perf bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Box(
        modifier = Modifier
          .size(6.dp)
          .clip(CircleShape)
          .background(EdgeAccent)
      )
      Spacer(Modifier.width(6.dp))
      Text(
        text = "$activeModelName · on-device",
        color = EdgeTextMute,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Spacer(Modifier.height(8.dp))
  }

  if (showAttachmentPicker) {
    AttachmentPickerSheet(
      onDismiss = { showAttachmentPicker = false },
      onPickPhotos = {
        showAttachmentPicker = false
        imagePickerLauncher.launch(
          Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
          }
        )
      },
      onPickDocuments = {
        showAttachmentPicker = false
        documentPickerLauncher.launch(
          Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(
              Intent.EXTRA_MIME_TYPES,
              arrayOf(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain",
                "text/csv",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
              ),
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
          }
        )
      },
    )
  }
}

private fun takePersistablePermission(
  context: android.content.Context,
  intent: Intent,
  uri: android.net.Uri,
) {
  val modeFlags = intent.flags and
    (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
  if (modeFlags == 0) return
  try {
    context.contentResolver.takePersistableUriPermission(uri, modeFlags)
  } catch (_: SecurityException) {
  }
}

// ── Drawer ────────────────────────────────────────────────────────────────────

@Composable
fun EdgeDrawer(
  activeModelName: String,
  historyItems: List<ConversationEntity>,
  hasMoreHistory: Boolean,
  currentConversationId: String?,
  onClose: () -> Unit,
  onNewChat: () -> Unit,
  onHistoryItemClick: (ConversationEntity) -> Unit,
  onDeleteConversation: (ConversationEntity) -> Unit,
  onMoreHistory: () -> Unit,
  onModelsNav: () -> Unit,
  onSettingsNav: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxHeight()
      .width(300.dp)
      .background(EdgeSurface)
      .border(1.dp, EdgeBorderStrong, RoundedCornerShape(0.dp))
      .windowInsetsPadding(WindowInsets.statusBars)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // ── Fixed top section ──────────────────────────────────────────────────
      Column(modifier = Modifier.padding(16.dp)) {
        // Logo
        Row(verticalAlignment = Alignment.CenterVertically) {
          EdgeMarkLogo(size = 32.dp)
          Spacer(Modifier.width(10.dp))
          Text(
            text = "edge · ai",
            color = EdgeText,
            fontSize = 16.sp,
            fontFamily = appFontFamily,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
          )
        }

        Spacer(Modifier.height(20.dp))

        // Search bar (placeholder)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(EdgeRadiusSm))
            .background(EdgeSurface2)
            .border(1.dp, EdgeBorderStrong, RoundedCornerShape(EdgeRadiusSm))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
          Text(
            text = "Search conversations…",
            color = EdgeTextMute,
            fontSize = 13.sp,
            fontFamily = appFontFamily,
          )
        }

        Spacer(Modifier.height(12.dp))

        // New chat
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(EdgeRadiusSm))
            .background(EdgeAccentSoft)
            .border(1.dp, EdgeAccentBorder, RoundedCornerShape(EdgeRadiusSm))
            .clickable(onClick = onNewChat)
            .padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = EdgeAccent,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "New chat",
            color = EdgeAccent,
            fontSize = 13.sp,
            fontFamily = appFontFamily,
            fontWeight = FontWeight.SemiBold,
          )
        }

        Spacer(Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "RECENT",
            color = EdgeTextMute,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            modifier = Modifier.weight(1f),
          )
          if (historyItems.isEmpty()) {
            Text(
              text = "No history",
              color = EdgeTextMute.copy(alpha = 0.5f),
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
            )
          }
        }

        Spacer(Modifier.height(4.dp))
      }

      // ── Scrollable history list ────────────────────────────────────────────
      LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
      ) {
        items(historyItems, key = { it.id }) { conv ->
          DrawerHistoryItem(
            conversation = conv,
            isActive = conv.id == currentConversationId,
            onClick = { onHistoryItemClick(conv) },
            onDelete = { onDeleteConversation(conv) },
          )
        }

        if (hasMoreHistory) {
          item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onMoreHistory)
                .padding(horizontal = 8.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = null,
                tint = EdgeTextMute,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = "View all history",
                color = EdgeTextMute,
                fontSize = 12.sp,
                fontFamily = appFontFamily,
              )
            }
          }
        }
      }

      // ── Fixed bottom section ───────────────────────────────────────────────
      Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(EdgeBorderStrong)
        )

        Spacer(Modifier.height(12.dp))

        // Model hub
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onModelsNav)
            .padding(horizontal = 4.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(text = "⬡", color = EdgeTextDim, fontSize = 16.sp)
          Text(
            text = "Model hub",
            color = EdgeTextDim,
            fontSize = 14.sp,
            fontFamily = appFontFamily,
          )
        }

        // Settings
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onSettingsNav)
            .padding(horizontal = 4.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(text = "⚙", color = EdgeTextDim, fontSize = 16.sp)
          Text(
            text = "Settings",
            color = EdgeTextDim,
            fontSize = 14.sp,
            fontFamily = appFontFamily,
          )
        }

        Spacer(Modifier.height(8.dp))

        // Footer
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(EdgeSurface2)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(EdgeAccentSoft)
                .border(1.dp, EdgeAccentBorder, CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "ME",
                color = EdgeAccent,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
              )
            }
            Spacer(Modifier.width(10.dp))
            Column {
              Text(
                text = "Local device",
                color = EdgeText,
                fontSize = 12.sp,
                fontFamily = appFontFamily,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = "No account",
                color = EdgeTextMute,
                fontSize = 11.sp,
                fontFamily = appFontFamily,
              )
            }
          }
        }

        Spacer(Modifier.height(8.dp))
      }
    }
  }
}

// ── Drawer history item ────────────────────────────────────────────────────────

@Composable
private fun DrawerHistoryItem(
  conversation: ConversationEntity,
  isActive: Boolean,
  onClick: () -> Unit,
  onDelete: () -> Unit,
) {
  var showDelete by remember { mutableStateOf(false) }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(6.dp))
      .background(
        if (isActive) EdgeAccentSoft else Color.Transparent
      )
      .pointerInput(Unit) {
        detectTapGestures(
          onTap = { onClick() },
          onLongPress = { showDelete = !showDelete },
        )
      }
      .padding(horizontal = 4.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = conversation.title,
        color = if (isActive) EdgeAccent else EdgeTextDim,
        fontSize = 13.sp,
        fontFamily = appFontFamily,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = conversation.updatedAt.toRelativeTime(),
        color = EdgeTextMute,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
      )
    }

    if (showDelete) {
      IconButton(
        onClick = {
          showDelete = false
          onDelete()
        },
        modifier = Modifier.size(28.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "Delete",
          tint = Color(0xFFFF6B6B),
          modifier = Modifier.size(14.dp),
        )
      }
    }
  }
}

// ── EdgeMark logo composable ──────────────────────────────────────────────────

@Composable
fun EdgeMarkLogo(size: androidx.compose.ui.unit.Dp) {
  Box(
    modifier = Modifier
      .size(size)
      .clip(RoundedCornerShape(size * 0.28f))
      .background(EdgeSurface2)
      .border(
        width = (size.value * 0.04f).dp,
        color = EdgeAccent,
        shape = RoundedCornerShape(size * 0.28f),
      ),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = "E",
      color = EdgeAccent,
      fontSize = (size.value * 0.45f).sp,
      fontFamily = appFontFamily,
      fontWeight = FontWeight.ExtraBold,
    )
  }
}
