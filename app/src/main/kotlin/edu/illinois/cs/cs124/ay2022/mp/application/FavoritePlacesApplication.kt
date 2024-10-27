package edu.illinois.cs.cs124.ay2022.mp.application

import android.app.Application
import android.os.Build
import edu.illinois.cs.cs124.ay2022.mp.network.Server

/*
 * One instance of the Application class is created when the app is launched and persists throughout its lifetime.
 * This is unlike activities, which are created and destroyed as the user navigates to different screens in the app.
 * As a result, the Application class can be a good place to store constants and initialize things that are
 * potentially needed by multiple activities.
 *
 * You may need to change the code in this file, but probably not that much.
 */
@Suppress("unused")
class FavoritePlacesApplication : Application() {

    // Called when the app is created
    override fun onCreate() {
        super.onCreate()

        // We start the API server differently depending on whether we are in a testing environment or not
        if (Build.FINGERPRINT == "robolectric") {
            Server.start()
        } else {
            Thread { Server.start() }.start()
        }
    }

    companion object {
        // Default API server port and URL
        // You can modify the port setting if it is conflicting with something else on your machine
        const val DEFAULT_SERVER_PORT = 8989

        const val SERVER_URL = "http://localhost:$DEFAULT_SERVER_PORT/"

        // Put your ID (from ID.txt) here
        const val CLIENT_ID = "4148a459-cf00-4f1a-98d9-54071cbbf3b4"
    }
}
