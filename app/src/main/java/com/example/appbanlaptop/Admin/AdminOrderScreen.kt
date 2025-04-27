package com.example.appbanlaptop.Admin

import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appbanlaptop.Model.Order
import com.example.appbanlaptop.Model.OrderItem
import com.example.appbanlaptop.Model.OrderManager
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrderScreen(modifier: Modifier = Modifier) {
    val orders by OrderManager.ordersFlow.collectAsState()
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Gọi fetchAllOrders để đảm bảo dữ liệu được tải ban đầu
    LaunchedEffect(Unit) {
        OrderManager.fetchAllOrders(
            onSuccess = { fetchedOrders ->
                isLoading = false
                Log.d("AdminOrderScreen", "Fetched ${fetchedOrders.size} orders")
                Toast.makeText(context, "Đã tải ${fetchedOrders.size} đơn hàng", Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                isLoading = false
                errorMessage = error
                Log.e("AdminOrderScreen", "Error fetching orders: $error")
                Toast.makeText(context, "Lỗi khi tải đơn hàng: $error", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Log khi danh sách đơn hàng thay đổi
    LaunchedEffect(orders) {
        Log.d("AdminOrderScreen", "UI received ${orders.size} orders")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quản lý đơn hàng",
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
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Lỗi: $errorMessage",
                            color = Color.Red,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                OrderManager.fetchAllOrders(
                                    onSuccess = { fetchedOrders ->
                                        isLoading = false
                                        Toast.makeText(context, "Đã tải ${fetchedOrders.size} đơn hàng", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { error ->
                                        isLoading = false
                                        errorMessage = error
                                        Toast.makeText(context, "Lỗi khi tải đơn hàng: $error", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        ) {
                            Text("Thử lại")
                        }
                    }
                }
            }
            orders.isEmpty() -> {
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
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF5F5F5))
                        .padding(paddingValues)
                ) {
                    items(orders) { order ->
                        if (order.id != null && order.userId != null) {
                            AdminOrderItem(
                                order = order,
                                onStatusUpdate = { newStatus ->
                                    OrderManager.updateOrderStatus(
                                        userId = order.userId,
                                        orderId = order.id,
                                        newStatus = newStatus,
                                        onSuccess = {
                                            Toast.makeText(context, "Cập nhật trạng thái thành công", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, "Lỗi: $error", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        } else {
                            Log.w("AdminOrderScreen", "Skipping order with null id or userId: $order")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminOrderItem(order: Order, onStatusUpdate: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showItems by remember { mutableStateOf(false) }
    val statusOptions = listOf("pending", "confirmed", "shipping", "delivered", "cancelled")

    // Log để kiểm tra items
    LaunchedEffect(order) {
        Log.d("AdminOrderItem", "Order ${order.id}: items = ${order.items?.size ?: 0}, items = ${order.items}")
        order.items?.forEach { item ->
            Log.d("AdminOrderItem", "Item: title=${item.title}, price=${item.price}, quantity=${item.quantity}")
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Đơn hàng #${order.id?.shortenOrderId() ?: "N/A"}",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                OrderStatusChip(status = order.status ?: "pending")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Người dùng: #${order.userId?.toHashedNumber() ?: "N/A"}",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ngày đặt: ${
                    order.createdAt?.let { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it)) }
                        ?: "N/A"
                }",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${order.items?.size ?: 0} sản phẩm",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.clickable { showItems = !showItems }
            )

            // Hiển thị danh sách sản phẩm nếu showItems = true
            if (showItems) {
                if (order.items.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Không có sản phẩm trong đơn hàng",
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    order.items.forEach { item ->
                        OrderItemRow(item = item)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tổng tiền: ${DecimalFormat("#,###").format(order.totalPrice ?: 0L)}đ",
                color = Color(0xFFFF0000),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box {
                Text(
                    text = "Cập nhật trạng thái",
                    color = Color.Blue,
                    modifier = Modifier
                        .clickable { expanded = true }
                        .padding(vertical = 8.dp)
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    statusOptions.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                onStatusUpdate(status)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderItemRow(item: OrderItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F0F0))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Tên: ${item.title ?: "Không xác định"}",
                color = Color.Black,
                fontSize = 14.sp
            )
            Text(
                text = "Số lượng: ${item.quantity ?: 0}",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        Text(
            text = "Giá: ${DecimalFormat("#,###").format(item.price ?: 0L)}đ",
            color = Color.Black,
            fontSize = 14.sp
        )
    }
}


@Composable
fun OrderStatusChip(status: String) {
    val (backgroundColor, textColor) = when (status.lowercase()) {
        "pending" -> Color(0xFFFFF3E0) to Color(0xFFFF9800)
        "confirmed" -> Color(0xFFE3F2FD) to Color(0xFF2196F3)
        "shipping" -> Color(0xFFF3E5F5) to Color(0xFF9C27B0)
        "delivered" -> Color(0xFFE8F5E9) to Color(0xFF4CAF50)
        "cancelled" -> Color(0xFFFFEBEE) to Color(0xFFF44336)
        else -> Color(0xFFFFF3E0) to Color(0xFFFF9800)
    }

    val statusText = when (status.lowercase()) {
        "pending" -> "Chờ xác nhận"
        "confirmed" -> "Đã xác nhận"
        "shipping" -> "Đang giao"
        "delivered" -> "Đã giao"
        "cancelled" -> "Đã hủy"
        else -> "Chờ xác nhận"
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

fun String.shortenOrderId(maxLength: Int = 10): String {
    return if (this.length <= maxLength) {
        this
    } else {
        "${this.take(maxLength)}..."
    }
}

fun String.toHashedNumber(maxLength: Int = 10): String {
    val hash = this.hashCode().toLong().absoluteValue % 100000000
    return hash.toString().padStart(maxLength, '0')
}

