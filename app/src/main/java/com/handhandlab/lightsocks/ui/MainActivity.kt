package com.handhandlab.lightsocks.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.handhandlab.lightsocks.databinding.ActivityMainBinding
import com.handhandlab.lightsocks.LightsocksDroidNative
import com.handhandlab.lightsocks.vpn.LSVPNService
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LightsocksDroidNative()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnTest.setOnClickListener {
            //empty
        }
        LSVPNService.start(this,
            "192.168.31.79",
            1080,
            "127.0.0.1:7300")?.apply {
                startActivityForResult(this, 123)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == RESULT_OK){
            LSVPNService.start(this,
                "192.168.31.79",
                1080,
                "127.0.0.1:7300")
        }
    }

}