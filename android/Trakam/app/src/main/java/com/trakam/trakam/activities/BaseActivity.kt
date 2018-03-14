package com.trakam.trakam.activities

import android.support.v7.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    protected fun enableNavigateUp() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}