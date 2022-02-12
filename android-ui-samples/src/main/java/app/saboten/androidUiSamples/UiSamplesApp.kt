@file:OptIn(ExperimentalMaterialNavigationApi::class)

package app.saboten.androidUiSamples

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.navigation.plusAssign
import app.hdj.datepick.ui.animation.materialTransitionZaxisIn
import app.hdj.datepick.ui.animation.materialTransitionZaxisOut
import app.saboten.androidUi.styles.MainTheme
import app.saboten.androidUiSamples.screens.*
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator

@Composable
fun UiSamplesApp() {

    val navController = rememberAnimatedNavController()
    val bottomSheetNavigator = rememberBottomSheetNavigator()

    navController.navigatorProvider += bottomSheetNavigator

    val default = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(default) }

    MainTheme(isDarkTheme) {
        ModalBottomSheetLayout(
            bottomSheetNavigator = bottomSheetNavigator,
            sheetBackgroundColor = Color.Unspecified
        ) {

            AnimatedNavHost(
                navController = navController,
                startDestination = UiSamplesAppRoutes.Home.route,
                enterTransition = { materialTransitionZaxisIn },
                exitTransition = { materialTransitionZaxisOut }
            ) {

                composable(UiSamplesAppRoutes.Home.route) {
                    HomeScreen(navController) {
                        isDarkTheme = isDarkTheme.not()
                    }
                }

                composable(UiSamplesAppRoutes.Buttons.route) {
                    ButtonsScreen(navController)
                }

                composable(UiSamplesAppRoutes.Dialogs.route) {
                    DialogsScreen(navController)
                }

                composable(UiSamplesAppRoutes.Colors.route) {
                    ColorsScreen(navController)
                }

                composable(UiSamplesAppRoutes.Typographies.route) {
                    TypographiesScreen(navController)
                }

                composable(UiSamplesAppRoutes.TextFields.route) {
                    TextFieldsScreen(navController)
                }

                composable(UiSamplesAppRoutes.Lists.route) {
                    ListsScreen(navController)
                }

            }

        }
    }


}