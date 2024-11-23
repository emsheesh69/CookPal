package com.example.cookpal.Listeners;

import com.example.cookpal.Models.IngredientSubstitution;

public interface IngredientSubstituteListener {
    void didFetch(IngredientSubstitution response, String message);
    void didError(String message);
}