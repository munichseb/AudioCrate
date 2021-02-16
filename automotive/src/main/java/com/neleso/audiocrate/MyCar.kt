package com.neleso.audiocrate

import android.car.Car
import android.car.CarInfoManager
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import java.net.URL

object MyCar {

    // private var car: Car
    private val permissions = arrayOf(Car.PERMISSION_ENERGY, Car.PERMISSION_POWERTRAIN)
    private var carPropertyManager: CarPropertyManager? = null
    private var car_range : Double = 0.0
    private var rangeValueLast : Double = 0.0

    private var chargingValue : Double = 0.0
    private var chargingValueLast : Double = 0.0
    private var vehicleSpeed : Int = 0
    private var wheel_ticks : Int = 0
    private var chargeRate : Int = 0

    private var carInfoManager : CarInfoManager? = null

    public val model : String
    get() {

        return carInfoManager?.model ?: "generic"
    }

    public val evPortLocation : Int
        get() {
            return carInfoManager?.evPortLocation ?: 99
        }

    var my_uuid = "---"



    private var carPropertyListenerRANGE = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<Any>) {
            var myval = value.value
            car_range = myval.toString().toDouble()

            if (rangeValueLast != car_range) {
                rangeValueLast = car_range
                URL("https://trelp.datacar.io/datacar/callback.php?uuid=" + my_uuid + "&m=" + car_range + "&t=range").readText()
            }
        }
        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w("Settings", "Received error car property event (Range), propId=$propId")
        }
    }

    private var carPropertyListenerCharging = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<Any>) {
            var myval = value.value
            chargingValue = myval.toString().toDouble()

            if (chargingValueLast != chargingValue) {
                chargingValueLast = chargingValue
                URL("https://trelp.datacar.io/datacar/callback.php?uuid=" + my_uuid + "&m=" + chargingValue + "&t=charging").readText()
            }
        }
        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w("Settings", "Received error car property event (Range), propId=$propId")
        }
    }

    private var carPropertyListenerWheelTick = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<Any>) {
            var myval = value.value
            wheel_ticks = myval.toString().toInt()
        }
        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w("Settings", "Received error car property event (wheelTick), propId=$propId")
        }
    }

    private var carPropertyListenerSpeed = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<Any>) {
            var myval = value.value
            vehicleSpeed = myval.toString().toInt()
        }
        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w("Settings", "Received error car property event (vehicleSpeed), propId=$propId")
        }
    }

    private var carPropertyListenerChargeRate = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<Any>) {
            var myval = value.value
            chargeRate = myval.toString().toInt()
        }
        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w("Settings", "Received error car property event (chargeRate), propId=$propId")
        }
    }


    fun createCar(context: Context, uuid_local: String) {
        my_uuid = uuid_local;
        val car = Car.createCar(context)
        carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
        carInfoManager = car.getCarManager(Car.INFO_SERVICE) as CarInfoManager
    }

    fun fireAndForget() {

        Log.w("Settings", "Sending RANGE " + car_range + " Update to home server (" + my_uuid + ")")
        URL("https://trelp.datacar.io/datacar/callback.php?uuid=" + my_uuid + "&m=" + car_range + "&t=range").readText()

        Log.w("Settings", "Sending WHEEL_TICK " + wheel_ticks + " Update to home server (" + my_uuid + ")")
        URL("https://trelp.datacar.io/datacar/callback.php?uuid=" + my_uuid + "&m=" + wheel_ticks + "&t=wheel_ticks").readText()

    }


    fun register() {

        if (carPropertyManager != null) {

            // Range -----------------------------------------------
            carPropertyManager!!.registerCallback(
                carPropertyListenerRANGE,
                VehiclePropertyIds.RANGE_REMAINING,
                CarPropertyManager.SENSOR_RATE_ONCHANGE
            )

            // Wheel Tick -----------------------------------------------
            carPropertyManager!!.registerCallback(
                carPropertyListenerWheelTick,
                VehiclePropertyIds.WHEEL_TICK,
                CarPropertyManager.SENSOR_RATE_ONCHANGE
            )

            // Speed -----------------------------------------------
            carPropertyManager!!.registerCallback(
                carPropertyListenerSpeed,
                VehiclePropertyIds.PERF_VEHICLE_SPEED,
                CarPropertyManager.SENSOR_RATE_ONCHANGE
            )

            // Charging -----------------------------------------------
            carPropertyManager!!.registerCallback(
                carPropertyListenerChargeRate,
                VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE,
                CarPropertyManager.SENSOR_RATE_ONCHANGE
            )


        }
    }

}






