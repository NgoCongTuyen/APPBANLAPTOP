package com.example.appbanlaptop.Activity

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appbanlaptop.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onSignUpClicked: () -> Unit,
    onGoogleSignInClicked: () -> Unit,
    onLogoutClicked: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            // Tiêu đề và mô tả
            Text(
                text = "ĐPT Shop",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Blue,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Ứng dụng mua hàng đơn giản và hiệu quả",
                fontSize = 14.sp,
                color = Color.Blue,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
        item {
            // Tiêu đề "Login"
            Text(
                text = "Đăng nhập",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Blue,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        item {
            // Trường nhập Email
            OutlinedTextField(
                value = viewModel.email.value,
                onValueChange = { viewModel.email.value = it },
                label = { Text("Nhập email") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.MailOutline,
                        contentDescription = "Email Icon",
                        tint = Color.Gray
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color.Blue,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = Color.Blue,
                    unfocusedLabelColor = Color.Gray
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = !viewModel.isLoading.value
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            // Trường nhập Password
            OutlinedTextField(
                value = viewModel.password.value,
                onValueChange = { viewModel.password.value = it },
                label = { Text("Mật khẩu") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password Icon",
                        tint = Color.Gray
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { viewModel.passwordVisible.value = !viewModel.passwordVisible.value }) {
                        Icon(
                            imageVector = if (viewModel.passwordVisible.value) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            tint = Color.Gray
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                visualTransformation = if (viewModel.passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color.Blue,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = Color.Blue,
                    unfocusedLabelColor = Color.Gray
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !viewModel.isLoading.value
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            Text(
                text = "Quên mật khẩu?",
                fontSize = 14.sp,
                color = Color.Blue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { viewModel.onForgetPasswordClicked() },
                textAlign = TextAlign.End
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        item {
            // Hiển thị thông báo lỗi (nếu có)
            viewModel.errorMessage.value?.let { error ->
                Text(
                    text = error,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        item {
            // Nút "Login"
            Button(
                onClick = { viewModel.onLoginClicked(onLoginSuccess) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
                enabled = !viewModel.isLoading.value && viewModel.email.value.isNotBlank() && viewModel.password.value.isNotBlank()
            ) {
                if (viewModel.isLoading.value) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "Đăng nhập",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            // Liên kết "Don't have an account? Sign up"
            Text(
                text = "Chưa có tài khoản? Đăng ký",
                fontSize = 14.sp,
                color = Color.Blue,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.onSignUpClicked(onSignUpClicked) },
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        item {
            // Phần "OR"
            Text(
                text = "HOẶC",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
        }
        item {
            // Biểu tượng mạng xã hội
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp), // Thêm padding để không sát mép
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(55.dp) // Chiều cao cố định
                        .fillMaxWidth(0.85f) // Chiếm 80% chiều rộng để kéo dài
                        .background(
                            color = Color.White, // Màu nền trắng
                            shape = RoundedCornerShape(12.dp) // Bo góc
                        )
                        .border(
                            width = 1.dp,
                            color = Color.Gray.copy(alpha = 0.2f), // Viền nhẹ
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.onSocialLoginClicked("Google", onGoogleSignInClicked) }, // Xử lý click
                    contentAlignment = Alignment.CenterStart // Căn trái nội dung
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp), // Padding bên trong
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.google),
                            contentDescription = "Google",
                            modifier = Modifier.size(35.dp) // Kích thước ảnh Google
                        )
                        Spacer(modifier = Modifier.width(14.dp)) // Khoảng cách giữa biểu tượng và chữ
                        Text(
                            text = "Đăng nhập bằng Google",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}