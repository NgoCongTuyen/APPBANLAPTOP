package com.example.appbanlaptop.Activity

import android.icu.text.CaseMap.Title
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.appbanlaptop.R
import com.example.appbanlaptop.ViewModel.MainViewModel

class ListItemsActivity : BaseActivity() {
    private val viewModel = MainViewModel()
    private var id: String = ""
    private  var title: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        id = intent.getStringExtra("id") ?: ""
        id = intent.getStringExtra("title") ?: ""

        setContent{
            ListItemSceen(
                title = title,
                onBackClick = {finish()},
                viewModel = viewModel,
                id= id
            )
        }
    }
}

@Composable
fun ListItemSceen(
    title: String,
    onBackClick : ()-> Unit,
    viewModel: MainViewModel,
    id: String
) {

}