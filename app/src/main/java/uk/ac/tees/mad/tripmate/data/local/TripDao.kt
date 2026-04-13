package uk.ac.tees.mad.tripmate.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Query("SELECT * FROM trips WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllTrips(userId: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    suspend fun getTripById(tripId: String): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrips(trips: List<TripEntity>)

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTrip(tripId: String)

    @Query("DELETE FROM trips WHERE userId = :userId")
    suspend fun deleteAllTrips(userId: String)

    @Query("SELECT * FROM trips WHERE isSynced = 0")
    suspend fun getUnsyncedTrips(): List<TripEntity>

    @Query("UPDATE trips SET isSynced = 1 WHERE id = :tripId")
    suspend fun markAsSynced(tripId: String)
}