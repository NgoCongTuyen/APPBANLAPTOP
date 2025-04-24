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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.parcelize.Parcelize

// Data class để lưu thông tin đơn hàng
@Parcelize
data class Order(
    val orderId: String,
    val orderDate: Date,
    val items: List<OrderItem>,
    val totalAmount: Double, // Đây là tổng tiền từ database, có thể bao gồm phí bổ sung
    val status: OrderStatus,
    val shippingAddress: String,
    val recipientName: String, // Tên người nhận
    val recipientPhone: String // Số điện thoại người nhận
) : android.os.Parcelable {
    companion object {
        fun fromMap(orderId: String, map: Map<String, Any>): Order {
            val addressMap = map["address"] as? Map<String, Any> ?: emptyMap()
            val productsList = map["products"] as? List<Map<String, Any>> ?: emptyList()
            val createdAt = (map["createdAt"] as? Long) ?: 0L
            val totalPrice = (map["totalPrice"] as? Number)?.toDouble() ?: 0.0 // Xử lý cả Long và Double
            val statusString = map["status"] as? String ?: "pending"

            val items = productsList.map { productMap ->
                OrderItem(
                    productName = productMap["name"] as? String ?: "Unknown Item",
                    quantity = (productMap["quantity"] as? Long)?.toInt() ?: 1,
                    price = (productMap["price"] as? String)?.toDoubleOrNull()
                        ?: (productMap["price"] as? Number)?.toDouble() ?: 0.0, // Xử lý cả String và Number
                    imageUrl = productMap["imageUrl"] as? String
                )
            }

            // Tính lại tổng tiền của các sản phẩm (không bao gồm phí bổ sung)
            val itemsTotal = items.sumOf { it.price * it.quantity }

            return Order(
                orderId = orderId,
                orderDate = Date(createdAt),
                items = items,
                totalAmount = totalPrice, // Lưu tổng tiền từ database
                status = OrderStatus.valueOf(statusString.uppercase()),
                shippingAddress = addressMap["addressDetail"] as? String ?: "Unknown Address",
                recipientName = addressMap["name"] as? String ?: "Unknown Name",
                recipientPhone = addressMap["phone"] as? String ?: "Unknown Phone"
            )
        }
    }

    // Tính tổng tiền của các sản phẩm (không bao gồm phí bổ sung)
    val itemsTotal: Double
        get() = items.sumOf { it.price * it.quantity }

    // Kiểm tra xem có phí bổ sung không (phí vận chuyển, thuế, v.v.)
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            val orders = remember { mutableStateListOf<Order>() }
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            // Lấy dữ liệu đơn hàng từ Firebase Realtime Database
            LaunchedEffect(userId) {
                if (userId == null) {
                    Toast.makeText(this@OrderActivity, "Vui lòng đăng nhập để xem đơn hàng", Toast.LENGTH_SHORT).show()
                    return@LaunchedEffect
                }

                val database = FirebaseDatabase.getInstance()
                val ordersRef = database.getReference("orders").child(userId)

                ordersRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val orderList = mutableListOf<Order>()
                        for (orderSnapshot in snapshot.children) {
                            val orderId = orderSnapshot.key ?: continue
                            val orderMap = orderSnapshot.value as? Map<String, Any> ?: continue
                            try {
                                val order = Order.fromMap(orderId, orderMap)
                                orderList.add(order)
                            } catch (e: Exception) {
                                Log.e("OrderActivity", "Error parsing order $orderId: ${e.message}", e)
                            }
                        }
                        // Sắp xếp theo ngày đặt hàng (mới nhất trước)
                        orders.clear()
                        orders.addAll(orderList.sortedByDescending { it.orderDate })
                        Log.d("OrderActivity", "Loaded orders: $orders")
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("OrderActivity", "Error loading orders: ${error.message}", error.toException())
                        Toast.makeText(this@OrderActivity, "Lỗi khi tải đơn hàng: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            NavHost(navController = navController, startDestination = "order_list") {
                composable("order_list") {
                    OrderListScreen(orders = orders) { order ->
                        navController.navigate("order_detail/${order.orderId}")
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(orders: List<Order>, onOrderClick: (Order) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Đơn hàng của tôi",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
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
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
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
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                    text = "Đơn hàng #${order.orderId}",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                OrderStatusChip(status = order.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ngày đặt: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(order.orderDate)}",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${order.items.size} sản phẩm",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Hiển thị tổng tiền dựa trên itemsTotal
            Text(
                text = "Tổng tiền: ${DecimalFormat("#,###").format(order.itemsTotal)}đ",
                color = Color(0xFFFF0000),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun OrderStatusChip(status: OrderStatus) {
    val (backgroundColor, textColor) = when (status) {
        OrderStatus.PENDING -> Color(0xFFFFF3E0) to Color(0xFFFF9800)
        OrderStatus.CONFIRMED -> Color(0xFFE3F2FD) to Color(0xFF2196F3)
        OrderStatus.SHIPPING -> Color(0xFFF3E5F5) to Color(0xFF9C27B0)
        OrderStatus.DELIVERED -> Color(0xFFE8F5E9) to Color(0xFF4CAF50)
        OrderStatus.CANCELLED -> Color(0xFFFFEBEE) to Color(0xFFF44336)
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
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("<", color = Color.Black, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Thông tin đơn hàng
                        Text(
                            text = "Đơn hàng #${order.orderId}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OrderStatusChip(status = order.status)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Ngày đặt hàng",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(order.orderDate),
                            color = Color.Black,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Thông tin giao hàng",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Tên người nhận: ${order.recipientName}",
                            color = Color.Black,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Số điện thoại: ${order.recipientPhone}",
                            color = Color.Black,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Địa chỉ: ${order.shippingAddress}",
                            color = Color.Black,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color(0xFFEEEEEE))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Danh sách sản phẩm
                        Text(
                            text = "Sản phẩm",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        order.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.productName,
                                        color = Color.Black,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "x${item.quantity}",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                                Text(
                                    text = "${DecimalFormat("#,###").format(item.price)}đ",
                                    color = Color(0xFFFF0000),
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color(0xFFEEEEEE))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Hiển thị chi tiết giá
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tổng tiền sản phẩm",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            Text(
                                text = "${DecimalFormat("#,###").format(order.itemsTotal)}đ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        }

                        // Nếu có phí bổ sung (phí vận chuyển, thuế, v.v.), hiển thị riêng
                        if (order.additionalFee > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Phí bổ sung",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "+${DecimalFormat("#,###").format(order.additionalFee)}đ",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color(0xFFEEEEEE))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Tổng tiền cuối cùng (bao gồm phí bổ sung nếu có)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tổng tiền thanh toán",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.Black
                            )
                            Text(
                                text = "${DecimalFormat("#,###").format(order.totalAmount)}đ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFFFF0000)
                            )
                        }
                    }
                }
            }
        }
    }
}