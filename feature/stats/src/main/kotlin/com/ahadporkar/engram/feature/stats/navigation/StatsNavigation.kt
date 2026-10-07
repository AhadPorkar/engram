package com.ahadporkar.engram.feature.stats.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ahadporkar.engram.feature.stats.StatsRoute
import kotlinx.serialization.Serializable

@Serializable
data object StatsDestination

fun NavController.navigateToStats() = navigate(StatsDestination)

fun NavGraphBuilder.statsScreen(onBack: () -> Unit) {
    composable<StatsDestination> { StatsRoute(onBack = onBack) }
}
