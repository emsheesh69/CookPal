package com.example.cookpal

import android.app.ProgressDialog
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.text.Html
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cookpal.Adapters.IngredientsAdapter
import com.example.cookpal.Models.RecipeDetailsResponse
import com.example.cookpal.listeners.RecipeDetailsListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import java.util.Date
import java.util.Locale

class RecipeDetails : AppCompatActivity() {

    private var id: Int = 0
    private var isFavorite = false
    private lateinit var btnFavorite: ImageButton
    private lateinit var textViewMealName: TextView
    private lateinit var textViewMealSource: TextView
    private lateinit var textViewMealSummary: TextView
    private lateinit var imageViewMealImage: ImageView
    private lateinit var recyclerMealIngredients: RecyclerView
    private lateinit var manager: RequestManager
    private lateinit var dialog: ProgressDialog
    private lateinit var textViewMealInstructions: TextView
    private lateinit var startCookingButton: Button

    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private var instructions: ArrayList<String> = ArrayList()  // Store instructions here

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_details)

        textViewMealName = findViewById(R.id.meal_name)
        textViewMealSource = findViewById(R.id.meal_source)
        textViewMealSummary = findViewById(R.id.meal_summary)
        imageViewMealImage = findViewById(R.id.meal_image)
        recyclerMealIngredients = findViewById(R.id.meal_ingredients)
        textViewMealInstructions = findViewById(R.id.meal_instructions)
        startCookingButton = findViewById(R.id.start_cooking_button)  // Initialize button
        btnFavorite = findViewById(R.id.btn_favorite) // Adjust the ID according to your layout


        id = intent.getStringExtra("id")?.toIntOrNull() ?: 0

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        manager = RequestManager(this)

        dialog = ProgressDialog(this).apply {
            setMessage("Loading details...")
            show()
        }

        fetchRecipeDetails(id)
        checkFavoriteStatus()

        // Set up button click listener
        startCookingButton.setOnClickListener {
            startCooking()
        }

        btnFavorite.setOnClickListener {
            if (isFavorite) removeFromFavorites() else addToFavorites()
        }

        setupNavigationBar()
    }

    private fun fetchRecipeDetails(recipeId: Int) {
        manager.getRecipeDetails(recipeDetailsListener, recipeId)
    }

    private val recipeDetailsListener = object : RecipeDetailsListener {
        override fun didFetch(response: RecipeDetailsResponse, message: String) {
            dialog.dismiss()

            textViewMealName.text = response.title ?: "No title available"
            textViewMealSource.text = response.sourceName ?: "No source available"

            val fullSummary = response.summary
            val summarySentences = fullSummary?.split(". ")?.take(2)?.joinToString(". ") + "."
            textViewMealSummary.text = Html.fromHtml(summarySentences, Html.FROM_HTML_MODE_LEGACY)

           Picasso.get().load(response.image).into(imageViewMealImage)
            imageViewMealImage.tag = response.image

            if (!response.analyzedInstructions.isNullOrEmpty()) {
                val steps = response.analyzedInstructions[0].steps
                instructions = ArrayList(steps.map { it.step ?: "No instruction available" })

                textViewMealInstructions.text = instructions.joinToString("\n\n")
            } else {
                textViewMealInstructions.text = "Instructions not available"
            }

            recyclerMealIngredients.setHasFixedSize(true)
            recyclerMealIngredients.layoutManager = LinearLayoutManager(this@RecipeDetails, LinearLayoutManager.VERTICAL, false)
            val ingredientsAdapter = IngredientsAdapter(this@RecipeDetails, response.extendedIngredients)
            recyclerMealIngredients.adapter = ingredientsAdapter
        }

        override fun didError(message: String) {
            dialog.dismiss()
            Toast.makeText(this@RecipeDetails, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkFavoriteStatus() {
        val databaseRef = FirebaseDatabase.getInstance().getReference("users/$userId/Favorites")
        databaseRef.child(id.toString()).get().addOnSuccessListener {
            isFavorite = it.exists()
            updateFavoriteButton()
        }
    }

    private fun updateFavoriteButton() {
        val icon = if (isFavorite) R.drawable.ic_fav_tick else R.drawable.ic_fav_untick
        btnFavorite.setImageResource(icon)
    }

    private fun addToFavorites() {
        val databaseRef = FirebaseDatabase.getInstance().getReference("users/$userId/Favorites")
        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val favorite = mapOf(
            "id" to id,
            "name" to textViewMealName.text.toString(),
            "image" to (imageViewMealImage.tag as? String ?: ""),
            "date" to currentDate
        )

        databaseRef.child(id.toString()).setValue(favorite).addOnSuccessListener {
            Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show()
            isFavorite = true
            updateFavoriteButton()
        }
    }

    private fun removeFromFavorites() {
        val databaseRef = FirebaseDatabase.getInstance().getReference("users/$userId/Favorites")
        databaseRef.child(id.toString()).removeValue().addOnSuccessListener {
            Toast.makeText(this, "Removed from Favorites", Toast.LENGTH_SHORT).show()
            isFavorite = false
            updateFavoriteButton()
        }
    }

    private fun startCooking() {
        if (instructions.isNotEmpty()) {
            val intent = Intent(this@RecipeDetails, CookingActivity::class.java)
            intent.putExtra("id", id) // Pass the recipe ID
            intent.putExtra("name", textViewMealName.text.toString()) // Pass the recipe name
            intent.putExtra("image", imageViewMealImage.tag as? String ?: "") // Pass the image URL
            intent.putStringArrayListExtra("instructions", instructions)  // Pass instructions to the next activity
            startActivity(intent)
        } else {
            Toast.makeText(this, "Instructions not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNavigationBar() {
        findViewById<LinearLayout>(R.id.nav_discover).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.nav_ingredients).setOnClickListener {
            startActivity(Intent(this, MyIngredientsActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.nav_voice_command).setOnClickListener {
            startActivity(Intent(this, VoiceCommandActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.nav_settings).setOnClickListener {
             startActivity(Intent(this, UserPreference::class.java))
        }
    }
}
