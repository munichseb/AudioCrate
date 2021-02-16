package com.neleso.audiocrate

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat


/**
 * Preference fragment hosted by [SettingsActivity]. Handles events to various preference changes.
 */

class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var viewModel: SettingsFragmentViewModel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        setPreferencesFromResource(R.xml.preferences, rootKey)
        viewModel = ViewModelProvider(this)
            .get(SettingsFragmentViewModel::class.java)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        return false
    }
}

/**
 * Basic ViewModel for [SettingsFragment].
 */
class SettingsFragmentViewModel(application: Application) : AndroidViewModel(application) {
    private val applicationContext = application.applicationContext

}
