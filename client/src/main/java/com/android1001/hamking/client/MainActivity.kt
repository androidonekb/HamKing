package com.android1001.hamking.client

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.BinderThread
import androidx.recyclerview.widget.RecyclerView
import com.android1001.hamking.api.*

class MainActivity : AppCompatActivity(), ServiceConnection, PickupListener {

    companion object {
        private const val REQUEST_ORDER = 1000
    }

    private var mService: IHamKingInterface? = null

    private lateinit var mErrorView: TextView
    private lateinit var mOrdersView: RecyclerView
    private lateinit var mStartOrderView: Button
    private lateinit var mOrderAdapter: OrderAdapter

    private var mOrders = mutableListOf<ClientOrderRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        initViews()

        val bindIntent = Intent("com.android1001.hamking.server.BIND_REMOTE").apply {
            setPackage("com.android1001.hamking.server")
        }
        bindService(bindIntent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        mService?.let { service ->
            mOrders.forEach { service.cancelOrder(it.mOrderSession) }
        }
    }

    private fun initViews() {
        mErrorView = findViewById(R.id.connection_error)
        mOrdersView = findViewById(R.id.order_list)
        mOrderAdapter = OrderAdapter(this)
        mOrdersView.adapter = mOrderAdapter
        mStartOrderView = findViewById(R.id.start_order)

        mStartOrderView.setOnClickListener {
            startActivityForResult(PrepareOrderActivity.getIntent(this), REQUEST_ORDER)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_ORDER) {
            val clientInfo = data?.extras?.getParcelable<ClientInfo>(PrepareOrderActivity.CLIENT_INFO)
            val orderInfo = data?.extras?.getParcelable<OrderInfo>(PrepareOrderActivity.ORDER_INFO)

            if (clientInfo != null && orderInfo != null) {
                mService?.let { sendOrder(it, clientInfo, orderInfo) }
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        mService = null
        // Server must have lost all our orders when it dies. We clear record on client side as well.
        mOrders.clear()
        showError()
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder?) {
        service?.let {
            mService = IHamKingInterface.Stub.asInterface(service)
        }
        showUI()
    }

    override fun onPickupClicked(orderId: String) {
        mService?.let { service ->
            val index = mOrders.indexOfFirst { it.mOrderInfo.mOrderId == orderId }
            if (index >= 0) {
                val record = mOrders[index]
                service.pickupOrder(record.mOrderSession, record.mOrderInfo)
                if (record.mOrderInfo.mBurgerImage == null) {
                    // We should get back a burger image from the server..
                    Toast.makeText(this, getString(R.string.order_error), Toast.LENGTH_SHORT).show()
                } else {
                    val newItem = ClientOrderRecord(
                        mOrderSession = record.mOrderSession,
                        mOrderInfo = record.mOrderInfo,
                        mClientInfo = record.mClientInfo,
                        mStatus = OrderStatus.PICKED
                    )
                    mOrders.removeAt(index)
                    mOrders.add(index, newItem)
                    updateList()
                }
            }
        }
    }

    private fun sendOrder(service: IHamKingInterface, clientInfo: ClientInfo, orderInfo: OrderInfo) {
        val orderSession = service.requestOrder(clientInfo, orderInfo)
        orderSession.registerListener(object : IOrderListener.Stub() {

            @BinderThread
            override fun onOrderReady(session: IOrderSession) {
                val index = mOrders.indexOfFirst { it.mOrderSession.asBinder() == session.asBinder() }
                if (index >= 0) {
                    val oldItem = mOrders.removeAt(index)
                    val newOrder = ClientOrderRecord(
                        mOrderSession = oldItem.mOrderSession,
                        mOrderInfo = oldItem.mOrderInfo,
                        mClientInfo = oldItem.mClientInfo,
                        mStatus = OrderStatus.READY
                    )
                    mOrders.add(index, newOrder)
                    updateList()
                }
            }
        })
        val clientOrderRecord = ClientOrderRecord(
            mOrderSession = orderSession,
            mOrderInfo = orderInfo,
            mClientInfo = clientInfo,
            mStatus = OrderStatus.PREPARING
        )
        mOrders.add(clientOrderRecord)
        updateList()
    }

    private fun updateList() {
        val ordersForAdapter = mOrders.map {
            OrderItem(
                mOrderId = it.mOrderInfo.mOrderId,
                mCustomerName = it.mClientInfo.mName,
                mProteinType = it.mOrderInfo.mProteinType,
                mSpicy = it.mOrderInfo.mSpicy,
                mSauces = it.mOrderInfo.mSauces,
                mStatus = it.mStatus,
                mBurgerImage = it.mOrderInfo.mBurgerImage
            )
        }
        runOnUiThread { mOrderAdapter.updateItems(ordersForAdapter) }
    }

    private fun showUI() {
        mOrdersView.visibility = View.VISIBLE
        mStartOrderView.visibility = View.VISIBLE
        mErrorView.visibility = View.GONE
    }

    private fun showError() {
        mErrorView.visibility = View.VISIBLE
        mStartOrderView.visibility = View.GONE
        mOrdersView.visibility = View.GONE
    }

    inner class ClientOrderRecord(
        val mClientInfo: ClientInfo,
        val mOrderInfo: OrderInfo,
        val mOrderSession: IOrderSession,
        var mStatus: OrderStatus = OrderStatus.PREPARING
    )
}
