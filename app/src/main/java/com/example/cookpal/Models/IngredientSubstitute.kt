package com.example.cookpal.Models

data class IngredientSubstitution(
    val status: String?,
    val ingredient: String?,
    val substitutes: List<String>?,
    val message: String?
)
