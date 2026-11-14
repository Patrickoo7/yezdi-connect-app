package com.yezdiconnect.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yezdiconnect.app.R
import com.yezdiconnect.app.YezdiApp

private enum class Tab(val route: String, @StringRes val label: Int, @DrawableRes val icon: Int) {
    Map("map", R.string.tab_map, R.drawable.ic_map),
    Rides("rides", R.string.tab_rides, R.drawable.ic_list),
    Bike("bike", R.string.tab_bike, R.drawable.ic_bike),
    Setup("setup", R.string.tab_setup, R.drawable.ic_settings),
}

@Composable
fun YezdiAppUi(app: YezdiApp) {
    YezdiTheme {
        val nav = rememberNavController()
        val backStack by nav.currentBackStackEntryAsState()
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        val selected = backStack?.destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            },
        ) { padding ->
            NavHost(nav, startDestination = Tab.Map.route, modifier = Modifier.padding(padding)) {
                composable(Tab.Map.route) { MapScreen(app) }
                composable(Tab.Rides.route) { RidesScreen(app, onOpen = { id -> nav.navigate("ride/$id") }) }
                composable(
                    "ride/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry -> RideDetailScreen(app, entry.arguments!!.getLong("id")) }
                composable(Tab.Bike.route) { BikeScreen(app) }
                composable(Tab.Setup.route) { SetupScreen() }
            }
        }
    }
}
