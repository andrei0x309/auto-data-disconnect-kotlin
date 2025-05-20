package eu.flashsoft.automatic_data_disconnect

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class ServiceMonitorWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
 
    override fun doWork(): Result {
        Log.d(AppConstants.WORKER_SERVICE_TAG, "Worker started")

        // --- Check if the MobileDataMonitorService is running ---
        if (!MobileDataMonitorService.isServiceRunning(applicationContext) && MobileDataMonitorService.isServiceEnabled(applicationContext)) {
            Log.d(AppConstants.WORKER_SERVICE_TAG, "MobileDataMonitorService is not running, starting it...")

            // --- Start the service ---
             MobileDataMonitorService.startService(applicationContext)
            // -------------------------

        } else {
            Log.d(AppConstants.WORKER_SERVICE_TAG, "MobileDataMonitorService is already running.")
        }
        // -------------------------------------------------------
        
        WorkerHelper.registerServiceMonitorWorker(applicationContext)

        return Result.success()
    }
}