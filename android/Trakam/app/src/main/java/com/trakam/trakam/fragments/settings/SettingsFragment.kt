package com.trakam.trakam.fragments.settings

import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceManager.getDefaultSharedPreferences
import com.trakam.trakam.R
import com.trakam.trakam.util.PrefKeys

class SettingsFragment : PreferenceFragment() {

    companion object {
        val TAG = SettingsFragment::class.qualifiedName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.settings)
        setupPrefs()
    }

    private fun setupPrefs() {
        setupCategoryServer()
    }

    private fun setupCategoryServer() {
        val sharedPrefs = getDefaultSharedPreferences(activity)
        val prefHost = findPreference(PrefKeys.Server.KEY_SERVER_HOST)
        prefHost.summary = sharedPrefs.getString(PrefKeys.Server.KEY_SERVER_HOST,
                PrefKeys.Server.Default.SERVER_HOST)

        val prefPort = findPreference(PrefKeys.Server.KEY_SERVER_PORT)
        prefPort.summary = sharedPrefs.getString(PrefKeys.Server.KEY_SERVER_PORT,
                PrefKeys.Server.Default.SERVER_PORT)
    }

}