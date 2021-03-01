package com.android1001.hamking.server

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.IBinder
import androidx.annotation.BinderThread
import androidx.annotation.Nullable
import com.android1001.hamking.api.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.*
import java.util.concurrent.TimeUnit

class HamKingService : Service() {

    companion object {
        const val ACTION_BIND_LOCAL = "com.android1001.hamking.server.BIND_LOCAL"
        const val ACTION_BIND_REMOTE = "com.android1001.hamking.server.BIND_REMOTE"
    }

    private val mSaucePrices = mapOf(
        "ketchup" to 2,
        "hot_sauce" to 2,
        "white_sauce" to 3
    )

    private val mProteinPrices = mapOf(
        "beef" to 20,
        "pork" to 10,
        "tofu" to 5
    )

    private val mOrderListeners = mutableListOf<OrderListener>()
    private val mOrders = mutableListOf<OrderRecord>()

    private val mLocalService = LocalService()
    private val mRemoteService = RemoteService()

    private lateinit var mBurgerImages: List<Bitmap>

    override fun onCreate() {
        super.onCreate()
        mBurgerImages = listOf(
            BitmapFactory.decodeResource(resources, R.drawable.burger_1),
            BitmapFactory.decodeResource(resources, R.drawable.burger_2),
            BitmapFactory.decodeResource(resources, R.drawable.burger_3),
            BitmapFactory.decodeResource(resources, R.drawable.burger_4)
        )
    }

    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        return when (intent.action) {
            ACTION_BIND_REMOTE -> mRemoteService
            ACTION_BIND_LOCAL -> mLocalService
            else -> null
        }
    }

    override fun onDestroy() {
        mOrders.forEach { it.finish() }
        mOrders.clear()
    }

    private fun notifyOrderChanged() {
        mOrderListeners.forEach { it.onOrderStateChanged(mOrders) }
    }

    private fun removeDeadOrders(card: ICreditCard) {
        val ordersToRemove = mOrders.filter { it.mClient.mCreditCard == card }
        ordersToRemove.forEach { it.finish() }
        mOrders.removeAll(ordersToRemove)
        notifyOrderChanged()
    }

    inner class LocalService : Binder() {
        fun registerOrderListener(listener: OrderListener) {
            mOrderListeners.add(listener)
            listener.onOrderStateChanged(mOrders)
        }

        fun unregisterOrderListener(listener: OrderListener) {
            mOrderListeners.remove(listener)
        }
    }

    inner class RemoteService : IHamKingInterface.Stub() {

        @BinderThread
        override fun pickupOrder(session: IOrderSession, order: OrderInfo) {
            val index = mOrders.indexOf(session)
            if (index < 0) return

            val record = mOrders[index]
            if (record.mOrderReady) {
                order.mBurgerImage = record.mOrder.mBurgerImage
                record.finish()
                mOrders.remove(record)
                notifyOrderChanged()
            }
            return
        }

        @BinderThread
        override fun cancelOrder(session: IOrderSession) {
            val ordersToCancel = mOrders.filter { it == session }
            ordersToCancel.forEach { it.finish() }
            mOrders.removeAll(ordersToCancel)
            notifyOrderChanged()
        }

        @BinderThread
        override fun requestOrder(client: ClientInfo, order: OrderInfo): IOrderSession {
            val basePrice = 15;
            val proteinPrice = mProteinPrices[order.mProteinType] ?: 0
            val saucesPrice = order.mSauces.sumBy { mSaucePrices[it] ?: 0 }
            val totalPrice = basePrice + saucesPrice + proteinPrice

            client.mCreditCard.charge(totalPrice)
            // If a client died, we remove all corresponding orders
            client.mCreditCard.asBinder().linkToDeath({
                removeDeadOrders(client.mCreditCard)
            }, 0)
            order.mOrderId = UUID.randomUUID().toString()

            val orderRecord = OrderRecord(client, order)
            orderRecord.prepare()
            mOrders.add(orderRecord)
            notifyOrderChanged()

            return orderRecord
        }
    }

    inner class OrderRecord(
        val mClient: ClientInfo,
        val mOrder: OrderInfo,
        var mOrderReady: Boolean = false
    ) : IOrderSession.Stub() {
        private var mListener: IOrderListener? = null
        private var mDisposable: Disposable? = null

        @BinderThread
        override fun registerListener(listener: IOrderListener) {
            this.mListener = listener
            this.mListener?.let {
                // Tell the client that the order is ready
                if (mOrderReady) it.onOrderReady(this)
            }
        }

        fun prepare() {
            val timeToPrepare = 10L + mOrder.mSauces.size + if (mOrder.mSpicy) 1L else 0L
            mDisposable = Observable
                .timer(timeToPrepare, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    mOrderReady = true
                    // Choose a random burger image
                    mOrder.mBurgerImage = mBurgerImages[(timeToPrepare % mBurgerImages.size).toInt()]
                    notifyOrderChanged()
                    this.mListener?.onOrderReady(this)
                }
        }

        fun finish() = mDisposable?.dispose()
    }

    interface OrderListener {
        fun onOrderStateChanged(orders: List<OrderRecord>)
    }
}
