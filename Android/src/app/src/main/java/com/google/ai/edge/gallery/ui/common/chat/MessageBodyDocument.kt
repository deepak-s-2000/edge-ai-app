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

package com.google.ai.edge.gallery.ui.common.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun MessageBodyDocument(message: ChatMessageDocument) {
  Row(
    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Icon(
      imageVector = documentIconForMimeType(message.mimeType),
      contentDescription = null,
      modifier = Modifier.size(32.dp),
    )
    Column {
      Text(
        text = message.fileName,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      if (message.sizeBytes > 0) {
        Text(
          text = formatFileSizeForDisplay(message.sizeBytes),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
      }
    }
  }
}

internal fun documentIconForMimeType(mimeType: String): ImageVector {
  return when {
    mimeType.contains("pdf") -> Icons.Rounded.PictureAsPdf
    mimeType.contains("spreadsheet") ||
      mimeType.contains("excel") ||
      mimeType.contains("csv") -> Icons.Rounded.TableChart
    else -> Icons.Rounded.Description
  }
}

internal fun formatFileSizeForDisplay(sizeBytes: Long): String {
  return when {
    sizeBytes <= 0 -> ""
    sizeBytes < 1024 -> "$sizeBytes B"
    sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
    else -> String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
  }
}
