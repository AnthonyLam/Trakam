package com.trakam.trakam.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.trakam.trakam.R
import com.trakam.trakam.fragments.recentactivity.RecentActivityFragment
import com.trakam.trakam.services.ServerPollingService
import com.trakam.trakam.util.replace

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startService(Intent(this, ServerPollingService::class.java))

        if (savedInstanceState == null) {
            fragmentManager.replace(R.id.content_frame, RecentActivityFragment(),
                    RecentActivityFragment.TAG)
        }
    }
}
