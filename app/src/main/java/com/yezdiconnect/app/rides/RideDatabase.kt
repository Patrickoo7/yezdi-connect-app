package com.yezdiconnect.app.rides

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {
    @Insert suspend fun insert(ride: Ride): Long
    @Update suspend fun update(ride: Ride)
    @Insert suspend fun insertPoint(point: RidePoint)

    @Query("SELECT * FROM rides WHERE id = :id") suspend fun ride(id: Long): Ride?
    @Query("SELECT * FROM rides WHERE endedAt IS NOT NULL ORDER BY startedAt DESC") fun finishedRides(): Flow<List<Ride>>
    @Query("SELECT * FROM rides WHERE id = :id") fun observeRide(id: Long): Flow<Ride?>
    @Query("SELECT * FROM ride_points WHERE rideId = :rideId ORDER BY time") fun points(rideId: Long): Flow<List<RidePoint>>
    @Query("DELETE FROM rides WHERE id = :id") suspend fun delete(id: Long)
    /** Rides left open by a crash or a killed service. */
    @Query("SELECT * FROM rides WHERE endedAt IS NULL") suspend fun unfinished(): List<Ride>

    @Insert suspend fun insertFillUp(fillUp: FillUp): Long
    @Query("SELECT * FROM fill_ups ORDER BY odometerKm DESC") fun fillUps(): Flow<List<FillUp>>
    @Query("DELETE FROM fill_ups WHERE id = :id") suspend fun deleteFillUp(id: Long)
}

@Database(entities = [Ride::class, RidePoint::class, FillUp::class], version = 2, exportSchema = false)
abstract class RideDatabase : RoomDatabase() {
    abstract fun rides(): RideDao

    companion object {
        /** Version 2 adds the fill-up log; existing rides are kept. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `fill_ups` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`time` INTEGER NOT NULL, `odometerKm` REAL NOT NULL, `litres` REAL NOT NULL, `fullTank` INTEGER NOT NULL)",
                )
            }
        }

        fun create(context: Context): RideDatabase =
            Room.databaseBuilder(context, RideDatabase::class.java, "rides.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
