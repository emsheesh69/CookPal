package com.example.cookpal

import android.app.ProgressDialog
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_details)

        textViewMealName = findViewById(R.id.meal_name)
        textViewMealSource = findViewById(R.id.meal_source)
        textViewMealSummary = findViewById(R.id.meal_summary)
        imageViewMealImage = findViewById(R.id.meal_image)
        recyclerMealIngredients = findViewById(R.id.meal_ingredients)
        id = intent.getStringExtra("id")?.toIntOrNull() ?: 0

        // Initialize RequestManager before calling fetchRecipeDetails
        manager = RequestManager(this)

        dialog = ProgressDialog(this).apply {
            setMessage("Loading details...")
            show()
        }

        fetchRecipeDetails(id)  // Call fetch after initializing manager
    }

    private fun fetchRecipeDetails(recipeId: Int) {
        manager.getRecipeDetails(recipeDetailsListener, recipeId)
    }

    private val recipeDetailsListener = object : RecipeDetailsListener {
        override fun didFetch(response: RecipeDetailsResponse, message: String) {
            dialog.dismiss()

            textViewMealName.text = response.title
            textViewMealSource.text = response.sourceName
            textViewMealSummary.text = response.summary

            Picasso.get()
                .load(response.image)
                .into(imageViewMealImage)

            recyclerMealIngredients.setHasFixedSize(true)
            recyclerMealIngredients.layoutManager =
                LinearLayoutManager(this@RecipeDetails, LinearLayoutManager.HORIZONTAL, false)

            val ingredientsAdapter =
                IngredientsAdapter(this@RecipeDetails, response.extendedIngredients)
            recyclerMealIngredients.adapter = ingredientsAdapter
        }

        override fun didError(message: String) {
            // Implement your error handling logic here
            dialog.dismiss()
            Toast.makeText(this@RecipeDetails, message, Toast.LENGTH_SHORT).show()
        }
    }
}
