package com.example.appbanlaptop.Model

data class CartItem(
    val id: Int,
    val title: String,
    val details: String,
    val price: Double,
    val imageUrl: String?, // Thêm trường imageUrl
    var quantity: Int = 1,
    var isSelected: Boolean = false
)

// Singleton để lưu trữ danh sách cartItems toàn cục
object CartManager {
    private val cartItems = mutableListOf<CartItem>()

    fun getCartItems(): List<CartItem> = cartItems.toList()

    fun addCartItem(item: CartItem) {
        cartItems.add(item)
    }

    fun updateCartItem(updatedItem: CartItem) {
        val index = cartItems.indexOfFirst { it.id == updatedItem.id }
        if (index != -1) {
            cartItems[index] = updatedItem
        }
    }

    fun removeCartItem(itemId: Int) {
        cartItems.removeAll { it.id == itemId }
    }

    fun clearCart() {
        cartItems.clear()
    }
}
