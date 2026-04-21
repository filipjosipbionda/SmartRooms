package com.benza.smartrooms.ui.di

import com.benza.smartrooms.feature.auth.forgotpassword.ForgotPasswordViewModel
import com.benza.smartrooms.feature.auth.login.LoginViewModel
import com.benza.smartrooms.feature.auth.register.RegisterViewModel
import com.benza.smartrooms.feature.home.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * UI-layer dependency graph for feature ViewModels.
 */
internal val viewModelModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
    viewModelOf(::ForgotPasswordViewModel)
    viewModelOf(::HomeViewModel)
}
