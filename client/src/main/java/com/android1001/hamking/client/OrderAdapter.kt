package com.android1001.hamking.client

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView

class OrderAdapter(
    private val mPickupListener: PickupListener
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    private val mOrders = mutableListOf<OrderItem>()

    @UiThread
    fun updateItems(items: List<OrderItem>) {
        mOrders.clear()
        mOrders.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemView = layoutInflater.inflate(R.layout.list_item_order, parent, false)
        return OrderViewHolder(itemView, mPickupListener)
    }

    override fun getItemCount() = mOrders.size

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) =
        holder.bind(mOrders[position])

    class OrderViewHolder(
        itemView: View, private val mPickupListener: PickupListener
    ) : RecyclerView.ViewHolder(itemView) {
        private val mInfoView: TextView = itemView.findViewById(R.id.order_info)
        private val mBurgerView: ImageView = itemView.findViewById(R.id.order_image)
        private val mPickupView: Button = itemView.findViewById(R.id.pickup_order)

        fun bind(orderItem: OrderItem) {
            val infoBuilder = StringBuilder("Order ID: ${orderItem.mOrderId}\n")
            infoBuilder.append("Customer name: ${orderItem.mCustomerName}\n")
            infoBuilder.append("Protein type: ${orderItem.mProteinType}\n")
            infoBuilder.append("Spicy: ${if (orderItem.mSpicy) "yes" else "no"}\n")
            infoBuilder.append("Sauces: ${orderItem.mSauces.map { "$it " }}\n")
            infoBuilder.append("Status: ${orderItem.mStatus.state}\n")

            mInfoView.text = infoBuilder.toString()

            if (orderItem.mBurgerImage == null) {
                mBurgerView.visibility = View.GONE
            } else {
                mBurgerView.visibility = View.VISIBLE
                mBurgerView.setImageBitmap(orderItem.mBurgerImage)
            }

            mPickupView.visibility = if (orderItem.mStatus == OrderStatus.READY) View.VISIBLE else View.GONE
            mPickupView.setOnClickListener {
                mPickupListener.onPickupClicked(orderId = orderItem.mOrderId)
            }
        }
    }
}
