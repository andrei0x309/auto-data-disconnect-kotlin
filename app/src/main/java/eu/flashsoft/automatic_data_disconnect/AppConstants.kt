package eu.flashsoft.automatic_data_disconnect

object AppConstants {


    const val SERVICE_TAG = "DataMonitorService"
    const val WORKER_SERVICE_TAG = "ServiceMonitorWorker"
    const val WORKER_DISCONNECT_TAG = "DisconnectWorker"
    const val MAIN_ACTIVITY_TAG = "MainActivity"
    const val FRAGMENT_LOGS_TAG = "LogsFragment"


    const val NOTIFICATION_CHANNEL_ID = "mobile_data_monitor_channel"
    const val NOTIFICATION_ID = 309
    const val PREFS_NAME = "app_settings" // Name for Shared Preferences file
    const val IS_SERVICE_RUNNING_KEY = "isServiceRunning" // Key for the boolean flag
    const val IS_SERVICE_ENABLED_KEY = "isServiceEnabled" // Key for the boolean flag
    const val IS_LOGS_ENABLED_KEY = "isLogsEnabled" // Key for the boolean flag
    const val IS_WIFI_DISCONNECT_ENABLED_KEY = "isWifiDisconnectEnabled" // Key for the boolean flag
    const val IS_SERVICE_NOTIFICATION_ENABLED_A13_KEY = "isServiceNotificationEnabledA13" // Key for the boolean flag
    const val DISCONNECT_TIMER_KEY = "disconnectTimerMin"
    const val DISCONNECT_TIMESTAMP = "disconnectStamp"
    const val PREFERENCES_INITIALIZED_KEY = "initialized"
    const val NETWORK_STATUS_KEY = "networkStatus"

}
