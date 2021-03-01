package com.android1001.hamking.api;

import android.graphics.Bitmap;

parcelable OrderInfo {
    @nullable String mOrderId;
    String mProteinType;
    boolean mSpicy;
    List<String> mSauces;
    /** For a new order this field is null. After an order is ready, this field is set to a random
      * hamburger image to mimic a prepared hamburger
      */
    @nullable Bitmap mBurgerImage;
}
