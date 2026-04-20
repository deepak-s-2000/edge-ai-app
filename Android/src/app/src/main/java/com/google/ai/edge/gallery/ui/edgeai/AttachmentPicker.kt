package com.google.ai.edge.gallery.ui.edgeai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.common.decodeSampledBitmapFromUri
import com.google.ai.edge.gallery.ui.theme.appFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "EdgeAttachmentPicker"

fun getPersistedEdgeAttachment(
  context: Context,
  uri: Uri,
  preferDocument: Boolean,
): EdgeAttachment {
  var name = "Unknown file"
  var size = 0L
  context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
    if (cursor.moveToFirst()) {
      val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
      if (nameIndex >= 0) {
        name = cursor.getString(nameIndex) ?: name
      }
      if (sizeIndex >= 0) {
        size = cursor.getLong(sizeIndex)
      }
    }
  }

  val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
  return if (!preferDocument || mimeType.startsWith("image/")) {
    EdgeAttachment.Image(
      uri = uri,
      fileName = name,
      mimeType = mimeType,
      sizeBytes = size,
    )
  } else {
    EdgeAttachment.Document(
      uri = uri,
      fileName = name,
      mimeType = mimeType,
      sizeBytes = size,
    )
  }
}

fun formatAttachmentSize(bytes: Long): String = when {
  bytes <= 0L -> ""
  bytes < 1_024L -> "${bytes}B"
  bytes < 1_048_576L -> "${bytes / 1_024}KB"
  else -> "${"%.1f".format(bytes / 1_048_576.0)}MB"
}

suspend fun loadImageAttachmentBitmaps(
  context: Context,
  attachments: List<EdgeAttachment>,
): List<Bitmap> = withContext(Dispatchers.IO) {
  attachments
    .filterIsInstance<EdgeAttachment.Image>()
    .mapNotNull { image ->
      try {
        decodeSampledBitmapFromUri(context, image.uri, 1024, 1024)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to decode image attachment: ${image.fileName}", e)
        null
      }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentPickerSheet(
  onDismiss: () -> Unit,
  onPickPhotos: () -> Unit,
  onPickDocuments: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = EdgeSurface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 24.dp),
    ) {
      Text(
        text = "Add attachment",
        color = EdgeText,
        fontSize = 16.sp,
        fontFamily = appFontFamily,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
      )

      ListItem(
        headlineContent = { Text("Photos", fontFamily = appFontFamily) },
        supportingContent = { Text("Choose up to 8 images", fontFamily = appFontFamily) },
        leadingContent = {
          Icon(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            tint = EdgeAccent,
          )
        },
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .clickable(onClick = onPickPhotos),
      )
      Box(modifier = Modifier.fillMaxWidth().height(0.dp))
      ListItem(
        headlineContent = { Text("Documents", fontFamily = appFontFamily) },
        supportingContent = {
          Text("PDF, Word, text, spreadsheets, and more", fontFamily = appFontFamily)
        },
        leadingContent = {
          Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = EdgeAccent,
          )
        },
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .clickable(onClick = onPickDocuments),
      )
    }
  }
}

@Composable
fun AttachmentPreviewRow(
  attachments: List<EdgeAttachment>,
  onRemove: (EdgeAttachment) -> Unit,
) {
  if (attachments.isEmpty()) return

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    attachments.forEach { attachment ->
      AttachmentPreviewChip(
        attachment = attachment,
        onRemove = { onRemove(attachment) },
      )
    }
  }
}

@Composable
private fun AttachmentPreviewChip(
  attachment: EdgeAttachment,
  onRemove: () -> Unit,
) {
  Box {
    when (attachment) {
      is EdgeAttachment.Image -> ImagePreviewChip(uri = attachment.uri)
      is EdgeAttachment.Document -> DocumentPreviewChip(attachment = attachment)
    }

    IconButton(
      onClick = onRemove,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .size(22.dp)
        .background(EdgeSurface, CircleShape),
    ) {
      Icon(
        imageVector = Icons.Default.Close,
        contentDescription = "Remove attachment",
        tint = EdgeTextDim,
        modifier = Modifier.size(12.dp),
      )
    }
  }
}

@Composable
private fun ImagePreviewChip(uri: Uri) {
  val context = LocalContext.current
  var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }

  LaunchedEffect(uri) {
    bitmap = withContext(Dispatchers.IO) {
      try {
        // 200px is plenty for a 72dp thumbnail — keeps memory low
        decodeSampledBitmapFromUri(context, uri, 200, 200)
      } catch (_: Exception) {
        null
      }
    }
  }

  Box(
    modifier = Modifier
      .size(72.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(EdgeSurface2)
      .border(1.dp, EdgeBorderStrong, RoundedCornerShape(12.dp)),
    contentAlignment = Alignment.Center,
  ) {
    val value = bitmap
    if (value == null) {
      Icon(
        imageVector = Icons.Default.Image,
        contentDescription = null,
        tint = EdgeTextMute,
      )
    } else {
      Image(
        bitmap = value.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

@Composable
private fun DocumentPreviewChip(attachment: EdgeAttachment.Document) {
  Row(
    modifier = Modifier
      .height(58.dp)
      .widthIn(min = 140.dp, max = 220.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(EdgeSurface2)
      .border(1.dp, EdgeBorderStrong, RoundedCornerShape(12.dp))
      .padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Icon(
      imageVector = Icons.Default.Description,
      contentDescription = null,
      tint = EdgeAccent,
      modifier = Modifier.size(20.dp),
    )
    Column {
      Text(
        text = attachment.fileName,
        color = EdgeText,
        fontSize = 12.sp,
        fontFamily = appFontFamily,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = formatAttachmentSize(attachment.sizeBytes),
        color = EdgeTextMute,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
      )
    }
  }
}

@Composable
fun AttachmentBubbles(attachments: List<EdgeAttachment>) {
  if (attachments.isEmpty()) return

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    attachments.forEach { attachment ->
      when (attachment) {
        is EdgeAttachment.Image -> SentImageBubble(uri = attachment.uri)
        is EdgeAttachment.Document -> SentDocumentBubble(attachment = attachment)
      }
    }
  }
}

@Composable
private fun SentImageBubble(uri: Uri) {
  val context = LocalContext.current
  var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }

  LaunchedEffect(uri) {
    bitmap = withContext(Dispatchers.IO) {
      try {
        // Use a bounded max size to avoid OOM on high-res camera photos.
        decodeSampledBitmapFromUri(context, uri, 800, 800)
      } catch (_: Exception) {
        null
      }
    }
  }

  val value = bitmap ?: return
  Image(
    bitmap = value.asImageBitmap(),
    contentDescription = null,
    contentScale = ContentScale.Crop,
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(max = 220.dp)
      .clip(RoundedCornerShape(12.dp)),
  )
}

@Composable
private fun SentDocumentBubble(attachment: EdgeAttachment.Document) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(EdgeSurface2)
      .border(1.dp, EdgeBorderStrong, RoundedCornerShape(12.dp))
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Icon(
      imageVector = Icons.Default.Description,
      contentDescription = null,
      tint = EdgeAccent,
      modifier = Modifier.size(18.dp),
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = attachment.fileName,
        color = EdgeText,
        fontSize = 13.sp,
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = buildString {
          append(formatAttachmentSize(attachment.sizeBytes))
          if (attachment.mimeType.isNotBlank()) {
            append(" · ")
            append(attachment.mimeType.substringAfterLast('/').uppercase())
          }
        },
        color = EdgeTextMute,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
      )
    }
  }
}
