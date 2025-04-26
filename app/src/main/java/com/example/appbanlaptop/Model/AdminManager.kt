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


data class Product(
    val id: String? = null,
    val title: String? = null,
    val price: Double = 0.0,
    val description: String? = null,
    val picUrl: List<String>? = emptyList(),
    val categoryId: String? = null,
    val rating: Double? = 0.0,
    val showRecommended: Boolean? = false,
    val model: List<String>? = emptyList()
)

object ProductManager {
    private val database = FirebaseDatabase.getInstance()
    private val productsRef = database.getReference("Items")
    private val products = mutableListOf<Product>()
    private val _productsFlow = MutableStateFlow<List<Product>>(emptyList())
    val productsFlow: StateFlow<List<Product>> = _productsFlow
    private var valueEventListener: ValueEventListener? = null

    init {
        setupListener()
    }

    fun addProduct(product: Product, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val newRef = productsRef.push()
        val productWithId = product.copy(id = newRef.key)

        newRef.setValue(productWithId)
            .addOnSuccessListener {
                Log.d("ProductManager", "Thêm sản phẩm thành công: ${productWithId.title}")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e("ProductManager", "Lỗi khi thêm sản phẩm: ${it.message}")
                onError(it.message ?: "Lỗi không xác định")
            }
    }

    fun updateProduct(product: Product, onSuccess: () -> Unit, onError: (String) -> Unit) {
        product.id?.let { id ->
            productsRef.child(id).setValue(product)
                .addOnSuccessListener {
                    Log.d("ProductManager", "Cập nhật sản phẩm thành công: ${product.title}")
                    onSuccess()
                }
                .addOnFailureListener {
                    Log.e("ProductManager", "Lỗi khi cập nhật sản phẩm: ${it.message}")
                    onError(it.message ?: "Lỗi không xác định")
                }
        } ?: run {
            Log.e("ProductManager", "ID sản phẩm là null")
            onError("ID sản phẩm là null")
        }
    }

    fun removeProduct(productId: String?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        productId?.let { id ->
            productsRef.child(id).removeValue()
                .addOnSuccessListener {
                    Log.d("ProductManager", "Xóa sản phẩm thành công với ID: $id")
                    onSuccess()
                }
                .addOnFailureListener {
                    Log.e("ProductManager", "Lỗi khi xóa sản phẩm: ${it.message}")
                    onError(it.message ?: "Lỗi không xác định")
                }
        } ?: run {
            Log.e("ProductManager", "ID sản phẩm là null")
            onError("ID sản phẩm là null")
        }
    }

    fun cleanup() {
        valueEventListener?.let {
            productsRef.removeEventListener(it)
            valueEventListener = null
            Log.d("ProductManager", "Đã dọn dẹp listener Firebase")
        }
    }

    private fun setupListener() {
        cleanup() // Dọn dẹp listener cũ trước khi tạo mới
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("ProductManager", "Nhận snapshot dữ liệu: exists=${snapshot.exists()}, childrenCount=${snapshot.childrenCount}")
                products.clear()
                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    Log.d("ProductManager", "Không tìm thấy sản phẩm trong cơ sở dữ liệu")
                    _productsFlow.value = emptyList()
                    return
                }

                for (item in snapshot.children) {
                    try {
                        val product = item.getValue(Product::class.java)?.copy(id = item.key)
                        if (product != null) {
                            products.add(product)
                            Log.d("ProductManager", "Thêm sản phẩm: id=${product.id}, title=${product.title}")
                        } else {
                            Log.w("ProductManager", "Dữ liệu sản phẩm không hợp lệ tại key: ${item.key}")
                        }
                    } catch (e: Exception) {
                        Log.e("ProductManager", "Lỗi khi phân tích sản phẩm tại key: ${item.key}, lỗi: ${e.message}")
                    }
                }
                _productsFlow.value = products.toList()
                Log.d("ProductManager", "Cập nhật productsFlow: ${products.size} sản phẩm")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ProductManager", "Lỗi cơ sở dữ liệu: ${error.message}, chi tiết: ${error.details}")
                _productsFlow.value = emptyList() // Thông báo trạng thái lỗi bằng danh sách rỗng
            }
        }
        valueEventListener?.let {
            productsRef.addValueEventListener(it)
            Log.d("ProductManager", "Thiết lập listener Firebase thành công")
        }
    }
}

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
            Log.e("UserManager", "User UID is null")
            onError("User UID is null")
            return
        }
        usersRef.child(user.uid).setValue(user)
            .addOnSuccessListener {
                Log.d("UserManager", "Successfully added user: ${user.uid}")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e("UserManager", "Failed to add user: ${it.message}")
                onError(it.message ?: "Unknown error")
            }
    }

    fun updateUser(user: User, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (user.uid == null) {
            Log.e("UserManager", "User UID is null")
            onError("User UID is null")
            return
        }
        usersRef.child(user.uid).setValue(user)
            .addOnSuccessListener {
                Log.d("UserManager", "Successfully updated user: ${user.uid}")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e("UserManager", "Failed to update user: ${it.message}")
                onError(it.message ?: "Unknown error")
            }
    }

    fun removeUser(userId: String?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (userId == null) {
            Log.e("UserManager", "User ID is null")
            onError("User ID is null")
            return
        }
        usersRef.child(userId).removeValue()
            .addOnSuccessListener {
                Log.d("UserManager", "Successfully removed user: $userId")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e("UserManager", "Failed to remove user: ${it.message}")
                onError(it.message ?: "Unknown error")
            }
    }

    private fun setupListener() {
        Log.d("UserManager", "Setting up Firebase listener for users")
        valueEventListener?.let { usersRef.removeEventListener(it) }
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("UserManager", "Data snapshot received: exists=${snapshot.exists()}, childrenCount=${snapshot.childrenCount}")
                users.clear()
                if (!snapshot.exists() || snapshot.childrenCount.toInt() == 0) {
                    Log.d("UserManager", "No users found in database")
                    _usersFlow.value = emptyList()
                    return
                }

                for (item in snapshot.children) {
                    try {
                        val uid = item.key ?: continue
                        val displayName = item.child("displayName").getValue(String::class.java) ?: "Unknown User"
                        val email = item.child("email").getValue(String::class.java) ?: ""
                        val role = item.child("role").getValue(String::class.java) ?: "user"
                        val createdAt = item.child("createdAt").getValue(Long::class.java) ?: 0L
                        val user = User(uid, displayName, email, role, createdAt)
                        users.add(user)
                        Log.d("UserManager", "Added user: uid=$uid, displayName=$displayName")
                    } catch (e: Exception) {
                        Log.e("UserManager", "Error parsing user: ${item.key}, error: ${e.message}")
                    }
                }
                _usersFlow.value = users.toList()
                Log.d("UserManager", "Updated usersFlow: ${users.size} users")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserManager", "Database error: ${error.message}, details: ${error.details}")
                // Không cập nhật _usersFlow để giữ danh sách hiện tại nếu có lỗi
            }
        }
        valueEventListener?.let {
            usersRef.addValueEventListener(it)
            Log.d("UserManager", "Firebase listener set up successfully")
        }
    }

    fun fetchAllUsers(onSuccess: (List<User>) -> Unit, onError: (String) -> Unit) {
        usersRef.get().addOnSuccessListener { snapshot ->
            Log.d("UserManager", "Fetching all users - Snapshot exists: ${snapshot.exists()}, Children count: ${snapshot.childrenCount}")
            val userList = mutableListOf<User>()
            if (!snapshot.exists() || snapshot.childrenCount.toInt() == 0) {
                Log.d("UserManager", "No users found in database")
                users.clear()
                _usersFlow.value = emptyList()
                onSuccess(emptyList())
                return@addOnSuccessListener
            }

            for (item in snapshot.children) {
                try {
                    val uid = item.key ?: continue
                    val displayName = item.child("displayName").getValue(String::class.java) ?: "Unknown User"
                    val email = item.child("email").getValue(String::class.java) ?: ""
                    val role = item.child("role").getValue(String::class.java) ?: "user"
                    val createdAt = item.child("createdAt").getValue(Long::class.java) ?: 0L
                    val user = User(uid, displayName, email, role, createdAt)
                    userList.add(user)
                    Log.d("UserManager", "Added user to fetch list: uid=$uid, displayName=$displayName")
                } catch (e: Exception) {
                    Log.e("UserManager", "Error parsing user: ${item.key}, error: ${e.message}")
                }
            }
            users.clear()
            users.addAll(userList)
            _usersFlow.value = users.toList()
            Log.d("UserManager", "Fetched users: ${userList.size} users")
            onSuccess(userList)
        }.addOnFailureListener { error ->
            Log.e("UserManager", "Failed to fetch users: ${error.message}")
            onError(error.message ?: "Unknown error")
        }
    }

    fun cleanup() {
        valueEventListener?.let {
            usersRef.removeEventListener(it)
            valueEventListener = null
            Log.d("UserManager", "Cleaned up Firebase listener")
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
                Log.d("OrderManager", "Data snapshot received: exists=${snapshot.exists()}, childrenCount=${snapshot.childrenCount}")
                orders.clear()
                if (!snapshot.exists() || snapshot.childrenCount.toInt() == 0) {
                    Log.d("OrderManager", "No orders found in database")
                    _ordersFlow.value = emptyList()
                    return
                }

                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    Log.d("OrderManager", "Processing orders for user: $userId, ordersCount=${userSnapshot.childrenCount}")
                    for (orderSnapshot in userSnapshot.children) {
                        try {
                            val orderId = orderSnapshot.key ?: continue
                            Log.d("OrderManager", "Processing order: $orderId")
                            val totalPrice = orderSnapshot.child("totalPrice").getValue(Double::class.java) ?: 0.0
                            val status = orderSnapshot.child("status").getValue(String::class.java) ?: "pending"
                            val createdAt = orderSnapshot.child("createdAt").getValue(Long::class.java) ?: 0L
                            val addressMap = orderSnapshot.child("address").value as? Map<String, Any>
                            val itemsSnapshot = orderSnapshot.child("products")
                            val itemsList = mutableListOf<OrderItem>()

                            if (!itemsSnapshot.exists()) {
                                Log.w("OrderManager", "No products found for order: $orderId")
                            } else {
                                for (item in itemsSnapshot.children) {
                                    try {
                                        val name = item.child("name").getValue(String::class.java)
                                            ?: item.child("title").getValue(String::class.java)
                                            ?: "Unknown Item"
                                        val price = item.child("price").getValue(Double::class.java) ?: 0.0
                                        val quantityValue = item.child("quantity").value
                                        val quantity = when (quantityValue) {
                                            is Long -> quantityValue.toInt()
                                            is Int -> quantityValue
                                            else -> {
                                                Log.w("OrderManager", "Invalid quantity for item in order $orderId: $quantityValue")
                                                0
                                            }
                                        }
                                        val imageUrl = item.child("imageUrl").getValue(String::class.java)
                                        itemsList.add(OrderItem(name, price, quantity, imageUrl))
                                        Log.d("OrderManager", "Added item: name=$name, price=$price, quantity=$quantity")
                                    } catch (e: Exception) {
                                        Log.e("OrderManager", "Error parsing item in order $orderId: ${e.message}")
                                    }
                                }
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
                            Log.d("OrderManager", "Added order: id=$orderId, userId=$userId, items=${itemsList.size}")
                        } catch (e: Exception) {
                            Log.e("OrderManager", "Error parsing order: ${orderSnapshot.key}, error: ${e.message}")
                        }
                    }
                }
                orders.sortByDescending { it.createdAt }
                _ordersFlow.value = orders.toList()
                Log.d("OrderManager", "Updated ordersFlow: ${orders.size} orders")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("OrderManager", "Database error: ${error.message}, details: ${error.details}")
            }
        }
        valueEventListener?.let {
            ordersRef.addValueEventListener(it)
            Log.d("OrderManager", "Firebase listener set up successfully")
        }
    }

    fun fetchAllOrders(onSuccess: (List<Order>) -> Unit, onError: (String) -> Unit) {
        ordersRef.get().addOnSuccessListener { snapshot ->
            Log.d("OrderManager", "Fetching all orders - Snapshot exists: ${snapshot.exists()}, Children count: ${snapshot.childrenCount}")
            val orderList = mutableListOf<Order>()
            if (!snapshot.exists() || snapshot.childrenCount.toInt() == 0) {
                Log.d("OrderManager", "No orders found in database")
                orders.clear()
                _ordersFlow.value = emptyList()
                onSuccess(emptyList())
                return@addOnSuccessListener
            }

            for (userSnapshot in snapshot.children) {
                val userId = userSnapshot.key ?: continue
                Log.d("OrderManager", "Processing orders for user: $userId, Orders count: ${userSnapshot.childrenCount}")
                for (orderSnapshot in userSnapshot.children) {
                    try {
                        val orderId = orderSnapshot.key ?: continue
                        Log.d("OrderManager", "Processing order: $orderId")
                        val totalPrice = orderSnapshot.child("totalPrice").getValue(Double::class.java) ?: 0.0
                        val status = orderSnapshot.child("status").getValue(String::class.java) ?: "pending"
                        val createdAt = orderSnapshot.child("createdAt").getValue(Long::class.java) ?: 0L
                        val addressMap = orderSnapshot.child("address").value as? Map<String, Any>
                        val itemsSnapshot = orderSnapshot.child("products")
                        val itemsList = mutableListOf<OrderItem>()

                        if (!itemsSnapshot.exists()) {
                            Log.w("OrderManager", "No products found for order: $orderId")
                        } else {
                            for (item in itemsSnapshot.children) {
                                try {
                                    val name = item.child("name").getValue(String::class.java)
                                        ?: item.child("title").getValue(String::class.java)
                                        ?: "Unknown Item"
                                    val price = item.child("price").getValue(Double::class.java) ?: 0.0
                                    val quantityValue = item.child("quantity").value
                                    val quantity = when (quantityValue) {
                                        is Long -> quantityValue.toInt()
                                        is Int -> quantityValue
                                        else -> {
                                            Log.w("OrderManager", "Invalid quantity for item in order $orderId: $quantityValue")
                                            0
                                        }
                                    }
                                    val imageUrl = item.child("imageUrl").getValue(String::class.java)
                                    itemsList.add(OrderItem(name, price, quantity, imageUrl))
                                    Log.d("OrderManager", "Added item: name=$name, price=$price, quantity=$quantity")
                                } catch (e: Exception) {
                                    Log.e("OrderManager", "Error parsing item in order $orderId: ${e.message}")
                                }
                            }
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
                        orderList.add(order)
                        Log.d("OrderManager", "Added order to fetch list: id=$orderId, userId=$userId, items=${itemsList.size}")
                    } catch (e: Exception) {
                        Log.e("OrderManager", "Error parsing order: ${orderSnapshot.key}, error: ${e.message}")
                    }
                }
            }
            orderList.sortByDescending { it.createdAt }
            orders.clear()
            orders.addAll(orderList)
            _ordersFlow.value = orders.toList()
            Log.d("OrderManager", "Fetched orders: ${orderList.size} orders")
            onSuccess(orderList)
        }.addOnFailureListener { error ->
            Log.e("OrderManager", "Failed to fetch orders: ${error.message}")
            onError(error.message ?: "Unknown error")
        }
    }

    fun updateOrderStatus(
        userId: String,
        orderId: String,
        newStatus: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (userId.isEmpty() || orderId.isEmpty()) {
            Log.e("OrderManager", "Invalid userId or orderId: userId=$userId, orderId=$orderId")
            onError("Invalid userId or orderId")
            return
        }
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