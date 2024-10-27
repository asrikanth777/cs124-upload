package edu.illinois.cs.cs124.ay2022.mp.network

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opencsv.CSVReaderBuilder
import edu.illinois.cs.cs124.ay2022.mp.application.FavoritePlacesApplication
import edu.illinois.cs.cs124.ay2022.mp.models.Place
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.net.HttpURLConnection
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

/*
 * Favorite Place API server.
 *
 * Normally this code would run on a separate machine from your app, which would make requests to
 * it over the internet.
 * However, for our MP we have this run inside the app alongside the rest of your code, to allow
 * you to gain experience with full-stack app development.
 * You are both developing the client (the Android app) and the server that it requests data from.
 * This is a very common programming paradigm and one used by most or all of the smartphone apps
 * that you use regularly.
 *
 * You will need to some of the code here and make changes starting with MP1.
 */

// We are using the Jackson JSON serialization library to serialize and deserialize data on the server
private val objectMapper = jacksonObjectMapper().apply {
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
}

/*
 * Load place information from a CSV file and create a List<Place>.
 * You will need to examine and modify this code for MP1.
 */
private fun loadPlaces(): MutableList<Place> {
    // An unfortunate bit of code required to read an entire stream into a `String`
    // Blame Java for this mess
    val input = Server::class.java.getResourceAsStream("/places.csv")
        ?.bufferedReader()
        ?.use(BufferedReader::readText)
        ?: error("Couldn't load places.csv")

    // We skip the first two lines in the CSV
    // The first is a hash for verifying integrity during testing, and the second is the header
    val csvReader = CSVReaderBuilder(StringReader(input)).withSkipLines(2).build()

    // Load all CSV rows into a list and return
    val toReturn: MutableList<Place> = ArrayList()
    for (parts in csvReader) {
        toReturn.add(Place(parts[0], parts[1], parts[2].toDouble(), parts[3].toDouble(), parts[4]))
    }
    return toReturn
}

object Server : Dispatcher() {
    // Stores the List<Place> containing information about all of the favorite places created during
    // server startup
    private var places: MutableList<Place> = loadPlaces()

    // Helper method for the GET /places route, called by the dispatch method below
    private fun getPlaces(): MockResponse =
        MockResponse()
            // Indicate that the request succeeded (HTTP 200 OK)
            .setResponseCode(HttpURLConnection.HTTP_OK)
            // Load the JSON string with place information into the body of the response
            // We use Jackson to serialize the List<Place> to a String
            .setBody(objectMapper.writeValueAsString(places))
            /*
             * Set the HTTP header that indicates that this is JSON with the utf-8 charset.
             * There may be special characters in our data set, so it's important to mark it as utf-8
             * so it is parsed properly by clients.
             */
            .setHeader("Content-Type", "application/json; charset=utf-8")

    private fun postFavoritePlace(request: RecordedRequest): MockResponse {

        try {
            var place1 = objectMapper.readValue(request.body.readUtf8(), Place::class.java)
            require(place1.id != null && place1.id != "")
            require(place1.name != null && place1.name != "")
            require(place1.latitude >= -90.0 && place1.latitude <= 90.0)
            require(place1.longitude >= -180.0 && place1.longitude <= 180.0)
            require(place1.description != null && place1.description != "")
            require(UUID.fromString(place1.id).toString() == place1.id)
            for(i in places.indices) {
                if (place1.id == places[i].id) {
                    places[i] = place1
                    return MockResponse()
                        .setResponseCode(HttpURLConnection.HTTP_OK)
                        .setHeader("Content-Type","application/json; charset=utf-8")
                }
            }
            places.add(place1)
        } catch(e: Exception) {
            return MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .setHeader("Content-Type","application/json; charset=utf-8")
        }







        // create place object
        // validate input
        // take data from request and put into object
        // push object into list of places

        // Have a valid Place object, insert it into our list of places
        // if the ID already exists in the list, replace it
        // Otherwise, add it
        return MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setHeader("Content-Type", "application/json; charset=utf-8")
    }
    /*
     * Server request dispatcher.
     * Responsible for parsing the HTTP request and determining how to respond.
     * You will need to understand this code and augment it starting with MP2.
     */
    override fun dispatch(request: RecordedRequest): MockResponse {
        // Reject malformed requests
        if (request.path == null || request.method == null) {
            return MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
        }
        return try {
            /*
             * We perform a few normalization steps before we begin route dispatch, since this makes the
             * when statement below simpler.
             */

            // Normalize the path by removing trailing slashes and replacing multiple repeated slashes
            // with single slashes
            val path = request.path!!.removeSuffix("/").replace("/+".toRegex(), "/")
            // Normalize the request method by converting to uppercase
            val method = request.method!!.uppercase()

            // Main route dispatch tree, dispatching routes based on request path and type
            when {
                // This route is used by the client during startup, so don't remove
                path == "" && method == "GET" ->
                    MockResponse().setBody("CS 124").setResponseCode(HttpURLConnection.HTTP_OK)
                // This route is used during testing, so don't remove or alter
                path == "/reset" && method == "GET" -> {
                    places = loadPlaces()
                    MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                }
                // Return the JSON list of restaurants for a GET request to the path /restaurants
                path == "/places" && method == "GET" -> getPlaces()
                path == "/favoriteplace" && method == "POST" -> postFavoritePlace(request)
                // If the route didn't match above, then we return a 404 NOT FOUND
                else -> MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
                    // If we don't set a body here Volley will choke with a strange error
                    // Normally a 404 for a web API would not need a body
                    .setBody("Not Found")
            }
        } catch (e: Exception) {
            // Return a HTTP 500 if an exception is thrown
            // You may need to add logging here during later checkpoints
            MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
        }
    }

    /*
     * You do not need to modify the code below.
     * However, you may want to understand how it works.
     * It implements the singleton pattern and initializes the server when Server.start() is called.
     * We also check to make sure that no other servers are running on the same machine,
     * which can cause problems.
     */
    init {
        if (!isRunning(false)) {
            Logger.getLogger(MockWebServer::class.java.name).level = Level.OFF
            MockWebServer().apply {
                dispatcher = this@Server
                start(FavoritePlacesApplication.DEFAULT_SERVER_PORT)
            }
        }
    }

    // Empty method to create singleton
    fun start() {}

    fun isRunning(wait: Boolean, retryCount: Int = 8, retryDelay: Long = 512): Boolean {
        for (i in 0 until retryCount) {
            val client = OkHttpClient()
            val request: Request = Request.Builder().url(FavoritePlacesApplication.SERVER_URL).get().build()
            try {
                val response = client.newCall(request).execute()
                check(response.isSuccessful)
                check(response.body?.string() == "CS 124") {
                    "Another server is running on ${FavoritePlacesApplication.DEFAULT_SERVER_PORT}"
                }
                return true
            } catch (ignored: IOException) {
                if (!wait) {
                    break
                }
                try {
                    Thread.sleep(retryDelay)
                } catch (ignored1: InterruptedException) {
                }
            }
        }
        return false
    }

    fun reset() {
        val request = Request.Builder().url("${FavoritePlacesApplication.SERVER_URL}/reset/").get().build()
        val response = OkHttpClient().newCall(request).execute()
        check(response.isSuccessful)
    }
}
