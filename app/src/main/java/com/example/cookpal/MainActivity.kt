package com.example.cookpal

import RandomRecipeAdapter
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cookpal.Models.ComplexSearchApiResponse
import com.example.cookpal.Models.RandomRecipeApiResponse
import com.example.cookpal.adapters.ComplexSearchAdapter
import com.example.cookpal.adapters.PopularRecipeAdapter
import com.example.cookpal.listeners.ClickedRecipeListener
import com.example.cookpal.listeners.ComplexSearchListener
import com.example.cookpal.listeners.RandomRecipeResponseListener
import android.Manifest
import androidx.appcompat.app.AlertDialog
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.example.cookpal.Models.Recipe

class MainActivity : AppCompatActivity(), ClickedRecipeListener {
    private lateinit var dialog: ProgressDialog
    private lateinit var manager: RequestManager
    private lateinit var recyclerRecommended: RecyclerView
    private lateinit var recyclerPopular: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var buttonFilter: ImageView
    private lateinit var navDiscover: LinearLayout
    private lateinit var navIngredients: LinearLayout
    private lateinit var navVoiceCommand: LinearLayout
    private lateinit var navSettings: LinearLayout

    private val selectedTags: MutableList<String> = mutableListOf()
    private var currentSearchQuery: String = ""
    private val REQUEST_CODE_RECORD_AUDIO = 1
    private val REQUEST_CODE_POST_NOTIFICATIONS = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dialog = ProgressDialog(this)
        dialog.setMessage("Checking Permissions...")
        dialog.show()

        checkPermissions()
        initializeViews()
        setupSearchView()
        setupNavigationTabs()
        setupFilterButton()
        loadInitialRecipes()
        dialog.dismiss()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_CODE_RECORD_AUDIO)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_POST_NOTIFICATIONS)
        }
    }

    private fun initializeViews() {
        manager = RequestManager(this)

        recyclerRecommended = findViewById(R.id.recycler_recommended)
        recyclerRecommended.setHasFixedSize(true)
        recyclerRecommended.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        recyclerPopular = findViewById(R.id.recycler_popular)
        recyclerPopular.setHasFixedSize(true)
        recyclerPopular.layoutManager = GridLayoutManager(this, 2)

        searchView = findViewById(R.id.searchView_home)
        buttonFilter = findViewById(R.id.button_filter)

        navDiscover = findViewById(R.id.nav_discover)
        navIngredients = findViewById(R.id.nav_ingredients)
        navVoiceCommand = findViewById(R.id.nav_voice_command)
        navSettings = findViewById(R.id.nav_settings)
    }

    private fun setupSearchView() {
        searchView.setOnSearchClickListener {
            val currentQuery = searchView.query.toString()
            if (currentQuery.isNotEmpty()) {
                performSearch(currentQuery, selectedTags)
            }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    if (it.isNotEmpty()) {
                        currentSearchQuery = it
                        performSearch(it, selectedTags)
                        searchView.clearFocus()
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText ?: ""
                return true
            }
        })

        searchView.setOnClickListener {
            searchView.isIconified = false
            val query = searchView.query.toString()
            if (query.isNotEmpty()) {
                performSearch(query, selectedTags)
            }
        }
    }

    private fun setupNavigationTabs() {
        navDiscover.setOnClickListener { setHighlightedTab(navDiscover) }

        navIngredients.setOnClickListener {
            setHighlightedTab(navIngredients)
            startActivity(Intent(this, MyIngredientsActivity::class.java))
        }

        navVoiceCommand.setOnClickListener {
            setHighlightedTab(navVoiceCommand)
            startActivity(Intent(this, VoiceCommandActivity::class.java))
        }

        navSettings.setOnClickListener {
            setHighlightedTab(navSettings)
            startActivity(Intent(this, UserPreference::class.java))
        }

        setHighlightedTab(navDiscover)
    }

    private fun setupFilterButton() {
        buttonFilter.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun loadInitialRecipes() {
        manager.getRandomRecipes(randomRecipeResponseListener, selectedTags)
    }

    private fun performSearch(query: String, tags: List<String> = listOf()) {
        searchView.clearFocus()
        dialog.setMessage("Fetching recipes...")
        dialog.show()

        var type: String? = null
        var cuisine: String? = null
        var diet: String? = null

        tags.forEach { tag ->
            when (tag.lowercase()) {
                "appetizers", "dinner", "lunch", "breakfast" -> type = tag
                "asian", "european", "african", "chinese", "japanese" -> cuisine = tag
                "vegetarian", "vegan", "gluten free" -> diet = tag
            }
        }

        manager.getComplexSearch(
            complexSearchListener,
            listOf(),
            listOf(),
            50,
            query
//            diet,
//            type,
//            cuisine
        )

        if (tags.isNotEmpty()) {
            manager.getRandomRecipes(randomRecipeResponseListener, selectedTags)
        }
    }

    private fun showFilterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.filter_layout, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val checkBoxes = mapOf(
            "appetizers" to dialogView.findViewById(R.id.checkbox_appetizers),
            "dinner" to dialogView.findViewById(R.id.checkbox_dinner),
            "lunch" to dialogView.findViewById(R.id.checkbox_lunch),
            "breakfast" to dialogView.findViewById(R.id.checkbox_breakfast),
            "vegetarian" to dialogView.findViewById(R.id.checkbox_vegetarian),
            "gluten free" to dialogView.findViewById(R.id.checkbox_dairy_free),
            "vegan" to dialogView.findViewById(R.id.checkbox_vegan),
            "asian" to dialogView.findViewById(R.id.checkbox_asian),
            "european" to dialogView.findViewById(R.id.checkbox_european),
            "african" to dialogView.findViewById(R.id.checkbox_african),
            "chinese" to dialogView.findViewById(R.id.checkbox_chinese),
            "japanese" to dialogView.findViewById<CheckBox>(R.id.checkbox_japanese)
        )

        val buttonApplyFilter = dialogView.findViewById<Button>(R.id.btn_apply_filters)
        val buttonClearFilter = dialogView.findViewById<Button>(R.id.btn_clear_filters)

        buttonApplyFilter.setOnClickListener {
            selectedTags.clear()
            checkBoxes.forEach { (tag, checkBox) ->
                if (checkBox.isChecked) selectedTags.add(tag)
            }
            performSearch(currentSearchQuery, selectedTags)
            dialog.dismiss()
        }

        buttonClearFilter.setOnClickListener {
            checkBoxes.values.forEach { it.isChecked = false }
            selectedTags.clear()
            performSearch(currentSearchQuery)
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        dialog.dismiss()

        when (requestCode) {
            REQUEST_CODE_RECORD_AUDIO -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Microphone permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
                    showPermissionDeniedDialog()
                }
            }
            REQUEST_CODE_POST_NOTIFICATIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
                    showPermissionDeniedDialog()
                }
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setMessage("This app requires the requested permissions to work properly. Please go to settings and grant the permissions.")
            .setCancelable(false)
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
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

    override fun onRecipeClicked(id: String) {
        val intent = Intent(this, RecipeDetails::class.java).apply {
            putExtra("id", id)
        }
        startActivity(intent)
    }

    private val complexSearchListener = object : ComplexSearchListener {
        override fun didFetch(response: ComplexSearchApiResponse, message: String) {
            dialog.dismiss()
            response.results?.let { results ->
                val recommendedRecipes = results.take(25).map { mapToRecipe(it) }
                val popularRecipes = results.drop(25).take(25).map { mapToRecipe(it) }

                recyclerRecommended.adapter = ComplexSearchAdapter(
                    this@MainActivity,
                    recommendedRecipes,
                    this@MainActivity
                )

                recyclerPopular.adapter = PopularRecipeAdapter(
                    this@MainActivity,
                    popularRecipes,
                    this@MainActivity
                )
            }
        }

        override fun didError(message: String) {
            dialog.dismiss()
            Log.e("ComplexSearch", "Error: $message")
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private val randomRecipeResponseListener = object : RandomRecipeResponseListener {
        override fun didFetch(response: RandomRecipeApiResponse, message: String) {
            dialog.dismiss()
            response.recipes?.let {
                val recommendedRecipes = it.take(25)
                val popularRecipes = it.drop(25)
                recyclerRecommended.adapter = RandomRecipeAdapter(
                    this@MainActivity,
                    recommendedRecipes,
                    selectedTags,
                    this@MainActivity
                )
                recyclerPopular.adapter = PopularRecipeAdapter(
                    this@MainActivity,
                    popularRecipes,
                    this@MainActivity
                )
            }
        }

        override fun didError(message: String) {
            dialog.dismiss()
            Log.e("RandomRecipe", "Error: $message")
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun mapToRecipe(complexRecipe: ComplexSearchApiResponse.Recipe): Recipe {
        return Recipe().apply {
            id = complexRecipe.id
            title = complexRecipe.title
            image = complexRecipe.image
            imageType = complexRecipe.imageType

        }
    }
}