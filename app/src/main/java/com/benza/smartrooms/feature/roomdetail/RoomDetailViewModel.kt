package com.benza.smartrooms.feature.roomdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benza.smartrooms.R
import com.benza.smartrooms.data.auth.model.AuthUser
import com.benza.smartrooms.data.auth.repository.AuthRepository
import com.benza.smartrooms.data.room.model.CreateRoomInvitationRequest
import com.benza.smartrooms.data.room.model.QuestionType
import com.benza.smartrooms.data.room.model.RoomAnnouncement
import com.benza.smartrooms.data.room.model.RoomAnnouncementAttachment
import com.benza.smartrooms.data.room.model.RoomInvitationAccess
import com.benza.smartrooms.data.room.model.RoomOperationResult
import com.benza.smartrooms.data.room.model.RoomQuizStatus
import com.benza.smartrooms.data.room.model.RoomQuizSummary
import com.benza.smartrooms.data.room.repository.RoomRepository
import com.benza.smartrooms.data.userprofile.model.UserProfile
import com.benza.smartrooms.data.userprofile.model.UserProfileOperationResult
import com.benza.smartrooms.data.userprofile.model.UserRole
import com.benza.smartrooms.data.userprofile.repository.UserProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        val authorName: String,
        val attachments: List<RoomAnnouncementAttachment>,
        val canManage: Boolean,
    ) : RoomFeedItemUiState

    data class Quiz(
        override val id: String,
        override val createdAtEpochMillis: Long,
        val title: String,
        val topic: String,
        val cefrLevel: String,
        val status: RoomQuizStatus,
        val questionTypeLabelRes: Int,
        val questionCount: Int,
    ) : RoomFeedItemUiState
}

internal data class RoomDetailUiState(
    val roomId: String,
    val roomName: String,
    val roomTopic: String,
    val cefrLevel: String = "",
    val ownerId: String = "",
    val memberIds: List<String> = emptyList(),
    val collaboratorIds: List<String> = emptyList(),
    val isCurrentUserOwner: Boolean = false,
    val feedItems: List<RoomFeedItemUiState> = emptyList(),
    val isLoadingRoom: Boolean = true,
    val isLoadingFeed: Boolean = true,
    val isInviteDialogOpen: Boolean = false,
    val inviteSearchQuery: String = "",
    val inviteSearchResults: List<InviteUserUiState> = emptyList(),
    val isSearchingInviteUsers: Boolean = false,
    val isSendingInvite: Boolean = false,
    val pendingAnnouncementDeletion: PendingAnnouncementDeletion? = null,
    val isDeletingAnnouncement: Boolean = false,
    val errorMessageRes: Int? = null,
    val infoMessageRes: Int? = null,
)

internal data class InviteUserUiState(
    val uid: String,
    val displayName: String,
    val email: String,
    val role: UserRole?,
    val access: RoomInvitationAccess,
)

internal data class PendingAnnouncementDeletion(
    val announcementId: String,
    val announcementTitle: String,
    val attachments: List<RoomAnnouncementAttachment>,
)

internal class RoomDetailViewModel(
    roomId: String,
    roomName: String,
    roomTopic: String,
    roomCefrLevel: String,
    private val roomRepository: RoomRepository,
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {
    private val currentUser = authRepository.getCurrentUser()
    private var inviteSearchJob: Job? = null
    private var latestQuizzes: List<RoomQuizSummary> = emptyList()
    private var latestAnnouncements: List<RoomAnnouncement> = emptyList()
    private var areQuizzesLoaded = false
    private var areAnnouncementsLoaded = false
    private var quizzesErrorMessageRes: Int? = null
    private var announcementsErrorMessageRes: Int? = null

    private val _uiState =
        MutableStateFlow(
            RoomDetailUiState(
                roomId = roomId,
                roomName = roomName,
                roomTopic = roomTopic,
                cefrLevel = roomCefrLevel,
                isLoadingRoom = roomCefrLevel.isBlank(),
            ),
        )
    val uiState: StateFlow<RoomDetailUiState> = _uiState.asStateFlow()

    init {
        observeRoom()
        observeFeed()
    }

    internal fun consumeInfoMessage() {
        _uiState.update { it.copy(infoMessageRes = null) }
    }

    internal fun requestAnnouncementDeletion(item: RoomFeedItemUiState.Announcement) {
        if (!item.canManage) return

        _uiState.update {
            it.copy(
                pendingAnnouncementDeletion =
                    PendingAnnouncementDeletion(
                        announcementId = item.id,
                        announcementTitle = item.title,
                        attachments = item.attachments,
                    ),
                errorMessageRes = null,
                infoMessageRes = null,
            )
        }
    }

    internal fun dismissAnnouncementDeletion() {
        if (_uiState.value.isDeletingAnnouncement) return
        _uiState.update { it.copy(pendingAnnouncementDeletion = null) }
    }

    internal fun deleteAnnouncement() {
        currentUser ?: run {
            _uiState.update { it.copy(errorMessageRes = R.string.error_room_auth_required) }
            return
        }
        val pendingDeletion = _uiState.value.pendingAnnouncementDeletion ?: return
        if (_uiState.value.isDeletingAnnouncement) return

        _uiState.update {
            it.copy(
                isDeletingAnnouncement = true,
                errorMessageRes = null,
                infoMessageRes = null,
            )
        }

        viewModelScope.launch {
            when (
                val result =
                    roomRepository.deleteAnnouncement(
                        roomId = _uiState.value.roomId,
                        announcementId = pendingDeletion.announcementId,
                        attachments = pendingDeletion.attachments,
                    )
            ) {
                is RoomOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            pendingAnnouncementDeletion = null,
                            isDeletingAnnouncement = false,
                            infoMessageRes = R.string.room_detail_post_deleted,
                        )
                    }
                }

                is RoomOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            pendingAnnouncementDeletion = null,
                            isDeletingAnnouncement = false,
                            errorMessageRes = result.messageRes,
                        )
                    }
                }
            }
        }
    }

    internal fun showInviteDialog() {
        if (!_uiState.value.isCurrentUserOwner) return

        _uiState.update {
            it.copy(
                isInviteDialogOpen = true,
                inviteSearchQuery = "",
                inviteSearchResults = emptyList(),
                isSearchingInviteUsers = false,
                errorMessageRes = null,
            )
        }
    }

    internal fun dismissInviteDialog() {
        if (_uiState.value.isSendingInvite) return
        inviteSearchJob?.cancel()

        _uiState.update {
            it.copy(
                isInviteDialogOpen = false,
                inviteSearchQuery = "",
                inviteSearchResults = emptyList(),
                isSearchingInviteUsers = false,
            )
        }
    }

    internal fun onInviteSearchQueryChanged(value: String) {
        val query = value.trimStart()
        inviteSearchJob?.cancel()

        _uiState.update {
            it.copy(
                inviteSearchQuery = query,
                inviteSearchResults =
                    if (query.length <
                        MIN_INVITE_SEARCH_QUERY_LENGTH
                    ) {
                        emptyList()
                    } else {
                        it.inviteSearchResults
                    },
                isSearchingInviteUsers = query.length >= MIN_INVITE_SEARCH_QUERY_LENGTH,
                errorMessageRes = null,
            )
        }

        if (query.length < MIN_INVITE_SEARCH_QUERY_LENGTH) {
            _uiState.update { it.copy(isSearchingInviteUsers = false) }
            return
        }

        inviteSearchJob =
            viewModelScope.launch {
                when (val result = userProfileRepository.searchProfiles(query)) {
                    is UserProfileOperationResult.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                inviteSearchResults =
                                    result.data
                                        .filter { it.uid != currentUser?.uid }
                                        .map(UserProfile::toInviteUserUiState)
                                        .filterNot { it.uid in state.memberIds || it.uid in state.collaboratorIds },
                                isSearchingInviteUsers = false,
                            )
                        }
                    }

                    is UserProfileOperationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isSearchingInviteUsers = false,
                                errorMessageRes = result.messageRes,
                            )
                        }
                    }
                }
            }
    }

    internal fun sendInvite(invitee: InviteUserUiState) {
        val user =
            currentUser ?: run {
                _uiState.update { it.copy(errorMessageRes = R.string.error_room_auth_required) }
                return
            }
        val state = _uiState.value
        if (!state.isCurrentUserOwner || state.isSendingInvite) return
        if (invitee.uid in state.memberIds || invitee.uid in state.collaboratorIds) {
            _uiState.update { it.copy(errorMessageRes = R.string.error_room_invite_already_in_room) }
            return
        }

        _uiState.update {
            it.copy(
                isSendingInvite = true,
                errorMessageRes = null,
                infoMessageRes = null,
            )
        }

        viewModelScope.launch {
            when (
                val result =
                    roomRepository.createRoomInvitation(
                        CreateRoomInvitationRequest(
                            roomId = state.roomId,
                            roomName = state.roomName,
                            inviterId = user.uid,
                            inviterName = user.displayNameOrEmailName(),
                            inviteeId = invitee.uid,
                            inviteeEmail = invitee.email,
                            inviteeDisplayName = invitee.displayName,
                            access = invitee.access,
                        ),
                    )
            ) {
                is RoomOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSendingInvite = false,
                            isInviteDialogOpen = false,
                            inviteSearchQuery = "",
                            inviteSearchResults = emptyList(),
                            infoMessageRes = R.string.room_invite_sent,
                        )
                    }
                }

                is RoomOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSendingInvite = false,
                            errorMessageRes = result.messageRes,
                        )
                    }
                }
            }
        }
    }

    private fun observeFeed() {
        val currentUserId = currentUser?.uid.orEmpty()
        viewModelScope.launch {
            roomRepository.observeQuizzes(_uiState.value.roomId).collect { result ->
                when (result) {
                    is RoomOperationResult.Success -> {
                        latestQuizzes = result.data
                        areQuizzesLoaded = true
                        quizzesErrorMessageRes = null
                        publishFeedState(currentUserId)
                    }

                    is RoomOperationResult.Error -> {
                        areQuizzesLoaded = true
                        quizzesErrorMessageRes = result.messageRes
                        publishFeedState(currentUserId)
                    }
                }
            }
        }

        viewModelScope.launch {
            roomRepository.observeAnnouncements(_uiState.value.roomId).collect { result ->
                when (result) {
                    is RoomOperationResult.Success -> {
                        latestAnnouncements = result.data
                        areAnnouncementsLoaded = true
                        announcementsErrorMessageRes = null
                        publishFeedState(currentUserId)
                    }

                    is RoomOperationResult.Error -> {
                        areAnnouncementsLoaded = true
                        announcementsErrorMessageRes = result.messageRes
                        publishFeedState(currentUserId)
                    }
                }
            }
        }
    }

    private fun publishFeedState(currentUserId: String) {
        _uiState.update {
            it.copy(
                feedItems =
                    buildFeedItems(
                        quizzes = latestQuizzes,
                        announcements = latestAnnouncements,
                        currentUserId = currentUserId,
                    ),
                isLoadingFeed = !areQuizzesLoaded || !areAnnouncementsLoaded,
                errorMessageRes = quizzesErrorMessageRes ?: announcementsErrorMessageRes,
            )
        }
    }

    private fun observeRoom() {
        viewModelScope.launch {
            roomRepository.observeRoom(_uiState.value.roomId).collect { result ->
                when (result) {
                    is RoomOperationResult.Success -> {
                        _uiState.update {
                            it.copy(
                                roomName = result.data.name,
                                roomTopic = result.data.topic,
                                cefrLevel = result.data.cefrLevel,
                                ownerId = result.data.ownerId,
                                memberIds = result.data.memberIds,
                                collaboratorIds = result.data.collaboratorIds,
                                isCurrentUserOwner = result.data.ownerId == currentUser?.uid,
                                isLoadingRoom = false,
                                errorMessageRes = null,
                            )
                        }
                    }

                    is RoomOperationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoadingRoom = false,
                                errorMessageRes = result.messageRes,
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
    announcements: List<RoomAnnouncement>,
    currentUserId: String,
): List<RoomFeedItemUiState> =
    buildList {
        announcements.forEach { announcement ->
            add(
                RoomFeedItemUiState.Announcement(
                    id = announcement.id,
                    createdAtEpochMillis = announcement.createdAtEpochMillis,
                    title = announcement.title,
                    message = announcement.message,
                    authorName = announcement.authorName,
                    attachments = announcement.attachments,
                    canManage = announcement.authorId == currentUserId,
                ),
            )
        }
        quizzes.forEach { quiz ->
            if (quiz.status == RoomQuizStatus.READY) {
                add(
                    RoomFeedItemUiState.Quiz(
                        id = quiz.id,
                        createdAtEpochMillis = quiz.createdAtEpochMillis,
                        title = quiz.title,
                        topic = quiz.topic,
                        cefrLevel = quiz.cefrLevel,
                        status = quiz.status,
                        questionTypeLabelRes = labelRes(quiz.questionType),
                        questionCount = quiz.questionCount,
                    ),
                )
            }
        }
    }.sortedByDescending(RoomFeedItemUiState::createdAtEpochMillis)

private fun AuthUser.displayNameOrEmailName(): String =
    displayName
        ?.takeIf(String::isNotBlank)
        ?: email?.substringBefore("@").orEmpty()

private fun UserProfile.toInviteUserUiState(): InviteUserUiState {
    val access =
        when (role) {
            UserRole.TEACHER -> RoomInvitationAccess.COLLABORATOR
            UserRole.STUDENT, null -> RoomInvitationAccess.MEMBER
        }

    return InviteUserUiState(
        uid = uid,
        displayName = displayName,
        email = email,
        role = role,
        access = access,
    )
}

internal fun labelRes(questionType: QuestionType): Int =
    when (questionType) {
        QuestionType.MULTIPLE_CHOICE -> R.string.room_detail_type_multiple_choice
        QuestionType.FILL_IN_BLANK -> R.string.room_detail_type_fill_in_blank
        QuestionType.WORD_SCRAMBLE -> R.string.room_detail_type_word_scramble
    }

private const val MIN_INVITE_SEARCH_QUERY_LENGTH = 2
