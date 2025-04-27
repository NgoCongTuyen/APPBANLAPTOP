package com.example.appbanlaptop.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC), // Giữ màu tím nhạt
    onPrimary = Color.Black,
    surface = Color(0xFF2A2E34), // Xám tối cho bề mặt (CategoryItem, ProductItem)
    surfaceVariant = Color(0xFF3B4048), // Xám trung cho CategoryItem không chọn
    onSurface = Color(0xFFE0E0E0), // Xám nhạt cho văn bản trên bề mặt
    background = Color(0xFF1C2526), // Xám đậm cho nền chính
    onBackground = Color(0xFFE0E0E0), // Xám nhạt cho văn bản trên nền
    error = Color(0xFFCF6679), // Giữ màu đỏ lỗi
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE), // Giữ màu tím
    onPrimary = Color.White,
    surface = Color(0xFFF5F5F5), // Xám nhạt
    surfaceVariant = Color(0xFFEEEEEE), // Xám rất nhạt cho CategoryItem không chọn
    onSurface = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    error = Color(0xFFB00020), // Giữ màu đỏ lỗi
)


@Composable
fun APPBANLAPTOPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun PaymentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}