package com.example.acerestaurant.data.model

/** Root JSON model for the menu payload. */
data class Menu(val categories: List<Category>, val items: List<MenuItem>)