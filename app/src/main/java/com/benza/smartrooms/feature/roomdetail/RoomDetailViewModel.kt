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
import com.benza.smartrooms.data.room.model.RoomQuizResult
import com.benza.smartrooms.data.room.model.RoomQuizStatus
import com.benza.smartrooms.data.room.model.RoomQuizSummary
import com.benza.smartrooms.data.room.repository.RoomRepository
import com.benza.smartrooms.data.userprofile.model.UserProfile
import com.benza.smartrooms.data.userprofile.model.UserProfileOperationResult
import com.benza.smartrooms.data.userprofile.model.UserRole
import com.benza.smartrooms.data.userprofile.repository.UserProfileRepository
import com.benza.smartrooms.util.orPrettyEmailLocalPart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class RoomAnnouncementCardUiState(
    val id: String,
    val createdAtEpochMillis: Long,
    val title: String,
    val message: String,
    val authorName: String,
    val attachments: List<RoomAnnouncementAttachment>,
    val canManage: Boolean,
)

internal data class RoomDetailUiState(
    val roomId: String,
    val roomName: String,
    val roomTopic: String,
    val cefrLevel: String = "",
    val ownerId: String = "",
    val memberIds: List<String> = emptyList(),
    val collaboratorIds: List<String> = emptyList(),
    val isCurrentUserOwner: Boolean = false,
    val isCurrentUserCollaborator: Boolean = false,
    val currentUserRole: UserRole? = null,
    val announcements: List<RoomAnnouncementCardUiState> = emptyList(),
    val quizzes: List<RoomQuizSummary> = emptyList(),
    val quizResultsByQuizId: Map<String, RoomQuizResult> = emptyMap(),
    val isLoadingRoom: Boolean = true,
    val isLoadingFeed: Boolean = true,
    val isLoadingQuizzes: Boolean = true,
    val isInviteDialogOpen: Boolean = false,
    val inviteSearchQuery: String = "",
    val inviteSearchResults: List<InviteUserUiState> = emptyList(),
    val inviteSearchEmptyMessageRes: Int? = null,
    val isSearchingInviteUsers: Boolean = false,
    val isSendingInvite: Boolean = false,
    val pendingAnnouncementDeletion: PendingAnnouncementDeletion? = null,
    val isDeletingAnnouncement: Boolean = false,
    val errorMessageRes: Int? = null,
    val infoMessageRes: Int? = null,
) {
    val canManageQuizzes: Boolean
        get() = currentUserRole == UserRole.TEACHER && (isCurrentUserOwner || isCurrentUserCollaborator)

    val canManagePosts: Boolean
        get() = currentUserRole == UserRole.TEACHER
}

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
    authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {
    private val currentUser = authRepository.getCurrentUser()
    private val currentUserId = currentUser?.uid.orEmpty()
    private var inviteSearchJob: Job? = null
    private var latestQuizzes: List<RoomQuizSummary> = emptyList()
    private var latestQuizResultsByQuizId: Map<String, RoomQuizResult> = emptyMap()
    private var latestAnnouncements: List<RoomAnnouncement> = emptyList()
    private var areQuizzesLoaded = false
    private var areAnnouncementsLoaded = false
    private var roomErrorMessageRes: Int? = null
    private var profileErrorMessageRes: Int? = null
    private var quizzesErrorMessageRes: Int? = null
    private var quizResultsErrorMessageRes: Int? = null
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
        observeCurrentUserProfile()
        observeRoom()
        observeAnnouncements()
        observeQuizzes()
        observeQuizResults()
    }

    internal fun consumeInfoMessage() {
        _uiState.update { it.copy(infoMessageRes = null) }
    }

    internal fun requestAnnouncementDeletion(item: RoomAnnouncementCardUiState) {
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
                inviteSearchEmptyMessageRes = null,
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
                inviteSearchEmptyMessageRes = null,
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
                    if (query.length < MIN_INVITE_SEARCH_QUERY_LENGTH) {
                        emptyList()
                    } else {
                        it.inviteSearchResults
                    },
                inviteSearchEmptyMessageRes = null,
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
                            val rawResults = result.data.map(UserProfile::toInviteUserUiState)
                            val containsCurrentUser = rawResults.any { it.uid == currentUser?.uid }
                            val containsExistingRoomUser =
                                rawResults.any { it.uid in state.memberIds || it.uid in state.collaboratorIds }
                            val filteredResults =
                                rawResults
                                    .filter { it.uid != currentUser?.uid }
                                    .filterNot { it.uid in state.memberIds || it.uid in state.collaboratorIds }
                            state.copy(
                                inviteSearchResults = filteredResults,
                                inviteSearchEmptyMessageRes =
                                    when {
                                        filteredResults.isNotEmpty() -> null
                                        containsCurrentUser -> R.string.room_invite_search_self_hidden
                                        containsExistingRoomUser -> R.string.room_invite_search_already_in_room
                                        else -> R.string.room_invite_search_empty
                                    },
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

    private fun observeAnnouncements() {
        viewModelScope.launch {
            roomRepository.observeAnnouncements(_uiState.value.roomId).collect { result ->
                when (result) {
                    is RoomOperationResult.Success -> {
                        latestAnnouncements = result.data
                        areAnnouncementsLoaded = true
                        announcementsErrorMessageRes = null
                        publishAnnouncementState()
                    }

                    is RoomOperationResult.Error -> {
                        areAnnouncementsLoaded = true
                        announcementsErrorMessageRes = result.messageRes
                        publishAnnouncementState()
                    }
                }
            }
        }
    }

    private fun observeQuizzes() {
        viewModelScope.launch {
            roomRepository.observeQuizzes(_uiState.value.roomId).collect { result ->
                when (result) {
                    is RoomOperationResult.Success -> {
                        latestQuizzes = result.data
                        areQuizzesLoaded = true
                        quizzesErrorMessageRes = null
                        publishQuizState()
                    }

                    is RoomOperationResult.Error -> {
                        areQuizzesLoaded = true
                        quizzesErrorMessageRes = result.messageRes
                        publishQuizState()
                    }
                }
            }
        }
    }

    private fun observeQuizResults() {
        if (currentUserId.isBlank()) return

        viewModelScope.launch {
            roomRepository.observeQuizResults(currentUserId, _uiState.value.roomId).collect { result ->
                when (result) {
                    is RoomOperationResult.Success -> {
                        latestQuizResultsByQuizId = result.data.associateBy(RoomQuizResult::quizId)
                        quizResultsErrorMessageRes = null
                        publishQuizState()
                    }

                    is RoomOperationResult.Error -> {
                        quizResultsErrorMessageRes = result.messageRes
                        publishQuizState()
                    }
                }
            }
        }
    }

    private fun publishAnnouncementState() {
        _uiState.update { state ->
            state.copy(
                announcements =
                    latestAnnouncements
                        .map { announcement ->
                            announcement.toAnnouncementCardUiState(
                                currentUserId = currentUserId,
                                canCurrentUserManagePosts = state.canManagePosts,
                            )
                        }.sortedByDescending(RoomAnnouncementCardUiState::createdAtEpochMillis),
                isLoadingFeed = !areAnnouncementsLoaded,
                errorMessageRes = currentDataError(),
            )
        }
    }

    private fun publishQuizState() {
        _uiState.update { state ->
            state.copy(
                quizzes =
                    buildVisibleQuizzes(latestQuizzes),
                quizResultsByQuizId = latestQuizResultsByQuizId,
                isLoadingQuizzes = !areQuizzesLoaded,
                errorMessageRes = currentDataError(),
            )
        }
    }

    private fun observeRoom() {
        viewModelScope.launch {
            roomRepository.observeRoom(_uiState.value.roomId).collect { result ->
                when (result) {
                    is RoomOperationResult.Success -> {
                        roomErrorMessageRes = null
                        _uiState.update {
                            it.copy(
                                roomName = result.data.name,
                                roomTopic = result.data.topic,
                                cefrLevel = result.data.cefrLevel,
                                ownerId = result.data.ownerId,
                                memberIds = result.data.memberIds,
                                collaboratorIds = result.data.collaboratorIds,
                                isCurrentUserOwner = result.data.ownerId == currentUserId,
                                isCurrentUserCollaborator = currentUserId in result.data.collaboratorIds,
                                isLoadingRoom = false,
                                errorMessageRes = currentDataError(),
                            )
                        }
                        publishQuizState()
                    }

                    is RoomOperationResult.Error -> {
                        roomErrorMessageRes = result.messageRes
                        _uiState.update {
                            it.copy(
                                isLoadingRoom = false,
                                errorMessageRes = currentDataError(),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun observeCurrentUserProfile() {
        val user = currentUser ?: return

        viewModelScope.launch {
            userProfileRepository.observeProfile(user).collect { result ->
                when (result) {
                    is UserProfileOperationResult.Success -> {
                        profileErrorMessageRes = null
                        _uiState.update {
                            it.copy(
                                currentUserRole = result.data.role,
                                errorMessageRes = currentDataError(),
                            )
                        }
                        publishAnnouncementState()
                    }

                    is UserProfileOperationResult.Error -> {
                        profileErrorMessageRes = result.messageRes
                        _uiState.update {
                            it.copy(
                                currentUserRole = null,
                                errorMessageRes = currentDataError(),
                            )
                        }
                        publishAnnouncementState()
                    }
                }
            }
        }
    }

    private fun currentDataError(): Int? =
        roomErrorMessageRes ?: profileErrorMessageRes ?: announcementsErrorMessageRes ?: quizzesErrorMessageRes
            ?: quizResultsErrorMessageRes
}

private fun RoomAnnouncement.toAnnouncementCardUiState(
    currentUserId: String,
    canCurrentUserManagePosts: Boolean,
): RoomAnnouncementCardUiState =
    RoomAnnouncementCardUiState(
        id = id,
        createdAtEpochMillis = createdAtEpochMillis,
        title = title,
        message = message,
        authorName = authorName,
        attachments = attachments,
        canManage = canCurrentUserManagePosts && authorId == currentUserId,
    )

private fun buildVisibleQuizzes(quizzes: List<RoomQuizSummary>): List<RoomQuizSummary> =
    quizzes
        .asSequence()
        .filter { quiz -> quiz.status == RoomQuizStatus.READY }
        .sortedByDescending(RoomQuizSummary::createdAtEpochMillis)
        .toList()

private fun AuthUser.displayNameOrEmailName(): String =
    displayName
        ?.takeIf(String::isNotBlank)
        ?: email.orPrettyEmailLocalPart(email)

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
