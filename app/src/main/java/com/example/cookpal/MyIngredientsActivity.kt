package com.example.cookpal

import MyIngredientsAdapter
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cookpal.MainActivity
import com.example.cookpal.R

class MyIngredientsActivity : AppCompatActivity() {

    private lateinit var editTextIngredient: EditText
    private lateinit var buttonAddIngredient: Button
    private lateinit var recyclerIngredients: RecyclerView
    private lateinit var ingredientsAdapter: MyIngredientsAdapter
    private var ingredientsList: MutableList<String> = mutableListOf()

    private lateinit var navDiscover: LinearLayout
    private lateinit var navIngredients: LinearLayout
    private lateinit var navVoiceCommand: LinearLayout
    private lateinit var navSettings: LinearLayout

    private lateinit var sharedPreferences: SharedPreferences
    private val prefsFileName = "MyIngredientsPrefs"
    private val ingredientsKey = "ingredientsList"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_ingredients)

        // Initialize Shared Preferences
        sharedPreferences = getSharedPreferences(prefsFileName, Context.MODE_PRIVATE)
        loadIngredients() // Load ingredients from Shared Preferences

        editTextIngredient = findViewById(R.id.editTextIngredient)
        buttonAddIngredient = findViewById(R.id.buttonAddIngredient)
        recyclerIngredients = findViewById(R.id.recyclerIngredients)

        ingredientsAdapter = MyIngredientsAdapter(ingredientsList, ::editIngredient, ::deleteIngredient)
        recyclerIngredients.layoutManager = LinearLayoutManager(this)
        recyclerIngredients.adapter = ingredientsAdapter

        buttonAddIngredient.setOnClickListener {
            val ingredient = editTextIngredient.text.toString()
            if (ingredient.isNotEmpty()) {
                ingredientsList.add(ingredient)
                ingredientsAdapter.notifyDataSetChanged()
                editTextIngredient.text.clear()
                saveIngredients() // Save updated ingredients to Shared Preferences
            } else {
                Toast.makeText(this, "Please enter an ingredient", Toast.LENGTH_SHORT).show()
            }
        }

        // Initialize Navigation Bar Views
        navDiscover = findViewById(R.id.nav_discover)
        navIngredients = findViewById(R.id.nav_ingredients)
        navVoiceCommand = findViewById(R.id.nav_voice_command)
        navSettings = findViewById(R.id.nav_settings)

        navDiscover.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        navIngredients.setOnClickListener { setHighlightedTab(navIngredients) }
        navVoiceCommand.setOnClickListener {
            // Intent for voice command activity can be added here
        }
        navSettings.setOnClickListener {
            // Intent for settings activity can be added here
        }

        // Highlight My Ingredients tab
        setHighlightedTab(navIngredients)
    }

    private fun loadIngredients() {
        val savedIngredients = sharedPreferences.getStringSet(ingredientsKey, emptySet())
        ingredientsList.clear()
        savedIngredients?.let {
            ingredientsList.addAll(it)
        }
    }

    private fun saveIngredients() {
        val editor = sharedPreferences.edit()
        editor.putStringSet(ingredientsKey, ingredientsList.toSet())
        editor.apply()
    }

    private fun setHighlightedTab(selectedTab: LinearLayout) {
        resetAllTabs()

        // Highlight the selected tab
        val icon = selectedTab.getChildAt(0) as ImageView
        val text = selectedTab.getChildAt(1) as TextView

        icon.setColorFilter(ContextCompat.getColor(this, R.color.highlight_color))
        text.setTextColor(ContextCompat.getColor(this, R.color.highlight_color))
    }

    private fun resetAllTabs() {
        val tabs = listOf(navDiscover, navIngredients, navVoiceCommand, navSettings)

        for (tab in tabs) {
            val icon = tab.getChildAt(0) as ImageView
            val text = tab.getChildAt(1) as TextView

            icon.setColorFilter(ContextCompat.getColor(this, R.color.white))
            text.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
    }

    private fun editIngredient(position: Int) {
        val currentIngredient = ingredientsList[position]
        editTextIngredient.setText(currentIngredient)
        buttonAddIngredient.setOnClickListener {
            val updatedIngredient = editTextIngredient.text.toString()
            if (updatedIngredient.isNotEmpty()) {
                ingredientsList[position] = updatedIngredient
                ingredientsAdapter.notifyItemChanged(position)
                editTextIngredient.text.clear()
                buttonAddIngredient.setText("Add")
                saveIngredients() // Save updated ingredients
            } else {
                Toast.makeText(this, "Please enter an ingredient", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteIngredient(position: Int) {
        ingredientsList.removeAt(position)
        ingredientsAdapter.notifyItemRemoved(position)
        saveIngredients() // Save updated ingredients after deletion
    }
}
