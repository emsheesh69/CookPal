package com.example.cookpal

import MyIngredientsAdapter
import OpenAIRequest
import OpenAIResponse
import OpenAIService
import RequestMessage
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
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
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response



class MyIngredientsActivity : AppCompatActivity() {

    private lateinit var editTextIngredient: EditText
    private lateinit var buttonAddIngredient: Button
    private lateinit var buttonGetRecipe: Button
    private lateinit var recyclerIngredients: RecyclerView
    private lateinit var ingredientsAdapter: MyIngredientsAdapter
    private var ingredientsList: MutableList<String> = mutableListOf()

    private lateinit var navDiscover: LinearLayout
    private lateinit var navIngredients: LinearLayout
    private lateinit var navVoiceCommand: LinearLayout
    private lateinit var navSettings: LinearLayout

    // Initialize Retrofit and apiService directly as a property
    private val apiService: OpenAIService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAIService::class.java)
    }

//    private lateinit var apiService: OpenAIService

    private lateinit var sharedPreferences: SharedPreferences
    private val prefsFileName = "MyIngredientsPrefs"
    private val ingredientsKey = "ingredientsList"
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val ingredientsCollection = "user_ingredients"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_ingredients)

        // Log the OpenAI API key to check if it's loaded correctly
        Log.d("ChatGPT", "OpenAI API Key: ${BuildConfig.OPENAI_API_KEY}")

        // Initialize Shared Preferences
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
        buttonGetRecipe = findViewById(R.id.buttonGetRecipe)
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

        buttonGetRecipe.setOnClickListener {
            if (ingredientsList.isNotEmpty()) {
                getRecipeSuggestion()
            } else {
                Toast.makeText(this, "Please add ingredients first.", Toast.LENGTH_SHORT).show()
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
            setHighlightedTab(navVoiceCommand)
            startActivity(Intent(this, VoiceCommandActivity::class.java))
        }
        navSettings.setOnClickListener {
            setHighlightedTab(navSettings)
            startActivity(Intent(this, UserPreference::class.java))
            finish()
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



    private fun getRecipeSuggestion() {
        // Log to debug the current state of ingredients
        Log.d("ChatGPT", "Ingredients: ${ingredientsList.joinToString(", ")}")

        // Create a unique key for caching based on the ingredients list
        val ingredientsKey = ingredientsList.joinToString(",").hashCode().toString()

        // Check if the recipe exists in the cache
        val cachedRecipe = getCachedRecipe(ingredientsKey)
        if (cachedRecipe != null) {
            // Use the cached recipe
            Log.d("ChatGPT", "Using cached recipe.")
            showRecipeSuggestion(cachedRecipe)
            return
        }

        // Cache doesn't exist; proceed with the API request
        // Create messages for the ChatGPT API
        val messages = listOf(
            RequestMessage("system", "You are a professional chef providing clear, concise, and structured recipes."),
            RequestMessage("user", "Provide a recipe using these ingredients: ${ingredientsList.joinToString(", ")}. Include a brief description of the dish (two sentences max), followed by the ingredients list, and detailed step-by-step cooking instructions. Strictly avoid conversational language or pleasantries. Focus only on the recipe content.")
        )

        // Construct the OpenAIRequest object
        val openAIRequest = OpenAIRequest(
            model = "gpt-4o-mini-2024-07-18",
            messages = messages,
            max_tokens = 2048,
            temperature = 0.6,
            top_p = 0.9
        )

        val gson = Gson()
        Log.d("ChatGPT", "Request JSON: ${gson.toJson(openAIRequest)}")



        // Send the API request
        apiService.getRecipeSuggestions("Bearer ${BuildConfig.OPENAI_API_KEY}", openAIRequest)
            .enqueue(object : Callback<OpenAIResponse> {
            override fun onResponse(call: Call<OpenAIResponse>, response: Response<OpenAIResponse>) {
                if (response.isSuccessful) {
                    val recipeSuggestion = response.body()?.choices?.firstOrNull()?.message?.content
                    if (recipeSuggestion != null) {
                        // Save the recipe to the cache
                        saveRecipeToCache(ingredientsKey, recipeSuggestion)
                        // Display the recipe suggestion in an AlertDialog
                        showRecipeSuggestion(recipeSuggestion)
                    } else {
                        Toast.makeText(this@MyIngredientsActivity, "No recipe found.", Toast.LENGTH_SHORT).show()
                        Log.e("ChatGPT", "No content in response body.")
                    }
                } else {
                    // Handle errors, such as malformed request
                    Toast.makeText(this@MyIngredientsActivity, "Failed to get recipe suggestion.", Toast.LENGTH_SHORT).show()
                    Log.e("ChatGPT", "Failed to get response. Code: ${response.code()} Message: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<OpenAIResponse>, t: Throwable) {
                Toast.makeText(this@MyIngredientsActivity, "API request failed: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("ChatGPT", "Error: ${t.message}")
            }
        })
    }

    private fun showRecipeSuggestion(recipe: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Recipe Suggestion")
            .setMessage(recipe)
            .setPositiveButton("OK", null)
            .create()
        dialog.show()
    }

    private fun saveRecipeToCache(key: String, recipe: String) {
        val sharedPreferences = getSharedPreferences("RecipeCache", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(key, recipe).apply()
    }

    private fun getCachedRecipe(key: String): String? {
        val sharedPreferences = getSharedPreferences("RecipeCache", Context.MODE_PRIVATE)
        return sharedPreferences.getString(key, null)
    }

}
