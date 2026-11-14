package com.yezdiconnect.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.yezdiconnect.app.YezdiApp
import com.yezdiconnect.app.nav.ManeuverParser
import com.yezdiconnect.app.nav.NavigationState
import com.yezdiconnect.app.obd.Dtc
import com.yezdiconnect.app.obd.FaultCodes
import com.yezdiconnect.app.obd.FuelRate
import com.yezdiconnect.app.obd.ObdInfo
import com.yezdiconnect.app.obd.ObdLive
import com.yezdiconnect.app.obd.ObdPid
import com.yezdiconnect.app.obd.ObdState
import com.yezdiconnect.app.rides.FillUp
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
        // Sample OBD connection, as it would look with an adapter on a CAN bike.
        app.obd.showSample(
            ObdState.Connected("OBDLink CX", ObdInfo("ELM327 v1.4b", "CAN 11-bit 500 kbit/s", setOf(0x04, 0x05, 0x0B, 0x0C, 0x0D, 0x0F, 0x11)), "MD2ABCDEFG1234567"),
            ObdLive(
                values = mapOf(
                    ObdPid.SPEED to 62.0, ObdPid.RPM to 4180.0, ObdPid.COOLANT_TEMP to 86.0, ObdPid.THROTTLE to 22.0,
                    ObdPid.ENGINE_LOAD to 38.0, ObdPid.INTAKE_AIR_TEMP to 31.0,
                ),
                batteryVolts = 13.8,
                fuelRate = FuelRate(litresPerHour = 1.9, kmPerLitre = 32.6),
                tripKm = 18.4, tripLitres = 0.55,
            ),
            FaultCodes.Read(listOf(Dtc("P0133"), Dtc("P0301", pending = true)), 1_794_000_000_000L),
        )
        runBlocking {
            val dao = app.database.rides()
            dao.insertFillUp(FillUp(time = 1_793_000_000_000L, odometerKm = 4210.0, litres = 9.1, fullTank = true))
            dao.insertFillUp(FillUp(time = 1_793_500_000_000L, odometerKm = 4512.0, litres = 8.6, fullTank = true))
            dao.insertFillUp(FillUp(time = 1_794_000_000_000L, odometerKm = 4830.0, litres = 9.3, fullTank = true))
        }
        compose.setContent { YezdiAppUi(app) }
        compose.onNodeWithText("Bike").performClick()
        compose.waitForIdle()
        shot("3-bike")
        compose.onNodeWithText("Read fault codes").performScrollTo()
        compose.waitForIdle()
        shot("3b-bike-health")
        compose.onNodeWithText("Add fill-up").performScrollTo()
        compose.waitForIdle()
        shot("3c-bike-fuel")
    }

    @Test fun guide() {
        compose.setContent {
            YezdiTheme {
                androidx.compose.foundation.layout.Box(
                    androidx.compose.ui.Modifier.background(YezdiColors.Background),
                ) { ObdGuideContent(androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/5-guide.png")
        compose.onNodeWithText("Tap Read fault codes.").performScrollTo()
        compose.onRoot().captureRoboImage("build/screenshots/5b-guide.png")
        compose.onNodeWithText("The app only reads data", substring = true).performScrollTo()
        compose.onRoot().captureRoboImage("build/screenshots/5c-guide.png")
    }

    @Test fun setup() {
        compose.setContent { YezdiAppUi(app) }
        compose.onNodeWithText("Setup").performClick()
        compose.waitForIdle()
        shot("4-setup")
    }
}
