package edu.illinois.cs.cs124.ay2022.mp.network

import android.os.Build
import android.util.Log
import com.android.volley.ExecutorDelivery
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.VolleyLog
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.NoCache
import com.android.volley.toolbox.StringRequest
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import edu.illinois.cs.cs124.ay2022.mp.application.FavoritePlacesApplication
import edu.illinois.cs.cs124.ay2022.mp.models.Place
import edu.illinois.cs.cs124.ay2022.mp.models.ResultMightThrow
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.Executors
import java.util.function.Consumer

// You may find this useful when debugging
private val TAG = Client::class.java.simpleName

// We are using the Jackson JSON serialization library to deserialize data from the server
private val objectMapper = jacksonObjectMapper().apply {
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

/*
 * Client object used by the app to interact with the place API server.
 *
 * In Kotlin a class declared using object rather than class follows what is called the singleton pattern, meaning
 * that there can only be up to 1 instance of that class created, and it will be created as soon as it is needed.
 * Other than that it functions identically to the classes that you are familiar with, including allowing us to use
 * init blocks, define functions, and store state.
 *
 * You will need to understand some of the code here and make changes starting with MP2.
 */
object Client {
    /*
     * Retrieve and deserialize a list of places from the backend server.
     * Takes as an argument a callback method to call when the request completes which will be passed
     * the deserialized list of places received from the server, wrapped in a ResultMightThrow
     * to allow us to also report errors.
     * We will discuss callbacks in more detail once you need to augment this code in MP2.
     */
    fun getPlaces(callback: Consumer<ResultMightThrow<List<Place>>>) {
        /*
         * Construct the request itself.
         * We use a StringRequest allowing us to receive a String from the server.
         * The String will be valid JSON containing a list of place objects which we can deserialize into instances
         * of our Place model.
         */
        val getPlacesRequest = StringRequest(
            Request.Method.GET,
            "${FavoritePlacesApplication.SERVER_URL}/places/",
            { response: String? ->
                // This code runs on success
                /*
                 * Deserialize the String into a List<Restaurant> using Jackson.
                 * The TypeReference<List<Place>>() {} is a bit of magic required to have Jackson
                 * return a List with the correct type.
                 * We wrap this in a try-catch to handle deserialization errors that may occur.
                 */
                try {
                    val places = objectMapper.readValue(
                        response,
                        object : TypeReference<List<Place>>() {}
                    )
                    // Pass the List<Place> to the callback
                    callback.accept(ResultMightThrow(places))
                } catch (error: JsonProcessingException) {
                    // Pass the Exception to the callback on error
                    callback.accept(ResultMightThrow(error))
                }
            },
            { error: VolleyError? ->
                // This code runs on failure
                // Pass the Exception to the callback on error
                callback.accept(ResultMightThrow(error))
            }
        )
        // Actually queue the request
        // The callbacks above will be run once it completes
        requestQueue.add(getPlacesRequest)
    }

    @Suppress("UNUSED_PARAMETER")
    fun postFavoritePlace(place: Place, callback: Consumer<ResultMightThrow<Boolean>>) {
        val postFavoritePlaceRequest = object : StringRequest(
            Request.Method.POST,
            "${FavoritePlacesApplication.SERVER_URL}/favoriteplace/",
            { response: String? ->
                // This code runs on success
                /*
                 * Deserialize the String into a List<Restaurant> using Jackson.
                 * The TypeReference<List<Place>>() {} is a bit of magic required to have Jackson
                 * return a List with the correct type.
                 * We wrap this in a try-catch to handle deserialization errors that may occur.
                 */
                try {
                    val places = objectMapper.readValue(
                        response,
                        object : TypeReference<List<Place>>() {}
                    )
                    // Pass the List<Place> to the callback
                    callback.accept(ResultMightThrow(setResult = false))
                } catch (error: JsonProcessingException) {
                    // Pass the Exception to the callback on error
                    callback.accept(ResultMightThrow(setResult = true))
                }
            },
            { error: VolleyError? ->
                // This code runs on failure
                // Pass the Exception to the callback on error
                callback.accept(ResultMightThrow(error))
            }
        ) {
            override fun getBody(): ByteArray {
                return objectMapper.writeValueAsString(place).toByteArray()
            }

            override fun getBodyContentType() = "application/json; charset=utf-8"

        }
        // Actually queue the request
        // The callbacks above will be run once it completes
        requestQueue.add(postFavoritePlaceRequest)
    }

    /*
     * You do not need to modify the code below.
     * However, you may want to understand how it works.
     * The client tests to make sure it can connect to the backend server on startup.
     * We also initialize the client somewhat differently depending on whether we are testing your code or actually
     * running the app.
     */

    private const val INITIAL_CONNECTION_RETRY_DELAY = 1000L
    private const val MAX_STARTUP_RETRIES = 8
    private const val THREAD_POOL_SIZE = 4

    var connected = false
    private val requestQueue = if (Build.FINGERPRINT == "robolectric") {
        RequestQueue(
            NoCache(),
            BasicNetwork(HurlStack()),
            THREAD_POOL_SIZE,
            ExecutorDelivery(Executors.newSingleThreadExecutor())
        )
    } else {
        RequestQueue(
            NoCache(),
            BasicNetwork(HurlStack())
        )
    }

    init {
        VolleyLog.DEBUG = false

        HttpURLConnection.setFollowRedirects(true)

        val serverURL: URL = try {
            URL(FavoritePlacesApplication.SERVER_URL)
        } catch (e: MalformedURLException) {
            Log.e(TAG, "Bad server URL: ${FavoritePlacesApplication.SERVER_URL}")
            throw e
        }

        Thread {
            repeat(MAX_STARTUP_RETRIES) {
                @Suppress("EmptyCatchBlock")
                try {
                    (serverURL.openConnection() as HttpURLConnection).apply {
                        connect()
                        check(inputStream.bufferedReader().readText() == "CS 124")
                        disconnect()
                    }
                    connected = true
                    requestQueue.start()
                    return@Thread
                } catch (_: Exception) {
                }
                try {
                    Thread.sleep(INITIAL_CONNECTION_RETRY_DELAY)
                } catch (ignored: InterruptedException) {
                }
            }
            error { "Could not connect to server" }
        }.start()
    }

    // Empty method to create singleton
    fun start() {}
}
