package com.trakam.trakam.activities

import android.os.Bundle
import com.trakam.trakam.R
import com.trakam.trakam.fragments.settings.SettingsFragment
import com.trakam.trakam.util.replace

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (savedInstanceState == null) {
            fragmentManager.replace(R.id.content_frame, SettingsFragment(), SettingsFragment.TAG)
        }
    }

}