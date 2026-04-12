package uk.ac.tees.mad.tripmate.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val destination: String,
    val latitude: Double,
    val longitude: Double,
    val startDate: Long,
    val endDate: Long,
    val activities: String,
    val userId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isSynced: Boolean = false
)