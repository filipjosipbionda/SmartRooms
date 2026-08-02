package com.benza.smartrooms.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.benza.smartrooms.ui.theme.Coral
import com.benza.smartrooms.ui.theme.Lagoon

@Composable
internal fun UserAvatar(
    displayName: String,
    photoUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageModel = photoUrl?.takeIf(String::isNotBlank)
    val imageRequest =
        imageModel?.let {
            remember(context, it) {
                ImageRequest
                    .Builder(context)
                    .data(it)
                    .crossfade(AVATAR_CROSSFADE_MILLIS)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
        }

    Box(
        modifier =
            modifier
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Lagoon, Coral),
                    ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        AvatarInitials(displayName = displayName)
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun AvatarInitials(displayName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayName.toInitials(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

internal fun String.toInitials(): String =
    split(" ")
        .filter(String::isNotBlank)
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifBlank { "?" }

private const val AVATAR_CROSSFADE_MILLIS = 180
