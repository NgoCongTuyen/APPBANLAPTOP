package com.example.appbanlaptop.Model



data class ProductItem(
    val categoryId: String? = null, // Đổi thành "categoryId" để khớp với JSON
    val description: String? = null,
    val model: List<String>? = null,
    val picUrl: List<String>? = null,
    val price: String? = null,
    val rating: Double? = null,
    val showRecommended: Boolean = false,
    val title: String? = null
)

