package com.wisdomtower.academy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wisdomtower.academy.data.model.ResourceType
import com.wisdomtower.academy.data.repository.WisdomRepository
import com.wisdomtower.academy.ui.screens.auth.AuthScreen
import com.wisdomtower.academy.ui.screens.downloads.DownloadsScreen
import com.wisdomtower.academy.ui.screens.home.HomeScreen
import com.wisdomtower.academy.ui.screens.packages.PackageDetailScreen
import com.wisdomtower.academy.ui.screens.subjects.SubjectHubScreen
import com.wisdomtower.academy.ui.screens.viewer.FlashcardsScreen
import com.wisdomtower.academy.ui.screens.viewer.NotesViewerScreen
import com.wisdomtower.academy.ui.screens.viewer.PdfViewerScreen
import com.wisdomtower.academy.ui.screens.viewer.QuizScreen

@Composable
fun WisdomNavHost(
    repository: WisdomRepository = WisdomRepository(LocalContext.current)
) {
    val navController = rememberNavController()
    val isLoggedIn = remember { repository.sessionManager.isLoggedIn() }
    val startDestination = if (isLoggedIn) Screen.Home.route else Screen.Auth.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                repository = repository,
                onAuthSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                repository = repository,
                onNavigateToPackage = { packageId ->
                    navController.navigate(Screen.PackageDetail.createRoute(packageId))
                },
                onNavigateToDownloads = {
                    navController.navigate(Screen.Downloads.route)
                },
                onLogout = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.PackageDetail.route,
            arguments = listOf(navArgument("packageId") { type = NavType.StringType })
        ) { backStackEntry ->
            val packageId = backStackEntry.arguments?.getString("packageId") ?: "pkg_freshman"
            PackageDetailScreen(
                packageId = packageId,
                repository = repository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSubject = { pkgId, subjectId ->
                    navController.navigate(Screen.SubjectHub.createRoute(pkgId, subjectId))
                }
            )
        }

        composable(
            route = Screen.SubjectHub.route,
            arguments = listOf(
                navArgument("packageId") { type = NavType.StringType },
                navArgument("subjectId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val packageId = backStackEntry.arguments?.getString("packageId") ?: "freshman"
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            SubjectHubScreen(
                packageId = packageId,
                subjectId = subjectId,
                repository = repository,
                onNavigateBack = { navController.popBackStack() },
                onOpenResource = { resource ->
                    when (resource.resourceType) {
                        ResourceType.BOOK_PDF -> navController.navigate(Screen.PdfViewer.createRoute(resource.id))
                        ResourceType.SHORT_NOTE -> navController.navigate(Screen.NotesViewer.createRoute(resource.id))
                        ResourceType.FLASHCARDS -> navController.navigate(Screen.Flashcards.createRoute(resource.id))
                        ResourceType.QUESTION_BANK -> navController.navigate(Screen.Quiz.createRoute(resource.id, isTimed = false))
                        ResourceType.MOCK_EXAM -> navController.navigate(Screen.Quiz.createRoute(resource.id, isTimed = true))
                        ResourceType.VIDEO -> navController.navigate(Screen.NotesViewer.createRoute(resource.id))
                    }
                }
            )
        }

        composable(
            route = Screen.NotesViewer.route,
            arguments = listOf(navArgument("resourceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId") ?: ""
            NotesViewerScreen(
                resourceId = resourceId,
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PdfViewer.route,
            arguments = listOf(navArgument("resourceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId") ?: ""
            PdfViewerScreen(
                resourceId = resourceId,
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Flashcards.route,
            arguments = listOf(navArgument("resourceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId") ?: ""
            FlashcardsScreen(
                resourceId = resourceId,
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Quiz.route,
            arguments = listOf(
                navArgument("resourceId") { type = NavType.StringType },
                navArgument("isTimed") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId") ?: ""
            val isTimed = backStackEntry.arguments?.getBoolean("isTimed") ?: false
            QuizScreen(
                resourceId = resourceId,
                isTimed = isTimed,
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Downloads.route) {
            DownloadsScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() },
                onOpenDownloadedResource = { entity ->
                    val type = try {
                        ResourceType.valueOf(entity.resourceType)
                    } catch (e: Exception) {
                        ResourceType.SHORT_NOTE
                    }
                    when (type) {
                        ResourceType.BOOK_PDF -> navController.navigate(Screen.PdfViewer.createRoute(entity.id))
                        ResourceType.SHORT_NOTE -> navController.navigate(Screen.NotesViewer.createRoute(entity.id))
                        ResourceType.FLASHCARDS -> navController.navigate(Screen.Flashcards.createRoute(entity.id))
                        ResourceType.QUESTION_BANK -> navController.navigate(Screen.Quiz.createRoute(entity.id, isTimed = false))
                        ResourceType.MOCK_EXAM -> navController.navigate(Screen.Quiz.createRoute(entity.id, isTimed = true))
                        ResourceType.VIDEO -> navController.navigate(Screen.NotesViewer.createRoute(entity.id))
                    }
                }
            )
        }
    }
}
