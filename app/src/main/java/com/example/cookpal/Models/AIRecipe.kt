package com.example.cookpal.Models


data class AIRecipe(
    val title: String,
    val summary: String,
    val image: String?,
    val ingredients: List<String>,
    val instructions: List<String>
)
