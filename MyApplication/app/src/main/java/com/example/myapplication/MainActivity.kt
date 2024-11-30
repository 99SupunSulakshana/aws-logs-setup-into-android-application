package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.amazonaws.BuildConfig
import com.example.myapplication.constants.AWSInfo
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.utils.aws.CloudWatchLogger
import com.viditure.weleta.util.aws.LogInterceptor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(BuildConfig.DEBUG && AWSInfo.isAWSLogsEnabled){
            CloudWatchLogger.initialize(AWSInfo.LOG_GROUP_ACCESS_KEY, AWSInfo.LOG_GROUP_SECRET_KEY, AWSInfo.LOG_GROUP_REGION)
            LogInterceptor.startLogging()
        }
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}