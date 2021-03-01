package com.android1001.hamking.client

import android.graphics.Bitmap

// This class is only used by the UI
class OrderItem(
    val mOrderId: String,
    val mCustomerName: String,
    val mProteinType: String,
    val mSpicy: Boolean,
    val mSauces: List<String>,
    val mStatus: OrderStatus,
    val mBurgerImage: Bitmap?
)
