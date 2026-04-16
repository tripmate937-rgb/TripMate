package uk.ac.tees.mad.tripmate.utils

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import uk.ac.tees.mad.tripmate.data.model.Trip
import java.util.*

object CalendarHelper {

    private const val TAG = "CalendarHelper"

    fun hasCalendarPermission(context: Context): Boolean {
        val writePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        val readPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "Write: $writePermission, Read: $readPermission")
        return writePermission && readPermission
    }

    private fun getAllCalendars(context: Context): List<Triple<Long, String, Boolean>> {
        val calendars = mutableListOf<Triple<Long, String, Boolean>>()
        try {
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
            )

            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.VISIBLE} = 1",
                null,
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    val displayName = it.getString(1)
                    val accountName = it.getString(2)
                    val accessLevel = it.getInt(3)

                    val canWrite = accessLevel >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR

                    calendars.add(Triple(id, displayName, canWrite))
                    Log.d(TAG, "Calendar: ID=$id, Name=$displayName, Account=$accountName, CanWrite=$canWrite")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting calendars", e)
        }
        return calendars
    }

    private fun getDefaultCalendarId(context: Context): Long {
        val calendars = getAllCalendars(context)
        if (calendars.isEmpty()) {
            Log.e(TAG, "No calendars found!")
            return -1
        }

        val writablePersonalCalendar = calendars.firstOrNull { (_, name, canWrite) ->
            canWrite && !name.contains("Holidays", ignoreCase = true) && !name.contains("Holiday", ignoreCase = true)
        }

        val anyWritableCalendar = calendars.firstOrNull { (_, _, canWrite) -> canWrite }

        val selectedCalendar = writablePersonalCalendar ?: anyWritableCalendar ?: calendars.first()

        Log.d(TAG, "✅ Selected calendar: ${selectedCalendar.second} (ID: ${selectedCalendar.first}, Writable: ${selectedCalendar.third})")
        return selectedCalendar.first
    }

    fun addTripToCalendar(context: Context, trip: Trip): Long {
        Log.d(TAG, "=== ADDING TRIP TO CALENDAR ===")
        Log.d(TAG, "Trip: ${trip.title}")
        Log.d(TAG, "Destination: ${trip.destination}")

        if (!hasCalendarPermission(context)) {
            Log.e(TAG, "❌ No calendar permission!")
            return -1
        }

        val calendarId = getDefaultCalendarId(context)
        if (calendarId == -1L) {
            Log.e(TAG, "❌ No valid calendar found!")
            return -1
        }

        val startCal = Calendar.getInstance().apply {
            timeInMillis = trip.startDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = Calendar.getInstance().apply {
            timeInMillis = trip.endDate
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        Log.d(TAG, "Start: ${startCal.time}")
        Log.d(TAG, "End: ${endCal.time}")

        return try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, "🧳 ${trip.title}")
                put(CalendarContract.Events.DESCRIPTION, buildString {
                    append("📍 Destination: ${trip.destination}\n\n")
                    if (trip.activities.isNotBlank()) {
                        append("📝 Activities:\n${trip.activities}\n\n")
                    }
                    append("Created with TripMate")
                })
                put(CalendarContract.Events.EVENT_LOCATION, trip.destination)
                put(CalendarContract.Events.DTSTART, startCal.timeInMillis)
                put(CalendarContract.Events.DTEND, endCal.timeInMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.ALL_DAY, 1)
                put(CalendarContract.Events.HAS_ALARM, 0)
                put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            }

            Log.d(TAG, "Inserting into calendar ID: $calendarId")

            val uri = context.contentResolver.insert(
                CalendarContract.Events.CONTENT_URI,
                values
            )

            if (uri != null) {
                val eventId = uri.lastPathSegment?.toLongOrNull() ?: -1
                Log.d(TAG, "✅✅✅ SUCCESS! Event created!")
                Log.d(TAG, "Event ID: $eventId")
                Log.d(TAG, "Event URI: $uri")
                Log.d(TAG, "Check your calendar app now!")
                eventId
            } else {
                Log.e(TAG, "❌ Insert returned null URI!")
                -1
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Security error: ${e.message}")
            e.printStackTrace()
            -1
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "❌ Illegal argument: ${e.message}")
            e.printStackTrace()
            -1
        } catch (e: Exception) {
            Log.e(TAG, "❌ Unexpected error: ${e.message}", e)
            e.printStackTrace()
            -1
        }
    }
}