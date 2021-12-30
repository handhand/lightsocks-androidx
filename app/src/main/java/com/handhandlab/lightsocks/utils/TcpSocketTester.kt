package com.handhandlab.lightsocks.utils

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.lang.Exception
import java.net.Socket

class TcpSocketTester {
    private var clientSocket: Socket? = null
    private var out: PrintWriter? = null
    private var `in`: BufferedReader? = null
    fun startConnection(ip: String?, port: Int) {
        clientSocket = Socket(ip, port)
        out = PrintWriter(clientSocket!!.getOutputStream(), true)
        `in` = BufferedReader(InputStreamReader(clientSocket?.getInputStream()))
    }

    fun sendMessage(msg: String?): String? {
        try{

            out?.println(msg)
            return `in`?.readLine()
        }catch (e:Exception){
            e.printStackTrace()
            stopConnection()
        }
        return null
    }

    fun stopConnection() {
        `in`?.close()
        out?.close()
        clientSocket?.close()
    }
}