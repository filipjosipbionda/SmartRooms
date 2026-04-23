package com.benza.smartrooms.feature.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benza.smartrooms.R
import com.benza.smartrooms.ui.components.AuthFeedbackBanner
import com.benza.smartrooms.ui.components.AuthFeedbackType
import com.benza.smartrooms.ui.theme.Coral
import com.benza.smartrooms.ui.theme.Lagoon
import com.benza.smartrooms.ui.theme.SmartRoomsTheme
import org.koin.androidx.compose.koinViewModel

/**
 * Route-level home entry that binds [HomeViewModel] to the stateless home screen.
 */
@Composable
internal fun HomeRouteScreen(
    onProfileClick: () -> Unit,
    onRoomClick: (HomeRoomUiState) -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState.value,
        onProfileClick = onProfileClick,
        onRoomClick = onRoomClick,
        onShowCreateRoomDialog = viewModel::showCreateRoomDialog,
        onDismissCreateRoomDialog = viewModel::dismissCreateRoomDialog,
        onRoomNameChange = viewModel::onRoomNameChanged,
        onRoomTopicChange = viewModel::onRoomTopicChanged,
        onCreateRoomClick = viewModel::createRoom,
        onPreviousRoomsPageClick = viewModel::goToPreviousRoomsPage,
        onNextRoomsPageClick = viewModel::goToNextRoomsPage
    )
}

/**
 * Stateless home UI shown after authentication succeeds.
 */
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onProfileClick: () -> Unit,
    onRoomClick: (HomeRoomUiState) -> Unit,
    onShowCreateRoomDialog: () -> Unit,
    onDismissCreateRoomDialog: () -> Unit,
    onRoomNameChange: (String) -> Unit,
    onRoomTopicChange: (String) -> Unit,
    onCreateRoomClick: () -> Unit,
    onPreviousRoomsPageClick: () -> Unit,
    onNextRoomsPageClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = onShowCreateRoomDialog) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.action_open_create_room_dialog)
                )
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                HomeHeader(
                    profileInitials = uiState.profileInitials,
                    onProfileClick = onProfileClick
                )
            }
            item {
                JoinedRoomsHeroCard(
                    joinedRoomsCount = uiState.joinedRoomsCount,
                    unansweredQuizCount = uiState.unansweredQuizCount
                )
            }
            item {
                if (uiState.errorMessageRes != null) {
                    AuthFeedbackBanner(
                        message = stringResource(uiState.errorMessageRes),
                        type = AuthFeedbackType.Error
                    )
                } else if (uiState.infoMessageRes != null) {
                    AuthFeedbackBanner(
                        message = stringResource(uiState.infoMessageRes),
                        type = AuthFeedbackType.Success
                    )
                }
            }
            item {
                JoinedRoomsSection(
                    title = stringResource(R.string.home_joined_rooms_title),
                    subtitle = stringResource(R.string.home_joined_rooms_subtitle)
                ) {
                    when {
                        uiState.isLoadingRooms -> {
                            Text(
                                text = stringResource(R.string.home_rooms_loading),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        uiState.rooms.isEmpty() -> {
                            Text(
                                text = stringResource(R.string.home_rooms_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        else -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                uiState.pagedRooms.forEach { room ->
                                    RoomCard(
                                        room = room,
                                        onOpenRoomClick = { onRoomClick(room) }
                                    )
                                }
                                if (uiState.totalPages > 1) {
                                    RoomsPagination(
                                        currentPage = uiState.currentPage,
                                        totalPages = uiState.totalPages,
                                        onPreviousClick = onPreviousRoomsPageClick,
                                        onNextClick = onNextRoomsPageClick
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(88.dp))
            }
        }

        if (uiState.isCreateRoomDialogOpen) {
            CreateRoomDialog(
                uiState = uiState,
                onRoomNameChange = onRoomNameChange,
                onRoomTopicChange = onRoomTopicChange,
                onDismiss = onDismissCreateRoomDialog,
                onConfirm = onCreateRoomClick
            )
        }
    }
}

/**
 * Header section for the home screen.
 */
@Composable
private fun HomeHeader(
    profileInitials: String,
    onProfileClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_brand_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = stringResource(R.string.home_joined_rooms_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onProfileClick) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Lagoon, Coral)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profileInitials,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

/**
 * Highlight card for joined rooms and active sessions.
 */
@Composable
private fun JoinedRoomsHeroCard(
    joinedRoomsCount: Int,
    unansweredQuizCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Lagoon.copy(alpha = 0.16f),
                            Coral.copy(alpha = 0.14f)
                        )
                    )
                )
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.home_summary_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = stringResource(R.string.home_metric_joined_rooms),
                    value = joinedRoomsCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = stringResource(R.string.home_metric_unanswered_quizzes),
                    value = unansweredQuizCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Small metric card used by the home summary row.
 */
@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 104.dp)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

/**
 * Full-width joined rooms section header and content.
 */
@Composable
private fun JoinedRoomsSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

/**
 * Room card rendered from Firestore data.
 */
@Composable
private fun RoomCard(
    room: HomeRoomUiState,
    onOpenRoomClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.home_room_topic, room.topic),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RoomStatSurface(
                    label = stringResource(R.string.home_room_participants_label),
                    value = room.participantCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                RoomStatSurface(
                    label = stringResource(R.string.home_room_unanswered_quizzes_label),
                    value = room.unansweredQuizCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedButton(onClick = onOpenRoomClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_open_room))
            }
        }
    }
}

/**
 * Symmetric stat surface used inside room cards.
 */
@Composable
private fun RoomStatSurface(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Pagination controls for joined rooms.
 */
@Composable
private fun RoomsPagination(
    currentPage: Int,
    totalPages: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            modifier = Modifier.weight(1f),
            onClick = onPreviousClick,
            enabled = currentPage > 0
        ) {
            Text(stringResource(R.string.action_previous))
        }
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.home_pagination_indicator, currentPage + 1, totalPages),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        OutlinedButton(
            modifier = Modifier.weight(1f),
            onClick = onNextClick,
            enabled = currentPage < totalPages - 1
        ) {
            Text(stringResource(R.string.action_next))
        }
    }
}

/**
 * Dialog used to manually enter room details before Firestore creation.
 */
@Composable
private fun CreateRoomDialog(
    uiState: HomeUiState,
    onRoomNameChange: (String) -> Unit,
    onRoomTopicChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.create_room_dialog_title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.create_room_dialog_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = uiState.roomNameInput,
                    onValueChange = onRoomNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isCreatingRoom,
                    label = { Text(stringResource(R.string.label_room_name)) },
                    singleLine = true,
                    isError = uiState.roomNameErrorRes != null,
                    supportingText = uiState.roomNameErrorRes?.let {
                        { Text(stringResource(it)) }
                    }
                )
                OutlinedTextField(
                    value = uiState.roomTopicInput,
                    onValueChange = onRoomTopicChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isCreatingRoom,
                    label = { Text(stringResource(R.string.label_room_topic)) },
                    singleLine = true,
                    isError = uiState.roomTopicErrorRes != null,
                    supportingText = uiState.roomTopicErrorRes?.let {
                        { Text(stringResource(it)) }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !uiState.isCreatingRoom
            ) {
                if (uiState.isCreatingRoom) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                }
                Text(stringResource(R.string.action_create_room))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !uiState.isCreatingRoom
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/**
 * Preview for the home screen.
 */
@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SmartRoomsTheme {
        HomeScreen(
            uiState = HomeUiState(
                profileInitials = "FB",
                joinedRoomsCount = 6,
                unansweredQuizCount = 7,
                rooms = listOf(
                    HomeRoomUiState(
                        id = "1",
                        name = "Filip Room 1",
                        topic = "Firebase",
                        participantCount = 1,
                        unansweredQuizCount = 1
                    ),
                    HomeRoomUiState(
                        id = "2",
                        name = "Filip Room 2",
                        topic = "Kotlin",
                        participantCount = 4,
                        unansweredQuizCount = 0
                    ),
                    HomeRoomUiState(
                        id = "3",
                        name = "Filip Room 3",
                        topic = "Databases",
                        participantCount = 5,
                        unansweredQuizCount = 2
                    ),
                    HomeRoomUiState(
                        id = "4",
                        name = "Filip Room 4",
                        topic = "Compose",
                        participantCount = 3,
                        unansweredQuizCount = 1
                    ),
                    HomeRoomUiState(
                        id = "5",
                        name = "Filip Room 5",
                        topic = "AI",
                        participantCount = 2,
                        unansweredQuizCount = 3
                    ),
                    HomeRoomUiState(
                        id = "6",
                        name = "Filip Room 6",
                        topic = "Testing",
                        participantCount = 6,
                        unansweredQuizCount = 0
                    )
                ),
                pagedRooms = listOf(
                    HomeRoomUiState(
                        id = "1",
                        name = "Filip Room 1",
                        topic = "Firebase",
                        participantCount = 1,
                        unansweredQuizCount = 1
                    ),
                    HomeRoomUiState(
                        id = "2",
                        name = "Filip Room 2",
                        topic = "Kotlin",
                        participantCount = 4,
                        unansweredQuizCount = 0
                    ),
                    HomeRoomUiState(
                        id = "3",
                        name = "Filip Room 3",
                        topic = "Databases",
                        participantCount = 5,
                        unansweredQuizCount = 2
                    ),
                    HomeRoomUiState(
                        id = "4",
                        name = "Filip Room 4",
                        topic = "Compose",
                        participantCount = 3,
                        unansweredQuizCount = 1
                    )
                ),
                totalPages = 2,
                isLoadingRooms = false
            ),
            onProfileClick = {},
            onRoomClick = {},
            onShowCreateRoomDialog = {},
            onDismissCreateRoomDialog = {},
            onRoomNameChange = {},
            onRoomTopicChange = {},
            onCreateRoomClick = {},
            onPreviousRoomsPageClick = {},
            onNextRoomsPageClick = {}
        )
    }
}
