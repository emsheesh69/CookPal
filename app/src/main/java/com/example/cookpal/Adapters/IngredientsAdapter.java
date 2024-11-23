package com.example.cookpal.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookpal.Models.ExtendedIngredient;
import com.example.cookpal.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class IngredientsAdapter extends RecyclerView.Adapter<IngredientsViewHolder> {

    Context context;
    List<ExtendedIngredient> list;
    OnIngredientClickListener listener;

    // Constructor with listener
    public IngredientsAdapter(Context context, List<ExtendedIngredient> list, OnIngredientClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public IngredientsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new IngredientsViewHolder(LayoutInflater.from(context).inflate(R.layout.meal_ingredient, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientsViewHolder holder, int position) {
        ExtendedIngredient ingredient = list.get(position);

        holder.textview_ingredients_name.setText(ingredient.getName());
        holder.textview_ingredients_name.setSelected(true);
        holder.textview_ingredients_quantity.setText(ingredient.getOriginal());
        holder.textview_ingredients_quantity.setSelected(true);

        // Uncomment if using ingredient images
        // Picasso.get().load("https://spoonacular.com/cdn/ingredients_100x100/" + ingredient.getImage()).into(holder.imageview_ingredients);

        // Handle click event
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onIngredientClick(ingredient.getName()); // Pass the ingredient name
            }
        });
    }

    private void showSubstituteDialog(String ingredientName, List<String> substitutes) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle("Substitutes for " + ingredientName);

        StringBuilder substituteText = new StringBuilder();
        for (String substitute : substitutes) {
            substituteText.append(substitute).append("\n");
        }

        dialogBuilder.setMessage(substituteText.toString());

        dialogBuilder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());  // Dismiss the dialog on OK

        dialogBuilder.create().show();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // Interface for click handling
    public interface OnIngredientClickListener {
        void onIngredientClick(String ingredientName);
    }
}

class IngredientsViewHolder extends RecyclerView.ViewHolder {
    TextView textview_ingredients_quantity, textview_ingredients_name;
    ImageView imageview_ingredients;

    public IngredientsViewHolder(@NonNull View itemView) {
        super(itemView);
        textview_ingredients_quantity = itemView.findViewById(R.id.textview_ingredients_quantity);
        textview_ingredients_name = itemView.findViewById(R.id.textview_ingredients_name);
        // Uncomment if using ingredient images
        // imageview_ingredients = itemView.findViewById(R.id.imageview_ingredients);
    }
}
