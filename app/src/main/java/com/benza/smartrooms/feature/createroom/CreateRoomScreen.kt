package com.benza.smartrooms.feature.createroom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benza.smartrooms.R
import com.benza.smartrooms.ui.components.AuthFeedbackBanner
import com.benza.smartrooms.ui.components.AuthFeedbackType
import com.benza.smartrooms.ui.theme.Coral
import com.benza.smartrooms.ui.theme.Lagoon
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun CreateRoomRouteScreen(
    onBackClick: () -> Unit,
    onRoomCreated: () -> Unit,
    viewModel: CreateRoomViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.value.isRoomCreated) {
        if (uiState.value.isRoomCreated) {
            viewModel.consumeRoomCreated()
            onRoomCreated()
        }
    }

    CreateRoomScreen(
        uiState = uiState.value,
        onBackClick = onBackClick,
        onRoomNameChange = viewModel::onRoomNameChanged,
        onRoomTopicChange = viewModel::onRoomTopicChanged,
        onCefrLevelSelected = viewModel::onCefrLevelSelected,
        onCreateRoomClick = viewModel::createRoom,
    )
}

@Composable
internal fun CreateRoomScreen(
    uiState: CreateRoomUiState,
    onBackClick: () -> Unit,
    onRoomNameChange: (String) -> Unit,
    onRoomTopicChange: (String) -> Unit,
    onCefrLevelSelected: (String) -> Unit,
    onCreateRoomClick: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CreateRoomTopBar(onBackClick = onBackClick)
            }
            item {
                if (uiState.errorMessageRes != null) {
                    AuthFeedbackBanner(
                        message = stringResource(uiState.errorMessageRes),
                        type = AuthFeedbackType.Error,
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    brush =
                                        Brush.linearGradient(
                                            colors =
                                                listOf(
                                                    Lagoon.copy(alpha = 0.16f),
                                                    MaterialTheme.colorScheme.surface,
                                                    Coral.copy(alpha = 0.14f),
                                                ),
                                        ),
                                ).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.create_room_screen_title),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Text(
                                text = stringResource(R.string.create_room_screen_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedTextField(
                            value = uiState.roomNameInput,
                            onValueChange = onRoomNameChange,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isCreatingRoom,
                            label = { Text(stringResource(R.string.label_room_name)) },
                            singleLine = true,
                            isError = uiState.roomNameErrorRes != null,
                            supportingText =
                                uiState.roomNameErrorRes?.let {
                                    { Text(stringResource(it)) }
                                },
                        )
                        OutlinedTextField(
                            value = uiState.roomTopicInput,
                            onValueChange = onRoomTopicChange,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isCreatingRoom,
                            label = { Text(stringResource(R.string.label_room_topic)) },
                            singleLine = true,
                            isError = uiState.roomTopicErrorRes != null,
                            supportingText =
                                uiState.roomTopicErrorRes?.let {
                                    { Text(stringResource(it)) }
                                },
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = stringResource(R.string.label_cefr_level),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(R.string.create_room_cefr_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                CEFR_LEVELS.take(3).forEach { level ->
                                    FilterChip(
                                        selected = uiState.selectedCefrLevel == level,
                                        onClick = { onCefrLevelSelected(level) },
                                        enabled = !uiState.isCreatingRoom,
                                        label = { Text(level) },
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                CEFR_LEVELS.drop(3).forEach { level ->
                                    FilterChip(
                                        selected = uiState.selectedCefrLevel == level,
                                        onClick = { onCefrLevelSelected(level) },
                                        enabled = !uiState.isCreatingRoom,
                                        label = { Text(level) },
                                    )
                                }
                            }
                            uiState.cefrLevelErrorRes?.let { errorRes ->
                                Text(
                                    text = stringResource(errorRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        Button(
                            onClick = onCreateRoomClick,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isCreatingRoom,
                        ) {
                            if (uiState.isCreatingRoom) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Text(
                                    text = stringResource(R.string.action_create_room),
                                    modifier = Modifier.padding(start = 10.dp),
                                )
                            } else {
                                Text(stringResource(R.string.action_create_room))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateRoomTopBar(onBackClick: () -> Unit) {
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
                text = stringResource(R.string.action_create_room),
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.create_room_top_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val CEFR_LEVELS = listOf("A1", "A2", "B1", "B2", "C1", "C2")
