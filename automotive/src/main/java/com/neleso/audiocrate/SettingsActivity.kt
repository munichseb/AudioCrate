package com.neleso.audiocrate

import android.car.Car
import android.car.VehiclePropertyIds
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.*
import java.net.URL
import java.util.*

/**
 * This class exposes application settings
 * for integration with MediaCenter in Android Automotive.
 */

class SettingsActivity : AppCompatActivity(), MyCarListener, SensorEventListener {

    private lateinit var toolbar: Toolbar
    private lateinit var car: Car
    private val permissions = arrayOf(Car.PERMISSION_ENERGY, Car.PERMISSION_POWERTRAIN)

    private val listOfCarProperties: Map<String, Int> = mapOf(
        "currentGear" to VehiclePropertyIds.GEAR_SELECTION,
        "ev_battLevel" to VehiclePropertyIds.EV_BATTERY_LEVEL,
        "ev_battKapa" to VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY,
        "ev_chargeRate" to VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE,
        "remainingRange" to VehiclePropertyIds.RANGE_REMAINING,
    )

    private val listOfPermissions = arrayOf<String>(
        Car.PERMISSION_ENERGY,
        Car.PERMISSION_POWERTRAIN
    )

    private lateinit var sensorManager: SensorManager

    var superU = "superUUID"

    var lastSensorMessage =  java.lang.System.currentTimeMillis()

    // Values are taken from android.car.hardware.CarSensorEvent class.
    private val VEHICLE_GEARS = mapOf(
        0x0000 to "GEAR_UNKNOWN",
        0x0001 to "GEAR_NEUTRAL",
        0x0002 to "GEAR_REVERSE",
        0x0004 to "GEAR_PARK",
        0x0008 to "GEAR_DRIVE"
    )

    companion object {
        private const val TAG = "SettingsActivity"
    }

    // ----- Lambda Magic

    inline fun <T> T.guard(block: T.() -> Unit): T {
        if (this == null) block()
        return this
    }

    /* ---------------------  INNER ----------------------- */

    class SettingsFragment(context: Context) : PreferenceFragmentCompat() {

        private val appContext = context

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            Log.d("Settings", "UUID : " + MyCar.carId)

        }

    }

    /* --------------------- ON CREATE ----------------------- */

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        MySharedPreferences.create(this.applicationContext)
        MyCar.createCar(this.applicationContext)


        var isDenied = false
        listOfPermissions.forEach {
            if (checkSelfPermission(it) == PackageManager.PERMISSION_DENIED)
                isDenied = true
        }

        if (isDenied) {
            requestPermissions(listOfPermissions, 0)
        }

        setContentView(R.layout.activity_settings)
        Log.d("Settings", "Settings onCreate")
        Log.d("Settings", "request permission: : " + permissions[0])

        // mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // focus in accelerometer
        // mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        // setup the window

        // -----------------------------------------------------------------------------------------
        // handle button click - close view
        // -----------------------------------------------------------------------------------------

        val clickButton = findViewById(R.id.settings_close) as ImageButton
        clickButton.setOnClickListener {
            super.onBackPressed()
            finish()
        }

        // Init event handler

        MyCar.register(this, listOfCarProperties)

        // Send vehicle version to server
        URL("https://trelp.datacar.io/datacar/callback.php?uuid=" + MyCar.carId + "&t=appVersion&m=18").readText()

        // -----------------------------------------------------------------------------------------

        val versionAPI = Build.VERSION.SDK_INT
        val versionRelease = Build.VERSION.RELEASE

        // Send vehicle version to server
        URL("https://trelp.datacar.io/datacar/callback.php?uuid=" + MyCar.carId + "&t=androidVersion&m=" + versionAPI + "_release-" + versionRelease).readText()

        val locale: String = this.getResources().getConfiguration().locale.getDisplayCountry()

        this.sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager


        // Model und Make ermitteln

        val vistaWeb: WebView = findViewById<View>(R.id.settings_webview) as WebView

        vistaWeb.clearCache(true)
        vistaWeb.clearHistory()
        vistaWeb.getSettings().setJavaScriptEnabled(true)
        vistaWeb.getSettings().setJavaScriptCanOpenWindowsAutomatically(true)
        vistaWeb.setWebViewClient(WebViewClient())
        vistaWeb.loadUrl("https://trelp.datacar.io/qr-gen/?uuid=" + MyCar.carId + "&locale=" + locale);

    } // onCreate

    fun handleButtonClick(view: View) {
        with(view as Button) {
            // Log.d("TAG", "$text, $id")

            Log.w("Settings", "Received BUTTON CLICK")
            // val apiResponse = URL("https://trelp.datacar.io/datacar/callback.php?m=BUttonClick").readText()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }


    override fun onChangedProperty(vehilcePropertyId: Int, value: Any) {

        Log.d(
            TAG,
            "Got VehiclePropertyId: $vehilcePropertyId with value: ${value.toString()} - ${MyCar.carId}"
        )

        when(vehilcePropertyId) {

            // VehiclePropertyIds.PERF_VEHICLE_SPEED -> {
            //     Log.d(TAG, "PERF_VEHICLE_SPEED: ${value.toString()}")
            // }

            VehiclePropertyIds.EV_BATTERY_LEVEL -> {
                Log.d(TAG, "EV_BATTERY_LEVEL: ${value.toString()}")
                URL("https://trelp.datacar.io/datacar/callback.php?uuid=" + MyCar.carId + "&m=" + value.toString() + "&t=batt_level").readText()
            }

            VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY -> {
                Log.d(TAG, "INFO_EV_BATTERY_CAPACITY: ${value.toString()}")
                URL("https://trelp.datacar.io/datacar/callback.php?uuid=" + MyCar.carId + "&m=" + value.toString() + "&t=batt_capacity").readText()
            }

            VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE -> {
                Log.d(TAG, "EV_BATTERY_INSTANTANEOUS_CHARGE_RATE: ${value.toString()}")
                URL("https://trelp.datacar.io/datacar/callback.php?uuid=" + MyCar.carId + "&m=" + value.toString() + "&t=charge_rate").readText()
            }

            VehiclePropertyIds.RANGE_REMAINING -> {
                Log.d(TAG, "RANGE_REMAINING: ${value.toString()}")
                URL("https://trelp.datacar.io/datacar/callback.php?uuid=" + MyCar.carId + "&m=" + value.toString() + "&t=range").readText()
            }

            VehiclePropertyIds.GEAR_SELECTION -> {
                Log.d(TAG, "GEAR_SELECTION: ${VEHICLE_GEARS[value]}")
                URL("https://trelp.datacar.io/datacar/callback.php?uuid=" + MyCar.carId + "&m=" + VEHICLE_GEARS[value] + "&t=gear").readText()
            }

        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && (lastSensorMessage < (java.lang.System.currentTimeMillis() - 1000))) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {

            Log.d(TAG, "TYPE_GYROSCOPE:  ${event.values[0]}")
            URL(
                "https://trelp.datacar.io/datacar/callback.php?uuid=" + MyCar.carId + "&m=" +
                        "x" + event.values[0] +
                        "_y" + event.values[1] +
                        "_z" + event.values[2] +
                        "&t=gyroscope"
            ).readText()

            lastSensorMessage =  java.lang.System.currentTimeMillis()

        }}

    }
    /* --------------------------------------------------------------------------------------------- */

}

    // String vin = propertyManager.getProperty<String>(CAR_INFO, VEHICLE_AREA_TYPE_GLOBAL)?.value
