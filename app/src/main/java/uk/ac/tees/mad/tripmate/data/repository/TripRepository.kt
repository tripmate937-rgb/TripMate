package uk.ac.tees.mad.tripmate.data.repository


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uk.ac.tees.mad.tripmate.data.model.Trip

class TripRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String = auth.currentUser?.uid ?: ""

    private fun getTripsCollection() = firestore
        .collection("users")
        .document(getUserId())
        .collection("trips")

    fun getTripsFlow(): Flow<List<Trip>> = callbackFlow {
        val userId = getUserId()
        if (userId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = getTripsCollection()
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val trips = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Trip::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(trips)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addTrip(trip: Trip): Result<String> {
        return try {
            val userId = getUserId()
            if (userId.isEmpty()) {
                return Result.failure(Exception("User not authenticated"))
            }

            val tripWithUserId = trip.copy(userId = userId)
            val docRef = getTripsCollection().add(tripWithUserId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTrip(trip: Trip): Result<Unit> {
        return try {
            if (trip.id.isEmpty()) {
                return Result.failure(Exception("Trip ID is required"))
            }

            getTripsCollection()
                .document(trip.id)
                .set(trip)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTrip(tripId: String): Result<Unit> {
        return try {
            getTripsCollection()
                .document(tripId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTripById(tripId: String): Result<Trip?> {
        return try {
            val doc = getTripsCollection()
                .document(tripId)
                .get()
                .await()

            val trip = doc.toObject(Trip::class.java)?.copy(id = doc.id)
            Result.success(trip)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}