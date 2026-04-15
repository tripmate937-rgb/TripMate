package uk.ac.tees.mad.tripmate.data.mapper

import uk.ac.tees.mad.tripmate.data.local.TripEntity
import uk.ac.tees.mad.tripmate.data.model.Trip

fun Trip.toEntity(): TripEntity {
    return TripEntity(
        id = id,
        title = title,
        destination = destination,
        latitude = latitude,
        longitude = longitude,
        startDate = startDate,
        endDate = endDate,
        activities = activities,
        userId = userId,
        createdAt = createdAt?.time ?: System.currentTimeMillis(),
        updatedAt = updatedAt?.time ?: System.currentTimeMillis(),
        isSynced = true
    )
}

fun TripEntity.toTrip(): Trip {
    return Trip(
        id = id,
        title = title,
        destination = destination,
        latitude = latitude,
        longitude = longitude,
        startDate = startDate,
        endDate = endDate,
        activities = activities,
        userId = userId,
        createdAt = java.util.Date(createdAt),
        updatedAt = java.util.Date(updatedAt)
    )
}