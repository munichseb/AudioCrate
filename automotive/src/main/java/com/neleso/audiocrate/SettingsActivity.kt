package com.neleso.audiocrate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.car.Car
import android.car.CarInfoManager
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import java.net.URL
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private val permissions = arrayOf(Car.PERMISSION_ENERGY, Car.PERMISSION_POWERTRAIN, Car.PERMISSION_SPEED)

    private val GEAR_UNKNOWN = "GEAR_UNKNOWN"
    private val allow_backend_comms = true

    var current_soc = 0.0
    var current_charging_speed = 0.0
    var range_remaining = 0.0
    var gearSelected = "-"

    var superU = "superUUID"

    var txLimiter_soc = System.currentTimeMillis() - 15000
    var txLimiter_range = System.currentTimeMillis() - 15000
    var txLimiter_kapa = System.currentTimeMillis() - 15000
    var txLimiter_charge = System.currentTimeMillis() - 15000

    // Values are taken from android.car.hardware.CarSensorEvent class.
    private val VEHICLE_GEARS = mapOf(
        0x0000 to GEAR_UNKNOWN,
        0x0001 to "GEAR_NEUTRAL",
        0x0002 to "GEAR_REVERSE",
        0x0004 to "GEAR_PARK",
        0x0008 to "GEAR_DRIVE"
    )

    private lateinit var prefs: SharedPreferences.Editor

    companion object {
        private const val TAG = "SettingsActivity"
    }

    // ----- Lambda Magic

    inline fun <T> T.guard(block: T.() -> Unit): T {
        if (this == null) block()
        return this
    }

    // ------------------

    private var _uuid: String
        set(value) {
            this.prefs.putString(value, "unkown")
            this.prefs.apply()
        }
        get() {
           if (PreferenceManager.getDefaultSharedPreferences(this).contains("uuid")) {

               return PreferenceManager.getDefaultSharedPreferences(this)
                   .getString("uuid", "error").toString()

           } else {

                val randomUUID = UUID.randomUUID().toString()
                this.prefs.putString(randomUUID, "unkown")
                this.prefs.apply()
                return PreferenceManager.getDefaultSharedPreferences(this).getString(
                    "uuid",
                    randomUUID
                ).toString()
           }
        }

    public var uuid: String
        get() {
            return _uuid ?: throw AssertionError("uuid could not be retrieved.")
        }
        set(value) {
            _uuid = value
        }

    // ----- UUID

     /* ---------------------  INNER ----------------------- */



    class SettingsFragment(context: Context) : PreferenceFragmentCompat() {

        private val appContext = context

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        }

    }



    /* --------------------- ON CREATE ----------------------- */

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        this.prefs = PreferenceManager.getDefaultSharedPreferences(this).edit()

        // var myContext = getContext();
        // var myC = myContext
        // hier ist es falsch - Log.d("Settings", "onCreate: UUID: " + uuid)
        //

        setContentView(R.layout.activity_settings)
        Log.d("Settings", "Settings onCreate")

        // --------------------------- edit preference text ------------------------------------

        Log.d("Settings", "request permission: : " + permissions[0])

        var isPermitted = false

        permissions.forEach {
            if (checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED) {
                Log.d("Settings", "permission not granted: " + it)
                isPermitted = true
            }
        }

        if  (isPermitted) {
            Log.d("Settings", "Request permission -2: " + permissions[2])

            // arrayOf(Car.PERMISSION_ENERGY, Car.PERMISSION_POWERTRAIN, Car.PERMISSION_SPEED)
            requestPermissions(arrayOf(Car.PERMISSION_SPEED), 0)

            Log.d("Settings", "Request permission -1: " + permissions[1])
            requestPermissions(arrayOf(Car.PERMISSION_POWERTRAIN), 0)


            Log.d("Settings", "Request permission -1: " + permissions[0])
            requestPermissions(arrayOf(Car.PERMISSION_ENERGY), 0)

            // requestPermissions(permissions, 0)
        }


        // -----------------------------------------------------------------------------------------
        // handle button click
        // -----------------------------------------------------------------------------------------

        val clickButton = findViewById(R.id.settings_close) as ImageButton
        clickButton.setOnClickListener {
            super.onBackPressed()
            finish()
        }


        // ----------------------------------------------------------------------------------------- GET UUID AND SET GLOBAALY

        // create my own preferences for this application which are not view based.
        val sharedPrefs = MySharedPreferences(this)
        // set uuid, if it is not defined already
        val myUuid = sharedPrefs.getValueString("uuid").guard {
            // now uuid is set for application and stored as long as app is on device
            sharedPrefs.save("uuid", UUID.randomUUID().toString())
        }

        Log.d("Settings", "onCreate UUID: " + myUuid)
        superU = myUuid.toString();
        uuid = myUuid.toString();

        // -----------------------------------------------------------------------------------------
        MyCar.createCar(this.applicationContext, myUuid.toString())
        MyCar.register()
        MyCar.my_uuid = myUuid.toString();

        // Send vehicle version to server
        URL("https://trelp.datacar.io/datacar/callback.php?uuid=" + superU + "&t=appVersion&m=17").readText()

        // -----------------------------------------------------------------------------------------

        val versionAPI = Build.VERSION.SDK_INT
        val versionRelease = Build.VERSION.RELEASE

        // Send vehicle version to server
        URL("https://trelp.datacar.io/datacar/callback.php?uuid=" + superU + "&t=androidVersion&m=" + versionAPI + "_release-" + versionRelease).readText()

        val locale: String = this.getResources().getConfiguration().locale.getDisplayCountry()

        // Model und Make ermitteln

        val vistaWeb: WebView = findViewById<View>(R.id.settings_webview) as WebView
        // vistaWeb.setWebChromeClient(MyCustomChromeClient(this))
        // vistaWeb.setWebViewClient(MyCustomWebViewClient(this))
        vistaWeb.clearCache(true)
        vistaWeb.clearHistory()
        vistaWeb.getSettings().setJavaScriptEnabled(true)
        vistaWeb.getSettings().setJavaScriptCanOpenWindowsAutomatically(true)
        vistaWeb.setWebViewClient(WebViewClient())
        vistaWeb.loadUrl("https://trelp.datacar.io/qr-gen/?uuid=" + superU + "&locale=" + locale + "&battLevel=" + current_soc + "&range=" + range_remaining + "&charging=" + current_charging_speed + "&gear=" + gearSelected);

        // ---------------------------------------- FIREBASE ---------------------------------------

        Log.w("Firebase", "onCreate superU: " + superU)

        // Create channel to show notifications.
        val channelId = getString(R.string.default_notification_channel_id)
        val channelName = getString(R.string.default_notification_channel_name)

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(
            NotificationChannel(
                channelId,
                channelName, NotificationManager.IMPORTANCE_LOW
            )
        )

        Firebase.messaging.token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.d(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            val msg = getString(R.string.msg_token_fmt, token)
            Log.d(TAG, msg)

            // TODO (Sebastian): Here you can send logToken to backend
            val apiResponse =
                URL("https://trelp.datacar.io/datacar/callback.php?uuid=" + superU + "&t=logToken&m=" + msg + "").readText()

            // Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        })



        // ---------------------------------------- FIREBASE ---------------------------------------

        Log.w("Settings", "Gear selectd: " + gearSelected)
        Log.w("Settings", "range_remaining: " + range_remaining)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment(this))
            .commit()

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


    /* --------------------------------------------------------------------------------------------- */

}