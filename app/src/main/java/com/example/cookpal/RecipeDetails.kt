package com.example.cookpal

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RecipeDetails : AppCompatActivity() {

    private var id: Int = 0




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_details)

        id = intent.getStringExtra("id")?.toIntOrNull() ?: 0

        // Call the API with the captured id
        fetchRecipeDetails(id)

        }
    }
