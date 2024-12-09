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
    private var originalInstructions: ArrayList<String> = ArrayList()
    private val ingredientSubstitutes = HashMap<String, String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_details)

        textViewMealName = findViewById(R.id.meal_name)
        textViewMealSource = findViewById(R.id.meal_source)
        textViewMealSummary = findViewById(R.id.meal_summary)
        imageViewMealImage = findViewById(R.id.meal_image)
        recyclerMealIngredients = findViewById(R.id.meal_ingredients)
        textViewMealInstructions = findViewById(R.id.meal_instructions)
        startCookingButton = findViewById(R.id.start_cooking_button)
        btnFavorite = findViewById(R.id.btn_favorite)


        id = intent.getStringExtra("id")?.toIntOrNull() ?: 0

        val isAIRecipe = intent.getBooleanExtra("isAIRecipe", false)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        toolbar.setNavigationOnClickListener {
            finish()
        }

        manager = RequestManager(this)
        loadRecipeDetails()
        dialog = ProgressDialog(this).apply {
            setMessage("Loading details...")
            show()
        }

        if (isAIRecipe) {
            loadAIRecipeDetails()
        } else {
            fetchRecipeDetails(id)
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

        val items = substitutes.toMutableList()
        items.add("Revert to original")
        val selectedIndex = items.indexOf(ingredientSubstitutes[ingredientName])

        dialogBuilder.setSingleChoiceItems(items.toTypedArray(), selectedIndex) { dialog, which ->
            val selectedSubstitute = items[which]
            if (selectedSubstitute == "Revert to original") {
                revertToOriginalIngredient(ingredientName)
            } else {
                swapIngredientInUI(ingredientName, selectedSubstitute)
                updateInstructionsWithSubstitute(ingredientName, selectedSubstitute)
                ingredientSubstitutes[ingredientName] = selectedSubstitute
            }
            dialog.dismiss()
        }

        dialogBuilder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        dialogBuilder.create().show()
    }

    private fun revertToOriginalIngredient(ingredientName: String) {
        val adapter = recyclerMealIngredients.adapter as? IngredientsAdapter
        if (adapter == null) return

        val updatedIngredients = mutableListOf<ExtendedIngredient>()
        var ingredientUpdated = false

        for (ingredient in adapter.ingredients) {
            if (ingredient.name.equals(ingredientName, ignoreCase = true)) {
                Log.d("RevertIngredient", "Reverting: ${ingredient.name} -> ${ingredient.original}")

                val revertedIngredient = ExtendedIngredient().apply {
                    name = ingredient.original
                    original = ingredient.original
                    id = ingredient.id
                }
                updatedIngredients.add(revertedIngredient)
                ingredientUpdated = true
            } else {
                updatedIngredients.add(ingredient)
            }
        }

        if (ingredientUpdated) {
            adapter.updateIngredients(updatedIngredients)
            Toast.makeText(this, "$ingredientName reverted to original", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No changes to revert", Toast.LENGTH_SHORT).show()
        }
    }

    private fun swapIngredientInUI(originalIngredient: String, substitute: String) {
        val adapter = recyclerMealIngredients.adapter as? IngredientsAdapter ?: return
        val currentIngredients = adapter.getIngredients()
        val updatedIngredients = currentIngredients.map { ingredient ->
            if (ingredient.name.equals(originalIngredient, ignoreCase = true)) {
                ExtendedIngredient().apply {
                    name = substitute
                    original = originalIngredient
                    id = ingredient.id
                }
            } else {
                ingredient
            }
        }
        if (updatedIngredients != currentIngredients) {
            adapter.updateIngredients(updatedIngredients)
            Toast.makeText(this, "$originalIngredient replaced with $substitute", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No ingredients replaced", Toast.LENGTH_SHORT).show()
        }
    }
    private fun updateInstructionsWithSubstitute(originalIngredient: String, substitute: String) {
        if (originalInstructions.isEmpty()) {
            originalInstructions.addAll(instructions)
        }
        val updatedInstructions = originalInstructions.map { instruction ->
            instruction.replace("\\b$originalIngredient\\b".toRegex(RegexOption.IGNORE_CASE), substitute)
        }
        instructions = ArrayList(updatedInstructions)
        textViewMealInstructions.text = instructions.joinToString("\n\n")
        val ingredientsAdapter = recyclerMealIngredients.adapter as? IngredientsAdapter
        ingredientsAdapter?.updateIngredient(originalIngredient, substitute)
    }
    private fun loadAIRecipeDetails() {

        val title = intent.getStringExtra("title") ?: "No title available"
        val summary = intent.getStringExtra("summary") ?: "No summary available"
        val image = intent.getStringExtra("image")
        val ingredients = intent.getStringArrayListExtra("ingredients") ?: arrayListOf()
        this.instructions = intent.getStringArrayListExtra("instructions") ?: arrayListOf()
        textViewMealName.text = title
        textViewMealSource.text = "Generated by AI"
        textViewMealSummary.text = Html.fromHtml(summary, Html.FROM_HTML_MODE_LEGACY)
        if (!image.isNullOrBlank()) {
            Picasso.get().load(image).into(imageViewMealImage)
            imageViewMealImage.tag = image
        } else {
            imageViewMealImage.setImageResource(R.drawable.robo_chef)
            imageViewMealImage.tag = null
        }

        if (instructions.isNotEmpty()) {
            textViewMealInstructions.text = instructions.joinToString("\n\n")
        } else {
            textViewMealInstructions.text = "Instructions not available"
        }

        id = (intent.getStringExtra("id")?.toIntOrNull() ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt())

        val ingredientObjects = ingredients.map { ingredient ->
            ExtendedIngredient().apply {
                this.name = parseIngredientName(ingredient)
                this.original = ingredient
                this.id = 0
            }
        }

        if (ingredientObjects.isEmpty()) {
        }

        recyclerMealIngredients.setHasFixedSize(true)
        recyclerMealIngredients.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerMealIngredients.adapter = IngredientsAdapter(this, ingredientObjects) { ingredientName ->
            ingredientName?.let {
                getSubstituteForIngredient(it)
            }
        }

        if (::dialog.isInitialized && dialog.isShowing) {
            dialog.dismiss()
        }
    }

    private fun loadRecipeDetails() {
        val isAIRecipe = intent.getBooleanExtra("isAIRecipe", false)
        val recipeId = intent.getStringExtra("id") ?: return

        if (isAIRecipe) {
            loadAIRecipeDetails()
        } else {
            fetchRecipeDetails(recipeId.toInt())
        }
    }
    private fun parseIngredientName(fullIngredient: String): String {
        val regex = Regex("""\b(\d+[^a-zA-Z]*\s)?(cup[s]?|tablespoon[s]?|teaspoon[s]?|gram[s]?|ounce[s]?|kg|lb|liter[s]?|ml|slice[s]?|pinch[es]?|dash|whole|clove[s]?|handful)\b""")
        val cleaned = fullIngredient.replace(regex, "").trim()

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
//                Toast.makeText(this@RecipeDetails, message, Toast.LENGTH_SHORT).show()
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

        val ingredients = intent.getStringArrayListExtra("ingredients") ?: arrayListOf<String>()
        val instructions = intent.getStringArrayListExtra("instructions") ?: arrayListOf<String>()

        val favorite = mutableMapOf<String, Any>(
            "id" to id,
            "name" to textViewMealName.text.toString(),
            "image" to (imageViewMealImage.tag as? String ?: ""),
            "date" to currentDate,
            "source" to if (isAIRecipe) "AI" else "Spoonacular",
            "isAIRecipe" to isAIRecipe,
            "ingredients" to ingredients,
            "instructions" to instructions,
            "summary" to textViewMealSummary.text.toString()
        )

        databaseRef.child(id.toString()).setValue(favorite).addOnSuccessListener {
            Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show()
            isFavorite = true
            updateFavoriteButton()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to add to Favorites", Toast.LENGTH_SHORT).show()
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
