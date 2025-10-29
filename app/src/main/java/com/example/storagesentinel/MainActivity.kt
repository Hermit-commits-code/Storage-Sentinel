package com.example.storagesentinel

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.view.Gravity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this).apply {
            text = "Storage Sentinel - Native host"
            textSize = 18f
            gravity = Gravity.CENTER
        }
        setContentView(tv)
    }
}
