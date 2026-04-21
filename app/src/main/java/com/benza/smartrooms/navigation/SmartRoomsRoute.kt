package com.benza.smartrooms.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe destination for the login flow.
 */
@Serializable
internal data object LoginRoute

/**
 * Type-safe destination for account registration.
 */
@Serializable
internal data object RegisterRoute

/**
 * Type-safe destination for password reset.
 */
@Serializable
internal data object ForgotPasswordRoute

/**
 * Type-safe destination for the authenticated home screen.
 */
@Serializable
internal data object HomeRoute
