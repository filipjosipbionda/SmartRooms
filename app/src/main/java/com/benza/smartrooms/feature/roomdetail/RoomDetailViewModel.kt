package com.benza.smartrooms.feature.roomdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benza.smartrooms.R
import com.benza.smartrooms.data.auth.model.AuthUser
import com.benza.smartrooms.data.auth.repository.AuthRepository
import com.benza.smartrooms.data.room.model.CreateAnnouncementRequest
import com.benza.smartrooms.data.room.model.QuestionType
import com.benza.smartrooms.data.room.model.RoomAnnouncement
import com.benza.smartrooms.data.room.model.RoomOperationResult
import com.benza.smartrooms.data.room.model.RoomQuizSummary
import com.benza.smartrooms.data.room.repository.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal sealed interface RoomFeedItemUiState {
    val id: String
    val createdAtEpochMillis: Long

    data class Announcement(
        override val id: String,
        override val createdAtEpochMillis: Long,
        val title: String,
        val message: String,
        val authorName: String
    ) : RoomFeedItemUiState

    data class Quiz(
        override val id: String,
        override val createdAtEpochMillis: Long,
        val title: String,
        val cefrLevel: String,
        val questionTypeLabelRes: Int,
        val questionCount: Int
    ) : RoomFeedItemUiState
}

internal data class RoomDetailUiState(
    val roomId: String,
    val roomName: String,
    val roomTopic: String,
    val cefrLevel: String = DEFAULT_CEFR_LEVEL,
    val feedItems: List<RoomFeedItemUiState> = emptyList(),
    val isLoadingFeed: Boolean = true,
    val isCreateAnnouncementDialogOpen: Boolean = false,
    val isCreatingAnnouncement: Boolean = false,
    val announcementTitleInput: String = "",
    val announcementMessageInput: String = "",
    val announcementTitleErrorRes: Int? = null,
    val announcementMessageErrorRes: Int? = null,
    val errorMessageRes: Int? = null,
    val infoMessageRes: Int? = null
)

internal class RoomDetailViewModel(
    roomId: String,
    roomName: String,
    roomTopic: String,
    private val roomRepository: RoomRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        RoomDetailUiState(
            roomId = roomId,
            roomName = roomName,
            roomTopic = roomTopic
        )
    )
    internal val uiState: StateFlow<RoomDetailUiState> = _uiState.asStateFlow()

    init {
        observeFeed()
    }

    internal fun consumeInfoMessage() {
        _uiState.update { it.copy(infoMessageRes = null) }
    }

    internal fun showCreateAnnouncementDialog() {
        _uiState.update {
            it.copy(
                isCreateAnnouncementDialogOpen = true,
                announcementTitleErrorRes = null,
                announcementMessageErrorRes = null,
                errorMessageRes = null
            )
        }
    }

    internal fun dismissCreateAnnouncementDialog() {
        if (_uiState.value.isCreatingAnnouncement) return

        _uiState.update {
            it.copy(
                isCreateAnnouncementDialogOpen = false,
                announcementTitleInput = "",
                announcementMessageInput = "",
                announcementTitleErrorRes = null,
                announcementMessageErrorRes = null
            )
        }
    }

    internal fun onAnnouncementTitleChanged(value: String) {
        _uiState.update {
            it.copy(
                announcementTitleInput = value,
                announcementTitleErrorRes = null,
                errorMessageRes = null
            )
        }
    }

    internal fun onAnnouncementMessageChanged(value: String) {
        _uiState.update {
            it.copy(
                announcementMessageInput = value,
                announcementMessageErrorRes = null,
                errorMessageRes = null
            )
        }
    }

    internal fun createAnnouncement() {
        val user = authRepository.getCurrentUser() ?: run {
            _uiState.update { it.copy(errorMessageRes = R.string.error_room_auth_required) }
            return
        }
        if (_uiState.value.isCreatingAnnouncement) return

        val title = _uiState.value.announcementTitleInput.trim()
        val message = _uiState.value.announcementMessageInput.trim()
        val titleError = if (title.isBlank()) R.string.error_announcement_title_required else null
        val messageError = if (message.isBlank()) R.string.error_announcement_message_required else null

        _uiState.update {
            it.copy(
                announcementTitleInput = title,
                announcementMessageInput = message,
                announcementTitleErrorRes = titleError,
                announcementMessageErrorRes = messageError,
                errorMessageRes = if (titleError == null && messageError == null) {
                    null
                } else {
                    R.string.error_announcement_fix_fields
                }
            )
        }

        if (titleError != null || messageError != null) return

        _uiState.update {
            it.copy(
                isCreatingAnnouncement = true,
                errorMessageRes = null,
                infoMessageRes = null
            )
        }

        viewModelScope.launch {
            when (
                val result = roomRepository.createAnnouncement(
                    CreateAnnouncementRequest(
                        roomId = _uiState.value.roomId,
                        authorId = user.uid,
                        authorName = user.displayNameOrEmailName(),
                        title = title,
                        message = message
                    )
                )
            ) {
                is RoomOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCreatingAnnouncement = false,
                            isCreateAnnouncementDialogOpen = false,
                            announcementTitleInput = "",
                            announcementMessageInput = "",
                            announcementTitleErrorRes = null,
                            announcementMessageErrorRes = null,
                            infoMessageRes = R.string.room_detail_post_created
                        )
                    }
                }

                is RoomOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCreatingAnnouncement = false,
                            errorMessageRes = result.messageRes
                        )
                    }
                }
            }
        }
    }

    private fun observeFeed() {
        viewModelScope.launch {
            combine(
                roomRepository.observeQuizzes(_uiState.value.roomId),
                roomRepository.observeAnnouncements(_uiState.value.roomId)
            ) { quizzesResult, announcementsResult ->
                quizzesResult to announcementsResult
            }.collect { (quizzesResult, announcementsResult) ->
                when {
                    quizzesResult is RoomOperationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoadingFeed = false,
                                errorMessageRes = quizzesResult.messageRes
                            )
                        }
                    }

                    announcementsResult is RoomOperationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoadingFeed = false,
                                errorMessageRes = announcementsResult.messageRes
                            )
                        }
                    }

                    quizzesResult is RoomOperationResult.Success &&
                        announcementsResult is RoomOperationResult.Success -> {
                        _uiState.update {
                            it.copy(
                                feedItems = buildFeedItems(
                                    quizzes = quizzesResult.data,
                                    announcements = announcementsResult.data
                                ),
                                isLoadingFeed = false,
                                errorMessageRes = null
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildFeedItems(
    quizzes: List<RoomQuizSummary>,
    announcements: List<RoomAnnouncement>
): List<RoomFeedItemUiState> {
    return buildList {
        announcements.forEach { announcement ->
            add(
                RoomFeedItemUiState.Announcement(
                    id = announcement.id,
                    createdAtEpochMillis = announcement.createdAtEpochMillis,
                    title = announcement.title,
                    message = announcement.message,
                    authorName = announcement.authorName
                )
            )
        }
        quizzes.forEach { quiz ->
            add(
                RoomFeedItemUiState.Quiz(
                    id = quiz.id,
                    createdAtEpochMillis = quiz.createdAtEpochMillis,
                    title = quiz.title,
                    cefrLevel = quiz.cefrLevel,
                    questionTypeLabelRes = labelRes(quiz.questionType),
                    questionCount = quiz.questionCount
                )
            )
        }
    }.sortedByDescending(RoomFeedItemUiState::createdAtEpochMillis)
}

private fun AuthUser.displayNameOrEmailName(): String {
    return displayName
        ?.takeIf(String::isNotBlank)
        ?: email?.substringBefore("@").orEmpty()
}

internal fun labelRes(questionType: QuestionType): Int {
    return when (questionType) {
        QuestionType.MULTIPLE_CHOICE -> R.string.room_detail_type_multiple_choice
        QuestionType.FILL_IN_BLANK -> R.string.room_detail_type_fill_in_blank
    }
}

private const val DEFAULT_CEFR_LEVEL = "B1"
