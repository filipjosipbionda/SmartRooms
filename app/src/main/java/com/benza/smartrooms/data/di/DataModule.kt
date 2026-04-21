package com.benza.smartrooms.data.di

import com.benza.smartrooms.data.auth.remote.FirebaseAuthDataSource
import com.benza.smartrooms.data.auth.repository.AuthRepository
import com.benza.smartrooms.data.auth.repository.FirebaseAuthRepository
import com.benza.smartrooms.data.room.remote.FirestoreRoomDataSource
import com.benza.smartrooms.data.room.repository.FirestoreRoomRepository
import com.benza.smartrooms.data.room.repository.RoomRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.dsl.module

/**
 * Data-layer dependency graph for auth SDK objects and repositories.
 */
internal val dataModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single { FirebaseAuthDataSource(get()) }
    single { FirestoreRoomDataSource(get()) }
    single<AuthRepository> { FirebaseAuthRepository(get()) }
    single<RoomRepository> { FirestoreRoomRepository(get()) }
}
