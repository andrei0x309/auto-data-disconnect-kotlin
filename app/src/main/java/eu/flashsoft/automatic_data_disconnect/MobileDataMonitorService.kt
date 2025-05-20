package eu.flashsoft.automatic_data_disconnect

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences // Import SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit // Import the KTX edit extension function
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class MobileDataMonitorService : Service() {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob()) // Coroutine scope for background tasks
    private lateinit var sharedPreferences: SharedPreferences // SharedPreferences instance

    // Job for the notification timer coroutine
    private var notificationTimerJob: Job? = null

    companion object {
        // SERVICE_TAG is now in AppConstants
        // private const val TAG = "MobileDataMonitorService"


        /**
         * Helper function to check if the service is currently marked as running.
         * This can be called from any context in your app.
         */
        fun isServiceRunning(context: Context): Boolean {
            val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
            return prefs.getBoolean(AppConstants.IS_SERVICE_RUNNING_KEY, false)
        }

        /**
         * Helper function to check if the service is enabled by the user.
         */
        fun isServiceEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
            return prefs.getBoolean(AppConstants.IS_SERVICE_ENABLED_KEY, false)
        }

        /**
         * Helper function to enable the service flag in preferences.
         */
        fun enableService(context:Context) {
            val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
            // Use KTX edit extension function
            prefs.edit {
                putBoolean(AppConstants.IS_SERVICE_ENABLED_KEY, true)
            }
        }

        /**
         * Helper function to disable the service flag in preferences.
         */
        fun disableService(context:Context) {
            val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
            // Use KTX edit extension function
            prefs.edit {
                putBoolean(AppConstants.IS_SERVICE_ENABLED_KEY, false)
            }
        }

        /**
         * Helper function to start the foreground service.
         */
        fun startService(context: Context) {
            val serviceIntent = Intent(context, MobileDataMonitorService::class.java)
            // Use startForegroundService() for Android O+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }

        /**
         * Helper function to stop the service.
         */
        fun stopService(context: Context) {
            val serviceIntent = Intent(context, MobileDataMonitorService::class.java)
            context.stopService(serviceIntent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(AppConstants.SERVICE_TAG, "Service created")
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        sharedPreferences = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE) // Get SharedPreferences

        // --- Set the service running flag using KTX edit ---
        sharedPreferences.edit {
            putBoolean(AppConstants.IS_SERVICE_RUNNING_KEY, true)
        }
        Log.d(AppConstants.SERVICE_TAG, "Service running flag set to true")
        // ----------------------------------------------------

        createNotificationChannel() // Create notification channel for Android O+
        startForeground(AppConstants.NOTIFICATION_ID, createNotification()) // Start as a foreground service
        registerNetworkCallback() // Register the network listener
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(AppConstants.SERVICE_TAG, "Service started command")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(AppConstants.SERVICE_TAG, "Service destroyed")
        unregisterNetworkCallback() // Unregister the listener
        serviceScope.cancel() // Cancel the coroutine scope

        // --- Clear the service running flag using KTX edit ---
        sharedPreferences.edit {
            putBoolean(AppConstants.IS_SERVICE_RUNNING_KEY, false) // Set to false
        }
        Log.d(AppConstants.SERVICE_TAG, "Service running flag set to false")
        // ------------------------------------------------------
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Mobile Data Monitor"
            val descriptionText = "Monitors mobile data connection status"
            val importance = NotificationManager.IMPORTANCE_LOW
            // Use constant from AppConstants
            val channel = NotificationChannel(AppConstants.NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        // Build a notification for the foreground service
        val notificationBuilder = NotificationCompat.Builder(this, AppConstants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Mobile Data Monitor Active")
            .setContentText("Monitoring mobile data connection...")
            .setSmallIcon(R.drawable.ic_auto_disconnect_fg) // Replace with your app's icon
            .setPriority(NotificationCompat.PRIORITY_LOW) // Match channel importance
            .setOngoing(true) // Makes the notification non-dismissible

        return notificationBuilder.build()
    }

    /**
     * Updates the existing foreground service notification.
     * @param contentText The text to display in the notification content area.
     */
    private fun updateNotification(contentText: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val notificationBuilder = NotificationCompat.Builder(this, AppConstants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Mobile Data Monitor Active")
            .setContentText(contentText) // Set the updated text
            .setSmallIcon(R.drawable.ic_auto_disconnect_fg) // Use the same icon
            .setPriority(NotificationCompat.PRIORITY_LOW) // Use the same priority
            .setOngoing(true) // Keep it ongoing
            .setOnlyAlertOnce(true) // Important: Avoid sound/vibration on every update

        notificationManager.notify(AppConstants.NOTIFICATION_ID, notificationBuilder.build())
    }

    /**
     * Starts a coroutine to update the notification with the remaining time.
     */
    @SuppressLint("DefaultLocale")
    private fun startNotificationTimer() {
        // Cancel any existing timer job before starting a new one
        stopNotificationTimer()

        // Only start the timer if the service is enabled and a disconnect is pending
        if (!sharedPreferences.getBoolean(AppConstants.IS_SERVICE_ENABLED_KEY, false)) {
            Log.d(AppConstants.SERVICE_TAG, "Not starting notification timer: Service not enabled or disconnect not pending.")
            updateNotification("Monitoring mobile data connection...") // Reset notification text
            return
        }


        notificationTimerJob = serviceScope.launch {
            val disconnectTimerMinutes = sharedPreferences.getInt(AppConstants.DISCONNECT_TIMER_KEY, 15)
            val disconnectTimestamp = sharedPreferences.getLong(AppConstants.DISCONNECT_TIMESTAMP, System.currentTimeMillis())

            // Calculate the target disconnect time in milliseconds
            val targetDisconnectTimeMillis = disconnectTimestamp + TimeUnit.MINUTES.toMillis(disconnectTimerMinutes.toLong())

            Log.d(AppConstants.SERVICE_TAG, "Notification timer coroutine started.")

            while (isActive) { // Loop while the coroutine is active
                val currentTimeMillis = System.currentTimeMillis()
                val remainingTimeMillis = targetDisconnectTimeMillis - currentTimeMillis

                if (remainingTimeMillis <= 0) {
                    // Timer has reached zero or gone past
                    updateNotification("Disconnecting network...") // Update notification text
                    // The Worker should handle the actual disconnect and potentially stop the service
                    stopNotificationTimer() // Stop the timer coroutine

                    delay(1000)

                    updateNotification("Network disconnected")

                    // Pending state should be handled by the Worker upon completion/failure
                    break // Exit the loop
                } else {
                    // Calculate minutes and seconds
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTimeMillis)
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingTimeMillis) % 60

                    // Format the time as MM:SS
                    val timeString = String.format("%02d:%02d", minutes, seconds)

                    // Update the notification content text
                    updateNotification("Disconnecting in $timeString")
                }

                delay(1000) // Wait for 1 second
            }
            Log.d(AppConstants.SERVICE_TAG, "Notification timer coroutine finished.")
        }
    }

    private fun stopNotificationTimer() {
        notificationTimerJob?.cancel() // Cancel the job if it exists
        notificationTimerJob = null // Clear the job reference
        Log.d(AppConstants.SERVICE_TAG, "Notification timer stopped.")
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR) // Listen for mobile data

        val wiFiAlso = sharedPreferences.getBoolean(AppConstants.IS_WIFI_DISCONNECT_ENABLED_KEY, false)

        if(wiFiAlso) networkRequest.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)

        val builtNetworkRequest = networkRequest.build()


        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d(AppConstants.SERVICE_TAG, "Mobile data network available: $network")
                // --- Mobile data connected event received ---
                // This is where you would trigger your root-based scheduling logic.
                // We use a coroutine to avoid blocking the main thread.
                serviceScope.launch {
                    Log.d(AppConstants.SERVICE_TAG, "Scheduling mobile data disconnect...")
                    // Pass 'this' (the Service instance, which is a Context) to the function
                    scheduleMobileDataDisconnectWithRoot(this@MobileDataMonitorService) // Pass the context
                    val sharedPrefs = applicationContext.getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
                    sharedPrefs.edit {
                        putBoolean(AppConstants.NETWORK_STATUS_KEY, true)
                        putLong(AppConstants.DISCONNECT_TIMESTAMP, System.currentTimeMillis())
                    }
                    stopNotificationTimer()
                    startNotificationTimer()
                }
                // ------------------------------------------
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                WorkerHelper.unregisterDisconnectWorker(applicationContext)
                Log.d(AppConstants.SERVICE_TAG, "Mobile data network lost: $network")
                val sharedPrefs = applicationContext.getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
                sharedPrefs.edit {
                    putBoolean(AppConstants.NETWORK_STATUS_KEY, false)
                }
                // Handle mobile data disconnection if needed
            }

            // Implement other callback methods if necessary (onCapabilitiesChanged, onLosing, etc.)
        }

        // Register the callback
        connectivityManager.registerNetworkCallback(builtNetworkRequest, networkCallback)
        Log.d(AppConstants.SERVICE_TAG, "Network callback registered for mobile data")
    }

    private fun unregisterNetworkCallback() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Log.d(AppConstants.SERVICE_TAG, "Network callback unregistered")
        } catch (e: IllegalArgumentException) {
            Log.e(AppConstants.SERVICE_TAG, "Error unregistering network callback: ${e.message}")
            // This might happen if the callback was already unregistered or never registered
        }
    }

    /**
     * Placeholder function where you would implement your root-based
     * logic to schedule the mobile data disconnect.
     * This function should be safe to call from a background thread (e.g., within a coroutine).
     *
     * @param context The application context needed by WorkerHelper.
     */
    private fun scheduleMobileDataDisconnectWithRoot(context: Context) {
        Log.d(AppConstants.SERVICE_TAG, "Do Disconnect")
        // Check if the service is enabled by the user before scheduling the worker
        if (isServiceEnabled(context)) { // Use the helper function to check the enabled state
            WorkerHelper.unregisterDisconnectWorker(context)
            if (WorkerHelper.isMobileOnAllNetworks(context)) {
                WorkerHelper.registerDisconnectWorker(context)
            }
        } else {
            Log.d(AppConstants.SERVICE_TAG, "Service is disabled, not scheduling disconnect worker.")
        }
    }
}
