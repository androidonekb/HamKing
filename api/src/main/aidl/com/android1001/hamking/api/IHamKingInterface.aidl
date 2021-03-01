package com.android1001.hamking.api;

import com.android1001.hamking.api.ClientInfo;
import com.android1001.hamking.api.ICreditCard;
import com.android1001.hamking.api.IOrderSession;
import com.android1001.hamking.api.OrderInfo;

interface IHamKingInterface {
    IOrderSession requestOrder(in ClientInfo client, inout OrderInfo order);
    void cancelOrder(in IOrderSession session);
    void pickupOrder(IOrderSession session, inout OrderInfo order);
}
