package com.example.appbanlaptop.Admin

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appbanlaptop.Model.Order
import com.example.appbanlaptop.Model.OrderManager
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRevenueScreen(modifier: Modifier = Modifier) {
    val orders by OrderManager.ordersFlow.collectAsState()
    val context = LocalContext.current

    // Currency formatter for locale-aware formatting
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN")).apply {
        if (this is DecimalFormat) {
            applyPattern("#,### VNĐ")
        }
    }

    // Tính tổng doanh thu và số đơn hàng
    val totalRevenue by remember(orders) {
        derivedStateOf { orders.sumOf { it.totalPrice?.toDouble() ?: 0.0 } }
    }
    val totalOrders by remember(orders) { derivedStateOf { orders.size } }

    // Tính doanh thu theo ngày
    val revenueByDate by remember(orders) {
        derivedStateOf {
            val revenueMap = mutableMapOf<String, Double>()
            val dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())

            orders.forEach { order ->
                val date = order.createdAt?.let {
                    try {
                        dateFormat.format(Date(it))
                    } catch (e: Exception) {
                        Log.e("AdminRevenueScreen", "Invalid date format for order ${order.id}: ${order.createdAt}")
                        null
                    }
                } ?: return@forEach
                val price = order.totalPrice?.toDouble() ?: 0.0
                revenueMap[date] = (revenueMap[date] ?: 0.0) + price
            }

            revenueMap.entries.sortedBy {
                val calendar = Calendar.getInstance()
                try {
                    dateFormat.parse(it.key)?.let { date -> calendar.time = date }
                } catch (e: ParseException) {
                    Log.e("AdminRevenueScreen", "Failed to parse date: ${it.key}")
                }
                calendar.timeInMillis
            }.associate { it.key to it.value }
        }
    }

    // Log dữ liệu chỉ khi cần thiết (ví dụ, khi orders thay đổi lần đầu)
    LaunchedEffect(orders) {
        if (orders.isNotEmpty()) {
            Log.d("AdminRevenueScreen", "Orders loaded: ${orders.size}")
            orders.take(5).forEach { order -> // Giới hạn log để tránh spam
                Log.d("AdminRevenueScreen", "Order ${order.id}: items=${order.items?.size ?: 0}, totalPrice=${order.totalPrice}")
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Thống kê doanh thu",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Tổng số đơn hàng: $totalOrders",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Tổng doanh thu: ${currencyFormatter.format(totalRevenue)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        item {
            // Biểu đồ doanh thu
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Doanh thu theo ngày",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (revenueByDate.isEmpty()) {
                        Text(
                            text = "Không có dữ liệu doanh thu",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        SimpleBarChart(revenueByDate = revenueByDate, currencyFormatter = currencyFormatter)
                    }
                }
            }
        }

        items(orders, key = { it.id ?: UUID.randomUUID().toString() }) { order ->
            OrderItem(order = order, currencyFormatter = currencyFormatter)
        }
    }
}

@Composable
fun SimpleBarChart(revenueByDate: Map<String, Double>, currencyFormatter: NumberFormat) {
    val maxRevenue = revenueByDate.values.maxOrNull() ?: 1.0
    val dates = revenueByDate.keys.toList()
    val revenues = revenueByDate.values.toList()

    // Đặt chiều rộng mỗi cột và giới hạn số lượng cột hiển thị
    val barWidth = 120.dp // Tăng chiều rộng cột để có khoảng cách lớn hơn
    val maxVisibleBars = 20 // Giới hạn số cột hiển thị
    val visibleDates = dates.take(maxVisibleBars)
    val visibleRevenues = revenues.take(maxVisibleBars)
    val totalWidth = (barWidth * visibleDates.size).coerceAtLeast(100.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Canvas(
            modifier = Modifier
                .width(totalWidth)
                .height(350.dp)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            val maxHeight = size.height - 60.dp.toPx() // Chừa chỗ cho nhãn

            // Vẽ trục
            drawLine(
                start = Offset(0f, maxHeight),
                end = Offset(size.width, maxHeight),
                color = Color.Gray,
                strokeWidth = 2f
            )
            drawLine(
                start = Offset(0f, 0f),
                end = Offset(0f, maxHeight),
                color = Color.Gray,
                strokeWidth = 2f
            )

            // Vẽ các cột
            visibleRevenues.forEachIndexed { index, revenue ->
                val barHeight = (revenue / maxRevenue * maxHeight).toFloat()
                val x = index * barWidth.toPx()

                drawRect(
                    color = Color.Blue,
                    topLeft = Offset(x, maxHeight - barHeight),
                    size = Size(barWidth.toPx() * 0.6f, barHeight) // Giảm chiều rộng cột để tăng khoảng cách
                )

                // Vẽ nhãn ngày (hiển thị ngang, không xoay)
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        visibleDates[index].take(10), // Giới hạn độ dài nhãn
                        x + (barWidth.toPx() * 0.6f) / 2,
                        maxHeight + 40.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 30f // Giảm kích thước chữ để tránh chồng lấn
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }

                // Vẽ tổng tiền trên đỉnh cột
                if (revenue > 0) {
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            currencyFormatter.format(revenue),
                            x + (barWidth.toPx() * 0.6f) / 2,
                            maxHeight - barHeight - 10.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 27f // Giảm kích thước chữ cho rõ ràng
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderItem(order: Order, currencyFormatter: NumberFormat) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Mã đơn hàng: ${order.id ?: "N/A"}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Mã người dùng: ${order.userId ?: "N/A"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Tổng tiền: ${currencyFormatter.format(order.totalPrice ?: 0L)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Trạng thái: ${order.status ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall
            )
            if (order.items.isNullOrEmpty()) {
                Log.w("OrderItem", "Đơn hàng ${order.id} không có sản phẩm")
                Text(
                    text = "Không có sản phẩm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                order.items.forEach { item ->
                    Text(
                        text = "${item.title ?: "Sản phẩm không xác định"}: ${item.quantity ?: 0} x ${
                            currencyFormatter.format(item.price ?: 0L)
                        }",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}