package com.ahadporkar.engram.feature.study.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ahadporkar.engram.core.model.CramOrder
import com.ahadporkar.engram.core.model.StudyMode
import com.ahadporkar.engram.feature.study.StudyRoute
import kotlinx.serialization.Serializable

/**
 * A study session. `deckId = -1` studies all decks.
 * Successor of `FLashCardSettingActivity` + `FlashCardActivity` (ViewPager of fragments).
 */
@Serializable
data class StudyDestination(
    val deckId: Long = -1L,
    val mode: String = StudyMode.SMART.name,
    val cramOrder: String = CramOrder.SHUFFLE.name,
)

fun NavController.navigateToStudy(deckId: Long?, mode: StudyMode, cramOrder: CramOrder = CramOrder.SHUFFLE) =
    navigate(StudyDestination(deckId ?: -1L, mode.name, cramOrder.name))

fun NavGraphBuilder.studyScreen(onClose: () -> Unit) {
    composable<StudyDestination> {
        StudyRoute(onClose = onClose)
    }
}
