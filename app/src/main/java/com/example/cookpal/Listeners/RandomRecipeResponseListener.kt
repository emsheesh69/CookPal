package com.example.cookpal.listeners

import com.example.cookpal.Models.RandomRecipeApiResponse

interface RandomRecipeResponseListener {
    fun didFetch(response: RandomRecipeApiResponse, message: String)
    fun didError(message: String)
}
