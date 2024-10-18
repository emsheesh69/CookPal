package com.example.cookpal

import RandomRecipeAdapter
import com.example.cookpal.adapters.ComplexSearchAdapter
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SearchView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dialog = ProgressDialog(this)



        // Initialize Spinner and ArrayAdapter
        spinner = findViewById(R.id.spinner)
        val arrayAdapter: ArrayAdapter<CharSequence> = ArrayAdapter.createFromResource(
            this,
            R.array.tags,
            R.layout.spinner_text
        )
        spinner.adapter = arrayAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Handle the selected item
                tags.clear()
                val selectedTag = parent?.getItemAtPosition(position).toString()
                tags.add(selectedTag)

                // Correct parameter order
                manager.getRandomRecipes(randomRecipeResponseListener, tags)
                dialog.setMessage("Fetching recipes...")
                dialog.show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle the case when no item is selected
            }
        }

        arrayAdapter.setDropDownViewResource(R.layout.spinner_inner_text)

        // Initialize RequestManager and RecyclerViews
        manager = RequestManager(this)

        recyclerRecommended = findViewById(R.id.recycler_recommended)
        recyclerRecommended.setHasFixedSize(true)
        recyclerRecommended.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        recyclerPopular = findViewById(R.id.recycler_popular)
        recyclerPopular.setHasFixedSize(true)
        recyclerPopular.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        searchView = findViewById(R.id.searchView_home)
        searchView.setOnClickListener {
            searchView.isIconified = false
        }

        // Fetch random recipes immediately when the app starts
        manager.getRandomRecipes(randomRecipeResponseListener, tags)
        dialog.setMessage("Fetching random recipes...")
        dialog.show()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    // Use the query for titleMatch instead of tags
                    val titleMatch = it
                    val excludeIngredients = listOf<String>()
                    val includeIngredients = listOf<String>()  // Leave empty as we're focusing on titleMatch
                    val numberOfRecipes = 50

                    // Call getComplexSearch with titleMatch instead of tags
                    manager.getComplexSearch(complexSearchListener, excludeIngredients, includeIngredients, numberOfRecipes, titleMatch)
                    dialog.setMessage("Fetching recipes...")
                    dialog.show()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
    }

    override fun onRecipeClicked(id: String) {
        Toast.makeText(this, id, Toast.LENGTH_SHORT).show()
        val intent = Intent(this, RecipeDetails::class.java).apply {
            putExtra("id", id)
        }
        startActivity(intent)
    }

    // ComplexSearchListener to handle API response
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

    // RandomRecipeResponseListener to handle random recipe fetching
    private val randomRecipeResponseListener = object : RandomRecipeResponseListener {
        override fun didFetch(response: RandomRecipeApiResponse, message: String) {
            dialog.dismiss()
            response.recipes?.let {
                val recommendedRecipes = it.take(25)
                val popularRecipes = it.drop(25)
                recyclerRecommended.adapter = RandomRecipeAdapter(
                    this@MainActivity,
                    recommendedRecipes,
                    tags,
                    this@MainActivity
                )
                recyclerPopular.adapter = RandomRecipeAdapter(
                    this@MainActivity,
                    popularRecipes,
                    tags,
                    this@MainActivity
                )
            }
        }

        override fun didError(message: String) {
            dialog.dismiss()
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }
}
