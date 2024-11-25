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
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cookpal.Models.AIRecipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
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
    private lateinit var navDiscover: LinearLayout
    private lateinit var navIngredients: LinearLayout
    private lateinit var navVoiceCommand: LinearLayout
    private lateinit var navSettings: LinearLayout
    private lateinit var sharedPreferences: SharedPreferences

    private var ingredientsList: MutableList<String> = mutableListOf()

    private val prefsFileName = "MyIngredientsPrefs"
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val ingredientsCollection = "user_ingredients"

    // Initialize Retrofit and apiService directly as a property
    private val apiService: OpenAIService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAIService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_ingredients)

        // Log the OpenAI API key to check if it's loaded correctly
        Log.d("ChatGPT", "OpenAI API Key: ${BuildConfig.OPENAI_API_KEY}")


        val textClearList: TextView = findViewById(R.id.textClearList)

        // Create a SpannableString to apply underline
        val content = SpannableString(textClearList.text)
        content.setSpan(UnderlineSpan(), 0, content.length, 0)

        // Set the modified text to the TextView
        textClearList.text = content

        textClearList.setOnClickListener {
            if (ingredientsList.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Clear Ingredients")
                    .setMessage("Are you sure you want to clear all ingredients?")
                    .setPositiveButton("Yes") { _, _ ->
                        ingredientsList.clear()
                        ingredientsAdapter.notifyDataSetChanged()
                        saveIngredients()
                        Toast.makeText(this, "Ingredients list cleared.", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("No", null)
                    .show()
            } else {
                Toast.makeText(this, "The ingredients list is already empty.", Toast.LENGTH_SHORT).show()
            }
        }

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
            val ingredient = editTextIngredient.text.toString().trim()
            if (ingredient.isNotEmpty()) {
                if (ingredientsList.contains(ingredient.uppercase())) {
                    showToast("Ingredient already added.")
                } else {
                    ingredientsList.add(ingredient.uppercase())
                    ingredientsAdapter.notifyItemInserted(ingredientsList.size - 1)
                    editTextIngredient.text.clear()
                    saveIngredients()
                }
            } else {
                showToast("Please enter an ingredient.")
            }
        }

        buttonGetRecipe.setOnClickListener {
            if (ingredientsList.isNotEmpty()) {
                getRecipeSuggestion()
            } else {
                showToast("Please add ingredients first.")
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
                    val savedIngredients = document.get("ingredientsList") as? List<String>
                    ingredientsList.clear()
                    savedIngredients?.map { it.uppercase() }?.let { ingredientsList.addAll(it) }
                    ingredientsAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("MyIngredientsActivity", "Failed to load ingredients: ${e.message}", e)
                    showToast("Unable to load ingredients. Please try again later.")
                }
        }
    }

    private fun saveIngredients() {
        currentUser?.uid?.let { userId ->
            val ingredientsData = hashMapOf("ingredientsList" to ingredientsList)
            firestore.collection(ingredientsCollection).document(userId)
                .set(ingredientsData)
                .addOnSuccessListener {
                    Log.d("MyIngredientsActivity", "Ingredients saved successfully.")
                }
                .addOnFailureListener { e ->
                    Log.e("MyIngredientsActivity", "Failed to save ingredients: ${e.message}", e)
                    showToast("Failed to save ingredients. Please check your connection.")
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
        if (position in ingredientsList.indices) {
            val currentIngredient = ingredientsList[position]
            editTextIngredient.setText(currentIngredient)

            buttonAddIngredient.text = "Update"
            buttonAddIngredient.setOnClickListener {
                val updatedIngredient = editTextIngredient.text.toString().trim()
                if (updatedIngredient.isEmpty()) {
                    showToast("Please enter a valid ingredient.")
                } else if (ingredientsList.contains(updatedIngredient.uppercase()) && currentIngredient.uppercase() != updatedIngredient.uppercase()) {
                    showToast("This ingredient already exists.")
                } else {
                    ingredientsList[position] = updatedIngredient.uppercase()
                    ingredientsAdapter.notifyItemChanged(position)
                    resetAddIngredientButton()
                    saveIngredients()
                    // Clear the input field
                    editTextIngredient.setText("")
                    showToast("Ingredient updated successfully.")
                }
            }
        } else {
            Log.e("MyIngredientsActivity", "Invalid ingredient selection for editing at position: $position")
            showToast("Invalid selection.")
        }
    }

    private fun resetAddIngredientButton() {
        buttonAddIngredient.text = "Add"
        buttonAddIngredient.setOnClickListener {
            val ingredient = editTextIngredient.text.toString().trim()
            if (ingredient.isEmpty()) {
                showToast("Please enter an ingredient.")
            } else if (ingredientsList.contains(ingredient.uppercase())) {
                showToast("Ingredient already added.")
            } else {
                ingredientsList.add(ingredient.uppercase())
                ingredientsAdapter.notifyItemInserted(ingredientsList.size - 1)
                editTextIngredient.text.clear()
                saveIngredients()
            }
        }
    }

    private fun deleteIngredient(position: Int) {
        if (position in ingredientsList.indices) { // Safely check position
            val deletedIngredient = ingredientsList.removeAt(position)
            ingredientsAdapter.notifyItemRemoved(position)

            // If the list is now empty, notify the adapter
            if (ingredientsList.isEmpty()) {
                ingredientsAdapter.notifyDataSetChanged()
            }

            // Save updated list to Firebase
            saveIngredients()
            showToast("Ingredient '$deletedIngredient' deleted.")
        } else {
            Log.e("MyIngredientsActivity", "Attempted to delete invalid position: $position")
            showToast("Invalid position. Unable to delete.")
        }
    }



    private fun makeOpenAIRequest(
        messages: List<RequestMessage>,
        model: String = "gpt-4o-mini-2024-07-18",
        maxTokens: Int = 100,
        temperature: Float = 0.7f,
        topP: Float = 0.9f,
        onResponse: (String?) -> Unit
    ) {
        val openAIRequest = OpenAIRequest(
            model = model,
            messages = messages,
            max_tokens = maxTokens,
            temperature = temperature,
            top_p = topP
        )

        Log.d("OpenAIRequest", "Request JSON: ${Gson().toJson(openAIRequest)}")

        apiService.getRecipeSuggestions("Bearer ${BuildConfig.OPENAI_API_KEY}", openAIRequest)
            .enqueue(object : Callback<OpenAIResponse> {
                override fun onResponse(call: Call<OpenAIResponse>, response: Response<OpenAIResponse>) {
                    if (response.isSuccessful) {
                        val result = response.body()?.choices?.firstOrNull()?.message?.content
                        if (result != null) {
                            Log.d("OpenAIResponse", "Response: $result")
                            onResponse(result)
                        } else {
                            Log.e("OpenAIResponse", "Empty response body")
                            onResponse(null)
                        }
                    } else {
                        Log.e("OpenAIResponse", "Failed response. Code: ${response.code()}, Message: ${response.message()}")
                        onResponse(null)
                    }
                }

                override fun onFailure(call: Call<OpenAIResponse>, t: Throwable) {
                    showToast("API request failed: ${t.message}")
                    Log.e("OpenAIResponse", "Error: ${t.message}")
                    onResponse(null)
                }
            })
    }

    private fun preprocessIngredients(ingredients: List<String>): List<String> {
        // Normalize input
        return ingredients
            .map { it.trim().lowercase() }
            .distinct()
            .sorted()
    }

    private fun getRecipeSuggestion() {
        // Log the current state of ingredients
        Log.d("ChatGPT", "Ingredients: ${ingredientsList.joinToString(", ")}")

        // Preprocess ingredients before validation
        val preprocessedIngredients = preprocessIngredients(ingredientsList)
        Log.d("ChatGPT", "Preprocessed Ingredients: ${preprocessedIngredients.joinToString(", ")}")

        // Preprocessing stage: Limit the number of ingredients
        val maxIngredients = 15
        if (ingredientsList.size > maxIngredients) {
            val truncatedIngredients: MutableList<String> = ingredientsList.take(maxIngredients).toMutableList()
            notifyTruncatedIngredients(truncatedIngredients)
            ingredientsList = truncatedIngredients
        }

        // Validate Ingredients
        validateIngredients(preprocessedIngredients) { isValid, refinedList, feedbackMessage, hasStrangeIngredients ->
            if (isValid) {
//                ingredientsList = refinedList.toMutableList()
//                showAlertDialog("Your CookPal's Feedback", feedbackMessage ?: "Ingredients look great!")
//
//                Log.d("ChatGPT", "Validated Ingredients: ${ingredientsList.joinToString(", ")}")
//                // Ingredients are valid; proceed with recipe suggestion
//                Log.d("ChatGPT", "All ingredients are valid. Proceeding with recipe generation.")

                val dialogBuilder = AlertDialog.Builder(this)
                    .setTitle("Your CookPal's Feedback")
                    .setMessage(feedbackMessage)

                if (hasStrangeIngredients) {
                    dialogBuilder.setNeutralButton("Creative Recipe with All Ingredients") { _, _ ->
                        generateRecipe(preprocessedIngredients) // Full list, including strange ingredients
                    }
                }

                dialogBuilder
                    .setPositiveButton("Proceed with Refined Ingredients") { _, _ ->
                        generateRecipe(refinedList) // Refined list only
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()

            } else {
                // Handle invalid ingredients
                Log.e("ChatGPT", "errorMessageInvalidIngredients: $feedbackMessage")
                showAlertDialog("Oops! That’s Not a Snack!", feedbackMessage ?: "Please correct your ingredients.")
            }
        }
    }

    private fun validateIngredients(ingredients: List<String>, callback: (Boolean, List<String>, String?, Boolean) -> Unit) {
        val preprocessedIngredients = preprocessIngredients(ingredients)
        val truncatedIngredients = preprocessedIngredients.take(10)

        Log.d("OpenAIRequest", "Ingredients to validate: ${truncatedIngredients.joinToString(", ")}")
        val messages = listOf(
            RequestMessage(
                "system",
                """
                        You are a professional chef, culinary adviser, and data parser. Your role is to validate cooking ingredients and provide clear, actionable feedback. Respond strictly in JSON format.
                    
                        For each input:
                        - Flag any non-ingredient entries such as sentences, questions, incomprehensible or irrelevant words as "invalidIngredients."
                        - Exclude items that are illogical for cooking or unsuitable for recipes. Group these under "invalidIngredients."
                        - Example response: {
                            "refinedIngredients": ["chicken", "garlic"],
                            "feedbackMessage": "Some items were removed for better synergy, like 'ice cream' and 'car keys.'",
                            "invalidIngredients": ["laptop", "car keys"]
                          }.
                    
                        Always respond in JSON format and avoid adding explanations outside the structure.
                        """
            ),
            RequestMessage(
                "user",
                        """
                        Validate the following ingredients for cooking: ${truncatedIngredients.joinToString(", ")}.
                        Tasks:
                        1. Identify and return a "refinedIngredients" list with logical, valid cooking ingredients. Remove any illogical or strange ingredients.
                        2. Provide a "feedbackMessage" offering helpful and humorous comments based on the list. For example:
                           - If less than 5 ingredients, suggest adding more.
                           - If strange ingredients are present, mention and explain why they were removed.
                        3. Mention if any ingredients are strictly invalid or unrecognized separately. Flag any entries that are:
                           - Clearly non-ingredients (e.g., sentences, random questions, or unrelated words).
                           - Illogical or unsuitable for cooking (e.g., "ice cream" for a savory recipe).
                        Format the response strictly in JSON as shown:
                        {
                          "refinedIngredients": [list of valid ingredients],
                          "feedbackMessage": "string with feedback for the user",
                          "invalidIngredients": [list of invalid ingredients or unrecognized items, if any]
                        }
                        """.trimIndent()
            )
        )

        makeOpenAIRequest(
            messages = messages,
            maxTokens = 2048,
            temperature = 0.7f
        ) { response ->
            if (response != null) {
                Log.d("OpenAIResponse", "Raw AI response: $response")
                try {
                    // Parse AI response
                    val jsonResponse = JSONObject(response)
                    // Extract from JSON
                    val refinedIngredients = jsonResponse.optJSONArray("refinedIngredients")?.toList() ?: emptyList()
                    val feedbackMessage = jsonResponse.optString("feedbackMessage", "No feedback provided.")
                    val invalidIngredients = jsonResponse.optJSONArray("invalidIngredients")?.toList() ?: emptyList()

                    val hasStrangeIngredients = refinedIngredients.size < truncatedIngredients.size

                    // Handle
                    if (refinedIngredients.isEmpty()) {
                        Log.w("ChatGPT", "Refined ingredients are empty.")
                        callback(false, emptyList(), "No valid ingredients found.", false)
                    } else if (invalidIngredients.isNotEmpty()) {
                        callback(false, refinedIngredients, "Invalid ingredients: ${invalidIngredients.joinToString(", ")}", hasStrangeIngredients)
                    } else {
                        callback(true, refinedIngredients, feedbackMessage, hasStrangeIngredients)
                    }
                } catch (e: JSONException) {
                    Log.e("ChatGPT", "Error parsing AI response: ${e.message}")
                    callback(false, emptyList(), "Failed to parse AI response.", false)
                }
            } else {
                showToast("No response from your CookPal.")
                Log.e("validateIngredients", "No response received from OpenAI.")
                callback(false, emptyList(), "AI validation request failed.", false)
            }
        }
    }

    // Helper to convert JSONArray to List
    private fun JSONArray.toList(): List<String> = List(length()) { getString(it) }

    private fun generateRecipe(ingredients: List<String>) {
        // Create a unique key for caching based on the ingredients list
        val ingredientsKey = ingredients.joinToString(",").hashCode().toString()
        // Check if the recipe exists in the cache
        val cachedRecipe = getCachedRecipe(ingredientsKey)

        if (cachedRecipe != null) {
            // Use the cached recipe
            Log.d("ChatGPT", "Using cached recipe.")
            showRecipeSuggestion(cachedRecipe)
        } else {
            val messages = listOf(
                RequestMessage("system",
                    """
                            You are a professional chef, providing clear, concise, and authentic recipes that are well-structured and easy to follow. 
                            Each recipe should strictly follow the JSON structure and contain the following fields:
                            {
                                "title": "The name of the dish",
                                "summary": "A brief, 1-2 sentence description of the dish",
                                "ingredients": ["List", "of", "ingredients"],
                                "instructions": ["Detailed", "step-by-step", "cooking", "instructions"],
                                "imageURL": "A valid URL pointing to an image of the dish (if available)"
                            }
                            Ensure that the output is **strictly in JSON format** with no additional text or explanation.
                            Avoid conversational language or pleasantries. Focus solely on providing the recipe content in the above structure.
                            """),
                RequestMessage("user",
                    """
                            Provide a recipe using these ingredients: ${ingredients.joinToString(", ")}. 
                            The recipe should strictly follow the JSON format below:
                            {
                                "title": "The name of the dish",
                                "summary": "A brief description of the dish (1-2 sentences max)",
                                "ingredients": ["List", "all", "the", "ingredients"],
                                "instructions": ["Provide", "detailed", "step-by-step", "cooking", "instructions"],
                                "imageURL": "A URL pointing to an image of the dish (if available)"
                            }
                            """)
            )


            makeOpenAIRequest(messages, maxTokens = 2048, temperature = 0.7f, topP = 0.9f) { response ->
                if (response != null) {
                    Log.d("OpenAIResponse", "Raw AI response: $response")

                    try {
                        val jsonResponse = JSONObject(response)

                        // Parse AI response to create AIRecipe object
                        val title = jsonResponse.optString("title","No title")
                        val summary = jsonResponse.optString("summary","No summary available")
                        val image = jsonResponse.optString("image", "")
                        val ingredients = jsonResponse.optJSONArray("ingredients")?.toList() ?: emptyList()
                        val instructions = jsonResponse.optJSONArray("instructions")?.toList() ?: emptyList()

                        val aiRecipe = AIRecipe(
                            title = title,
                            summary = summary,
                            image = image,
                            ingredients = ingredients,
                            instructions = instructions
                        )

                        // Save the recipe to the cache
                        saveRecipeToCache(ingredientsKey, aiRecipe)
                        showRecipeSuggestion(aiRecipe)

                    } catch (e: JSONException) {
                        Log.e("ChatGPT", "Error parsing AI response: ${e.message}")
                        showToast("Failed to parse AI response.")
                    }
                } else {
                    showToast("Failed to generate a recipe.")
                    Log.e("ChatGPT", "No content in response body.")
                }
            }
        }
    }

    private fun showRecipeSuggestion(aiRecipe: AIRecipe) {
        val intent = Intent(this, RecipeDetails::class.java)

        // Pass the data to the new activity using intent extras
        intent.putExtra("isAIRecipe", true) // Set the flag for AI-generated recipe
        intent.putExtra("title", aiRecipe.title)
        intent.putExtra("summary", aiRecipe.summary)
        intent.putExtra("image", aiRecipe.image) // Pass image URL
        intent.putStringArrayListExtra("ingredients", ArrayList(aiRecipe.ingredients)) // Pass ingredients list
        intent.putStringArrayListExtra("instructions", ArrayList(aiRecipe.instructions)) // Pass instructions list

        // Start the new activity
        startActivity(intent)
    }

    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this@MyIngredientsActivity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun notifyTruncatedIngredients(truncatedIngredients: List<String>) {
        val message = """
        You provided more than 15 ingredients. Only the first 15 ingredients were used:
        ${truncatedIngredients.joinToString(", ")}
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Ingredient Limit Reached")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun saveRecipeToCache(key: String, aiRecipe: AIRecipe) {
        val sharedPreferences = getSharedPreferences("RecipeCache", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Serialize the AIRecipe object to JSON using Gson
        val jsonRecipe = Gson().toJson(aiRecipe)
        editor.putString(key, jsonRecipe)
        editor.apply()
    }

    private fun getCachedRecipe(key: String): AIRecipe? {
        val sharedPreferences = getSharedPreferences("RecipeCache", Context.MODE_PRIVATE)
        val jsonRecipe = sharedPreferences.getString(key, null)

        return if (jsonRecipe != null) {
            // Deserialize the JSON back into an AIRecipe object
            Gson().fromJson(jsonRecipe, AIRecipe::class.java)
        } else {
            null
        }
    }

    fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }

}
