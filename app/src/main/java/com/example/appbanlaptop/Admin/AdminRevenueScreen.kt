package com.example.appbanlaptop.Admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.appbanlaptop.Model.Order
import com.example.appbanlaptop.Model.OrderManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRevenueScreen(modifier: Modifier = Modifier) {
    val orders by OrderManager.ordersFlow.collectAsState()
    val totalRevenue by remember(orders) { derivedStateOf { orders.sumOf { it.totalPrice ?: 0.0 } } }
    val totalOrders by remember(orders) { derivedStateOf { orders.size } }
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
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
                        text = "Revenue Statistics",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Total Orders: $totalOrders",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Total Revenue: $${totalRevenue}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        items(orders, key = { it.id ?: it.hashCode().toString() }) { order ->
            OrderItem(order = order)
        }
    }
}

@Composable
fun OrderItem(order: Order) {
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
                text = "Order ID: ${order.id}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "User ID: ${order.userId}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Total: $${order.totalPrice}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Status: ${order.status}",
                style = MaterialTheme.typography.bodySmall
            )
            order.items?.forEach { item ->
                Text(
                    text = "${item.title}: ${item.quantity} x $${item.price}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}