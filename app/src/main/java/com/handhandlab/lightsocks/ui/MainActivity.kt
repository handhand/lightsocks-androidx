package com.handhandlab.lightsocks.ui

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.handhandlab.lightsocks.databinding.ActivityMainBinding
import com.handhandlab.lightsocks.utils.AssetUtils
import com.handhandlab.lightsocks.utils.AssetUtils.TUN2SOCKS_EXECUTABLE_NAME
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val t = AssetUtils.getAbi()
        // Example of a call to a native method
        binding.sampleText.text = stringFromJNI()

        Thread{
//            AssetUtils.copyAsset(this)
            val nativeLibDir = packageManager.getApplicationInfo("com.handhandlab.lightsocks", PackageManager.GET_SHARED_LIBRARY_FILES)
                .nativeLibraryDir
            Log.d("haha",nativeLibDir)
            AssetUtils.runExecutable(File(nativeLibDir, TUN2SOCKS_EXECUTABLE_NAME).absolutePath)
        }.start()
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}