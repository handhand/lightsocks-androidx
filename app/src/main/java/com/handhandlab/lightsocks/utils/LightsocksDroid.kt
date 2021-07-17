package com.handhandlab.lightsocks.utils

class LightsocksDroid {

    companion object{
        init {
            System.loadLibrary("libnative-lib")
        }
    }
}