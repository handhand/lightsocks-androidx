package com.handhandlab.lightsocks

class LightsocksDroidNative {

    companion object{
        init {
            System.loadLibrary("native-lib")
        }
    }

    external fun startTunSocks(
        fd:Int,
        tunmtu:Int,
        ifAddr:String,
        ifAddrMask:String,
        socksServerAddr:String,
        udpServerAddr:String
    )

    external fun startProxy(
        remoteSocksServerIp: String,
        remoteSocksServerPort: Int,
        localListenPort: Int
    )
}