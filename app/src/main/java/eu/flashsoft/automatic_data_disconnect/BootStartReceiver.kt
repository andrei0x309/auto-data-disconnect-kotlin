package eu.flashsoft.automatic_data_disconnect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi

class BootStartReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                if (MobileDataMonitorService.isServiceEnabled(context)) {
                    MobileDataMonitorService.startService(context)
                    WorkerHelper.registerServiceMonitorWorker(context)
                }
            }
        }
    }
}