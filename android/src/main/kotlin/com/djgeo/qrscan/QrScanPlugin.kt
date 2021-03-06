package com.djgeo.qrscan

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import com.djgeo.qrscan.g_scanner.QrCodeScannerActivity
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar

class QrScanPlugin(activity: Activity) : MethodCallHandler, PluginRegistry.ActivityResultListener {

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val qrscanPlugin = QrScanPlugin(registrar.activity())
            val channel = MethodChannel(registrar.messenger(), "qrscan")
            channel.setMethodCallHandler(qrscanPlugin)

            // 注册ActivityResult回调
            registrar.addActivityResultListener(qrscanPlugin)
        }

        const val SCANRESULT = "scan"
        const val Request_Scan = 1
    }

    private var activity: Activity? = activity
    private var mResult: Result? = null
    private var mResultPeriod = 0L

    override fun onMethodCall(call: MethodCall, result: Result) {

        when (call.method) {
            SCANRESULT -> {
                val args: Map<String, String>? = call.arguments()
                activity?.let {
                    val intent = Intent(it, QrCodeScannerActivity::class.java)
                    args?.keys?.map { key -> intent.putExtra(key, args[key]) }
                    it.startActivityForResult(intent, Request_Scan)
                    mResult = result
                }
            }
            else -> result.notImplemented()
        }
    }

    //issue tracking https://github.com/flutter/flutter/issues/29092
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        val currentTime = System.currentTimeMillis()
        if (requestCode == Request_Scan && resultCode == RESULT_OK && data != null) {
            if (currentTime - mResultPeriod >= 1000) {
                mResultPeriod = currentTime
                val resultString = data.getStringExtra(QrCodeScannerActivity.BUNDLE_SCAN_CALLBACK)
                resultString?.let {
                    mResult?.success(it)
                }
                return true
            }
        }
        return false
    }
}
