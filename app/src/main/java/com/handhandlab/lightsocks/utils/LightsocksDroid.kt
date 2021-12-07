package com.handhandlab.lightsocks.utils

class LightsocksDroid {

    companion object{
        init {
            System.loadLibrary("native-lib")
        }
    }

    external fun test(fd:Int):Int

    external fun start(
        fd:Int,
        tunmtu:Int,
        ifAddr:String,
        ifAddrMask:String,
        socksServerAddr:String,
        udpServerAddr:String
    )
}