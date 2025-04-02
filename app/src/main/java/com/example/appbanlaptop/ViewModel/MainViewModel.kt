package com.example.appbanlaptop.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.appbanlaptop.Model.CategoryModel
import com.example.appbanlaptop.Model.SliderModel
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {
    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()
    private  val _category = MutableLiveData<MutableList<CategoryModel>>()
    private val _banner = MutableLiveData<List<SliderModel>>()

    val banners: LiveData<List<SliderModel>> = _banner
    val categories: MutableLiveData<MutableList<CategoryModel>> = _category

    init {
        // Khởi tạo Firebase ở đây nếu cần
        // FirebaseApp.initializeApp(context) không thể được gọi ở đây vì không có context
    }

    fun loadBanners() {
        val ref = firebaseDatabase.getReference("Banner")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lists = mutableListOf<SliderModel>()

                for (childSnapshot in snapshot.children) {
                    val list = childSnapshot.getValue(SliderModel::class.java)
                    if (list != null) {
                        lists.add(list)
                    }
                }

                _banner.value = lists
            }

            override fun onCancelled(error: DatabaseError) {
                // Xử lý lỗi nếu cần
            }
        })
    }

    class CategoryViewModel : ViewModel() {
        private val database = Firebase.database.reference
        private val _categories = MutableStateFlow<List<CategoryModel>>(emptyList())
        val categories: StateFlow<List<CategoryModel>> = _categories

        init {
            loadCategories()
        }

        private fun loadCategories() {
            database.child("categories").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lists = snapshot.children.mapNotNull { it.getValue(CategoryModel::class.java) }
                    _categories.value = lists
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Failed to load categories: ${error.message}")
                }
            })
        }
    }


}