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
    val totalAmount: Double,
    val status: OrderStatus,
    val shippingAddress: String
) : android.os.Parcelable

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

// Object để lưu trữ danh sách đơn hàng toàn cục
object OrderState {
    val orders = mutableStateListOf<Order>().apply {
        addAll(createSampleOrders())
    }

    private fun createSampleOrders(): List<Order> {
        return listOf(
            Order(
                orderId = "OD001",
                orderDate = Date(),
                items = listOf(
                    OrderItem(
                        productName = "MacBook Pro M1",
                        quantity = 1,
                        price = 29990000.0,
                        imageUrl = null
                    ),
                    OrderItem(
                        productName = "Magic Mouse",
                        quantity = 1,
                        price = 2490000.0,
                        imageUrl = null
                    )
                ),
                totalAmount = 32480000.0,
                status = OrderStatus.DELIVERED,
                shippingAddress = "59/22C Mã Lò, Bình Trị Đông A, Bình Tân, Hồ Chí Minh"
            )
        )
    }
}

class OrderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Nhận đơn hàng mới từ Intent
        intent.getParcelableExtra<Order>("NEW_ORDER")?.let { newOrder ->
            Log.d("OrderActivity", "Received new order: $newOrder")
            if (!OrderState.orders.any { it.orderId == newOrder.orderId }) {
                OrderState.orders.add(0, newOrder) // Thêm vào đầu danh sách
                Toast.makeText(this, "Đã thêm đơn hàng mới: ${newOrder.orderId}", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("OrderActivity", "Order ${newOrder.orderId} already exists, skipping")
            }
        } ?: run {
            Log.d("OrderActivity", "No new order received in Intent")
        }

        // Log danh sách đơn hàng hiện tại
        Log.d("OrderActivity", "Current orders: ${OrderState.orders}")

        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "order_list") {
                composable("order_list") {
                    OrderListScreen(orders = OrderState.orders) { order ->
                        navController.navigate("order_detail/${order.orderId}")
                    }
                }
                composable("order_detail/{orderId}") { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString("orderId")
                    val order = OrderState.orders.find { it.orderId == orderId }
                    if (order != null) {
                        OrderDetailScreen(order = order, onBackClick = {
                            navController.popBackStack()
                        })
                    } else {
                        Log.e("OrderActivity", "Order with ID $orderId not found")
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

            Text(
                text = "Tổng tiền: ${DecimalFormat("#,###").format(order.totalAmount)}đ",
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
                            text = "Địa chỉ giao hàng",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = order.shippingAddress,
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

                        // Tổng tiền
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tổng tiền",
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