package com.example.cookpal

import RandomRecipeAdapter
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
import com.example.cookpal.Models.RandomRecipeApiResponse
import com.example.cookpal.listeners.ClickedRecipeListener
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

        dialog = ProgressDialog(this).apply {
            setTitle("Loading")
        }

        // Initialize Spinner and ArrayAdapter
        spinner = findViewById(R.id.spinner) // Ensure this ID is correct
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
                manager.getRandomRecipes(randomRecipeResponseListener, tags)
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

        // Fetch recipes and show dialog
        manager.getRandomRecipes(randomRecipeResponseListener, tags)
        dialog.show()
        searchView = findViewById(R.id.searchView_home)
        searchView.setOnClickListener {
            searchView.isIconified = false
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    tags.clear()
                    tags.add(it) // Add query as tag
                    manager.getRandomRecipes(randomRecipeResponseListener, tags) // Fetch recipes with query as tag
                    dialog.show() // Show loading dialog
                }
                return true // Return true to signal that query submission was handled
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true // Handle text change if needed
            }
        })
    }

    override fun onRecipeClicked(id: String) {
        Toast.makeText(this, id, Toast.LENGTH_SHORT).show()
        // Start the RecipeDetailsActivity and pass the recipe ID
        val intent = Intent(this, RecipeDetails::class.java).apply {
            putExtra("id", id)
        }
        startActivity(intent)
    }

    val randomRecipeResponseListener: RandomRecipeResponseListener = object : RandomRecipeResponseListener {
        override fun didFetch(response: RandomRecipeApiResponse, message: String) {
            dialog.dismiss()
            response.recipes?.let {
                // Split data into two lists
                val recommendedRecipes = it.take(50)  // First 50 recipes
                val popularRecipes = it.drop(50)      // Remaining recipes

                // Set adapters with the split data
                recyclerRecommended.adapter = RandomRecipeAdapter(this@MainActivity, recommendedRecipes, tags, this@MainActivity)
                recyclerPopular.adapter = RandomRecipeAdapter(this@MainActivity, popularRecipes, tags, this@MainActivity)

            }
        }

        override fun didError(message: String) {
            dialog.dismiss()
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private val spinnerSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            manager.getRandomRecipes(randomRecipeResponseListener, tags)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }
}
