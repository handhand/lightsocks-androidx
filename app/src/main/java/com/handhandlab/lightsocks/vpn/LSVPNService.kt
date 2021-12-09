package com.handhandlab.lightsocks.vpn

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.handhandlab.lightsocks.R
import com.handhandlab.lightsocks.ui.MainActivity
import com.handhandlab.lightsocks.LightsocksDroidNative
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket


class LSVPNService : VpnService() {

    private val lightsocksDroid = LightsocksDroidNative()

    companion object{
        private const val CHANNEL_ID = "lightsocks-android"
        const val EXTRA_SOCKS_IP = "extra_socks_addr"
        const val EXTRA_SOCKS_PORT = "extra_socks5_port"
        const val EXTRA_UDPGW_ADDR = "extra_udpgw_addr"
        const val LOCAL_PROXY_LISTEN_PORT = 6666
        const val MTU = 1500

        fun start(context: Activity,
                  socks5Ip:String,
                  socks5port:Int,
                  udpgwAddr:String = "127.0.0.1:7300"):Intent?{

            val intent = prepare(context)
            if (intent != null) {
                return intent
            } else {
                context.startService(Intent(context, LSVPNService::class.java).apply {
                    putExtra(EXTRA_SOCKS_IP, socks5Ip)
                    putExtra(EXTRA_SOCKS_PORT, socks5port)
                    putExtra(EXTRA_UDPGW_ADDR, udpgwAddr)
                })
            }
            return null
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

        val fd = configure()
        val socks5ip = intent!!.getStringExtra(EXTRA_SOCKS_IP)!!
        val socks5port = intent.getIntExtra(EXTRA_SOCKS_PORT, 0)
        val udpgwAddr = intent.getStringExtra(EXTRA_UDPGW_ADDR)!!
        startLightsocks(socks5ip, socks5port, LOCAL_PROXY_LISTEN_PORT)
        startTun2socks(fd!!, udpgwAddr)
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

    private fun configure(): ParcelFileDescriptor? {
        // Configure a new interface from our VpnService instance. This must be done
        // from inside a VpnService.
        val builder = Builder()

        // Create a local TUN interface using predetermined addresses. In your app,
        // you typically use values returned from the VPN gateway during handshaking.
        val localTunnel = builder
            .addAddress("10.0.0.1", 24)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("114.114.114.114")
            .setMtu(MTU)
            .addDisallowedApplication("com.handhandlab.lightsocks")
            .establish()

        return localTunnel
    }

    private fun startLightsocks(serverIp:String, serverPort:Int, localPort:Int){
        Thread {
            lightsocksDroid.startProxy(
                serverIp,
                serverPort,
                localPort
            )
        }.start()
    }

    private fun startTun2socks(fd: ParcelFileDescriptor,
                               udpgwAddr: String = "127.0.0.1:7300"){
        Thread {
            lightsocksDroid.startTunSocks(
                fd.detachFd(),
                MTU,
                "10.0.0.2",//ip of a device attached to other end of the tun interface
                "255.255.255.0",
                "127.0.0.1:$LOCAL_PROXY_LISTEN_PORT",//send to local lightsocks proxy
                udpgwAddr//this means the udp server is on the socks5 server
            )
        }.start()
    }
}