package com.example.appbanlaptop.Model



data class ProductItem(
    val categoryID: String? = null,
    val description: String? = null,
    val model: List<String>? = null,
    val picUrl: List<String>? = null,
    val price: Long? = null,
    val rating: Double? = null,
    val showRecommended: Boolean = false,
    val title: String? = null
)

