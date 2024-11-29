package com.example.cookpal

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.text.Html
import android.util.Log
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
import com.example.cookpal.Models.ExtendedIngredient
import com.example.cookpal.Listeners.IngredientSubstituteListener
import com.example.cookpal.Models.IngredientSubstitution
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

        // Check if the recipe is AI-generated
        val isAIRecipe = intent.getBooleanExtra("isAIRecipe", false)

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

        // Decide the data source based on the flag
        if (isAIRecipe) {
            loadAIRecipeDetails() // Load AI-generated recipe
        } else {
            fetchRecipeDetails(id) // Fetch Spoonacular recipe
        }

        if (intent.getStringExtra("type") == "Spoonacular") {
            fetchRecipeDetails(id)
        } else {
            loadAIRecipeDetails()
        }

        startCookingButton.setOnClickListener {
            startCooking()
        }

        checkFavoriteStatus()

        btnFavorite.setOnClickListener {
            if (isFavorite) removeFromFavorites() else addToFavorites()
        }

        setupNavigationBar()
    }

    private fun fetchRecipeDetails(recipeId: Int) {
        manager.getRecipeDetails(recipeDetailsListener, recipeId)
    }

    private fun getSubstituteForIngredient(ingredientName: String?) {
        ingredientName?.takeIf { it.isNotBlank() }?.let { name ->
            manager.getIngredientSubstitute(name, object : IngredientSubstituteListener {
                override fun didFetch(response: IngredientSubstitution, message: String) {
                    runOnUiThread {
                        val substitutes = response.substitutes ?: listOf("No substitutes available.")
                        showSubstituteDialog(name, substitutes)
                    }
                }

                override fun didError(message: String) {
                    runOnUiThread {
                        showSubstituteDialog(name, listOf("Error fetching substitutes: $message"))
                    }
                }
            })
        } ?: Toast.makeText(this, "Ingredient name not provided", Toast.LENGTH_SHORT).show()
    }

    private fun showSubstituteDialog(ingredientName: String, substitutes: List<String>) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Substitutes for $ingredientName")

        val substituteText = substitutes.joinToString("\n")
        dialogBuilder.setMessage(substituteText)

        dialogBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }

        dialogBuilder.create().show()
    }

    private fun loadAIRecipeDetails() {
        Log.d("RecipeDetails", "Loading AI Recipe Details...")

        // Retrieve AI recipe details from the intent
        val title = intent.getStringExtra("title") ?: "No title available"
        val summary = intent.getStringExtra("summary") ?: "No summary available"
        val image = intent.getStringExtra("image")
        val ingredients = intent.getStringArrayListExtra("ingredients") ?: arrayListOf()
        val instructions = intent.getStringArrayListExtra("instructions") ?: arrayListOf()
//        this.instructions = intent.getStringArrayListExtra("instructions") ?: arrayListOf()


        // Debugging logs
        Log.d("RecipeDetails", "AI Recipe Details - Title: $title")
        Log.d("RecipeDetails", "AI Recipe Details - Summary: $summary")
        Log.d("RecipeDetails", "AI Recipe Details - Image: ${image ?: "No image provided"}")
        Log.d("RecipeDetails", "AI Recipe Details - Ingredients: $ingredients")
        Log.d("RecipeDetails", "AI Recipe Details - Instructions: $instructions")

        // Log instructions to check
        Log.d("RecipeDetails", "AI Recipe Instructions: $instructions")

        // Populate the UI with AI recipe details
        textViewMealName.text = title
        textViewMealSource.text = "Generated by AI" // AI recipe label
        textViewMealSummary.text = Html.fromHtml(summary, Html.FROM_HTML_MODE_LEGACY)

        if (!image.isNullOrBlank()) {
            Picasso.get().load(image).into(imageViewMealImage)
            imageViewMealImage.tag = image
        } else {
            // Use a placeholder image for AI recipes without an image
            imageViewMealImage.setImageResource(R.drawable.cookpal)
            imageViewMealImage.tag = null
        }

        // Ensure instructions are displayed correctly
        if (instructions.isNotEmpty()) {
            textViewMealInstructions.text = instructions.joinToString("\n\n")
        } else {
            textViewMealInstructions.text = "Instructions not available"
        }

        // Set the recipe ID for the AI-generated recipe
        id = (intent.getStringExtra("id")?.toIntOrNull() ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt())

        // Map ingredients to ExtendedIngredient objects
        val ingredientObjects = ingredients.map { ingredient ->
            ExtendedIngredient().apply {
                this.name = parseIngredientName(ingredient) // Extract short name
                this.original = ingredient // Full AI-provided description
                this.id = 0 // Optional ID for consistency
            }
        }

        if (ingredientObjects.isEmpty()) {
            Log.w("RecipeDetails", "No ingredients provided for the AI recipe.")
        }

        recyclerMealIngredients.setHasFixedSize(true)
        recyclerMealIngredients.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerMealIngredients.adapter = IngredientsAdapter(this, ingredientObjects) { ingredientName ->
            ingredientName?.let {
                getSubstituteForIngredient(it)
            } ?: Log.e("RecipeDetails", "Ingredient name is null.")
        }

        // Ensure dialog dismissal happens safely
        if (::dialog.isInitialized && dialog.isShowing) {
            dialog.dismiss()
        } else {
            Log.w("RecipeDetails", "Dialog was not initialized or already dismissed.")
        }

        Log.d("RecipeDetails", "AI Recipe Details successfully loaded.")
    }

    private fun parseIngredientName(fullIngredient: String): String {
        // Remove quantities and preparation details using a regex
        val regex = Regex("""\b(\d+[^a-zA-Z]*\s)?(cup[s]?|tablespoon[s]?|teaspoon[s]?|gram[s]?|ounce[s]?|kg|lb|liter[s]?|ml|slice[s]?|pinch[es]?|dash|whole|clove[s]?|handful)\b""")
        val cleaned = fullIngredient.replace(regex, "").trim()

        // Extract the last significant word(s) as the ingredient name
        val words = cleaned.split(" ")
        return words.lastOrNull()?.lowercase() ?: cleaned
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
            val ingredientsAdapter = IngredientsAdapter(this@RecipeDetails, response.extendedIngredients) { ingredientName ->
                getSubstituteForIngredient(ingredientName)
            }
            recyclerMealIngredients.adapter = ingredientsAdapter

        }

        override fun didError(message: String) {
            dialog.dismiss()
            if (intent.getStringExtra("type") == "AI") {
                Toast.makeText(this@RecipeDetails, "Failed to load AI Recipe: $message", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@RecipeDetails, message, Toast.LENGTH_SHORT).show()
            }
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

        val isAIRecipe = intent.getBooleanExtra("isAIRecipe", false)

        val favorite = mutableMapOf(
            "id" to id,
            "name" to textViewMealName.text.toString(),
            "image" to (imageViewMealImage.tag as? String ?: ""),
            "date" to currentDate,
            "source" to if (isAIRecipe) "AI" else "Spoonacular",
            "isAIRecipe" to isAIRecipe // Flag indicating AI recipe
        )

        databaseRef.child(id.toString()).setValue(favorite).addOnSuccessListener {
            Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show()
            isFavorite = true
            updateFavoriteButton()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to add to Favorites", Toast.LENGTH_SHORT).show()
            Log.e("RecipeDetails", "Error adding to Favorites: ${it.message}")
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
        Log.d("RecipeDetails", "Instructions in startCooking: $instructions")
        // Ensure that instructions are available and not empty
        if (this.instructions != null && this.instructions.isNotEmpty()) {
            val intent = Intent(this@RecipeDetails, CookingActivity::class.java)
            intent.putExtra("id", id) // Pass the recipe ID
            intent.putExtra("name", textViewMealName.text.toString()) // Pass the recipe name
            intent.putExtra("image", imageViewMealImage.tag as? String ?: "") // Pass the image URL
            intent.putStringArrayListExtra("instructions", this.instructions)  // Pass instructions to the next activity
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
