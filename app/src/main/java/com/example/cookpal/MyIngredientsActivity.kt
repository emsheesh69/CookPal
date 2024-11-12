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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val ingredientsCollection = "user_ingredients"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_ingredients)

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            val loginIntent = Intent(this, Login::class.java)
            startActivity(loginIntent)
            finish()
            return
        }

        sharedPreferences = getSharedPreferences(prefsFileName, Context.MODE_PRIVATE)
        initViews()
        loadIngredients()
    }

    private fun initViews() {
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
                saveIngredients()
            } else {
                Toast.makeText(this, "Please enter an ingredient", Toast.LENGTH_SHORT).show()
            }
        }

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
            setHighlightedTab(navVoiceCommand)
            startActivity(Intent(this, VoiceCommandActivity::class.java))
        }
        navSettings.setOnClickListener {
            // Add intent for settings activity if needed
        }

        setHighlightedTab(navIngredients)
    }

    private fun loadIngredients() {
        currentUser?.uid?.let { userId ->
            firestore.collection(ingredientsCollection).document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val savedIngredients = document.get("ingredientsList") as? List<String>
                        ingredientsList.clear()
                        savedIngredients?.let { ingredientsList.addAll(it) }
                        ingredientsAdapter.notifyDataSetChanged()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load ingredients: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveIngredients() {
        currentUser?.uid?.let { userId ->
            val ingredientsData = hashMapOf("ingredientsList" to ingredientsList)
            firestore.collection(ingredientsCollection).document(userId)
                .set(ingredientsData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Ingredients saved successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save ingredients: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onResume() {
        super.onResume()
        loadIngredients()
    }

    private fun setHighlightedTab(selectedTab: LinearLayout) {
        resetAllTabs()
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
        buttonAddIngredient.text = "Update"
        buttonAddIngredient.setOnClickListener {
            val updatedIngredient = editTextIngredient.text.toString()
            if (updatedIngredient.isNotEmpty()) {
                ingredientsList[position] = updatedIngredient
                ingredientsAdapter.notifyItemChanged(position)
                editTextIngredient.text.clear()
                buttonAddIngredient.text = "Add"
                buttonAddIngredient.setOnClickListener {
                    val ingredient = editTextIngredient.text.toString()
                    if (ingredient.isNotEmpty()) {
                        ingredientsList.add(ingredient)
                        ingredientsAdapter.notifyDataSetChanged()
                        editTextIngredient.text.clear()
                        saveIngredients()
                    } else {
                        Toast.makeText(this, "Please enter an ingredient", Toast.LENGTH_SHORT).show()
                    }
                }
                saveIngredients()
            } else {
                Toast.makeText(this, "Please enter an ingredient", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteIngredient(position: Int) {
        ingredientsList.removeAt(position)
        ingredientsAdapter.notifyItemRemoved(position)
        saveIngredients()
    }
}
