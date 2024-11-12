package com.example.cookpal.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cookpal.Models.Recipe
import com.example.cookpal.R
import com.example.cookpal.listeners.ClickedRecipeListener
import com.squareup.picasso.Picasso

class PopularRecipeAdapter(
    private val context: Context,
    private val recipeList: List<Recipe>,
    private val listener: ClickedRecipeListener
) : RecyclerView.Adapter<PopularRecipeAdapter.PopularRecipeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopularRecipeViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_popular_recipe, parent, false)
        return PopularRecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: PopularRecipeViewHolder, position: Int) {
        val recipe = recipeList[position]
        holder.textViewTitle.text = recipe.title
        Picasso.get().load(recipe.image).into(holder.imageViewFood)

        holder.itemView.setOnClickListener {
            listener.onRecipeClicked(recipe.id.toString())
        }
    }

    override fun getItemCount(): Int = recipeList.size

    class PopularRecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewTitle: TextView = itemView.findViewById(R.id.textView_title_popular)
        val imageViewFood: ImageView = itemView.findViewById(R.id.imageView_food_popular)
    }
}
