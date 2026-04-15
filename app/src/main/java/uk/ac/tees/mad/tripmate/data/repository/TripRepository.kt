package uk.ac.tees.mad.tripmate.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uk.ac.tees.mad.tripmate.data.local.TripDatabase
import uk.ac.tees.mad.tripmate.data.local.TripEntity
import uk.ac.tees.mad.tripmate.data.mapper.toEntity
import uk.ac.tees.mad.tripmate.data.mapper.toTrip
import uk.ac.tees.mad.tripmate.data.model.Trip

class TripRepository(context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val tripDao = TripDatabase.getDatabase(context).tripDao()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private fun getUserId(): String = auth.currentUser?.uid ?: ""

    private fun getTripsCollection() = firestore
        .collection("users")
        .document(getUserId())
        .collection("trips")

    init {
        startFirestoreSync()
    }

    private fun startFirestoreSync() {
        val userId = getUserId()
        if (userId.isEmpty()) return

        repositoryScope.launch {
            getTripsCollection()
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("TripRepository", "Firestore sync error", error)
                        return@addSnapshotListener
                    }

                    snapshot?.documents?.let { docs ->
                        repositoryScope.launch {
                            val trips = docs.mapNotNull { doc ->
                                doc.toObject(Trip::class.java)?.copy(id = doc.id)
                            }

                            trips.forEach { trip ->
                                tripDao.insertTrip(trip.toEntity())
                            }
                        }
                    }
                }
        }
    }

    fun getTripsFlow(): Flow<List<Trip>> {
        val userId = getUserId()
        if (userId.isEmpty()) {
            return flow { emit(emptyList()) }
        }

        return tripDao.getAllTrips(userId)
            .catch { e ->
                Log.e("TripRepository", "Error loading trips from Room", e)
                emit(emptyList())
            }
            .let { roomFlow ->
                flow {
                    roomFlow.collect { entities ->
                        emit(entities.map { it.toTrip() })
                    }
                }
            }
    }

    suspend fun addTrip(trip: Trip): Result<String> {
        return try {
            val userId = getUserId()
            if (userId.isEmpty()) {
                return Result.failure(Exception("User not authenticated"))
            }

            val tripWithUserId = trip.copy(
                userId = userId,
                id = if (trip.id.isEmpty()) generateTripId() else trip.id
            )

            val entity = tripWithUserId.toEntity().copy(isSynced = false)
            tripDao.insertTrip(entity)

            try {
                getTripsCollection()
                    .document(tripWithUserId.id)
                    .set(tripWithUserId)
                    .await()

                tripDao.markAsSynced(tripWithUserId.id)
            } catch (e: Exception) {
                Log.e("TripRepository", "Failed to sync to Firestore, will retry later", e)
            }

            Result.success(tripWithUserId.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTrip(trip: Trip): Result<Unit> {
        return try {
            if (trip.id.isEmpty()) {
                return Result.failure(Exception("Trip ID is required"))
            }

            val entity = trip.toEntity().copy(
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )
            tripDao.updateTrip(entity)

            try {
                getTripsCollection()
                    .document(trip.id)
                    .set(trip)
                    .await()

                tripDao.markAsSynced(trip.id)
            } catch (e: Exception) {
                Log.e("TripRepository", "Failed to sync update to Firestore", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTrip(tripId: String): Result<Unit> {
        return try {
            tripDao.deleteTrip(tripId)

            try {
                getTripsCollection()
                    .document(tripId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.e("TripRepository", "Failed to delete from Firestore", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTripById(tripId: String): Result<Trip?> {
        return try {
            val entity = tripDao.getTripById(tripId)
            Result.success(entity?.toTrip())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncUnsyncedTrips() {
        try {
            val unsyncedTrips = tripDao.getUnsyncedTrips()
            unsyncedTrips.forEach { entity ->
                try {
                    val trip = entity.toTrip()
                    getTripsCollection()
                        .document(trip.id)
                        .set(trip)
                        .await()

                    tripDao.markAsSynced(trip.id)
                } catch (e: Exception) {
                    Log.e("TripRepository", "Failed to sync trip ${entity.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("TripRepository", "Error syncing unsynced trips", e)
        }
    }

    suspend fun clearLocalCache(userId: String) {
        try {
            tripDao.deleteAllTrips(userId)
        } catch (e: Exception) {
            Log.e("TripRepository", "Error clearing cache", e)
        }
    }

    private fun generateTripId(): String {
        return getTripsCollection().document().id
    }
}