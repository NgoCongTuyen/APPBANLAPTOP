package com.example.appbanlaptop.Admin

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
import com.example.appbanlaptop.Model.OrderManager
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrderScreen(modifier: Modifier = Modifier) {
    val orders by OrderManager.ordersFlow.collectAsState()
    val context = LocalContext.current

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
                    AdminOrderItem(
                        order = order,
                        onStatusUpdate = { newStatus ->
                            OrderManager.updateOrderStatus(
                                userId = order.userId ?: return@AdminOrderItem,
                                orderId = order.id ?: return@AdminOrderItem,
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
                }
            }
        }
    }
}

@Composable
fun AdminOrderItem(order: Order, onStatusUpdate: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val statusOptions = listOf("pending", "confirmed", "shipping", "delivered", "cancelled")

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
                text = "Người dùng: ${order.userId ?: "N/A"}",
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
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tổng tiền: ${DecimalFormat("#,###").format(order.totalPrice ?: 0.0)}đ",
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

fun String.shortenOrderId(maxLength: Int = 8): String {
    return if (this.length <= maxLength) {
        this
    } else {
        "${this.take(maxLength)}..."
    }
}