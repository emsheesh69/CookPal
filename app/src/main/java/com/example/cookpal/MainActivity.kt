package com.example.cookpal;

import RandomRecipeAdapter
import android.app.ProgressDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cookpal.Models.RandomRecipeApiResponse
import com.example.cookpal.R
import com.example.cookpal.RequestManager
import com.example.cookpal.listeners.RandomRecipeResponseListener

 class MainActivity : AppCompatActivity() {
    private lateinit var dialog: ProgressDialog
    private lateinit var manager: RequestManager
    private var randomRecipeAdapter: RandomRecipeAdapter? = null
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dialog = ProgressDialog(this)
        dialog.setTitle("Loading")

        manager = RequestManager(this)
        manager.getRandomRecipes(randomRecipeResponseListener)
        dialog.show()

        recyclerView = findViewById(R.id.recycler_random) as RecyclerView
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = GridLayoutManager(this, 1)
    }

    private val randomRecipeResponseListener: RandomRecipeResponseListener =
        object : RandomRecipeResponseListener {
            override fun didFetch(response: RandomRecipeApiResponse, message: String) {
                dialog.dismiss()
                response.recipes?.let {
                    randomRecipeAdapter = RandomRecipeAdapter(this@MainActivity, it)
                    recyclerView.adapter = randomRecipeAdapter
                }
            }

            override fun didError(message: String) {
                dialog.dismiss()
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
}
