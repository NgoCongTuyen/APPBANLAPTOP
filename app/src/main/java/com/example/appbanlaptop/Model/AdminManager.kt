package com.example.appbanlaptop.Model

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
@IgnoreExtraProperties
data class Order(
    @PropertyName("userId") val userId: String? = null,
    @PropertyName("id") val id: String? = null,
    @PropertyName("products") val items: List<OrderItem>? = null, // Ánh xạ "products" sang "items"
    @PropertyName("totalPrice") val totalPrice: Long? = null,
    @PropertyName("status") val status: String? = null,
    @PropertyName("createdAt") val createdAt: Long? = null,
    @PropertyName("shippingAddress") val shippingAddress: String? = null,
    @PropertyName("recipientName") val recipientName: String? = null,
    @PropertyName("recipientPhone") val recipientPhone: String? = null
)

@IgnoreExtraProperties
data class OrderItem(
    @PropertyName("name") val title: String? = null, // Ánh xạ "name" sang "title"
    @PropertyName("price") val price: Long? = null,
    @PropertyName("quantity") val quantity: Int? = null,
    @PropertyName("imageUrl") val imageUrl: String? = null,
    @PropertyName("color") val color: String? = null
)

object ProductManager {
    private val database = FirebaseDatabase.getInstance()
    private val productsRef = database.getReference("Items")
    private val _itemsFlow = MutableStateFlow<Map<String, ProductItem>>(emptyMap())
    val itemsFlow: StateFlow<Map<String, ProductItem>> = _itemsFlow.asStateFlow()
    private var valueEventListener: ValueEventListener? = null
    private var isListenerActive = false

    init {
        setupListener()
    }

    fun addProduct(product: ProductItem, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val newRef = productsRef.push()
        val productKey = newRef.key ?: run {
            Log.e("ProductManager", "Không thể tạo key cho sản phẩm mới")
            onError("Không thể tạo key cho sản phẩm")
            return
        }
        Log.d("ProductManager", "Thêm sản phẩm với key: $productKey, title: ${product.title}")
        newRef.setValue(product)
            .addOnSuccessListener {
                Log.d("ProductManager", "Thêm sản phẩm thành công: ${product.title}")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e("ProductManager", "Lỗi khi thêm sản phẩm: ${it.message}")
                onError(it.message ?: "Lỗi không xác định")
            }
    }

    fun updateProduct(product: ProductItem, productKey: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (productKey.isBlank()) {
            Log.e("ProductManager", "Key sản phẩm là rỗng")
            onError("Key sản phẩm là rỗng")
            return
        }
        Log.d("ProductManager", "Cập nhật sản phẩm với key: $productKey, title: ${product.title}")
        productsRef.child(productKey).setValue(product)
            .addOnSuccessListener {
                Log.d("ProductManager", "Cập nhật sản phẩm thành công: ${product.title}")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e("ProductManager", "Lỗi khi cập nhật sản phẩm: ${it.message}")
                onError(it.message ?: "Lỗi không xác định")
            }
    }

    fun removeProduct(productKey: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (productKey.isBlank()) {
            Log.e("ProductManager", "Key sản phẩm là rỗng")
            onError("Key sản phẩm là rỗng")
            return
        }
        Log.d("ProductManager", "Xóa sản phẩm với key: $productKey")
        productsRef.child(productKey).removeValue()
            .addOnSuccessListener {
                Log.d("ProductManager", "Xóa sản phẩm thành công với key: $productKey")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e("ProductManager", "Lỗi khi xóa sản phẩm: ${it.message}")
                onError(it.message ?: "Lỗi không xác định")
            }
    }

    fun cleanup() {
        valueEventListener?.let {
            productsRef.removeEventListener(it)
            valueEventListener = null
            isListenerActive = false
            Log.d("ProductManager", "Đã dọn dẹp listener Firebase")
        }
    }

    fun ensureListener() {
        if (!isListenerActive) {
            setupListener()
        }
    }

    private fun setupListener() {
        cleanup()
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("ProductManager", "Nhận snapshot dữ liệu: exists=${snapshot.exists()}, childrenCount=${snapshot.childrenCount}")
                val newItems = mutableMapOf<String, ProductItem>()
                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    Log.d("ProductManager", "Không tìm thấy sản phẩm trong cơ sở dữ liệu")
                    _itemsFlow.value = emptyMap()
                    return
                }

                for (item in snapshot.children) {
                    try {
                        val product = item.getValue(ProductItem::class.java)
                        if (product != null && item.key != null) {
                            newItems[item.key!!] = product
                            Log.d("ProductManager", "Thêm sản phẩm: key=${item.key}, title=${product.title}")
                        } else {
                            Log.w("ProductManager", "Dữ liệu sản phẩm không hợp lệ tại key: ${item.key}")
                        }
                    } catch (e: Exception) {
                        Log.e("ProductManager", "Lỗi khi phân tích sản phẩm tại key: ${item.key}, lỗi: ${e.message}")
                    }
                }
                _itemsFlow.value = newItems
                Log.d("ProductManager", "Cập nhật itemsFlow: ${newItems.size} sản phẩm")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ProductManager", "Lỗi cơ sở dữ liệu: ${error.message}, chi tiết: ${error.details}")
            }
        }
        valueEventListener?.let {
            productsRef.addValueEventListener(it)
            isListenerActive = true
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
        // Lấy ID mới dựa trên số lượng category hiện tại
        val newId = categories.size
        val categoryWithId = category.copy(id = newId)

        // Sử dụng newId làm key thay vì push()
        categoriesRef.child(newId.toString()).setValue(categoryWithId)
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
                // Sau khi xóa, cập nhật lại ID của các category
                reindexCategories(categoryId, onSuccess)
            }
            .addOnFailureListener {
                onError(it.message ?: "Unknown error")
            }
    }

    private fun reindexCategories(deletedId: Int, onSuccess: () -> Unit) {
        // Lấy tất cả category hiện tại
        categoriesRef.get().addOnSuccessListener { snapshot ->
            val updates = mutableMapOf<String, Any?>()

            // Duyệt qua các category và cập nhật ID cho những category có ID > deletedId
            for (item in snapshot.children) {
                val currentId = item.key?.toIntOrNull()
                if (currentId != null && currentId > deletedId) {
                    val category = item.getValue(Category::class.java)?.copy(id = currentId - 1)
                    if (category != null) {
                        // Xóa node cũ và thêm node mới với ID mới
                        updates["${currentId}"] = null
                        updates["${currentId - 1}"] = category
                    }
                }
            }

            // Thực hiện cập nhật
            if (updates.isNotEmpty()) {
                categoriesRef.updateChildren(updates)
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener {
                        Log.e("CategoryManager", "Failed to reindex categories: ${it.message}")
                    }
            } else {
                onSuccess() // Nếu không có gì cần cập nhật, gọi onSuccess ngay
            }
        }.addOnFailureListener {
            Log.e("CategoryManager", "Failed to fetch categories for reindexing: ${it.message}")
        }
    }

    private fun setupListener() {
        valueEventListener?.let { categoriesRef.removeEventListener(it) }
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categories.clear()
                for (item in snapshot.children) {
                    try {
                        val id = item.key?.toIntOrNull() // Lấy key làm ID
                        val title = item.child("title").getValue(String::class.java)
                        val picUrl = item.child("picUrl").getValue(String::class.java)
                        if (id != null) {
                            val category = Category(id, title, picUrl)
                            categories.add(category)
                        }
                    } catch (e: Exception) {
                        Log.e("CategoryManager", "Error parsing category: ${item.key}, error: ${e.message}")
                    }
                }
                _categoriesFlow.value = categories.sortedBy { it.id }.toList() // Sắp xếp theo ID
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

    private fun parseOrder(userId: String, orderSnapshot: DataSnapshot): Order? {
        try {
            val orderId = orderSnapshot.key ?: return null
            Log.d("OrderManager", "Parsing order: $orderId")

            // Lấy các trường cơ bản
            val totalPriceRaw = orderSnapshot.child("totalPrice").value
            val totalPrice = when (totalPriceRaw) {
                is Long -> totalPriceRaw
                is Double -> totalPriceRaw.toLong()
                is String -> totalPriceRaw.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
                else -> 0L
            }
            val status = orderSnapshot.child("status").getValue(String::class.java) ?: "pending"
            val createdAt = orderSnapshot.child("createdAt").getValue(Long::class.java) ?: 0L
            val addressMap = orderSnapshot.child("address").value as? Map<String, Any>

            // Lấy danh sách sản phẩm
            val itemsSnapshot = orderSnapshot.child("products")
            val itemsList = mutableListOf<OrderItem>()

            if (!itemsSnapshot.exists()) {
                Log.w("OrderManager", "No products found for order: $orderId")
            } else {
                for (item in itemsSnapshot.children) {
                    try {
                        val name = item.child("name").getValue(String::class.java) ?: "Unknown Item"
                        val priceRaw = item.child("price").value
                        val price = when (priceRaw) {
                            is Long -> priceRaw
                            is Double -> priceRaw.toLong()
                            is String -> priceRaw.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
                            else -> 0L
                        }
                        val quantityRaw = item.child("quantity").value
                        val quantity = when (quantityRaw) {
                            is Long -> quantityRaw.toInt()
                            is Int -> quantityRaw
                            is Double -> quantityRaw.toInt()
                            else -> {
                                Log.w("OrderManager", "Invalid quantity for item in order $orderId: $quantityRaw")
                                0
                            }
                        }
                        val imageUrl = item.child("imageUrl").getValue(String::class.java)
                        val color = item.child("color").getValue(String::class.java) ?: "Default Color"
                        itemsList.add(OrderItem(name, price, quantity, imageUrl, color))
                        Log.d("OrderManager", "Added item: name=$name, price=$price, quantity=$quantity, imageUrl=$imageUrl")
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
            Log.d("OrderManager", "Parsed order: id=$orderId, userId=$userId, items=${itemsList.size}, totalPrice=$totalPrice, createdAt=$createdAt")
            return order
        } catch (e: Exception) {
            Log.e("OrderManager", "Error parsing order: ${orderSnapshot.key}, error: ${e.message}")
            return null
        }
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
                        val order = parseOrder(userId, orderSnapshot)
                        order?.let {
                            orders.add(it)
                            Log.d("OrderManager", "Added order: id=${it.id}, userId=${it.userId}, items=${it.items?.size ?: 0}, createdAt=${it.createdAt}")
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
        ordersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("OrderManager", "Fetching orders: exists=${snapshot.exists()}, childrenCount=${snapshot.childrenCount}")
                orders.clear()
                if (!snapshot.exists() || snapshot.childrenCount.toInt() == 0) {
                    Log.d("OrderManager", "No orders found in database")
                    _ordersFlow.value = emptyList()
                    onSuccess(emptyList())
                    return
                }

                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    Log.d("OrderManager", "Processing orders for user: $userId, ordersCount=${userSnapshot.childrenCount}")
                    for (orderSnapshot in userSnapshot.children) {
                        val order = parseOrder(userId, orderSnapshot)
                        order?.let {
                            orders.add(it)
                            Log.d("OrderManager", "Added order: id=${it.id}, userId=${it.userId}, items=${it.items?.size ?: 0}, createdAt=${it.createdAt}")
                            it.items?.forEach { item ->
                                Log.d("OrderManager", "Item: title=${item.title}, price=${item.price}, quantity=${item.quantity}")
                            }
                        }
                    }
                }
                orders.sortByDescending { it.createdAt }
                _ordersFlow.value = orders.toList()
                onSuccess(orders.toList())
                Log.d("OrderManager", "Fetch completed: ${orders.size} orders")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("OrderManager", "Database error: ${error.message}")
                onError(error.message)
            }
        })
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