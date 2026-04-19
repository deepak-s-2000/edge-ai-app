package com.google.ai.edge.gallery.ui.edgeai

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

private const val MAX_DOCUMENT_CHARS = 12_000
private const val MAX_PDF_PAGES = 20

suspend fun buildAttachmentPromptContext(
  context: Context,
  attachments: List<EdgeAttachment>,
): String = withContext(Dispatchers.IO) {
  val documents = attachments.filterIsInstance<EdgeAttachment.Document>()
  if (documents.isEmpty()) return@withContext ""

  val sections = documents.mapNotNull { document ->
    val extractedText = extractDocumentText(context, document)
    when {
      extractedText.isBlank() && document.mimeType.contains("pdf", ignoreCase = true) -> {
        "Document: ${document.fileName}\nNo extractable text was found in this PDF. " +
          "It may be a scanned PDF/image-only document."
      }
      extractedText.isBlank() -> {
        "Document: ${document.fileName}\nThis file type is attached, but readable text could not be extracted."
      }
      else -> {
        "Document: ${document.fileName}\n${extractedText.trim()}"
      }
    }
  }

  if (sections.isEmpty()) {
    ""
  } else {
    "\n[Attached document contents]\n" + sections.joinToString("\n\n---\n\n")
  }
}

private fun extractDocumentText(
  context: Context,
  document: EdgeAttachment.Document,
): String {
  return when {
    document.mimeType.contains("pdf", ignoreCase = true) -> {
      extractPdfText(context, document.uri)
    }
    document.mimeType.startsWith("text/") ||
      document.mimeType.contains("json", ignoreCase = true) ||
      document.mimeType.contains("xml", ignoreCase = true) ||
      document.fileName.endsWith(".md", ignoreCase = true) ||
      document.fileName.endsWith(".csv", ignoreCase = true) ||
      document.fileName.endsWith(".log", ignoreCase = true) -> {
        extractPlainText(context, document.uri)
      }
    else -> ""
  }.take(MAX_DOCUMENT_CHARS)
}

private fun extractPlainText(context: Context, uri: Uri): String {
  return try {
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
      BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
        buildString {
          while (length < MAX_DOCUMENT_CHARS) {
            val line = reader.readLine() ?: break
            appendLine(line)
          }
        }
      }
    }.orEmpty()
  } catch (_: Exception) {
    ""
  }
}

private fun extractPdfText(context: Context, uri: Uri): String {
  return try {
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
      PDDocument.load(inputStream).use { document ->
        val stripper = PDFTextStripper().apply {
          startPage = 1
          endPage = minOf(document.numberOfPages, MAX_PDF_PAGES)
          sortByPosition = true
        }
        stripper.getText(document)
      }
    }.orEmpty()
  } catch (_: Exception) {
    ""
  }
}
