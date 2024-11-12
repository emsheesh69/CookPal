package com.example.cookpal

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.Html
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
import com.squareup.picasso.Picasso

class RecipeDetails : AppCompatActivity() {

    private var id: Int = 0
    private lateinit var textViewMealName: TextView
    private lateinit var textViewMealSource: TextView
    private lateinit var textViewMealSummary: TextView
    private lateinit var imageViewMealImage: ImageView
    private lateinit var recyclerMealIngredients: RecyclerView
    private lateinit var manager: RequestManager
    private lateinit var dialog: ProgressDialog
    private lateinit var textViewMealInstructions: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_details)

        textViewMealName = findViewById(R.id.meal_name)
        textViewMealSource = findViewById(R.id.meal_source)
        textViewMealSummary = findViewById(R.id.meal_summary)
        imageViewMealImage = findViewById(R.id.meal_image)
        recyclerMealIngredients = findViewById(R.id.meal_ingredients)
        textViewMealInstructions = findViewById(R.id.meal_instructions)
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

        setupNavigationBar()
    }

    private fun fetchRecipeDetails(recipeId: Int) {
        manager.getRecipeDetails(recipeDetailsListener, recipeId)
    }

    private val recipeDetailsListener = object : RecipeDetailsListener {
        override fun didFetch(response: RecipeDetailsResponse, message: String) {
            dialog.dismiss()

            textViewMealName.text = response.title
            textViewMealSource.text = response.sourceName

            val fullSummary = response.summary
            val summarySentences = fullSummary.split(". ").take(2).joinToString(". ") + "."
            textViewMealSummary.text = Html.fromHtml(summarySentences, Html.FROM_HTML_MODE_LEGACY)

            Picasso.get()
                .load(response.image)
                .into(imageViewMealImage)

            if (!response.analyzedInstructions.isNullOrEmpty()) {
                val steps = response.analyzedInstructions[0].steps
                val instructions = steps.joinToString("\n") {
                    "${it.number}. ${it.step}"
                }
                textViewMealInstructions.text = instructions
            } else {
                textViewMealInstructions.text = "Instructions not available"
            }

            recyclerMealIngredients.setHasFixedSize(true)
            recyclerMealIngredients.layoutManager =
                LinearLayoutManager(this@RecipeDetails, LinearLayoutManager.HORIZONTAL, false)

            val ingredientsAdapter =
                IngredientsAdapter(this@RecipeDetails, response.extendedIngredients)
            recyclerMealIngredients.adapter = ingredientsAdapter
        }

        override fun didError(message: String) {
            dialog.dismiss()
            Toast.makeText(this@RecipeDetails, message, Toast.LENGTH_SHORT).show()
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
            // Intent to Voice Command
            startActivity(Intent(this, VoiceCommandActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.nav_settings).setOnClickListener {
            // Intent to Settings
            // startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
