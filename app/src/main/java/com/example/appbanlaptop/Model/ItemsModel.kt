package com.example.appbanlaptop.Model

data class ProductItem(
    val id: String? = null, // Thêm trường id
    val categoryId: String? = null,
    val description: String? = null,
    val model: List<String>? = null,
    val picUrl: List<String>? = null,
    val price: String? = null,
    val rating: Double? = null,
    val showRecommended: Boolean = false,
    val title: String? = null
)

