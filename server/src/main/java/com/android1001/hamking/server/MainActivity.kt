package com.android1001.hamking.server

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import androidx.recyclerview.widget.RecyclerView
import com.android1001.hamking.server.HamKingService.LocalService

class MainActivity : AppCompatActivity(), ServiceConnection, HamKingService.OrderListener {

    private lateinit var mOrdersView: RecyclerView
    private lateinit var mOrderAdapter: OrderAdapter

    private var mService: LocalService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mOrdersView = findViewById(R.id.order_list)
        mOrderAdapter = OrderAdapter()
        mOrdersView.adapter = mOrderAdapter

        val bindIntent = Intent(this, HamKingService::class.java).apply {
            action = HamKingService.ACTION_BIND_LOCAL
        }
        bindService(bindIntent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        mService?.unregisterOrderListener(this)
    }

    override fun onServiceDisconnected(name: ComponentName) = Unit

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        mService = service as LocalService
        mService?.registerOrderListener(this)
    }

    override fun onOrderStateChanged(orders: List<HamKingService.OrderRecord>) {
        val orderItems = orders.map {
            OrderItem(
                mOrderId = it.mOrder.mOrderId,
                mCustomerName = it.mClient.mName,
                mCreditCardNumber = it.mClient.mCreditCard.cardNumber,
                mProteinType = it.mOrder.mProteinType,
                mSpicy = it.mOrder.mSpicy,
                mSauces = it.mOrder.mSauces,
                mBurgerImage = it.mOrder.mBurgerImage
            )
        }
        runOnUiThread { mOrderAdapter.updateItems(orderItems) }
    }
}
