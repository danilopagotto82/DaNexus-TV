package com.nuvio.tv.ui.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nuvio.tv.core.danexus.DanexusPeopleCredits
import com.nuvio.tv.domain.model.MetaCastMember
import com.nuvio.tv.ui.navigation.Screen
import com.nuvio.tv.ui.screens.cast.CastDetailScreen

/** Keep the paused player mounted while browsing the same native person screen used in Details. */
@Composable
internal fun DanexusPersonOverlay(
    person: MetaCastMember,
    onDismiss: () -> Unit,
    onNavigateToDetail: (String, String, String?) -> Unit
) {
    val personId = person.tmdbId?.takeIf { it > 0 } ?: return
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(
        usePlatformDefaultWidth = false, dismissOnClickOutside = false
    )) {
        val navigation = rememberNavController()
        NavHost(
            navController = navigation,
            startDestination = Screen.CastDetail.createRoute(personId, person.name, DanexusPeopleCredits.isDirector(person)),
            modifier = Modifier.fillMaxSize()
        ) {
            composable(
                Screen.CastDetail.route,
                arguments = listOf(
                    navArgument("personId") { type = NavType.StringType },
                    navArgument("personName") { type = NavType.StringType },
                    navArgument("preferCrew") { type = NavType.BoolType; defaultValue = false }
                )
            ) {
                CastDetailScreen(
                    onBackPress = onDismiss,
                    onNavigateToDetail = { id, type, addon ->
                        onDismiss()
                        onNavigateToDetail(id, type, addon)
                    }
                )
            }
        }
    }
}
