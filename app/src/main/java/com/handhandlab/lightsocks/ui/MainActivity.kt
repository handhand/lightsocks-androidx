package com.handhandlab.lightsocks.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.handhandlab.lightsocks.databinding.ActivityMainBinding
import com.handhandlab.lightsocks.utils.LightsocksDroid
import com.handhandlab.lightsocks.vpn.LSVPNService
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LightsocksDroid()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnTest.setOnClickListener {
            testTcp()
        }
        LSVPNService.start(this, 123,"192.168.31.71:1080", "192.168.31.71:7300")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == RESULT_OK){
            LSVPNService.start(this, 123,"192.168.31.71:1080", "192.168.31.71:7300")
        }
    }

    private fun testTcp(){
        Thread {
            val client = GreetClient()
            client.startConnection("127.0.0.1",6666)
            val result = client.sendMessage("test message looking for your echo")
            Log.d("haha","result:$result")
        }.start()
    }

    class GreetClient {
        private var clientSocket: Socket? = null
        private var out: PrintWriter? = null
        private var `in`: BufferedReader? = null
        fun startConnection(ip: String?, port: Int) {
            clientSocket = Socket(ip, port)
            out = PrintWriter(clientSocket!!.getOutputStream(), true)
            `in` = BufferedReader(InputStreamReader(clientSocket?.getInputStream()))
        }

        fun sendMessage(msg: String?): String? {
            out?.println(msg)
            return `in`?.readLine()
        }

        fun stopConnection() {
            `in`?.close()
            out?.close()
            clientSocket?.close()
        }
    }
}