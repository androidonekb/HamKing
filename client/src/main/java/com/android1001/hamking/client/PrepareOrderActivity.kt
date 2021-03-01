package com.android1001.hamking.client

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android1001.hamking.api.ClientInfo
import com.android1001.hamking.api.ICreditCard
import com.android1001.hamking.api.OrderInfo

class PrepareOrderActivity : AppCompatActivity() {

    companion object {
        const val ORDER_INFO = "ORDER_INFO"
        const val CLIENT_INFO = "CLIENT_INFO"

        fun getIntent(context: Context) = Intent(context, PrepareOrderActivity::class.java)
    }

    private lateinit var mCustomerNameView: EditText
    private lateinit var mCreditCardNumberView: EditText
    private lateinit var mProteinChooserSpinner: Spinner
    private lateinit var mSpicyCheckbox: CheckBox
    private lateinit var mHotSauceChooser: CheckBox
    private lateinit var mWriteSauceChooser: CheckBox
    private lateinit var mKetchupChooser: CheckBox
    private lateinit var mFinishSetupButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_prepare_order)
        initViews()
    }

    private fun initViews() {
        mCustomerNameView = findViewById(R.id.full_name_info)
        mCreditCardNumberView = findViewById(R.id.credit_card_info)
        mSpicyCheckbox = findViewById(R.id.spicy_chooser)
        mProteinChooserSpinner = findViewById(R.id.protein_chooser)
        mHotSauceChooser = findViewById(R.id.hot_sauce_chooser)
        mWriteSauceChooser = findViewById(R.id.write_sauce_chooser)
        mKetchupChooser = findViewById(R.id.ketchup_chooser)
        mFinishSetupButton = findViewById(R.id.finish_prepare_order)

        ArrayAdapter.createFromResource(
            this,
            R.array.proteins,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mProteinChooserSpinner.adapter = adapter
        }

        mFinishSetupButton.setOnClickListener {
            val customer = mCustomerNameView.text.toString()
            val creditCardNumber = mCreditCardNumberView.text.toString()
            val protein = mProteinChooserSpinner.selectedItem.toString()
            val hotSauce = mHotSauceChooser.isChecked
            val writeSauce = mWriteSauceChooser.isChecked
            val ketchupSauce = mKetchupChooser.isChecked

            if (TextUtils.isEmpty(customer) || TextUtils.isEmpty(creditCardNumber) || TextUtils.isEmpty(protein)) {
                Toast.makeText(this, getString(R.string.invalid_input), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val clientInfo = ClientInfo().apply {
                mName = customer
                mCreditCard = object : ICreditCard.Stub() {
                    var credit = 10000

                    override fun charge(amount: Int) {
                        credit -= amount
                    }

                    override fun getCardNumber() = creditCardNumber
                }
            }

            val orderInfo = OrderInfo().apply {
                mOrderId = null // Just a place holder, server will fill up the order id
                mSpicy = mSpicyCheckbox.isChecked
                mProteinType = protein
                mSauces = mutableListOf<String>().apply {
                    if (hotSauce) add("hot_sauce")
                    if (writeSauce) add("write_sauce")
                    if (ketchupSauce) add("ketchup")
                }
            }

            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(ORDER_INFO, orderInfo)
                putExtra(CLIENT_INFO, clientInfo)
            })
            finish()
        }
    }
}
