package com.benza.smartrooms.data.di

import com.benza.smartrooms.data.auth.remote.FirebaseAuthDataSource
import com.benza.smartrooms.data.auth.repository.AuthRepository
import com.benza.smartrooms.data.auth.repository.FirebaseAuthRepository
import com.benza.smartrooms.data.room.remote.FirebaseFunctionsRoomDataSource
import com.benza.smartrooms.data.room.remote.FirestoreRoomDataSource
import com.benza.smartrooms.data.room.repository.FirestoreRoomRepository
import com.benza.smartrooms.data.room.repository.RoomRepository
import com.benza.smartrooms.data.userprofile.remote.FirebaseFunctionsUserProfileDataSource
import com.benza.smartrooms.data.userprofile.remote.FirestoreUserProfileDataSource
import com.benza.smartrooms.data.userprofile.repository.FirestoreUserProfileRepository
import com.benza.smartrooms.data.userprofile.repository.UserProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import org.koin.dsl.module

/**
 * Data-layer dependency graph for auth SDK objects and repositories.
 */
internal val dataModule =
    module {
        single { FirebaseAuth.getInstance() }
        single { FirebaseFirestore.getInstance() }
        single { FirebaseFunctions.getInstance(FUNCTIONS_REGION) }
        single { FirebaseAuthDataSource(get()) }
        single { FirestoreRoomDataSource(get()) }
        single { FirestoreUserProfileDataSource(get()) }
        single { FirebaseFunctionsRoomDataSource(get()) }
        single { FirebaseFunctionsUserProfileDataSource(get()) }
        single<AuthRepository> { FirebaseAuthRepository(get()) }
        single<UserProfileRepository> { FirestoreUserProfileRepository(get(), get()) }
        single<RoomRepository> { FirestoreRoomRepository(get(), get()) }
    }

private const val FUNCTIONS_REGION = "europe-west3"
