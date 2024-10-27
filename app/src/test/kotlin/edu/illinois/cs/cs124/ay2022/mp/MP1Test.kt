@file:Suppress("SpellCheckingInspection")

package edu.illinois.cs.cs124.ay2022.mp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fasterxml.jackson.databind.node.ArrayNode
import com.google.common.truth.Truth.assertWithMessage
import edu.illinois.cs.cs124.ay2022.mp.application.FavoritePlacesApplication
import edu.illinois.cs.cs124.ay2022.mp.models.Place
import edu.illinois.cs.cs124.ay2022.mp.models.search
import edu.illinois.cs.cs124.ay2022.mp.network.Client
import edu.illinois.cs.cs124.ay2022.mp.network.Server
import edu.illinois.cs.cs125.gradlegrader.annotations.Graded
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.net.HttpURLConnection

@RunWith(Enclosed::class)
class MP1Test {

    // Unit tests that don't require simulating the entire app
    @FixMethodOrder(MethodSorters.NAME_ASCENDING)
    class UnitTests {
        // Create an HTTP client to test the server with
        private val httpClient = OkHttpClient()

        init {
            // Start the API server
            Server.start()
        }

        @Before
        fun resetServer() {
            Server.reset()
        }

        @Graded(points = 20, friendlyName = "Test Load Place Fields")
        @Test(timeout = 3000L)
        fun test0_LoadPlaceFields() {
            // Build a GET request for /places/
            val courseRequest = Request.Builder().url(FavoritePlacesApplication.SERVER_URL + "/places/").build()
            // Execute the request
            val courseResponse = httpClient.newCall(courseRequest).execute()
            // The request should have succeeded
            assertWithMessage("Request should succeed")
                .that(courseResponse.code)
                .isEqualTo(HttpURLConnection.HTTP_OK)
            // The response body should not be null
            val body = courseResponse.body
            assertWithMessage("Body should not be null").that(body).isNotNull()
            // The response body should be a JSON array
            val placesList = objectMapper.readTree(body!!.string())
            assertWithMessage("Request should return a JSON array")
                .that(placesList is ArrayNode)
                .isTrue()
            // The JSON array should contain the correct number of places
            assertWithMessage("Wrong place count").that(placesList).hasSize(PLACES_COUNT)

            // Check the JSON nodes for the correct fields
            var hardcodedDescription = ""
            for (placeNode in placesList) {
                assertWithMessage("JSON is missing field id").that(placeNode.has("id")).isTrue()
                assertWithMessage("JSON id field is empty")
                    .that(placeNode["id"].textValue())
                    .isNotEmpty()

                assertWithMessage("JSON is missing field name").that(placeNode.has("name")).isTrue()
                assertWithMessage("JSON name field is empty")
                    .that(placeNode["name"].textValue())
                    .isNotEmpty()

                assertWithMessage("JSON is missing field latitude")
                    .that(placeNode.has("latitude"))
                    .isTrue()
                assertWithMessage("JSON latitude field is wrong type")
                    .that(placeNode["latitude"].isDouble)
                    .isTrue()
                val latitude = placeNode["latitude"].asDouble()
                assertWithMessage("JSON latitude field is invalid")
                    .that(-90.0 <= latitude && latitude <= 90.0)
                    .isTrue()

                assertWithMessage("JSON is missing field longitude")
                    .that(placeNode.has("longitude"))
                    .isTrue()
                assertWithMessage("JSON longitude field is wrong type")
                    .that(placeNode["longitude"].isDouble)
                    .isTrue()
                val longitude = placeNode["longitude"].asDouble()
                assertWithMessage("JSON longitude field is invalid")
                    .that(-180.0 <= longitude && longitude <= 180.0)
                    .isTrue()

                assertWithMessage("JSON is missing field description")
                    .that(placeNode.has("description"))
                    .isTrue()
                assertWithMessage("JSON description field is empty")
                    .that(placeNode["description"].textValue())
                    .isNotEmpty()

                if (hardcodedDescription == "") {
                    hardcodedDescription = placeNode["description"].textValue()
                } else {
                    assertWithMessage("JSON description field is hardcoded")
                        .that(placeNode["description"].textValue())
                        .isNotEqualTo(hardcodedDescription)
                }
            }
        }

        private fun testSearchHelper(searchInput: String, expectedCount: Int) {
            val results = defaultPlaces.search(searchInput)
            assertWithMessage("search modified passed list").that(defaultPlaces).hasSize(PLACES_COUNT)
            assertWithMessage("incorrect search result for \"$searchInput\"")
                .that(results.size)
                .isEqualTo(expectedCount)
        }

        @Graded(points = 30, friendlyName = "Test Places Search Method")
        @Test(timeout = 3000L)
        fun test1_PlacesSearch() {
            assertWithMessage("Didn't handle empty list")
                .that(listOf<Place>().search("thai"))
                .hasSize(0)
            testSearchHelper("thai", 3)
            testSearchHelper(" THAI", 3)
            testSearchHelper("tHaI  ", 3)
            testSearchHelper("trex", 1)
            testSearchHelper("MCDONALDS", 1)
            testSearchHelper("some  ", 5)
            testSearchHelper("JOURNALS", 1)
            testSearchHelper("study", 4)
            testSearchHelper("vibeS", 1)
            testSearchHelper(" bOba ", 1)
            testSearchHelper("farmers", 1)
            testSearchHelper(" favorite\t", 4)
            testSearchHelper("aesthetic", 1)
            testSearchHelper("\tCURL\t", 1)
            testSearchHelper("try", 1)
            testSearchHelper("blues", 1)
            testSearchHelper("MTD", 1)
            testSearchHelper("activity", 3)
            testSearchHelper("fun", 1)
            testSearchHelper(" am", 0)
            testSearchHelper("es ", 0)
            testSearchHelper("OUTSIDE", 1)
            testSearchHelper("2ND", 1)
            testSearchHelper("", PLACES_COUNT)
            testSearchHelper(" ", PLACES_COUNT)
            testSearchHelper("\t\n ", PLACES_COUNT)
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
        }

        @Before
        fun resetServer() {
            Server.reset()
        }

        // After each test make sure the client connected successfully
        @After
        fun checkClient() {
            assertWithMessage("Client should be connected").that(Client.connected).isTrue()
        }

        @Graded(points = 30, friendlyName = "Test Places Search Bar")
        @Test(timeout = 30000L)
        fun test2_PlacesSearchBar() {
            startActivity().onActivity {
                // Check that the right number of restaurants is shown initially
                onView(withId(R.id.map)).check(countMarkersOverlay(PLACES_COUNT))

                // Perform a search that returns no results
                onView(withId(R.id.search)).perform(searchFor("abcdefgh"))
                // Pauses are required here to let the UI catch up
                pause()
                // All results should still be shown
                onView(withId(R.id.map)).check(countMarkersOverlay(PLACES_COUNT))
                // Make sure that clearing the search causes the full list to be displayed again
                onView(withId(R.id.search)).perform(searchFor(""))
                pause()
                onView(withId(R.id.map)).check(countMarkersOverlay(PLACES_COUNT))

                // Perform a search that returns some results
                onView(withId(R.id.search)).perform(searchFor("  thai "))
                pause()
                onView(withId(R.id.map)).check(countMarkersOverlay(3))
                onView(withId(R.id.search)).perform(searchFor(""))
                pause()
                onView(withId(R.id.map)).check(countMarkersOverlay(PLACES_COUNT))

                // Perform a search that returns one result
                onView(withId(R.id.search)).perform(searchFor("oUtside"))
                pause()
                onView(withId(R.id.map)).check(countMarkersOverlay(1))
                onView(withId(R.id.search)).perform(searchFor(""))
                pause()
                onView(withId(R.id.map)).check(countMarkersOverlay(PLACES_COUNT))

                // Perform a search that returns some more results
                onView(withId(R.id.search)).perform(searchFor(" some "))
                pause()
                onView(withId(R.id.map)).check(countMarkersOverlay(5))
                onView(withId(R.id.search)).perform(searchFor(""))
                pause()
                onView(withId(R.id.map)).check(countMarkersOverlay(PLACES_COUNT))
            }
        }
    }
}
// DO NOT REMOVE THIS LINE
// md5: f2eeb9ba50e1032653a856ac4c836b53
