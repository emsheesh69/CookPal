package com.example.cookpal


import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cookpal.Adapters.CookingHistoryAdapter
import com.example.cookpal.Adapters.FavoritesAdapter
import com.example.cookpal.Models.FavModel
import com.example.cookpal.Models.RecipeModel
import com.example.cookpal.listeners.ClickedRecipeListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class UserActivity : AppCompatActivity(), ClickedRecipeListener {
    private lateinit var navDiscover: LinearLayout
    private lateinit var navIngredients: LinearLayout
    private lateinit var navVoiceCommand: LinearLayout
    private lateinit var navSettings: LinearLayout
    private lateinit var tabPreferences: TextView
    private lateinit var tabActivity: TextView
    private lateinit var userNameTextView: TextView
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var favRecyclerView: RecyclerView
    private lateinit var historyAdapter: CookingHistoryAdapter
    private lateinit var favoritesAdapter: FavoritesAdapter
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.black) // Replace with your color
            window.isNavigationBarContrastEnforced = true // Ensures contrast with buttons
        }
        setContentView(R.layout.activity_user)
        navDiscover = findViewById(R.id.nav_discover)
        navIngredients = findViewById(R.id.nav_ingredients)
        navVoiceCommand = findViewById(R.id.nav_voice_command)
        navSettings = findViewById(R.id.nav_settings)
        tabPreferences = findViewById(R.id.tab_preferences)
        tabActivity = findViewById(R.id.tab_activity)
        userNameTextView = findViewById(R.id.userName)
        historyRecyclerView = findViewById(R.id.cooking_history_list)
        favRecyclerView = findViewById(R.id.recycler_favorites)
        historyRecyclerView.layoutManager = LinearLayoutManager(this)
        favRecyclerView.layoutManager = LinearLayoutManager(this)

        historyAdapter = CookingHistoryAdapter(this)
        favoritesAdapter = FavoritesAdapter(this)

        historyRecyclerView.adapter = historyAdapter
        favRecyclerView .adapter = favoritesAdapter

        fetchCookingHistory()

        fetchFavorites()

        displayUserEmail()

        tabPreferences.setOnClickListener {
            setTab(tabPreferences)
            // Redirect to the Preferences screen
            startActivity(Intent(this, UserPreference::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        tabActivity.setOnClickListener {
            setTab(tabActivity)
            // Redirect to the UserActivity screen
            startActivity(Intent(this, UserActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }


        navDiscover.setOnClickListener {
            setHighlightedTab(navDiscover)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        navIngredients.setOnClickListener {
            setHighlightedTab(navIngredients)
            startActivity(Intent(this, MyIngredientsActivity::class.java))
            finish()
        }
        navVoiceCommand.setOnClickListener {
            setHighlightedTab(navVoiceCommand)
            startActivity(Intent(this, VoiceCommandActivity::class.java))
            finish()
        }
        navSettings.setOnClickListener {
            setHighlightedTab(navSettings)
        }

        setTab(tabActivity)
        setHighlightedTab(navSettings)
    }

    private fun fetchCookingHistory() {
        val user = FirebaseAuth.getInstance().currentUser
        val emptyStateLayout = findViewById<LinearLayout>(R.id.empty_cooking_history) // Find the empty state layout

        if (user != null) {
            database = FirebaseDatabase.getInstance().getReference("users/${user.uid}/Cooking History")
            database.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val historyList = ArrayList<RecipeModel>()
                    for (data in snapshot.children) {
                        val recipe = data.getValue(RecipeModel::class.java)
                        if (recipe != null) {
                            historyList.add(recipe)
                        }
                    }
                    if (historyList.isEmpty()) {
                        // Show empty state
                        emptyStateLayout.visibility = View.VISIBLE
                        historyRecyclerView.visibility = View.GONE
                    } else {
                        // Show cooking history
                        emptyStateLayout.visibility = View.GONE
                        historyRecyclerView.visibility = View.VISIBLE
                        historyAdapter.setRecipes(historyList)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@UserActivity, "Failed to fetch history.", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchFavorites() {
        val user = FirebaseAuth.getInstance().currentUser
        val emptyStateLayout = findViewById<LinearLayout>(R.id.empty_likes_section)
        if (user != null) {
            database = FirebaseDatabase.getInstance().getReference("users/${user.uid}/Favorites")
            database.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val favoritesList = ArrayList<FavModel>()
                    for (data in snapshot.children) {
                        val recipe = data.getValue(FavModel::class.java)
                        if (recipe != null) {
                            favoritesList.add(recipe)
                        }
                    }
                    if (favoritesList.isEmpty()) {
                        // Show empty state
                        emptyStateLayout.visibility = View.VISIBLE
                        favRecyclerView.visibility = View.GONE
                    } else {
                        // Show cooking history
                        emptyStateLayout.visibility = View.GONE
                        favRecyclerView.visibility = View.VISIBLE
                        favoritesAdapter.setFavorites(favoritesList)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@UserActivity, "Failed to fetch favorites.", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun displayUserEmail() {
        // Get the current Firebase user
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Check if the user is signed in
        if (currentUser != null) {
            // Retrieve the user's email
            val userEmail = currentUser.email

            // Set the email to the TextView
            userNameTextView.text = userEmail
        } else {
            // If no user is signed in, show a default message
            userNameTextView.text = "Guest"
        }
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

    private fun setTab(selectedTab: TextView) {
        resetTabs()

        // Set the selected tab's text color to highlight color
        selectedTab.setTextColor(ContextCompat.getColor(this, R.color.highlight_color))
    }

    private fun resetTabs() {
        val tabs = listOf(tabPreferences, tabActivity)
        for (tab in tabs) {
            tab.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
    }

    override fun onRecipeClicked(id: String) {
        val intent = Intent(this, RecipeDetails::class.java)
        intent.putExtra("id", id)
        startActivity(intent)
    }
}