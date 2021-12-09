package com.handhandlab.lightsocks.utils

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
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
        out?.println(msg)
        return `in`?.readLine()
    }

    fun stopConnection() {
        `in`?.close()
        out?.close()
        clientSocket?.close()
    }
}