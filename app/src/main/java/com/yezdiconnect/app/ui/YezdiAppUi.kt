package com.yezdiconnect.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.yezdiconnect.app.obd.ObdState
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
            containerColor = YezdiColors.Background,
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
                Column(Modifier.fillMaxWidth().statusBarsPadding()) {
                    Wordmark(Modifier.padding(horizontal = 16.dp, vertical = 14.dp))
                    // The bike's own Bluetooth link isn't built yet; the OBD adapter is the only live link so far.
                    val obd by app.obd.state.collectAsStateWithLifecycle()
                    when (val s = obd) {
                        is ObdState.Connected -> StatusBanner(stringResource(R.string.banner_obd_connected, s.adapterName), BannerKind.Ok)
                        is ObdState.Connecting -> StatusBanner(stringResource(R.string.banner_obd_connecting), BannerKind.Warn)
                        else -> StatusBanner(stringResource(R.string.banner_bike_not_connected), BannerKind.Warn)
                    }
                }
            },
            bottomBar = {
                Column {
                    HorizontalDivider(color = YezdiColors.OutlineVariant)
                    NavigationBar(containerColor = YezdiColors.Surface) {
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
                                label = { Text(stringResource(tab.label), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = YezdiColors.Accent,
                                    selectedTextColor = YezdiColors.Accent,
                                    indicatorColor = YezdiColors.Accent.copy(alpha = 0.16f),
                                    unselectedIconColor = YezdiColors.Muted,
                                    unselectedTextColor = YezdiColors.Muted,
                                ),
                            )
                        }
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
