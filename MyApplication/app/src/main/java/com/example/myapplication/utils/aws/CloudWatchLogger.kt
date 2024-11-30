package com.example.myapplication.utils.aws

import android.os.Build
import android.util.Log
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.logs.AmazonCloudWatchLogsClient
import com.amazonaws.services.logs.model.CreateLogGroupRequest
import com.amazonaws.services.logs.model.CreateLogStreamRequest
import com.amazonaws.services.logs.model.DescribeLogGroupsRequest
import com.amazonaws.services.logs.model.DescribeLogStreamsRequest
import com.amazonaws.services.logs.model.InputLogEvent
import com.amazonaws.services.logs.model.PutLogEventsRequest
import com.amazonaws.services.logs.model.ResourceAlreadyExistsException
import com.example.myapplication.constants.AWSInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID


class CloudWatchLogger {
    companion object {
        private const val LOG_GROUP_NAME = AWSInfo.LOG_GROUP_NAME
        private const val LOG_GROUP_NAME_NEW = AWSInfo.LOG_GROUP_NAME_NEW
        private var LOG_STREAM_NAME = getLogStreamName()
        private lateinit var logsClient: AmazonCloudWatchLogsClient

        fun initialize(accessKey: String, secretKey: String, region: String) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Set AWS credentials using BasicAWSCredentials
                    val awsCredentials = BasicAWSCredentials(
                        accessKey,
                        secretKey
                    )
                    // Initialize CloudWatch Logs client
                    logsClient = AmazonCloudWatchLogsClient(awsCredentials)

                    // Set the AWS region explicitly
                    val awsRegion = Region.getRegion(Regions.fromName(region))
                    logsClient.setRegion(awsRegion)

                    // Validate the region setup
                    Log.d(
                        "CloudWatchLogger",
                        "CloudWatch Logs Client initialized in region: ${logsClient.regions}"
                    )
                    // Log to confirm the region setting
                    Log.d("CloudWatchLogger", "Set AWS region to: ${awsRegion.name}")
                    listLogGroups()
                    // Create a log group and log stream
                    // createLogGroup()
                    if (!doesLogGroupExist(LOG_GROUP_NAME)) {
                        Log.e("CloudWatch", "doesLogGroupExists")
                        createLogGroup(LOG_GROUP_NAME_NEW)
                    } else {
                        Log.e("CloudWatch", "doesLogGroupExists")
                    }
                    createLogStream()
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {}
                }
            }
        }

        private fun createLogGroup(logGroupNameNew: String) {
            try {
                val createLogGroupRequest = CreateLogGroupRequest().apply {
                    logGroupName = logGroupNameNew
                }
                logsClient.createLogGroup(createLogGroupRequest)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Function to check if a log group exists
        private fun doesLogGroupExist(logGroupNameForDoesGroupExits: String): Boolean {
            return try {
                val describeLogGroupsRequest = DescribeLogGroupsRequest().apply {
                    this.logGroupNamePrefix = logGroupNameForDoesGroupExits
                }
                val describeLogGroupsResult = logsClient.describeLogGroups(describeLogGroupsRequest)
                describeLogGroupsResult.logGroups.any { it.logGroupName == logGroupNameForDoesGroupExits }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        private fun listLogGroups() {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Create the request to describe log groups
                    val describeLogGroupsRequest = DescribeLogGroupsRequest()

                    // Make the call to AWS CloudWatch Logs to get log groups
                    val describeLogGroupsResult =
                        logsClient.describeLogGroups(describeLogGroupsRequest)

                    // Retrieve and log the log group names
                    val logGroups = describeLogGroupsResult.logGroups
                    if (logGroups.isNotEmpty()) {
                        logGroups.forEach { logGroup ->
                            Log.d("CloudWatchLogger", "Log Group Name: ${logGroup.logGroupName}")
                        }
                    } else {
                        Log.d("CloudWatchLogger", "No log groups found.")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("CloudWatchLogger", "Failed to list log groups: ${e.message}")
                }
            }
        }


        private fun createLogStream() {
            try {
                // Check if the log stream exists
                val describeLogStreamsRequest = DescribeLogStreamsRequest().apply {
                    this.logGroupName = LOG_GROUP_NAME
                    this.logStreamNamePrefix = LOG_STREAM_NAME
                }

                val describeLogStreamsResult =
                    logsClient.describeLogStreams(describeLogStreamsRequest)
                val existingLogStream = describeLogStreamsResult.logStreams.firstOrNull {
                    it.logStreamName == LOG_STREAM_NAME
                }

                // Create log stream only if it does not exist
                if (existingLogStream == null) {
                    val createLogStreamRequest = CreateLogStreamRequest().apply {
                        logGroupName = LOG_GROUP_NAME
                        logStreamName = LOG_STREAM_NAME
                    }
                    logsClient.createLogStream(createLogStreamRequest)
                    Log.d("CloudWatchLogger", "Log stream created: $LOG_STREAM_NAME")
                } else {
                    Log.d("CloudWatchLogger", "Log stream already exists: $LOG_STREAM_NAME")
                }
            } catch (e: ResourceAlreadyExistsException) {
                // Log group already exists; this is fine
                Log.d("CloudWatchLogger", "Log stream already exists: $LOG_STREAM_NAME")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun logMessage(message: String) {
            CoroutineScope(Dispatchers.IO).launch {
                if (LOG_GROUP_NAME.isBlank() || LOG_STREAM_NAME.isBlank()) {
                    Log.e("CloudWatchLogger", "Log group name or log stream name is not set.")
                    return@launch
                }

                val logEvent = InputLogEvent().apply {
                    this.message = message
                    this.timestamp = System.currentTimeMillis()
                }

                try {
                    val describeLogStreamsRequest = DescribeLogStreamsRequest().apply {
                        this.logGroupName = LOG_GROUP_NAME
                        this.logStreamNamePrefix = LOG_STREAM_NAME
                    }

                    val describeLogStreamsResult =
                        logsClient.describeLogStreams(describeLogStreamsRequest)
                    val logStream =
                        describeLogStreamsResult.logStreams.firstOrNull { it.logStreamName == LOG_STREAM_NAME }

                    if (logStream == null) {
                        Log.e("CloudWatchLogger", "Log stream does not exist. Creating log stream.")
                        createLogStream()

                        // Re-fetch the log stream after creation
                        val updatedDescribeLogStreamsResult =
                            logsClient.describeLogStreams(describeLogStreamsRequest)
                        val updatedLogStream =
                            updatedDescribeLogStreamsResult.logStreams.firstOrNull { it.logStreamName == LOG_STREAM_NAME }

                        if (updatedLogStream == null) {
                            Log.e("CloudWatchLogger", "Failed to create log stream.")
                            return@launch
                        }

                        val sequenceToken = updatedLogStream.uploadSequenceToken
                        sendLogEvent(logEvent, sequenceToken)
                    } else {
                        val sequenceToken = logStream.uploadSequenceToken
                        sendLogEvent(logEvent, sequenceToken)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("CloudWatchLogger", "Error sending log event: ${e.message}")
                }
            }
        }

        private fun sendLogEvent(logEvent: InputLogEvent, sequenceToken: String?) {
            try {
                val putLogEventsRequest = PutLogEventsRequest().apply {
                    this.logGroupName = LOG_GROUP_NAME
                    this.logStreamName = LOG_STREAM_NAME
                    this.sequenceToken = sequenceToken
                }
                putLogEventsRequest.setLogEvents(listOf(logEvent))
                logsClient.putLogEvents(putLogEventsRequest)
                Log.d("CloudWatchLogger", "Log event sent successfully.")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("CloudWatchLogger", "Error sending log event: ${e.message}")
            }
        }

        private fun getLogStreamName(): String {
            val deviceName = "${Build.MANUFACTURER}_${Build.MODEL}".replace(" ", "_")
            val uuid = UUID.randomUUID().toString()
            return "${uuid}_${deviceName}"
        }
    }
}