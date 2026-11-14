package com.yezdiconnect.app.rides

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** A phone with app version 0.1/0.2 data (database version 1) must keep its rides after updating. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RideDatabaseMigrationTest {

    @Test fun `version 1 rides survive and fill-ups work after migrating to version 2`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.getDatabasePath("rides.db").apply { parentFile?.mkdirs(); delete() }

        // Exactly what Room created for database version 1 (from the generated RideDatabase_Impl).
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `rides` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER, `distanceMeters` REAL NOT NULL, `maxSpeedKmh` REAL NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `ride_points` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `rideId` INTEGER NOT NULL, `time` INTEGER NOT NULL, `lat` REAL NOT NULL, `lng` REAL NOT NULL, `speedKmh` REAL NOT NULL, `accuracyMeters` REAL NOT NULL, FOREIGN KEY(`rideId`) REFERENCES `rides`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ride_points_rideId` ON `ride_points` (`rideId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'd3bbde58e987f8c33d422077d5a4f57f')")
            db.execSQL("INSERT INTO rides (startedAt, endedAt, distanceMeters, maxSpeedKmh) VALUES (1000, 2000, 12345.0, 88.0)")
            db.version = 1
        }

        val database = RideDatabase.create(context)
        try {
            val rides = database.rides().finishedRides().first()
            assertEquals(1, rides.size)
            assertEquals(12345.0, rides[0].distanceMeters, 0.001)

            database.rides().insertFillUp(FillUp(time = 1, odometerKm = 1000.0, litres = 8.0, fullTank = true))
            assertEquals(1, database.rides().fillUps().first().size)
        } finally {
            database.close()
        }
    }
}
