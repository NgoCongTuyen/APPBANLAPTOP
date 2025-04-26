package com.example.appbanlaptop.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appbanlaptop.Activity.BottomActivity.BottomMenu
import com.example.appbanlaptop.R
import com.example.appbanlaptop.cart.CartScreenActivity
import com.example.appbanlaptop.ui.theme.APPBANLAPTOPTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.parcelize.Parcelize

// Extension function để rút gọn orderId
fun String.shortenOrderId(maxLength: Int = 8): String {
    return if (this.length <= maxLength) {
        this
    } else {
        "${this.take(maxLength)}..."
    }
}

// Data class để lưu thông tin đơn hàng
@Parcelize
data class Order(
    val orderId: String,
    val orderDate: Date,
    val items: List<OrderItem>,
    val totalAmount: Double,
    val status: OrderStatus,
    val shippingAddress: String,
    val recipientName: String,
    val recipientPhone: String
) : android.os.Parcelable {
    companion object {
        fun fromMap(orderId: String, map: Map<String, Any>): Order {
            val addressMap = map["address"] as? Map<String, Any> ?: emptyMap()
            val productsList = map["products"] as? List<Map<String, Any>> ?: emptyList()
            val createdAt = (map["createdAt"] as? Long) ?: 0L
            val totalPrice = (map["totalPrice"] as? Number)?.toDouble() ?: 0.0
            val statusString = map["status"] as? String ?: "pending"

            val items = productsList.map { productMap ->
                OrderItem(
                    productName = productMap["name"] as? String ?: "Unknown Item",
                    quantity = (productMap["quantity"] as? Long)?.toInt() ?: 1,
                    price = (productMap["price"] as? String)?.toDoubleOrNull()
                        ?: (productMap["price"] as? Number)?.toDouble() ?: 0.0,
                    imageUrl = productMap["imageUrl"] as? String
                )
            }

            val itemsTotal = items.sumOf { it.price * it.quantity }

            return Order(
                orderId = orderId,
                orderDate = Date(createdAt),
                items = items,
                totalAmount = totalPrice,
                status = OrderStatus.valueOf(statusString.uppercase()),
                shippingAddress = addressMap["addressDetail"] as? String ?: "Unknown Address",
                recipientName = addressMap["name"] as? String ?: "Unknown Name",
                recipientPhone = addressMap["phone"] as? String ?: "Unknown Phone"
            )
        }
    }

    val itemsTotal: Double
        get() = items.sumOf { it.price * it.quantity }

    val additionalFee: Double
        get() = totalAmount - itemsTotal
}

@Parcelize
data class OrderItem(
    val productName: String,
    val quantity: Int,
    val price: Double,
    val imageUrl: String?
) : android.os.Parcelable

enum class OrderStatus {
    PENDING, CONFIRMED, SHIPPING, DELIVERED, CANCELLED
}

class OrderActivity : ComponentActivity() {
    private var isDarkMode = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isDarkMode.value = ThemeManager.isDarkMode(this)

        setContent {
            APPBANLAPTOPTheme(darkTheme = isDarkMode.value) {
                val navController = rememberNavController()
                val orders = remember { mutableStateListOf<Order>() }
                val userId = FirebaseAuth.getInstance().currentUser?.uid

                LaunchedEffect(userId) {
                    if (userId == null) {
                        Toast.makeText(this@OrderActivity, "Vui lòng đăng nhập để xem đơn hàng", Toast.LENGTH_SHORT).show()
                        return@LaunchedEffect
                    }

                    val database = FirebaseDatabase.getInstance()
                    val ordersRef = database.getReference("orders").child(userId)
                    ordersRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            orders.clear()
                            for (orderSnapshot in snapshot.children) {
                                try {
                                    val orderId = orderSnapshot.key ?: continue
                                    val orderMap = orderSnapshot.value as? Map<String, Any> ?: continue
                                    val order = Order.fromMap(orderId, orderMap)
                                    orders.add(order)
                                } catch (e: Exception) {
                                    Log.e("OrderActivity", "Error parsing order: ${e.message}")
                                }
                            }
                            orders.sortByDescending { it.orderDate }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("OrderActivity", "Database error: ${error.message}")
                        }
                    })
                }

                NavHost(navController = navController, startDestination = "order_list") {
                    composable("order_list") {
                        OrderListScreen(
                            orders = orders,
                            onOrderClick = { order ->
                                navController.navigate("order_detail/${order.orderId}")
                            },
                            onBackClick = { finish() }
                        )
                    }
                    composable("order_detail/{orderId}") { backStackEntry ->
                        val orderId = backStackEntry.arguments?.getString("orderId")
                        val order = orders.find { it.orderId == orderId }
                        if (order != null) {
                            OrderDetailScreen(order = order, onBackClick = {
                                navController.popBackStack()
                            })
                        } else {
                            Log.e("OrderActivity", "Order with ID $orderId not found")
                            Toast.makeText(this@OrderActivity, "Không tìm thấy đơn hàng", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isDarkMode.value = ThemeManager.isDarkMode(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    orders: List<Order>,
    onOrderClick: (Order) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Đơn hàng của tôi",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.back),
                            contentDescription = "Back",
                            modifier = Modifier.size(40.dp),
                            tint = Color.Unspecified
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            BottomMenu(
                onItemClick = {
                    context.startActivity(Intent(context, CartScreenActivity::class.java))
                }
            )
        }
    ) { paddingValues ->
        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có đơn hàng nào",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
            ) {
                items(orders) { order ->
                    OrderItem(order = order, onClick = { onOrderClick(order) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun OrderItem(order: Order, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Đơn hàng #${order.orderId.shortenOrderId()}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OrderStatusChip(status = order.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ngày đặt: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(order.orderDate)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${order.items.size} sản phẩm",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tổng tiền: ${DecimalFormat("#,###").format(order.itemsTotal)}đ",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun OrderStatusChip(status: OrderStatus) {
    val (backgroundColor, textColor) = when (status) {
        OrderStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        OrderStatus.CONFIRMED -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        OrderStatus.SHIPPING -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        OrderStatus.DELIVERED -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }

    val statusText = when (status) {
        OrderStatus.PENDING -> "Chờ xác nhận"
        OrderStatus.CONFIRMED -> "Đã xác nhận"
        OrderStatus.SHIPPING -> "Đang giao"
        OrderStatus.DELIVERED -> "Đã giao"
        OrderStatus.CANCELLED -> "Đã hủy"
    }

    Surface(
        modifier = Modifier.clip(RoundedCornerShape(4.dp)),
        color = backgroundColor
    ) {
        Text(
            text = statusText,
            color = textColor,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(order: Order, onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chi tiết đơn hàng",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("<", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Đơn hàng #${order.orderId}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OrderStatusChip(status = order.status)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Thông tin người nhận",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Họ tên: ${order.recipientName}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Số điện thoại: ${order.recipientPhone}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Địa chỉ: ${order.shippingAddress}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Danh sách sản phẩm",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        order.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.productName} x${item.quantity}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${DecimalFormat("#,###").format(item.price)}đ",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tổng tiền:",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${DecimalFormat("#,###").format(order.itemsTotal)}đ",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}