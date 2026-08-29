package com.benza.smartrooms.feature.roominvite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benza.smartrooms.R
import com.benza.smartrooms.data.userprofile.model.UserRole
import com.benza.smartrooms.ui.components.AuthFeedbackBanner
import com.benza.smartrooms.ui.components.AuthFeedbackType
import com.benza.smartrooms.ui.components.UserAvatar
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun RoomInviteRouteScreen(
    roomId: String,
    roomName: String,
    onBackClick: () -> Unit,
    viewModel: RoomInviteViewModel =
        koinViewModel(
            parameters = { parametersOf(roomId, roomName) },
        ),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    RoomInviteScreen(
        uiState = uiState.value,
        onBackClick = onBackClick,
        onQueryChange = viewModel::onQueryChanged,
        onSendInviteClick = viewModel::sendInvite,
        onInfoMessageShown = viewModel::consumeInfoMessage,
    )
}

@Composable
internal fun RoomInviteScreen(
    uiState: RoomInviteUiState,
    onBackClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSendInviteClick: (RoomInviteSearchUserUiState) -> Unit,
    onInfoMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val infoMessage = uiState.infoMessageRes?.let { stringResource(it) }

    LaunchedEffect(infoMessage) {
        val message = infoMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onInfoMessageShown()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.room_invite_screen_title, uiState.roomName),
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.room_invite_screen_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (uiState.errorMessageRes != null) {
                AuthFeedbackBanner(
                    message = stringResource(uiState.errorMessageRes),
                    type = AuthFeedbackType.Error,
                )
            }

            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.label_user_search)) },
                placeholder = { Text(stringResource(R.string.room_invite_search_placeholder)) },
                singleLine = true,
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    uiState.query.length < 2 -> {
                        item {
                            PlaceholderCard(text = stringResource(R.string.room_invite_search_hint))
                        }
                    }

                    uiState.isSearching -> {
                        item {
                            LoadingCard(text = stringResource(R.string.room_invite_search_loading))
                        }
                    }

                    uiState.results.isEmpty() -> {
                        item {
                            PlaceholderCard(
                                text =
                                    stringResource(
                                        uiState.emptyMessageRes ?: R.string.room_invite_search_empty,
                                    ),
                            )
                        }
                    }

                    else -> {
                        items(
                            items = uiState.results,
                            key = RoomInviteSearchUserUiState::uid,
                        ) { user ->
                            InviteUserCard(
                                user = user,
                                isSendingInvite = uiState.isSendingInvite,
                                onSendInviteClick = { onSendInviteClick(user) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteUserCard(
    user: RoomInviteSearchUserUiState,
    isSendingInvite: Boolean,
    onSendInviteClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(
                displayName = user.displayName,
                photoUrl = user.photoUrl,
                modifier = Modifier.size(54.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(user.roleLabelRes()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onSendInviteClick,
                enabled = !isSendingInvite,
            ) {
                Text(stringResource(R.string.action_send_invite))
            }
        }
    }
}

@Composable
private fun PlaceholderCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoadingCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private fun RoomInviteSearchUserUiState.roleLabelRes(): Int =
    when (role) {
        UserRole.TEACHER -> R.string.profile_role_professor
        UserRole.STUDENT -> R.string.profile_role_student
        null -> R.string.profile_role_not_selected
    }
