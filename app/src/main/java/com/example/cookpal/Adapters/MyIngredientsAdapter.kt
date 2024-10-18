import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cookpal.R

class MyIngredientsAdapter(
    private val ingredients: List<String>,
    private val onEditClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<MyIngredientsAdapter.IngredientViewHolder>() {

    class IngredientViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewIngredient: TextView = view.findViewById(R.id.textViewIngredient)
        val buttonEdit: Button = view.findViewById(R.id.buttonEditIngredient)
        val buttonDelete: Button = view.findViewById(R.id.buttonDeleteIngredient)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingredient, parent, false)
        return IngredientViewHolder(view)
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        val ingredient = ingredients[position]
        holder.textViewIngredient.text = ingredient
        holder.buttonEdit.setOnClickListener { onEditClick(position) }
        holder.buttonDelete.setOnClickListener { onDeleteClick(position) }
    }

    override fun getItemCount(): Int = ingredients.size
}
