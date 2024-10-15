package com.example.cookpal.listeners

import com.example.cookpal.Models.ComplexSearchApiResponse

interface ComplexSearchListener {
    fun didFetch(response: ComplexSearchApiResponse, message: String)
    fun didError(message: String)
}
