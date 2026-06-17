package com.gatekeeper.mobile.ui.navigation

import androidx.navigation.NavController

fun NavController.safeNavigate(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
    }
}
