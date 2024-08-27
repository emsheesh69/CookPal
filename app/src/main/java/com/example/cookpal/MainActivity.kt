package com.example.cookpal

import RandomRecipeAdapter
import android.app.ProgressDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cookpal.Models.RandomRecipeApiResponse
import com.example.cookpal.listeners.RandomRecipeResponseListener

class MainActivity : AppCompatActivity() {
    private lateinit var dialog: ProgressDialog
    private lateinit var manager: RequestManager
    private lateinit var recyclerRecommended: RecyclerView
    private lateinit var recyclerPopular: RecyclerView
    private lateinit var spinner: Spinner
    private val tags: MutableList<String> = mutableListOf()

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
                manager.getRandomRecipes(randomRecipeResponseListener)
                dialog.show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle the case when no item is selected
            }
        }

        // Set dropdown view resource
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
        manager.getRandomRecipes(randomRecipeResponseListener)
        dialog.show()
    }

    private val randomRecipeResponseListener: RandomRecipeResponseListener = object : RandomRecipeResponseListener {
        override fun didFetch(response: RandomRecipeApiResponse, message: String) {
            dialog.dismiss()
            response.recipes?.let {
                // Split data into two lists
                val recommendedRecipes = it.take(50)  // First 50 recipes
                val popularRecipes = it.drop(50)      // Remaining recipes

                // Set adapters with the split data
                recyclerRecommended.adapter = RandomRecipeAdapter(this@MainActivity, recommendedRecipes, tags)
                recyclerPopular.adapter = RandomRecipeAdapter(this@MainActivity, popularRecipes, tags)

            }
        }

        override fun didError(message: String) {
            dialog.dismiss()
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }
}
