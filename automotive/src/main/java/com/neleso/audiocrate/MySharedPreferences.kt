package com.neleso.audiocrate

import android.content.Context
import android.content.SharedPreferences
import java.lang.IllegalStateException

// my own preferences for this app
object MySharedPreferences {

    private const val PREFSNAME = "myPrefs"
    private var sharedPref: SharedPreferences? = null

    fun create(context: Context) {
        sharedPref = context.getSharedPreferences(PREFSNAME, 0)
    }

    fun save(KEY_NAME: String, text: String) {
        sharedPref?.let {
            val editor: SharedPreferences.Editor = it.edit()
            editor.putString(KEY_NAME, text)
            editor.commit()
        } ?: run { throw IllegalStateException("Shared Preferences not created") }
    }

    fun save(KEY_NAME: String, value: Int) {
        sharedPref?.let {
            val editor: SharedPreferences.Editor = it.edit()
            editor.putInt(KEY_NAME, value)
            editor.commit()
        }
    }

    fun save(KEY_NAME: String, status: Boolean) {
        sharedPref?.let {
            val editor: SharedPreferences.Editor = it.edit()
            editor.putBoolean(KEY_NAME, status)
            editor.commit()
        }
    }

    fun getValueString(KEY_NAME: String): String? {
        return sharedPref?.getString(KEY_NAME, null)
    }

    fun getValueInt(KEY_NAME: String): Int? {
        return sharedPref?.getInt(KEY_NAME, 0)
    }

    fun getValueBoolean(KEY_NAME: String, defaultValue: Boolean): Boolean? {
        return sharedPref?.getBoolean(KEY_NAME, defaultValue)
    }

    fun clearSharedPreference() {
        sharedPref?.let {
            val editor: SharedPreferences.Editor = it.edit()
            //sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            editor.clear()
            editor.commit()
        }
    }

    fun removeValue(KEY_NAME: String) {
        sharedPref?.let {
            val editor: SharedPreferences.Editor = it.edit()
            editor.remove(KEY_NAME)
            editor.commit()
        }
    }
}