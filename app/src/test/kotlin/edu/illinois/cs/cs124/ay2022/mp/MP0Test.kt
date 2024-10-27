package edu.illinois.cs.cs124.ay2022.mp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import edu.illinois.cs.cs124.ay2022.mp.activities.MainActivity
import edu.illinois.cs.cs124.ay2022.mp.application.FavoritePlacesApplication
import edu.illinois.cs.cs124.ay2022.mp.models.Place
import edu.illinois.cs.cs124.ay2022.mp.models.ResultMightThrow
import edu.illinois.cs.cs124.ay2022.mp.network.Client
import edu.illinois.cs.cs124.ay2022.mp.network.Client.getPlaces
import edu.illinois.cs.cs124.ay2022.mp.network.Server
import edu.illinois.cs.cs125.gradlegrader.annotations.Graded
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.CompletableFuture

/*
 * This is the MP0 test suite.
 * The code below is used to evaluate your app during testing, local grading, and official grading.
 * You will probably not understand all of the code below, but you'll need to have some
 * understanding of how it works so that you can determine what is wrong with your app and what you
 * need to fix.
 *
 * ALL CHANGES TO THIS FILE WILL BE OVERWRITTEN DURING OFFICIAL GRADING.
 * You may modify the code below if it is useful during your own local testing, but any changes you
 * make will be lost once you submit.
 * Please keep that in mind, particularly if you see differences between your local scores and your
 * official scores.
 *
 * Our test suites are broken into two parts.
 * The unit tests (in the UnitTests class) are tests that we can perform without running your app.
 * They test things like whether a specific method works properly, or the behavior of your API
 * server.
 * Unit tests are usually fairly fast.
 *
 * The integration tests (in the IntegrationTests class) are tests that require simulating your app.
 * This allows us to test things like your API client, and higher-level aspects of your app's
 * behavior, such as whether it displays the right thing on the display.
 * Because integration tests require simulating your app, they run more slowly.
 *
 * Our test suites will also include a mixture of graded and ungraded tests.
 * The graded tests are marking with the `@Graded` annotation which contains a point total.
 * Ungraded tests do not have this annotation.
 * Some ungraded tests will work immediately, and are there to help you pinpoint regressions:
 * meaning changes that you made that might have broken things that were working previously.
 * The ungraded tests below were actually written by me (Geoff) during MP development.
 * Other ungraded tests are simply there to help your development process.
 */

// Where the map should be centered
private val DEFAULT_CENTER = GeoPoint(40.10986682167534, -88.22831928981661)

@RunWith(Enclosed::class)
class MP0Test {
    init {
        // Make sure the CSV has not been modified
        checkCSV()
    }

    // Unit tests that don't require simulating the entire app
    @FixMethodOrder(MethodSorters.NAME_ASCENDING)
    class UnitTests {

        // Create an HTTP client to test the server with
        private val httpClient = OkHttpClient()

        init {
            // Start the API server
            Server.start()
        }

        // Reset the server before each test
        @Before
        fun resetServer() {
            Server.reset()
        }

        // THIS TEST SHOULD WORK
        // Test whether the GET /places server route works properly
        @Test(timeout = 3000L)
        fun test0_PlacesRoute() {
            // Formulate a GET request to the API server for the /places/ route
            val placesRequest = Request.Builder().url(FavoritePlacesApplication.SERVER_URL + "/places/").build()
            // Execute the request
            val placesResponse: Response = httpClient.newCall(placesRequest).execute()

            // The request should have succeeded
            assertWithMessage("Request should have succeeded")
                .that(placesResponse.isSuccessful)
                .isTrue()

            // The response body should not be null
            val body = placesResponse.body
            assertWithMessage("Response body should not be null").that(body).isNotNull()

            // The response body should be a JSON array with the expected size
            val placeList = objectMapper.readTree(body!!.string())
            assertWithMessage("Places list is not the right size").that(placeList).hasSize(PLACES_COUNT)
        }
    }

    // Integration tests that require simulating the entire app
    @RunWith(AndroidJUnit4::class)
    @LooperMode(LooperMode.Mode.PAUSED)
    @Config(qualifiers = "w1080dp-h2088dp")
    @FixMethodOrder(MethodSorters.NAME_ASCENDING)
    class IntegrationTests {
        init {
            // Set up logging so that you can see log output during testing
            configureLogging()
            // Establish a separate API client for testing
            Client.start()
        }

        // Reset the server before each test
        @Before
        fun resetServer() {
            Server.reset()
        }

        // After each test make sure the client connected successfully
        @After
        fun checkClient() {
            assertWithMessage("Client should be connected").that(Client.connected).isTrue()
        }

        // Graded test that the activity displays the correct title
        @Graded(points = 40, friendlyName = "Test Activity Title")
        @Test(timeout = 30000L)
        fun test1_ActivityTitle() {
            // Start the main activity
            startActivity().onActivity { activity ->
                // Once the activity starts, check that it has the correct title
                assertWithMessage("MainActivity has wrong title").that(activity.title).isEqualTo("Favorite Places")
            }
        }

        // Graded test that the app centers the map correctly after launch
        @Graded(points = 50, friendlyName = "Test Map Center")
        @Test(timeout = 30000L)
        fun test2_MapCenter() {
            startActivity().onActivity { activity: MainActivity ->
                pause()
                val mapView = activity.findViewById<MapView>(R.id.map)
                assertThat(compareGeopoints(mapView.mapCenter, DEFAULT_CENTER)).isTrue()
            }
        }

        // THIS TEST SHOULD WORK
        // Test that the API client retrieves the list of places correctly
        @Test(timeout = 10000L)
        fun test3_ClientGetPlaces() {
            // A CompletableFuture allows us to wait for the result of an asynchronous call
            val completableFuture = CompletableFuture<ResultMightThrow<List<Place>>>()
            // When getPlaces returns, we complete the CompletableFuture
            getPlaces { places -> completableFuture.complete(places) }
            // Wait for the CompletableFuture to complete
            val result = completableFuture.get()
            assertWithMessage("getPlaces threw an exception").that(result.exception).isNull()

            // The List<Places> should not be null, which is returned by getPlaces when something went wrong
            assertWithMessage("Request failed").that(result.result).isNotNull()

            // Check that the List<Place> has the correct size
            assertWithMessage("Places list is not the right size").that(result.result).hasSize(PLACES_COUNT)
        }

        // THIS TEST SHOULD WORK
        // Test that the main activity displays the right number of places after launch
        @Test(timeout = 10000L)
        fun test4_ActivityPlaceCount() {
            startActivity().onActivity { activity: MainActivity ->
                pause()
                val mapView = activity.findViewById<MapView>(R.id.map)
                assertThat(countMarkers(mapView)).isEqualTo(PLACES_COUNT)
            }
        }
    }
}
// DO NOT REMOVE THIS LINE
// md5: a2b3642705fedcdc3a27a60ed42501b0
