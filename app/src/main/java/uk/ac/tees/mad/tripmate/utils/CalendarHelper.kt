package uk.ac.tees.mad.tripmate.utils

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import uk.ac.tees.mad.tripmate.data.model.Trip
import java.util.*

object CalendarHelper {

    fun hasCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CALENDAR
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun addTripToCalendar(context: Context, trip: Trip): Long {
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, trip.startDate)
            put(CalendarContract.Events.DTEND, trip.endDate)
            put(CalendarContract.Events.TITLE, trip.title)
            put(CalendarContract.Events.DESCRIPTION, "Destination: ${trip.destination}\n\nActivities: ${trip.activities}")
            put(CalendarContract.Events.CALENDAR_ID, 1)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.EVENT_LOCATION, trip.destination)
        }

        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        return uri?.lastPathSegment?.toLongOrNull() ?: -1
    }
}