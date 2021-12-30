package com.handhandlab.lightsocks.vpn

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.handhandlab.lightsocks.*
import com.handhandlab.lightsocks.ui.MainActivity
import kotlinx.coroutines.*

/**
 * VPN service,获取到android系统的/dev/tun的设备fd
 * 然后启动native端的
 * 1.tun2socks：负责从/dev/tun读取ip包并转换为高层的socks协议
 * 2.libevent proxy：负责读取tun2socks的socks协议包，并加密转发给服务器，然后解密服务器数据再转发回给tun2socks
 */
class LightsocksVPNService : VpnService(){

    private val lightsocksDroid = LightsocksDroidNative()
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var ipcCallback: ILightsocksServiceCallback? = null

    private val binder = object : ILightsocksService.Stub() {

        override fun setCallback(callback: ILightsocksServiceCallback?) {
            this@LightsocksVPNService.ipcCallback = callback
        }

        override fun stop() {
            this@LightsocksVPNService.stop()
        }
    }

    companion object{
        private const val CHANNEL_ID = "lightsocks-android"
        const val EXTRA_SOCKS_IP = "extra_socks_addr"
        const val EXTRA_SOCKS_PORT = "extra_socks5_port"
        const val EXTRA_UDPGW_ADDR = "extra_udpgw_addr"
        const val EXTRA_SECRET = "extra_secret"
        const val EXTRA_FROM_SERVICE = "extra_from_service"
        const val LOCAL_PROXY_LISTEN_PORT = 6666
        const val MTU = 1500

        fun startOrGetPrepareIntent(context: Activity,
                                    serverIp:String,
                                    serverPort:Int,
                                    udpgwAddr:String = "127.0.0.1:7300",
                                    secret: String):Intent?{

            val intent = prepare(context)
            if (intent != null) {
                return intent
            } else {
                context.startService(Intent(context, LightsocksVPNService::class.java).apply {
                    putExtra(EXTRA_SOCKS_IP, serverIp)
                    putExtra(EXTRA_SOCKS_PORT, serverPort)
                    putExtra(EXTRA_UDPGW_ADDR, udpgwAddr)
                    putExtra(EXTRA_SECRET, secret)
                })
            }
            return null
        }
    }

    private fun startForeground(){
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java).apply { putExtra(EXTRA_FROM_SERVICE, true) }
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
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
        val secret = intent.getStringExtra(EXTRA_SECRET)!!
//        startLightsocks(socks5ip, socks5port, LOCAL_PROXY_LISTEN_PORT)
//        startTun2socks(fd!!, udpgwAddr)
        start(socks5ip,
            socks5port,
            fd!!,
            udpgwAddr,
            secret
        )
        return START_STICKY
    }

//    override fun onBind(intent: Intent?) = serviceBinder
    override fun onBind(intent: Intent?) = binder

    /**
     * service killed by system
     */
    override fun onDestroy() {
        super.onDestroy()
//        onStopListener = null
        Log.d("haha","service ondestroy")
        serviceJob.cancel()
        stop()
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

    private fun start(
        socks5serverIp:String,
        socks5serverPort:Int,
        fd: ParcelFileDescriptor,
        udpgwAddr: String,
        secret: String){
        serviceScope.launch {
            val proxyDeferred = async {
                startLightsocks(
                    socks5serverIp,
                    socks5serverPort,
                    LOCAL_PROXY_LISTEN_PORT,
                    secret)
            }

            val tun2socksDeferred = async {
                startTun2socks(
                    fd,
                    udpgwAddr)
            }

            tun2socksDeferred.await()
            proxyDeferred.await()

            withContext(Dispatchers.Main){
                ipcCallback?.onState(0, "STOP")
                stopSelf()
                Log.d("haha","stop!")
            }
        }

    }

    private fun startTun2socks(fd: ParcelFileDescriptor,
                               udpgwAddr: String = "127.0.0.1:7300") {
        Log.d("haha","adsf")
        lightsocksDroid.startTunSocks(
            fd.fd,
            MTU,
            "10.0.0.2",//ip of a device attached to other end of the tun interface
            "255.255.255.0",
            "127.0.0.1:$LOCAL_PROXY_LISTEN_PORT",//send to local lightsocks proxy
//            "192.168.31.63:6666",//test tun2socks directly to a real socks5 proxy
            udpgwAddr//this means the udp server is on the socks5 server
        )
        fd.close()
    }

    private fun startLightsocks(serverIp:String,
                                        serverPort:Int,
                                        localPort:Int,
                                        secret: String){
        lightsocksDroid.startProxy(
            serverIp,
            serverPort,
            localPort,
            secret,
            object : NativeStatusCallback {
                override fun onState(status: Int, msg: String) {
                    ipcCallback?.onState(status, msg)
                }
            }
        )
    }

    private fun stop() {
        stopForeground(true)
        stopSelf()
        lightsocksDroid.stopProxy()
        lightsocksDroid.stopTunSocks()
        Log.d("haha","before process killed!!!")
        //简单粗暴，干手净脚
        Handler(Looper.getMainLooper()).post {
            Process.killProcess(Process.myPid())
        }
    }
}