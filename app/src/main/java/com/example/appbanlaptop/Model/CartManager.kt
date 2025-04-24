package com.example.appbanlaptop.Model

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

object CartManager {
    private val database = FirebaseDatabase.getInstance()
    private var userId: String? = null
    private val cartItems = mutableListOf<CartItem>()
    private var valueEventListener: ValueEventListener? = null
    private val _cartItemsFlow = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItemsFlow: StateFlow<List<CartItem>> = _cartItemsFlow
    private var isAddingItem = false
    private var lastAddedTimestamp = System.currentTimeMillis()

    private val cartRef
        get() = userId?.let { database.getReference("Cart").child(it).child("items") }

    fun setUserId(id: String?) {
        userId = id
        setupListener()
    }

    fun getCartItems(): List<CartItem> {
        return cartItems.toList()
    }

    fun addCartItem(
        item: CartItem, 
        onError: (String) -> Unit = {}, 
        onDuplicate: (String) -> Unit = {},
        onSuccess: (String) -> Unit = {}
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e("CartManager", "User not logged in, cannot add to cart")
            onError("Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng")
            return
        }

        if (userId == null) {
            userId = currentUser.uid
            setupListener()
        }

        cartRef?.let { ref ->
            Log.d("CartManager", "Adding cart item: $item")
            
            // Kiểm tra xem sản phẩm đã tồn tại trong giỏ hàng chưa
            val existingItem = cartItems.find { it.title == item.title }
            if (existingItem != null) {
                // Nếu sản phẩm đã tồn tại, thông báo và đẩy lên đầu danh sách
                onDuplicate("Sản phẩm đã có trong giỏ hàng")
                cartItems.remove(existingItem)
                lastAddedTimestamp = System.currentTimeMillis()
                val updatedItem = existingItem.copy(timestamp = lastAddedTimestamp)
                cartItems.add(0, updatedItem)
                _cartItemsFlow.value = cartItems.toList()
                
                // Cập nhật lên Firebase
                existingItem.firebaseKey?.let { key ->
                    cartRef?.child(key)?.setValue(updatedItem)
                }
                return
            }

            val newRef = ref.push()
            lastAddedTimestamp = System.currentTimeMillis()
            val newItem = item.copy(firebaseKey = newRef.key, timestamp = lastAddedTimestamp)

            isAddingItem = true
            cartItems.add(0, newItem)
            _cartItemsFlow.value = cartItems.toList()
            Log.d("CartManager", "Updated cartItemsFlow locally: ${_cartItemsFlow.value}")

            newRef.setValue(newItem)
                .addOnSuccessListener {
                    Log.d("CartManager", "Successfully added item: $newItem with key: ${newRef.key}")
                    isAddingItem = false
                    onSuccess("Đã thêm ${item.title} vào giỏ hàng")
                }
                .addOnFailureListener {
                    Log.e("CartManager", "Failed to add cart item: ${it.message}")
                    cartItems.remove(newItem)
                    _cartItemsFlow.value = cartItems.toList()
                    isAddingItem = false
                    onError("Không thể thêm sản phẩm vào giỏ hàng: ${it.message}")
                }
        } ?: run {
            Log.e("CartManager", "Cart reference is null, cannot add to cart")
            onError("Lỗi hệ thống, vui lòng thử lại")
        }
    }

    fun updateCartItem(item: CartItem) {
        item.firebaseKey?.let { key ->
            cartRef?.child(key)?.setValue(item)
                ?.addOnSuccessListener {
                    Log.d("CartManager", "Successfully updated item: $item")
                    val index = cartItems.indexOfFirst { it.firebaseKey == key }
                    if (index != -1) {
                        cartItems[index] = item
                        _cartItemsFlow.value = cartItems.toList()
                        Log.d("CartManager", "Updated cartItemsFlow after update: ${_cartItemsFlow.value}")
                    } else {
                        Log.w("CartManager", "Item with key $key not found in local list")
                    }
                }
                ?.addOnFailureListener {
                    Log.e("CartManager", "Failed to update cart item: ${it.message}")
                    _cartItemsFlow.value = cartItems.toList()
                }
        } ?: Log.e("CartManager", "Cannot update item: firebaseKey is null")
    }

    fun removeCartItem(firebaseKey: String?) {
        firebaseKey?.let { key ->
            cartRef?.child(key)?.removeValue()
                ?.addOnSuccessListener {
                    Log.d("CartManager", "Successfully removed item with key: $key")
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
        cleanup()
        cartRef?.let { ref ->
            ref.get().addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    Log.d("CartManager", "Cart node does not exist, initializing empty node")
                    ref.setValue(emptyMap<String, Any>())
                }
            }

            valueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("CartManager", "Data changed, snapshot: $snapshot")
                    if (!snapshot.exists() || snapshot.childrenCount.toInt() == 0) {
                        Log.d("CartManager", "Cart is empty")
                        cartItems.clear()
                        _cartItemsFlow.value = emptyList()
                        return
                    }

                    val updatedItems = mutableListOf<CartItem>()
                    for (item in snapshot.children) {
                        try {
                            val cartItem = item.getValue(CartItem::class.java)
                            if (cartItem != null) {
                                val key = item.key ?: "unknown"
                                val updatedItem = cartItem.copy(firebaseKey = key)
                                updatedItems.add(updatedItem)
                                Log.d("CartManager", "Added item: $updatedItem")
                            }
                        } catch (e: Exception) {
                            Log.e("CartManager", "Error parsing cart item: ${item.key}, error: ${e.message}")
                        }
                    }

                    // Sắp xếp items theo timestamp giảm dần (mới nhất lên đầu)
                    val sortedItems = updatedItems.sortedByDescending { it.timestamp }
                    
                    val currentItemsMap = cartItems.associateBy { it.firebaseKey }
                    cartItems.clear()
                    
                    sortedItems.forEach { newItem ->
                        val existingItem = currentItemsMap[newItem.firebaseKey]
                        if (existingItem != null) {
                            cartItems.add(newItem.copy(isSelected = existingItem.isSelected))
                        } else {
                            cartItems.add(newItem)
                        }
                    }
                    
                    _cartItemsFlow.value = cartItems.toList()
                    Log.d("CartManager", "Updated cartItemsFlow: ${_cartItemsFlow.value}")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("CartManager", "Database error: ${error.message}")
                    _cartItemsFlow.value = cartItems.toList()
                }
            }
            valueEventListener?.let {
                ref.addValueEventListener(it)
                Log.d("CartManager", "Firebase listener set up for user: $userId")
            }
        }
    }
}