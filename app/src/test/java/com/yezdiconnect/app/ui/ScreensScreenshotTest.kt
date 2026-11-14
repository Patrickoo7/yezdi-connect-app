package com.yezdiconnect.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.yezdiconnect.app.YezdiApp
import com.yezdiconnect.app.nav.ManeuverParser
import com.yezdiconnect.app.nav.NavigationState
import com.yezdiconnect.app.rides.Ride
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders each tab at Galaxy M31 size (411 x 891 dp, 420 dpi) with sample data.
 * Pixels are only written with -Pscreenshots; otherwise this just checks the screens compose.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-port-420dpi")
class ScreensScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<YezdiApp>()

    private fun shot(name: String) = compose.onRoot().captureRoboImage("build/screenshots/$name.png")

    @Test fun map() {
        NavigationState.update(ManeuverParser.parse("350 m", "Turn left onto MG Road"), "350 m | Turn left onto MG Road")
        compose.setContent { YezdiAppUi(app) }
        shot("1-map")
    }

    @Test fun rides() {
        runBlocking {
            val dao = app.database.rides()
            val day = 86_400_000L
            val base = 1_794_000_000_000L
            dao.insert(Ride(startedAt = base, endedAt = base + 52 * 60_000, distanceMeters = 38_400.0, maxSpeedKmh = 92.0))
            dao.insert(Ride(startedAt = base - day, endedAt = base - day + 125 * 60_000, distanceMeters = 112_700.0, maxSpeedKmh = 108.0))
        }
        compose.setContent { YezdiAppUi(app) }
        compose.onNodeWithText("Rides").performClick()
        compose.waitForIdle()
        shot("2-rides")
    }

    @Test fun bike() {
        compose.setContent { YezdiAppUi(app) }
        compose.onNodeWithText("Bike").performClick()
        compose.waitForIdle()
        shot("3-bike")
    }

    @Test fun setup() {
        compose.setContent { YezdiAppUi(app) }
        compose.onNodeWithText("Setup").performClick()
        compose.waitForIdle()
        shot("4-setup")
    }
}
