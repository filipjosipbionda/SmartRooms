package com.benza.smartrooms.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter

@Composable
internal fun AttachmentPreview(
    modifier: Modifier = Modifier,
    model: Any?,
    mimeType: String,
    fileName: String,
) {
    val context = LocalContext.current
    val imagePainter =
        if (mimeType.startsWith("image/") && model != null) {
            rememberAsyncImagePainter(model = model)
        } else {
            null
        }
    val pdfThumbnail by produceState<Bitmap?>(initialValue = null, mimeType, model) {
        value =
            if (mimeType.isPdfMimeType() && model != null) {
                PdfThumbnailLoader.loadThumbnail(
                    context = context,
                    source = model.toString(),
                )
            } else {
                null
            }
    }

    when {
        imagePainter != null -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (imagePainter.state is AsyncImagePainter.State.Success) {
                        Image(
                            painter = imagePainter,
                            contentDescription = fileName,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        FileAttachmentFallback(
                            modifier = Modifier.fillMaxSize(),
                            mimeType = mimeType,
                            fileName = fileName,
                        )
                    }
                }
            }
        }

        pdfThumbnail != null -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ) {
                Image(
                    bitmap = pdfThumbnail!!.asImageBitmap(),
                    contentDescription = fileName,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        else -> {
            FileAttachmentFallback(
                modifier = modifier,
                mimeType = mimeType,
                fileName = fileName,
            )
        }
    }
}

@Composable
private fun FileAttachmentFallback(
    modifier: Modifier = Modifier,
    mimeType: String,
    fileName: String,
) {
    val extensionLabel =
        fileName
            .substringAfterLast('.', "")
            .uppercase()
            .take(4)
            .ifBlank { mimeType.toFileKindLabel() }
    val fallbackBrush =
        when {
            mimeType.isPdfMimeType() ->
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.78f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
                    ),
                )
            mimeType.startsWith("image/") ->
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.82f),
                    ),
                )
            else ->
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
                    ),
                )
        }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(fallbackBrush)
                    .padding(8.dp),
        ) {
            Surface(
                modifier = Modifier.align(Alignment.TopStart),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
            ) {
                Icon(
                    imageVector =
                        if (mimeType.startsWith("image/")) {
                            Icons.Outlined.Image
                        } else {
                            Icons.Outlined.AttachFile
                        },
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp).size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Column(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                ) {
                    Text(
                        text = extensionLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = mimeType.toFileKindLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f),
                )
            }
        }
    }
}

internal fun attachmentTypeLabel(
    fileName: String,
    mimeType: String,
): String =
    fileName
        .substringAfterLast('.', "")
        .uppercase()
        .take(4)
        .ifBlank { mimeType.toFileKindLabel() }

private fun String.toFileKindLabel(): String =
    when {
        isPdfMimeType() -> "PDF"
        contains("word") -> "DOC"
        contains("sheet") || contains("excel") -> "XLS"
        contains("presentation") || contains("powerpoint") -> "PPT"
        startsWith("text/") -> "TXT"
        else -> "FILE"
    }

private fun String.isPdfMimeType(): Boolean = startsWith("application/pdf")
