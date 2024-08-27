import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cookpal.Models.Recipe
import com.example.cookpal.R
import com.squareup.picasso.Picasso

class RandomRecipeAdapter(
    private val context: Context,
    private val recipeList: List<Recipe>,
    private val tags: List<String> // Add tags as a parameter
) : RecyclerView.Adapter<RandomRecipeAdapter.RandomRecipeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RandomRecipeViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.list_random_recipe, parent, false)
        return RandomRecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RandomRecipeViewHolder, position: Int) {
        val recipe = recipeList[position]
        holder.textViewTitle.text = recipe.title
        holder.textViewTitle.isSelected = true
        Picasso.get().load(recipe.image).into(holder.imageViewFood)

        // Example usage of tags; adjust according to your needs
        holder.textViewTags.text = tags.joinToString(", ")
    }

    override fun getItemCount(): Int {
        return recipeList.size
    }

    class RandomRecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textViewTitle: TextView = itemView.findViewById(R.id.textView_title)
        var imageViewFood: ImageView = itemView.findViewById(R.id.imageView_food)
        var textViewTags: TextView = itemView.findViewById(R.id.textView_tags) // Ensure this view exists in your layout
    }
}
