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
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
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

    private var editingPosition: Int? = null
    private var ingredientsList: MutableList<String> = mutableListOf()
    private lateinit var foodPreferences: Map<String, Any>


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
        val textClearList: TextView = findViewById(R.id.textClearList)

        val content = SpannableString(textClearList.text)
        content.setSpan(UnderlineSpan(), 0, content.length, 0)

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

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            val loginIntent = Intent(this, Login::class.java)
            startActivity(loginIntent)
            finish()
            return
        }

        sharedPreferences = getSharedPreferences(prefsFileName, Context.MODE_PRIVATE)

        foodPreferences = loadFoodPreferences()

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
                }
                .addOnFailureListener { e ->
                    showToast("Failed to save ingredients. Please check your connection.")
                }
        }
    }

    override fun onResume() {
        super.onResume()
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.GONE
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
            editingPosition = position // Save the position being edited

            buttonAddIngredient.setOnClickListener {
                val updatedIngredient = editTextIngredient.text.toString().trim()
                if (updatedIngredient.isEmpty()) {
                    showToast("Please enter a valid ingredient.")
                } else if (ingredientsList.contains(updatedIngredient.uppercase()) &&
                    currentIngredient.uppercase() != updatedIngredient.uppercase()) {
                    showToast("This ingredient already exists.")
                } else {
                    editingPosition?.let { pos ->
                        if (pos in ingredientsList.indices) {
                            ingredientsList[pos] = updatedIngredient.uppercase()
                            ingredientsAdapter.notifyItemChanged(pos)
                            showToast("Ingredient updated successfully.")
                        }
                    }
                    resetAddIngredientButton()
                    saveIngredients()
                    editTextIngredient.setText("") // Clear the input field
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
        if (position in ingredientsList.indices) {
            val deletedIngredient = ingredientsList.removeAt(position)

            ingredientsAdapter.notifyItemRemoved(position)

            ingredientsAdapter.notifyItemRangeChanged(position, ingredientsList.size - position)

            if (ingredientsList.isEmpty()) {
                ingredientsAdapter.notifyDataSetChanged()
                showToast("All ingredients have been removed.")
            }

            if (editingPosition == position) {
                resetAddIngredientButton()
                editTextIngredient.setText("")
                editingPosition = null
                showToast("The ingredient being edited was deleted. Editing canceled.")
            } else if (editingPosition != null && position < editingPosition!!) {
                editingPosition = editingPosition!! - 1
            }
            saveIngredients()
            showToast("Ingredient '$deletedIngredient' deleted.")

        } else {
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
//                    showToast("API request failed: ${t.message}")
                    Log.e("OpenAIResponse", "Error: ${t.message}")
                    onResponse(null)
                }
            })
    }

    private fun loadFoodPreferences(): Map<String, Any> {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        // Load the saved food preferences
        val dietaryRestrictions = sharedPreferences.getStringSet("dietary_restrictions", emptySet()) ?: emptySet()
        val displayedSensitivities = sharedPreferences.getStringSet("displayed_sensitivities", emptySet()) ?: emptySet()
        val nutritionalPreferences = sharedPreferences.getStringSet("nutritional_preferences", emptySet()) ?: emptySet()

        // Log the preferences for debugging
        Log.d("MyIngredientsActivity", "Loaded Displayed Sensitivities: $displayedSensitivities")
        Log.d("MyIngredientsActivity", "Loaded Nutritional Preferences: $nutritionalPreferences")
        Log.d("MyIngredientsActivity", "Loaded Dietary Restrictions: $dietaryRestrictions")

        // Return the preferences as a Map
        return mapOf(
            "dietaryRestrictions" to dietaryRestrictions,
            "displayedSensitivities" to displayedSensitivities,
            "nutritionalPreferences" to nutritionalPreferences
        )
    }

    private fun preprocessIngredients(ingredients: List<String>): List<String> {
        // Normalize input
        return ingredients
            .map { it.trim().lowercase() }
            .distinct()
            .sorted()
    }

    private fun getRecipeSuggestion() {

        // Reference the ProgressBar
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        // Show the spinner
        progressBar.visibility = View.VISIBLE

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
        validateIngredients(preprocessedIngredients, foodPreferences) { isValid, refinedList, feedbackMessage, hasStrangeIngredients, violatedPreferences ->
            // Hide the spinner after processing
            progressBar.visibility = View.GONE

            if (isValid) {
                val dialogBuilder = AlertDialog.Builder(this)
                    .setTitle("Your CookPal's Feedback")
                    .setMessage(feedbackMessage)

                if (hasStrangeIngredients || violatedPreferences.isNotEmpty()) {
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

    private fun validateIngredients(ingredients: List<String>, foodPreferences: Map<String, Any>, callback: (Boolean, List<String>, String?, Boolean, List<String>) -> Unit) {
        val preprocessedIngredients = preprocessIngredients(ingredients)
        val truncatedIngredients = preprocessedIngredients.take(10)

        // Extract preferences from the provided map
        val dietaryRestrictions = foodPreferences["dietaryRestrictions"] as Set<String>
        val displayedSensitivities = foodPreferences["displayedSensitivities"] as Set<String>
        val nutritionalPreferences = foodPreferences["nutritionalPreferences"] as Set<String>

        Log.d("UserPreferences", "Dietary Restrictions: ${dietaryRestrictions.joinToString(", ")}")
        Log.d("UserPreferences", "Displayed Sensitivities: ${displayedSensitivities.joinToString(", ")}")
        Log.d("UserPreferences", "Nutritional Preferences: ${nutritionalPreferences.joinToString(", ")}")

        // Prepare the user preference details
        val preferenceDetails = """
            User Preferences:
            ${if (displayedSensitivities.isNotEmpty()) "- Displayed Sensitivities: ${displayedSensitivities.joinToString(", ")}" else "- Displayed Sensitivities: None"}
            ${if (nutritionalPreferences.isNotEmpty()) "- Nutritional Preferences: ${nutritionalPreferences.joinToString(", ")}" else "- Nutritional Preferences: None"}
            ${if (dietaryRestrictions.isNotEmpty()) "- Dietary Restrictions: ${dietaryRestrictions.joinToString(", ")}" else "- Dietary Restrictions: None"}
        """.trimIndent()

        Log.d("OpenAIRequest", "Sending the following data to AI:")
        Log.d("OpenAIRequest", "Ingredients to validate: ${truncatedIngredients.joinToString(", ")}")
        Log.d("OpenAIRequest", "Preferences:\n$preferenceDetails")

        val messages = listOf(
            RequestMessage(
                "system",
                """
                        You are a professional chef, culinary adviser, nutrition expert, and data parser. 
                        Your role is to validate cooking ingredients, provide clear, actionable feedback, and ensure they comply with user preferences: $preferenceDetails. 
                        Respond strictly in JSON format.
                    
                        Validation Criteria:
                        - Identify and categorize non-food item or illogical entries such as sentences, questions, incomprehensible or irrelevant words as "invalidIngredients."
                        - Enforce sensitivities (e.g., allergies like nuts, dairy) and remove ingredients violating these preferences.
                        - Consider nutritional preferences (e.g., high-protein, low-carb) for ingredient selection.
                        - Respect dietary restrictions (e.g., vegetarian, vegan) by removing non-compliant items.
                        - Exclude items that are illogical for cooking or unsuitable for recipes. Group these under "invalidIngredients."
                         - Flag ingredients that violate food preferences (e.g., "beef" for vegetarian) as "violatedPreferences."
                        
                        - Example response: {
                            "refinedIngredients": ["chicken", "garlic"],
                            "feedbackMessage": "Some items were removed for better synergy, like 'ice cream' and 'car keys.'",
                            "violatedPreferences": ["pork"],
                            "invalidIngredients": ["laptop", "car keys"]
                          }.
                    
                        Always provide feedback explaining your decisions. Strictly respond in JSON format, avoid adding explanations outside the structure.
                        """.trimIndent()
            ),
            RequestMessage(
                "user",
                """
                        Validate the following ingredients for cooking: ${truncatedIngredients.joinToString(", ")}.
                        Tasks:
                        1. Identify and return a "refinedIngredients" list with logical, valid cooking ingredients. Remove any illogical or strange ingredients.
                        2. Remove any ingredients that violate allergies, nutritional preferences, or dietary restrictions
                        (e.g., no dairy for lactose-intolerant users, no nuts for nut allergies, consider high protein ingredients for high-protein diet, vegetables only for vegetarian, etc.).
                        3. Provide a "feedbackMessage" offering helpful and humorous comments based on the list such as:
                           - If less than 5 ingredients, suggest adding more.
                           - If strange or irrelevant ingredients are present, mention and explain why they were removed (e.g., "removed due to being non-vegan").
                           - What ingredients were removed due to dietary restrictions or preferences.
                           - The reason why an ingredient was removed (e.g., "pork is not allowed in a vegetarian diet").
                        4. Mention if any ingredients are strictly invalid or unrecognized separately. Flag any entries that are:
                           - Clearly non-ingredients (e.g., sentences, random questions, or unrelated words).
                           - Illogical or unsuitable for cooking (e.g., "ice cream" for a savory recipe).
                           - Violated dietary or nutritional preferences and why.
                       
                           
                        5. Format the response strictly in JSON as shown:
                        {
                          "refinedIngredients": [list of valid ingredients],
                          "feedbackMessage": "string with feedback for the user",
                          "violatedPreferences": [list of ingredients that violate food preferences],
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
                    val violatedPreferences = jsonResponse.optJSONArray("violatedPreferences")?.toList() ?: emptyList()
                    val invalidIngredients = jsonResponse.optJSONArray("invalidIngredients")?.toList() ?: emptyList()

                    Log.d("ValidationResults", "Refined Ingredients: ${refinedIngredients.joinToString(", ")}")
                    Log.d("ValidationResults", "Feedback Message: $feedbackMessage")
                    Log.d("ValidationResults", "Violated Preference: ${violatedPreferences.joinToString(", ")}")
                    Log.d("ValidationResults", "Invalid Ingredients: ${invalidIngredients.joinToString(", ")}")

                    val hasStrangeIngredients = refinedIngredients.size < truncatedIngredients.size

                    // Handle
                    if (refinedIngredients.isEmpty()) {
                        Log.w("ValidationResults", "Refined ingredients are empty.")
                        callback(false, emptyList(), "No valid ingredients found.", false, violatedPreferences)
                    } else if (invalidIngredients.isNotEmpty()) {
                        Log.w("ValidationResults", "Invalid Ingredients Detected: ${invalidIngredients.joinToString(", ")}")
                        callback(false, refinedIngredients, "Invalid ingredients: ${invalidIngredients.joinToString(", ")}", hasStrangeIngredients, violatedPreferences)
                    } else {
                        Log.i("ValidationResults", "Validation successful with refined ingredients.")
                        callback(true, refinedIngredients, feedbackMessage, hasStrangeIngredients, violatedPreferences)
                    }

                } catch (e: JSONException) {
                    Log.e("ValidationResults", "Error parsing AI response: ${e.message}")
                    callback(false, emptyList(), "Failed to parse AI response.", false, emptyList())
                }
            } else {
                showToast("No response from your CookPal.")
                Log.e("validateIngredients", "No response received from OpenAI.")
                callback(false, emptyList(), "AI validation request failed.", false, emptyList())
            }
        }
    }

    // Helper to convert JSONArray to List
    private fun JSONArray.toList(): List<String> = List(length()) { getString(it) }

    private fun generateRecipe(ingredients: List<String>) {

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

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
                            You are a professional chef tasked with, providing clear, concise, and authentic recipes that are well-structured and easy to follow.
                            Before generating the recipe, analyze the provided ingredients and determine the most appropriate recipe type: snack, meal, dessert, appetizer, or drink. Base your choice on the nature of the ingredients and their typical culinary uses
                            Once determined, generate a recipe based on the provided ingredients.
                            Each recipe should strictly follow the JSON structure and contain the following fields:
                            {
                                "title": "The name of the dish",
                                "summary": "A brief, 1-2 sentence description of the dish",
                                "ingredients": ["List", "of", "ingredients"],
                                "instructions": ["Detailed", "step-by-step", "cooking", "instructions"],
                                "imageURL": "A valid URL pointing to an image of the dish (if available)"
                            }
                            Include specific details necessary, such as cooking methods for meat based on doneness preferences (rare, medium rare, well done)
                            Ensure that the output is **strictly in JSON format** with no additional text or explanation.
                            Avoid conversational language or pleasantries. Focus solely on providing the recipe content in the above structure.
                            """.trimIndent()
                ),
                RequestMessage("user",
                    """
                            Provide a recipe using these ingredients: ${ingredients.joinToString(", ")}. 
                            The recipe should strictly follow the JSON format below:
                            {
                                "title": "The name of the dish",
                                "summary": "A brief description of the dish (1-2 sentences max)",
                                "ingredients": ["List", "all", "the", "ingredients"],
                                "instructions": [
                                    "Provide detailed step-by-step cooking instructions.",
                                    "If the recipe includes meat, specify the cooking instructions for different levels of doneness (e.g., rare, medium rare, well done).",
                                    "Include internal temperature guidelines for each doneness level, if applicable."
                                ],
                                "imageURL": "A URL pointing to an image of the dish (if available)"
                            }
                            """
                )
            )


            makeOpenAIRequest(messages, maxTokens = 2048, temperature = 0.7f, topP = 0.9f) { response ->
                // Hide the spinner once the recipe is received
                progressBar.visibility = View.GONE

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

        intent.putExtra("isAIRecipe", true) // Set the flag for AI-generated recipe
        intent.putExtra("title", aiRecipe.title)
        intent.putExtra("summary", aiRecipe.summary)
        intent.putExtra("image", aiRecipe.image) // Pass image URL
        intent.putStringArrayListExtra("ingredients", ArrayList(aiRecipe.ingredients)) // Pass ingredients list
        intent.putStringArrayListExtra("instructions", ArrayList(aiRecipe.instructions)) // Pass instructions list
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