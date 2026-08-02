package com.benza.smartrooms.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

@Composable
internal fun AttachmentPreview(
    modifier: Modifier = Modifier,
    model: Any?,
    mimeType: String,
    fileName: String,
) {
    val context = LocalContext.current
    val source = model?.toString()
    val imageRequest =
        if (mimeType.startsWith("image/") && model != null) {
            remember(context, model) {
                ImageRequest
                    .Builder(context)
                    .data(model)
                    .crossfade(THUMBNAIL_CROSSFADE_MILLIS)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
        } else {
            null
        }
    var imagePreviewState by remember(imageRequest) {
        mutableStateOf(ImagePreviewState.Loading)
    }
    val initialPdfState =
        if (mimeType.isPdfMimeType() && source != null) {
            PdfThumbnailLoader
                .getCachedThumbnail(source)
                ?.let(PdfPreviewState::Loaded)
                ?: PdfPreviewState.Loading
        } else {
            PdfPreviewState.Unavailable
        }
    val pdfPreviewState by produceState<PdfPreviewState>(
        initialValue = initialPdfState,
        mimeType,
        source,
    ) {
        value =
            if (mimeType.isPdfMimeType() && source != null) {
                val thumbnail =
                    PdfThumbnailLoader.getCachedThumbnail(source)
                        ?: PdfThumbnailLoader.loadThumbnail(
                            context = context,
                            source = source,
                        )
                thumbnail?.let(PdfPreviewState::Loaded) ?: PdfPreviewState.Failed
            } else {
                PdfPreviewState.Unavailable
            }
    }

    when {
        imageRequest != null -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = fileName,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop,
                        onLoading = { imagePreviewState = ImagePreviewState.Loading },
                        onSuccess = { imagePreviewState = ImagePreviewState.Loaded },
                        onError = { imagePreviewState = ImagePreviewState.Failed },
                    )
                    when (imagePreviewState) {
                        ImagePreviewState.Failed -> {
                            FileAttachmentFallback(
                                modifier = Modifier.fillMaxSize(),
                                mimeType = mimeType,
                                fileName = fileName,
                            )
                        }

                        ImagePreviewState.Loading -> {
                            AttachmentPreviewLoadingPlaceholder(modifier = Modifier.fillMaxSize())
                        }

                        ImagePreviewState.Loaded -> Unit
                    }
                }
            }
        }

        pdfPreviewState is PdfPreviewState.Loaded -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ) {
                Image(
                    bitmap = (pdfPreviewState as PdfPreviewState.Loaded).bitmap.asImageBitmap(),
                    contentDescription = fileName,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        pdfPreviewState is PdfPreviewState.Loading -> {
            AttachmentPreviewLoadingPlaceholder(modifier = modifier)
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
private fun AttachmentPreviewLoadingPlaceholder(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val fallbackStyle = rememberFileFallbackStyle(fileName = fileName, mimeType = mimeType)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = fallbackStyle.containerColor,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = fallbackStyle.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = fallbackStyle.contentColor,
            )
            Text(
                text = fallbackStyle.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                color = fallbackStyle.contentColor,
            )
        }
    }
}

internal fun attachmentTypeLabel(
    fileName: String,
    mimeType: String,
): String = fileKindLabel(fileName = fileName, mimeType = mimeType)

@Composable
private fun rememberFileFallbackStyle(
    fileName: String,
    mimeType: String,
): FileFallbackStyle {
    val label = fileKindLabel(fileName = fileName, mimeType = mimeType)
    return when {
        mimeType.isPdfMimeType() ->
            FileFallbackStyle(
                label = label,
                icon = Icons.Outlined.PictureAsPdf,
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )

        mimeType.startsWith("image/") ->
            FileFallbackStyle(
                label = label,
                icon = Icons.Outlined.Image,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )

        mimeType.startsWith("audio/") ->
            FileFallbackStyle(
                label = label,
                icon = Icons.Outlined.AudioFile,
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )

        mimeType.startsWith("video/") ->
            FileFallbackStyle(
                label = label,
                icon = Icons.Outlined.VideoFile,
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )

        mimeType.isDocumentMimeType() ->
            FileFallbackStyle(
                label = label,
                icon = Icons.Outlined.Description,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.66f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )

        mimeType.isSpreadsheetMimeType() ->
            FileFallbackStyle(
                label = label,
                icon = Icons.Outlined.TableChart,
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )

        mimeType.isPresentationMimeType() ->
            FileFallbackStyle(
                label = label,
                icon = Icons.Outlined.Slideshow,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )

        mimeType.startsWith("text/") ->
            FileFallbackStyle(
                label = label,
                icon = Icons.AutoMirrored.Outlined.Article,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )

        else ->
            FileFallbackStyle(
                label = label,
                icon =
                    if (label == "FILE") {
                        Icons.AutoMirrored.Outlined.InsertDriveFile
                    } else {
                        Icons.Outlined.AttachFile
                    },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

private sealed interface PdfPreviewState {
    data object Unavailable : PdfPreviewState

    data object Loading : PdfPreviewState

    data object Failed : PdfPreviewState

    data class Loaded(
        val bitmap: Bitmap,
    ) : PdfPreviewState
}

private enum class ImagePreviewState {
    Loading,
    Loaded,
    Failed,
}

private data class FileFallbackStyle(
    val label: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
)

private fun fileKindLabel(
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
        isDocumentMimeType() -> "DOC"
        isSpreadsheetMimeType() -> "XLS"
        isPresentationMimeType() -> "PPT"
        startsWith("text/") -> "TXT"
        startsWith("audio/") -> "AUD"
        startsWith("video/") -> "VID"
        contains("zip") || contains("compressed") -> "ZIP"
        else -> "FILE"
    }

private fun String.isPdfMimeType(): Boolean = startsWith("application/pdf")

private fun String.isDocumentMimeType(): Boolean = contains("word") || contains("document") || endsWith("msword")

private fun String.isSpreadsheetMimeType(): Boolean = contains("sheet") || contains("excel")

private fun String.isPresentationMimeType(): Boolean = contains("presentation") || contains("powerpoint")

private const val THUMBNAIL_CROSSFADE_MILLIS = 140
