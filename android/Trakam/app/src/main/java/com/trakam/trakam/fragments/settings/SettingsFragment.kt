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
        setupCategoryLiveFeed()
    }

    private fun setupCategoryLiveFeed() {
        val sharedPrefs = getDefaultSharedPreferences(activity)
        val prefPort = findPreference(PrefKeys.LiveFeed.KEY_PORT)
        prefPort.summary = sharedPrefs.getString(PrefKeys.LiveFeed.KEY_PORT,
                PrefKeys.LiveFeed.Default.PORT)
    }

    private fun setupCategoryServer() {
        val sharedPrefs = getDefaultSharedPreferences(activity)
        val prefHost = findPreference(PrefKeys.Server.KEY_HOST)
        prefHost.summary = sharedPrefs.getString(PrefKeys.Server.KEY_HOST,
                PrefKeys.Server.Default.HOST)

        val prefPort = findPreference(PrefKeys.Server.KEY_PORT)
        prefPort.summary = sharedPrefs.getString(PrefKeys.Server.KEY_PORT,
                PrefKeys.Server.Default.PORT)
    }

}