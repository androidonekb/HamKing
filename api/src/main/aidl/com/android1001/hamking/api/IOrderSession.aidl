package com.android1001.hamking.api;

import com.android1001.hamking.api.IOrderListener;

interface IOrderSession {
    void registerListener(IOrderListener listener);
}
