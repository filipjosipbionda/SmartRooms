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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
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
    onLoggedOut: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState.value,
        onCreateRoomClick = viewModel::createRoom,
        onLogoutClick = {
            viewModel.logout()
            onLoggedOut()
        }
    )
}

/**
 * Stateless home UI shown after authentication succeeds.
 */
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onCreateRoomClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            HeroCard(
                userName = uiState.userName.ifBlank { stringResource(R.string.sample_user_name) },
                featuredRoom = uiState.featuredRoomName.ifBlank {
                    stringResource(R.string.home_featured_room_fallback)
                },
                liveQuizCount = uiState.liveQuizCount,
                onLogoutClick = onLogoutClick
            )
        }
        item {
            MetricsRow(
                joinedRoomsCount = uiState.joinedRoomsCount,
                liveQuizCount = uiState.liveQuizCount
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
            SectionCard(
                title = stringResource(R.string.home_room_generator_title),
                subtitle = stringResource(R.string.home_room_generator_subtitle)
            ) {
                Button(
                    onClick = onCreateRoomClick,
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
                    Text(
                        text = stringResource(
                            if (uiState.isCreatingRoom) {
                                R.string.action_generating_room
                            } else {
                                R.string.action_generate_room
                            }
                        )
                    )
                }
            }
        }
        item {
            SectionCard(
                title = stringResource(R.string.home_rooms_title),
                subtitle = stringResource(R.string.home_rooms_subtitle)
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
                            uiState.rooms.forEach { room ->
                                RoomCard(room = room)
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Hero header for the home screen.
 */
@Composable
private fun HeroCard(
    userName: String,
    featuredRoom: String,
    liveQuizCount: Int,
    onLogoutClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Lagoon, Coral, Color(0xFF203B5D))
                )
            )
            .padding(24.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.home_greeting, userName),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Text(
                text = stringResource(R.string.home_featured_room_message, featuredRoom),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.84f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.home_live_quiz_sessions,
                            liveQuizCount,
                            liveQuizCount
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        color = Color.White
                    )
                }
                Button(onClick = onLogoutClick) {
                    Text(stringResource(R.string.action_log_out))
                }
            }
        }
    }
}

/**
 * Metric summary row for joined rooms and live quizzes.
 */
@Composable
private fun MetricsRow(
    joinedRoomsCount: Int,
    liveQuizCount: Int
) {
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
            title = stringResource(R.string.home_metric_live_quizzes),
            value = liveQuizCount.toString(),
            modifier = Modifier.weight(1f)
        )
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
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
 * Reusable content section card for home screen groups.
 */
@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
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
}

/**
 * Room card rendered from Firestore data.
 */
@Composable
private fun RoomCard(room: HomeRoomUiState) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.home_room_participants, room.participantCount),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = stringResource(R.string.home_room_live_quizzes, room.liveQuizCount),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
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
                userName = "Filip",
                featuredRoomName = "Filip Room 1",
                joinedRoomsCount = 2,
                liveQuizCount = 1,
                rooms = listOf(
                    HomeRoomUiState(
                        id = "1",
                        name = "Filip Room 1",
                        topic = "Firebase",
                        participantCount = 1,
                        liveQuizCount = 1
                    ),
                    HomeRoomUiState(
                        id = "2",
                        name = "Filip Room 2",
                        topic = "Kotlin",
                        participantCount = 4,
                        liveQuizCount = 0
                    )
                ),
                isLoadingRooms = false
            ),
            onCreateRoomClick = {},
            onLogoutClick = {}
        )
    }
}
