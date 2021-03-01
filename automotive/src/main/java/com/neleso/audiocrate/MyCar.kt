package com.neleso.audiocrate

import android.car.Car
import android.car.CarInfoManager
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import java.util.*

interface MyCarListener {
    fun onChangedProperty(vehilcePropertyId: Int, value: Any)
}

object MyCar {
    private const val TAG = "MyCar"
    private const val UUID_KEY = "TheUuid"

    private var myCar: Car? = null

    private var carPropertyManager: CarPropertyManager? = null
    private var carInfoManager : CarInfoManager? = null

    val model : String
        get() {
            return carInfoManager?.model ?: "generic"
        }

    val evPortLocation : Int
        get() {
            return carInfoManager?.evPortLocation ?: 99
        }

    val carId: String
        get() {
            var uuid = MySharedPreferences.getValueString(UUID_KEY)
            if (uuid == null) {
                uuid = UUID.randomUUID().toString()
                try {
                    MySharedPreferences.save(UUID_KEY, uuid)
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "SharedPreferences have to be initialized")
                    throw e
                }
            }
            return uuid
        }

    private var delegate: MyCarListener? = null

    private fun createCallback(callback: MyCarListener) : CarPropertyManager.CarPropertyEventCallback {
        val theCallback = object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<Any>) {
                val id = value.propertyId
                val myval = value.value
                Log.d(TAG, "OnEvent changed: $myval for key: $id")
                callback.onChangedProperty(id, myval)
            }
            override fun onErrorEvent(propId: Int, zone: Int) {
                Log.w("Settings", "Received error car property event (chargeRate), propId=$propId")
            }
        }
        return theCallback
    }

    fun createCar(context: Context) {
        if (myCar == null) {
            Log.d(TAG, "Create car singelton")
            this.myCar = Car.createCar(context)
            carPropertyManager = this.myCar!!.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            carInfoManager = this.myCar!!.getCarManager(Car.INFO_SERVICE) as CarInfoManager
        }
    }

    fun register(delegate: MyCarListener, listOfCarProperties: Map<String, Int>) {
        this.delegate = delegate
        if (carPropertyManager != null) {
            for ((k, vehicleId) in listOfCarProperties) {
                Log.d(TAG, "Callback for <$k> - ")
                if (delegate != null) {
                    val myCallback = createCallback(this.delegate!!)
                    carPropertyManager!!.registerCallback(
                        myCallback,
                        vehicleId,
                        CarPropertyManager.SENSOR_RATE_ONCHANGE
                    )
                    Log.d(TAG, "registered")
                } else {
                    throw RuntimeException("Delegate for car onEventChanged listener is missing")
                }
            }
        }
    }
}







