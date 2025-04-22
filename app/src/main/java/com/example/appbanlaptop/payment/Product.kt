package com.example.appbanlaptop.payment

import android.os.Parcel
import android.os.Parcelable
import com.example.appbanlaptop.Model.CartItem
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    val name: String,
    val color: String,
    val price: String,
    val quantity: Int,
    val imageUrl: String? = null
) : Parcelable {
    companion object {
        fun fromCartItem(cartItem: CartItem): Product {
            return Product(
                name = cartItem.title ?: "Unknown Item",
                color = "Default Color",
                price = cartItem.price.toInt().toString(),
                quantity = cartItem.quantity,
                imageUrl = cartItem.imageUrl
            )
        }
    }
}