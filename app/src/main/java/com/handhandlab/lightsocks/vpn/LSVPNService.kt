package com.handhandlab.lightsocks.vpn

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.handhandlab.lightsocks.R
import com.handhandlab.lightsocks.ui.MainActivity
import com.handhandlab.lightsocks.utils.LightsocksDroid
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket


class LSVPNService : VpnService() {

    private val lightsocksDroid = LightsocksDroid()

    companion object{
        private const val CHANNEL_ID = "lightsocks-android"
        const val EXTRA_SOCKS_ADDR = "extra_socks_addr"
        const val EXTRA_UDPGW_ADDR = "extra_udpgw_addr"

        fun start(context: Activity, requestCode:Int, serverAddr:String, udpgwAddr:String){

            val intent = prepare(context)
            if (intent != null) {
                context.startActivityForResult(intent, requestCode)
            } else {
                context.startService(Intent(context, LSVPNService::class.java).apply {
                    putExtra(EXTRA_SOCKS_ADDR, serverAddr)
                    putExtra(EXTRA_UDPGW_ADDR, udpgwAddr)
                })

            }
        }
    }

    private fun startForeground(){
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("notificationTitle")
            .setContentText("input")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(serviceChannel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()

        val fd = configure("123")
        val socks5addr = intent!!.getStringExtra(EXTRA_SOCKS_ADDR)!!
        val udpgwAddr = intent.getStringExtra(EXTRA_UDPGW_ADDR)!!
        Thread {
//            startConnection("192.168.31.71",7300)
            lightsocksDroid.start(
                fd!!.detachFd(),
                1500,
                "10.0.0.2",
                "255.255.255.0",
                socks5addr,
                udpgwAddr
            )
        }.start()
        return START_STICKY
    }

    fun startConnection(ip: String?, port: Int) {
        val clientSocket = Socket(ip, port)
        val out = PrintWriter(clientSocket.getOutputStream(), true)
        val ins = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
        out.println("testestset")
        out.close()
        clientSocket.close()
    }

    private fun configure(parameters: String): ParcelFileDescriptor? {
        // Configure a new interface from our VpnService instance. This must be done
        // from inside a VpnService.
        val builder = Builder()

        // Create a local TUN interface using predetermined addresses. In your app,
        // you typically use values returned from the VPN gateway during handshaking.
        val localTunnel = builder
            .addAddress("10.0.0.1", 24)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("114.114.114.114")
            .setMtu(1500)
            .addDisallowedApplication("com.handhandlab.lightsocks")
            .establish()

        return localTunnel
    }
}