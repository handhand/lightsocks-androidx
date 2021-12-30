package com.handhandlab.lightsocks

class LightsocksDroidNative {

    companion object{
        init {
            System.loadLibrary("native-lib")
        }
    }

    /**
     * Natiev端会进入事件循环，所以直到tun2socks退出，这里是一直block住的
     */
    external fun startTunSocks(
        fd:Int,
        tunmtu:Int,
        ifAddr:String,
        ifAddrMask:String,
        socksServerAddr:String,
        udpServerAddr:String
    )

    /**
     * Natiev端会进入libevent事件循环，所以直到libevent退出，这里是一直block住的
     */
    external fun startProxy(
        remoteSocksServerIp: String,
        remoteSocksServerPort: Int,
        localListenPort: Int,
        encryptionKey: String,
        nativeStatusCallback: NativeStatusCallback
    )

    external fun stopTunSocks()

    external fun stopProxy()
}