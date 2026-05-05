package com.benza.smartrooms.ui.di

import com.benza.smartrooms.feature.auth.authgate.AuthGateViewModel
import com.benza.smartrooms.feature.auth.forgotpassword.ForgotPasswordViewModel
import com.benza.smartrooms.feature.auth.login.LoginViewModel
import com.benza.smartrooms.feature.auth.register.RegisterViewModel
import com.benza.smartrooms.feature.auth.roleselection.RoleSelectionViewModel
import com.benza.smartrooms.feature.createroom.CreateRoomViewModel
import com.benza.smartrooms.feature.home.HomeViewModel
import com.benza.smartrooms.feature.profile.ProfileViewModel
import com.benza.smartrooms.feature.roomdetail.RoomDetailViewModel
import com.benza.smartrooms.feature.roomquizbuilder.RoomQuizBuilderViewModel
import com.benza.smartrooms.feature.roomquizplayer.RoomQuizPlayerViewModel
import com.benza.smartrooms.feature.roomquizreview.RoomQuizReviewViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * UI-layer dependency graph for feature ViewModels.
 */
internal val viewModelModule =
    module {
        viewModelOf(::AuthGateViewModel)
        viewModelOf(::LoginViewModel)
        viewModelOf(::RegisterViewModel)
        viewModelOf(::ForgotPasswordViewModel)
        viewModelOf(::RoleSelectionViewModel)
        viewModelOf(::HomeViewModel)
        viewModelOf(::CreateRoomViewModel)
        viewModelOf(::ProfileViewModel)
        viewModel { (roomId: String, roomName: String, roomTopic: String) ->
            RoomDetailViewModel(roomId, roomName, roomTopic, get(), get(), get())
        }
        viewModel { (roomId: String, roomName: String, roomTopic: String) ->
            RoomQuizBuilderViewModel(roomId, roomName, roomTopic, get())
        }
        viewModel { (roomId: String, roomName: String, quizId: String) ->
            RoomQuizPlayerViewModel(roomId, roomName, quizId, get())
        }
        viewModel { (roomId: String, roomName: String, quizId: String) ->
            RoomQuizReviewViewModel(roomId, roomName, quizId, get())
        }
    }
