import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cookpal.Models.Recipe
import com.example.cookpal.R
import com.example.cookpal.RecipeDetails
import com.example.cookpal.listeners.ClickedRecipeListener
import com.squareup.picasso.Picasso

class RandomRecipeAdapter(
    private val context: Context,
    private val recipeList: List<Recipe>,
    private val tags: List<String>,
    private val listener: ClickedRecipeListener
) : RecyclerView.Adapter<RandomRecipeAdapter.RandomRecipeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RandomRecipeViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_random_recipe, parent, false)
        return RandomRecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RandomRecipeViewHolder, position: Int) {
        val recipe = recipeList[position]
        holder.textViewTitle.text = recipe.title
        holder.textViewTitle.isSelected = true
        Picasso.get().load(recipe.image).into(holder.imageViewFood)

        holder.textViewTags.text = tags.joinToString(", ")

        holder.itemView.setOnClickListener {
            val intent = Intent(context, RecipeDetails::class.java).apply {
                putExtra("id", recipe.id.toString())
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = recipeList.size

    class RandomRecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewTitle: TextView = itemView.findViewById(R.id.textView_title)
        val imageViewFood: ImageView = itemView.findViewById(R.id.imageView_food)
        val textViewTags: TextView = itemView.findViewById(R.id.textView_tags)
    }
}
