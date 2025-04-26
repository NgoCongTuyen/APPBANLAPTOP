package com.example.appbanlaptop.payment

import android.content.Intent
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
import androidx.compose.foundation.border
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
import com.example.appbanlaptop.Activity.ThemeManager
import com.example.appbanlaptop.MainActivity
import com.example.appbanlaptop.Model.CartItem
import com.example.appbanlaptop.R
import com.example.appbanlaptop.ui.theme.APPBANLAPTOPTheme
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
            // Sử dụng remember để theo dõi sự thay đổi của theme
            val isDarkMode = remember { mutableStateOf(ThemeManager.isDarkMode(this)) }
            
            // Cập nhật theme khi có thay đổi
            LaunchedEffect(Unit) {
                ThemeManager.isDarkMode(this@PaymentActivity).let { darkMode ->
                    if (darkMode != isDarkMode.value) {
                        isDarkMode.value = darkMode
                    }
                }
            }

            APPBANLAPTOPTheme(darkTheme = isDarkMode.value) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
                                AddAddressScreen(navController, null)
                            }
                            composable("edit_address/{index}") { backStackEntry ->
                                val index = backStackEntry.arguments?.getString("index")?.toIntOrNull()
                                val addressToEdit = index?.let { AddressState.addresses.getOrNull(it) }
                                AddAddressScreen(navController, addressToEdit)
                            }
                            composable("order_success") {
                                OrderSuccessScreen(
                                    navController = navController,
                                    products = checkoutItems,
                                    totalPrice = totalPrice
                                )
                            }
                        }
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

    val products = remember(checkoutItems) {
        mutableStateListOf<Product>().apply {
            try {
                addAll(checkoutItems.map { Product.fromCartItem(it) })
            } catch (e: Exception) {
                Log.e("PaymentScreen", "Error mapping CartItem to Product: ${e.message}", e)
            }
        }
    }

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
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (navController.context as? ComponentActivity)?.finish()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.back),
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Không có sản phẩm để thanh toán",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
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
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 8.dp)
    ) {
        TextField(
            value = selectedAddress?.let { "${it.name}\n${it.phone}\n${it.addressDetail}" } ?: "Chưa chọn địa chỉ",
            onValueChange = { /* Không cho chỉnh sửa trực tiếp */ },
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                .clickable { onAddressClick() },
            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp),
            enabled = false,
            colors = TextFieldDefaults.colors(
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
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
                title = { Text("Địa chỉ của bạn", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.back),
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
                Text("Thêm địa chỉ mới", color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(AddressState.addresses) { index, address ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color.Black,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White)
                            .clickable {
                                Log.d("AddressListScreen", "Selected address: $address")
                                navController.previousBackStackEntry?.savedStateHandle?.set("selected_address", address)
                                navController.popBackStack()
                            }
                    ) {
                        Text(
                            text = address.name,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = address.phone,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp
                        )
                        Text(
                            text = address.addressDetail,
                            color = MaterialTheme.colorScheme.onBackground,
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
                        text = "Sửa",
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
    var newName by remember { mutableStateOf(addressToEdit?.name ?: "") }
    var newPhone by remember { mutableStateOf(addressToEdit?.phone ?: "") }
    var newProvince by remember { mutableStateOf(addressToEdit?.addressDetail?.split(", ")?.getOrNull(3) ?: "") }
    var newDistrict by remember { mutableStateOf(addressToEdit?.addressDetail?.split(", ")?.getOrNull(2) ?: "") }
    var newWard by remember { mutableStateOf(addressToEdit?.addressDetail?.split(", ")?.getOrNull(1) ?: "") }
    var newAddressDetail by remember { mutableStateOf(addressToEdit?.addressDetail?.split(", ")?.getOrNull(0) ?: "") }
    var isDefault by remember { mutableStateOf(addressToEdit?.isDefault ?: false) }

    val isNameValid = newName.isNotBlank()
    val isPhoneValid = newPhone.isNotBlank() && newPhone.length == 10 && newPhone.startsWith("0") && newPhone.all { it.isDigit() }
    var isProvinceValid = newProvince.isNotBlank()
    val isDistrictValid = newDistrict.isNotBlank()
    val isWardValid = newWard.isNotBlank()
    val isAddressDetailValid = newAddressDetail.isNotBlank()
    val isFormValid = isNameValid && isPhoneValid && isProvinceValid && isDistrictValid && isWardValid && isAddressDetailValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (addressToEdit == null) "Thêm địa chỉ mới" else "Chỉnh sửa địa chỉ", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.back),
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
                        AddressState.newAddress.value = updatedAddress
                        Log.d("AddAddressScreen", "Bước 3: Đã lưu new_address vào AddressState: ${AddressState.newAddress.value}")
                        navController.popBackStack("payment", inclusive = false)
                    } else {
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
                    containerColor = if (isFormValid) Color(0xFFFF0000) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            ) {
                Text("Lưu", color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            TextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Tên", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                label = { Text("Số điện thoại", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                label = { Text("Tỉnh/Thành phố", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            if (!isProvinceValid && newProvince.isEmpty()) {
                Text(
                    text = "Vui lòng nhập Tỉnh/Thành phố",
                    color = Color(0xFFFF0000),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = newDistrict,
                onValueChange = { newDistrict = it },
                label = { Text("Quận/Huyện", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            if (!isDistrictValid && newDistrict.isEmpty()) {
                Text(
                    text = "Vui lòng nhập Quận/Huyện",
                    color = Color(0xFFFF0000),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = newWard,
                onValueChange = { newWard = it },
                label = { Text("Phường/Xã", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            if (!isWardValid && newWard.isEmpty()) {
                Text(
                    text = "Vui lòng nhập Phường/Xã",
                    color = Color(0xFFFF0000),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = newAddressDetail,
                onValueChange = { newAddressDetail = it },
                label = { Text("Địa chỉ cụ thể", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            if (!isAddressDetailValid && newAddressDetail.isEmpty()) {
                Text(
                    text = "Vui lòng nhập địa chỉ cụ thể",
                    color = Color(0xFFFF0000),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isDefault,
                    onCheckedChange = { isDefault = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFFF0000),
                        uncheckedColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text("Đặt làm mặc định", color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
fun StoreHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "APPLE STORE",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProductItem(product: Product, onQuantityChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 0.2.dp,
                    color = Color.Black,
                    shape = RoundedCornerShape(8.dp)
                )
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
                            .padding(2.dp)
                    )
                } else if (isImageError) {
                    Text(
                        text = "Lỗi",
                        fontSize = 12.sp,
                        color = Color(0xFFFF0000),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
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
                color = MaterialTheme.colorScheme.onSurface,
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
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
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
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(4.dp).clickable { onQuantityChange(product.quantity - 1) }
            )
            Text(
                text = product.quantity.toString(),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = "+",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(4.dp).clickable { onQuantityChange(product.quantity + 1) }
            )
        }
    }
}

@Composable
fun PaymentMethod() {
    var selectedMethod by remember { mutableStateOf("Thanh toán khi nhận hàng") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = "Phương thức thanh toán",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        PaymentOption("Thanh toán khi nhận hàng", selectedMethod == "Thanh toán khi nhận hàng") { selectedMethod = "Thanh toán khi nhận hàng" }
        PaymentOption("Thẻ tín dụng", selectedMethod == "Thẻ tín dụng") { selectedMethod = "Thẻ tín dụng" }
        PaymentOption("Ví Momo", selectedMethod == "Ví Momo") { selectedMethod = "Ví Momo" }
        PaymentOption("ZaloPay", selectedMethod == "ZaloPay") { selectedMethod = "ZaloPay" }
    }
}