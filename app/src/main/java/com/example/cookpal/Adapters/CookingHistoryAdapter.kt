package com.example.cookpal.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cookpal.Models.RecipeModel
import com.example.cookpal.R
import com.example.cookpal.listeners.ClickedRecipeListener
import com.squareup.picasso.Picasso

class CookingHistoryAdapter(
    private val listener: ClickedRecipeListener
) : RecyclerView.Adapter<CookingHistoryAdapter.CookingHistoryViewHolder>() {

    private var recipes: List<RecipeModel> = listOf()

    fun setRecipes(list: List<RecipeModel>) {
        recipes = list.sortedByDescending { it.date } // Assuming `date` is in a sortable format
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CookingHistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cooking_history, parent, false)
        return CookingHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CookingHistoryViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.bind(recipe)
        holder.itemView.setOnClickListener {
            listener.onRecipeClicked(recipe.id.toString())
        }
    }

    override fun getItemCount(): Int = recipes.size

    inner class CookingHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recipeImage: ImageView = itemView.findViewById(R.id.recipe_image)
        private val recipeName: TextView = itemView.findViewById(R.id.recipe_name)
        private val cookingDate: TextView = itemView.findViewById(R.id.cooking_date)

        fun bind(recipe: RecipeModel) {
            recipeName.text = recipe.name
            cookingDate.text = recipe.date
            Picasso.get().load(recipe.image).into(recipeImage)
        }
    }
}
