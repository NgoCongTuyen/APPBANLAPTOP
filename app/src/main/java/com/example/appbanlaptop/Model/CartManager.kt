package com.example.appbanlaptop.Model

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object CartManager {
    private val database = FirebaseDatabase.getInstance()
    private var userId: String? = null
    private val cartItems = mutableListOf<CartItem>()
    private var valueEventListener: ValueEventListener? = null
    private val _cartItemsFlow = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItemsFlow: StateFlow<List<CartItem>> = _cartItemsFlow

    private val cartRef
        get() = userId?.let { database.getReference("Cart").child(it).child("items") }

    fun setUserId(id: String?) {
        userId = id
        setupListener()
    }

    fun getCartItems(): List<CartItem> {
        return cartItems.toList()
    }

    fun addCartItem(item: CartItem) {
        cartRef?.let { ref ->
            Log.d("CartManager", "Adding cart item: $item")
            val newRef = ref.push()
            val newItem = item.copy(firebaseKey = newRef.key)
            newRef.setValue(newItem)
                .addOnSuccessListener {
                    Log.d("CartManager", "Successfully added item: $newItem with key: ${newRef.key}")
                    // Cập nhật danh sách cục bộ ngay lập tức
                    cartItems.add(newItem)
                    _cartItemsFlow.value = cartItems.toList()
                }
                .addOnFailureListener {
                    Log.e("CartManager", "Failed to add cart item: ${it.message}")
                }
        } ?: run {
            Log.e("CartManager", "User not logged in, cannot add to cart")
        }
    }

    fun updateCartItem(item: CartItem) {
        item.firebaseKey?.let { key ->
            cartRef?.child(key)?.setValue(item)
                ?.addOnSuccessListener {
                    Log.d("CartManager", "Successfully updated item: $item")
                    // Cập nhật danh sách cục bộ ngay lập tức
                    val index = cartItems.indexOfFirst { it.firebaseKey == key }
                    if (index != -1) {
                        cartItems[index] = item
                        _cartItemsFlow.value = cartItems.toList()
                        Log.d("CartManager", "Updated cartItemsFlow after update: ${_cartItemsFlow.value}")
                    }
                }
                ?.addOnFailureListener {
                    Log.e("CartManager", "Failed to update cart item: ${it.message}")
                }
        } ?: Log.e("CartManager", "Cannot update item: firebaseKey is null")
    }

    fun removeCartItem(firebaseKey: String?) {
        firebaseKey?.let { key ->
            cartRef?.child(key)?.removeValue()
                ?.addOnSuccessListener {
                    Log.d("CartManager", "Successfully removed item with key: $key")
                    // Cập nhật danh sách cục bộ ngay lập tức
                    cartItems.removeAll { it.firebaseKey == key }
                    _cartItemsFlow.value = cartItems.toList()
                }
                ?.addOnFailureListener {
                    Log.e("CartManager", "Failed to remove cart item: ${it.message}")
                }
        }
    }

    fun cleanup() {
        valueEventListener?.let {
            cartRef?.removeEventListener(it)
            valueEventListener = null
            Log.d("CartManager", "Cleaned up Firebase listener")
        }
    }

    private fun setupListener() {
        cleanup() // Xóa listener cũ trước khi tạo mới
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("CartManager", "Data changed, snapshot: $snapshot")
                cartItems.clear()
                if (!snapshot.exists() || snapshot.childrenCount.toInt() == 0) {
                    Log.d("CartManager", "Cart is empty")
                    _cartItemsFlow.value = emptyList()
                    return
                }
                for (item in snapshot.children) {
                    try {
                        val cartItem = item.getValue(CartItem::class.java)
                        if (cartItem != null) {
                            val key = item.key ?: "unknown"
                            val updatedItem = cartItem.copy(firebaseKey = key)
                            cartItems.add(updatedItem)
                            Log.d("CartManager", "Added item: $updatedItem")
                        }
                    } catch (e: Exception) {
                        Log.e("CartManager", "Error parsing cart item: ${item.key}, error: ${e.message}")
                    }
                }
                _cartItemsFlow.value = cartItems.toList()
                Log.d("CartManager", "Updated cartItemsFlow: ${_cartItemsFlow.value}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CartManager", "Database error: ${error.message}")
                _cartItemsFlow.value = emptyList()
            }
        }
        valueEventListener?.let {
            cartRef?.addValueEventListener(it)
            Log.d("CartManager", "Firebase listener set up for user: $userId")
        }
    }
}