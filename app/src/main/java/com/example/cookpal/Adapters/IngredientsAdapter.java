package com.example.cookpal.Adapters;

import android.annotation.SuppressLint;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class IngredientsAdapter extends RecyclerView.Adapter<IngredientsViewHolder> {

    private final Context context;
    private List<ExtendedIngredient> ingredients;
    private final OnIngredientClickListener listener;

    public IngredientsAdapter(Context context, List<ExtendedIngredient> ingredients, OnIngredientClickListener listener) {
        this.context = context;
        this.ingredients = ingredients;
        this.listener = listener;
    }

    @NonNull
    @Override
    public IngredientsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new IngredientsViewHolder(LayoutInflater.from(context).inflate(R.layout.meal_ingredient, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientsViewHolder holder, int position) {
        ExtendedIngredient ingredient = ingredients.get(position);

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
        dialogBuilder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        dialogBuilder.create().show();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateIngredient(String original, String substitute) {
        List<ExtendedIngredient> updatedList = new ArrayList<>();
        boolean ingredientUpdated = false;

        for (ExtendedIngredient ingredient : ingredients) {
            if (Objects.requireNonNull(ingredient.getName()).equalsIgnoreCase(original)) {
                if (substitute == null || substitute.isEmpty()) {
                    ingredient.setName(ingredient.getOriginal());
                } else {
                    ingredient.setName(substitute);
                }
                updatedList.add(ingredient);
                ingredientUpdated = true;
            } else {
                updatedList.add(ingredient);
            }
        }

        if (ingredientUpdated) {
            this.ingredients = updatedList;
            notifyDataSetChanged();
        }
    }

    public List<ExtendedIngredient> getIngredients() {
        return new ArrayList<>(ingredients);
    }

    public void updateIngredients(List<ExtendedIngredient> updatedIngredients) {
        this.ingredients = updatedIngredients;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return ingredients.size();
    }

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
