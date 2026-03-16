package uk.ac.tees.mad.tripmate

import android.app.Application
import com.google.firebase.FirebaseApp

class TripMateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}

