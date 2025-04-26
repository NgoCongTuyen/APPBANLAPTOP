package com.example.appbanlaptop.payment

import android.os.Bundle
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appbanlaptop.ui.theme.PaymentTheme
import java.text.DecimalFormat
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.room.util.copy
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.appbanlaptop.Model.CartItem
import com.example.appbanlaptop.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

// Định nghĩa nguồn dữ liệu chung
object AddressState {
    val newAddress = mutableStateOf<Address?>(null)
    val addresses = mutableStateListOf(
        Address("Nguyễn Thanh Đức", "(+84)03****3838", "59/22C Mã Lò, Bình Trị Đông A, Bình Tân, Hồ Chí Minh, Việt Nam", true),
        Address("Hương", "(+84)84****30", "ngõ 87 ngách 43 số nhà 10, Yên Nghĩa, Hà Đông, Hà Nội, Việt Nam", false),
        Address("Mai Hương", "(+84)84****30", "số 1 ngõ 25, thôn Bảo Lộc 4, Võng Xuyên, Phúc Thọ, Hà Nội, Việt Nam", false)
    )
}

class PaymentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Nhận dữ liệu từ Intent
        val checkoutItems = intent.getParcelableArrayListExtra<CartItem>("CHECKOUT_ITEMS") ?: emptyList()
        val totalPrice = intent.getDoubleExtra("TOTAL_PRICE", 0.0)
        Log.d("PaymentActivity", "Received items: size=${checkoutItems.size}, items=$checkoutItems, totalPrice=$totalPrice")

        setContent {
            PaymentTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "payment") {
                    composable("payment") {
                        PaymentScreen(navController, checkoutItems, totalPrice)
                    }
                    composable("address_list") {
                        AddressListScreen(navController)
                    }
                    composable("add_address") {
                        AddAddressScreen(navController, null) // Thêm mới
                    }
                    composable("edit_address/{index}") { backStackEntry ->
                        val index = backStackEntry.arguments?.getString("index")?.toIntOrNull()
                        val addressToEdit = index?.let { AddressState.addresses.getOrNull(it) }
                        AddAddressScreen(navController, addressToEdit) // Chỉnh sửa
                    }
                    composable("order_success") {
                        OrderSuccessScreen(
                            navController = navController,
                            products = checkoutItems,
                            totalPrice = totalPrice,
                            address = AddressState.addresses.firstOrNull { it.isDefault }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(navController: NavController, checkoutItems: List<CartItem>, totalPriceFromIntent: Double) {
    var selectedAddress by remember {
        mutableStateOf(AddressState.addresses.firstOrNull { it.isDefault } ?: AddressState.addresses.firstOrNull())
    }

    val backStackEntry = navController.currentBackStackEntry
    val selectedFromList = backStackEntry?.savedStateHandle?.get<Address>("selected_address")

    LaunchedEffect(selectedFromList) {
        selectedFromList?.let { address ->
            Log.d("PaymentScreen", "Selected address from AddressListScreen: $address")
            selectedAddress = address
            backStackEntry.savedStateHandle.remove<Address>("selected_address")
        }
    }

    AddressState.newAddress.value?.let { newAddress ->
        Log.d("PaymentScreen", "New address detected: $newAddress")
        if (newAddress.isDefault) {
            AddressState.addresses.forEachIndexed { index, address ->
                AddressState.addresses[index] = address.copy(isDefault = false)
            }
        }
        AddressState.addresses.add(newAddress)
        selectedAddress = newAddress
        AddressState.newAddress.value = null
    }

    // Chuyển đổi CartItem sang Product
    val products = remember(checkoutItems) {
        mutableStateListOf<Product>().apply {
            try {
                addAll(checkoutItems.map { Product.fromCartItem(it) })
            } catch (e: Exception) {
                Log.e("PaymentScreen", "Error mapping CartItem to Product: ${e.message}", e)
            }
        }
    }

    // Tính tổng giá
    val totalPrice by remember(products) {
        derivedStateOf {
            products.sumOf { product ->
                try {
                    product.price.toInt() * product.quantity
                } catch (e: NumberFormatException) {
                    Log.e("PaymentScreen", "Error parsing price for product: ${product.name}, price=${product.price}", e)
                    0
                }
            }.toDouble()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Payment",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (navController.context as? ComponentActivity)?.finish()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            TotalAndCheckoutButton(
                productsSize = products.size,
                totalPrice = totalPrice,
                products = products,
                selectedAddress = selectedAddress,
                navController = navController
            )
        }
    ) { paddingValues ->
        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Không có sản phẩm để thanh toán",
                    color = Color.Black,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(paddingValues),
            ) {
                item {
                    ShippingInfo(
                        selectedAddress = selectedAddress,
                        onAddressClick = {
                            navController.navigate("address_list")
                        }
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                        Box(modifier = Modifier.weight(1f).background(Color(0xFFFF0000)))
                        Box(modifier = Modifier.weight(1f).background(Color(0xFF00C4FF)))
                    }
                }
                item { StoreHeader() }
                items(products) { product ->
                    ProductItem(
                        product = product,
                        onQuantityChange = { newQuantity ->
                            val index = products.indexOf(product)
                            if (newQuantity >= 1) {
                                products[index] = product.copy(quantity = newQuantity)
                            }
                        }
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                        Box(modifier = Modifier.weight(1f).background(Color(0xFFFF0000)))
                        Box(modifier = Modifier.weight(1f).background(Color(0xFF00C4FF)))
                    }
                }
                item { PaymentMethod() }
            }
        }
    }
}

@Composable
fun ShippingInfo(selectedAddress: Address?, onAddressClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(bottom = 8.dp)
    ) {
        TextField(
            value = selectedAddress?.let { "${it.name}\n${it.phone}\n${it.addressDetail}" } ?: "Chưa chọn địa chỉ",
            onValueChange = { /* Không cho chỉnh sửa trực tiếp */ },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
                .clickable { onAddressClick() },
            textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp),
            enabled = false,
            colors = TextFieldDefaults.colors(
                disabledContainerColor = Color(0xFF1C2526),
                disabledTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressListScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Địa chỉ của bạn", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("<", color = Color.White, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        bottomBar = {
            Button(
                onClick = { navController.navigate("add_address") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000))
            ) {
                Text("Thêm địa chỉ mới", color = Color.White, fontSize = 16.sp)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues)
        ) {
            itemsIndexed(AddressState.addresses) { index, address ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                Log.d("AddressListScreen", "Selected address: $address")
                                navController.previousBackStackEntry?.savedStateHandle?.set("selected_address", address)
                                navController.popBackStack()
                            }
                    ) {
                        Text(
                            text = address.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = address.phone,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = address.addressDetail,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        if (address.isDefault) {
                            Text(
                                text = "Mặc định",
                                color = Color(0xFFFF0000),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Text(
                        text = "Chỉnh sửa",
                        color = Color(0xFFFF0000),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable {
                                Log.d("AddressListScreen", "Edit address clicked: $address")
                                navController.navigate("edit_address/$index")
                            }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAddressScreen(navController: NavController, addressToEdit: Address?) {
    // Nếu addressToEdit không null, điền thông tin hiện tại để chỉnh sửa
    var newName by remember { mutableStateOf(addressToEdit?.name ?: "") }
    var newPhone by remember { mutableStateOf(addressToEdit?.phone ?: "") }
    var newProvince by remember { mutableStateOf(addressToEdit?.addressDetail?.split(", ")?.getOrNull(3) ?: "") }
    var newDistrict by remember { mutableStateOf(addressToEdit?.addressDetail?.split(", ")?.getOrNull(2) ?: "") }
    var newWard by remember { mutableStateOf(addressToEdit?.addressDetail?.split(", ")?.getOrNull(1) ?: "") }
    var newAddressDetail by remember { mutableStateOf(addressToEdit?.addressDetail?.split(", ")?.getOrNull(0) ?: "") }
    var isDefault by remember { mutableStateOf(addressToEdit?.isDefault ?: false) }

    val isNameValid = newName.isNotBlank()
    val isPhoneValid = newPhone.isNotBlank() && newPhone.length == 10 && newPhone.startsWith("0") && newPhone.all { it.isDigit() }
    val isProvinceValid = newProvince.isNotBlank()
    val isDistrictValid = newDistrict.isNotBlank()
    val isWardValid = newWard.isNotBlank()
    val isAddressDetailValid = newAddressDetail.isNotBlank()
    val isFormValid = isNameValid && isPhoneValid && isProvinceValid && isDistrictValid && isWardValid && isAddressDetailValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (addressToEdit == null) "Thêm địa chỉ mới" else "Chỉnh sửa địa chỉ", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("<", color = Color.White, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    Log.d("AddAddressScreen", "Bước 1: Nút Lưu được nhấn")
                    val updatedAddress = Address(
                        name = newName,
                        phone = newPhone,
                        addressDetail = "$newAddressDetail, $newWard, $newDistrict, $newProvince",
                        isDefault = isDefault
                    )
                    Log.d("AddAddressScreen", "Bước 2: Địa chỉ đã cập nhật: $updatedAddress")
                    if (addressToEdit == null) {
                        // Thêm mới
                        AddressState.newAddress.value = updatedAddress
                        Log.d("AddAddressScreen", "Bước 3: Đã lưu new_address vào AddressState: ${AddressState.newAddress.value}")
                        navController.popBackStack("payment", inclusive = false)
                    } else {
                        // Chỉnh sửa
                        val index = AddressState.addresses.indexOf(addressToEdit)
                        if (index != -1) {
                            if (updatedAddress.isDefault) {
                                AddressState.addresses.forEachIndexed { i, addr ->
                                    AddressState.addresses[i] = addr.copy(isDefault = false)
                                }
                            }
                            AddressState.addresses[index] = updatedAddress
                            Log.d("AddAddressScreen", "Bước 3: Đã cập nhật địa chỉ tại index $index: ${AddressState.addresses}")
                            navController.popBackStack("address_list", inclusive = false)
                        }
                    }
                    Log.d("AddAddressScreen", "Bước 4: Đã gọi popBackStack")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFormValid) Color(0xFFFF0000) else Color.Gray,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text("Lưu", color = Color.White, fontSize = 16.sp)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            TextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Tên", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1C2526),
                    unfocusedContainerColor = Color(0xFF1C2526),
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.White
                )
            )
            if (!isNameValid && newName.isEmpty()) {
                Text(
                    text = "Vui lòng nhập tên",
                    color = Color(0xFFFF0000),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = newPhone,
                onValueChange = { newPhone = it },
                label = { Text("Số điện thoại", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1C2526),
                    unfocusedContainerColor = Color(0xFF1C2526),
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.White
                )
            )
            if (!isPhoneValid) {
                Text(
                    text = when {
                        newPhone.isEmpty() -> "Vui lòng nhập số điện thoại"
                        newPhone.length != 10 || !newPhone.startsWith("0") || !newPhone.all { it.isDigit() } -> "Số điện thoại không hợp lệ"
                        else -> ""
                    },
                    color = Color(0xFFFF0000),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = newProvince,
                onValueChange = { newProvince = it },
                label = { Text("Tỉnh/Thành phố", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1C2526),
                    unfocusedContainerColor = Color(0xFF1C2526),
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = newDistrict,
                onValueChange = { newDistrict = it },
                label = { Text("Quận/Huyện", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1C2526),
                    unfocusedContainerColor = Color(0xFF1C2526),
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = newWard,
                onValueChange = { newWard = it },
                label = { Text("Phường/Xã", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1C2526),
                    unfocusedContainerColor = Color(0xFF1C2526),
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = newAddressDetail,
                onValueChange = { newAddressDetail = it },
                label = { Text("Địa chỉ cụ thể", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1C2526),
                    unfocusedContainerColor = Color(0xFF1C2526),
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isDefault,
                    onCheckedChange = { isDefault = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFFF0000),
                        uncheckedColor = Color.White
                    )
                )
                Text("Đặt làm mặc định", color = Color.White)
            }
        }
    }
}

@Composable
fun StoreHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF1C2526)).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "APPLE STORE", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProductItem(product: Product, onQuantityChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C2526))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hiển thị hình ảnh sản phẩm
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        )
        {
            if (product.imageUrl != null && product.imageUrl.isNotEmpty()) {
                var isImageLoading by remember(product.imageUrl) { mutableStateOf(true) }
                var isImageError by remember(product.imageUrl) { mutableStateOf(false) }

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit,
                    placeholder = painterResource(R.drawable.loadding),
                    error = painterResource(R.drawable.error),
                    onLoading = { isImageLoading = true },
                    onSuccess = { isImageLoading = false; isImageError = false },
                    onError = { isImageLoading = false; isImageError = true }
                )

                if (isImageLoading) {
                    Text(
                        text = "Đang tải...",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.7f))
                            .padding(2.dp)
                    )
                } else if (isImageError) {
                    Text(
                        text = "Lỗi",
                        fontSize = 12.sp,
                        color = Color.Red,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.7f))
                            .padding(2.dp)
                    )
                }
            } else {
                Image(
                    painter = painterResource(id = R.drawable.cat1),
                    contentDescription = "Default Product Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.name,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.color,
                color = Color(0xFFFFD700),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.background(Color(0xFF3A3A3A), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Text(
                text = "${DecimalFormat("#,###").format(product.price.toInt())}đ",
                color = Color(0xFFFF0000),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = "-",
                color = Color.White,
                modifier = Modifier.padding(4.dp).clickable { onQuantityChange(product.quantity - 1) }
            )
            Text(
                text = product.quantity.toString(),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = "+",
                color = Color.White,
                modifier = Modifier.padding(4.dp).clickable { onQuantityChange(product.quantity + 1) }
            )
        }
    }
}

@Composable
fun PaymentMethod() {
    var selectedMethod by remember { mutableStateOf("Thanh toán khi nhận hàng") }
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1C2526)).padding(16.dp)) {
        Text(text = "Phương thức thanh toán", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        PaymentOption("Thanh toán khi nhận hàng", selectedMethod == "Thanh toán khi nhận hàng") { selectedMethod = "Thanh toán khi nhận hàng" }
        PaymentOption("Thẻ tín dụng", selectedMethod == "Thẻ tín dụng") { selectedMethod = "Thẻ tín dụng" }
        PaymentOption("Ví Momo", selectedMethod == "Ví Momo") { selectedMethod = "Ví Momo" }
        PaymentOption("ZaloPay", selectedMethod == "ZaloPay") { selectedMethod = "ZaloPay" }
    }
}

@Composable
fun PaymentOption(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF3A3A3A) else Color.Transparent)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            color = if (isSelected) Color(0xFFFF0000) else Color.White,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun TotalAndCheckoutButton(
    productsSize: Int,
    totalPrice: Double,
    products: List<Product>,
    selectedAddress: Address?,
    navController: NavController
) {
    val formatter = DecimalFormat("#,###")
    val formattedTotal = "${formatter.format(totalPrice.toInt())}đ"
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C2526))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = "$productsSize mặt hàng, tổng cộng", color = Color.White, fontSize = 14.sp)
            Text(text = formattedTotal, color = Color(0xFFFF0000), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = {
                // Kiểm tra xem đã chọn địa chỉ chưa
                if (selectedAddress == null) {
                    android.widget.Toast.makeText(context, "Vui lòng chọn địa chỉ giao hàng", android.widget.Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // Lấy userId từ Firebase Authentication
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId == null) {
                    android.widget.Toast.makeText(context, "Vui lòng đăng nhập để đặt hàng", android.widget.Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // Tạo dữ liệu đơn hàng
                val order = mapOf(
                    "address" to mapOf(
                        "name" to selectedAddress.name,
                        "phone" to selectedAddress.phone,
                        "addressDetail" to selectedAddress.addressDetail,
                        "isDefault" to selectedAddress.isDefault
                    ),
                    "products" to products.map { product ->
                        mapOf(
                            "name" to product.name,
                            "color" to product.color,
                            "price" to product.price,
                            "quantity" to product.quantity,
                            "imageUrl" to product.imageUrl
                        )
                    },
                    "totalPrice" to totalPrice,
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis()
                )

                // Lưu vào Realtime Database
                val database = FirebaseDatabase.getInstance()
                val ordersRef = database.getReference("orders").child(userId)
                val newOrderRef = ordersRef.push() // Tạo ID duy nhất cho đơn hàng

                newOrderRef.setValue(order)
                    .addOnSuccessListener {
                        // Xóa các sản phẩm đã thanh toán khỏi giỏ hàng
                        val cartRef = database.getReference("Cart").child(userId).child("items")
                        products.forEach { product ->
                            cartRef.orderByChild("title").equalTo(product.name)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        for (itemSnapshot in snapshot.children) {
                                            itemSnapshot.ref.removeValue()
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e("TotalAndCheckoutButton", "Error removing cart item: ${error.message}")
                                    }
                                })
                        }
                        
                        android.widget.Toast.makeText(context, "Đặt hàng thành công!", android.widget.Toast.LENGTH_SHORT).show()
                        navController.navigate("order_success")
                    }
                    .addOnFailureListener { e ->
                        android.widget.Toast.makeText(context, "Lỗi khi đặt hàng: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        Log.e("TotalAndCheckoutButton", "Error saving order: ${e.message}", e)
                    }
            },
            modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000))
        ) {
            Text(text = "Đặt hàng", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderSuccessScreen(
    navController: NavController,
    products: List<CartItem>,
    totalPrice: Double,
    address: Address?
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đặt hàng thành công", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.successful),
                contentDescription = "Success",
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(100.dp)
                    .padding(16.dp)
            )

            Text(
                text = "Đặt hàng thành công!",
                color = Color(0xFF00C853),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2526))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Chi tiết đơn hàng",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    address?.let {
                        Text(
                            text = "Địa chỉ giao hàng:",
                            color = Color(0xFFAAAAAA),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${it.name}\n${it.phone}\n${it.addressDetail}",
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    Text(
                        text = "Sản phẩm:",
                        color = Color(0xFFAAAAAA),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    products.forEach { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${product.title} x${product.quantity}",
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${DecimalFormat("#,###").format(product.price.toInt() * product.quantity)}đ",
                                color = Color(0xFFFF0000),
                                fontSize = 14.sp
                            )
                        }
                    }

                    Divider(
                        color = Color(0xFF3A3A3A),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tổng cộng:",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${DecimalFormat("#,###").format(totalPrice.toInt())}đ",
                            color = Color(0xFFFF0000),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Button(
                onClick = {
                    (navController.context as? ComponentActivity)?.finish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000))
            ) {
                Text(
                    text = "Trở về trang chủ",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}