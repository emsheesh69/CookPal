package com.example.cookpal

import RandomRecipeAdapter
import com.example.cookpal.adapters.ComplexSearchAdapter
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cookpal.Models.ComplexSearchApiResponse
import com.example.cookpal.Models.RandomRecipeApiResponse
import com.example.cookpal.listeners.ClickedRecipeListener
import com.example.cookpal.listeners.ComplexSearchListener
import com.example.cookpal.listeners.RandomRecipeResponseListener

class MainActivity : AppCompatActivity(), ClickedRecipeListener {

    private lateinit var dialog: ProgressDialog
    private lateinit var manager: RequestManager
    private lateinit var recyclerRecommended: RecyclerView
    private lateinit var recyclerPopular: RecyclerView
    private lateinit var spinner: Spinner
    private val tags: MutableList<String> = mutableListOf()
    private lateinit var searchView: SearchView

    private lateinit var navDiscover: LinearLayout
    private lateinit var navIngredients: LinearLayout
    private lateinit var navVoiceCommand: LinearLayout
    private lateinit var navSettings: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dialog = ProgressDialog(this)

        spinner = findViewById(R.id.spinner)
        val arrayAdapter: ArrayAdapter<CharSequence> = ArrayAdapter.createFromResource(
            this,
            R.array.tags,
            R.layout.spinner_text
        )
        spinner.adapter = arrayAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                tags.clear()
                val selectedTag = parent?.getItemAtPosition(position).toString()
                tags.add(selectedTag)
                manager.getRandomRecipes(randomRecipeResponseListener, tags)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        arrayAdapter.setDropDownViewResource(R.layout.spinner_inner_text)

        manager = RequestManager(this)

        recyclerRecommended = findViewById(R.id.recycler_recommended)
        recyclerRecommended.setHasFixedSize(true)
        recyclerRecommended.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        recyclerPopular = findViewById(R.id.recycler_popular)
        recyclerPopular.setHasFixedSize(true)
        recyclerPopular.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        searchView = findViewById(R.id.searchView_home)
        searchView.setOnClickListener { searchView.isIconified = false }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    val titleMatch = it
                    val excludeIngredients = listOf<String>()
                    val includeIngredients = listOf<String>()
                    val numberOfRecipes = 50
                    manager.getComplexSearch(complexSearchListener, excludeIngredients, includeIngredients, numberOfRecipes, titleMatch)
                    dialog.setMessage("Fetching recipes...")
                    dialog.show()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean = true
        })

        manager.getRandomRecipes(randomRecipeResponseListener, tags)

        navDiscover = findViewById(R.id.nav_discover)
        navIngredients = findViewById(R.id.nav_ingredients)
        navVoiceCommand = findViewById(R.id.nav_voice_command)
        navSettings = findViewById(R.id.nav_settings)

        navDiscover.setOnClickListener { setHighlightedTab(navDiscover) }

        navIngredients.setOnClickListener {
            setHighlightedTab(navIngredients)
            val intent = Intent(this, MyIngredientsActivity::class.java)
            startActivity(intent)
        }

        navVoiceCommand.setOnClickListener {
            // Start VoiceCommandActivity when this button is clicked
            setHighlightedTab(navVoiceCommand)
            val intent = Intent(this, VoiceCommandActivity::class.java)
            startActivity(intent)
        }

        setHighlightedTab(navDiscover)
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
            response.results?.let {
                val recommendedRecipes = it.take(25)
                val popularRecipes = it.drop(25)
                recyclerRecommended.adapter = ComplexSearchAdapter(this@MainActivity, recommendedRecipes, this@MainActivity)
                recyclerPopular.adapter = ComplexSearchAdapter(this@MainActivity, popularRecipes, this@MainActivity)
            }
        }

        override fun didError(message: String) {
            dialog.dismiss()
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private val randomRecipeResponseListener = object : RandomRecipeResponseListener {
        override fun didFetch(response: RandomRecipeApiResponse, message: String) {
            dialog.dismiss()
            response.recipes?.let {
                val recommendedRecipes = it.take(25)
                val popularRecipes = it.drop(25)
                recyclerRecommended.adapter = RandomRecipeAdapter(this@MainActivity, recommendedRecipes, tags, this@MainActivity)
                recyclerPopular.adapter = RandomRecipeAdapter(this@MainActivity, popularRecipes, tags, this@MainActivity)
            }
        }

        override fun didError(message: String) {
            dialog.dismiss()
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }
}
