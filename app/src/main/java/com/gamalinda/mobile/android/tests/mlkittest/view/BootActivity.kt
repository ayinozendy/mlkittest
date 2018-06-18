package com.gamalinda.mobile.android.tests.mlkittest.view

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class BootActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }
}
