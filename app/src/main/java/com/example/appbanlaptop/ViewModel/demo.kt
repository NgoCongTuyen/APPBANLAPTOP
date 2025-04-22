//package com.example.appbanlaptop.ViewModel
//
//import android.util.Log
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import com.example.appbanlaptop.Model.CategoryModel
//import com.example.appbanlaptop.Model.SliderModel
//import com.google.firebase.Firebase
//import com.google.firebase.database.DataSnapshot
//import com.google.firebase.database.DatabaseError
//import com.google.firebase.database.FirebaseDatabase
//import com.google.firebase.database.ValueEventListener
//import com.google.firebase.database.database
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//
//class MainViewModel : ViewModel() {
//    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()
//    private  val _category = MutableLiveData<MutableList<CategoryModel>>()
//    private val _banner = MutableLiveData<List<SliderModel>>()
//
//    val banners: LiveData<List<SliderModel>> = _banner
//    val categories: MutableLiveData<MutableList<CategoryModel>> = _category
//
//    init {
//        // Khởi tạo Firebase ở đây nếu cần
//        // FirebaseApp.initializeApp(context) không thể được gọi ở đây vì không có context
//    }
//
//    fun loadBanners() {
//        val ref = firebaseDatabase.getReference("Banner")
//        ref.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val lists = mutableListOf<SliderModel>()
//
//                for (childSnapshot in snapshot.children) {
//                    val list = childSnapshot.getValue(SliderModel::class.java)
//                    if (list != null) {
//                        lists.add(list)
//                    }
//                }
//
//                _banner.value = lists
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                // Xử lý lỗi nếu cần
//            }
//        })
//    }
//
//    class CategoryViewModel : ViewModel() {
//        private val database = Firebase.database.reference
//        private val _categories = MutableStateFlow<List<CategoryModel>>(emptyList())
//        val categories: StateFlow<List<CategoryModel>> = _categories
//
//        init {
//            loadCategories()
//        }
//
//        private fun loadCategories() {
//            database.child("categories").addValueEventListener(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    val lists = snapshot.children.mapNotNull { it.getValue(CategoryModel::class.java) }
//                    _categories.value = lists
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    Log.e("Firebase", "Failed to load categories: ${error.message}")
//                }
//            })
//        }
//    }
//
//
//}

//class PaymentActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            PaymentTheme {
//                val navController = rememberNavController()
//                NavHost(navController = navController, startDestination = "payment") {
//                    composable("payment") {
//                        PaymentScreen(navController)
//                    }
//                    composable("address_list") {
//                        AddressListScreen(navController)
//                    }
//                    composable("add_address") {
//                        AddAddressScreen(navController, null) // Thêm mới
//                    }
//                    composable("edit_address/{index}") { backStackEntry ->
//                        val index = backStackEntry.arguments?.getString("index")?.toIntOrNull()
//                        val addressToEdit = index?.let { AddressState.addresses.getOrNull(it) }
//                        AddAddressScreen(navController, addressToEdit) // Chỉnh sửa
//                    }
//                }
//            }
//        }
//    }
//}
//









//@Composable
//fun PaymentScreen(navController: NavController) {
//    var selectedAddress by remember {
//        mutableStateOf(AddressState.addresses.firstOrNull { it.isDefault } ?: AddressState.addresses.firstOrNull())
//    }
//
//    val backStackEntry = navController.currentBackStackEntry
//    val selectedFromList = backStackEntry?.savedStateHandle?.get<Address>("selected_address")
//
//    LaunchedEffect(selectedFromList) {
//        selectedFromList?.let { address ->
//            Log.d("PaymentScreen", "Selected address from AddressListScreen: $address")
//            selectedAddress = address
//            backStackEntry.savedStateHandle.remove<Address>("selected_address")
//        }
//    }
//
//    Log.d("PaymentScreen", "PaymentScreen recomposed")
//
//    AddressState.newAddress.value?.let { newAddress ->
//        Log.d("PaymentScreen", "New address detected: $newAddress")
//        if (newAddress.isDefault) {
//            AddressState.addresses.forEachIndexed { index, address ->
//                AddressState.addresses[index] = address.copy(isDefault = false)
//            }
//        }
//        AddressState.addresses.add(newAddress)
//        selectedAddress = newAddress
//        AddressState.newAddress.value = null
//        Log.d("PaymentScreen", "Addresses updated: ${AddressState.addresses}")
//    }
//
//    val products = remember {
//        mutableStateListOf(
//            Product("Bàn phím cơ Gaming ZIYOU K550 V...", "Trắng Đen", "348874", 1),
//            Product("Chuột Silent Gaming Atlas F20 Không d...", "F20 Đen", "207326", 1)
//        )
//    }
//
//    val totalPrice by remember(products) {
//        derivedStateOf {
//            products.sumOf { product -> product.price.toInt() * product.quantity }
//        }
//    }
//
//    Scaffold(
//        bottomBar = {
//            TotalAndCheckoutButton(productsSize = products.size, totalPrice = totalPrice)
//        }
//    ) { paddingValues ->
//        LazyColumn(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(Color.Black)
//                .padding( bottom = paddingValues.calculateBottomPadding()),
//            //contentPadding = PaddingValues(horizontal = 16.dp)
//        ) {
//            item {
//                ShippingInfo(
//                    selectedAddress = selectedAddress,
//                    onAddressClick = {
//                        navController.navigate("address_list")
//                    }
//                )
//            }
//            item {
//                Row(modifier = Modifier.fillMaxWidth().height(2.dp)) {
//                    Box(modifier = Modifier.weight(1f).background(Color(0xFFFF0000)))
//                    Box(modifier = Modifier.weight(1f).background(Color(0xFF00C4FF)))
//                }
//            }
//            item { StoreHeader() }
//            items(products) { product ->
//                ProductItem(
//                    product = product,
//                    onQuantityChange = { newQuantity ->
//                        val index = products.indexOf(product)
//                        if (newQuantity >= 1) {
//                            products[index] = product.copy(quantity = newQuantity)
//                        }
//                    }
//                )
//            }
//            item {
//                Row(modifier = Modifier.fillMaxWidth().height(10.dp)) {
//                    Box(modifier = Modifier.weight(1f).background(Color(0xFFFF0000)))
//                    Box(modifier = Modifier.weight(1f).background(Color(0xFF00C4FF)))
//                }
//            }
//            item { PaymentMethod() }
//        }
//    }
//}
