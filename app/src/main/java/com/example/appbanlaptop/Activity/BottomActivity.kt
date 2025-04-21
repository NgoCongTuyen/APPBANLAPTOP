package com.example.appbanlaptop.Activity

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appbanlaptop.MainActivity
import com.example.appbanlaptop.R
import com.example.appbanlaptop.cart.CartScreenActivity

object BottomActivity {

    @Composable
    fun BottomMenu(
        modifier: Modifier = Modifier,
        onItemClick: () -> Unit
    ) {
        val context = LocalContext.current
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .background(
                    color = colorResource(R.color.purple),
                    shape = RoundedCornerShape(10.dp)
                ),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomMenuItem(
                icon = painterResource(R.drawable.btn_1),
                text = "Explore",
                onItemClick = {
                    if (context !is MainActivity) {
                        val intent = Intent(context, MainActivity::class.java)
                        context.startActivity(intent)
                    }
                }
            )
            BottomMenuItem(
                icon = painterResource(R.drawable.btn_2),
                text = "Cart",
                onItemClick = {
                    if (context !is CartScreenActivity) {
                        val intent = Intent(context, CartScreenActivity::class.java)
                        context.startActivity(intent)
                    }
                }
            )
            BottomMenuItem(
                icon = painterResource(R.drawable.btn_4),
                text = "Order"
            )
            BottomMenuItem(
                icon = painterResource(id = R.drawable.btn_5),
                text = "Profile",
                onItemClick = {
                    if (context !is ProfileActivity) {
                        val intent = Intent(context, ProfileActivity::class.java)
                        context.startActivity(intent)
                    }
                }
            )
        }
    }

    @Composable
    fun BottomMenuItem(
        icon: Painter,
        text: String,
        onItemClick: (() -> Unit)? = null
    ) {
        Column(
            modifier = Modifier
                .height(60.dp)
                .clickable(
                    enabled = onItemClick != null,
                    onClick = { onItemClick?.invoke() }
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = text,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
