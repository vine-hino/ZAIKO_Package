package com.vine.zaiko_package.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vine.navigation.ZaikoRoute
import com.vine.auth.LoginRoute
import com.vine.ht_home.HtHomeRoute
import com.vine.ht_operations.ui.HtAdjustmentScreen
import com.vine.ht_operations.ui.HtCompletedScreen
import com.vine.ht_operations.ui.HtInboundRoute
import com.vine.ht_operations.ui.HtOutboundRoute
import com.vine.ht_operations.ui.HtPreparingScreen
import com.vine.ht_operations.ui.HtStocktakeScreen

@Composable
fun ZaikoNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ZaikoRoute.LOGIN,
    ) {
        composable(ZaikoRoute.LOGIN) {
            LoginRoute(
                onLoginSuccess = {
                    navController.navigate(ZaikoRoute.HT_HOME) {
                        popUpTo(ZaikoRoute.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(ZaikoRoute.HT_HOME) {
            HtHomeRoute(
                onStockClick = { navController.navigate(ZaikoRoute.htPreparing("在庫照会")) },
                onInboundClick = { navController.navigate(ZaikoRoute.HT_INBOUND) },
                onOutboundClick = { navController.navigate(ZaikoRoute.HT_OUTBOUND) },
                onMoveClick = { navController.navigate(ZaikoRoute.htPreparing("在庫移動")) },
                onStocktakeClick = { navController.navigate(ZaikoRoute.HT_STOCKTAKE) },
                onAdjustmentClick = { navController.navigate(ZaikoRoute.HT_ADJUSTMENT) },
            )
        }

        composable(ZaikoRoute.HT_INBOUND) {
            HtInboundRoute(
                onBack = { navController.popBackStack() },
                onComplete = { message ->
                    navController.navigate(ZaikoRoute.htResult(message))
                },
            )
        }

        composable(ZaikoRoute.HT_OUTBOUND) {
            HtOutboundRoute(
                onBack = { navController.popBackStack() },
                onComplete = { message ->
                    navController.navigate(ZaikoRoute.htResult(message))
                },
            )
        }

        composable(ZaikoRoute.HT_STOCKTAKE) {
            HtStocktakeScreen(
                onBack = { navController.popBackStack() },
                onComplete = { message ->
                    navController.navigate(ZaikoRoute.htResult(message))
                },
            )
        }

        composable(ZaikoRoute.HT_ADJUSTMENT) {
            HtAdjustmentScreen(
                onBack = { navController.popBackStack() },
                onComplete = { message ->
                    navController.navigate(ZaikoRoute.htResult(message))
                },
            )
        }

        composable(
            route = ZaikoRoute.HT_PREPARING,
            arguments = listOf(
                navArgument(ZaikoRoute.PREPARING_LABEL_ARG) {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val label =
                backStackEntry.arguments?.getString(ZaikoRoute.PREPARING_LABEL_ARG).orEmpty()

            HtPreparingScreen(
                label = label,
                onBackHome = {
                    navController.navigate(ZaikoRoute.HT_HOME) {
                        popUpTo(ZaikoRoute.HT_HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = ZaikoRoute.HT_RESULT,
            arguments = listOf(
                navArgument(ZaikoRoute.RESULT_MESSAGE_ARG) {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val message =
                backStackEntry.arguments?.getString(ZaikoRoute.RESULT_MESSAGE_ARG).orEmpty()

            HtCompletedScreen(
                message = message,
                onBackHome = {
                    navController.navigate(ZaikoRoute.HT_HOME) {
                        popUpTo(ZaikoRoute.HT_HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onContinue = {
                    navController.popBackStack()
                },
            )
        }
    }
}