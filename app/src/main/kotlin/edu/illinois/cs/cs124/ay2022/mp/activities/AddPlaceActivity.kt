package edu.illinois.cs.cs124.ay2022.mp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import edu.illinois.cs.cs124.ay2022.mp.R
import edu.illinois.cs.cs124.ay2022.mp.application.FavoritePlacesApplication
import edu.illinois.cs.cs124.ay2022.mp.models.Place
import edu.illinois.cs.cs124.ay2022.mp.models.ResultMightThrow
import edu.illinois.cs.cs124.ay2022.mp.network.Client
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

private val TAG = AddPlaceActivity::class.java.name

class AddPlaceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_addplace)

        val returnToMain = Intent(this, MainActivity::class.java)

        returnToMain.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK + Intent.FLAG_ACTIVITY_NEW_TASK


        val cancelButton = findViewById<Button>(R.id.cancel_button)
        cancelButton.setOnClickListener {
            startActivity(returnToMain)
        }

        val saveButton = findViewById<Button>(R.id.save_button)
        saveButton.setOnClickListener {
            val latt = intent.getStringExtra("latitude")
            require(latt != null)
            val longg = intent.getStringExtra("longitude")
            require(longg != null)
            val idd =  FavoritePlacesApplication.CLIENT_ID
            val namee =  "Mickey Mouse"
            val descc = findViewById<EditText>(R.id.description).getText().toString()
            val placee = Place(idd, namee, latt.toDouble(), longg.toDouble(), descc)

            val completable: CompletableFuture<ResultMightThrow<Boolean>> = CompletableFuture()

            Client.postFavoritePlace(placee, completable::complete)


            startActivity(returnToMain)
        }
    }
}
