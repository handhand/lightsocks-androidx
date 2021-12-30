package com.handhandlab.lightsocks

interface NativeStatusCallback {
    fun onState(status:Int, msg:String)
}