package com.example.appbanlaptop.Model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CartItem(
    val id: Int = 0,
    val title: String = "",
    val details: String = "",
    val price: Double = 0.0,
    val imageUrl: String? = null,
    val quantity: Int = 1,
    val isSelected: Boolean = false,
    val firebaseKey: String? = null,
    val maxStock: Int = 100 // Thêm thuộc tính maxStock, mặc định là 100
) : Parcelable {

}