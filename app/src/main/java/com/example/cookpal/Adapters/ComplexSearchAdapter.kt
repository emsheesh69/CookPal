package com.example.cookpal.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cookpal.Models.ComplexSearchApiResponse
import com.example.cookpal.R
import com.example.cookpal.RecipeDetails
import com.example.cookpal.listeners.ClickedRecipeListener
import com.squareup.picasso.Picasso

class ComplexSearchAdapter(
    private val context: Context,
    private val recipeList: List<ComplexSearchApiResponse.Recipe>,
    private val listener: ClickedRecipeListener
) : RecyclerView.Adapter<ComplexSearchAdapter.ComplexSearchViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComplexSearchViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_random_recipe, parent, false)
        return ComplexSearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComplexSearchViewHolder, position: Int) {
        val recipe = recipeList[position]
        holder.recipeTitle.text = recipe.title
        holder.recipeTitle.isSelected = true
        Picasso.get().load(recipe.image).into(holder.recipeImage)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, RecipeDetails::class.java).apply {
                putExtra("id", recipe.id.toString())
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return recipeList.size
    }

    inner class ComplexSearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recipeImage: ImageView = itemView.findViewById(R.id.imageView_food)
        val recipeTitle: TextView = itemView.findViewById(R.id.textView_title)
    }
}
