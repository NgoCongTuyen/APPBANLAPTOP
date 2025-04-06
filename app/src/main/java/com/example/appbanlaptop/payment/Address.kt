package com.example.appbanlaptop.payment

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Address(
    val name: String,
    val phone: String,
    val addressDetail: String,
    val isDefault: Boolean
) : Parcelable