package com.viditure.weleta.util.aws

import android.util.Log
import com.example.myapplication.utils.aws.CloudWatchLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

// CloudWatchLogger class remains as defined previously with logMessage function

object LogInterceptor {
    fun startLogging() {
        Log.d("LogInterceptor", "Starting log interception...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val process = Runtime.getRuntime().exec("logcat")
                val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))

                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    line?.let { logLine ->
                        if (!logLine.contains("AWS4Signer") && !logLine.contains("com.amazonaws") && !logLine.contains(
                                "CloudWatchLogger"
                            )
                        ) {
                            CloudWatchLogger.logMessage(logLine)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("LogInterceptor", "Error intercepting logs: ${e.message}")
            }
        }
    }
}
