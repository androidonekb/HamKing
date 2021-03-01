package com.android1001.hamking.api;

import com.android1001.hamking.api.IOrderSession;

interface IOrderListener {
    void onOrderReady(IOrderSession session);
}
