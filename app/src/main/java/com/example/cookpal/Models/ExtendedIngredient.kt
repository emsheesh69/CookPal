package com.example.cookpal.Models

class ExtendedIngredient(
    var id: Int = 0,
    var aisle: String? = null,
    var image: String? = null,
    var consistency: String? = null,
    var name: String? = null,
    var nameClean: String? = null,
    var original: String? = null,
    var originalName: String? = null,
    var amount: Double = 0.0,
    var unit: String? = null,
    var meta: ArrayList<String>? = null,
    var measures: Measures? = null,
) {
    // Additional constructor to set only name and original
    constructor(name: String?, original: String?) : this() {
        this.name = name
        this.original = original
    }
}
