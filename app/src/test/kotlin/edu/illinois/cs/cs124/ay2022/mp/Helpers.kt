@file:Suppress("unused")

package edu.illinois.cs.cs124.ay2022.mp

import android.os.Looper
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.matcher.ViewMatchers
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.truth.Truth.assertThat
import com.opencsv.CSVReaderBuilder
import edu.illinois.cs.cs124.ay2022.mp.activities.MainActivity
import edu.illinois.cs.cs124.ay2022.mp.models.Place
import edu.illinois.cs.cs124.ay2022.mp.network.Server
import org.hamcrest.Matchers
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowLog
import java.io.BufferedReader
import java.io.StringReader
import java.math.BigInteger
import java.security.MessageDigest
import java.util.Random
import kotlin.math.abs

/*
 * This file contains helper code used by the test suites.
 * You should not need to modify it.
 * ALL CHANGES TO THIS FILE WILL BE OVERWRITTEN DURING OFFICIAL GRADING.
 */

// Number of places that we expect
const val PLACES_COUNT = 58

// Map boundaries
const val MAP_LIMIT_NORTH = 40.1741
const val MAP_LIMIT_SOUTH = 40.0247
const val MAP_LIMIT_WEST = -88.3331
const val MAP_LIMIT_EAST = -88.1433

// ObjectMapper properly configured for testing
val objectMapper = jacksonObjectMapper().apply {
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

// List of places used for testing
val defaultPlaces = loadPlacesFromCSV()

// Helper method to start the activity
fun startActivity(): ActivityScenario<MainActivity> = ActivityScenario.launch(MainActivity::class.java).apply {
    moveToState(Lifecycle.State.CREATED)
    moveToState(Lifecycle.State.RESUMED)
}.also {
    assert(Server.isRunning(true))
}

// Check the CSV for changes which might break our tests
internal fun checkCSV(): String {
    val input = Server::class.java.getResourceAsStream("/places.csv")
        ?.bufferedReader()
        ?.use(BufferedReader::readText)
        ?: error("Couldn't load places.csv")
    val fingerprint = fingerprintCSV(input)
    check(fingerprint == PLACES_HASH) { "places.csv was modified" }
    return fingerprint
}

// Helpers to load data from the places.csv file
fun loadPlacesFromCSV(): List<Place> {
    checkCSV()
    return objectMapper.readValue(loadPlacesStringFromCSV(), object : TypeReference<List<Place>>() {})
        .sortedBy { it.id }
        .also {
            check(it.size == PLACES_COUNT)
        }
}

fun loadPlacesStringFromCSV(): String {
    val input = Server::class.java.getResourceAsStream("/places.csv")
        ?.bufferedReader()
        ?.use(BufferedReader::readText)
        ?: error("Couldn't load places.csv")
    val csvReader = CSVReaderBuilder(StringReader(input)).withSkipLines(2).build()
    val places = JsonNodeFactory.instance.arrayNode()
    for (parts in csvReader) {
        val place = JsonNodeFactory.instance.objectNode()
        place.put("id", parts[0])
        place.put("name", parts[1])
        place.put("latitude", parts[2].toDouble())
        place.put("longitude", parts[3].toDouble())
        place.put("description", parts[4])
        places.add(place)
    }
    return places.toPrettyString()
}

// Get a list of all the Markers on the map, since not every Overlay is a Marker
fun getMarkers(mapView: MapView) = mapView.overlays.filterIsInstance<Marker>()

fun countMarkers(mapView: MapView) = getMarkers(mapView).size

// View assertions used by the MP test suites
fun countMarkersOverlay(expected: Int) = ViewAssertion { view: View, noViewFoundException: NoMatchingViewException? ->
    if (noViewFoundException != null) {
        throw noViewFoundException
    }
    check(view is MapView) { "View passed to countMarkersOverlay should be a MapView" }
    assertThat(countMarkers(view)).isEqualTo(expected)
}

fun searchFor(query: String, submit: Boolean = false) = object : ViewAction {
    override fun getConstraints() = Matchers.allOf(ViewMatchers.isDisplayed())
    override fun getDescription() = when (submit) {
        true -> "Set query to $query and submit"
        false -> "Set query to $query but don't submit"
    }

    override fun perform(uiController: UiController, view: View) = (view as SearchView).setQuery(query, submit)
}

// Pause helper to improve the stability of our Robolectric tests
fun pause(length: Long = 100) {
    Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks()
    Thread.sleep(length)
}

// Set up logging properly for testing
fun configureLogging() {
    if (System.getenv("OFFICIAL_GRADING") == null) {
        ShadowLog.stream = System.out
    }
}

private const val MAX_GEOPOINT_DIFF = 0.00001

// Fuzzy comparison of GeoPoints using the maximum diff defined above
fun compareGeopoints(first: IGeoPoint, second: IGeoPoint): Boolean {
    val latDiff = abs(first.latitude - second.latitude)
    val lonDiff = abs(first.longitude - second.longitude)
    return latDiff < MAX_GEOPOINT_DIFF && lonDiff < MAX_GEOPOINT_DIFF
}

const val PLACES_HASH = "40537703c53990288b03612e29ac5914"

// Fingerprint the CSV file to check for changes
internal fun fingerprintCSV(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    val filtered = input.lines().filter { !it.startsWith("# md5: ") }.joinToString("\n")
    return BigInteger(1, md.digest(filtered.toByteArray())).toString(16).padStart(32, '0')
}

// Various helper methods for creating Place objects and JSON strings
fun makePlaceJSON(
    id: String?,
    name: String?,
    latitude: Double?,
    longitude: Double?,
    description: String?
): String = JsonNodeFactory.instance.objectNode().apply {
    put("id", id)
    put("name", name)
    if (latitude != null) {
        put("latitude", latitude)
    }
    if (longitude != null) {
        put("longitude", longitude)
    }
    put("description", description)
}.toPrettyString()

fun makePlaceFromJson(
    id: String?,
    name: String?,
    latitude: Double?,
    longitude: Double?,
    description: String?
): Place = objectMapper.readValue(makePlaceJSON(id, name, latitude, longitude, description), Place::class.java)

// Grab a random GeoPoint within our map bounds
fun randomGeoPointOnMap(random: Random): GeoPoint {
    val randomLatitude = (MAP_LIMIT_NORTH - MAP_LIMIT_SOUTH) * random.nextDouble() + MAP_LIMIT_SOUTH
    val randomLongitude: Double = (MAP_LIMIT_EAST - MAP_LIMIT_WEST) * random.nextDouble() + MAP_LIMIT_WEST
    return GeoPoint(randomLatitude, randomLongitude)
}

// Grab a random GeoPoint that is actually visible on the map as currently zoomed and panned
fun randomGeoPointInMap(random: Random, mapView: MapView): GeoPoint {
    val center = mapView.mapCenter
    val northBorder = center.latitude - mapView.latitudeSpanDouble / 2.0
    val southBorder = center.latitude + mapView.latitudeSpanDouble / 2.0
    val westBorder = center.longitude - mapView.longitudeSpanDouble / 2.0
    val eastBorder = center.longitude + mapView.longitudeSpanDouble / 2.0
    val randomLatitude = (northBorder - southBorder) * random.nextDouble() + southBorder
    val randomLongitude = (eastBorder - westBorder) * random.nextDouble() + westBorder
    return GeoPoint(randomLatitude, randomLongitude)
}
