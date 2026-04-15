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
import com.vine.ht_operations.HtAdjustmentScreen
import com.vine.ht_operations.HtCompletedScreen
import com.vine.ht_operations.HtInboundRoute
import com.vine.ht_operations.HtMoveScreen
import com.vine.ht_operations.HtOutboundRoute
import com.vine.ht_operations.HtOutboundScreen
import com.vine.ht_operations.HtStockHistoryScreen
import com.vine.ht_operations.HtStockListScreen
import com.vine.ht_operations.HtStocktakeScreen

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
                onMoveClick = { navController.navigate(ZaikoRoute.HT_MOVE) },
                onStocktakeClick = { navController.navigate(ZaikoRoute.HT_STOCKTAKE) },
                onAdjustmentClick = { navController.navigate(ZaikoRoute.HT_ADJUSTMENT) },
            )
        }

        composable(ZaikoRoute.HT_STOCK_LIST) {
            HtStockListScreen(
                onBack = { navController.popBackStack() },
                onOpenHistory = { navController.navigate(ZaikoRoute.HT_STOCK_HISTORY) },
            )
        }

        composable(ZaikoRoute.HT_STOCK_HISTORY) {
            HtStockHistoryScreen(
                onBack = { navController.popBackStack() },
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
                onComplete = {
                    navController.navigate(ZaikoRoute.htResult("出庫を登録しました"))
                },
            )
        }

        composable(ZaikoRoute.HT_MOVE) {
            HtMoveScreen(
                onBack = { navController.popBackStack() },
                onComplete = {
                    navController.navigate(ZaikoRoute.htResult("在庫移動を登録しました"))
                },
            )
        }

        composable(ZaikoRoute.HT_STOCKTAKE) {
            HtStocktakeScreen(
                onBack = { navController.popBackStack() },
                onComplete = {
                    navController.navigate(ZaikoRoute.htResult("棚卸を保存しました"))
                },
            )
        }

        composable(ZaikoRoute.HT_ADJUSTMENT) {
            HtAdjustmentScreen(
                onBack = { navController.popBackStack() },
                onComplete = {
                    navController.navigate(ZaikoRoute.htResult("在庫調整を登録しました"))
                },
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