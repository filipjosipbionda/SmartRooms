package com.benza.smartrooms

import android.app.Application
import com.benza.smartrooms.data.di.dataModule
import com.benza.smartrooms.ui.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * Application entry point that boots the Koin dependency graph.
 */
class SmartRoomsApplication : Application() {
    /**
     * Initializes dependency injection once for the whole process.
     */
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@SmartRoomsApplication)
            modules(dataModule, viewModelModule)
        }
    }
}
