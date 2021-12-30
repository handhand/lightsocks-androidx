package com.handhandlab.lightsocks.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import com.handhandlab.lightsocks.ILightsocksService
import com.handhandlab.lightsocks.ILightsocksServiceCallback
import com.handhandlab.lightsocks.databinding.ActivityMainBinding
import com.handhandlab.lightsocks.utils.TcpSocketTester
import com.handhandlab.lightsocks.vpn.LightsocksVPNService
import com.handhandlab.lightsocks.vpn.LightsocksVPNService.Companion.EXTRA_FROM_SERVICE

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var serviceConnection: ServiceConnection = LightsocksServiceConnection()
    private var remoteLightsocksService: ILightsocksService? = null
    private var callback: ILightsocksServiceCallback = object : ILightsocksServiceCallback.Stub() {
        override fun onState(status: Int, msg: String?) {
            if (status==0){
                unbindService(serviceConnection)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnStart.setOnClickListener {
            startVPN()
        }

        binding.btnStop.setOnClickListener {
            remoteLightsocksService?.apply {
                updateUI(loading = true)
                stop()
            }
        }

        binding.btnTest2.setOnClickListener {
            testProxy()
        }

        binding.etSecret.setText(secret)
        binding.btnStop.isEnabled = false
        processRestartByService(intent)
    }

    /**
     * activity被service重新启动
     */
    private fun processRestartByService(intent: Intent){
        Log.d("haha","process intent: ${intent.getBooleanExtra(EXTRA_FROM_SERVICE, false)}")
        if (intent.getBooleanExtra(EXTRA_FROM_SERVICE, false) && remoteLightsocksService == null){
            updateUI(loading = true)
            bindVPNService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == RESULT_OK){
            startVPN()
        }
    }

    private fun startVPN(){

        updateUI(loading = true)
        val serverIp = binding.etSocks5Ip.text.toString()
        val serverPort = binding.etSocks5Port.text.toString().toIntOrNull()
        val udpgwPort = binding.etUdpgwPort.text.toString()
        val secret = binding.etSecret.text.toString()

        var hasError = false
        if (serverIp.isEmpty()){
            binding.layoutSocks5Ip.error = "请输入lightsocks-server的ip地址"
            hasError = true
        }
        if (secret.isEmpty()){
            binding.layoutSecret.error = "请输入lightsocks-server的password"
            hasError = true
        }
        if (serverPort == null){
            binding.layoutSocks5Port.error = "请输入lightsocks-server的端口"
            hasError = true
        }
        if (udpgwPort.isEmpty() || !udpgwPort.isDigitsOnly()){
            binding.layoutUdpgwPort.error = "请输入udpgw的端口；注意udpgw是运行在lightsocks相同服务器上的另一个服务，用于远程dns解析"
            hasError = true
        }
        if (hasError) {
            updateUI(startEnabled = true)
            return
        }

        LightsocksVPNService.startOrGetPrepareIntent(this,
            serverIp = serverIp,
            serverPort = serverPort!!,
            udpgwAddr = "127.0.0.1:$udpgwPort",
            secret = secret
        )?.apply {
            startActivityForResult(this, 123)
            return
        }

        bindVPNService()
    }

    private fun bindVPNService(){
        bindService(
            Intent(this, LightsocksVPNService::class.java),
            serviceConnection,
            BIND_AUTO_CREATE
        )
    }

    private inner class LightsocksServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            remoteLightsocksService = ILightsocksService.Stub.asInterface(service)
            remoteLightsocksService?.setCallback(callback)
            updateUI(startEnabled = false)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("haha","onServiceDisconnected")
            remoteLightsocksService = null
            updateUI(startEnabled = true)
        }
    }

    private fun testProxy(){
        Thread{
            val test = TcpSocketTester()
            test.startConnection("192.168.31.61",44119)
            Log.d("haha","result: ${test.sendMessage("tttt")}")
            test.stopConnection()
        }.start()
    }

    private fun updateUI(loading: Boolean = false, startEnabled: Boolean = false){

        binding.pbLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnStop.isEnabled = !startEnabled && !loading
        binding.btnStart.isEnabled = startEnabled && !loading

        binding.etSecret.isEnabled = startEnabled
        binding.etSocks5Ip.isEnabled = startEnabled
        binding.etSocks5Port.isEnabled = startEnabled
        binding.etUdpgwPort.isEnabled = startEnabled
    }

    private val secret = "tr83QUiSt4YZd5v/931jjKFMlAhd+YuKURSyr4Ukx9IHO6fw0TYlwYDUzeUo4CnTT7qP9VRDcugr8XiJf9bnXiI5jszLP/vavQJWBpp7grwdZS5FHLmDyLjPLNCEWFtizlxr1aU0rDDpMbSdFw/JbcVaErs1YMRQlyc8IJzK5JUVQvQKc5MBL7CZ+hjDG6juSyOelrXe22aBsUQ+FkeiZI3AExoJ4llGb0mu45ENMx9XOtd1A+/+Z02qqb6zoyqH2VOgDOYeaSaYfHnfMhFVpAsFq26I8wBKn8Y94d1A7P0EevZh/E74kHDtX6bq660hwi1xENg4dvLcaFIOfmpsdA=="
}