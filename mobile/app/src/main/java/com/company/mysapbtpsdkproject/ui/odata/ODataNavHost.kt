package com.company.mysapbtpsdkproject.ui.odata

import android.app.Application
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.company.mysapbtpsdkproject.ui.odata.screens.*
import com.company.mysapbtpsdkproject.ui.odata.viewmodel.*
import com.sap.cloud.mobile.kotlin.odata.EntityValue
import com.sap.cloud.mobile.kotlin.odata.Property
import com.sap.cloud.mobile.kotlin.odata.StructureBase

const val SETTINGS_SCREEN_ROUTE = "settings"

@Composable
fun ODataNavHost(
    navController: NavHostController,
    windowSize: WindowSizeClass,
    modifier: Modifier
) {
    val isExpandedScreen = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded
    NavHost(
        navController = navController,
        startDestination = EntitySetsDest.route,
        modifier = modifier
    ) {
        composable(route = EntitySetsDest.route) {
            EntitySetScreen(
                getEntitySetScreenInfoList(),
                navController::navigateToEntityList,
            ) { navController.navigate(SETTINGS_SCREEN_ROUTE) }
        }

        composable(route = SETTINGS_SCREEN_ROUTE) {
            SettingsScreen(navigateUp = navController::navigateUp)
        }

        //complex types
        ComplexScreenInfo.entries.forEach { screenInfo ->
            val complexType = screenInfo.complexType
            val entityExpandScreen = screenInfo.entityExpandScreen
            val entityListScreen = screenInfo.entityListScreen
            val entityEditScreen = screenInfo.entityEditScreen
            val entityDetailScreen = screenInfo.entityDetailScreen
            composable(
                route = EntityNavigationCommands(complexType).toEntitiesNav.route,
                arguments = EntityNavigationCommands(complexType).toEntitiesNav.arguments
            ) { navBackStackEntry ->
                val parent =
                    navController.previousBackStackEntry?.savedStateHandle?.get<EntityValue>(
                        "master"
                    )
                val operationType =
                    navController.previousBackStackEntry?.savedStateHandle?.get<IEntityOperationType>(
                        "operationType"
                    )
                val navProperty =
                    navBackStackEntry.arguments?.getString(navigationPropertyNameArg)
                ODataScreen(
                    navController,
                    isExpandedScreen,
                    viewModel(
                        factory = ODataComplexTypeViewModelFactory(
                            LocalContext.current.applicationContext as Application,
                            complexType,
                            getOrderByProperty(complexType),
                            parent,
                            navProperty,
                            operationType ?: EntityOperationType.DETAIL
                        )
                    ),
                    entityExpandScreen,
                    entityListScreen,
                    entityEditScreen,
                    entityDetailScreen
                )
            }
        }

        //EntitySets
        EntityScreenInfo.entries.forEach { screenInfo ->
            val entityType = screenInfo.entityType
            val entitySet = screenInfo.entitySet
            val entityExpandScreen = screenInfo.entityExpandScreen
            val entityListScreen = screenInfo.entityListScreen
            val entityEditScreen = screenInfo.entityEditScreen
            val entityDetailScreen = screenInfo.entityDetailScreen
            navigation(
                startDestination = EntityNavigationCommands(entityType).entityListNav.route,
                route = entityType.name
            ) {
                composable(route = EntityNavigationCommands(entityType).entityListNav.route) {
                    ODataScreen(
                        navController,
                        isExpandedScreen,
                        viewModel(
                            factory = ODataEntityViewModelFactory(
                                LocalContext.current.applicationContext as Application,
                                entityType,
                                entitySet,
                                getOrderByProperty(entityType)
                            )
                        ),
                        entityExpandScreen,
                        entityListScreen,
                        entityEditScreen,
                        entityDetailScreen
                    )
                }

                composable(
                    route = EntityNavigationCommands(entityType).toEntitiesNav.route,
                    arguments = EntityNavigationCommands(entityType).toEntitiesNav.arguments
                ) { navBackStackEntry ->
                    val parent =
                        navController.previousBackStackEntry?.savedStateHandle?.get<EntityValue>(
                            "master"
                        )
                    val navProperty =
                        navBackStackEntry.arguments?.getString(navigationPropertyNameArg)

                    ODataScreen(
                        navController,
                        isExpandedScreen,
                        viewModel(
                            factory = ODataEntityViewModelFactory(
                                LocalContext.current.applicationContext as Application,
                                entityType,
                                entitySet,
                                getOrderByProperty(entityType),
                                parent,
                                navProperty,
                            )
                        ),
                        entityExpandScreen,
                        entityListScreen,
                        entityEditScreen,
                        entityDetailScreen
                    )
                }
            }
        }
    }
}

@Composable
private fun <T : StructureBase> ODataScreen(
    navController: NavHostController, isExpandedScreen: Boolean, viewModel: ODataViewModel<T>,
    entityExpandScreen: @Composable (
        navigateToHome: () -> Unit,
        navigateUp: () -> Unit,
        onNavigateProperty: (T, Property, IEntityOperationType) -> Unit,
        viewModel: ODataViewModel<T>,
    ) -> Unit,
    entityListScreen: @Composable (
        navigateToHome: () -> Unit, navigateUp: () -> Unit, viewModel: ODataViewModel<T>, isExpandedScreen: Boolean
    ) -> Unit,
    entityEditScreen: @Composable (
        onNavigateProperty: (T, Property, IEntityOperationType) -> Unit,
        navigateUp: () -> Unit, viewModel: ODataViewModel<T>, isExpandedScreen: Boolean
    ) -> Unit,
    entityDetailScreen: @Composable (
        onNavigateProperty: (T, Property, IEntityOperationType) -> Unit, navigateUp: () -> Unit, viewModel: ODataViewModel<T>, isExpandedScreen: Boolean
    ) -> Unit,
) {
    val onNavigateProperty: (StructureBase, Property, IEntityOperationType) -> Unit =
        { master, navProp, operationType ->
            val navSavedHandler = navController.currentBackStackEntry?.savedStateHandle
            navSavedHandler?.set(
                key = "master", value = master
            )
            navSavedHandler?.set(
                key = "operationType", value = operationType
            )
            navController.navigateToNavigatePropertyList(navProp)
        }

    val uiState by viewModel.odataUIState.collectAsState()
    if (isExpandedScreen) {
        entityExpandScreen(
            {},
            navController::navigateUp,
            onNavigateProperty,
            viewModel
        )
    } else {
        if (!uiState.isEntityFocused) {
            entityListScreen(
                {
                    navController.popBackStack(
                        EntitySetsDest.route, false
                    )
                },
                navController::navigateUp,
                viewModel,
                false
            )
        } else {
            when (uiState.entityOperationType) {
                EntityOperationType.DETAIL -> entityDetailScreen(
                    onNavigateProperty,
                    viewModel::lostEntityFocus,
                    viewModel,
                    false
                )

                EntityOperationType.CREATE, EntityUpdateOperationType.UPDATE_FROM_LIST, EntityUpdateOperationType.UPDATE_FROM_DETAIL -> entityEditScreen(
                    onNavigateProperty,
                    viewModel::exitEditor,
                    viewModel,
                    false
                )

                EntityOperationType.UNSPECIFIED -> {}
            }
        }
    }
}
