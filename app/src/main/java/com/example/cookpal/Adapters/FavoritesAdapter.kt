package com.example.cookpal.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cookpal.Models.FavModel
import com.example.cookpal.R

import com.example.cookpal.listeners.ClickedRecipeListener
import com.squareup.picasso.Picasso


class FavoritesAdapter(
    private val listener: ClickedRecipeListener
) : RecyclerView.Adapter<FavoritesAdapter.FavoritesViewHolder>() {

    private var favorites: List<FavModel> = listOf()

    fun setFavorites(list: ArrayList<FavModel>) {
        favorites = list.sortedByDescending { it.date } // Sort by date in descending order
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cooking_history, parent, false)
        return FavoritesViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoritesViewHolder, position: Int) {
        val favorite = favorites[position]
        holder.bind(favorite)
        holder.itemView.setOnClickListener {
            listener.onRecipeClicked(favorite.id.toString())
        }
    }

    override fun getItemCount(): Int = favorites.size

    inner class FavoritesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recipeImage: ImageView = itemView.findViewById(R.id.recipe_image)
        private val recipeName: TextView = itemView.findViewById(R.id.recipe_name)
        private val cookingDate: TextView = itemView.findViewById(R.id.cooking_date)

        fun bind(favorite: FavModel) {
            recipeName.text = favorite.name
            cookingDate.text = favorite.date
            Picasso.get().load(favorite.image).into(recipeImage)
        }
    }
}
