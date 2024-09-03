package com.example.cookpal.listeners

import com.example.cookpal.Models.RecipeDetailsResponse

interface RecipeDetailsListener {
    fun didFetch(response: RecipeDetailsResponse, message: String)
    fun didError(message: String)
}
