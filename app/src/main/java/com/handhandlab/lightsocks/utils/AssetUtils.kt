package com.handhandlab.lightsocks.utils

import android.content.Context
import android.os.Build
import android.util.Log
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.IOException


object AssetUtils {

    private val API_LISTS = arrayListOf("armeabi-v7a", "arm64-v8a")

    const val TUN2SOCKS_EXECUTABLE_NAME = "libbadvpn-tun2socks.so"

    fun copyAsset(context: Context){
        getAbi()?.let { abi->
            val fileSource = context.assets.open("$abi/$TUN2SOCKS_EXECUTABLE_NAME")
            val outputExecutableFile = File(context.filesDir, TUN2SOCKS_EXECUTABLE_NAME)
            if(outputExecutableFile.exists()){
                Log.d("haha","${outputExecutableFile.length()}")
                outputExecutableFile.delete()
            }
            outputExecutableFile.sink().buffer().writeAll(fileSource.source())
        }
    }

    fun runExecutable(cmd:String){
        try {
            // Executes the command.
            val process = Runtime.getRuntime().exec(cmd)
            process.inputStream.source().buffer().use {
                while (true){
                    val line = it.readUtf8Line() ?: break
                    Log.d("haha",line)
                }
            }

            // Waits for the command to finish.
            process.waitFor()
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
    }

    fun getAbi(): String? {
        return Build.SUPPORTED_ABIS[0]
        // on newer Android versions, we'll return only the most important Abi version
    }
}