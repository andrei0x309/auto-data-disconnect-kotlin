package eu.flashsoft.automatic_data_disconnect

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.provider.Settings
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import androidx.core.content.edit

class WorkerHelper {


    companion object {

        fun isMobileOnAllNetworks(context: Context): Boolean {
            var result = false
            try {
                result = Settings.Global.getInt(context.contentResolver, "mobile_data", 1) == 1
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return result
        }


        fun registerDisconnectWorker(context: Context) {
               val sharedPrefs = context.getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
               val timerDisconnect = sharedPrefs.getInt(AppConstants.DISCONNECT_TIMER_KEY, 15)

               sharedPrefs.edit {
                   putBoolean(AppConstants.NETWORK_STATUS_KEY, true)
               }


               val uploadWorkRequest = OneTimeWorkRequestBuilder<DisconnectWorker>()
                   .setInitialDelay(timerDisconnect.toLong(), TimeUnit.MINUTES)
                   .addTag(AppConstants.WORKER_DISCONNECT_TAG)
                   // Additional configuration
                   .build()
               WorkManager.getInstance(context)
                   .enqueue(uploadWorkRequest)

        }

        fun unregisterDisconnectWorker(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(AppConstants.WORKER_DISCONNECT_TAG)
        }

        fun registerServiceMonitorWorker(context: Context) {
            val uploadWorkRequest = OneTimeWorkRequestBuilder<ServiceMonitorWorker>()
                .setInitialDelay(1, TimeUnit.MINUTES)
                .addTag(AppConstants.WORKER_SERVICE_TAG)
                // Additional configuration
                .build()
            WorkManager.getInstance(context)
                .enqueue(uploadWorkRequest)
        }

        fun unregisterServiceMonitorWorker(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(AppConstants.WORKER_SERVICE_TAG)
        }

    }

}