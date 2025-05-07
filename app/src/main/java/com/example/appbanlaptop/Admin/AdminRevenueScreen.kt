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

    // Lấy tháng và năm hiện tại
    val currentCalendar = Calendar.getInstance()
    val currentMonthYear = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(currentCalendar.time)

    // Tính doanh thu theo ngày trong tháng hiện tại
    val revenueByDay by remember(orders, currentMonthYear) {
        derivedStateOf {
            val revenueMap = mutableMapOf<String, Double>()
            val dayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val monthFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())

            orders.forEach { order ->
                val orderDate = order.createdAt?.let {
                    try {
                        Date(it)
                    } catch (e: Exception) {
                        Log.e("AdminRevenueScreen", "Invalid date format for order ${order.id}: ${order.createdAt}")
                        null
                    }
                } ?: return@forEach

                // Kiểm tra nếu đơn hàng thuộc tháng hiện tại
                if (monthFormat.format(orderDate) == currentMonthYear) {
                    val day = dayFormat.format(orderDate)
                    val price = order.totalPrice?.toDouble() ?: 0.0
                    revenueMap[day] = (revenueMap[day] ?: 0.0) + price
                }
            }

            revenueMap.entries.sortedBy {
                val calendar = Calendar.getInstance()
                try {
                    dayFormat.parse(it.key)?.let { date -> calendar.time = date }
                } catch (e: ParseException) {
                    Log.e("AdminRevenueScreen", "Failed to parse date: ${it.key}")
                }
                calendar.timeInMillis
            }.associate { it.key to it.value }
        }
    }

    // Tính doanh thu theo tháng
    val revenueByMonth by remember(orders) {
        derivedStateOf {
            val revenueMap = mutableMapOf<String, Double>()
            val monthFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())

            orders.forEach { order ->
                val monthYear = order.createdAt?.let {
                    try {
                        monthFormat.format(Date(it))
                    } catch (e: Exception) {
                        Log.e("AdminRevenueScreen", "Invalid date format for order ${order.id}: ${order.createdAt}")
                        null
                    }
                } ?: return@forEach
                val price = order.totalPrice?.toDouble() ?: 0.0
                revenueMap[monthYear] = (revenueMap[monthYear] ?: 0.0) + price
            }

            revenueMap.entries.sortedBy {
                val calendar = Calendar.getInstance()
                try {
                    monthFormat.parse(it.key)?.let { date -> calendar.time = date }
                } catch (e: ParseException) {
                    Log.e("AdminRevenueScreen", "Failed to parse date: ${it.key}")
                }
                calendar.timeInMillis
            }.associate { it.key to it.value }
        }
    }

    // Log dữ liệu
    LaunchedEffect(orders) {
        if (orders.isNotEmpty()) {
            Log.d("AdminRevenueScreen", "Orders loaded: ${orders.size}")
            orders.take(5).forEach { order ->
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
            // Biểu đồ doanh thu theo ngày trong tháng hiện tại
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Doanh thu theo ngày ($currentMonthYear)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (revenueByDay.isEmpty()) {
                        Text(
                            text = "Không có dữ liệu doanh thu trong tháng này",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        DailyBarChart(revenueByDay = revenueByDay, currencyFormatter = currencyFormatter)
                    }
                }
            }
        }

        item {
            // Biểu đồ doanh thu theo tháng
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Doanh thu theo tháng",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (revenueByMonth.isEmpty()) {
                        Text(
                            text = "Không có dữ liệu doanh thu",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        MonthlyBarChart(revenueByMonth = revenueByMonth, currencyFormatter = currencyFormatter)
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
fun DailyBarChart(revenueByDay: Map<String, Double>, currencyFormatter: NumberFormat) {
    val maxRevenue = revenueByDay.values.maxOrNull() ?: 1.0
    val days = revenueByDay.keys.toList()
    val revenues = revenueByDay.values.toList()

    // Đặt chiều rộng mỗi cột và giới hạn số lượng cột hiển thị
    val barWidth = 80.dp // Cột hẹp hơn vì có nhiều ngày
    val maxVisibleBars = 31 // Tối đa 31 ngày trong tháng
    val visibleDays = days.take(maxVisibleBars)
    val visibleRevenues = revenues.take(maxVisibleBars)
    val totalWidth = (barWidth * visibleDays.size).coerceAtLeast(100.dp)

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
                    color = Color.Green, // Màu khác để phân biệt
                    topLeft = Offset(x, maxHeight - barHeight),
                    size = Size(barWidth.toPx() * 0.6f, barHeight)
                )

                // Vẽ nhãn ngày
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        visibleDays[index].substring(0, 2), // Chỉ lấy ngày (dd)
                        x + (barWidth.toPx() * 0.6f) / 2,
                        maxHeight + 40.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 24f // Nhỏ hơn vì nhiều nhãn
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
                                textSize = 22f
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
fun MonthlyBarChart(revenueByMonth: Map<String, Double>, currencyFormatter: NumberFormat) {
    val maxRevenue = revenueByMonth.values.maxOrNull() ?: 1.0
    val months = revenueByMonth.keys.toList()
    val revenues = revenueByMonth.values.toList()

    // Đặt chiều rộng mỗi cột và giới hạn số lượng cột hiển thị
    val barWidth = 120.dp
    val maxVisibleBars = 12 // Tối đa 12 tháng
    val visibleMonths = months.take(maxVisibleBars)
    val visibleRevenues = revenues.take(maxVisibleBars)
    val totalWidth = (barWidth * visibleMonths.size).coerceAtLeast(100.dp)

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
                    size = Size(barWidth.toPx() * 0.6f, barHeight)
                )

                // Vẽ nhãn tháng/năm
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        visibleMonths[index],
                        x + (barWidth.toPx() * 0.6f) / 2,
                        maxHeight + 40.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 30f
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
                                textSize = 24f
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