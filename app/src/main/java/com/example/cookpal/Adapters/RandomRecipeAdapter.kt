
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


class RandomRecipeAdapter(private val context: Context, private val recipeList: List<Recipe>) :
    RecyclerView.Adapter<RandomRecipeAdapter.RandomRecipeViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RandomRecipeViewHolder {
        val view: View =
            LayoutInflater.from(context).inflate(R.layout.list_random_recipe, parent, false)
        return RandomRecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RandomRecipeViewHolder, position: Int) {
        val recipe = recipeList[position]
        holder.textViewTitle.text = recipe.title
        holder.textViewTitle.isSelected = true
        holder.textViewLikes.text = recipe.aggregateLikes.toString() + " likes"
        holder.textViewServings.text = recipe.servings.toString() + " servings"
        holder.textViewTime.text = recipe.readyInMinutes.toString() + " mins"
        Picasso.get().load(recipe.image).into(holder.imageViewFood)
    }

    override fun getItemCount(): Int {
        return recipeList.size
    }

    class RandomRecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textViewTitle: TextView = itemView.findViewById<TextView>(R.id.textView_title)
        var textViewServings: TextView = itemView.findViewById<TextView>(R.id.textView_servings)
        var textViewLikes: TextView = itemView.findViewById<TextView>(R.id.textView_likes)
        var textViewTime: TextView = itemView.findViewById<TextView>(R.id.textView_time)
        var imageViewFood: ImageView = itemView.findViewById<ImageView>(R.id.imageView_food)
    }
}