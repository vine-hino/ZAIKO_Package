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
import com.vine.ht_operations.ui.HtStockListScreen
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
                onStockClick = { navController.navigate(ZaikoRoute.HT_STOCK_LIST) },
                onInboundClick = { navController.navigate(ZaikoRoute.HT_INBOUND) },
                onOutboundClick = { navController.navigate(ZaikoRoute.HT_OUTBOUND) },
                onStocktakeClick = { navController.navigate(ZaikoRoute.HT_STOCKTAKE) },
            )
        }

        composable(ZaikoRoute.HT_STOCK_LIST) {
            HtStockListScreen(
                onBack = { navController.popBackStack() },
                onSelectStock = { productCode, _, locationCode ->
                    navController.navigate(
                        ZaikoRoute.htAdjustment(
                            productCode = productCode,
                            locationCode = locationCode,
                        ),
                    )
                },
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

        composable(
            route = ZaikoRoute.HT_ADJUSTMENT,
            arguments = listOf(
                navArgument(ZaikoRoute.HT_ADJUSTMENT_PRODUCT_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(ZaikoRoute.HT_ADJUSTMENT_LOCATION_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { backStackEntry ->
            val productCode = backStackEntry.arguments
                ?.getString(ZaikoRoute.HT_ADJUSTMENT_PRODUCT_ARG)
                ?.ifBlank { null }
            val locationCode = backStackEntry.arguments
                ?.getString(ZaikoRoute.HT_ADJUSTMENT_LOCATION_ARG)
                ?.ifBlank { null }

            HtAdjustmentScreen(
                onBack = { navController.popBackStack() },
                onComplete = { message ->
                    navController.navigate(ZaikoRoute.htResult(message))
                },
                initialProductCode = productCode,
                initialLocationCode = locationCode,
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
