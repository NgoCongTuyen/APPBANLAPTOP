package com.example.appbanlaptop.Model

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Model cho User
data class User(
    val uid: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val role: String? = "user",
    val createdAt: Long? = null
)

// Model cho Category
data class Category(
    val id: Int? = null,
    val title: String? = null,
    val picUrl: String? = null,
    val imageUrl: String? = null
)

// Model cho Order
data class OrderItem(
    val title: String? = null,
    val price: Double? = 0.0,
    val quantity: Int? = 0,
    val imageUrl: String?
)

data class Order(
    val id: String? = null,
    val userId: String? = null,
    val items: List<OrderItem>? = emptyList(),
    val totalPrice: Double? = 0.0,
    val status: String? = "pending",
    val createdAt: Long? = null,
    val shippingAddress: String? = null,
    val recipientName: String? = null,
    val recipientPhone: String? = null
)

// Manager cho User
object UserManager {
    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("users")
    private val users = mutableListOf<User>()
    private val _usersFlow = MutableStateFlow<List<User>>(emptyList())
    val usersFlow: StateFlow<List<User>> = _usersFlow
    private var valueEventListener: ValueEventListener? = null

    init {
        setupListener()
    }

    fun addUser(user: User, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (user.uid == null) {
            onError("User UID is null")
            return
        }
        usersRef.child(user.uid).setValue(user)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError(it.message ?: "Unknown error")
            }
    }

    fun updateUser(user: User, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (user.uid == null) {
            onError("User UID is null")
            return
        }
        usersRef.child(user.uid).setValue(user)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError(it.message ?: "Unknown error")
            }
    }

    fun removeUser(userId: String?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (userId == null) {
            onError("User ID is null")
            return
        }
        usersRef.child(userId).removeValue()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError(it.message ?: "Unknown error")
            }
    }

    private fun setupListener() {
        valueEventListener?.let { usersRef.removeEventListener(it) }
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                users.clear()
                for (item in snapshot.children) {
                    try {
                        val uid = item.key
                        val displayName = item.child("displayName").getValue(String::class.java)
                        val email = item.child("email").getValue(String::class.java)
                        val role = item.child("role").getValue(String::class.java) ?: "user"
                        val createdAt = item.child("createdAt").getValue(Long::class.java)
                        val user = User(uid, displayName, email, role, createdAt)
                        users.add(user)
                    } catch (e: Exception) {
                        Log.e("UserManager", "Error parsing user: ${item.key}, error: ${e.message}")
                    }
                }
                _usersFlow.value = users.toList()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserManager", "Database error: ${error.message}")
            }
        }
        valueEventListener?.let { usersRef.addValueEventListener(it) }
    }

    fun cleanup() {
        valueEventListener?.let {
            usersRef.removeEventListener(it)
            valueEventListener = null
        }
    }
}

// Manager cho Category
object CategoryManager {
    private val database = FirebaseDatabase.getInstance()
    private val categoriesRef = database.getReference("Category")
    private val categories = mutableListOf<Category>()
    private val _categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    val categoriesFlow: StateFlow<List<Category>> = _categoriesFlow
    private var valueEventListener: ValueEventListener? = null

    init {
        setupListener()
    }

    fun addCategory(category: Category, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val newRef = categoriesRef.push()
        val categoryWithId = category.copy(id = categories.size)
        newRef.setValue(categoryWithId)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError(it.message ?: "Unknown error")
            }
    }

    fun updateCategory(category: Category, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (category.id == null) {
            onError("Category ID is null")
            return
        }
        categoriesRef.child(category.id.toString()).setValue(category)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError(it.message ?: "Unknown error")
            }
    }

    fun removeCategory(categoryId: Int?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (categoryId == null) {
            onError("Category ID is null")
            return
        }
        categoriesRef.child(categoryId.toString()).removeValue()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError(it.message ?: "Unknown error")
            }
    }

    private fun setupListener() {
        valueEventListener?.let { categoriesRef.removeEventListener(it) }
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categories.clear()
                for (item in snapshot.children) {
                    try {
                        val id = item.child("id").getValue(Int::class.java)
                        val title = item.child("title").getValue(String::class.java)
                        val picUrl = item.child("picUrl").getValue(String::class.java)
                        val category = Category(id, title, picUrl)
                        categories.add(category)
                    } catch (e: Exception) {
                        Log.e("CategoryManager", "Error parsing category: ${item.key}, error: ${e.message}")
                    }
                }
                _categoriesFlow.value = categories.toList()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CategoryManager", "Database error: ${error.message}")
            }
        }
        valueEventListener?.let { categoriesRef.addValueEventListener(it) }
    }

    fun cleanup() {
        valueEventListener?.let {
            categoriesRef.removeEventListener(it)
            valueEventListener = null
        }
    }
}

// Manager cho Order
object OrderManager {
    private val database = FirebaseDatabase.getInstance()
    private val ordersRef = database.getReference("orders")
    private val orders = mutableListOf<Order>()
    private val _ordersFlow = MutableStateFlow<List<Order>>(emptyList())
    val ordersFlow: StateFlow<List<Order>> = _ordersFlow
    private var valueEventListener: ValueEventListener? = null

    init {
        setupListener()
    }

    private fun setupListener() {
        Log.d("OrderManager", "Setting up Firebase listener for orders")
        valueEventListener?.let { ordersRef.removeEventListener(it) }
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("OrderManager", "Data snapshot received: $snapshot")
                orders.clear()
                if (!snapshot.exists() || snapshot.childrenCount.toInt() == 0) {
                    Log.d("OrderManager", "No orders found in database")
                    _ordersFlow.value = emptyList()
                    return
                }

                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    Log.d("OrderManager", "Processing orders for user: $userId")
                    for (orderSnapshot in userSnapshot.children) {
                        try {
                            val orderId = orderSnapshot.key
                            Log.d("OrderManager", "Processing order: $orderId")
                            val totalPrice = orderSnapshot.child("totalPrice").getValue(Double::class.java)
                            val status = orderSnapshot.child("status").getValue(String::class.java)
                            val createdAt = orderSnapshot.child("createdAt").getValue(Long::class.java)
                            val addressMap = orderSnapshot.child("address").value as? Map<String, Any>
                            val itemsSnapshot = orderSnapshot.child("products")
                            val itemsList = mutableListOf<OrderItem>()
                            for (item in itemsSnapshot.children) {
                                val name = item.child("name").getValue(String::class.java)
                                val price = item.child("price").getValue(Double::class.java)
                                val quantity = item.child("quantity").getValue(Long::class.java)?.toInt()
                                val imageUrl = item.child("imageUrl").getValue(String::class.java)
                                itemsList.add(OrderItem(name, price, quantity, imageUrl))
                            }
                            val order = Order(
                                id = orderId,
                                userId = userId,
                                items = itemsList,
                                totalPrice = totalPrice,
                                status = status,
                                createdAt = createdAt,
                                shippingAddress = addressMap?.get("addressDetail") as? String,
                                recipientName = addressMap?.get("name") as? String,
                                recipientPhone = addressMap?.get("phone") as? String
                            )
                            orders.add(order)
                            Log.d("OrderManager", "Added order: $order")
                        } catch (e: Exception) {
                            Log.e("OrderManager", "Error parsing order: ${orderSnapshot.key}, error: ${e.message}")
                        }
                    }
                }
                orders.sortByDescending { it.createdAt }
                _ordersFlow.value = orders.toList()
                Log.d("OrderManager", "Updated ordersFlow: ${_ordersFlow.value}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("OrderManager", "Database error: ${error.message}, details: ${error.details}")
                _ordersFlow.value = orders.toList()
            }
        }
        valueEventListener?.let {
            ordersRef.addValueEventListener(it)
            Log.d("OrderManager", "Firebase listener set up successfully")
        }
    }

    fun updateOrderStatus(
        userId: String,
        orderId: String,
        newStatus: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        ordersRef.child(userId).child(orderId).child("status").setValue(newStatus)
            .addOnSuccessListener {
                Log.d("OrderManager", "Successfully updated status for order $orderId to $newStatus")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e("OrderManager", "Failed to update status for order $orderId: ${it.message}")
                onError(it.message ?: "Unknown error")
            }
    }

    fun cleanup() {
        valueEventListener?.let {
            ordersRef.removeEventListener(it)
            valueEventListener = null
            Log.d("OrderManager", "Cleaned up Firebase listener")
        }
    }
}