package edu.illinois.cs.cs124.ay2022.mp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import edu.illinois.cs.cs124.ay2022.mp.R
import edu.illinois.cs.cs124.ay2022.mp.models.Place
import edu.illinois.cs.cs124.ay2022.mp.models.ResultMightThrow
import edu.illinois.cs.cs124.ay2022.mp.models.search
import edu.illinois.cs.cs124.ay2022.mp.network.Client
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.function.Consumer


/*
 * App main activity.
 * Started when the app is launched, based on the configuration in the Android Manifest (AndroidManifest.xml).
 * Should display places on the map based on data retrieved from the server.
 *
 * You will need to understand some of the code here and make changes to complete most project checkpoints.
 */

// You may find this useful when adding logging
@Suppress("unused")
private val TAG = MainActivity::class.java.name

// Map boundaries, used to limit the scrollable area.
// Our tile server does not provide tiles outside this geographic region.
const val MAP_LIMIT_NORTH = 40.1741
const val MAP_LIMIT_SOUTH = 40.0247
const val MAP_LIMIT_WEST = -88.3331
const val MAP_LIMIT_EAST = -88.1433

// Max and default map zoom levels
const val MAP_MIN_ZOOM = 12.0
const val MAP_DEFAULT_ZOOM = 17.0

// Default MAp
class MainActivity :
    AppCompatActivity(),
    Consumer<ResultMightThrow<List<Place>>>, SearchView.OnQueryTextListener, MapEventsReceiver {

    // Reference to the MapView, initialized in onCreate, handy to have in other places
    // Marking the variable as lateinit allows us to not initialize it in the constructor but also have it be
    // not nullable
    // If it is used before it is initialized with a non-null value, an exception will be thrown
    private lateinit var mapView: MapView

    // List of all places retrieved from the server, initially set to an empty list to avoid nullability
    private var allPlaces = listOf<Place>()

    // ID of the currently open place, used to keep the same popup open when the list of places is updated
    // nullable because null indicates no currently open popup
    private var openPlace: String? = null

    /*
     * onCreate is the first method called when this activity is created.
     * Code here normally does a variety of setup tasks, and functions somewhat similarly to a constructor.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the layout for this activity and set the title
        setContentView(R.layout.activity_main)
        title = "Favorite Places"

        // Find the MapView component in the layout and configure it properly
        // Also save the reference for later use
        mapView = findViewById(R.id.map)

        val searchView: SearchView = findViewById(R.id.search)
        searchView.setOnQueryTextListener(this)

        // A OpenStreetMaps tile source provides the tiles that are used to render the map.
        // We use our own tile source with relatively-recent tiles for the Champaign-Urbana area, to avoid adding
        // load to existing OSM tile servers.
        mapView.setTileSource(
            XYTileSource(
                "CS124",
                12,
                18,
                256,
                ".png",
                arrayOf("https://tiles.cs124.org/tiles/")
            )
        )

        // Limit the map to the Champaign-Urbana area, which is also the only area that our tile server can provide
        // tiles for.
        mapView.setScrollableAreaLimitLatitude(MAP_LIMIT_NORTH, MAP_LIMIT_SOUTH, 0)
        mapView.setScrollableAreaLimitLongitude(MAP_LIMIT_WEST, MAP_LIMIT_EAST, 0)

        // Only allow zooming out so far
        mapView.minZoomLevel = MAP_MIN_ZOOM

        // Set the current map zoom level to the default
        mapView.controller.setZoom(MAP_DEFAULT_ZOOM)

        val p = GeoPoint(40.10986682167534, -88.22831928981661)
        mapView.controller.setCenter(p)
    }

    /*
     * onResume is called right before the activity begins interacting with the user.
     * So this is a good time to update our list of places.
     * We pass the MainActivity as the callback to the call to getPlaces, which is why this class implements
     * Consumer<ResultMightThrow<List<Place>>>, a functional interface allowing our networking client to pass back
     * the list of places to us once the network call completes.
     * We'll discuss this more when we talk about networking in Android on MP2.
     */
    override fun onResume() {
        super.onResume()
        Client.getPlaces(this)
    }

    /*
     * Called by code in Client.kt when the call to retrieve the list of places from the server completes.
     * We save the full list of places and update the UI.
     * Note the use of the ResultMightThrow to have the exception thrown and caught here.
     * This is due to how Android networking requests are handled.
     * For a longer explanation, see the note on ResultMightThrow.kt.
     */
    override fun accept(result: ResultMightThrow<List<Place>>) {
        // We use a try-catch because getResult throws if the result contains an exception
        try {
            // Save the list of all available places
            allPlaces = result.result
            // Update the UI to show all available places
            updateShownPlaces(allPlaces)
        } catch (e: Exception) {
            Log.e(TAG, "getPlaces threw an exception: " + result.exception)
        }
    }

    /*
     * Update the list of places shown on the map.
     *
     * Helper method used to convert our List<Place> to a set of markers that will appear on the map drawn by osmdroid.
     */
    private fun updateShownPlaces(showPlaces: List<Place>) {
        /*
         * Go through all existing overlays that are markers and close their popups.
         * If we don't do this, updates to the list of places that are currently visible can leave open popups that
         * aren't connected to any marker.
         * This seems like a bug in osmdroid,
         * reported here: https://github.com/osmdroid/osmdroid/issues/1858.
         */
        for (existing in mapView.overlays.filterIsInstance<Marker>()) {
            existing.closeInfoWindow()
        }

        // Clear all overlays and the ID of the currently open info window
        mapView.overlays.clear()
        var newOpenPlace: String? = null

        // Create markers for each place in our list and add them to the map
        for (place in showPlaces) {
            // Create a new Marker
            val marker = Marker(mapView)

            // Set the ID so that we can track which marker has an open popup
            marker.id = place.id

            // Set the position and other attributes appropriately
            marker.position = GeoPoint(place.latitude, place.longitude)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = place.description

            /*
             * Normally clicking on the marker both opens the popup and recenters the map.
             * The map recentering is a bit annoying, so we override this callback here to disable it.
             * The argument to setOnMarkerClickListener is just a lambda function called whenever the marker is clicked.
             * This also allows us to track which marker was open.
             */

            marker.setOnMarkerClickListener { m: Marker, _: MapView? ->
                openPlace = if (!m.isInfoWindowShown) {
                    m.showInfoWindow()
                    m.id
                } else {
                    m.closeInfoWindow()
                    null
                }
                true
            }

            // Preserve the currently open place if there was one, and reopen the popup on the
            // appropriate marker
            if (marker.id == openPlace) {
                marker.showInfoWindow()
                newOpenPlace = openPlace
            }

            // Add the marker to the map
            mapView.overlays.add(marker)
        }

        // Update the currently-open marker
        // This will clear openPlace if the marker that was previously shown is no longer open
        openPlace = newOpenPlace

        mapView.overlays.add(MapEventsOverlay(this))

        // Force the MapView to redraw so that we see the updated list of markers
        mapView.invalidate()
    }

    override fun onQueryTextChange(text: String?): Boolean {
        require(text != null)
        val newsearch = allPlaces.search(text)
        if (newsearch.size == 0) {
            updateShownPlaces(allPlaces)
        } else {
            updateShownPlaces(newsearch)
        }

        Log.d(TAG, "OnQueryTextSubmit $text")

        return false
    }

    override fun onQueryTextSubmit(text: String?): Boolean {
        Log.d(TAG, "OnQueryTextSubmit $text")
        return true
    }

    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {

        return false
    }

    override fun longPressHelper(p: GeoPoint): Boolean {
        val startAddPlaceActivity = Intent(this, AddPlaceActivity::class.java)
        startAddPlaceActivity.putExtra("latitude", p.latitude.toString())
        startAddPlaceActivity.putExtra("longitude", p.longitude.toString())
        startActivity(startAddPlaceActivity)
        return false
    }
}
