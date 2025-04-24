    package com.example.appbanlaptop.Model

    import android.util.Log
    import com.google.firebase.auth.FirebaseAuth
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
        private var isAddingItem = false // Theo dõi trạng thái thêm sản phẩm

        private val cartRef
            get() = userId?.let { database.getReference("Cart").child(it).child("items") }

        fun setUserId(id: String?) {
            userId = id
            setupListener()
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
                }
        } ?: Log.e("CartManager", "Cannot update item: firebaseKey is null")
    }

        fun addCartItem(item: CartItem, onError: (String) -> Unit = {}) {
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
                val newRef = ref.push()
                val newItem = item.copy(firebaseKey = newRef.key)

                // Đánh dấu đang thêm sản phẩm
                isAddingItem = true
                // Cập nhật danh sách cục bộ
                cartItems.add(newItem)
                _cartItemsFlow.value = cartItems.toList()
                Log.d("CartManager", "Updated cartItemsFlow locally: ${_cartItemsFlow.value}")

                // Đẩy lên Firebase
                newRef.setValue(newItem)
                    .addOnSuccessListener {
                        Log.d("CartManager", "Successfully added item: $newItem with key: ${newRef.key}")
                        isAddingItem = false
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
                        }
                    }
                    ?.addOnFailureListener {
                        Log.e("CartManager", "Failed to update cart item: ${it.message}")
                    }
            }
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
                        // Bỏ qua snapshot rỗng nếu đang thêm sản phẩm hoặc danh sách cục bộ không rỗng
                        if ((isAddingItem || cartItems.isNotEmpty()) && (!snapshot.exists() || snapshot.childrenCount.toInt() == 0)) {
                            Log.d("CartManager", "Ignoring empty snapshot while adding item or cartItems not empty")
                            return
                        }

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
                        // Giữ danh sách cục bộ nếu có lỗi
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